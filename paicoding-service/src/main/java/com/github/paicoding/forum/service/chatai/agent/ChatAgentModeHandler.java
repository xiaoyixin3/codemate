package com.github.paicoding.forum.service.chatai.agent;

import org.springframework.stereotype.Component;

@Component
public class ChatAgentModeHandler implements AgentModeHandler {
    @Override
    public AgentMode mode() {
        return AgentMode.CHAT;
    }

    @Override
    public String systemPrompt() {
        return null;
    }

    @Override
    public String validateAndNormalize(String input) {
        return input;
    }

    @Override
    public boolean requiresStructuredOutput() {
        return false;
    }
}
