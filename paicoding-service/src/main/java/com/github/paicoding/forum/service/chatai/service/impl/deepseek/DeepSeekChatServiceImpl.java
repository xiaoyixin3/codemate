package com.github.paicoding.forum.service.chatai.service.impl.deepseek;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.enums.ChatAnswerTypeEnum;
import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.api.model.enums.ai.AiChatStatEnum;
import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.api.model.vo.chat.ChatRecordsVo;
import com.github.paicoding.forum.service.chatai.agent.AgentModeHandler;
import com.github.paicoding.forum.service.chatai.agent.AgentMode;
import com.github.paicoding.forum.service.chatai.agent.AgentModeRegistry;
import com.github.paicoding.forum.service.chatai.agent.AgentRequest;
import com.github.paicoding.forum.service.chatai.agent.AgentRequestResolver;
import com.github.paicoding.forum.service.chatai.constants.ChatConstants;
import com.github.paicoding.forum.service.chatai.langchain4j.service.AgentStreamObserver;
import com.github.paicoding.forum.service.chatai.langchain4j.service.LangChain4jAgentOrchestrator;
import com.github.paicoding.forum.service.chatai.service.AbsChatService;
import dev.langchain4j.model.chat.response.ChatResponse;
import com.plexpt.chatgpt.listener.AbstractStreamListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** DeepSeek chat implementation. Agent-specific behavior is delegated to registered handlers. */
@Slf4j
@Service
public class DeepSeekChatServiceImpl extends AbsChatService {
    @Autowired
    private DeepSeekIntegration deepSeekIntegration;
    @Autowired
    private AgentRequestResolver agentRequestResolver;
    @Autowired
    private AgentModeRegistry agentModeRegistry;
    @Autowired
    private LangChain4jAgentOrchestrator langChain4jAgentOrchestrator;

    @Override
    public AiChatStatEnum doAnswer(Long user, ChatItemVo chat) {
        return deepSeekIntegration.directReturn(chat) ? AiChatStatEnum.END : AiChatStatEnum.ERROR;
    }

    @Override
    public AiChatStatEnum doAsyncAnswer(Long user, ChatRecordsVo response, BiConsumer<AiChatStatEnum, ChatRecordsVo> consumer) {
        ChatItemVo item = response.getRecords().get(0);
        final AgentModeHandler handler;
        final AgentMode mode;
        final String normalizedInput;
        try {
            AgentRequest request = agentRequestResolver.resolve(item.getQuestion());
            mode = request.getMode();
            handler = agentModeRegistry.get(request.getMode());
            normalizedInput = handler.validateAndNormalize(request.getInput());
            item.setQuestion(handler.displayQuestion(normalizedInput)).setAgentMode(request.getMode().name());
        } catch (IllegalArgumentException e) {
            item.initAnswer(e.getMessage()).setAnswerType(ChatAnswerTypeEnum.STREAM_END);
            consumer.accept(AiChatStatEnum.ERROR, response);
            return AiChatStatEnum.IGNORE;
        }

        if (langChain4jAgentOrchestrator.isAvailable()) {
            try {
                langChain4jAgentOrchestrator.stream(
                        mode,
                        user,
                        ReqInfoContext.getReqInfo().getChatId(),
                        normalizedInput,
                        response.getRecords(),
                        item,
                        new AgentStreamObserver() {
                            @Override
                            public void onToken(String token) {
                                if (StringUtils.isNotEmpty(token)) {
                                    item.appendAnswer(token);
                                    consumer.accept(AiChatStatEnum.MID, response);
                                }
                            }

                            @Override
                            public void onComplete(ChatResponse chatResponse) {
                                if (StringUtils.isBlank(item.getAnswer())) {
                                    item.initAnswer("The model returned an empty response")
                                            .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                    consumer.accept(AiChatStatEnum.ERROR, response);
                                    return;
                                }
                                item.appendAnswer("\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                consumer.accept(AiChatStatEnum.END, response);
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.warn("LangChain4j streaming request failed, mode={}", mode, error);
                                String message = StringUtils.defaultIfBlank(error.getMessage(), error.getClass().getSimpleName());
                                item.appendAnswer("\nAgent error: " + message)
                                        .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                consumer.accept(AiChatStatEnum.ERROR, response);
                            }
                        });
                return AiChatStatEnum.IGNORE;
            } catch (RuntimeException e) {
                if (!langChain4jAgentOrchestrator.isFallbackEnabled()) {
                    item.initAnswer("Agent startup failed: " + e.getMessage())
                            .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                    consumer.accept(AiChatStatEnum.ERROR, response);
                    return AiChatStatEnum.IGNORE;
                }
                log.warn("LangChain4j could not start; falling back to the legacy DeepSeek stream, mode={}", mode, e);
            }
        }

        AbstractStreamListener listener = new AbstractStreamListener() {
            @Override
            public void onOpen(EventSource eventSource, Response res) {
                super.onOpen(eventSource, res);
            }

            @Override
            public void onClosed(EventSource eventSource) {
                super.onClosed(eventSource);
                if (item.getAnswerType() != ChatAnswerTypeEnum.STREAM_END) {
                    if (StringUtils.isBlank(lastMessage)) {
                        item.appendAnswer("模型未返回结果，请重新提问。\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                        consumer.accept(AiChatStatEnum.ERROR, response);
                    } else {
                        item.appendAnswer("\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                        consumer.accept(AiChatStatEnum.END, response);
                    }
                }
            }

            @Override
            public void onMsg(String message) {
                if (StringUtils.isNotBlank(lastMessage)) {
                    item.appendAnswer(message);
                    consumer.accept(AiChatStatEnum.MID, response);
                }
            }

            @Override
            public void onError(Throwable throwable, String res) {
                item.appendAnswer("Error:" + (StringUtils.isBlank(res) ? throwable.getMessage() : res))
                        .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                consumer.accept(AiChatStatEnum.ERROR, response);
            }
        };
        listener.setOnComplate(s -> {
            item.appendAnswer("\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
            consumer.accept(AiChatStatEnum.END, response);
        });

        List<ChatItemVo> records = response.getRecords();
        String systemPrompt = handler.systemPrompt(normalizedInput);
        if (StringUtils.isNotBlank(systemPrompt)) {
            records = new ArrayList<>(records);
            records.add(new ChatItemVo().setQuestion(ChatConstants.PROMPT_TAG + systemPrompt));
        }
        deepSeekIntegration.streamReturn(records, listener);
        return AiChatStatEnum.IGNORE;
    }

    @Override
    protected void processAfterSuccessedAnswered(Long user, ChatRecordsVo response) {
        ChatItemVo item = response.getRecords().get(0);
        try {
            AgentRequest request = agentRequestResolver.resolve(item.getQuestion());
            agentModeRegistry.get(request.getMode()).transformResult(user, ReqInfoContext.getReqInfo().getChatId(), response, item);
        } catch (IllegalArgumentException ignored) {
            // The request was normalized to a display-safe question before streaming.
        }
        super.processAfterSuccessedAnswered(user, response);
    }

    @Override
    public AISourceEnum source() {
        return AISourceEnum.DEEP_SEEK;
    }
}
