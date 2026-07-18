package com.github.paicoding.forum.service.chatai.rag.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.paicoding.forum.service.chatai.rag.repository.entity.AiKnowledgeChunkDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunkDO> {
}
