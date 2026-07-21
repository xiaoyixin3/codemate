package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Binds LangChain4j's framework-injected memory id to a server-authenticated user.
 * The memory id is never supplied by the model, so tools do not need a userId argument.
 */
@Component
public class TrustedToolContextRegistry {
    private final ConcurrentMap<String, Long> users = new ConcurrentHashMap<>();

    public void bind(String memoryId, Long userId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("A valid memory id is required");
        }
        if (userId == null || userId <= 0) {
            users.remove(memoryId);
            return;
        }
        users.put(memoryId, userId);
    }

    public Long requireUserId(String memoryId) {
        Long userId = findUserId(memoryId);
        if (userId == null || userId <= 0) {
            throw new ToolExecutionException("AUTHENTICATION_REQUIRED", "Authenticated tool context is unavailable");
        }
        return userId;
    }

    public Long findUserId(String memoryId) {
        return memoryId == null ? null : users.get(memoryId);
    }

    public void unbind(String memoryId) {
        if (memoryId != null) {
            users.remove(memoryId);
        }
    }
}
