package com.github.paicoding.forum.service.chatai.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagChunkerTest {
    private final RagChunker chunker = new RagChunker();

    @Test
    void shouldSplitLongContentAndKeepOverlap() {
        String content = repeat("Spring Boot Agent 可以拆解任务并检索知识。", 60);
        List<String> chunks = chunker.split(content, 240, 40);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 240));
        assertFalse(chunks.get(0).isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankContent() {
        assertEquals(0, chunker.split("  ", 1000, 150).size());
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
