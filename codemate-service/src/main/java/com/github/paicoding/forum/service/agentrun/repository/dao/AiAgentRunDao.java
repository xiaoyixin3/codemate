package com.github.paicoding.forum.service.agentrun.repository.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentRunDO;
import com.github.paicoding.forum.service.agentrun.repository.mapper.AiAgentRunMapper;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Repository
public class AiAgentRunDao extends ServiceImpl<AiAgentRunMapper, AiAgentRunDO> {
    public AiAgentRunDO getByIdAndUserId(Long runId, Long userId) {
        return lambdaQuery().eq(AiAgentRunDO::getId, runId).eq(AiAgentRunDO::getUserId, userId).one();
    }

    public AiAgentRunDO getByIdForUpdate(Long runId) {
        return lambdaQuery().eq(AiAgentRunDO::getId, runId).last("FOR UPDATE").one();
    }

    public List<AiAgentRunDO> listByUserId(Long userId, int limit) {
        return lambdaQuery().eq(AiAgentRunDO::getUserId, userId)
                .orderByDesc(AiAgentRunDO::getId).last("limit " + limit).list();
    }

    public boolean updateStatus(Long runId, AgentRunStatusEnum target, Date endTime, String failureReason,
                                AgentRunStatusEnum... expected) {
        return update(Wrappers.<AiAgentRunDO>lambdaUpdate()
                .eq(AiAgentRunDO::getId, runId)
                .in(AiAgentRunDO::getStatus, Arrays.stream(expected).map(Enum::name).toArray())
                .set(AiAgentRunDO::getStatus, target.name())
                .set(AiAgentRunDO::getEndTime, endTime)
                .set(AiAgentRunDO::getFailureReason, failureReason)
                .set(AiAgentRunDO::getUpdateTime, new Date()));
    }

    public List<AiAgentRunDO> listNonTerminal(int limit) {
        return lambdaQuery().in(AiAgentRunDO::getStatus,
                        AgentRunStatusEnum.CREATED.name(), AgentRunStatusEnum.PLANNING.name(),
                        AgentRunStatusEnum.WAITING_CONFIRMATION.name(), AgentRunStatusEnum.EXECUTING.name())
                .isNotNull(AiAgentRunDO::getStartTime)
                .orderByAsc(AiAgentRunDO::getStartTime).last("limit " + limit).list();
    }
}
