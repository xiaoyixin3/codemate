package com.github.paicoding.forum.service.chatai.memory;

import lombok.Value;

@Value
public class MemoryIdentity {
    Long userId;
    String chatId;
    String agentMode;

    public static MemoryIdentity parse(Object value) {
        String memoryId = String.valueOf(value);
        int first = memoryId.indexOf(':');
        int last = memoryId.lastIndexOf(':');
        if (first <= 0 || last <= first + 1 || last == memoryId.length() - 1) {
            throw new IllegalArgumentException("Invalid memory id");
        }
        try {
            return new MemoryIdentity(Long.valueOf(memoryId.substring(0, first)),
                    memoryId.substring(first + 1, last), memoryId.substring(last + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid memory owner", e);
        }
    }
}
