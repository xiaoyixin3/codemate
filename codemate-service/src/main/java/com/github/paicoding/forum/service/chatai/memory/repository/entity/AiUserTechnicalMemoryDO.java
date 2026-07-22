package com.github.paicoding.forum.service.chatai.memory.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_user_technical_memory")
public class AiUserTechnicalMemoryDO extends BaseDO {
    private Long userId;
    private String memoryType;
    private String content;
    private String sourceType;
    private String sourceRef;
    private Double confidence;
    private Date expiresAt;
}
