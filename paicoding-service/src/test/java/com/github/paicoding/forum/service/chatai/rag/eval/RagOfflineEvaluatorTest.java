package com.github.paicoding.forum.service.chatai.rag.eval;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagOfflineEvaluatorTest {
    @Test
    void fixedFiftyCaseDatasetShowsHybridRecallAndMrrImprovement() throws Exception {
        List<String[]> rows = rows();
        List<RagEvaluationCase> baseline = cases(rows, 3, 5);
        List<RagEvaluationCase> hybrid = cases(rows, 4, 6);

        RagEvaluationReport before = RagOfflineEvaluator.evaluate(baseline);
        RagEvaluationReport after = RagOfflineEvaluator.evaluate(hybrid);

        assertEquals(50, before.getDatasetSize());
        assertEquals(0.60D, before.getRecallAt3(), 0.0001D);
        assertEquals(0.80D, before.getRecallAt5(), 0.0001D);
        assertEquals(1D, after.getRecallAt3(), 0.0001D);
        assertEquals(1D, after.getMrr(), 0.0001D);
        assertEquals(1D, after.getCitationLegalRate(), 0.0001D);
        assertTrue(after.getMrr() > before.getMrr());
    }

    private List<String[]> rows() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/codemate/rag-eval-v1.csv");
        if (stream == null) throw new IllegalStateException("RAG evaluation fixture missing");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).map(line -> line.split(",", -1)).collect(Collectors.toList());
        }
    }

    private List<RagEvaluationCase> cases(List<String[]> rows, int resultColumn, int latencyColumn) {
        List<RagEvaluationCase> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(new RagEvaluationCase(row[0], ids(row[2]), ids(row[resultColumn]),
                    Long.parseLong(row[latencyColumn]), true));
        }
        return result;
    }

    private List<Long> ids(String value) {
        return Arrays.stream(value.split("\\|")).map(Long::valueOf).collect(Collectors.toList());
    }
}
