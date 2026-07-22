package com.github.paicoding.forum.service.chatai.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "codemate.rag")
public class RagProperties {
    private boolean enabled;
    private String apiHost = "https://api.openai.com/v1";
    private String apiKey;
    private String embeddingModel = "text-embedding-3-small";
    private int chunkSize = 1000;
    private int chunkOverlap = 150;
    private int topK = 5;
    private int maxCandidateChunks = 2000;
    private double minScore = 0.45D;
    private long timeoutSeconds = 60L;
    private String indexVersion = "v2-hybrid";
    private int keywordCandidateChunks = 200;
    private int vectorCandidateChunks = 1000;
    private double vectorWeight = 0.65D;
    private double keywordWeight = 0.25D;
    private double freshnessWeight = 0.10D;
    private int debugMaxResults = 20;
}
