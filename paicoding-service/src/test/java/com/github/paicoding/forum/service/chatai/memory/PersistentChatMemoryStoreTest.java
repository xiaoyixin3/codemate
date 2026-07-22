package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.langchain4j.memory.CodeMateChatMemoryProvider;
import com.github.paicoding.forum.service.chatai.memory.repository.dao.AiChatMemoryDao;
import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiChatMemoryDO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentChatMemoryStoreTest {

    @Test
    void shouldRecoverRecentMessagesAcrossProviderInstances() {
        LangChain4jProperties properties = new LangChain4jProperties();
        properties.setMemoryMaxMessages(4);
        AtomicReference<AiChatMemoryDO> database = new AtomicReference<>();
        AiChatMemoryDao dao = backingDao(database);
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(dao, properties);

        CodeMateChatMemoryProvider first = new CodeMateChatMemoryProvider(properties, store);
        first.get("7:chat-a:CHAT").add(UserMessage.from("Spring Boot 版本是多少？"));

        CodeMateChatMemoryProvider restarted = new CodeMateChatMemoryProvider(properties, store);
        ChatMemory recovered = restarted.get("7:chat-a:CHAT");
        assertThat(recovered.messages()).hasSize(1);
        assertThat(((UserMessage) recovered.messages().get(0)).singleText()).contains("Spring Boot");
    }

    @Test
    void shouldSummarizeMessagesDroppedFromWindow() {
        LangChain4jProperties properties = new LangChain4jProperties();
        AiChatMemoryDO existing = new AiChatMemoryDO();
        existing.setId(1L);
        existing.setMemoryId("7:chat-a:CHAT");
        existing.setUserId(7L);
        existing.setChatId("chat-a");
        existing.setAgentMode("CHAT");
        existing.setSummarizedMessageCount(0);
        existing.setMessagesJson(ChatMessageSerializer.messagesToJson(Arrays.asList(
                UserMessage.from("old question"), AiMessage.from("old answer"), UserMessage.from("recent"))));
        AtomicReference<AiChatMemoryDO> database = new AtomicReference<>(existing);
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(backingDao(database), properties);

        store.updateMessages("7:chat-a:CHAT", Arrays.<ChatMessage>asList(
                AiMessage.from("old answer"), UserMessage.from("recent")));

        assertThat(database.get().getConversationSummary()).contains("old question");
        assertThat(database.get().getSummarizedMessageCount()).isEqualTo(1);
    }

    @Test
    void shouldRedactCredentialsBeforePersistence() {
        LangChain4jProperties properties = new LangChain4jProperties();
        AtomicReference<AiChatMemoryDO> database = new AtomicReference<>();
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(backingDao(database), properties);

        store.updateMessages("7:chat-a:CHAT", Arrays.<ChatMessage>asList(
                UserMessage.from("password=hello123 and token=secret-token-value")));

        assertThat(database.get().getMessagesJson()).contains("[REDACTED]")
                .doesNotContain("hello123").doesNotContain("secret-token-value");
    }

    private AiChatMemoryDao backingDao(AtomicReference<AiChatMemoryDO> database) {
        AiChatMemoryDao dao = mock(AiChatMemoryDao.class);
        when(dao.findByMemoryId(any())).thenAnswer(invocation -> database.get());
        doAnswer(invocation -> {
            database.set(invocation.getArgument(0));
            return null;
        }).when(dao).saveState(any());
        return dao;
    }
}
