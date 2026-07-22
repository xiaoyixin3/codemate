package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class StreamTimeoutPolicy {
    private final LangChain4jProperties properties;

    public StreamTimeoutPolicy(LangChain4jProperties properties) {
        this.properties = properties;
    }

    public long remainingTotalMillis(Instant requestStartedAt, Instant now) {
        long budget = TimeUnit.SECONDS.toMillis(Math.max(1, properties.getTotalResponseTimeoutSeconds()));
        return Math.max(1L, budget - Math.max(0L, Duration.between(requestStartedAt, now).toMillis()));
    }

    public long firstTokenTimeoutMillis(long remainingTotalMillis) {
        long firstToken = TimeUnit.SECONDS.toMillis(Math.max(1, properties.getFirstTokenTimeoutSeconds()));
        return Math.max(1L, Math.min(remainingTotalMillis, firstToken));
    }
}
