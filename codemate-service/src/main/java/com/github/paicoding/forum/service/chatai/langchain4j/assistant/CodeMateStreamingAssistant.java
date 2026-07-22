package com.github.paicoding.forum.service.chatai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CodeMateStreamingAssistant {
    @SystemMessage("{{systemMessage}}")
    TokenStream chat(@MemoryId String memoryId, @V("systemMessage") String systemMessage,
                     @UserMessage String message);
}
