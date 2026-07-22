package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.memory.repository.dao.AiChatMemoryDao;
import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiChatMemoryDO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {
    private final AiChatMemoryDao memoryDao;
    private final LangChain4jProperties properties;
    private final SensitiveMemoryGuard sensitiveGuard;

    @Autowired
    public PersistentChatMemoryStore(AiChatMemoryDao memoryDao, LangChain4jProperties properties,
                                     SensitiveMemoryGuard sensitiveGuard) {
        this.memoryDao = memoryDao;
        this.properties = properties;
        this.sensitiveGuard = sensitiveGuard;
    }

    /** Test-friendly constructor. */
    public PersistentChatMemoryStore(AiChatMemoryDao memoryDao, LangChain4jProperties properties) {
        this.memoryDao = memoryDao;
        this.properties = properties;
        this.sensitiveGuard = new SensitiveMemoryGuard();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        AiChatMemoryDO state = memoryDao.findByMemoryId(String.valueOf(memoryId));
        return state == null ? Collections.emptyList() : deserialize(state.getMessagesJson());
    }

    @Override
    public synchronized void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);
        MemoryIdentity identity = MemoryIdentity.parse(id);
        AiChatMemoryDO existing = memoryDao.findByMemoryId(id);
        List<ChatMessage> previous = existing == null ? Collections.emptyList() : deserialize(existing.getMessagesJson());
        String safeMessagesJson = sensitiveGuard.redact(ChatMessageSerializer.messagesToJson(messages));
        List<ChatMessage> safeMessages = deserialize(safeMessagesJson);
        List<ChatMessage> dropped = droppedMessages(previous, safeMessages);

        AiChatMemoryDO state = existing == null ? new AiChatMemoryDO() : existing;
        state.setMemoryId(id);
        state.setUserId(identity.getUserId());
        state.setChatId(identity.getChatId());
        state.setAgentMode(identity.getAgentMode());
        state.setMessagesJson(safeMessagesJson);
        if (!dropped.isEmpty()) {
            state.setConversationSummary(mergeSummary(state.getConversationSummary(), dropped));
            state.setSummarizedMessageCount((state.getSummarizedMessageCount() == null ? 0
                    : state.getSummarizedMessageCount()) + dropped.size());
        }
        memoryDao.saveState(state);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        AiChatMemoryDO state = memoryDao.findByMemoryId(String.valueOf(memoryId));
        if (state != null) memoryDao.removeById(state.getId());
    }

    public String getSummary(String memoryId) {
        AiChatMemoryDO state = memoryDao.findByMemoryId(memoryId);
        return state == null ? null : state.getConversationSummary();
    }

    public void clearConversation(Long userId, String chatId) {
        memoryDao.removeConversation(userId, chatId);
    }

    private List<ChatMessage> deserialize(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    private List<ChatMessage> droppedMessages(List<ChatMessage> previous, List<ChatMessage> current) {
        Map<String, Integer> remaining = new HashMap<>();
        for (ChatMessage message : current) {
            String key = ChatMessageSerializer.messageToJson(message);
            remaining.put(key, remaining.getOrDefault(key, 0) + 1);
        }
        List<ChatMessage> dropped = new ArrayList<>();
        for (ChatMessage message : previous) {
            String key = ChatMessageSerializer.messageToJson(message);
            int count = remaining.getOrDefault(key, 0);
            if (count > 0) {
                remaining.put(key, count - 1);
            } else if (message.type() != ChatMessageType.SYSTEM) {
                dropped.add(message);
            }
        }
        return dropped;
    }

    private String mergeSummary(String previous, List<ChatMessage> dropped) {
        StringBuilder result = new StringBuilder(StringUtils.defaultString(previous));
        for (ChatMessage message : dropped) {
            String text = readableText(message);
            if (StringUtils.isBlank(text)) continue;
            if (result.length() > 0) result.append('\n');
            result.append(message.type() == ChatMessageType.USER ? "User: " : "Assistant: ")
                    .append(StringUtils.normalizeSpace(text));
        }
        int max = Math.max(500, properties.getMemorySummaryMaxChars());
        return result.length() <= max ? result.toString() : result.substring(result.length() - max);
    }

    private String readableText(ChatMessage message) {
        if (message instanceof UserMessage) return ((UserMessage) message).singleText();
        if (message instanceof AiMessage) return ((AiMessage) message).text();
        return null;
    }
}
