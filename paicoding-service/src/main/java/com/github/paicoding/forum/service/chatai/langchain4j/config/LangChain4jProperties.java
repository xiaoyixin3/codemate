package com.github.paicoding.forum.service.chatai.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "codemate.langchain4j")
public class LangChain4jProperties {
    private boolean enabled = true;
    private boolean fallbackEnabled = true;
    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey;
    private String chatModel = "deepseek-chat";
    private double temperature = 0.2D;
    private int timeoutSeconds = 120;
    private int memoryMaxMessages = 20;
    private int maxToolRoundTrips = 3;
    private boolean logRequests;
    private boolean logResponses;

    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
}
