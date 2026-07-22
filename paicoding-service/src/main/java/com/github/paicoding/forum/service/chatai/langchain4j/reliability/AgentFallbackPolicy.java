package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

import com.github.paicoding.forum.service.chatai.agent.AgentMode;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentFallbackPolicy {
    public Fallback next(AgentMode failedMode) {
        if (failedMode == AgentMode.CHAT) return null;
        if (failedMode == AgentMode.KNOWLEDGE_QA) {
            return new Fallback(AgentMode.CHAT, "站内知识检索暂不可用，已降级为普通问答；回答可能不包含站内证据。");
        }
        return new Fallback(AgentMode.KNOWLEDGE_QA, "Agent 执行失败，已降级为站内知识检索问答。");
    }

    @Value
    public static class Fallback {
        AgentMode mode;
        String userNotice;
    }
}
