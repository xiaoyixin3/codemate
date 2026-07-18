package com.github.paicoding.forum.service.taskplan.repository.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanStepDO;
import com.github.paicoding.forum.service.taskplan.repository.mapper.AiTaskPlanStepMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AiTaskPlanStepDao extends ServiceImpl<AiTaskPlanStepMapper, AiTaskPlanStepDO> {

    public List<AiTaskPlanStepDO> listByPlanIdAndUserId(Long planId, Long userId) {
        return baseMapper.selectList(Wrappers.<AiTaskPlanStepDO>lambdaQuery()
                .eq(AiTaskPlanStepDO::getPlanId, planId)
                .eq(AiTaskPlanStepDO::getUserId, userId)
                .eq(AiTaskPlanStepDO::getDeleted, YesOrNoEnum.NO.getCode())
                .orderByAsc(AiTaskPlanStepDO::getStepNo));
    }

    public AiTaskPlanStepDO getByIdAndPlanIdAndUserId(Long stepId, Long planId, Long userId) {
        return baseMapper.selectOne(Wrappers.<AiTaskPlanStepDO>lambdaQuery()
                .eq(AiTaskPlanStepDO::getId, stepId)
                .eq(AiTaskPlanStepDO::getPlanId, planId)
                .eq(AiTaskPlanStepDO::getUserId, userId)
                .eq(AiTaskPlanStepDO::getDeleted, YesOrNoEnum.NO.getCode()));
    }
}
