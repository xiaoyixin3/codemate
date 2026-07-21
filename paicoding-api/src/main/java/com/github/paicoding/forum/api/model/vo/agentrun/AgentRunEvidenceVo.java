package com.github.paicoding.forum.api.model.vo.agentrun;

import lombok.Data;

@Data
public class AgentRunEvidenceVo {
    private Long evidenceId;
    private String sourceType;
    private Long articleId;
    private Integer chunkIndex;
    private String title;
    private String evidenceSummary;
    private Double relevance;
}
