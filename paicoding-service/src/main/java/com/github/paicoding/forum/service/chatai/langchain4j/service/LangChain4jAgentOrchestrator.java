package com.github.paicoding.forum.service.chatai.langchain4j.service;

import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.service.chatai.agent.AgentMode;
import com.github.paicoding.forum.service.chatai.agent.AgentModeRegistry;
import com.github.paicoding.forum.service.chatai.langchain4j.assistant.CodeMateStreamingAssistant;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.memory.CodeMateChatMemoryProvider;
import com.github.paicoding.forum.service.chatai.langchain4j.observability.AgentMetrics;
import com.github.paicoding.forum.service.chatai.langchain4j.tool.CommunityKnowledgeTools;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class LangChain4jAgentOrchestrator {
    private static final String BASE_PROMPT = "You are CodeMate, a senior programming assistant. "
            + "Answer in concise Chinese Markdown, keep claims verifiable, use the community-search tool "
            + "when project facts are needed, and never claim to have run code or commands that were not run.";

    private final LangChain4jProperties properties;
    private final StreamingChatModel streamingChatModel;
    private final CodeMateChatMemoryProvider memoryProvider;
    private final ContentRetriever contentRetriever;
    private final CommunityKnowledgeTools knowledgeTools;
    private final AgentModeRegistry modeRegistry;
    private final AgentMetrics metrics;
    private final Map<AgentMode, CodeMateStreamingAssistant> assistants = new EnumMap<>(AgentMode.class);
    private final ConcurrentMap<String, AtomicBoolean> conversationBusy = new ConcurrentHashMap<>();

    public LangChain4jAgentOrchestrator(LangChain4jProperties properties,
                                        @Qualifier("codeMateStreamingChatModel") StreamingChatModel streamingChatModel,
                                        CodeMateChatMemoryProvider memoryProvider,
                                        @Qualifier("codeMateContentRetriever") ContentRetriever contentRetriever,
                                        CommunityKnowledgeTools knowledgeTools,
                                        AgentModeRegistry modeRegistry,
                                        AgentMetrics metrics) {
        this.properties = properties;
        this.streamingChatModel = streamingChatModel;
        this.memoryProvider = memoryProvider;
        this.contentRetriever = contentRetriever;
        this.knowledgeTools = knowledgeTools;
        this.modeRegistry = modeRegistry;
        this.metrics = metrics;
    }

    @PostConstruct
    public void initialize() {
        assistants.put(AgentMode.CHAT, buildAssistant(BASE_PROMPT, true, false));
        assistants.put(AgentMode.BUG_DIAGNOSIS,
                buildAssistant(modeRegistry.get(AgentMode.BUG_DIAGNOSIS).systemPrompt(), false, false));
        assistants.put(AgentMode.TASK_PLANNING,
                buildAssistant(modeRegistry.get(AgentMode.TASK_PLANNING).systemPrompt(), false, false));
        assistants.put(AgentMode.KNOWLEDGE_QA,
                buildAssistant(modeRegistry.get(AgentMode.KNOWLEDGE_QA).systemPrompt(), false, true));
    }

    public boolean isAvailable() {
        return properties.isAvailable();
    }

    public boolean isFallbackEnabled() {
        return properties.isFallbackEnabled();
    }

    public void stream(AgentMode mode,
                       Long userId,
                       String chatId,
                       String input,
                       List<ChatItemVo> history,
                       ChatItemVo current,
                       AgentStreamObserver observer) {
        if (!isAvailable()) {
            throw new IllegalStateException("LangChain4j is disabled or the DeepSeek API key is missing");
        }
        CodeMateStreamingAssistant assistant = assistants.get(mode);
        if (assistant == null) {
            throw new IllegalArgumentException("Unsupported LangChain4j agent mode: " + mode);
        }
        String memoryId = userId + ":" + chatId + ":" + mode.name();
        AtomicBoolean busy = conversationBusy.computeIfAbsent(memoryId, key -> new AtomicBoolean());
        if (!busy.compareAndSet(false, true)) {
            throw new IllegalStateException("An agent request is already running for this conversation");
        }
        memoryProvider.seedIfEmpty(memoryId, history, current);
        metrics.request(mode);
        Instant startedAt = Instant.now();
        try {
            TokenStream stream = assistant.chat(memoryId, input);
            stream.onPartialResponse(observer::onToken)
                    .onRetrieved(contents -> {
                        metrics.retrieved(mode, contents.size());
                        observer.onRetrieved(contents);
                    })
                    .onToolExecuted(execution -> {
                        metrics.toolExecuted(mode, execution.hasFailed());
                        observer.onToolExecuted(execution);
                    })
                    .onCompleteResponse(response -> {
                        metrics.success(mode, Duration.between(startedAt, Instant.now()), response.tokenUsage());
                        release(memoryId, busy);
                        observer.onComplete(response);
                    })
                    .onError(error -> {
                        metrics.error(mode);
                        release(memoryId, busy);
                        observer.onError(error);
                    })
                    .start();
        } catch (RuntimeException e) {
            metrics.error(mode);
            release(memoryId, busy);
            throw e;
        }
    }

    public void evictMemory(Long userId, String chatId, AgentMode mode) {
        memoryProvider.evict(userId + ":" + chatId + ":" + mode.name());
    }

    private CodeMateStreamingAssistant buildAssistant(String systemPrompt, boolean tools, boolean rag) {
        AiServices<CodeMateStreamingAssistant> builder = AiServices.builder(CodeMateStreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .systemMessage(StringUtils.defaultIfBlank(systemPrompt, BASE_PROMPT))
                .chatMemoryProvider(memoryProvider)
                .storeRetrievedContentInChatMemory(false)
                .maxToolCallingRoundTrips(properties.getMaxToolRoundTrips());
        if (tools) {
            builder.tools(knowledgeTools);
        }
        if (rag) {
            builder.contentRetriever(contentRetriever);
        }
        return builder.build();
    }

    private void release(String memoryId, AtomicBoolean busy) {
        busy.set(false);
        conversationBusy.remove(memoryId, busy);
    }
}
