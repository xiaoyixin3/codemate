package com.github.paicoding.forum.service.chatai.eval;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentEvaluationComparison {
    private String baselineVersion;
    private String candidateVersion;
    private Map<String, Double> metricDeltas = new LinkedHashMap<>();
    private List<String> regressions = new ArrayList<>();
}
