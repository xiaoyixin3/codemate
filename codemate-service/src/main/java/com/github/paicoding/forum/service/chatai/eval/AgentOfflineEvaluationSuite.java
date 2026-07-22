package com.github.paicoding.forum.service.chatai.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.service.chatai.rag.eval.RagEvaluationCase;
import com.github.paicoding.forum.service.chatai.rag.eval.RagEvaluationReport;
import com.github.paicoding.forum.service.chatai.rag.eval.RagOfflineEvaluator;

import java.util.List;

public class AgentOfflineEvaluationSuite {
    private final TaskPlanningOfflineEvaluator taskPlanningEvaluator;
    private final BugDiagnosisOfflineEvaluator bugDiagnosisEvaluator;

    public AgentOfflineEvaluationSuite(ObjectMapper objectMapper) {
        this.taskPlanningEvaluator = new TaskPlanningOfflineEvaluator(objectMapper);
        this.bugDiagnosisEvaluator = new BugDiagnosisOfflineEvaluator(objectMapper);
    }

    public AgentEvaluationReport evaluate(String version, long randomSeed, boolean externalModelEnabled,
                                          List<RagEvaluationCase> ragCases,
                                          List<TaskPlanningEvaluationCase> taskCases,
                                          List<ToolCallingEvaluationCase> toolCases,
                                          List<BugDiagnosisEvaluationCase> bugCases) {
        AgentEvaluationReport report = new AgentEvaluationReport();
        report.setVersion(version);
        report.setRandomSeed(randomSeed);
        report.setExternalModelEnabled(externalModelEnabled);
        report.setRag(rag(RagOfflineEvaluator.evaluate(ragCases)));
        report.setTaskPlanning(taskPlanningEvaluator.evaluate(taskCases));
        report.setToolCalling(ToolCallingOfflineEvaluator.evaluate(toolCases));
        report.setBugDiagnosis(bugDiagnosisEvaluator.evaluate(bugCases));
        return report;
    }

    private EvaluationGroupReport rag(RagEvaluationReport source) {
        EvaluationGroupReport report = new EvaluationGroupReport();
        report.setCaseCount(source.getDatasetSize());
        return report.metric("recallAt3", source.getRecallAt3())
                .metric("recallAt5", source.getRecallAt5())
                .metric("mrr", source.getMrr())
                .metric("citationLegalRate", source.getCitationLegalRate())
                .metric("noEvidenceRefusalRate", source.getNoEvidenceRefusalRate())
                .metric("averageLatencyMillis", source.getAverageLatencyMillis());
    }
}
