package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiUserTechnicalMemoryDO;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryContextAssemblerTest {
    @Test
    void shouldAssembleSystemPreferencesThenSummary() {
        PersistentChatMemoryStore store = mock(PersistentChatMemoryStore.class);
        TechnicalMemoryService memories = mock(TechnicalMemoryService.class);
        AiUserTechnicalMemoryDO preference = new AiUserTechnicalMemoryDO();
        preference.setMemoryType("LANGUAGE");
        preference.setContent("Prefer Java 17");
        when(memories.listActive(7L)).thenReturn(Collections.singletonList(preference));
        when(store.getSummary("7:chat-a:CHAT")).thenReturn("Previously discussed Redis recovery.");

        String prompt = new MemoryContextAssembler(store, memories)
                .assemble("SYSTEM", 7L, "7:chat-a:CHAT");

        assertThat(prompt.indexOf("SYSTEM")).isLessThan(prompt.indexOf("Prefer Java 17"));
        assertThat(prompt.indexOf("Prefer Java 17")).isLessThan(prompt.indexOf("Previously discussed"));
    }
}
