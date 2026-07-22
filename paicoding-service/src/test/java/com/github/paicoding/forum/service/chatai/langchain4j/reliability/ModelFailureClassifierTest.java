package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.exception.ToolArgumentsException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelFailureClassifierTest {
    private final ModelFailureClassifier classifier = new ModelFailureClassifier();

    @Test
    void separatesRateLimitNetworkBusinessAndModelFailures() {
        assertEquals(ModelFailureType.RATE_LIMIT, classifier.classify(new RateLimitException("429")));
        assertEquals(ModelFailureType.RETRIABLE_NETWORK, classifier.classify(new TimeoutException("slow")));
        assertEquals(ModelFailureType.RETRIABLE_NETWORK,
                classifier.classify(new RuntimeException(new IOException("reset"))));
        assertEquals(ModelFailureType.BUSINESS, classifier.classify(new ToolArgumentsException("bad args")));
        assertEquals(ModelFailureType.MODEL, classifier.classify(new IllegalStateException("bad response")));
    }
}
