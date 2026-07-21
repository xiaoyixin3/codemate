package com.github.paicoding.forum.service.taskplan.service;

import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanDetailVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanListVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanReopenReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanUpdateReq;

import java.util.List;

public interface AiTaskPlanService {
    Long createPlan(Long userId, AiTaskPlanCreateReq req);

    List<AiTaskPlanListVo> listPlans(Long userId);

    AiTaskPlanDetailVo queryPlanDetail(Long userId, Long planId);

    void updatePlan(Long userId, Long planId, AiTaskPlanUpdateReq req);

    void deletePlan(Long userId, Long planId);

    void startPlan(Long userId, Long planId);

    void cancelPlan(Long userId, Long planId);

    void updateStepStatus(Long userId, Long planId, Long stepId, AiTaskPlanStepStatusUpdateReq req);

    void saveStepResult(Long userId, Long planId, Long stepId, AiTaskPlanStepResultUpdateReq req);

    Long addStep(Long userId, Long planId, AiTaskPlanStepCreateReq req);

    void reopenPlan(Long userId, Long planId, AiTaskPlanReopenReq req);
}
