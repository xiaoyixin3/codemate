package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamTimeoutPolicyTest {
    @Test
    void enforcesFirstTokenAndEndToEndBudgetsAcrossFallbackAttempts() {
        LangChain4jProperties properties = new LangChain4jProperties();
        properties.setFirstTokenTimeoutSeconds(5);
        properties.setTotalResponseTimeoutSeconds(12);
        StreamTimeoutPolicy policy = new StreamTimeoutPolicy(properties);
        Instant started = Instant.parse("2026-07-22T00:00:00Z");

        assertEquals(5000L, policy.firstTokenTimeoutMillis(policy.remainingTotalMillis(started, started)));
        assertEquals(3000L, policy.remainingTotalMillis(started, started.plusSeconds(9)));
        assertEquals(3000L, policy.firstTokenTimeoutMillis(3000L));
        assertEquals(1L, policy.remainingTotalMillis(started, started.plusSeconds(30)));
    }
}
