package com.github.paicoding.forum.service.chatai.eval;

import lombok.Data;

@Data
public class AgentEvaluationReport {
    private String version;
    private long randomSeed;
    private boolean externalModelEnabled;
    private EvaluationGroupReport rag;
    private EvaluationGroupReport taskPlanning;
    private EvaluationGroupReport toolCalling;
    private EvaluationGroupReport bugDiagnosis;
}
