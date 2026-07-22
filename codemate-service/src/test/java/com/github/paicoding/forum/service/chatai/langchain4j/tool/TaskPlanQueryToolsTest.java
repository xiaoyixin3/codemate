package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanDetailVo;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskPlanQueryToolsTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AiTaskPlanService taskPlanService;
    private TrustedToolContextRegistry contextRegistry;
    private SafeToolExecutor executor;
    private TaskPlanQueryTools tools;

    @BeforeEach
    void setUp() {
        taskPlanService = mock(AiTaskPlanService.class);
        contextRegistry = new TrustedToolContextRegistry();
        executor = new SafeToolExecutor(objectMapper, new LangChain4jProperties(),
                new ToolMetrics(new SimpleMeterRegistry()), Executors.newSingleThreadExecutor());
        tools = new TaskPlanQueryTools(taskPlanService, contextRegistry, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void listUsesOnlyServerBoundUser() throws Exception {
        contextRegistry.bind("trusted-memory", 7L);
        when(taskPlanService.listPlans(7L)).thenReturn(Collections.emptyList());
        JsonNode result = objectMapper.readTree(tools.listCurrentUserTaskPlans("trusted-memory"));
        assertTrue(result.get("success").asBoolean());
        verify(taskPlanService).listPlans(7L);
    }

    @Test
    void missingTrustedContextIsRejected() throws Exception {
        JsonNode result = objectMapper.readTree(tools.listCurrentUserTaskPlans("model-invented-memory"));
        assertFalse(result.get("success").asBoolean());
        assertEquals("AUTHENTICATION_REQUIRED", result.get("errorCode").asText());
    }

    @Test
    void planFromAnotherUserIsNotDisclosed() throws Exception {
        contextRegistry.bind("trusted-memory", 7L);
        when(taskPlanService.queryPlanDetail(7L, 99L)).thenThrow(new RuntimeException("not owned"));
        String json = tools.getCurrentUserTaskPlan("trusted-memory", 99L);
        JsonNode result = objectMapper.readTree(json);
        assertFalse(result.get("success").asBoolean());
        assertEquals("ACCESS_DENIED_OR_NOT_FOUND", result.get("errorCode").asText());
        assertFalse(json.contains("not owned"));
    }

    @Test
    void readsOwnedPlanDetail() throws Exception {
        contextRegistry.bind("trusted-memory", 7L);
        AiTaskPlanDetailVo detail = new AiTaskPlanDetailVo();
        detail.setPlanId(5L);
        detail.setTitle("owned");
        when(taskPlanService.queryPlanDetail(7L, 5L)).thenReturn(detail);
        JsonNode result = objectMapper.readTree(tools.getCurrentUserTaskPlan("trusted-memory", 5L));
        assertTrue(result.get("success").asBoolean());
        assertEquals(5L, result.at("/data/planId").asLong());
    }
}
