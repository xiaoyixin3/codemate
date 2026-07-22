package com.github.paicoding.forum.api.model.enums.ai;

import lombok.Getter;

/**
 * AI任务计划步骤状态
 */
@Getter
public enum AiTaskPlanStepStatusEnum {
    TODO(0, "待办"),
    IN_PROGRESS(1, "执行中"),
    COMPLETED(2, "已完成"),
    BLOCKED(3, "阻塞"),
    SKIPPED(4, "跳过");

    private final Integer code;
    private final String desc;

    AiTaskPlanStepStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AiTaskPlanStepStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AiTaskPlanStepStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
