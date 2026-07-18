package com.github.paicoding.forum.service.chatai.rag.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RagChunker {
    public List<String> split(String content, int chunkSize, int overlap) {
        String normalized = StringUtils.defaultString(content).replace("\r\n", "\n").replace('\r', '\n').trim();
        if (StringUtils.isBlank(normalized)) {
            return Collections.emptyList();
        }
        int safeSize = Math.max(200, chunkSize);
        int safeOverlap = Math.max(0, Math.min(overlap, safeSize / 3));
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + safeSize);
            if (end < normalized.length()) {
                end = findBoundary(normalized, start + safeSize / 2, end);
            }
            String chunk = normalized.substring(start, end).trim();
            if (StringUtils.isNotBlank(chunk)) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(start + 1, end - safeOverlap);
        }
        return chunks;
    }

    private int findBoundary(String text, int min, int end) {
        for (int i = end - 1; i >= min; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        return end;
    }
}
