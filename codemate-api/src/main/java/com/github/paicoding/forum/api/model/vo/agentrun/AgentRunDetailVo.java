package com.github.paicoding.forum.api.model.vo.agentrun;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentRunDetailVo extends AgentRunListVo {
    private Integer inputTokenCount;
    private Integer outputTokenCount;
    private Integer maxToolCalls;
    private Integer maxExecutionSeconds;
    private Integer maxTokenBudget;
    private String failureReason;
    private List<AgentRunStepVo> steps;
    private List<AgentRunEvidenceVo> evidence;
}
