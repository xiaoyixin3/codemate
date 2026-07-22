package com.github.paicoding.forum.service.chatai.memory.repository.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.chatai.memory.repository.entity.AiUserTechnicalMemoryDO;
import com.github.paicoding.forum.service.chatai.memory.repository.mapper.AiUserTechnicalMemoryMapper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class AiUserTechnicalMemoryDao extends ServiceImpl<AiUserTechnicalMemoryMapper, AiUserTechnicalMemoryDO> {
    public List<AiUserTechnicalMemoryDO> listOwned(Long userId, int limit) {
        return list(Wrappers.<AiUserTechnicalMemoryDO>lambdaQuery()
                .eq(AiUserTechnicalMemoryDO::getUserId, userId)
                .orderByDesc(AiUserTechnicalMemoryDO::getUpdateTime)
                .last("limit " + Math.max(1, Math.min(limit, 100))));
    }

    public List<AiUserTechnicalMemoryDO> listActive(Long userId, int limit) {
        return list(Wrappers.<AiUserTechnicalMemoryDO>lambdaQuery()
                .eq(AiUserTechnicalMemoryDO::getUserId, userId)
                .and(q -> q.isNull(AiUserTechnicalMemoryDO::getExpiresAt)
                        .or().gt(AiUserTechnicalMemoryDO::getExpiresAt, new Date()))
                .orderByDesc(AiUserTechnicalMemoryDO::getConfidence)
                .orderByDesc(AiUserTechnicalMemoryDO::getUpdateTime)
                .last("limit " + Math.max(1, Math.min(limit, 100))));
    }

    public AiUserTechnicalMemoryDO findOwned(Long userId, Long id) {
        return getOne(Wrappers.<AiUserTechnicalMemoryDO>lambdaQuery()
                .eq(AiUserTechnicalMemoryDO::getUserId, userId).eq(AiUserTechnicalMemoryDO::getId, id), false);
    }

    public boolean removeOwned(Long userId, Long id) {
        return remove(Wrappers.<AiUserTechnicalMemoryDO>lambdaQuery()
                .eq(AiUserTechnicalMemoryDO::getUserId, userId).eq(AiUserTechnicalMemoryDO::getId, id));
    }

    public void removeConversationSources(Long userId, String chatId) {
        remove(Wrappers.<AiUserTechnicalMemoryDO>lambdaQuery()
                .eq(AiUserTechnicalMemoryDO::getUserId, userId)
                .eq(AiUserTechnicalMemoryDO::getSourceType, "CONVERSATION")
                .eq(AiUserTechnicalMemoryDO::getSourceRef, chatId));
    }
}
