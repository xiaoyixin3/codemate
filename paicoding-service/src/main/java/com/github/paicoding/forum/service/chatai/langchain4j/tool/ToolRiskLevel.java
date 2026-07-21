package com.github.paicoding.forum.service.chatai.langchain4j.tool;

/** Risk classification used by the common tool execution policy. */
public enum ToolRiskLevel {
    READ_ONLY,
    REVERSIBLE_WRITE,
    SENSITIVE_WRITE
}
