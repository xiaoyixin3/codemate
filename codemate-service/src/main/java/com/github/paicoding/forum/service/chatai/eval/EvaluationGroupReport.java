package com.github.paicoding.forum.service.chatai.eval;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class EvaluationGroupReport {
    private int caseCount;
    private Map<String, Double> metrics = new LinkedHashMap<>();

    public EvaluationGroupReport metric(String name, double value) {
        metrics.put(name, value);
        return this;
    }
}
