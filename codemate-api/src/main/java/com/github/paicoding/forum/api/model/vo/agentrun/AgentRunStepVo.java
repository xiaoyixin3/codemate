package com.github.paicoding.forum.api.model.vo.agentrun;

import lombok.Data;

import java.util.Date;

@Data
public class AgentRunStepVo {
    private Long stepId;
    private Integer stepNo;
    private String type;
    private String toolName;
    private String argumentSummary;
    private String resultSummary;
    private String status;
    private Long durationMs;
    private String errorType;
    private Date createTime;
}
