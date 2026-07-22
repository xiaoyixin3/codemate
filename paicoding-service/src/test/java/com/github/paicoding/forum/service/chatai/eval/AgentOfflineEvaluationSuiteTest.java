package com.github.paicoding.forum.service.chatai.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.service.chatai.rag.eval.RagEvaluationCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOfflineEvaluationSuiteTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode fixture;

    @BeforeEach
    void loadFixture() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/codemate/agent-eval-v1.json")) {
            if (input == null) throw new IllegalStateException("Agent evaluation fixture missing");
            fixture = objectMapper.readTree(input);
        }
    }

    @Test
    void evaluatesFixedDatasetWithoutCallingExternalModels() throws Exception {
        AgentEvaluationReport report = evaluate();

        assertEquals("agent-eval-v1", report.getVersion());
        assertEquals(20260722L, report.getRandomSeed());
        assertFalse(report.isExternalModelEnabled());
        assertEquals(52, report.getRag().getCaseCount());
        assertEquals(1D, report.getRag().getMetrics().get("recallAt3"), 0.0001D);
        assertEquals(0.5D, report.getRag().getMetrics().get("noEvidenceRefusalRate"), 0.0001D);
        assertEquals(0.5D, report.getTaskPlanning().getMetrics().get("jsonParseSuccessRate"), 0.0001D);
        assertEquals(0.5D, report.getTaskPlanning().getMetrics().get("stateMachineLegalRate"), 0.0001D);
        assertEquals(2D / 3D, report.getToolCalling().getMetrics().get("toolSelectionAccuracy"), 0.0001D);
        assertEquals(1D, report.getToolCalling().getMetrics().get("unauthorizedBlockRate"), 0.0001D);
        assertEquals(0.5D, report.getBugDiagnosis().getMetrics().get("completeDiagnosisRate"), 0.0001D);
    }

    @Test
    void writesMachineAndHumanReportsAndComparesVersions() throws Exception {
        AgentEvaluationReport baseline = evaluate();
        AgentEvaluationReport candidate = evaluate();
        candidate.setVersion("agent-eval-v2");
        candidate.getToolCalling().getMetrics().put("toolSelectionAccuracy", 0.3D);
        candidate.getToolCalling().getMetrics().put("duplicateInvocationRate", 0.8D);

        AgentEvaluationReportWriter writer = new AgentEvaluationReportWriter(objectMapper);
        String json = writer.toJson(candidate);
        String markdown = writer.toMarkdown(candidate);
        AgentEvaluationComparison comparison = AgentEvaluationComparator.compare(baseline, candidate, 0.01D);

        assertTrue(json.contains("agent-eval-v2"));
        assertTrue(markdown.contains("工具调用"));
        assertTrue(comparison.getRegressions().contains("toolCalling.toolSelectionAccuracy"));
        assertTrue(comparison.getRegressions().contains("toolCalling.duplicateInvocationRate"));
        assertEquals(0D, comparison.getMetricDeltas().get("rag.recallAt3"), 0.0001D);
    }

    private AgentEvaluationReport evaluate() throws Exception {
        AgentOfflineEvaluationSuite suite = new AgentOfflineEvaluationSuite(objectMapper);
        return suite.evaluate(fixture.get("version").asText(), fixture.get("randomSeed").asLong(), false,
                ragCases(), taskCases(), toolCases(), bugCases());
    }

    private List<RagEvaluationCase> ragCases() throws Exception {
        List<RagEvaluationCase> result = new ArrayList<>();
        try (InputStream input = getClass().getResourceAsStream("/codemate/rag-eval-v1.csv")) {
            if (input == null) throw new IllegalStateException("RAG evaluation fixture missing");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                for (String[] row : reader.lines().skip(1).map(line -> line.split(",", -1)).collect(Collectors.toList())) {
                    result.add(new RagEvaluationCase(row[0], ids(row[2]), ids(row[4]), Long.parseLong(row[6]), true));
                }
            }
        }
        for (JsonNode node : fixture.get("noEvidenceCases")) {
            result.add(new RagEvaluationCase(node.get("id").asText(), Collections.emptyList(), Collections.emptyList(),
                    node.get("latencyMillis").asLong(), true, true, node.get("refused").asBoolean()));
        }
        return result;
    }

    private List<TaskPlanningEvaluationCase> taskCases() throws Exception {
        List<TaskPlanningEvaluationCase> result = new ArrayList<>();
        for (JsonNode node : fixture.get("taskPlanning")) {
            JsonNode output = node.get("output");
            result.add(new TaskPlanningEvaluationCase(node.get("id").asText(),
                    output.isTextual() ? output.asText() : objectMapper.writeValueAsString(output),
                    node.get("expectedMinimumSteps").asInt(), strings(node.get("stateTransitions"))));
        }
        return result;
    }

    private List<ToolCallingEvaluationCase> toolCases() throws Exception {
        List<ToolCallingEvaluationCase> result = new ArrayList<>();
        for (JsonNode node : fixture.get("toolCalling")) {
            result.add(objectMapper.treeToValue(node, ToolCallingEvaluationCase.class));
        }
        return result;
    }

    private List<BugDiagnosisEvaluationCase> bugCases() throws Exception {
        List<BugDiagnosisEvaluationCase> result = new ArrayList<>();
        for (JsonNode node : fixture.get("bugDiagnosis")) {
            JsonNode output = node.get("output");
            result.add(new BugDiagnosisEvaluationCase(node.get("id").asText(),
                    output.isTextual() ? output.asText() : objectMapper.writeValueAsString(output)));
        }
        return result;
    }

    private List<Long> ids(String value) {
        return Arrays.stream(value.split("\\|")).map(Long::valueOf).collect(Collectors.toList());
    }

    private List<String> strings(JsonNode node) {
        List<String> result = new ArrayList<>();
        node.forEach(value -> result.add(value.asText()));
        return result;
    }
}
