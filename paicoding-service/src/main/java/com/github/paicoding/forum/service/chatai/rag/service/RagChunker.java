package com.github.paicoding.forum.service.chatai.rag.service;

import com.github.paicoding.forum.service.chatai.rag.model.RagChunk;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Markdown-aware chunker that keeps headings, fenced code and tables together when practical. */
@Component
public class RagChunker {
    public List<String> split(String content, int chunkSize, int overlap) {
        return splitStructured(content, chunkSize, overlap).stream()
                .map(RagChunk::getContent).collect(Collectors.toList());
    }

    public List<RagChunk> splitStructured(String content, int chunkSize, int overlap) {
        String normalized = StringUtils.defaultString(content).replace("\r\n", "\n").replace('\r', '\n').trim();
        if (StringUtils.isBlank(normalized)) {
            return Collections.emptyList();
        }
        int safeSize = Math.max(200, chunkSize);
        int safeOverlap = Math.max(0, Math.min(overlap, safeSize / 3));
        List<Block> blocks = parseBlocks(normalized);
        List<RagChunk> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String heading = "正文";
        String type = "TEXT";
        for (Block block : blocks) {
            if (buffer.length() > 0 && !heading.equals(block.heading)) {
                addChunk(chunks, heading, type, buffer.toString());
                buffer.setLength(0);
                type = "TEXT";
            }
            if (buffer.length() > 0 && buffer.length() + block.text.length() + 2 > safeSize) {
                addChunk(chunks, heading, type, buffer.toString());
                String tail = overlapTail(buffer.toString(), safeOverlap);
                buffer.setLength(0);
                if (!block.atomic && StringUtils.isNotBlank(tail)) {
                    buffer.append(tail).append("\n\n");
                }
                type = "TEXT";
            }
            heading = block.heading;
            type = mergeType(type, block.type);
            if (block.text.length() > safeSize && !block.atomic) {
                if (buffer.length() > 0) {
                    addChunk(chunks, heading, type, buffer.toString());
                    buffer.setLength(0);
                }
                splitLongText(chunks, block, safeSize, safeOverlap);
                type = "TEXT";
            } else {
                if (buffer.length() > 0) buffer.append("\n\n");
                buffer.append(block.text);
            }
        }
        if (buffer.length() > 0) addChunk(chunks, heading, type, buffer.toString());
        return chunks;
    }

    private List<Block> parseBlocks(String text) {
        List<Block> blocks = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        String heading = "正文";
        StringBuilder paragraph = new StringBuilder();
        for (int i = 0; i < lines.length;) {
            String line = lines[i];
            if (line.matches("^#{1,6}\\s+.*")) {
                flushParagraph(blocks, paragraph, heading);
                heading = line.replaceFirst("^#{1,6}\\s+", "").trim();
                blocks.add(new Block(heading, "HEADING", line, false));
                i++;
            } else if (line.trim().startsWith("```")) {
                flushParagraph(blocks, paragraph, heading);
                StringBuilder code = new StringBuilder(line);
                i++;
                while (i < lines.length) {
                    code.append('\n').append(lines[i]);
                    if (lines[i].trim().startsWith("```")) { i++; break; }
                    i++;
                }
                blocks.add(new Block(heading, "CODE", code.toString(), true));
            } else if (isTableLine(line)) {
                flushParagraph(blocks, paragraph, heading);
                StringBuilder table = new StringBuilder(line);
                i++;
                while (i < lines.length && isTableLine(lines[i])) table.append('\n').append(lines[i++]);
                blocks.add(new Block(heading, "TABLE", table.toString(), true));
            } else if (StringUtils.isBlank(line)) {
                flushParagraph(blocks, paragraph, heading);
                i++;
            } else {
                if (paragraph.length() > 0) paragraph.append('\n');
                paragraph.append(line);
                i++;
            }
        }
        flushParagraph(blocks, paragraph, heading);
        return blocks;
    }

    private void splitLongText(List<RagChunk> chunks, Block block, int size, int overlap) {
        int start = 0;
        while (start < block.text.length()) {
            int end = Math.min(block.text.length(), start + size);
            if (end < block.text.length()) end = findBoundary(block.text, start + size / 2, end);
            addChunk(chunks, block.heading, block.type, block.text.substring(start, end));
            if (end >= block.text.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
    }

    private void flushParagraph(List<Block> blocks, StringBuilder paragraph, String heading) {
        if (paragraph.length() > 0) {
            blocks.add(new Block(heading, "TEXT", paragraph.toString().trim(), false));
            paragraph.setLength(0);
        }
    }

    private boolean isTableLine(String line) {
        String value = line.trim();
        return value.startsWith("|") && value.endsWith("|") && value.length() > 2;
    }

    private String mergeType(String current, String incoming) {
        if (current == null || "TEXT".equals(current)) return incoming;
        return current.equals(incoming) ? current : "MIXED";
    }

    private String overlapTail(String value, int overlap) {
        if (overlap <= 0 || value.length() <= overlap) return "";
        return value.substring(value.length() - overlap).trim();
    }

    private void addChunk(List<RagChunk> chunks, String heading, String type, String content) {
        String value = StringUtils.trimToEmpty(content);
        if (StringUtils.isNotBlank(value)) chunks.add(new RagChunk(StringUtils.defaultIfBlank(heading, "正文"), type, value));
    }

    private int findBoundary(String text, int min, int end) {
        for (int i = end - 1; i >= min; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') return i + 1;
        }
        return end;
    }

    private static final class Block {
        private final String heading;
        private final String type;
        private final String text;
        private final boolean atomic;

        private Block(String heading, String type, String text, boolean atomic) {
            this.heading = heading;
            this.type = type;
            this.text = text;
            this.atomic = atomic;
        }
    }
}
