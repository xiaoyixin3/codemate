package com.github.paicoding.forum.service.chatai.memory.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_memory")
public class AiChatMemoryDO extends BaseDO {
    private String memoryId;
    private Long userId;
    private String chatId;
    private String agentMode;
    private String messagesJson;
    private String conversationSummary;
    private Integer summarizedMessageCount;
}
