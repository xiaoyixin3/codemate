package com.github.paicoding.forum.service.chatai.memory.repository.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiChatMemoryDO;
import com.github.paicoding.forum.service.chatai.memory.repository.mapper.AiChatMemoryMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AiChatMemoryDao extends ServiceImpl<AiChatMemoryMapper, AiChatMemoryDO> {
    public AiChatMemoryDO findByMemoryId(String memoryId) {
        return getOne(Wrappers.<AiChatMemoryDO>lambdaQuery().eq(AiChatMemoryDO::getMemoryId, memoryId), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveState(AiChatMemoryDO state) {
        AiChatMemoryDO existing = findByMemoryId(state.getMemoryId());
        if (existing == null) {
            state.setSummarizedMessageCount(state.getSummarizedMessageCount() == null ? 0 : state.getSummarizedMessageCount());
            save(state);
            return;
        }
        state.setId(existing.getId());
        updateById(state);
    }

    public void removeConversation(Long userId, String chatId) {
        remove(Wrappers.<AiChatMemoryDO>lambdaQuery()
                .eq(AiChatMemoryDO::getUserId, userId).eq(AiChatMemoryDO::getChatId, chatId));
    }
}
