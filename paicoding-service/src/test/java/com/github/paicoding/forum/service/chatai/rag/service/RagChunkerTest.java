package com.github.paicoding.forum.service.chatai.rag.service;

import com.github.paicoding.forum.service.chatai.rag.model.RagChunk;
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

    @Test
    void shouldKeepHeadingCodeBlockAndTableMetadata() {
        String markdown = "# Redis 排查\n说明段落。\n\n```java\nString key = redis.get(\"key\");\n```\n\n"
                + "| 指标 | 值 |\n| --- | --- |\n| hit | 90% |";
        List<RagChunk> chunks = chunker.splitStructured(markdown, 240, 20);

        assertTrue(chunks.stream().anyMatch(chunk -> "Redis 排查".equals(chunk.getHeading())));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("```java")
                && chunk.getContent().contains("```")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("| hit | 90% |")));
    }

    @Test
    void shouldNotMixDifferentHeadingSectionsIntoOneChunk() {
        List<RagChunk> chunks = chunker.splitStructured("# 第一节\n第一节内容\n\n# 第二节\n第二节内容", 1000, 100);

        assertEquals(2, chunks.size());
        assertEquals("第一节", chunks.get(0).getHeading());
        assertEquals("第二节", chunks.get(1).getHeading());
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
