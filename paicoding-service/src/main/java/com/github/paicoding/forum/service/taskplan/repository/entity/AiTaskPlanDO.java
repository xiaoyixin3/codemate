package com.github.paicoding.forum.service.taskplan.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * AI任务计划
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task_plan")
public class AiTaskPlanDO extends BaseDO {
    private Long userId;
    private String chatId;
    private Integer sourceAiType;
    private String planKey;
    private String title;
    private String goal;
    private Integer status;
    private Integer progress;
    private Integer stepTotal;
    private Integer completedStepCount;
    private String agentType;
    private String agentConfig;
    private Date lastExecuteTime;
    private String reopenReason;
    private Date reopenTime;
    private String extra;
    private Integer deleted;
}
