package com.github.paicoding.forum.service.chatai.rag.eval;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RagEvaluationCase {
    private String id;
    private List<Long> expectedArticleIds;
    private List<Long> retrievedArticleIds;
    private long latencyMillis;
    private boolean citationsValid;
}
