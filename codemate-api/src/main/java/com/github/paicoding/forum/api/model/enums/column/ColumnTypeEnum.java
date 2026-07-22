package com.github.paicoding.forum.api.model.enums.column;

import lombok.Getter;

@Getter
public enum ColumnTypeEnum {
    FREE(0, "免费"),
    LOGIN(1, "登录阅读"),
    TIME_FREE(2, "限时免费");

    private final int type;
    private final String desc;
    ColumnTypeEnum(int type, String desc) { this.type = type; this.desc = desc; }
    public static ColumnTypeEnum formCode(int code) {
        for (ColumnTypeEnum value : values()) if (value.type == code) return value;
        return FREE;
    }
}
