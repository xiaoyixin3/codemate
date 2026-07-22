package com.github.paicoding.forum.service.chatai.langchain4j.model;

import lombok.Value;

@Value
public class ChatModelCapabilities {
    boolean streaming;
    boolean toolCalling;
    boolean structuredOutput;
    int contextWindowTokens;
}
