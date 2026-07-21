package com.github.paicoding.forum.service.chatai.langchain4j.service;

import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.api.model.vo.chat.AiCitationVo;
import com.github.paicoding.forum.service.chatai.agent.AgentMode;
import com.github.paicoding.forum.service.chatai.agent.AgentModeRegistry;
import com.github.paicoding.forum.service.chatai.langchain4j.assistant.CodeMateStreamingAssistant;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.memory.CodeMateChatMemoryProvider;
import com.github.paicoding.forum.service.chatai.memory.MemoryContextAssembler;
import com.github.paicoding.forum.service.chatai.langchain4j.observability.AgentMetrics;
import com.github.paicoding.forum.service.chatai.langchain4j.tool.CodeMateTool;
import com.github.paicoding.forum.service.chatai.langchain4j.tool.TrustedToolContextRegistry;
import com.github.paicoding.forum.service.agentrun.service.AgentRunContextRegistry;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import com.github.paicoding.forum.service.chatai.rag.store.MysqlArticleEmbeddingStore;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class LangChain4jAgentOrchestrator {
    private static final String BASE_PROMPT = "You are CodeMate, a senior programming assistant. "
            + "Answer in concise Chinese Markdown, keep claims verifiable, use the available read-only community tools "
            + "when project facts are needed, and never claim to have run code or commands that were not run.";

    private final LangChain4jProperties properties;
    private final StreamingChatModel streamingChatModel;
    private final CodeMateChatMemoryProvider memoryProvider;
    private final ContentRetriever contentRetriever;
    private final List<CodeMateTool> tools;
    private final TrustedToolContextRegistry toolContextRegistry;
    private final AgentRunContextRegistry runContextRegistry;
    private final AgentRunService agentRunService;
    private final AgentModeRegistry modeRegistry;
    private final AgentMetrics metrics;
    private final MemoryContextAssembler memoryContextAssembler;
    private final Map<AgentMode, CodeMateStreamingAssistant> assistants = new EnumMap<>(AgentMode.class);
    private final ConcurrentMap<String, AtomicBoolean> conversationBusy = new ConcurrentHashMap<>();

    public LangChain4jAgentOrchestrator(LangChain4jProperties properties,
                                        @Qualifier("codeMateStreamingChatModel") StreamingChatModel streamingChatModel,
                                        CodeMateChatMemoryProvider memoryProvider,
                                        @Qualifier("codeMateContentRetriever") ContentRetriever contentRetriever,
                                        List<CodeMateTool> tools,
                                        TrustedToolContextRegistry toolContextRegistry,
                                        AgentRunContextRegistry runContextRegistry,
                                        AgentRunService agentRunService,
                                        AgentModeRegistry modeRegistry,
                                        AgentMetrics metrics,
                                        MemoryContextAssembler memoryContextAssembler) {
        this.properties = properties;
        this.streamingChatModel = streamingChatModel;
        this.memoryProvider = memoryProvider;
        this.contentRetriever = contentRetriever;
        this.tools = tools;
        this.toolContextRegistry = toolContextRegistry;
        this.runContextRegistry = runContextRegistry;
        this.agentRunService = agentRunService;
        this.modeRegistry = modeRegistry;
        this.metrics = metrics;
        this.memoryContextAssembler = memoryContextAssembler;
    }

    @PostConstruct
    public void initialize() {
        assistants.put(AgentMode.CHAT, buildAssistant(BASE_PROMPT, true, false, true));
        assistants.put(AgentMode.BUG_DIAGNOSIS,
                buildAssistant(modeRegistry.get(AgentMode.BUG_DIAGNOSIS).systemPrompt(), true, true, false));
        assistants.put(AgentMode.TASK_PLANNING,
                buildAssistant(modeRegistry.get(AgentMode.TASK_PLANNING).systemPrompt(), false, false, false));
        assistants.put(AgentMode.KNOWLEDGE_QA,
                buildAssistant(modeRegistry.get(AgentMode.KNOWLEDGE_QA).systemPrompt(), false, true, false));
    }

    public boolean isAvailable() {
        return properties.isAvailable();
    }

    public boolean isFallbackEnabled() {
        return properties.isFallbackEnabled();
    }

    public void stream(AgentMode mode,
                       Long userId,
                       Long runId,
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
        Instant startedAt = Instant.now();
        try {
            toolContextRegistry.bind(memoryId, userId);
            runContextRegistry.bind(memoryId, runId);
            memoryProvider.seedIfEmpty(memoryId, history, current);
            metrics.request(mode);
            String systemPrompt = memoryContextAssembler.assemble(systemPrompt(mode), userId, memoryId);
            TokenStream stream = assistant.chat(memoryId, systemPrompt, input);
            stream.onPartialResponse(observer::onToken)
                    .onRetrieved(contents -> {
                        metrics.retrieved(mode, contents.size());
                        recordEvidence(runId, contents);
                        attachCitations(current, contents);
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

    private CodeMateStreamingAssistant buildAssistant(String systemPrompt, boolean tools, boolean rag,
                                                       boolean allowWriteTools) {
        AiServices<CodeMateStreamingAssistant> builder = AiServices.builder(CodeMateStreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryProvider)
                .storeRetrievedContentInChatMemory(false)
                .maxToolCallingRoundTrips(properties.getMaxToolRoundTrips());
        if (tools) {
            builder.tools(this.tools.stream()
                    .filter(tool -> allowWriteTools || tool.riskLevel() == com.github.paicoding.forum.service.chatai.langchain4j.tool.ToolRiskLevel.READ_ONLY)
                    .map(tool -> (Object) tool).collect(java.util.stream.Collectors.toList()));
        }
        if (rag) {
            builder.contentRetriever(contentRetriever);
        }
        return builder.build();
    }

    private String systemPrompt(AgentMode mode) {
        if (mode == AgentMode.CHAT) return BASE_PROMPT;
        return StringUtils.defaultIfBlank(modeRegistry.get(mode).systemPrompt(), BASE_PROMPT);
    }

    private void release(String memoryId, AtomicBoolean busy) {
        toolContextRegistry.unbind(memoryId);
        runContextRegistry.unbind(memoryId);
        busy.set(false);
        conversationBusy.remove(memoryId, busy);
    }

    private void recordEvidence(Long runId, List<Content> contents) {
        if (runId == null || contents == null) {
            return;
        }
        for (Content content : contents) {
            try {
                Long articleId = content.textSegment().metadata()
                        .getLong(MysqlArticleEmbeddingStore.ARTICLE_ID);
                Integer chunkIndex = content.textSegment().metadata()
                        .getInteger(MysqlArticleEmbeddingStore.CHUNK_INDEX);
                String title = content.textSegment().metadata()
                        .getString(MysqlArticleEmbeddingStore.ARTICLE_TITLE);
                Object score = content.metadata().get(ContentMetadata.SCORE);
                Double relevance = score instanceof Number ? ((Number) score).doubleValue() : null;
                agentRunService.recordEvidence(runId, articleId, chunkIndex, title,
                        content.textSegment().text(), relevance);
            } catch (RuntimeException e) {
                // Trace persistence must never break the streaming response.
                org.slf4j.LoggerFactory.getLogger(LangChain4jAgentOrchestrator.class)
                        .warn("Failed to persist Agent evidence, runId={}", runId, e);
            }
        }
    }

    private void attachCitations(ChatItemVo current, List<Content> contents) {
        if (current == null || contents == null) return;
        List<AiCitationVo> citations = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Content content : contents) {
            try {
                Long articleId = content.textSegment().metadata().getLong(MysqlArticleEmbeddingStore.ARTICLE_ID);
                Integer chunkIndex = content.textSegment().metadata().getInteger(MysqlArticleEmbeddingStore.CHUNK_INDEX);
                String key = articleId + ":" + chunkIndex;
                if (!seen.add(key)) continue;
                AiCitationVo citation = new AiCitationVo();
                citation.setCitationIndex(citations.size() + 1);
                citation.setArticleId(articleId);
                citation.setChunkIndex(chunkIndex);
                citation.setTitle(content.textSegment().metadata().getString(MysqlArticleEmbeddingStore.ARTICLE_TITLE));
                citation.setHeading(content.textSegment().metadata().getString(MysqlArticleEmbeddingStore.ARTICLE_HEADING));
                citation.setExcerpt(StringUtils.left(content.textSegment().text(), 500));
                Object score = content.metadata().get(ContentMetadata.SCORE);
                citation.setRelevance(score instanceof Number ? ((Number) score).doubleValue() : null);
                citations.add(citation);
            } catch (RuntimeException ignored) {
                // Invalid metadata is omitted instead of exposing an unverifiable citation.
            }
        }
        current.setCitations(citations);
    }
}
