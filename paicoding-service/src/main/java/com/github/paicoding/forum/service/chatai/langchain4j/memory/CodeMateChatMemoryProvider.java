package com.github.paicoding.forum.service.chatai.langchain4j.memory;

import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CodeMateChatMemoryProvider implements ChatMemoryProvider {
    private final ConcurrentMap<Object, ChatMemory> memories = new ConcurrentHashMap<>();
    private final LangChain4jProperties properties;

    public CodeMateChatMemoryProvider(LangChain4jProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId, id -> MessageWindowChatMemory.builder()
                .id(id)
                .maxMessages(properties.getMemoryMaxMessages())
                .alwaysKeepSystemMessageFirst(true)
                .build());
    }

    public void seedIfEmpty(String memoryId, List<ChatItemVo> records, ChatItemVo current) {
        ChatMemory memory = get(memoryId);
        synchronized (memory) {
            if (!memory.messages().isEmpty() || records == null) {
                return;
            }
            List<ChatItemVo> chronological = new ArrayList<>(records);
            Collections.reverse(chronological);
            for (ChatItemVo record : chronological) {
                if (record == current) {
                    continue;
                }
                if (StringUtils.isNotBlank(record.getQuestion())) {
                    memory.add(UserMessage.from(record.getQuestion()));
                }
                if (StringUtils.isNotBlank(record.getAnswer())) {
                    memory.add(AiMessage.from(record.getAnswer()));
                }
            }
        }
    }

    public void evict(String memoryId) {
        memories.remove(memoryId);
    }

    public int activeMemoryCount() {
        return memories.size();
    }
}
