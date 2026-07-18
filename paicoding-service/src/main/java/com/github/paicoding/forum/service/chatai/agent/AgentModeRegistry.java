package com.github.paicoding.forum.service.chatai.agent;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentModeRegistry {
    private final Map<AgentMode, AgentModeHandler> handlers = new EnumMap<>(AgentMode.class);

    public AgentModeRegistry(List<AgentModeHandler> handlerList) {
        for (AgentModeHandler handler : handlerList) {
            if (handlers.put(handler.mode(), handler) != null) {
                throw new IllegalStateException("重复注册 Agent 模式：" + handler.mode());
            }
        }
    }

    public AgentModeHandler get(AgentMode mode) {
        AgentModeHandler handler = handlers.get(mode);
        if (handler == null) {
            throw new IllegalArgumentException("当前 Agent 模式暂未启用：" + mode);
        }
        return handler;
    }
}
