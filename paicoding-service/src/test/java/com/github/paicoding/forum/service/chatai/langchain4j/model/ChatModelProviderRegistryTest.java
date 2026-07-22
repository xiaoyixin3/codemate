package com.github.paicoding.forum.service.chatai.langchain4j.model;

import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatModelProviderRegistryTest {
    @Test
    void switchesProviderFromConfiguration() {
        LangChain4jProperties properties = new LangChain4jProperties();
        properties.setProvider("openai-compatible");
        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(properties,
                Arrays.asList(provider("deepseek", true), provider("openai-compatible", true)));

        assertEquals("openai-compatible", registry.requireAvailable().name());
        assertEquals(128000, registry.active().capabilities().getContextWindowTokens());
    }

    @Test
    void rejectsUnknownOrUnavailableProvider() {
        LangChain4jProperties properties = new LangChain4jProperties();
        ChatModelProviderRegistry registry = new ChatModelProviderRegistry(properties,
                Arrays.asList(provider("deepseek", false)));
        assertThrows(IllegalStateException.class, registry::requireAvailable);

        properties.setProvider("missing");
        assertThrows(IllegalStateException.class, registry::active);
    }

    private ChatModelProvider provider(String name, boolean available) {
        return new ChatModelProvider() {
            @Override public String name() { return name; }
            @Override public String modelName() { return name + "-model"; }
            @Override public ChatModelCapabilities capabilities() {
                return new ChatModelCapabilities(true, true, true, 128000);
            }
            @Override public boolean isAvailable() { return available; }
            @Override public ChatModel chatModel() { return null; }
            @Override public StreamingChatModel streamingChatModel() { return null; }
        };
    }
}
