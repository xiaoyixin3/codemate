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
    private int memorySummaryMaxChars = 4000;
    private int memoryPreferenceMaxItems = 20;
    private int maxToolRoundTrips = 3;
    private long toolTimeoutMillis = 3000L;
    private int toolMaxOutputChars = 12000;
    private int toolMaxKeywordLength = 80;
    private int toolMaxArticleContentChars = 6000;
    private int maxRunToolCalls = 8;
    private int maxRunExecutionSeconds = 180;
    private int maxRunTokenBudget = 16000;
    private long runSweeperDelayMillis = 10000L;
    private boolean logRequests;
    private boolean logResponses;

    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
}
