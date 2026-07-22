package com.github.paicoding.forum.service.agentrun.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_evidence")
public class AiAgentEvidenceDO extends BaseDO {
    private Long runId;
    private Long userId;
    private String sourceType;
    private Long articleId;
    private Integer chunkIndex;
    private String title;
    private String evidenceSummary;
    private Double relevance;
}
