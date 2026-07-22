package com.github.paicoding.forum.api.model.enums.ai;

public enum AgentRunStatusEnum {
    CREATED,
    PLANNING,
    WAITING_CONFIRMATION,
    EXECUTING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
