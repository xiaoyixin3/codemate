package com.github.paicoding.forum.api.model.enums.column;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum ColumnArticleReadEnum {
    COLUMN_TYPE(0, "沿用专栏类型"),
    LOGIN(1, "登录阅读"),
    TIME_FREE(2, "限时免费");

    private final int read;
    private final String desc;
    private static final Map<Integer, ColumnArticleReadEnum> CACHE = new HashMap<>();
    static { for (ColumnArticleReadEnum value : values()) CACHE.put(value.read, value); }
    public static ColumnArticleReadEnum valueOf(int value) { return CACHE.getOrDefault(value, COLUMN_TYPE); }
}
