package com.github.paicoding.forum.web.front.taskplan.rest;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanDetailVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanListVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanReopenReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanUpdateReq;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * AI任务计划接口
 */
@RestController
@Permission(role = UserRole.LOGIN)
@RequestMapping("/task-plan/api")
public class AiTaskPlanRestController {
    @Resource
    private AiTaskPlanService aiTaskPlanService;

    @PostMapping
    public ResVo<Long> create(@RequestBody AiTaskPlanCreateReq req) {
        return ResVo.ok(aiTaskPlanService.createPlan(currentUserId(), req));
    }

    @GetMapping
    public ResVo<List<AiTaskPlanListVo>> list() {
        return ResVo.ok(aiTaskPlanService.listPlans(currentUserId()));
    }

    @GetMapping("/{planId}")
    public ResVo<AiTaskPlanDetailVo> detail(@PathVariable Long planId) {
        return ResVo.ok(aiTaskPlanService.queryPlanDetail(currentUserId(), planId));
    }

    @PutMapping("/{planId}")
    public ResVo<Boolean> update(@PathVariable Long planId, @RequestBody AiTaskPlanUpdateReq req) {
        aiTaskPlanService.updatePlan(currentUserId(), planId, req);
        return ResVo.ok(true);
    }

    @DeleteMapping("/{planId}")
    public ResVo<Boolean> delete(@PathVariable Long planId) {
        aiTaskPlanService.deletePlan(currentUserId(), planId);
        return ResVo.ok(true);
    }

    @PutMapping("/{planId}/start")
    public ResVo<Boolean> start(@PathVariable Long planId) {
        aiTaskPlanService.startPlan(currentUserId(), planId);
        return ResVo.ok(true);
    }

    @PutMapping("/{planId}/cancel")
    public ResVo<Boolean> cancel(@PathVariable Long planId) {
        aiTaskPlanService.cancelPlan(currentUserId(), planId);
        return ResVo.ok(true);
    }

    @PutMapping("/{planId}/steps/{stepId}/status")
    public ResVo<Boolean> updateStepStatus(@PathVariable Long planId,
                                            @PathVariable Long stepId,
                                            @RequestBody AiTaskPlanStepStatusUpdateReq req) {
        aiTaskPlanService.updateStepStatus(currentUserId(), planId, stepId, req);
        return ResVo.ok(true);
    }

    @PutMapping("/{planId}/steps/{stepId}/result")
    public ResVo<Boolean> saveStepResult(@PathVariable Long planId,
                                         @PathVariable Long stepId,
                                         @RequestBody AiTaskPlanStepResultUpdateReq req) {
        aiTaskPlanService.saveStepResult(currentUserId(), planId, stepId, req);
        return ResVo.ok(true);
    }

    @PutMapping("/{planId}/reopen")
    public ResVo<Boolean> reopen(@PathVariable Long planId, @RequestBody AiTaskPlanReopenReq req) {
        aiTaskPlanService.reopenPlan(currentUserId(), planId, req);
        return ResVo.ok(true);
    }

    private Long currentUserId() {
        return ReqInfoContext.getReqInfo().getUserId();
    }
}
