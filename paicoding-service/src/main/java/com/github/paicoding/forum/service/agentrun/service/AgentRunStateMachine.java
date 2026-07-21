package com.github.paicoding.forum.service.agentrun.service;

import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;

import java.util.EnumSet;

public final class AgentRunStateMachine {
    private AgentRunStateMachine() {
    }

    public static void validate(AgentRunStatusEnum current, AgentRunStatusEnum target) {
        if (current == null || target == null || current == target || current.isTerminal()) {
            throw new IllegalStateException("Illegal Agent Run status transition: " + current + " -> " + target);
        }
        boolean allowed = (current == AgentRunStatusEnum.CREATED
                && EnumSet.of(AgentRunStatusEnum.PLANNING, AgentRunStatusEnum.EXECUTING,
                AgentRunStatusEnum.FAILED, AgentRunStatusEnum.CANCELLED).contains(target))
                || (current == AgentRunStatusEnum.PLANNING
                && EnumSet.of(AgentRunStatusEnum.WAITING_CONFIRMATION, AgentRunStatusEnum.EXECUTING,
                AgentRunStatusEnum.FAILED, AgentRunStatusEnum.CANCELLED).contains(target))
                || (current == AgentRunStatusEnum.WAITING_CONFIRMATION
                && EnumSet.of(AgentRunStatusEnum.EXECUTING, AgentRunStatusEnum.FAILED,
                AgentRunStatusEnum.CANCELLED).contains(target))
                || (current == AgentRunStatusEnum.EXECUTING
                && EnumSet.of(AgentRunStatusEnum.WAITING_CONFIRMATION, AgentRunStatusEnum.COMPLETED, AgentRunStatusEnum.FAILED,
                AgentRunStatusEnum.CANCELLED).contains(target));
        if (!allowed) {
            throw new IllegalStateException("Illegal Agent Run status transition: " + current + " -> " + target);
        }
    }
}
