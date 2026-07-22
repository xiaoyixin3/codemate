package com.github.paicoding.forum.service.bugdiagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanCreateReq;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import com.github.paicoding.forum.service.bugdiagnosis.repository.dao.AiBugDiagnosisDao;
import com.github.paicoding.forum.service.bugdiagnosis.repository.entity.AiBugDiagnosisDO;
import com.github.paicoding.forum.service.bugdiagnosis.service.impl.BugDiagnosisServiceImpl;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskWriteAuditDao;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskWriteAuditDO;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class BugDiagnosisServiceImplTest {
    private AiBugDiagnosisDao diagnosisDao;
    private AiTaskWriteAuditDao auditDao;
    private AiTaskPlanService planService;
    private AgentRunService runService;
    private BugDiagnosisServiceImpl service;

    @BeforeEach
    void setUp() {
        diagnosisDao = mock(AiBugDiagnosisDao.class);
        auditDao = mock(AiTaskWriteAuditDao.class);
        planService = mock(AiTaskPlanService.class);
        runService = mock(AgentRunService.class);
        service = new BugDiagnosisServiceImpl(new ObjectMapper(), diagnosisDao, auditDao, planService, runService);
        AgentRunDetailVo run = new AgentRunDetailVo();
        run.setStatus(AgentRunStatusEnum.WAITING_CONFIRMATION.name());
        run.setMode("BUG_DIAGNOSIS");
        when(runService.detail(7L, 11L)).thenReturn(run);
        when(runService.transition(11L, AgentRunStatusEnum.EXECUTING)).thenReturn(true);
    }

    @Test
    void validStructuredOutputCreatesPreviewWithoutCreatingPlan() {
        AiBugDiagnosisDO source = previewDiagnosis();
        doAnswer(invocation -> {
            AiBugDiagnosisDO saved = invocation.getArgument(0);
            saved.setId(3L);
            return true;
        }).when(diagnosisDao).save(any(AiBugDiagnosisDO.class));

        Long diagnosisId = service.createPreview(7L, 11L, "chat-1", source.getDiagnosisJson());

        assertEquals(3L, diagnosisId);
        ArgumentCaptor<AiBugDiagnosisDO> saved = ArgumentCaptor.forClass(AiBugDiagnosisDO.class);
        verify(diagnosisDao).save(saved.capture());
        assertEquals("PREVIEW", saved.getValue().getStatus());
        assertEquals("保存时出现空指针", saved.getValue().getProblemSummary());
        verify(planService, never()).createPlan(any(), any());
    }

    @Test
    void invalidStructuredOutputDoesNotCreatePreview() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPreview(7L, 11L, "chat-1", "{\"problemSummary\":\"字段不完整\"}"));
        verify(diagnosisDao, never()).save(any());
    }

    @Test
    void confirmationCreatesPlanWithVerificationAndRegressionStepsAndAudit() {
        AiBugDiagnosisDO diagnosis = previewDiagnosis();
        when(diagnosisDao.getOwnedForUpdate(3L, 7L)).thenReturn(diagnosis);
        when(planService.createPlan(any(), any())).thenReturn(99L);

        BugDiagnosisConfirmVo result = service.confirm(7L, 3L, "confirm-key-123");

        assertEquals(99L, result.getPlanId());
        assertEquals("/task-plan/99", result.getPlanUrl());
        ArgumentCaptor<AiTaskPlanCreateReq> plan = ArgumentCaptor.forClass(AiTaskPlanCreateReq.class);
        verify(planService).createPlan(org.mockito.ArgumentMatchers.eq(7L), plan.capture());
        assertEquals("bug-diagnosis:3", plan.getValue().getPlanKey());
        assertEquals(3, plan.getValue().getSteps().size());
        assertTrue(plan.getValue().getSteps().get(2).getTitle().startsWith("回归验证"));
        verify(auditDao).save(any(AiTaskWriteAuditDO.class));
        verify(runService).transition(11L, AgentRunStatusEnum.EXECUTING);
        verify(runService).complete(11L, null, null, null);
    }

    @Test
    void duplicateConfirmationReturnsExistingPlanWithoutWritingAgain() {
        AiBugDiagnosisDO diagnosis = previewDiagnosis();
        diagnosis.setStatus("CONFIRMED");
        diagnosis.setPlanId(42L);
        when(diagnosisDao.getOwnedForUpdate(3L, 7L)).thenReturn(diagnosis);

        BugDiagnosisConfirmVo first = service.confirm(7L, 3L, "confirm-key-123");
        BugDiagnosisConfirmVo second = service.confirm(7L, 3L, "another-key-456");

        assertEquals(42L, first.getPlanId());
        assertEquals(42L, second.getPlanId());
        verify(planService, never()).createPlan(any(), any());
        verify(auditDao, never()).save(any());
    }

    @Test
    void unauthorizedConfirmationCannotReadOrCreatePlan() {
        when(diagnosisDao.getOwnedForUpdate(3L, 8L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.confirm(8L, 3L, "confirm-key-123"));
        verify(planService, never()).createPlan(any(), any());
    }

    @Test
    void partialFailureDoesNotMarkDiagnosisOrWriteAudit() {
        AiBugDiagnosisDO diagnosis = previewDiagnosis();
        when(diagnosisDao.getOwnedForUpdate(3L, 7L)).thenReturn(diagnosis);
        when(planService.createPlan(any(), any())).thenThrow(new IllegalStateException("step insert failed"));

        assertThrows(IllegalStateException.class, () -> service.confirm(7L, 3L, "confirm-key-123"));

        assertEquals("PREVIEW", diagnosis.getStatus());
        verify(diagnosisDao, never()).updateById(any());
        verify(auditDao, never()).save(any());
    }

    @Test
    void concurrentConfirmationCreatesOnlyOnePlan() throws Exception {
        AiBugDiagnosisDO diagnosis = previewDiagnosis();
        when(diagnosisDao.getOwnedForUpdate(3L, 7L)).thenReturn(diagnosis);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(planService.createPlan(any(), any())).thenAnswer(invocation -> {
            entered.countDown();
            release.await();
            return 77L;
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<BugDiagnosisConfirmVo> first = pool.submit(() -> service.confirm(7L, 3L, "confirm-key-123"));
            entered.await();
            Future<BugDiagnosisConfirmVo> second = pool.submit(() -> service.confirm(7L, 3L, "confirm-key-123"));
            release.countDown();
            assertEquals(77L, first.get().getPlanId());
            assertEquals(77L, second.get().getPlanId());
            verify(planService, times(1)).createPlan(any(), any());
        } finally {
            pool.shutdownNow();
        }
    }

    private AiBugDiagnosisDO previewDiagnosis() {
        AiBugDiagnosisDO diagnosis = new AiBugDiagnosisDO();
        diagnosis.setId(3L);
        diagnosis.setRunId(11L);
        diagnosis.setUserId(7L);
        diagnosis.setChatId("chat-1");
        diagnosis.setStatus("PREVIEW");
        diagnosis.setDiagnosisJson("{\"problemSummary\":\"保存时出现空指针\","
                + "\"causeHypotheses\":[{\"hypothesis\":\"字段未初始化\",\"supportingEvidence\":[\"异常栈指向保存逻辑\"],\"confidence\":0.8}],"
                + "\"supportingEvidence\":[{\"articleId\":12,\"title\":\"空指针排查\",\"excerpt\":\"保存前校验字段\"}],"
                + "\"verificationSteps\":[\"构造缺失字段请求复现\"],\"fixSuggestions\":[\"增加非空校验\"],"
                + "\"regressionPlan\":[\"运行保存接口正常与异常用例\"]}");
        return diagnosis;
    }
}
