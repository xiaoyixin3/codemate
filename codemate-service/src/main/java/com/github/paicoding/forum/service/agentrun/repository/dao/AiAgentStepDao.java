package com.github.paicoding.forum.service.agentrun.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.paicoding.forum.api.model.enums.ai.AgentStepTypeEnum;
import com.github.paicoding.forum.api.model.enums.ai.AgentStepStatusEnum;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentStepDO;
import com.github.paicoding.forum.service.agentrun.repository.mapper.AiAgentStepMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date;

@Repository
public class AiAgentStepDao extends ServiceImpl<AiAgentStepMapper, AiAgentStepDO> {
    public List<AiAgentStepDO> listByRunId(Long runId) {
        return lambdaQuery().eq(AiAgentStepDO::getRunId, runId).orderByAsc(AiAgentStepDO::getStepNo).list();
    }

    public AiAgentStepDO getLastToolStep(Long runId) {
        return lambdaQuery().eq(AiAgentStepDO::getRunId, runId)
                .eq(AiAgentStepDO::getType, AgentStepTypeEnum.TOOL_CALL.name())
                .orderByDesc(AiAgentStepDO::getStepNo).last("limit 1").one();
    }

    public int nextStepNo(Long runId) {
        return Math.toIntExact(baseMapper.selectCount(Wrappers.<AiAgentStepDO>lambdaQuery()
                .eq(AiAgentStepDO::getRunId, runId))) + 1;
    }

    public void finishExecutingByRunId(Long runId, AgentStepStatusEnum target, String errorType) {
        lambdaUpdate().eq(AiAgentStepDO::getRunId, runId)
                .eq(AiAgentStepDO::getStatus, AgentStepStatusEnum.EXECUTING.name())
                .set(AiAgentStepDO::getStatus, target.name())
                .set(AiAgentStepDO::getErrorType, errorType)
                .set(AiAgentStepDO::getUpdateTime, new Date()).update();
    }
}
