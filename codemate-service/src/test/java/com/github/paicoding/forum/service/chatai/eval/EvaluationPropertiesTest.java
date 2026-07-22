package com.github.paicoding.forum.service.chatai.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EvaluationPropertiesTest {
    @Test
    void externalModelEvaluationIsOptIn() {
        EvaluationProperties properties = new EvaluationProperties();
        assertEquals(20260722L, properties.getFixedRandomSeed());
        assertFalse(properties.isExternalModelEnabled());
    }
}
