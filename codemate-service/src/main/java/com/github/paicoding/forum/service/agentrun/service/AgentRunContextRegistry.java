package com.github.paicoding.forum.service.agentrun.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AgentRunContextRegistry {
    private final ConcurrentMap<String, Long> runs = new ConcurrentHashMap<>();

    public void bind(String memoryId, Long runId) {
        if (memoryId != null && runId != null) {
            runs.put(memoryId, runId);
        }
    }

    public Long findRunId(String memoryId) {
        return memoryId == null ? null : runs.get(memoryId);
    }

    public void unbind(String memoryId) {
        if (memoryId != null) {
            runs.remove(memoryId);
        }
    }
}
