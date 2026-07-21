package com.github.paicoding.forum.service.agentrun.service;

import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentRunStateMachineTest {
    @Test
    void acceptsExpectedLifecycle() {
        assertDoesNotThrow(() -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.CREATED, AgentRunStatusEnum.PLANNING));
        assertDoesNotThrow(() -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.PLANNING, AgentRunStatusEnum.EXECUTING));
        assertDoesNotThrow(() -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.EXECUTING, AgentRunStatusEnum.COMPLETED));
        assertDoesNotThrow(() -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.EXECUTING, AgentRunStatusEnum.WAITING_CONFIRMATION));
        assertDoesNotThrow(() -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.WAITING_CONFIRMATION, AgentRunStatusEnum.EXECUTING));
    }

    @Test
    void rejectsIllegalAndTerminalTransitions() {
        assertThrows(IllegalStateException.class, () -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.CREATED, AgentRunStatusEnum.COMPLETED));
        assertThrows(IllegalStateException.class, () -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.COMPLETED, AgentRunStatusEnum.EXECUTING));
        assertThrows(IllegalStateException.class, () -> AgentRunStateMachine.validate(
                AgentRunStatusEnum.EXECUTING, AgentRunStatusEnum.EXECUTING));
    }
}
