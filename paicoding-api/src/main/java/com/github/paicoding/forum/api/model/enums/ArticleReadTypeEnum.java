package com.github.paicoding.forum.api.model.enums;

import lombok.Getter;
import java.util.Objects;

@Getter
public enum ArticleReadTypeEnum {
    NORMAL(0, "普通阅读"),
    LOGIN(1, "登录阅读"),
    TIME_READ(2, "限时阅读"),
    PAY_READ(4, "付费阅读");

    private final Integer type;
    private final String desc;

    ArticleReadTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static ArticleReadTypeEnum typeOf(Integer type) {
        for (ArticleReadTypeEnum value : values()) {
            if (Objects.equals(type, value.type)) return value;
        }
        return null;
    }
}
