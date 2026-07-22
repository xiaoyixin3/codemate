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
import com.github.paicoding.forum.service.chatai.langchain4j.model.ChatModelProvider;
import com.github.paicoding.forum.service.chatai.langchain4j.model.ChatModelProviderRegistry;
import com.github.paicoding.forum.service.chatai.langchain4j.reliability.AgentFallbackPolicy;
import com.github.paicoding.forum.service.chatai.langchain4j.reliability.ModelFailureClassifier;
import com.github.paicoding.forum.service.chatai.langchain4j.reliability.ModelFailureType;
import com.github.paicoding.forum.service.chatai.langchain4j.reliability.StreamTimeoutException;
import com.github.paicoding.forum.service.chatai.langchain4j.reliability.StreamTimeoutPolicy;
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
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private final ChatModelProviderRegistry providerRegistry;
    private final ModelFailureClassifier failureClassifier;
    private final AgentFallbackPolicy fallbackPolicy;
    private final StreamTimeoutPolicy timeoutPolicy;
    private final Map<AgentMode, CodeMateStreamingAssistant> assistants = new EnumMap<>(AgentMode.class);
    private final ConcurrentMap<String, AtomicBoolean> conversationBusy = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "codemate-model-timeout");
        thread.setDaemon(true);
        return thread;
    });

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
                                        MemoryContextAssembler memoryContextAssembler,
                                        ChatModelProviderRegistry providerRegistry,
                                        ModelFailureClassifier failureClassifier,
                                        AgentFallbackPolicy fallbackPolicy,
                                        StreamTimeoutPolicy timeoutPolicy) {
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
        this.providerRegistry = providerRegistry;
        this.failureClassifier = failureClassifier;
        this.fallbackPolicy = fallbackPolicy;
        this.timeoutPolicy = timeoutPolicy;
    }

    @PostConstruct
    public void initialize() {
        assistants.put(AgentMode.CHAT, buildAssistant(BASE_PROMPT, true, false, false));
        assistants.put(AgentMode.BUG_DIAGNOSIS,
                buildAssistant(modeRegistry.get(AgentMode.BUG_DIAGNOSIS).systemPrompt(), true, true, false));
        assistants.put(AgentMode.TASK_PLANNING,
                buildAssistant(modeRegistry.get(AgentMode.TASK_PLANNING).systemPrompt(), false, false, false));
        assistants.put(AgentMode.KNOWLEDGE_QA,
                buildAssistant(modeRegistry.get(AgentMode.KNOWLEDGE_QA).systemPrompt(), false, true, false));
    }

    public boolean isAvailable() {
        try {
            ChatModelProvider provider = providerRegistry.active();
            return properties.isEnabled() && provider.isAvailable() && provider.capabilities().isStreaming();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isFallbackEnabled() {
        return properties.isFallbackEnabled();
    }

    public String activeProviderName() {
        return providerRegistry.active().name();
    }

    public String activeModelName() {
        return providerRegistry.active().modelName();
    }

    public void stream(AgentMode mode,
                       Long userId,
                       Long runId,
                       String chatId,
                       String input,
                       List<ChatItemVo> history,
                       ChatItemVo current,
                       AgentStreamObserver observer) {
        ChatModelProvider provider = providerRegistry.requireAvailable();
        if (!provider.capabilities().isStreaming()) throw new IllegalStateException("Active provider does not support streaming");
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
            metrics.request(mode, provider.name());
            String systemPrompt = memoryContextAssembler.assemble(systemPrompt(mode), userId, memoryId);
            startAttempt(mode, memoryId, runId, systemPrompt, input, current, observer, busy, startedAt, provider.name());
        } catch (RuntimeException e) {
            metrics.error(mode, provider.name(), failureClassifier.classify(e));
            release(memoryId, busy);
            throw e;
        }
    }

    private void startAttempt(AgentMode mode, String memoryId, Long runId, String systemPrompt, String input,
                              ChatItemVo current, AgentStreamObserver observer, AtomicBoolean busy,
                              Instant requestStartedAt, String providerName) {
        CodeMateStreamingAssistant assistant = assistants.get(mode);
        if (assistant == null) throw new IllegalArgumentException("Unsupported fallback mode: " + mode);
        Instant attemptStartedAt = Instant.now();
        AtomicBoolean finished = new AtomicBoolean();
        AtomicBoolean firstToken = new AtomicBoolean();
        long remainingMillis = timeoutPolicy.remainingTotalMillis(requestStartedAt, attemptStartedAt);
        ScheduledFuture<?> firstTokenTimeout = timeoutExecutor.schedule(
                () -> failAttempt(mode, memoryId, runId, systemPrompt, input, current, observer, busy,
                        requestStartedAt, providerName, finished,
                        new StreamTimeoutException("FIRST_TOKEN_TIMEOUT")),
                timeoutPolicy.firstTokenTimeoutMillis(remainingMillis),
                TimeUnit.MILLISECONDS);
        ScheduledFuture<?> totalTimeout = timeoutExecutor.schedule(
                () -> failAttempt(mode, memoryId, runId, systemPrompt, input, current, observer, busy,
                        requestStartedAt, providerName, finished,
                        new StreamTimeoutException("TOTAL_RESPONSE_TIMEOUT")),
                remainingMillis, TimeUnit.MILLISECONDS);
        try {
            TokenStream stream = assistant.chat(memoryId, systemPrompt, input);
            stream.onPartialResponse(token -> {
                        if (finished.get()) return;
                        if (firstToken.compareAndSet(false, true)) {
                            firstTokenTimeout.cancel(false);
                            metrics.firstToken(mode, providerName, Duration.between(attemptStartedAt, Instant.now()));
                        }
                        observer.onToken(token);
                    })
                    .onRetrieved(contents -> {
                        if (finished.get()) return;
                        metrics.retrieved(mode, contents.size());
                        recordEvidence(runId, contents);
                        attachCitations(current, contents);
                        observer.onRetrieved(contents);
                    })
                    .onToolExecuted(execution -> {
                        if (finished.get()) return;
                        metrics.toolExecuted(mode, execution.hasFailed());
                        observer.onToolExecuted(execution);
                    })
                    .onCompleteResponse(response -> {
                        if (!finished.compareAndSet(false, true)) return;
                        firstTokenTimeout.cancel(false);
                        totalTimeout.cancel(false);
                        metrics.success(mode, providerName, Duration.between(attemptStartedAt, Instant.now()), response.tokenUsage());
                        release(memoryId, busy);
                        observer.onComplete(response);
                    })
                    .onError(error -> failAttempt(mode, memoryId, runId, systemPrompt, input, current, observer,
                            busy, requestStartedAt, providerName, finished, error))
                    .start();
        } catch (RuntimeException e) {
            failAttempt(mode, memoryId, runId, systemPrompt, input, current, observer, busy,
                    requestStartedAt, providerName, finished, e);
        }
    }

    private void failAttempt(AgentMode mode, String memoryId, Long runId, String systemPrompt, String input,
                             ChatItemVo current, AgentStreamObserver observer, AtomicBoolean busy,
                             Instant requestStartedAt, String providerName, AtomicBoolean finished, Throwable error) {
        if (!finished.compareAndSet(false, true)) return;
        ModelFailureType failureType = failureClassifier.classify(error);
        metrics.error(mode, providerName, failureType);
        AgentFallbackPolicy.Fallback fallback = properties.isFallbackEnabled()
                && failureType != ModelFailureType.BUSINESS ? fallbackPolicy.next(mode) : null;
        if (fallback != null) {
            metrics.fallback(mode, fallback.getMode(), providerName);
            observer.onFallback(fallback.getUserNotice());
            startAttempt(fallback.getMode(), memoryId, runId, systemPrompt(fallback.getMode()), input,
                    current, observer, busy, requestStartedAt, providerName);
            return;
        }
        release(memoryId, busy);
        observer.onError(error);
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
        if (tools && providerRegistry.active().capabilities().isToolCalling()) {
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

    @PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdownNow();
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
