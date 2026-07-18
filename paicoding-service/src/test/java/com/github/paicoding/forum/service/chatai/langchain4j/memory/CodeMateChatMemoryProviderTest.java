package com.github.paicoding.forum.service.chatai.langchain4j.memory;

import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CodeMateChatMemoryProviderTest {

    @Test
    void shouldSeedHistoryExcludeCurrentAndEvictMemory() {
        LangChain4jProperties properties = new LangChain4jProperties();
        properties.setMemoryMaxMessages(4);
        CodeMateChatMemoryProvider provider = new CodeMateChatMemoryProvider(properties);
        ChatItemVo current = new ChatItemVo().setQuestion("current");
        ChatItemVo previous = new ChatItemVo().setQuestion("previous").setAnswer("answer");

        provider.seedIfEmpty("u:c:CHAT", Arrays.asList(current, previous), current);
        ChatMemory memory = provider.get("u:c:CHAT");

        assertThat(memory.messages()).hasSize(2);
        assertThat(provider.activeMemoryCount()).isEqualTo(1);
        memory.add(UserMessage.from("one"));
        memory.add(UserMessage.from("two"));
        memory.add(UserMessage.from("three"));
        assertThat(memory.messages()).hasSizeLessThanOrEqualTo(4);

        provider.evict("u:c:CHAT");
        assertThat(provider.activeMemoryCount()).isZero();
    }
}
