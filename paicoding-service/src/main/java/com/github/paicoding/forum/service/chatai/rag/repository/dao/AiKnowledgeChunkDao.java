package com.github.paicoding.forum.service.chatai.rag.repository.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.chatai.rag.repository.entity.AiKnowledgeChunkDO;
import com.github.paicoding.forum.service.chatai.rag.repository.mapper.AiKnowledgeChunkMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Repository
public class AiKnowledgeChunkDao extends ServiceImpl<AiKnowledgeChunkMapper, AiKnowledgeChunkDO> {
    @Transactional(rollbackFor = Exception.class)
    public void replaceArticleChunks(Long articleId, List<AiKnowledgeChunkDO> chunks) {
        remove(Wrappers.<AiKnowledgeChunkDO>lambdaQuery().eq(AiKnowledgeChunkDO::getArticleId, articleId));
        for (AiKnowledgeChunkDO chunk : chunks) {
            save(chunk);
        }
    }

    public List<AiKnowledgeChunkDO> listCandidates(String model, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return list(Wrappers.<AiKnowledgeChunkDO>lambdaQuery()
                .eq(AiKnowledgeChunkDO::getEnabled, 1)
                .eq(AiKnowledgeChunkDO::getEmbeddingModel, model)
                .orderByDesc(AiKnowledgeChunkDO::getUpdateTime)
                .last("limit " + limit));
    }
}
