package com.github.paicoding.forum.service.taskplan.service;

import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepAddReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;

public interface TaskPlanWriteService {
    Long addStep(Long userId, Long runId, Long planId, AiTaskPlanStepAddReq req);

    void updateStepStatus(Long userId, Long runId, Long planId, Long stepId, AiTaskPlanStepStatusUpdateReq req);

    void recordStepResult(Long userId, Long runId, Long planId, Long stepId, AiTaskPlanStepResultUpdateReq req);
}
