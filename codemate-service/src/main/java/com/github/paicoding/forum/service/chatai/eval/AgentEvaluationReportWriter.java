package com.github.paicoding.forum.service.chatai.eval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Locale;

public class AgentEvaluationReportWriter {
    private final ObjectMapper objectMapper;

    public AgentEvaluationReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(AgentEvaluationReport report) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
    }

    public String toMarkdown(AgentEvaluationReport report) {
        StringBuilder output = new StringBuilder("# CodeMate Agent 离线评测报告\n\n")
                .append("- 版本：`").append(report.getVersion()).append("`\n")
                .append("- 固定随机种子：`").append(report.getRandomSeed()).append("`\n")
                .append("- 外部模型：").append(report.isExternalModelEnabled() ? "启用" : "禁用").append("\n");
        append(output, "RAG", report.getRag());
        append(output, "任务规划", report.getTaskPlanning());
        append(output, "工具调用", report.getToolCalling());
        append(output, "Bug 诊断", report.getBugDiagnosis());
        return output.toString();
    }

    private void append(StringBuilder output, String title, EvaluationGroupReport group) {
        output.append("\n## ").append(title).append("\n\n")
                .append("用例数：").append(group == null ? 0 : group.getCaseCount()).append("\n\n")
                .append("| 指标 | 数值 |\n|---|---:|\n");
        if (group != null) {
            for (Map.Entry<String, Double> entry : group.getMetrics().entrySet()) {
                output.append("| ").append(entry.getKey()).append(" | ")
                        .append(String.format(Locale.ROOT, "%.4f", entry.getValue())).append(" |\n");
            }
        }
    }
}
