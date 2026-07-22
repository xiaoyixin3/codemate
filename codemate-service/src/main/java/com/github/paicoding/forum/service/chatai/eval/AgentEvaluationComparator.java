package com.github.paicoding.forum.service.chatai.eval;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AgentEvaluationComparator {
    private AgentEvaluationComparator() {
    }

    public static AgentEvaluationComparison compare(AgentEvaluationReport baseline,
                                                     AgentEvaluationReport candidate,
                                                     double regressionThreshold) {
        if (baseline == null || candidate == null) throw new IllegalArgumentException("evaluation reports are required");
        AgentEvaluationComparison result = new AgentEvaluationComparison();
        result.setBaselineVersion(baseline.getVersion());
        result.setCandidateVersion(candidate.getVersion());
        Map<String, Double> before = flatten(baseline);
        Map<String, Double> after = flatten(candidate);
        for (Map.Entry<String, Double> entry : before.entrySet()) {
            if (!after.containsKey(entry.getKey())) continue;
            double delta = after.get(entry.getKey()) - entry.getValue();
            result.getMetricDeltas().put(entry.getKey(), delta);
            boolean lowerIsBetter = lowerIsBetter(entry.getKey());
            if ((!lowerIsBetter && delta < -Math.abs(regressionThreshold))
                    || (lowerIsBetter && delta > Math.abs(regressionThreshold))) {
                result.getRegressions().add(entry.getKey());
            }
        }
        return result;
    }

    private static Map<String, Double> flatten(AgentEvaluationReport report) {
        Map<String, Double> result = new LinkedHashMap<>();
        add(result, "rag", report.getRag());
        add(result, "taskPlanning", report.getTaskPlanning());
        add(result, "toolCalling", report.getToolCalling());
        add(result, "bugDiagnosis", report.getBugDiagnosis());
        return result;
    }

    private static void add(Map<String, Double> target, String prefix, EvaluationGroupReport group) {
        if (group == null || group.getMetrics() == null) return;
        group.getMetrics().forEach((name, value) -> target.put(prefix + "." + name, value));
    }

    private static boolean lowerIsBetter(String metric) {
        String value = metric.toLowerCase(Locale.ROOT);
        return Arrays.asList("latency", "duplicate", "error", "failure").stream().anyMatch(value::contains);
    }
}
