package com.github.paicoding.forum.service.chatai.rag.eval;

import lombok.Data;

@Data
public class RagEvaluationReport {
    private int datasetSize;
    private double recallAt3;
    private double recallAt5;
    private double mrr;
    private double citationLegalRate;
    private double averageLatencyMillis;
}
