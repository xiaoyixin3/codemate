package com.github.paicoding.forum.service.agentrun.service;

import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunListVo;

import java.util.List;

public interface AgentRunService {
    Long create(Long userId, String chatId, String mode, String goal, String model);

    boolean transition(Long runId, AgentRunStatusEnum target);

    void complete(Long runId, Integer inputTokens, Integer outputTokens, Integer totalTokens);

    void waitForConfirmation(Long runId, Integer inputTokens, Integer outputTokens, Integer totalTokens);

    void fail(Long runId, String reason);

    void cancel(Long userId, Long runId);

    Long beginToolCall(Long runId, String toolName, String canonicalArguments);

    void finishToolCall(Long stepId, boolean success, String resultSummary, String errorType, long durationMs);

    void recordEvidence(Long runId, Long articleId, Integer chunkIndex, String title,
                        String summary, Double relevance);

    List<AgentRunListVo> listRuns(Long userId, int limit);

    AgentRunDetailVo detail(Long userId, Long runId);
}
