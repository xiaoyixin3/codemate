package com.github.paicoding.forum.service.chatai.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BugDiagnosisAgentModeHandlerTest {
    private final BugDiagnosisAgentModeHandler handler = new BugDiagnosisAgentModeHandler();

    @Test
    void sanitizesSecretsBeforeModelAndHistoryUse() {
        String input = "password=super-secret token:abc123456789 Bearer abcdefghijkl";

        String normalized = handler.validateAndNormalize(input);

        assertFalse(normalized.contains("super-secret"));
        assertFalse(normalized.contains("abc123456789"));
        assertFalse(normalized.contains("abcdefghijkl"));
        assertTrue(normalized.contains("[REDACTED]"));
    }

    @Test
    void truncatesLongLogsAndKeepsModeMetadata() {
        String normalized = handler.validateAndNormalize(repeat('x', 16_001));

        assertTrue(normalized.contains("已截断"));
        assertEquals(AgentMode.BUG_DIAGNOSIS, handler.mode());
        assertTrue(handler.requiresStructuredOutput());
    }

    @Test
    void rejectsEmptyDiagnosisInput() {
        assertThrows(IllegalArgumentException.class, () -> handler.validateAndNormalize("  "));
    }

    private String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
