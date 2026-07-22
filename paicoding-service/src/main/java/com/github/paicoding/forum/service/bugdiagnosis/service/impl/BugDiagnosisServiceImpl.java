package com.github.paicoding.forum.service.bugdiagnosis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisCauseDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisEvidenceDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisModelResponseDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisPreviewVo;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelResponseDTO;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelStepDTO;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepCreateReq;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import com.github.paicoding.forum.service.bugdiagnosis.repository.dao.AiBugDiagnosisDao;
import com.github.paicoding.forum.service.bugdiagnosis.repository.entity.AiBugDiagnosisDO;
import com.github.paicoding.forum.service.bugdiagnosis.service.BugDiagnosisService;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskWriteAuditDao;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskWriteAuditDO;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class BugDiagnosisServiceImpl implements BugDiagnosisService {
    private static final String PREVIEW = "PREVIEW";
    private static final String CONFIRMED = "CONFIRMED";
    private final ObjectMapper objectMapper;
    private final AiBugDiagnosisDao diagnosisDao;
    private final AiTaskWriteAuditDao auditDao;
    private final AiTaskPlanService taskPlanService;
    private final AgentRunService agentRunService;
    private final ConcurrentMap<Long, Object> confirmationLocks = new ConcurrentHashMap<>();

    public BugDiagnosisServiceImpl(ObjectMapper objectMapper, AiBugDiagnosisDao diagnosisDao,
                                   AiTaskWriteAuditDao auditDao, AiTaskPlanService taskPlanService,
                                   AgentRunService agentRunService) {
        this.objectMapper = objectMapper;
        this.diagnosisDao = diagnosisDao;
        this.auditDao = auditDao;
        this.taskPlanService = taskPlanService;
        this.agentRunService = agentRunService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPreview(Long userId, Long runId, String chatId, String rawJson) {
        checkUser(userId);
        AgentRunDetailVo run = agentRunService.detail(userId, runId);
        if (!"BUG_DIAGNOSIS".equals(run.getMode())) {
            throw new IllegalArgumentException("Agent Run 不是 Bug 诊断模式");
        }
        AiBugDiagnosisDO existing = diagnosisDao.getByRunId(runId);
        if (existing != null) {
            return existing.getId();
        }
        String json = unwrap(rawJson);
        BugDiagnosisModelResponseDTO model = parseAndValidate(json);
        AiBugDiagnosisDO diagnosis = new AiBugDiagnosisDO();
        diagnosis.setRunId(runId);
        diagnosis.setUserId(userId);
        diagnosis.setChatId(StringUtils.left(StringUtils.trimToEmpty(chatId), 64));
        diagnosis.setProblemSummary(model.getProblemSummary().trim());
        diagnosis.setDiagnosisJson(json);
        diagnosis.setStatus(PREVIEW);
        Date now = new Date();
        diagnosis.setCreateTime(now);
        diagnosis.setUpdateTime(now);
        try {
            diagnosisDao.save(diagnosis);
        } catch (DuplicateKeyException e) {
            return diagnosisDao.getByRunId(runId).getId();
        }
        return diagnosis.getId();
    }

    @Override
    public BugDiagnosisPreviewVo preview(Long userId, Long diagnosisId) {
        AiBugDiagnosisDO diagnosis = requireOwned(userId, diagnosisId, false);
        BugDiagnosisModelResponseDTO model = parseAndValidate(diagnosis.getDiagnosisJson());
        BugDiagnosisPreviewVo vo = objectMapper.convertValue(model, BugDiagnosisPreviewVo.class);
        vo.setDiagnosisId(diagnosis.getId());
        vo.setAgentRunId(diagnosis.getRunId());
        vo.setStatus(diagnosis.getStatus());
        vo.setPlanId(diagnosis.getPlanId());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BugDiagnosisConfirmVo confirm(Long userId, Long diagnosisId, String idempotencyKey) {
        String key = checkKey(idempotencyKey);
        Object lock = confirmationLocks.computeIfAbsent(diagnosisId, ignored -> new Object());
        synchronized (lock) {
            try {
                AiBugDiagnosisDO diagnosis = requireOwned(userId, diagnosisId, true);
                if (CONFIRMED.equals(diagnosis.getStatus()) && diagnosis.getPlanId() != null) {
                    return confirmed(diagnosis);
                }
                AgentRunDetailVo run = agentRunService.detail(userId, diagnosis.getRunId());
                if (!AgentRunStatusEnum.WAITING_CONFIRMATION.name().equals(run.getStatus())
                        || !agentRunService.transition(diagnosis.getRunId(), AgentRunStatusEnum.EXECUTING)) {
                    throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED,
                            "该诊断已取消、失效或不再等待确认");
                }
                BugDiagnosisModelResponseDTO model = parseAndValidate(diagnosis.getDiagnosisJson());
                AiTaskPlanCreateReq req = toPlan(diagnosis, model);
                Long planId = taskPlanService.createPlan(userId, req);

                Date now = new Date();
                diagnosis.setStatus(CONFIRMED);
                diagnosis.setConfirmIdempotencyKey(key);
                diagnosis.setPlanId(planId);
                diagnosis.setConfirmedTime(now);
                diagnosis.setUpdateTime(now);
                diagnosisDao.updateById(diagnosis);
                saveAudit(userId, diagnosis.getRunId(), planId, null, "createTaskPlan", key,
                        "根据 Bug 诊断预览创建修复计划");

                agentRunService.complete(diagnosis.getRunId(), null, null, null);
                return confirmed(diagnosis);
            } finally {
                confirmationLocks.remove(diagnosisId, lock);
            }
        }
    }

    private AiTaskPlanCreateReq toPlan(AiBugDiagnosisDO diagnosis, BugDiagnosisModelResponseDTO model) {
        AiTaskPlanModelResponseDTO extra = new AiTaskPlanModelResponseDTO();
        extra.setTitle(StringUtils.left("修复：" + model.getProblemSummary(), 128));
        extra.setGoal(model.getProblemSummary());
        extra.setScope("基于诊断证据验证根因、实施最小修复并完成回归验证");
        extra.setSummary(causeSummary(model.getCauseHypotheses()));
        extra.setRisks(model.getCauseHypotheses().stream().map(BugDiagnosisCauseDTO::getHypothesis)
                .collect(Collectors.toList()));
        extra.setValidationMethods(model.getVerificationSteps());
        extra.setAcceptanceCriteria(model.getRegressionPlan());

        List<AiTaskPlanStepCreateReq> steps = new ArrayList<>();
        int no = 1;
        for (String verification : model.getVerificationSteps()) {
            steps.add(step("验证根因 " + no++, verification, "获得可复现证据或排除该假设", verification,
                    "验证可能改变测试环境，先使用隔离环境"));
        }
        for (String fix : model.getFixSuggestions()) {
            steps.add(step("实施修复 " + no++, fix, "完成可审查的最小修复", "执行针对性测试并检查日志",
                    "修复前确认影响范围并保留回退方案"));
        }
        for (String regression : model.getRegressionPlan()) {
            steps.add(step("回归验证 " + no++, regression, "确认问题修复且无已知回归", regression,
                    "覆盖关键路径和边界条件"));
        }
        List<AiTaskPlanModelStepDTO> extraSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            AiTaskPlanStepCreateReq source = steps.get(i);
            AiTaskPlanModelStepDTO target = new AiTaskPlanModelStepDTO();
            target.setStepNumber(i + 1);
            target.setTitle(source.getTitle());
            target.setDescription(source.getContent());
            target.setExpectedOutput(source.getExpectedOutput());
            target.setValidationMethod(source.getVerificationMethod());
            target.setRiskNote(source.getRisk());
            extraSteps.add(target);
        }
        extra.setSteps(extraSteps);

        AiTaskPlanCreateReq req = new AiTaskPlanCreateReq();
        req.setTitle(extra.getTitle());
        req.setGoal(extra.getGoal());
        req.setChatId(diagnosis.getChatId());
        req.setSourceAiType(AISourceEnum.DEEP_SEEK.getCode());
        req.setPlanKey("bug-diagnosis:" + diagnosis.getId());
        req.setSteps(steps);
        try {
            req.setExtra(objectMapper.writeValueAsString(extra));
        } catch (Exception e) {
            throw new IllegalStateException("无法序列化修复计划", e);
        }
        return req;
    }

    private AiTaskPlanStepCreateReq step(String title, String content, String output, String verification, String risk) {
        AiTaskPlanStepCreateReq step = new AiTaskPlanStepCreateReq();
        step.setTitle(StringUtils.left(title, 256));
        step.setContent(content);
        step.setExpectedOutput(output);
        step.setVerificationMethod(verification);
        step.setRisk(risk);
        return step;
    }

    private BugDiagnosisModelResponseDTO parseAndValidate(String json) {
        try {
            BugDiagnosisModelResponseDTO model = objectMapper.readValue(json, BugDiagnosisModelResponseDTO.class);
            if (model == null || StringUtils.isBlank(model.getProblemSummary())
                    || empty(model.getCauseHypotheses()) || empty(model.getSupportingEvidence())
                    || empty(model.getVerificationSteps()) || empty(model.getFixSuggestions())
                    || empty(model.getRegressionPlan()) || model.getCauseHypotheses().size() > 10
                    || model.getVerificationSteps().size() + model.getFixSuggestions().size()
                    + model.getRegressionPlan().size() > 30) {
                throw new IllegalArgumentException("Bug 诊断缺少必填字段或步骤过多");
            }
            if (model.getProblemSummary().length() > 1000) {
                throw new IllegalArgumentException("问题摘要过长");
            }
            for (BugDiagnosisCauseDTO cause : model.getCauseHypotheses()) {
                if (cause == null || StringUtils.isBlank(cause.getHypothesis()) || empty(cause.getSupportingEvidence())
                        || cause.getConfidence() == null || cause.getConfidence() < 0 || cause.getConfidence() > 1) {
                    throw new IllegalArgumentException("原因假设结构非法");
                }
            }
            for (BugDiagnosisEvidenceDTO evidence : model.getSupportingEvidence()) {
                if (evidence == null || StringUtils.isBlank(evidence.getTitle())
                        || StringUtils.isBlank(evidence.getExcerpt())) {
                    throw new IllegalArgumentException("支持证据结构非法");
                }
            }
            return model;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bug 诊断不是有效的结构化 JSON", e);
        }
    }

    private String unwrap(String raw) {
        String json = StringUtils.trimToEmpty(raw);
        if (json.startsWith("```")) {
            int newline = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (newline < 0 || end <= newline) {
                throw new IllegalArgumentException("Bug 诊断 JSON 代码块不完整");
            }
            json = json.substring(newline + 1, end).trim();
        }
        return json;
    }

    private AiBugDiagnosisDO requireOwned(Long userId, Long diagnosisId, boolean lock) {
        checkUser(userId);
        if (diagnosisId == null || diagnosisId <= 0) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "非法诊断 ID");
        }
        AiBugDiagnosisDO diagnosis = lock ? diagnosisDao.getOwnedForUpdate(diagnosisId, userId)
                : diagnosisDao.getOwned(diagnosisId, userId);
        if (diagnosis == null) {
            throw ExceptionUtil.of(StatusEnum.RECORDS_NOT_EXISTS, "诊断预览不存在或无权访问");
        }
        return diagnosis;
    }

    private String checkKey(String key) {
        String value = StringUtils.trimToEmpty(key);
        if (value.length() < 8 || value.length() > 64 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "幂等键格式非法");
        }
        return value;
    }

    private void checkUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw ExceptionUtil.of(StatusEnum.FORBID_NOTLOGIN);
        }
    }

    private void saveAudit(Long userId, Long runId, Long planId, Long stepId, String action,
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

    private BugDiagnosisConfirmVo confirmed(AiBugDiagnosisDO diagnosis) {
        BugDiagnosisConfirmVo vo = new BugDiagnosisConfirmVo();
        vo.setDiagnosisId(diagnosis.getId());
        vo.setPlanId(diagnosis.getPlanId());
        vo.setPlanUrl("/task-plan/" + diagnosis.getPlanId());
        return vo;
    }

    private String causeSummary(List<BugDiagnosisCauseDTO> causes) {
        return causes.stream().map(cause -> String.format("%.0f%%：%s", cause.getConfidence() * 100,
                cause.getHypothesis())).collect(Collectors.joining("\n"));
    }

    private boolean empty(List<?> values) {
        return values == null || values.isEmpty();
    }
}
