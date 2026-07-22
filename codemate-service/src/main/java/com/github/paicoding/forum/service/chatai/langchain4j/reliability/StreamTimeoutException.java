package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

public class StreamTimeoutException extends RuntimeException {
    public StreamTimeoutException(String message) {
        super(message);
    }
}
