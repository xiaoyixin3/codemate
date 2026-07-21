package com.github.paicoding.forum.service.chatai.rag.eval;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RagOfflineEvaluator {
    private RagOfflineEvaluator() {
    }

    public static RagEvaluationReport evaluate(List<RagEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("RAG evaluation dataset cannot be empty");
        double recall3 = 0D, recall5 = 0D, reciprocalRanks = 0D, legal = 0D, latency = 0D;
        for (RagEvaluationCase item : cases) {
            recall3 += recalled(item, 3) ? 1D : 0D;
            recall5 += recalled(item, 5) ? 1D : 0D;
            reciprocalRanks += reciprocalRank(item);
            legal += item.isCitationsValid() ? 1D : 0D;
            latency += Math.max(0L, item.getLatencyMillis());
        }
        RagEvaluationReport report = new RagEvaluationReport();
        report.setDatasetSize(cases.size());
        report.setRecallAt3(recall3 / cases.size());
        report.setRecallAt5(recall5 / cases.size());
        report.setMrr(reciprocalRanks / cases.size());
        report.setCitationLegalRate(legal / cases.size());
        report.setAverageLatencyMillis(latency / cases.size());
        return report;
    }

    private static boolean recalled(RagEvaluationCase item, int k) {
        Set<Long> expected = new HashSet<>(item.getExpectedArticleIds());
        List<Long> retrieved = item.getRetrievedArticleIds();
        for (int i = 0; i < Math.min(k, retrieved.size()); i++) if (expected.contains(retrieved.get(i))) return true;
        return false;
    }

    private static double reciprocalRank(RagEvaluationCase item) {
        Set<Long> expected = new HashSet<>(item.getExpectedArticleIds());
        for (int i = 0; i < item.getRetrievedArticleIds().size(); i++) {
            if (expected.contains(item.getRetrievedArticleIds().get(i))) return 1D / (i + 1D);
        }
        return 0D;
    }
}
