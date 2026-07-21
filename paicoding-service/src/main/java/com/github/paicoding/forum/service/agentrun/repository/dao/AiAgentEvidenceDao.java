package com.github.paicoding.forum.service.agentrun.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.agentrun.repository.entity.AiAgentEvidenceDO;
import com.github.paicoding.forum.service.agentrun.repository.mapper.AiAgentEvidenceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AiAgentEvidenceDao extends ServiceImpl<AiAgentEvidenceMapper, AiAgentEvidenceDO> {
    public List<AiAgentEvidenceDO> listByRunId(Long runId) {
        return lambdaQuery().eq(AiAgentEvidenceDO::getRunId, runId).orderByAsc(AiAgentEvidenceDO::getId).list();
    }

    public AiAgentEvidenceDO find(Long runId, Long articleId, Integer chunkIndex) {
        return lambdaQuery().eq(AiAgentEvidenceDO::getRunId, runId)
                .eq(AiAgentEvidenceDO::getArticleId, articleId)
                .eq(AiAgentEvidenceDO::getChunkIndex, chunkIndex).one();
    }
}
