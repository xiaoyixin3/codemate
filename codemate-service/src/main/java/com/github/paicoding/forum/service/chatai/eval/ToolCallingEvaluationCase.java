package com.github.paicoding.forum.service.chatai.eval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolCallingEvaluationCase {
    private String id;
    private String expectedTool;
    private String selectedTool;
    private boolean parametersValid;
    private boolean invocationSucceeded;
    private boolean duplicateInvocation;
    private boolean unauthorizedAttempt;
    private boolean unauthorizedBlocked;
}
