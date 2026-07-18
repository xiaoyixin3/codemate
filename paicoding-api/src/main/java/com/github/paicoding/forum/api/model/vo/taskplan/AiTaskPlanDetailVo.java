package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * AI任务计划详情
 */
@Data
public class AiTaskPlanDetailVo {
    private Long planId;
    private String title;
    private String goal;
    /** 由任务规划模型生成并持久化在 extra 中的补充信息 */
    private String scope;
    private String summary;
    private List<String> risks;
    private List<String> acceptanceCriteria;
    private String chatId;
    private Integer sourceAiType;
    private Integer status;
    private Integer progress;
    private Integer stepTotal;
    private Integer completedStepCount;
    private String reopenReason;
    private Date reopenTime;
    private Date createTime;
    private Date updateTime;
    private List<AiTaskPlanStepVo> steps;
}
