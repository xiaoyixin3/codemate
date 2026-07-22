package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

import com.github.paicoding.forum.service.chatai.agent.AgentMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentFallbackPolicyTest {
    private final AgentFallbackPolicy policy = new AgentFallbackPolicy();

    @Test
    void degradesAgentToRagAndRagToPlainChat() {
        AgentFallbackPolicy.Fallback rag = policy.next(AgentMode.BUG_DIAGNOSIS);
        assertEquals(AgentMode.KNOWLEDGE_QA, rag.getMode());
        assertTrue(rag.getUserNotice().contains("降级"));

        AgentFallbackPolicy.Fallback chat = policy.next(AgentMode.KNOWLEDGE_QA);
        assertEquals(AgentMode.CHAT, chat.getMode());
        assertTrue(chat.getUserNotice().contains("普通问答"));
        assertNull(policy.next(AgentMode.CHAT));
    }
}
