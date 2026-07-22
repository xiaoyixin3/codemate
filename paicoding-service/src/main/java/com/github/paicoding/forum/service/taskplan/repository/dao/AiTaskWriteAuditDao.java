package com.github.paicoding.forum.service.taskplan.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.taskplan.repository.entity.AiTaskWriteAuditDO;
import com.github.paicoding.forum.service.taskplan.repository.mapper.AiTaskWriteAuditMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AiTaskWriteAuditDao extends ServiceImpl<AiTaskWriteAuditMapper, AiTaskWriteAuditDO> {
    public AiTaskWriteAuditDO find(Long userId, String action, String idempotencyKey) {
        return lambdaQuery().eq(AiTaskWriteAuditDO::getUserId, userId)
                .eq(AiTaskWriteAuditDO::getAction, action)
                .eq(AiTaskWriteAuditDO::getIdempotencyKey, idempotencyKey).one();
    }
}
