package com.github.paicoding.forum.service.agentrun.service.impl;

import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AgentStepStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AgentStepTypeEnum;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunEvidenceVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunListVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunStepVo;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.service.agentrun.repository.dao.AiAgentEvidenceDao;
import com.github.paicoding.forum.service.agentrun.repository.dao.AiAgentRunDao;
import com.github.paicoding.forum.service.agentrun.repository.dao.AiAgentStepDao;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentEvidenceDO;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentRunDO;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentStepDO;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import com.github.paicoding.forum.service.agentrun.service.AgentRunStateMachine;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.tool.ToolExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AgentRunServiceImpl implements AgentRunService {
    private static final int MAX_LIST_SIZE = 100;
    private final AiAgentRunDao runDao;
    private final AiAgentStepDao stepDao;
    private final AiAgentEvidenceDao evidenceDao;
    private final LangChain4jProperties properties;

    public AgentRunServiceImpl(AiAgentRunDao runDao, AiAgentStepDao stepDao,
                               AiAgentEvidenceDao evidenceDao, LangChain4jProperties properties) {
        this.runDao = runDao;
        this.stepDao = stepDao;
        this.evidenceDao = evidenceDao;
        this.properties = properties;
    }

    @Override
    public Long create(Long userId, String chatId, String mode, String goal, String model) {
        if (StringUtils.isBlank(mode)) {
            throw new IllegalArgumentException("Agent mode is required");
        }
        Date now = new Date();
        AiAgentRunDO run = new AiAgentRunDO();
        run.setUserId(userId == null ? 0L : Math.max(0L, userId));
        run.setChatId(StringUtils.left(StringUtils.trimToEmpty(chatId), 64));
        run.setMode(StringUtils.left(mode, 32));
        run.setGoal(StringUtils.left(StringUtils.trimToEmpty(goal), 512));
        run.setStatus(AgentRunStatusEnum.CREATED.name());
        run.setModel(StringUtils.left(StringUtils.trimToEmpty(model), 128));
        run.setStartTime(now);
        run.setInputTokenCount(0);
        run.setOutputTokenCount(0);
        run.setTotalTokenCount(0);
        run.setToolCallCount(0);
        run.setMaxToolCalls(Math.max(1, properties.getMaxRunToolCalls()));
        run.setMaxExecutionSeconds(Math.max(1, properties.getMaxRunExecutionSeconds()));
        run.setMaxTokenBudget(Math.max(1, properties.getMaxRunTokenBudget()));
        run.setCreateTime(now);
        run.setUpdateTime(now);
        runDao.save(run);
        return run.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transition(Long runId, AgentRunStatusEnum target) {
        AiAgentRunDO run = runDao.getByIdForUpdate(runId);
        if (run == null) {
            return false;
        }
        AgentRunStatusEnum current = AgentRunStatusEnum.valueOf(run.getStatus());
        if (current.isTerminal()) {
            return false;
        }
        AgentRunStateMachine.validate(current, target);
        run.setStatus(target.name());
        if (target.isTerminal()) {
            run.setEndTime(new Date());
        }
        run.setUpdateTime(new Date());
        return runDao.updateById(run);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long runId, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        AiAgentRunDO run = runDao.getByIdForUpdate(runId);
        if (run == null || AgentRunStatusEnum.valueOf(run.getStatus()).isTerminal()) {
            return;
        }
        int safeInput = inputTokens == null ? positive(run.getInputTokenCount()) : positive(inputTokens);
        int safeOutput = outputTokens == null ? positive(run.getOutputTokenCount()) : positive(outputTokens);
        int safeTotal = totalTokens == null ? safeInput + safeOutput : positive(totalTokens);
        run.setInputTokenCount(safeInput);
        run.setOutputTokenCount(safeOutput);
        run.setTotalTokenCount(safeTotal);
        Date now = new Date();
        boolean expired = elapsedSeconds(run, now) > run.getMaxExecutionSeconds();
        boolean overTokens = safeTotal > run.getMaxTokenBudget();
        run.setStatus(expired || overTokens ? AgentRunStatusEnum.FAILED.name() : AgentRunStatusEnum.COMPLETED.name());
        run.setFailureReason(expired ? "EXECUTION_TIME_LIMIT_EXCEEDED"
                : overTokens ? "TOKEN_BUDGET_EXCEEDED" : null);
        run.setEndTime(now);
        run.setUpdateTime(now);
        runDao.updateById(run);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void waitForConfirmation(Long runId, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        AiAgentRunDO run = runDao.getByIdForUpdate(runId);
        if (run == null || !AgentRunStatusEnum.EXECUTING.name().equals(run.getStatus())) {
            return;
        }
        int safeInput = positive(inputTokens);
        int safeOutput = positive(outputTokens);
        int safeTotal = totalTokens == null ? safeInput + safeOutput : positive(totalTokens);
        if (safeTotal > run.getMaxTokenBudget()) {
            terminalizeLocked(run, "TOKEN_BUDGET_EXCEEDED", new Date());
            return;
        }
        run.setInputTokenCount(safeInput);
        run.setOutputTokenCount(safeOutput);
        run.setTotalTokenCount(safeTotal);
        run.setStatus(AgentRunStatusEnum.WAITING_CONFIRMATION.name());
        run.setUpdateTime(new Date());
        runDao.updateById(run);
    }

    @Override
    public void fail(Long runId, String reason) {
        String safeReason = safeReason(reason);
        boolean updated = runDao.updateStatus(runId, AgentRunStatusEnum.FAILED, new Date(), safeReason,
                AgentRunStatusEnum.CREATED, AgentRunStatusEnum.PLANNING,
                AgentRunStatusEnum.WAITING_CONFIRMATION, AgentRunStatusEnum.EXECUTING);
        if (updated) {
            stepDao.finishExecutingByRunId(runId, AgentStepStatusEnum.FAILED, safeReason);
        }
    }

    @Override
    public void cancel(Long userId, Long runId) {
        AiAgentRunDO run = requireOwnedRun(userId, runId);
        boolean updated = runDao.updateStatus(run.getId(), AgentRunStatusEnum.CANCELLED, new Date(), "Cancelled by user",
                AgentRunStatusEnum.CREATED, AgentRunStatusEnum.PLANNING,
                AgentRunStatusEnum.WAITING_CONFIRMATION, AgentRunStatusEnum.EXECUTING);
        if (updated) {
            stepDao.finishExecutingByRunId(runId, AgentStepStatusEnum.CANCELLED, "RUN_CANCELLED");
        }
    }

    @Override
    @Transactional(noRollbackFor = ToolExecutionException.class)
    public Long beginToolCall(Long runId, String toolName, String canonicalArguments) {
        AiAgentRunDO run = runDao.getByIdForUpdate(runId);
        if (run == null || !AgentRunStatusEnum.EXECUTING.name().equals(run.getStatus())) {
            throw new ToolExecutionException("RUN_NOT_EXECUTING", "Agent Run 已结束，不能继续调用工具");
        }
        Date now = new Date();
        if (elapsedSeconds(run, now) > run.getMaxExecutionSeconds()) {
            terminalizeLocked(run, "EXECUTION_TIME_LIMIT_EXCEEDED", now);
            throw new ToolExecutionException("EXECUTION_TIME_LIMIT_EXCEEDED", "Agent Run 已超过最大执行时间");
        }
        if (run.getToolCallCount() >= run.getMaxToolCalls()) {
            terminalizeLocked(run, "TOOL_CALL_LIMIT_EXCEEDED", now);
            throw new ToolExecutionException("TOOL_CALL_LIMIT_EXCEEDED", "Agent Run 已达到最大工具调用次数");
        }
        String fingerprint = fingerprint(toolName + "\n" + StringUtils.defaultString(canonicalArguments));
        AiAgentStepDO last = stepDao.getLastToolStep(runId);
        if (last != null && toolName.equals(last.getToolName()) && fingerprint.equals(last.getCallFingerprint())) {
            throw new ToolExecutionException("DUPLICATE_TOOL_CALL", "已阻止使用相同参数连续调用同一工具");
        }
        AiAgentStepDO step = new AiAgentStepDO();
        step.setRunId(runId);
        step.setUserId(run.getUserId());
        step.setStepNo(stepDao.nextStepNo(runId));
        step.setType(AgentStepTypeEnum.TOOL_CALL.name());
        step.setToolName(StringUtils.left(toolName, 128));
        step.setArgumentSummary("sha256:" + fingerprint + ",length=" + StringUtils.length(canonicalArguments));
        step.setCallFingerprint(fingerprint);
        step.setStatus(AgentStepStatusEnum.EXECUTING.name());
        step.setCreateTime(now);
        step.setUpdateTime(now);
        stepDao.save(step);
        run.setToolCallCount(run.getToolCallCount() + 1);
        run.setUpdateTime(now);
        runDao.updateById(run);
        return step.getId();
    }

    @Override
    public void finishToolCall(Long stepId, boolean success, String resultSummary, String errorType, long durationMs) {
        if (stepId == null) {
            return;
        }
        AiAgentStepDO step = stepDao.getById(stepId);
        if (step == null || !AgentStepStatusEnum.EXECUTING.name().equals(step.getStatus())) {
            return;
        }
        step.setResultSummary(StringUtils.left(StringUtils.defaultString(resultSummary), 1000));
        step.setStatus(success ? AgentStepStatusEnum.COMPLETED.name() : AgentStepStatusEnum.FAILED.name());
        step.setErrorType(StringUtils.left(errorType, 128));
        step.setDurationMs(Math.max(0L, durationMs));
        step.setUpdateTime(new Date());
        stepDao.updateById(step);
    }

    @Override
    public void recordEvidence(Long runId, Long articleId, Integer chunkIndex, String title,
                               String summary, Double relevance) {
        AiAgentRunDO run = runDao.getById(runId);
        if (run == null || articleId == null || chunkIndex == null
                || evidenceDao.find(runId, articleId, chunkIndex) != null) {
            return;
        }
        AiAgentEvidenceDO evidence = new AiAgentEvidenceDO();
        evidence.setRunId(runId);
        evidence.setUserId(run.getUserId());
        evidence.setSourceType("ARTICLE_CHUNK");
        evidence.setArticleId(articleId);
        evidence.setChunkIndex(chunkIndex);
        evidence.setTitle(StringUtils.left(StringUtils.defaultString(title), 256));
        evidence.setEvidenceSummary(StringUtils.left(StringUtils.defaultString(summary), 1000));
        evidence.setRelevance(relevance);
        evidence.setCreateTime(new Date());
        evidence.setUpdateTime(new Date());
        try {
            evidenceDao.save(evidence);
        } catch (DuplicateKeyException ignored) {
            // Concurrent callbacks for the same retrieved chunk are idempotent.
        }
    }

    @Override
    public List<AgentRunListVo> listRuns(Long userId, int limit) {
        checkUser(userId);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIST_SIZE));
        return runDao.listByUserId(userId, safeLimit).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public AgentRunDetailVo detail(Long userId, Long runId) {
        AiAgentRunDO run = requireOwnedRun(userId, runId);
        AgentRunDetailVo vo = new AgentRunDetailVo();
        copyRun(run, vo);
        vo.setInputTokenCount(run.getInputTokenCount());
        vo.setOutputTokenCount(run.getOutputTokenCount());
        vo.setMaxToolCalls(run.getMaxToolCalls());
        vo.setMaxExecutionSeconds(run.getMaxExecutionSeconds());
        vo.setMaxTokenBudget(run.getMaxTokenBudget());
        vo.setFailureReason(run.getFailureReason());
        vo.setSteps(stepDao.listByRunId(runId).stream().map(this::toStepVo).collect(Collectors.toList()));
        vo.setEvidence(evidenceDao.listByRunId(runId).stream().map(this::toEvidenceVo).collect(Collectors.toList()));
        return vo;
    }

    @Scheduled(fixedDelayString = "${codemate.langchain4j.run-sweeper-delay-millis:10000}")
    public void failExpiredRuns() {
        Date now = new Date();
        for (AiAgentRunDO run : runDao.listNonTerminal(100)) {
            if (AgentRunStatusEnum.WAITING_CONFIRMATION.name().equals(run.getStatus())) {
                continue;
            }
            if (elapsedSeconds(run, now) > run.getMaxExecutionSeconds()) {
                fail(run.getId(), "EXECUTION_TIME_LIMIT_EXCEEDED");
            }
        }
    }

    private void terminalizeLocked(AiAgentRunDO run, String reason, Date now) {
        run.setStatus(AgentRunStatusEnum.FAILED.name());
        run.setFailureReason(reason);
        run.setEndTime(now);
        run.setUpdateTime(now);
        runDao.updateById(run);
    }

    private long elapsedSeconds(AiAgentRunDO run, Date now) {
        return run.getStartTime() == null ? 0L
                : TimeUnit.MILLISECONDS.toSeconds(Math.max(0L, now.getTime() - run.getStartTime().getTime()));
    }

    private AiAgentRunDO requireOwnedRun(Long userId, Long runId) {
        checkUser(userId);
        if (runId == null || runId <= 0) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "非法 Agent Run ID");
        }
        AiAgentRunDO run = runDao.getByIdAndUserId(runId, userId);
        if (run == null) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "Agent Run 不存在或无权访问");
        }
        return run;
    }

    private void checkUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw ExceptionUtil.of(StatusEnum.FORBID_ERROR_MIXED, "请先登录");
        }
    }

    private int positive(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String safeReason(String reason) {
        return StringUtils.left(StringUtils.defaultIfBlank(reason, "AGENT_EXECUTION_FAILED"), 512);
    }

    private String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fingerprint tool call", e);
        }
    }

    private AgentRunListVo toListVo(AiAgentRunDO run) {
        AgentRunListVo vo = new AgentRunListVo();
        copyRun(run, vo);
        return vo;
    }

    private void copyRun(AiAgentRunDO run, AgentRunListVo vo) {
        vo.setRunId(run.getId());
        vo.setChatId(run.getChatId());
        vo.setMode(run.getMode());
        vo.setGoal(run.getGoal());
        vo.setStatus(run.getStatus());
        vo.setModel(run.getModel());
        vo.setToolCallCount(run.getToolCallCount());
        vo.setTotalTokenCount(run.getTotalTokenCount());
        vo.setStartTime(run.getStartTime());
        vo.setEndTime(run.getEndTime());
    }

    private AgentRunStepVo toStepVo(AiAgentStepDO step) {
        AgentRunStepVo vo = new AgentRunStepVo();
        vo.setStepId(step.getId());
        vo.setStepNo(step.getStepNo());
        vo.setType(step.getType());
        vo.setToolName(step.getToolName());
        vo.setArgumentSummary(step.getArgumentSummary());
        vo.setResultSummary(step.getResultSummary());
        vo.setStatus(step.getStatus());
        vo.setDurationMs(step.getDurationMs());
        vo.setErrorType(step.getErrorType());
        vo.setCreateTime(step.getCreateTime());
        return vo;
    }

    private AgentRunEvidenceVo toEvidenceVo(AiAgentEvidenceDO evidence) {
        AgentRunEvidenceVo vo = new AgentRunEvidenceVo();
        vo.setEvidenceId(evidence.getId());
        vo.setSourceType(evidence.getSourceType());
        vo.setArticleId(evidence.getArticleId());
        vo.setChunkIndex(evidence.getChunkIndex());
        vo.setTitle(evidence.getTitle());
        vo.setEvidenceSummary(evidence.getEvidenceSummary());
        vo.setRelevance(evidence.getRelevance());
        return vo;
    }
}
