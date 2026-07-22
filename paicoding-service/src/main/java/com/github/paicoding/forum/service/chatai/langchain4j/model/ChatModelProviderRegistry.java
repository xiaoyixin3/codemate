package com.github.paicoding.forum.service.chatai.langchain4j.model;

import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ChatModelProviderRegistry {
    private final LangChain4jProperties properties;
    private final Map<String, ChatModelProvider> providers = new LinkedHashMap<>();

    public ChatModelProviderRegistry(LangChain4jProperties properties, List<ChatModelProvider> providers) {
        this.properties = properties;
        for (ChatModelProvider provider : providers) {
            this.providers.put(normalize(provider.name()), provider);
        }
    }

    public ChatModelProvider active() {
        String requested = normalize(properties.getProvider());
        ChatModelProvider provider = providers.get(requested);
        if (provider == null) {
            throw new IllegalStateException("Unknown CodeMate model provider: " + requested);
        }
        return provider;
    }

    public ChatModelProvider requireAvailable() {
        ChatModelProvider provider = active();
        if (!properties.isEnabled() || !provider.isAvailable()) {
            throw new IllegalStateException("CodeMate model provider is disabled or missing credentials: " + provider.name());
        }
        return provider;
    }

    private String normalize(String value) {
        return StringUtils.defaultIfBlank(value, "deepseek").trim().toLowerCase(Locale.ROOT);
    }
}
