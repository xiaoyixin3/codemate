package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

import java.util.Date;

/**
 * AI任务计划列表项
 */
@Data
public class AiTaskPlanListVo {
    private Long planId;
    private String title;
    private Integer status;
    private Integer progress;
    private Integer stepTotal;
    private Integer completedStepCount;
    private String chatId;
    private Date updateTime;
}
