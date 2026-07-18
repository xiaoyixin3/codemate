package com.github.paicoding.forum.service.chatai.rag.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunkDO extends BaseDO {
    private static final long serialVersionUID = 1L;

    private Long articleId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String contentHash;
    private String embedding;
    private String embeddingModel;
    private Integer enabled;
}
