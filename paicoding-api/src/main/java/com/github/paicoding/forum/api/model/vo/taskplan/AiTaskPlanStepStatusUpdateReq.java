package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

/**
 * 更新任务计划步骤状态请求
 */
@Data
public class AiTaskPlanStepStatusUpdateReq {
    private Integer status;
    private String blockedReason;
    private String skippedReason;
    private String idempotencyKey;
}
