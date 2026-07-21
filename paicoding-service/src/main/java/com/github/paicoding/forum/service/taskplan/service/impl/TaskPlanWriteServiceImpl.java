package com.github.paicoding.forum.service.taskplan.service.impl;

import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepAddReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskWriteAuditDao;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskPlanDao;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskWriteAuditDO;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import com.github.paicoding.forum.service.taskplan.service.TaskPlanWriteService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class TaskPlanWriteServiceImpl implements TaskPlanWriteService {
    private final AiTaskPlanService taskPlanService;
    private final AiTaskWriteAuditDao auditDao;
    private final AiTaskPlanDao planDao;

    public TaskPlanWriteServiceImpl(AiTaskPlanService taskPlanService, AiTaskWriteAuditDao auditDao,
                                    AiTaskPlanDao planDao) {
        this.taskPlanService = taskPlanService;
        this.auditDao = auditDao;
        this.planDao = planDao;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStep(Long userId, Long runId, Long planId, AiTaskPlanStepAddReq req) {
        String key = key(req == null ? null : req.getIdempotencyKey());
        lockOwnedPlan(userId, planId);
        AiTaskWriteAuditDO existing = auditDao.find(userId, "addTaskPlanStep", key);
        if (existing != null) {
            return existing.getStepId();
        }
        Long stepId = taskPlanService.addStep(userId, planId, req);
        audit(userId, runId, planId, stepId, "addTaskPlanStep", key, "新增任务计划步骤");
        return stepId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStepStatus(Long userId, Long runId, Long planId, Long stepId,
                                 AiTaskPlanStepStatusUpdateReq req) {
        executeOnce(userId, runId, planId, stepId, "updateTaskStepStatus",
                req == null ? null : req.getIdempotencyKey(),
                () -> taskPlanService.updateStepStatus(userId, planId, stepId, req), "更新任务步骤状态");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordStepResult(Long userId, Long runId, Long planId, Long stepId,
                                 AiTaskPlanStepResultUpdateReq req) {
        executeOnce(userId, runId, planId, stepId, "recordTaskStepResult",
                req == null ? null : req.getIdempotencyKey(),
                () -> taskPlanService.saveStepResult(userId, planId, stepId, req), "记录任务步骤结果");
    }

    private void executeOnce(Long userId, Long runId, Long planId, Long stepId, String action,
                             String idempotencyKey, Runnable operation, String summary) {
        String key = key(idempotencyKey);
        lockOwnedPlan(userId, planId);
        if (auditDao.find(userId, action, key) != null) {
            return;
        }
        operation.run();
        audit(userId, runId, planId, stepId, action, key, summary);
    }

    private void lockOwnedPlan(Long userId, Long planId) {
        if (userId == null || userId <= 0 || planId == null || planId <= 0
                || planDao.getByIdAndUserIdForUpdate(planId, userId) == null) {
            throw ExceptionUtil.of(StatusEnum.RECORDS_NOT_EXISTS, "任务计划不存在或无权操作");
        }
    }

    private String key(String raw) {
        String value = StringUtils.trimToEmpty(raw);
        if (value.length() < 8 || value.length() > 64 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "幂等键格式非法");
        }
        return value;
    }

    private void audit(Long userId, Long runId, Long planId, Long stepId, String action,
                       String key, String summary) {
        AiTaskWriteAuditDO audit = new AiTaskWriteAuditDO();
        audit.setUserId(userId);
        audit.setRunId(runId);
        audit.setPlanId(planId);
        audit.setStepId(stepId);
        audit.setAction(action);
        audit.setIdempotencyKey(key);
        audit.setResultSummary(summary);
        Date now = new Date();
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        auditDao.save(audit);
    }
}
