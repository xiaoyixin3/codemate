package com.github.paicoding.forum.service.chatai.langchain4j.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/** Builds any OpenAI-compatible endpoint without leaking provider details into Agent services. */
public class OpenAiCompatibleChatModelProvider implements ChatModelProvider {
    private final String name;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final ChatModelCapabilities capabilities;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public OpenAiCompatibleChatModelProvider(String name, String baseUrl, String apiKey, String modelName,
                                              double temperature, int maxTokens, int timeoutSeconds,
                                              int contextWindowTokens, boolean logRequests,
                                              boolean logResponses) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.capabilities = new ChatModelCapabilities(true, true, true, contextWindowTokens);
        int boundedMaxTokens = Math.max(1, Math.min(maxTokens, contextWindowTokens));
        String safeKey = StringUtils.defaultIfBlank(apiKey, "not-configured");
        Duration timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl).apiKey(safeKey).modelName(modelName).temperature(temperature)
                .maxTokens(boundedMaxTokens).timeout(timeout).maxRetries(2)
                .logRequests(logRequests).logResponses(logResponses).build();
        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl).apiKey(safeKey).modelName(modelName).temperature(temperature)
                .maxTokens(boundedMaxTokens).timeout(timeout).accumulateToolCallId(false)
                .logRequests(logRequests).logResponses(logResponses).build();
    }

    @Override public String name() { return name; }
    @Override public String modelName() { return modelName; }
    @Override public ChatModelCapabilities capabilities() { return capabilities; }
    @Override public boolean isAvailable() {
        return StringUtils.isNotBlank(baseUrl) && StringUtils.isNotBlank(apiKey) && StringUtils.isNotBlank(modelName);
    }
    @Override public ChatModel chatModel() { return chatModel; }
    @Override public StreamingChatModel streamingChatModel() { return streamingChatModel; }
}
