package com.github.paicoding.forum.service.agentrun.service;

import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.service.agentrun.repository.dao.AiAgentEvidenceDao;
import com.github.paicoding.forum.service.agentrun.repository.dao.AiAgentRunDao;
import com.github.paicoding.forum.service.agentrun.repository.dao.AiAgentStepDao;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentRunDO;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentStepDO;
import com.github.paicoding.forum.service.agentrun.service.impl.AgentRunServiceImpl;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.tool.ToolExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class AgentRunServiceImplTest {
    private AiAgentRunDao runDao;
    private AiAgentStepDao stepDao;
    private AgentRunServiceImpl service;

    @BeforeEach
    void setUp() {
        runDao = mock(AiAgentRunDao.class);
        stepDao = mock(AiAgentStepDao.class);
        AiAgentEvidenceDao evidenceDao = mock(AiAgentEvidenceDao.class);
        LangChain4jProperties properties = new LangChain4jProperties();
        service = new AgentRunServiceImpl(runDao, stepDao, evidenceDao, properties);
    }

    @Test
    void tokenBudgetProducesExplicitFailedTerminalState() {
        AiAgentRunDO run = executingRun();
        run.setMaxTokenBudget(100);
        when(runDao.getByIdForUpdate(1L)).thenReturn(run);
        when(runDao.updateById(any())).thenReturn(true);

        service.complete(1L, 70, 50, 120);

        assertEquals(AgentRunStatusEnum.FAILED.name(), run.getStatus());
        assertEquals("TOKEN_BUDGET_EXCEEDED", run.getFailureReason());
        assertEquals(120, run.getTotalTokenCount());
        assertTrue(run.getEndTime() != null);
    }

    @Test
    void lateCompletionCannotOverwriteCancelledTerminalState() {
        AiAgentRunDO run = executingRun();
        run.setStatus(AgentRunStatusEnum.CANCELLED.name());
        when(runDao.getByIdForUpdate(1L)).thenReturn(run);

        service.complete(1L, 1, 1, 2);

        verify(runDao, never()).updateById(any());
        assertEquals(AgentRunStatusEnum.CANCELLED.name(), run.getStatus());
    }

    @Test
    void consecutiveDuplicateToolCallIsBlockedWithoutSecondStep() {
        AiAgentRunDO run = executingRun();
        when(runDao.getByIdForUpdate(1L)).thenReturn(run);
        when(stepDao.nextStepNo(1L)).thenReturn(1);
        doAnswer(invocation -> {
            AiAgentStepDO step = invocation.getArgument(0);
            step.setId(9L);
            return true;
        }).when(stepDao).save(any(AiAgentStepDO.class));
        when(runDao.updateById(any())).thenReturn(true);

        assertEquals(9L, service.beginToolCall(1L, "search", "keyword=spring"));
        ArgumentCaptor<AiAgentStepDO> captor = ArgumentCaptor.forClass(AiAgentStepDO.class);
        verify(stepDao).save(captor.capture());
        when(stepDao.getLastToolStep(1L)).thenReturn(captor.getValue());

        ToolExecutionException error = assertThrows(ToolExecutionException.class,
                () -> service.beginToolCall(1L, "search", "keyword=spring"));
        assertEquals("DUPLICATE_TOOL_CALL", error.getErrorCode());
    }

    @Test
    void toolCallLimitMovesRunToFailedTerminalState() {
        AiAgentRunDO run = executingRun();
        run.setToolCallCount(8);
        run.setMaxToolCalls(8);
        when(runDao.getByIdForUpdate(1L)).thenReturn(run);
        when(runDao.updateById(any())).thenReturn(true);

        ToolExecutionException error = assertThrows(ToolExecutionException.class,
                () -> service.beginToolCall(1L, "search", "keyword=spring"));

        assertEquals("TOOL_CALL_LIMIT_EXCEEDED", error.getErrorCode());
        assertEquals(AgentRunStatusEnum.FAILED.name(), run.getStatus());
        assertEquals("TOOL_CALL_LIMIT_EXCEEDED", run.getFailureReason());
    }

    @Test
    void anotherUserCannotReadRunDetail() {
        when(runDao.getByIdAndUserId(5L, 8L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.detail(8L, 5L));
        verify(stepDao, never()).listByRunId(any());
    }

    @Test
    void sweeperMovesExpiredRunOutOfExecuting() {
        AiAgentRunDO run = executingRun();
        run.setStartTime(new Date(System.currentTimeMillis() - 10_000L));
        run.setMaxExecutionSeconds(1);
        when(runDao.listNonTerminal(100)).thenReturn(Collections.singletonList(run));
        when(runDao.updateStatus(any(), any(), any(), any(), any())).thenReturn(true);

        service.failExpiredRuns();

        verify(runDao).updateStatus(eq(1L), eq(AgentRunStatusEnum.FAILED), any(Date.class),
                eq("EXECUTION_TIME_LIMIT_EXCEEDED"),
                eq(AgentRunStatusEnum.CREATED), eq(AgentRunStatusEnum.PLANNING),
                eq(AgentRunStatusEnum.WAITING_CONFIRMATION), eq(AgentRunStatusEnum.EXECUTING));
    }

    @Test
    void sweeperDoesNotExpireRunWaitingForUserConfirmation() {
        AiAgentRunDO run = executingRun();
        run.setStatus(AgentRunStatusEnum.WAITING_CONFIRMATION.name());
        run.setStartTime(new Date(System.currentTimeMillis() - 10_000L));
        run.setMaxExecutionSeconds(1);
        when(runDao.listNonTerminal(100)).thenReturn(Collections.singletonList(run));

        service.failExpiredRuns();

        verify(runDao, never()).updateStatus(any(), any(), any(), any(), any());
    }

    @Test
    void ownerCanCancelAndExecutingStepsBecomeCancelled() {
        AiAgentRunDO run = executingRun();
        when(runDao.getByIdAndUserId(1L, 7L)).thenReturn(run);
        when(runDao.updateStatus(any(), any(), any(), any(), any())).thenReturn(true);

        service.cancel(7L, 1L);

        verify(stepDao).finishExecutingByRunId(1L,
                com.github.paicoding.forum.api.model.enums.ai.AgentStepStatusEnum.CANCELLED,
                "RUN_CANCELLED");
    }

    private AiAgentRunDO executingRun() {
        AiAgentRunDO run = new AiAgentRunDO();
        run.setId(1L);
        run.setUserId(7L);
        run.setStatus(AgentRunStatusEnum.EXECUTING.name());
        run.setStartTime(new Date());
        run.setToolCallCount(0);
        run.setMaxToolCalls(8);
        run.setMaxExecutionSeconds(180);
        run.setMaxTokenBudget(16000);
        return run;
    }
}
