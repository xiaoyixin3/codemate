package com.github.paicoding.forum.service.chatai.rag.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorSimilarityTest {
    @Test
    void shouldCalculateCosineSimilarity() {
        assertEquals(1D, VectorSimilarity.cosine(new double[]{1, 2}, new double[]{1, 2}), 0.000001D);
        assertEquals(0D, VectorSimilarity.cosine(new double[]{1, 0}, new double[]{0, 1}), 0.000001D);
        assertTrue(VectorSimilarity.cosine(new double[]{1}, new double[]{1, 2}) < 0D);
    }
}
