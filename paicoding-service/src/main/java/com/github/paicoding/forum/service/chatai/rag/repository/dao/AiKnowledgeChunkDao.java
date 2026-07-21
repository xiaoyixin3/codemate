package com.github.paicoding.forum.service.chatai.rag.repository.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.chatai.rag.repository.entity.AiKnowledgeChunkDO;
import com.github.paicoding.forum.service.chatai.rag.repository.mapper.AiKnowledgeChunkMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Collection;

@Repository
public class AiKnowledgeChunkDao extends ServiceImpl<AiKnowledgeChunkMapper, AiKnowledgeChunkDO> {
    @Transactional(rollbackFor = Exception.class)
    public void replaceArticleChunks(Long articleId, List<AiKnowledgeChunkDO> chunks) {
        remove(Wrappers.<AiKnowledgeChunkDO>lambdaQuery().eq(AiKnowledgeChunkDO::getArticleId, articleId));
        for (AiKnowledgeChunkDO chunk : chunks) {
            save(chunk);
        }
    }

    public List<AiKnowledgeChunkDO> listCandidates(String model, String indexVersion, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return list(Wrappers.<AiKnowledgeChunkDO>lambdaQuery()
                .eq(AiKnowledgeChunkDO::getEnabled, 1)
                .eq(AiKnowledgeChunkDO::getEmbeddingModel, model)
                .eq(AiKnowledgeChunkDO::getIndexVersion, indexVersion)
                .orderByDesc(AiKnowledgeChunkDO::getUpdateTime)
                .last("limit " + limit));
    }

    public List<AiKnowledgeChunkDO> listArticleChunks(Long articleId, String model, String indexVersion) {
        return lambdaQuery().eq(AiKnowledgeChunkDO::getArticleId, articleId)
                .eq(AiKnowledgeChunkDO::getEmbeddingModel, model)
                .eq(AiKnowledgeChunkDO::getIndexVersion, indexVersion)
                .orderByAsc(AiKnowledgeChunkDO::getChunkIndex).list();
    }

    public List<AiKnowledgeChunkDO> listKeywordCandidates(String model, String indexVersion,
                                                           Collection<String> terms, int limit) {
        if (terms == null || terms.isEmpty() || limit <= 0) return Collections.emptyList();
        return list(Wrappers.<AiKnowledgeChunkDO>lambdaQuery()
                .eq(AiKnowledgeChunkDO::getEnabled, 1)
                .eq(AiKnowledgeChunkDO::getEmbeddingModel, model)
                .eq(AiKnowledgeChunkDO::getIndexVersion, indexVersion)
                .and(wrapper -> {
                    for (String term : terms) {
                        wrapper.or(nested -> nested.like(AiKnowledgeChunkDO::getTitle, term)
                                .or().like(AiKnowledgeChunkDO::getHeading, term)
                                .or().like(AiKnowledgeChunkDO::getTags, term)
                                .or().like(AiKnowledgeChunkDO::getCategory, term)
                                .or().like(AiKnowledgeChunkDO::getContent, term));
                    }
                }).orderByDesc(AiKnowledgeChunkDO::getArticleUpdatedAt).last("limit " + limit));
    }

    public void removeArticleChunks(Long articleId) {
        remove(Wrappers.<AiKnowledgeChunkDO>lambdaQuery().eq(AiKnowledgeChunkDO::getArticleId, articleId));
    }

    public long countEnabled(String model, String indexVersion) {
        return count(Wrappers.<AiKnowledgeChunkDO>lambdaQuery().eq(AiKnowledgeChunkDO::getEnabled, 1)
                .eq(AiKnowledgeChunkDO::getEmbeddingModel, model)
                .eq(AiKnowledgeChunkDO::getIndexVersion, indexVersion));
    }
}
