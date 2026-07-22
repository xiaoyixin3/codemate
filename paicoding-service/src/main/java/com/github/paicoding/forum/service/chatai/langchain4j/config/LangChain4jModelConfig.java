package com.github.paicoding.forum.service.chatai.langchain4j.config;

import com.github.paicoding.forum.service.chatai.rag.config.RagProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.model.ChatModelProvider;
import com.github.paicoding.forum.service.chatai.langchain4j.model.ChatModelProviderRegistry;
import com.github.paicoding.forum.service.chatai.langchain4j.model.OpenAiCompatibleChatModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jModelConfig {
    @Bean("codeMateChatModel")
    public ChatModel codeMateChatModel(ChatModelProviderRegistry registry) {
        return registry.active().chatModel();
    }

    @Bean("codeMateStreamingChatModel")
    public StreamingChatModel codeMateStreamingChatModel(ChatModelProviderRegistry registry) {
        return registry.active().streamingChatModel();
    }

    @Bean
    public ChatModelProvider deepSeekChatModelProvider(LangChain4jProperties properties) {
        return new OpenAiCompatibleChatModelProvider("deepseek", properties.getBaseUrl(), properties.getApiKey(),
                properties.getChatModel(), properties.getTemperature(), properties.getMaxRunTokenBudget(),
                properties.getTimeoutSeconds(), properties.getContextWindowTokens(), properties.isLogRequests(),
                properties.isLogResponses());
    }

    @Bean
    public ChatModelProvider openAiCompatibleChatModelProvider(LangChain4jProperties properties) {
        return new OpenAiCompatibleChatModelProvider("openai-compatible", properties.getOpenAiBaseUrl(),
                properties.getOpenAiApiKey(), properties.getOpenAiChatModel(), properties.getTemperature(),
                properties.getMaxRunTokenBudget(), properties.getTimeoutSeconds(),
                properties.getOpenAiContextWindowTokens(), properties.isLogRequests(), properties.isLogResponses());
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
