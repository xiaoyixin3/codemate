package com.github.paicoding.forum.service.chatai.langchain4j.service;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.tool.ToolExecution;

import java.util.List;

public interface AgentStreamObserver {
    void onToken(String token);

    void onComplete(ChatResponse response);

    void onError(Throwable error);

    default void onRetrieved(List<Content> contents) {
    }

    default void onToolExecuted(ToolExecution execution) {
    }
}
