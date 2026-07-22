package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.service.chatai.langchain4j.memory.CodeMateChatMemoryProvider;
import com.github.paicoding.forum.service.user.repository.dao.UserAiHistoryDao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConversationMemoryCleanupServiceTest {
    @Test
    void shouldDeleteAllConversationOwnedMemory() {
        CodeMateChatMemoryProvider provider = mock(CodeMateChatMemoryProvider.class);
        PersistentChatMemoryStore store = mock(PersistentChatMemoryStore.class);
        TechnicalMemoryService technical = mock(TechnicalMemoryService.class);
        UserAiHistoryDao history = mock(UserAiHistoryDao.class);
        ConversationMemoryCleanupService service = new ConversationMemoryCleanupService(provider, store, technical, history);

        service.clear(8L, "chat-x", AISourceEnum.DEEP_SEEK);

        verify(provider).evictConversation(8L, "chat-x");
        verify(store).clearConversation(8L, "chat-x");
        verify(technical).deleteConversationSources(8L, "chat-x");
        verify(history).removeConversation(8L, AISourceEnum.DEEP_SEEK.getCode(), "chat-x");
        assertThatThrownBy(() -> service.clear(null, "chat-x", AISourceEnum.DEEP_SEEK))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
