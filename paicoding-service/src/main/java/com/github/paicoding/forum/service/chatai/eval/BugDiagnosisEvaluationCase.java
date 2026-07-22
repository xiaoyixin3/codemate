package com.github.paicoding.forum.service.chatai.eval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BugDiagnosisEvaluationCase {
    private String id;
    private String modelOutput;
}
