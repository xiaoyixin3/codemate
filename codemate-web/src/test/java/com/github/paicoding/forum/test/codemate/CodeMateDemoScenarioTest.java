package com.github.paicoding.forum.test.codemate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.PushStatusEnum;
import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStepStatusEnum;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisCauseDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisEvidenceDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisModelResponseDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisPreviewVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanDetailVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepVo;
import com.github.paicoding.forum.service.agentrun.service.AgentRunContextRegistry;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import com.github.paicoding.forum.service.article.repository.dao.ArticleDao;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.bugdiagnosis.service.BugDiagnosisService;
import com.github.paicoding.forum.service.chatai.langchain4j.tool.ArticleKnowledgeTools;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import com.github.paicoding.forum.service.taskplan.service.TaskPlanWriteService;
import com.github.paicoding.forum.web.QuickForumApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable, no-paid-model demonstration of the complete CodeMate Agent workflow.
 * All rows use a dedicated random user id and are removed after the scenario.
 */
@SpringBootTest(classes = QuickForumApplication.class)
class CodeMateDemoScenarioTest {
    private static final String ARTICLE_KEYWORD = "CodeMate Demo Circuit Breaker";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private ArticleKnowledgeTools articleTools;
    @Autowired
    private AgentRunService agentRunService;
    @Autowired
    private AgentRunContextRegistry runContextRegistry;
    @Autowired
    private BugDiagnosisService bugDiagnosisService;
    @Autowired
    private AiTaskPlanService taskPlanService;
    @Autowired
    private TaskPlanWriteService taskPlanWriteService;

    private final long demoUserId = 2_000_000_000L + ThreadLocalRandom.current().nextInt(100_000_000);
    private final String suffix = UUID.randomUUID().toString().replace("-", "");
    private final String memoryId = "codemate-demo:" + suffix;
    private Long articleId;

    @Test
    void reproducesKnowledgeDiagnosisPlanAndTraceWorkflow() throws Exception {
        articleId = createPublishedArticle();
        Long runId = agentRunService.create(demoUserId, "demo-" + suffix, "BUG_DIAGNOSIS",
                "Diagnose repeated downstream timeouts and create a verified repair plan", "offline-demo");
        assertTrue(agentRunService.transition(runId, AgentRunStatusEnum.PLANNING));
        assertTrue(agentRunService.transition(runId, AgentRunStatusEnum.EXECUTING));

        runContextRegistry.bind(memoryId, runId);
        try {
            JsonNode search = objectMapper.readTree(articleTools.searchPublishedArticles(memoryId, ARTICLE_KEYWORD));
            assertTrue(search.path("success").asBoolean());
            assertFalse(search.path("data").isEmpty());
            assertEquals(articleId.longValue(), search.path("data").get(0).path("articleId").asLong());

            JsonNode article = objectMapper.readTree(articleTools.getPublishedArticle(memoryId, articleId));
            assertTrue(article.path("success").asBoolean());
            assertTrue(article.path("data").path("content").asText().contains("circuit breaker"));
        } finally {
            runContextRegistry.unbind(memoryId);
        }

        agentRunService.recordEvidence(runId, articleId, 0, ARTICLE_KEYWORD,
                "Timeout guidance recommends a bounded timeout and circuit breaker.", 0.96D);
        Long diagnosisId = bugDiagnosisService.createPreview(demoUserId, runId, "demo-" + suffix,
                diagnosisJson());
        agentRunService.waitForConfirmation(runId, 120, 80, 200);

        BugDiagnosisPreviewVo preview = bugDiagnosisService.preview(demoUserId, diagnosisId);
        assertEquals(runId, preview.getAgentRunId());
        assertEquals("PREVIEW", preview.getStatus());
        assertNotNull(preview.getSupportingEvidence());

        String confirmationKey = "demo-confirm-" + suffix;
        BugDiagnosisConfirmVo confirmation = bugDiagnosisService.confirm(demoUserId, diagnosisId, confirmationKey);
        BugDiagnosisConfirmVo repeatedConfirmation = bugDiagnosisService.confirm(demoUserId, diagnosisId, confirmationKey);
        assertEquals(confirmation.getPlanId(), repeatedConfirmation.getPlanId());
        assertEquals("/task-plan/" + confirmation.getPlanId(), confirmation.getPlanUrl());

        completePlan(runId, confirmation.getPlanId());

        AiTaskPlanDetailVo plan = taskPlanService.queryPlanDetail(demoUserId, confirmation.getPlanId());
        assertEquals(100, plan.getProgress());
        assertEquals(plan.getStepTotal(), plan.getCompletedStepCount());
        assertTrue(plan.getSteps().stream().allMatch(step -> step.getActualOutput() != null));

        AgentRunDetailVo trace = agentRunService.detail(demoUserId, runId);
        assertEquals(AgentRunStatusEnum.COMPLETED.name(), trace.getStatus());
        assertEquals(2, trace.getToolCallCount());
        assertEquals(2, trace.getSteps().size());
        assertEquals(1, trace.getEvidence().size());
        assertEquals(articleId, trace.getEvidence().get(0).getArticleId());
        assertThrows(RuntimeException.class, () -> agentRunService.detail(demoUserId + 1, runId));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_task_write_audit WHERE user_id = ?", Integer.class, demoUserId);
        assertNotNull(auditCount);
        assertTrue(auditCount >= 1 + plan.getStepTotal() * 3,
                "Confirmation and every step update must be auditable");
    }

    private Long createPublishedArticle() {
        ArticleDO article = new ArticleDO();
        article.setUserId(demoUserId);
        article.setArticleType(1);
        article.setTitle(ARTICLE_KEYWORD + " " + suffix.substring(0, 8));
        article.setShortTitle(ARTICLE_KEYWORD);
        article.setUrlSlug("codemate-demo-circuit-breaker-" + suffix);
        article.setPicture("");
        article.setSummary("A deterministic knowledge article used by the CodeMate demonstration.");
        article.setCategoryId(0L);
        article.setSource(2);
        article.setSourceUrl("");
        article.setStatus(PushStatusEnum.ONLINE.getCode());
        article.setOfficalStat(0);
        article.setToppingStat(0);
        article.setCreamStat(0);
        article.setDeleted(YesOrNoEnum.NO.getCode());
        articleDao.save(article);
        articleDao.saveArticleContent(article.getId(),
                "# Circuit breaker guidance\nUse a bounded client timeout, circuit breaker, and regression test.");
        return article.getId();
    }

    private String diagnosisJson() throws Exception {
        BugDiagnosisCauseDTO cause = new BugDiagnosisCauseDTO();
        cause.setHypothesis("The downstream client has no bounded timeout or circuit breaker");
        cause.setSupportingEvidence(Collections.singletonList("The retrieved article describes the same failure mode"));
        cause.setConfidence(0.91D);

        BugDiagnosisEvidenceDTO evidence = new BugDiagnosisEvidenceDTO();
        evidence.setArticleId(articleId);
        evidence.setTitle(ARTICLE_KEYWORD);
        evidence.setExcerpt("Use a bounded client timeout, circuit breaker, and regression test.");

        BugDiagnosisModelResponseDTO diagnosis = new BugDiagnosisModelResponseDTO();
        diagnosis.setProblemSummary("Repeated downstream timeouts exhaust request threads");
        diagnosis.setCauseHypotheses(Collections.singletonList(cause));
        diagnosis.setSupportingEvidence(Collections.singletonList(evidence));
        diagnosis.setVerificationSteps(Collections.singletonList("Reproduce the timeout with a controlled slow dependency"));
        diagnosis.setFixSuggestions(Collections.singletonList("Configure a bounded timeout and circuit breaker"));
        diagnosis.setRegressionPlan(Collections.singletonList("Run normal, timeout, and recovery-path tests"));
        return objectMapper.writeValueAsString(diagnosis);
    }

    private void completePlan(Long runId, Long planId) {
        AiTaskPlanDetailVo plan = taskPlanService.queryPlanDetail(demoUserId, planId);
        for (AiTaskPlanStepVo step : plan.getSteps()) {
            AiTaskPlanStepStatusUpdateReq started = new AiTaskPlanStepStatusUpdateReq();
            started.setStatus(AiTaskPlanStepStatusEnum.IN_PROGRESS.getCode());
            started.setIdempotencyKey(key("start", step.getStepId()));
            taskPlanWriteService.updateStepStatus(demoUserId, runId, planId, step.getStepId(), started);

            AiTaskPlanStepResultUpdateReq result = new AiTaskPlanStepResultUpdateReq();
            result.setActualOutput("Verified demo result for step " + step.getStepNo());
            result.setIdempotencyKey(key("result", step.getStepId()));
            taskPlanWriteService.recordStepResult(demoUserId, runId, planId, step.getStepId(), result);

            AiTaskPlanStepStatusUpdateReq completed = new AiTaskPlanStepStatusUpdateReq();
            completed.setStatus(AiTaskPlanStepStatusEnum.COMPLETED.getCode());
            completed.setIdempotencyKey(key("complete", step.getStepId()));
            taskPlanWriteService.updateStepStatus(demoUserId, runId, planId, step.getStepId(), completed);
        }
    }

    private String key(String action, Long stepId) {
        return "demo-" + action + "-" + stepId + "-" + suffix.substring(0, 8);
    }

    @AfterEach
    void cleanDemoRows() {
        runContextRegistry.unbind(memoryId);
        Arrays.asList(
                "DELETE FROM ai_task_write_audit WHERE user_id = ?",
                "DELETE FROM ai_task_plan_step WHERE user_id = ?",
                "DELETE FROM ai_task_plan WHERE user_id = ?",
                "DELETE FROM ai_bug_diagnosis WHERE user_id = ?",
                "DELETE FROM ai_agent_evidence WHERE user_id = ?",
                "DELETE FROM ai_agent_step WHERE user_id = ?",
                "DELETE FROM ai_agent_run WHERE user_id = ?"
        ).forEach(sql -> jdbcTemplate.update(sql, demoUserId));
        if (articleId != null) {
            jdbcTemplate.update("DELETE FROM article_detail WHERE article_id = ?", articleId);
            jdbcTemplate.update("DELETE FROM article WHERE id = ? AND user_id = ?", articleId, demoUserId);
        }
    }
}
