package com.github.paicoding.forum.service.chatai.memory;

import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemoryCreateReq;
import com.github.paicoding.forum.api.model.vo.memory.TechnicalMemorySaveReq;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.chatai.memory.repository.dao.AiUserTechnicalMemoryDao;
import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiUserTechnicalMemoryDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicalMemoryServiceTest {
    private final AiUserTechnicalMemoryDao dao = mock(AiUserTechnicalMemoryDao.class);
    private final TechnicalMemoryService service = new TechnicalMemoryService(
            dao, new SensitiveMemoryGuard(), new LangChain4jProperties());

    @Test
    void shouldRejectCredentialsAndNonTechnicalTypes() {
        TechnicalMemoryCreateReq secret = create("TECH_STACK", "api_key=credential-value-for-test");
        assertThatThrownBy(() -> service.create(1L, secret)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensitive");

        TechnicalMemoryCreateReq unsupported = create("PERSONAL_ADDRESS", "Lives in Shanghai");
        assertThatThrownBy(() -> service.create(1L, unsupported)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void shouldScopeUpdateAndDeleteToCurrentUser() {
        TechnicalMemorySaveReq update = new TechnicalMemorySaveReq();
        update.setMemoryType("LANGUAGE");
        update.setContent("Prefer Java 17");
        when(dao.findOwned(2L, 9L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(2L, 9L, update)).isInstanceOf(IllegalArgumentException.class);

        when(dao.removeOwned(2L, 9L)).thenReturn(true);
        service.delete(2L, 9L);
        verify(dao).removeOwned(2L, 9L);
    }

    private TechnicalMemoryCreateReq create(String type, String content) {
        TechnicalMemoryCreateReq request = new TechnicalMemoryCreateReq();
        request.setMemoryType(type);
        request.setContent(content);
        return request;
    }
}
