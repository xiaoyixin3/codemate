package com.github.paicoding.forum.api.model.enums.ai;

import lombok.Getter;

/**
 * AI任务计划状态
 */
@Getter
public enum AiTaskPlanStatusEnum {
    DRAFT(0, "草稿"),
    TODO(1, "待办"),
    IN_PROGRESS(2, "执行中"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消");

    private final Integer code;
    private final String desc;

    AiTaskPlanStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
