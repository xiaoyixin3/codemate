package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SafeToolExecutorTest {
    @Test
    void timeoutIsConvertedToStableJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        LangChain4jProperties properties = new LangChain4jProperties();
        properties.setToolTimeoutMillis(10);
        SafeToolExecutor executor = new SafeToolExecutor(mapper, properties,
                new ToolMetrics(new SimpleMeterRegistry()), Executors.newSingleThreadExecutor());
        try {
            String json = executor.execute("slowTool", ToolRiskLevel.READ_ONLY, ToolAccess.PUBLIC, null, () -> {
                Thread.sleep(1000);
                return ToolResult.success("slowTool", ToolRiskLevel.READ_ONLY, Collections.emptyList(), "done");
            });
            JsonNode result = mapper.readTree(json);
            assertFalse(result.get("success").asBoolean());
            assertEquals("TIMEOUT", result.get("errorCode").asText());
        } finally {
            executor.shutdown();
        }
    }
}
