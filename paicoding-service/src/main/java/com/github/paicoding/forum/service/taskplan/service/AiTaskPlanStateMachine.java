package com.github.paicoding.forum.service.taskplan.service;

import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStepStatusEnum;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanStepDO;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

/** 任务步骤及计划状态的唯一业务规则入口。 */
public final class AiTaskPlanStateMachine {
    private AiTaskPlanStateMachine() { }
    public static void validateStepTransition(AiTaskPlanStepStatusEnum current, AiTaskPlanStepStatusEnum target, boolean allowReopenCompleted) {
        if (current == null || target == null || current == target) reject("步骤状态未变化或状态非法");
        boolean allowed = (current == AiTaskPlanStepStatusEnum.TODO && (target == AiTaskPlanStepStatusEnum.IN_PROGRESS || target == AiTaskPlanStepStatusEnum.SKIPPED))
                || (current == AiTaskPlanStepStatusEnum.IN_PROGRESS && (target == AiTaskPlanStepStatusEnum.COMPLETED || target == AiTaskPlanStepStatusEnum.BLOCKED))
                || (current == AiTaskPlanStepStatusEnum.BLOCKED && (target == AiTaskPlanStepStatusEnum.IN_PROGRESS || target == AiTaskPlanStepStatusEnum.SKIPPED))
                || (current == AiTaskPlanStepStatusEnum.COMPLETED && target == AiTaskPlanStepStatusEnum.IN_PROGRESS && allowReopenCompleted);
        if (!allowed) reject("不允许从 " + current.name() + " 变更为 " + target.name());
    }
    public static void validateRequiredReason(AiTaskPlanStepStatusEnum target, String blockedReason, String skippedReason) {
        if (target == AiTaskPlanStepStatusEnum.BLOCKED && StringUtils.isBlank(blockedReason)) reject("步骤进入 BLOCKED 必须填写阻塞原因");
        if (target == AiTaskPlanStepStatusEnum.SKIPPED && StringUtils.isBlank(skippedReason)) reject("步骤进入 SKIPPED 必须填写跳过原因");
    }
    public static void assertNoOtherInProgress(List<AiTaskPlanStepDO> steps, Long stepId, AiTaskPlanStepStatusEnum target) {
        if (target != AiTaskPlanStepStatusEnum.IN_PROGRESS) return;
        for (AiTaskPlanStepDO item : steps) if (!item.getId().equals(stepId) && AiTaskPlanStepStatusEnum.IN_PROGRESS.getCode().equals(item.getStatus())) reject("当前计划已有执行中的步骤，同一时间只能执行一个步骤");
    }
    public static AiTaskPlanStatusEnum derivePlanStatus(List<AiTaskPlanStepDO> steps) {
        if (steps == null || steps.isEmpty()) return AiTaskPlanStatusEnum.TODO;
        boolean allTerminal = true, hasInProgressOrBlocked = false, hasStarted = false;
        for (AiTaskPlanStepDO step : steps) {
            AiTaskPlanStepStatusEnum status = AiTaskPlanStepStatusEnum.fromCode(step.getStatus());
            if (status == null) reject("步骤存在未知状态，无法计算计划进度");
            allTerminal &= status == AiTaskPlanStepStatusEnum.COMPLETED || status == AiTaskPlanStepStatusEnum.SKIPPED;
            hasInProgressOrBlocked |= status == AiTaskPlanStepStatusEnum.IN_PROGRESS || status == AiTaskPlanStepStatusEnum.BLOCKED;
            hasStarted |= status != AiTaskPlanStepStatusEnum.TODO;
        }
        if (allTerminal) return AiTaskPlanStatusEnum.COMPLETED;
        if (hasInProgressOrBlocked || hasStarted) return AiTaskPlanStatusEnum.IN_PROGRESS;
        return AiTaskPlanStatusEnum.TODO;
    }
    private static void reject(String message) { throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, message); }
}
