package com.github.paicoding.forum.service.chatai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface CodeMateStreamingAssistant {
    TokenStream chat(@MemoryId String memoryId, @UserMessage String message);
}
