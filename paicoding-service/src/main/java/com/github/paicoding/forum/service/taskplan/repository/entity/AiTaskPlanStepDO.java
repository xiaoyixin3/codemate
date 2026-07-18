package com.github.paicoding.forum.service.taskplan.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * AI任务计划步骤
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task_plan_step")
public class AiTaskPlanStepDO extends BaseDO {
    private Long planId;
    private Long userId;
    private Integer stepNo;
    private String title;
    private String content;
    private Integer status;
    private String expectedOutput;
    private String actualOutput;
    private String risk;
    private String verificationMethod;
    private String blockedReason;
    private String skippedReason;
    private Integer executorType;
    private String agentConfig;
    private Date startedTime;
    private Date completedTime;
    private String extra;
    private Integer deleted;
}
