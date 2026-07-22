package com.github.paicoding.forum.service.chatai.rag.eval;

import lombok.Data;

import java.util.List;

@Data
public class RagEvaluationCase {
    private String id;
    private List<Long> expectedArticleIds;
    private List<Long> retrievedArticleIds;
    private long latencyMillis;
    private boolean citationsValid;
    private boolean noEvidenceExpected;
    private boolean refusedWithoutEvidence;

    public RagEvaluationCase(String id, List<Long> expectedArticleIds, List<Long> retrievedArticleIds,
                             long latencyMillis, boolean citationsValid) {
        this(id, expectedArticleIds, retrievedArticleIds, latencyMillis, citationsValid, false, false);
    }

    public RagEvaluationCase(String id, List<Long> expectedArticleIds, List<Long> retrievedArticleIds,
                             long latencyMillis, boolean citationsValid, boolean noEvidenceExpected,
                             boolean refusedWithoutEvidence) {
        this.id = id;
        this.expectedArticleIds = expectedArticleIds;
        this.retrievedArticleIds = retrievedArticleIds;
        this.latencyMillis = latencyMillis;
        this.citationsValid = citationsValid;
        this.noEvidenceExpected = noEvidenceExpected;
        this.refusedWithoutEvidence = refusedWithoutEvidence;
    }
}
