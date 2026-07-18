package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

import java.util.Date;

/**
 * AI任务计划步骤详情
 */
@Data
public class AiTaskPlanStepVo {
    private Long stepId;
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
    private Date startedTime;
    private Date completedTime;
}
