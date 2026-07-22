package com.github.paicoding.forum.service.chatai.rag.eval;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RagOfflineEvaluator {
    private RagOfflineEvaluator() {
    }

    public static RagEvaluationReport evaluate(List<RagEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("RAG evaluation dataset cannot be empty");
        double recall3 = 0D, recall5 = 0D, reciprocalRanks = 0D, legal = 0D, latency = 0D, refusals = 0D;
        int noEvidenceCases = 0, retrievalCases = 0;
        for (RagEvaluationCase item : cases) {
            if (!item.isNoEvidenceExpected()) {
                retrievalCases++;
                recall3 += recalled(item, 3) ? 1D : 0D;
                recall5 += recalled(item, 5) ? 1D : 0D;
                reciprocalRanks += reciprocalRank(item);
            }
            legal += item.isCitationsValid() ? 1D : 0D;
            latency += Math.max(0L, item.getLatencyMillis());
            if (item.isNoEvidenceExpected()) {
                noEvidenceCases++;
                refusals += item.isRefusedWithoutEvidence() ? 1D : 0D;
            }
        }
        RagEvaluationReport report = new RagEvaluationReport();
        report.setDatasetSize(cases.size());
        report.setRecallAt3(retrievalCases == 0 ? 0D : recall3 / retrievalCases);
        report.setRecallAt5(retrievalCases == 0 ? 0D : recall5 / retrievalCases);
        report.setMrr(retrievalCases == 0 ? 0D : reciprocalRanks / retrievalCases);
        report.setCitationLegalRate(legal / cases.size());
        report.setAverageLatencyMillis(latency / cases.size());
        report.setNoEvidenceCaseCount(noEvidenceCases);
        report.setNoEvidenceRefusalRate(noEvidenceCases == 0 ? 0D : refusals / noEvidenceCases);
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
