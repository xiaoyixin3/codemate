package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

@Data
public class AiTaskPlanStepAddReq extends AiTaskPlanStepCreateReq {
    private String idempotencyKey;
}
