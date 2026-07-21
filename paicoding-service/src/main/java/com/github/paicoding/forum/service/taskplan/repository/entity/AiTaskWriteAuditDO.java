package com.github.paicoding.forum.service.taskplan.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task_write_audit")
public class AiTaskWriteAuditDO extends BaseDO {
    private Long userId;
    private Long runId;
    private Long planId;
    private Long stepId;
    private String action;
    private String idempotencyKey;
    private String resultSummary;
}
