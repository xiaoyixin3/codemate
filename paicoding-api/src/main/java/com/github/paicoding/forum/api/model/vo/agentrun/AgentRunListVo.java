package com.github.paicoding.forum.api.model.vo.agentrun;

import lombok.Data;

import java.util.Date;

@Data
public class AgentRunListVo {
    private Long runId;
    private String chatId;
    private String mode;
    private String goal;
    private String status;
    private String model;
    private Integer toolCallCount;
    private Integer totalTokenCount;
    private Date startTime;
    private Date endTime;
}
