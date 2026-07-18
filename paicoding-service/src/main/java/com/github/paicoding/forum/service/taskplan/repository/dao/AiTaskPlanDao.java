package com.github.paicoding.forum.service.taskplan.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskPlanDO;
import com.github.paicoding.forum.service.taskplan.repository.mapper.AiTaskPlanMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AiTaskPlanDao extends ServiceImpl<AiTaskPlanMapper, AiTaskPlanDO> {

    public AiTaskPlanDO getByIdAndUserId(Long planId, Long userId) {
        return baseMapper.selectOne(Wrappers.<AiTaskPlanDO>lambdaQuery()
                .eq(AiTaskPlanDO::getId, planId)
                .eq(AiTaskPlanDO::getUserId, userId)
                .eq(AiTaskPlanDO::getDeleted, YesOrNoEnum.NO.getCode()));
    }

    public List<AiTaskPlanDO> listByUserId(Long userId) {
        return baseMapper.selectList(Wrappers.<AiTaskPlanDO>lambdaQuery()
                .eq(AiTaskPlanDO::getUserId, userId)
                .eq(AiTaskPlanDO::getDeleted, YesOrNoEnum.NO.getCode())
                .orderByDesc(AiTaskPlanDO::getUpdateTime));
    }

    /** 在状态变更事务中锁住计划行，串行化同一计划的步骤状态更新。 */
    public AiTaskPlanDO getByIdAndUserIdForUpdate(Long planId, Long userId) {
        return baseMapper.selectOne(Wrappers.<AiTaskPlanDO>lambdaQuery()
                .eq(AiTaskPlanDO::getId, planId)
                .eq(AiTaskPlanDO::getUserId, userId)
                .eq(AiTaskPlanDO::getDeleted, YesOrNoEnum.NO.getCode())
                .last("FOR UPDATE"));
    }

    public AiTaskPlanDO getByUserIdAndPlanKey(Long userId, String planKey) {
        return baseMapper.selectOne(Wrappers.<AiTaskPlanDO>lambdaQuery()
                .eq(AiTaskPlanDO::getUserId, userId)
                .eq(AiTaskPlanDO::getPlanKey, planKey)
                .eq(AiTaskPlanDO::getDeleted, YesOrNoEnum.NO.getCode()));
    }
}
