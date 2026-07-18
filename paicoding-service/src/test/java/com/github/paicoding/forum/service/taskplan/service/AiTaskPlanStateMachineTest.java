package com.github.paicoding.forum.service.taskplan.service;

import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStepStatusEnum;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanStepDO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiTaskPlanStateMachineTest {
    @Test
    void todoCanStartOrSkipButSkipNeedsReason() {
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.TODO, AiTaskPlanStepStatusEnum.IN_PROGRESS, false));
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.TODO, AiTaskPlanStepStatusEnum.SKIPPED, false));
        assertThrows(RuntimeException.class, () -> AiTaskPlanStateMachine.validateRequiredReason(AiTaskPlanStepStatusEnum.SKIPPED, null, ""));
    }

    @Test
    void inProgressCanCompleteOrBlockAndBlockNeedsReason() {
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.IN_PROGRESS, AiTaskPlanStepStatusEnum.COMPLETED, false));
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.IN_PROGRESS, AiTaskPlanStepStatusEnum.BLOCKED, false));
        assertThrows(RuntimeException.class, () -> AiTaskPlanStateMachine.validateRequiredReason(AiTaskPlanStepStatusEnum.BLOCKED, "", null));
    }

    @Test
    void blockedCanResumeOrSkip() {
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.BLOCKED, AiTaskPlanStepStatusEnum.IN_PROGRESS, false));
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.BLOCKED, AiTaskPlanStepStatusEnum.SKIPPED, false));
    }

    @Test
    void completedCanOnlyReopenWhenConfigured() {
        assertThrows(RuntimeException.class, () -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.COMPLETED, AiTaskPlanStepStatusEnum.IN_PROGRESS, false));
        assertDoesNotThrow(() -> AiTaskPlanStateMachine.validateStepTransition(AiTaskPlanStepStatusEnum.COMPLETED, AiTaskPlanStepStatusEnum.IN_PROGRESS, true));
    }

    @Test
    void onlyOneStepCanBeInProgress() {
        assertThrows(RuntimeException.class, () -> AiTaskPlanStateMachine.assertNoOtherInProgress(Arrays.asList(step(1L, AiTaskPlanStepStatusEnum.IN_PROGRESS), step(2L, AiTaskPlanStepStatusEnum.TODO)), 2L, AiTaskPlanStepStatusEnum.IN_PROGRESS));
    }

    @Test
    void derivesPlanStatusFromStepStates() {
        assertEquals(AiTaskPlanStatusEnum.TODO, AiTaskPlanStateMachine.derivePlanStatus(Arrays.asList(step(1L, AiTaskPlanStepStatusEnum.TODO))));
        assertEquals(AiTaskPlanStatusEnum.IN_PROGRESS, AiTaskPlanStateMachine.derivePlanStatus(Arrays.asList(step(1L, AiTaskPlanStepStatusEnum.BLOCKED), step(2L, AiTaskPlanStepStatusEnum.TODO))));
        assertEquals(AiTaskPlanStatusEnum.COMPLETED, AiTaskPlanStateMachine.derivePlanStatus(Arrays.asList(step(1L, AiTaskPlanStepStatusEnum.COMPLETED), step(2L, AiTaskPlanStepStatusEnum.SKIPPED))));
    }

    private AiTaskPlanStepDO step(Long id, AiTaskPlanStepStatusEnum status) {
        AiTaskPlanStepDO step = new AiTaskPlanStepDO();
        step.setId(id);
        step.setStatus(status.getCode());
        return step;
    }
}
