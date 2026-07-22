package com.github.paicoding.forum.service.chatai.langchain4j.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public interface ChatModelProvider {
    String name();

    String modelName();

    ChatModelCapabilities capabilities();

    boolean isAvailable();

    ChatModel chatModel();

    StreamingChatModel streamingChatModel();
}
