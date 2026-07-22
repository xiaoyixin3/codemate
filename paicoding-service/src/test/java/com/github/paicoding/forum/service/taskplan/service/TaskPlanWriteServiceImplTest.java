package com.github.paicoding.forum.service.taskplan.service;

import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepAddReq;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskPlanDao;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskWriteAuditDao;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanDO;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskWriteAuditDO;
import com.github.paicoding.forum.service.taskplan.service.impl.TaskPlanWriteServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskPlanWriteServiceImplTest {
    @Test
    void idempotentAddReturnsAuditedStepWithoutDuplicateWrite() {
        AiTaskPlanService planService = mock(AiTaskPlanService.class);
        AiTaskWriteAuditDao auditDao = mock(AiTaskWriteAuditDao.class);
        AiTaskPlanDao planDao = mock(AiTaskPlanDao.class);
        when(planDao.getByIdAndUserIdForUpdate(5L, 7L)).thenReturn(new AiTaskPlanDO());
        AiTaskWriteAuditDO audit = new AiTaskWriteAuditDO();
        audit.setStepId(9L);
        when(auditDao.find(7L, "addTaskPlanStep", "step-key-123")).thenReturn(audit);
        TaskPlanWriteServiceImpl service = new TaskPlanWriteServiceImpl(planService, auditDao, planDao);
        AiTaskPlanStepAddReq req = new AiTaskPlanStepAddReq();
        req.setIdempotencyKey("step-key-123");

        assertEquals(9L, service.addStep(7L, 1L, 5L, req));
        verify(planService, never()).addStep(any(), any(), any());
    }

    @Test
    void anotherUsersPlanIsRejectedBeforeWrite() {
        AiTaskPlanService planService = mock(AiTaskPlanService.class);
        AiTaskWriteAuditDao auditDao = mock(AiTaskWriteAuditDao.class);
        AiTaskPlanDao planDao = mock(AiTaskPlanDao.class);
        when(planDao.getByIdAndUserIdForUpdate(5L, 8L)).thenReturn(null);
        TaskPlanWriteServiceImpl service = new TaskPlanWriteServiceImpl(planService, auditDao, planDao);
        AiTaskPlanStepAddReq req = new AiTaskPlanStepAddReq();
        req.setIdempotencyKey("step-key-123");

        assertThrows(RuntimeException.class, () -> service.addStep(8L, 1L, 5L, req));
        verify(planService, never()).addStep(any(), any(), any());
        verify(auditDao, never()).save(any());
    }
}
