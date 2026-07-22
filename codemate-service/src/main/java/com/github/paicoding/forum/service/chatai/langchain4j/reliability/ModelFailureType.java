package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

public enum ModelFailureType {
    RETRIABLE_NETWORK,
    RATE_LIMIT,
    MODEL,
    BUSINESS
}
