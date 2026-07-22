package com.github.paicoding.forum.service.chatai.eval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskPlanningEvaluationCase {
    private String id;
    private String modelOutput;
    private int expectedMinimumSteps;
    /** Transitions formatted as CURRENT>TARGET. */
    private List<String> stateTransitions;
}
