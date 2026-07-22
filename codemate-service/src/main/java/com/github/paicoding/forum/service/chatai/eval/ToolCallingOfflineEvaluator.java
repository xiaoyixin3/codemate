package com.github.paicoding.forum.service.chatai.eval;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class ToolCallingOfflineEvaluator {
    private ToolCallingOfflineEvaluator() {
    }

    public static EvaluationGroupReport evaluate(List<ToolCallingEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("tool evaluation dataset cannot be empty");
        double selected = 0D, parameters = 0D, succeeded = 0D, duplicates = 0D;
        double unauthorized = 0D, blocked = 0D;
        for (ToolCallingEvaluationCase item : cases) {
            if (StringUtils.equals(item.getExpectedTool(), item.getSelectedTool())) selected++;
            if (item.isParametersValid()) parameters++;
            if (item.isInvocationSucceeded()) succeeded++;
            if (item.isDuplicateInvocation()) duplicates++;
            if (item.isUnauthorizedAttempt()) {
                unauthorized++;
                if (item.isUnauthorizedBlocked()) blocked++;
            }
        }
        double count = cases.size();
        EvaluationGroupReport report = new EvaluationGroupReport();
        report.setCaseCount(cases.size());
        return report.metric("toolSelectionAccuracy", selected / count)
                .metric("parameterValidityRate", parameters / count)
                .metric("invocationSuccessRate", succeeded / count)
                .metric("duplicateInvocationRate", duplicates / count)
                .metric("unauthorizedBlockRate", unauthorized == 0D ? 0D : blocked / unauthorized);
    }
}
