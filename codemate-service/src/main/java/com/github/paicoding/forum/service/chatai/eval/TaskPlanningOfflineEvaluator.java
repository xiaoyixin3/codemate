package com.github.paicoding.forum.service.chatai.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.ai.AgentRunStatusEnum;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelResponseDTO;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelStepDTO;
import com.github.paicoding.forum.service.agentrun.service.AgentRunStateMachine;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class TaskPlanningOfflineEvaluator {
    private final ObjectMapper objectMapper;

    public TaskPlanningOfflineEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluationGroupReport evaluate(List<TaskPlanningEvaluationCase> cases) {
        requireCases(cases, "task planning");
        double parsed = 0D, complete = 0D, verification = 0D, legalState = 0D;
        for (TaskPlanningEvaluationCase item : cases) {
            try {
                AiTaskPlanModelResponseDTO output = objectMapper.readValue(item.getModelOutput(), AiTaskPlanModelResponseDTO.class);
                parsed++;
                if (complete(output, item.getExpectedMinimumSteps())) complete++;
                if (hasVerification(output)) verification++;
            } catch (Exception ignored) {
                // Invalid model output is an expected offline evaluation result.
            }
            if (legalTransitions(item.getStateTransitions())) legalState++;
        }
        double count = cases.size();
        EvaluationGroupReport report = new EvaluationGroupReport();
        report.setCaseCount(cases.size());
        return report.metric("jsonParseSuccessRate", parsed / count)
                .metric("stepCompletenessRate", complete / count)
                .metric("verificationStepRate", verification / count)
                .metric("stateMachineLegalRate", legalState / count);
    }

    private boolean complete(AiTaskPlanModelResponseDTO output, int expectedMinimumSteps) {
        if (output == null || StringUtils.isAnyBlank(output.getTitle(), output.getGoal(), output.getScope(), output.getSummary())
                || output.getSteps() == null || output.getSteps().size() < Math.max(1, expectedMinimumSteps)) return false;
        int expected = 1;
        for (AiTaskPlanModelStepDTO step : output.getSteps()) {
            if (step == null || step.getStepNumber() == null || step.getStepNumber() != expected++
                    || StringUtils.isAnyBlank(step.getTitle(), step.getDescription(), step.getExpectedOutput(),
                    step.getValidationMethod(), step.getRiskNote())) return false;
        }
        return true;
    }

    private boolean hasVerification(AiTaskPlanModelResponseDTO output) {
        if (output == null || output.getSteps() == null) return false;
        return output.getSteps().stream().anyMatch(step -> step != null
                && StringUtils.isNotBlank(step.getValidationMethod()));
    }

    private boolean legalTransitions(List<String> transitions) {
        if (transitions == null || transitions.isEmpty()) return false;
        try {
            for (String transition : transitions) {
                String[] values = StringUtils.split(transition, '>');
                if (values == null || values.length != 2) return false;
                AgentRunStateMachine.validate(AgentRunStatusEnum.valueOf(values[0]), AgentRunStatusEnum.valueOf(values[1]));
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void requireCases(List<?> cases, String name) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException(name + " evaluation dataset cannot be empty");
    }
}
