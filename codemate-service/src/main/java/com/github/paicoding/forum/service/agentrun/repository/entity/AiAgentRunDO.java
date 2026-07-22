package com.github.paicoding.forum.service.agentrun.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_run")
public class AiAgentRunDO extends BaseDO {
    private Long userId;
    private String chatId;
    private String mode;
    private String goal;
    private String status;
    private String model;
    private Date startTime;
    private Date endTime;
    private Integer inputTokenCount;
    private Integer outputTokenCount;
    private Integer totalTokenCount;
    private Integer toolCallCount;
    private Integer maxToolCalls;
    private Integer maxExecutionSeconds;
    private Integer maxTokenBudget;
    private String failureReason;
}
