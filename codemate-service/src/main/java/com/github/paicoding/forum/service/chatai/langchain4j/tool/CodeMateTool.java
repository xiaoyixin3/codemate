package com.github.paicoding.forum.service.chatai.langchain4j.tool;

/** Marker implemented by every object exposed to LangChain4j as a tool. */
public interface CodeMateTool {
    ToolRiskLevel riskLevel();
}
