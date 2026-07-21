package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.service.chatai.langchain4j.memory.CodeMateChatMemoryProvider;
import com.github.paicoding.forum.service.user.repository.dao.UserAiHistoryDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationMemoryCleanupService {
    private final CodeMateChatMemoryProvider chatMemoryProvider;
    private final PersistentChatMemoryStore persistentChatMemoryStore;
    private final TechnicalMemoryService technicalMemoryService;
    private final UserAiHistoryDao userAiHistoryDao;

    public ConversationMemoryCleanupService(CodeMateChatMemoryProvider chatMemoryProvider,
                                            PersistentChatMemoryStore persistentChatMemoryStore,
                                            TechnicalMemoryService technicalMemoryService,
                                            UserAiHistoryDao userAiHistoryDao) {
        this.chatMemoryProvider = chatMemoryProvider;
        this.persistentChatMemoryStore = persistentChatMemoryStore;
        this.technicalMemoryService = technicalMemoryService;
        this.userAiHistoryDao = userAiHistoryDao;
    }

    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId, String chatId, AISourceEnum source) {
        if (userId == null || userId <= 0 || StringUtils.isBlank(chatId) || source == null) {
            throw new IllegalArgumentException("Authenticated user, chat id and AI source are required");
        }
        chatMemoryProvider.evictConversation(userId, chatId);
        persistentChatMemoryStore.clearConversation(userId, chatId);
        technicalMemoryService.deleteConversationSources(userId, chatId);
        userAiHistoryDao.removeConversation(userId, source.getCode(), chatId);
    }
}
