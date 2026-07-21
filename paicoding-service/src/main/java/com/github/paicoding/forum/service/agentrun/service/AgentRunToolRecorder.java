package com.github.paicoding.forum.service.agentrun.service;

import org.springframework.stereotype.Component;

@Component
public class AgentRunToolRecorder {
    private final AgentRunContextRegistry contextRegistry;
    private final AgentRunService agentRunService;

    public AgentRunToolRecorder(AgentRunContextRegistry contextRegistry, AgentRunService agentRunService) {
        this.contextRegistry = contextRegistry;
        this.agentRunService = agentRunService;
    }

    public Long begin(String memoryId, String toolName, String canonicalArguments) {
        Long runId = contextRegistry.findRunId(memoryId);
        return runId == null ? null : agentRunService.beginToolCall(runId, toolName, canonicalArguments);
    }

    public void finish(Long stepId, boolean success, String summary, String errorType, long durationMs) {
        agentRunService.finishToolCall(stepId, success, summary, errorType, durationMs);
    }
}
