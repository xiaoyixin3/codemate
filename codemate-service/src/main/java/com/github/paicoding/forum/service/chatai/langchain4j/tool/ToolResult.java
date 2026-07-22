package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult<T> {
    private boolean success;
    private String tool;
    private ToolRiskLevel risk;
    private T data;
    private String summary;
    private String errorCode;
    private boolean truncated;

    public static <T> ToolResult<T> success(String tool, ToolRiskLevel risk, T data, String summary) {
        return new ToolResult<>(true, tool, risk, data, summary, null, false);
    }

    public static ToolResult<Void> failure(String tool, ToolRiskLevel risk, String errorCode, String summary) {
        return new ToolResult<>(false, tool, risk, null, summary, errorCode, false);
    }
}
