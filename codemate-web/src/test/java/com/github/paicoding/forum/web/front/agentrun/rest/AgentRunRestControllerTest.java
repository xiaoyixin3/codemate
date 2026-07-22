package com.github.paicoding.forum.web.front.agentrun.rest;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunListVo;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentRunRestControllerTest {
    private AgentRunService agentRunService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        agentRunService = mock(AgentRunService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentRunRestController(agentRunService)).build();
        ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo();
        reqInfo.setUserId(7L);
        ReqInfoContext.addReqInfo(reqInfo);
    }

    @AfterEach
    void tearDown() {
        ReqInfoContext.clear();
    }

    @Test
    void listsOnlyCurrentUsersRunsUsingRequestedLimit() throws Exception {
        AgentRunListVo run = new AgentRunListVo();
        run.setRunId(21L);
        run.setGoal("定位空指针异常");
        run.setStatus("EXECUTING");
        when(agentRunService.listRuns(7L, 12)).thenReturn(Collections.singletonList(run));

        mockMvc.perform(get("/agent-run/api").param("limit", "12").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(0))
                .andExpect(jsonPath("$.result[0].runId").value(21L))
                .andExpect(jsonPath("$.result[0].status").value("EXECUTING"));

        verify(agentRunService).listRuns(7L, 12);
    }

    @Test
    void returnsOwnedRunDetail() throws Exception {
        AgentRunDetailVo detail = new AgentRunDetailVo();
        detail.setRunId(21L);
        detail.setFailureReason("TOKEN_BUDGET_EXCEEDED");
        when(agentRunService.detail(7L, 21L)).thenReturn(detail);

        mockMvc.perform(get("/agent-run/api/21").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.runId").value(21L))
                .andExpect(jsonPath("$.result.failureReason").value("TOKEN_BUDGET_EXCEEDED"));

        verify(agentRunService).detail(7L, 21L);
    }

    @Test
    void cancelsOwnedRun() throws Exception {
        mockMvc.perform(put("/agent-run/api/21/cancel").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(agentRunService).cancel(7L, 21L);
    }
}
