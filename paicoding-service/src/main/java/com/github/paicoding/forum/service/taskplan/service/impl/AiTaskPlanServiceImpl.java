package com.github.paicoding.forum.service.taskplan.service.impl;

import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AiTaskPlanStepStatusEnum;
import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanDetailVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanListVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanReopenReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskPlanDao;
import com.github.paicoding.forum.service.taskplan.repository.dao.AiTaskPlanStepDao;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanDO;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanStepDO;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanStateMachine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiTaskPlanServiceImpl implements AiTaskPlanService {
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AiTaskPlanDao aiTaskPlanDao;
    @Resource
    private AiTaskPlanStepDao aiTaskPlanStepDao;
    @Value("${ai.task-plan.allow-reopen-completed-step:false}")
    private boolean allowReopenCompletedStep;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(Long userId, AiTaskPlanCreateReq req) {
        checkUserId(userId);
        checkCreateReq(req);
        if (StringUtils.isNotBlank(req.getPlanKey())) {
            AiTaskPlanDO existed = aiTaskPlanDao.getByUserIdAndPlanKey(userId, req.getPlanKey());
            if (existed != null) {
                return existed.getId();
            }
        }
        Date now = new Date();
        AiTaskPlanDO plan = new AiTaskPlanDO();
        plan.setUserId(userId);
        plan.setChatId(req.getChatId());
        plan.setSourceAiType(req.getSourceAiType());
        plan.setPlanKey(req.getPlanKey());
        plan.setTitle(req.getTitle().trim());
        plan.setGoal(req.getGoal());
        plan.setStatus(AiTaskPlanStatusEnum.TODO.getCode());
        plan.setProgress(0);
        plan.setStepTotal(req.getSteps().size());
        plan.setCompletedStepCount(0);
        plan.setDeleted(YesOrNoEnum.NO.getCode());
        plan.setExtra(req.getExtra());
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        aiTaskPlanDao.save(plan);

        int stepNo = 1;
        for (AiTaskPlanStepCreateReq stepReq : req.getSteps()) {
            AiTaskPlanStepDO step = new AiTaskPlanStepDO();
            step.setPlanId(plan.getId());
            step.setUserId(userId);
            step.setStepNo(stepNo++);
            step.setTitle(stepReq.getTitle().trim());
            step.setContent(stepReq.getContent());
            step.setExpectedOutput(stepReq.getExpectedOutput());
            step.setRisk(stepReq.getRisk());
            step.setVerificationMethod(stepReq.getVerificationMethod());
            step.setStatus(AiTaskPlanStepStatusEnum.TODO.getCode());
            step.setExecutorType(0);
            step.setDeleted(YesOrNoEnum.NO.getCode());
            step.setCreateTime(now);
            step.setUpdateTime(now);
            aiTaskPlanStepDao.save(step);
        }
        return plan.getId();
    }

    @Override
    public List<AiTaskPlanListVo> listPlans(Long userId) {
        checkUserId(userId);
        return aiTaskPlanDao.listByUserId(userId).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public AiTaskPlanDetailVo queryPlanDetail(Long userId, Long planId) {
        AiTaskPlanDO plan = getPlanOrThrow(userId, planId);
        AiTaskPlanDetailVo vo = new AiTaskPlanDetailVo();
        vo.setPlanId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setGoal(plan.getGoal());
        fillStructuredFields(plan, vo);
        vo.setChatId(plan.getChatId());
        vo.setSourceAiType(plan.getSourceAiType());
        vo.setStatus(plan.getStatus());
        vo.setProgress(plan.getProgress());
        vo.setStepTotal(plan.getStepTotal());
        vo.setCompletedStepCount(plan.getCompletedStepCount());
        vo.setReopenReason(plan.getReopenReason());
        vo.setReopenTime(plan.getReopenTime());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());
        vo.setSteps(aiTaskPlanStepDao.listByPlanIdAndUserId(planId, userId).stream().map(this::toStepVo).collect(Collectors.toList()));
        return vo;
    }

    /** 旧计划或手工创建计划没有结构化 extra 时仍可正常打开详情页。 */
    private void fillStructuredFields(AiTaskPlanDO plan, AiTaskPlanDetailVo vo) {
        if (StringUtils.isBlank(plan.getExtra())) {
            return;
        }
        try {
            AiTaskPlanModelResponseDTO model = objectMapper.readValue(plan.getExtra(), AiTaskPlanModelResponseDTO.class);
            vo.setScope(model.getScope());
            vo.setSummary(model.getSummary());
            vo.setRisks(model.getRisks());
            vo.setAcceptanceCriteria(model.getAcceptanceCriteria());
        } catch (Exception ignored) {
            // extra 是可选的展示增强字段，解析失败不影响计划主流程。
        }
    }

    @Override
    public void updatePlan(Long userId, Long planId, AiTaskPlanUpdateReq req) {
        checkUserId(userId);
        if (req == null || (StringUtils.isBlank(req.getTitle()) && StringUtils.isBlank(req.getGoal()))) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "标题或说明不能为空");
        }
        AiTaskPlanDO plan = getPlanOrThrow(userId, planId);
        if (StringUtils.isNotBlank(req.getTitle())) {
            if (req.getTitle().trim().length() > 128) {
                throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "标题长度不能超过128");
            }
            plan.setTitle(req.getTitle().trim());
        }
        if (req.getGoal() != null) {
            plan.setGoal(req.getGoal());
        }
        plan.setUpdateTime(new Date());
        aiTaskPlanDao.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long userId, Long planId) {
        AiTaskPlanDO plan = getPlanOrThrow(userId, planId);
        Date now = new Date();
        plan.setDeleted(YesOrNoEnum.YES.getCode());
        plan.setUpdateTime(now);
        aiTaskPlanDao.updateById(plan);
        List<AiTaskPlanStepDO> steps = aiTaskPlanStepDao.listByPlanIdAndUserId(planId, userId);
        for (AiTaskPlanStepDO step : steps) {
            step.setDeleted(YesOrNoEnum.YES.getCode());
            step.setUpdateTime(now);
            aiTaskPlanStepDao.updateById(step);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPlan(Long userId, Long planId) {
        AiTaskPlanDO plan = getPlanOrThrowForUpdate(userId, planId);
        if (!AiTaskPlanStatusEnum.DRAFT.getCode().equals(plan.getStatus())
                && !AiTaskPlanStatusEnum.TODO.getCode().equals(plan.getStatus())) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "当前计划不能开始");
        }
        plan.setStatus(AiTaskPlanStatusEnum.TODO.getCode());
        plan.setUpdateTime(new Date());
        aiTaskPlanDao.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPlan(Long userId, Long planId) {
        AiTaskPlanDO plan = getPlanOrThrowForUpdate(userId, planId);
        if (AiTaskPlanStatusEnum.COMPLETED.getCode().equals(plan.getStatus())
                || AiTaskPlanStatusEnum.CANCELED.getCode().equals(plan.getStatus())) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "当前计划不能取消");
        }
        plan.setStatus(AiTaskPlanStatusEnum.CANCELED.getCode());
        plan.setUpdateTime(new Date());
        aiTaskPlanDao.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStepStatus(Long userId, Long planId, Long stepId, AiTaskPlanStepStatusUpdateReq req) {
        if (req == null || AiTaskPlanStepStatusEnum.fromCode(req.getStatus()) == null) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "非法步骤状态");
        }
        AiTaskPlanDO plan = getPlanOrThrowForUpdate(userId, planId);
        if (AiTaskPlanStatusEnum.CANCELED.getCode().equals(plan.getStatus())) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "已取消计划不能更新步骤");
        }
        AiTaskPlanStepDO step = getStepOrThrow(userId, planId, stepId);
        AiTaskPlanStepStatusEnum target = AiTaskPlanStepStatusEnum.fromCode(req.getStatus());
        AiTaskPlanStepStatusEnum current = AiTaskPlanStepStatusEnum.fromCode(step.getStatus());
        AiTaskPlanStateMachine.validateStepTransition(current, target, allowReopenCompletedStep);
        AiTaskPlanStateMachine.validateRequiredReason(target, req.getBlockedReason(), req.getSkippedReason());
        AiTaskPlanStateMachine.assertNoOtherInProgress(aiTaskPlanStepDao.listByPlanIdAndUserId(planId, userId), stepId, target);
        if (current == null && target == null) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "不允许的步骤状态迁移");
        }

        Date now = new Date();
        step.setStatus(target.getCode());
        if (target == AiTaskPlanStepStatusEnum.IN_PROGRESS && step.getStartedTime() == null) {
            step.setStartedTime(now);
        }
        if (target == AiTaskPlanStepStatusEnum.COMPLETED || target == AiTaskPlanStepStatusEnum.SKIPPED) {
            step.setCompletedTime(now);
        }
        if (current == AiTaskPlanStepStatusEnum.COMPLETED && target == AiTaskPlanStepStatusEnum.IN_PROGRESS) {
            step.setCompletedTime(null);
        }
        if (target == AiTaskPlanStepStatusEnum.BLOCKED) {
            if (StringUtils.isBlank(req.getBlockedReason())) {
                throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "阻塞状态必须提供原因");
            }
            step.setBlockedReason(req.getBlockedReason().trim());
        } else if (target == AiTaskPlanStepStatusEnum.SKIPPED) {
            step.setSkippedReason(req.getSkippedReason().trim());
        } else if (target == AiTaskPlanStepStatusEnum.IN_PROGRESS) {
            step.setBlockedReason(null);
        }
        step.setUpdateTime(now);
        aiTaskPlanStepDao.updateById(step);
        refreshPlanProgress(plan, userId);
    }

    @Override
    public void saveStepResult(Long userId, Long planId, Long stepId, AiTaskPlanStepResultUpdateReq req) {
        checkUserId(userId);
        if (req == null || req.getActualOutput() == null) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "实际执行结果不能为空");
        }
        getPlanOrThrow(userId, planId);
        AiTaskPlanStepDO step = getStepOrThrow(userId, planId, stepId);
        step.setActualOutput(req.getActualOutput());
        step.setUpdateTime(new Date());
        aiTaskPlanStepDao.updateById(step);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenPlan(Long userId, Long planId, AiTaskPlanReopenReq req) {
        checkUserId(userId);
        if (req == null || StringUtils.isBlank(req.getReason())) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "重新开启原因不能为空");
        }
        AiTaskPlanDO plan = getPlanOrThrowForUpdate(userId, planId);
        if (!AiTaskPlanStatusEnum.COMPLETED.getCode().equals(plan.getStatus())) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "仅已完成计划可重新开启");
        }
        plan.setStatus(AiTaskPlanStatusEnum.IN_PROGRESS.getCode());
        plan.setReopenReason(req.getReason());
        plan.setReopenTime(new Date());
        plan.setUpdateTime(plan.getReopenTime());
        aiTaskPlanDao.updateById(plan);
    }

    private void refreshPlanProgress(AiTaskPlanDO plan, Long userId) {
        List<AiTaskPlanStepDO> steps = aiTaskPlanStepDao.listByPlanIdAndUserId(plan.getId(), userId);
        int total = steps.size();
        int completed = 0;
        for (AiTaskPlanStepDO step : steps) {
            Integer status = step.getStatus();
            if (AiTaskPlanStepStatusEnum.COMPLETED.getCode().equals(status)
                    || AiTaskPlanStepStatusEnum.SKIPPED.getCode().equals(status)) {
                completed++;
            }
        }
        plan.setStepTotal(total);
        plan.setCompletedStepCount(completed);
        plan.setProgress(total == 0 ? 0 : completed * 100 / total);
        if (!AiTaskPlanStatusEnum.CANCELED.getCode().equals(plan.getStatus())) {
            plan.setStatus(AiTaskPlanStateMachine.derivePlanStatus(steps).getCode());
        }
        plan.setUpdateTime(new Date());
        aiTaskPlanDao.updateById(plan);
    }

    private AiTaskPlanDO getPlanOrThrow(Long userId, Long planId) {
        checkUserId(userId);
        if (planId == null || planId <= 0) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS);
        }
        AiTaskPlanDO plan = aiTaskPlanDao.getByIdAndUserId(planId, userId);
        if (plan == null) {
            throw ExceptionUtil.of(StatusEnum.RECORDS_NOT_EXISTS, "任务计划不存在");
        }
        return plan;
    }

    private AiTaskPlanDO getPlanOrThrowForUpdate(Long userId, Long planId) {
        checkUserId(userId);
        if (planId == null || planId <= 0) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS);
        }
        AiTaskPlanDO plan = aiTaskPlanDao.getByIdAndUserIdForUpdate(planId, userId);
        if (plan == null) {
            throw ExceptionUtil.of(StatusEnum.RECORDS_NOT_EXISTS, "任务计划不存在");
        }
        return plan;
    }

    private AiTaskPlanStepDO getStepOrThrow(Long userId, Long planId, Long stepId) {
        if (stepId == null || stepId <= 0) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS);
        }
        AiTaskPlanStepDO step = aiTaskPlanStepDao.getByIdAndPlanIdAndUserId(stepId, planId, userId);
        if (step == null) {
            throw ExceptionUtil.of(StatusEnum.RECORDS_NOT_EXISTS, "任务计划步骤不存在");
        }
        return step;
    }

    private void checkCreateReq(AiTaskPlanCreateReq req) {
        if (req == null || StringUtils.isBlank(req.getTitle()) || req.getSteps() == null || req.getSteps().isEmpty()) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "计划标题和步骤不能为空");
        }
        if (req.getTitle().trim().length() > 128 || (req.getChatId() != null && req.getChatId().length() > 128)) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "标题或会话ID长度超限");
        }
        if (req.getSteps().size() > 100) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "步骤数量不能超过100");
        }
        if (req.getSourceAiType() != null && !validAiSource(req.getSourceAiType())) {
            throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "非法AI类型");
        }
        for (AiTaskPlanStepCreateReq step : req.getSteps()) {
            if (step == null || StringUtils.isBlank(step.getTitle()) || step.getTitle().trim().length() > 256) {
                throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "步骤标题不能为空且长度不能超过256");
            }
        }
    }

    private void checkUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ExceptionUtil.of(StatusEnum.FORBID_NOTLOGIN);
        }
    }

    private boolean validAiSource(Integer sourceAiType) {
        for (AISourceEnum source : AISourceEnum.values()) {
            if (source.getCode().equals(sourceAiType)) {
                return true;
            }
        }
        return false;
    }

    private AiTaskPlanListVo toListVo(AiTaskPlanDO plan) {
        AiTaskPlanListVo vo = new AiTaskPlanListVo();
        vo.setPlanId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setStatus(plan.getStatus());
        vo.setProgress(plan.getProgress());
        vo.setStepTotal(plan.getStepTotal());
        vo.setCompletedStepCount(plan.getCompletedStepCount());
        vo.setChatId(plan.getChatId());
        vo.setUpdateTime(plan.getUpdateTime());
        return vo;
    }

    private AiTaskPlanStepVo toStepVo(AiTaskPlanStepDO step) {
        AiTaskPlanStepVo vo = new AiTaskPlanStepVo();
        vo.setStepId(step.getId());
        vo.setStepNo(step.getStepNo());
        vo.setTitle(step.getTitle());
        vo.setContent(step.getContent());
        vo.setStatus(step.getStatus());
        vo.setExpectedOutput(step.getExpectedOutput());
        vo.setActualOutput(step.getActualOutput());
        vo.setRisk(step.getRisk());
        vo.setVerificationMethod(step.getVerificationMethod());
        vo.setBlockedReason(step.getBlockedReason());
        vo.setSkippedReason(step.getSkippedReason());
        vo.setStartedTime(step.getStartedTime());
        vo.setCompletedTime(step.getCompletedTime());
        return vo;
    }
}
