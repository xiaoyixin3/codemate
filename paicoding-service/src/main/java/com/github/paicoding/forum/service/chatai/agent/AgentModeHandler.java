package com.github.paicoding.forum.service.chatai.agent;

import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.api.model.vo.chat.ChatRecordsVo;

/** Defines only the differences between CodeMate modes; request streaming stays in DeepSeekIntegration. */
public interface AgentModeHandler {
    AgentMode mode();

    String systemPrompt();

    default String systemPrompt(String normalizedInput) {
        return systemPrompt();
    }

    String validateAndNormalize(String input);

    boolean requiresStructuredOutput();

    default String displayQuestion(String normalizedInput) {
        return normalizedInput;
    }

    default void transformResult(Long userId, String chatId, ChatRecordsVo response, ChatItemVo item) {
        // Plain chat modes return the model result unchanged.
    }
}
