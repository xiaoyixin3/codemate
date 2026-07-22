package com.github.paicoding.forum.service.agentrun.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_step")
public class AiAgentStepDO extends BaseDO {
    private Long runId;
    private Long userId;
    private Integer stepNo;
    private String type;
    private String toolName;
    private String argumentSummary;
    private String resultSummary;
    private String callFingerprint;
    private String status;
    private Long durationMs;
    private String errorType;
}
