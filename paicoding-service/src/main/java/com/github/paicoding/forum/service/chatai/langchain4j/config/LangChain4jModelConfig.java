package com.github.paicoding.forum.service.chatai.langchain4j.config;

import com.github.paicoding.forum.service.chatai.rag.config.RagProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jModelConfig {
    @Bean("codeMateChatModel")
    public ChatModel codeMateChatModel(LangChain4jProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(apiKeyOrPlaceholder(properties.getApiKey()))
                .modelName(properties.getChatModel())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .maxRetries(2)
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();
    }

    @Bean("codeMateStreamingChatModel")
    public StreamingChatModel codeMateStreamingChatModel(LangChain4jProperties properties) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(apiKeyOrPlaceholder(properties.getApiKey()))
                .modelName(properties.getChatModel())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .accumulateToolCallId(false)
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();
    }

    @Bean("codeMateEmbeddingModel")
    public EmbeddingModel codeMateEmbeddingModel(RagProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(properties.getApiHost())
                .apiKey(apiKeyOrPlaceholder(properties.getApiKey()))
                .modelName(properties.getEmbeddingModel())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .maxRetries(2)
                .build();
    }

    private String apiKeyOrPlaceholder(String apiKey) {
        return StringUtils.defaultIfBlank(apiKey, "not-configured");
    }
}
