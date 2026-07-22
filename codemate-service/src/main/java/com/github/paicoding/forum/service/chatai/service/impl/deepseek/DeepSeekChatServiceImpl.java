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
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.reliability.ModelFailureClassifier;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import com.github.paicoding.forum.service.bugdiagnosis.service.BugDiagnosisService;
import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    @Autowired
    private AgentRunService agentRunService;
    @Autowired
    private BugDiagnosisService bugDiagnosisService;
    @Autowired
    private LangChain4jProperties langChain4jProperties;
    @Autowired
    private ModelFailureClassifier modelFailureClassifier;

    @Override
    public AiChatStatEnum doAnswer(Long user, ChatItemVo chat) {
        AgentRequest request = agentRequestResolver.resolve(chat.getQuestion());
        AgentModeHandler handler = agentModeRegistry.get(request.getMode());
        String normalizedInput = handler.validateAndNormalize(request.getInput());
        chat.setQuestion(handler.displayQuestion(normalizedInput)).setAgentMode(request.getMode().name());
        Long runId = agentRunService.create(user, ReqInfoContext.getReqInfo().getChatId(),
                request.getMode().name(), normalizedInput, activeModelName());
        chat.setAgentRunId(runId);
        agentRunService.transition(runId, AgentRunStatusEnum.PLANNING);
        agentRunService.transition(runId, AgentRunStatusEnum.EXECUTING);
        try {
            boolean success = deepSeekIntegration.directReturn(chat);
            if (success) {
                return finishSuccessfulRun(user, request.getMode(), ReqInfoContext.getReqInfo().getChatId(),
                        chat, runId, null) ? AiChatStatEnum.END : AiChatStatEnum.ERROR;
            }
            failRun(runId, "MODEL_REQUEST_FAILED");
            return AiChatStatEnum.ERROR;
        } catch (RuntimeException e) {
            failRun(runId, e.getClass().getSimpleName());
            throw e;
        }
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

        final Long runId;
        try {
            runId = agentRunService.create(user, ReqInfoContext.getReqInfo().getChatId(), mode.name(),
                    normalizedInput, activeModelName());
            item.setAgentRunId(runId);
            agentRunService.transition(runId, AgentRunStatusEnum.PLANNING);
            agentRunService.transition(runId, AgentRunStatusEnum.EXECUTING);
        } catch (RuntimeException e) {
            item.initAnswer("Agent Run 创建失败").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
            consumer.accept(AiChatStatEnum.ERROR, response);
            return AiChatStatEnum.IGNORE;
        }

        if (langChain4jAgentOrchestrator.isAvailable()) {
            AtomicBoolean degraded = new AtomicBoolean();
            try {
                langChain4jAgentOrchestrator.stream(
                        mode,
                        user,
                        runId,
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
                                    failRun(runId, "EMPTY_MODEL_RESPONSE");
                                    item.initAnswer("The model returned an empty response")
                                            .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                    consumer.accept(AiChatStatEnum.ERROR, response);
                                    return;
                                }
                                boolean finished = degraded.get()
                                        ? completeDegradedRun(runId, chatResponse)
                                        : finishSuccessfulRun(user, mode, ReqInfoContext.getReqInfo().getChatId(),
                                                item, runId, chatResponse);
                                if (!finished) {
                                    item.setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                    consumer.accept(AiChatStatEnum.ERROR, response);
                                    return;
                                }
                                item.appendAnswer("\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                consumer.accept(AiChatStatEnum.END, response);
                            }

                            @Override
                            public void onError(Throwable error) {
                                failRun(runId, modelFailureClassifier.classify(error).name());
                                log.warn("LangChain4j streaming request failed, mode={}", mode, error);
                                item.appendAnswer("\n" + modelFailureClassifier.publicMessage(error))
                                        .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                                consumer.accept(AiChatStatEnum.ERROR, response);
                            }

                            @Override
                            public void onFallback(String notice) {
                                degraded.set(true);
                                item.appendAnswer("\n\n> " + notice + "\n\n");
                                consumer.accept(AiChatStatEnum.MID, response);
                            }
                        });
                return AiChatStatEnum.IGNORE;
            } catch (RuntimeException e) {
                if (!langChain4jAgentOrchestrator.isFallbackEnabled()) {
                    failRun(runId, "AGENT_STARTUP_FAILED");
                    item.initAnswer("Agent startup failed: " + e.getMessage())
                            .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                    consumer.accept(AiChatStatEnum.ERROR, response);
                    return AiChatStatEnum.IGNORE;
                }
                log.warn("LangChain4j could not start; falling back to the legacy DeepSeek stream, mode={}", mode, e);
                item.appendAnswer("\n\n> Agent 模型暂不可用，已降级为普通问答；回答不会包含工具执行结果。\n\n");
                consumer.accept(AiChatStatEnum.MID, response);
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
                        failRun(runId, "EMPTY_MODEL_RESPONSE");
                        item.appendAnswer("模型未返回结果，请重新提问。\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                        consumer.accept(AiChatStatEnum.ERROR, response);
                    } else {
                        if (!finishSuccessfulRun(user, mode, ReqInfoContext.getReqInfo().getChatId(),
                                item, runId, null)) {
                            item.setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                            consumer.accept(AiChatStatEnum.ERROR, response);
                            return;
                        }
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
                failRun(runId, throwable == null ? "LEGACY_STREAM_ERROR" : throwable.getClass().getSimpleName());
                item.appendAnswer("Error:" + (StringUtils.isBlank(res) ? throwable.getMessage() : res))
                        .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                consumer.accept(AiChatStatEnum.ERROR, response);
            }
        };
        listener.setOnComplate(s -> {
            if (!finishSuccessfulRun(user, mode, ReqInfoContext.getReqInfo().getChatId(), item, runId, null)) {
                item.setAnswerType(ChatAnswerTypeEnum.STREAM_END);
                consumer.accept(AiChatStatEnum.ERROR, response);
                return;
            }
            item.appendAnswer("\n").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
            consumer.accept(AiChatStatEnum.END, response);
        });

        List<ChatItemVo> records = response.getRecords();
        String systemPrompt = handler.systemPrompt(normalizedInput);
        if (StringUtils.isNotBlank(systemPrompt)) {
            records = new ArrayList<>(records);
            records.add(new ChatItemVo().setQuestion(ChatConstants.PROMPT_TAG + systemPrompt));
        }
        try {
            deepSeekIntegration.streamReturn(records, listener);
        } catch (RuntimeException e) {
            failRun(runId, e.getClass().getSimpleName());
            item.appendAnswer("Error:" + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()))
                    .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
            consumer.accept(AiChatStatEnum.ERROR, response);
        }
        return AiChatStatEnum.IGNORE;
    }

    private void completeRun(Long runId, ChatResponse response) {
        try {
            agentRunService.complete(runId,
                    response == null || response.tokenUsage() == null ? null : response.tokenUsage().inputTokenCount(),
                    response == null || response.tokenUsage() == null ? null : response.tokenUsage().outputTokenCount(),
                    response == null || response.tokenUsage() == null ? null : response.tokenUsage().totalTokenCount());
        } catch (RuntimeException e) {
            log.error("Failed to persist Agent Run completion, runId={}", runId, e);
        }
    }

    private boolean finishSuccessfulRun(Long userId, AgentMode mode, String chatId, ChatItemVo item,
                                        Long runId, ChatResponse response) {
        if (mode != AgentMode.BUG_DIAGNOSIS) {
            completeRun(runId, response);
            return true;
        }
        try {
            Long diagnosisId = bugDiagnosisService.createPreview(userId, runId, chatId, item.getAnswer());
            agentRunService.waitForConfirmation(runId,
                    response == null || response.tokenUsage() == null ? null : response.tokenUsage().inputTokenCount(),
                    response == null || response.tokenUsage() == null ? null : response.tokenUsage().outputTokenCount(),
                    response == null || response.tokenUsage() == null ? null : response.tokenUsage().totalTokenCount());
            if (!AgentRunStatusEnum.WAITING_CONFIRMATION.name()
                    .equals(agentRunService.detail(userId, runId).getStatus())) {
                throw new IllegalStateException("Bug diagnosis Run could not enter confirmation state");
            }
            item.setDiagnosisId(diagnosisId);
            return true;
        } catch (RuntimeException e) {
            log.warn("Bug diagnosis structured output could not be persisted, runId={}", runId, e);
            failRun(runId, "INVALID_DIAGNOSIS_OUTPUT");
            item.appendAnswer("\n\n诊断结果未通过结构校验，未创建预览或任务计划，请重试。");
            return false;
        }
    }

    private void failRun(Long runId, String reason) {
        try {
            agentRunService.fail(runId, reason);
        } catch (RuntimeException e) {
            log.error("Failed to persist Agent Run failure, runId={}", runId, e);
        }
    }

    private boolean completeDegradedRun(Long runId, ChatResponse response) {
        completeRun(runId, response);
        return true;
    }

    private String activeModelName() {
        try {
            return langChain4jAgentOrchestrator.activeProviderName() + ":" + langChain4jAgentOrchestrator.activeModelName();
        } catch (RuntimeException ignored) {
            return langChain4jProperties.getProvider() + ":unavailable";
        }
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
