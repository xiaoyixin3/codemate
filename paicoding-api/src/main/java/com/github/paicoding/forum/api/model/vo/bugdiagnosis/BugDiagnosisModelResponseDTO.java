package com.github.paicoding.forum.api.model.vo.bugdiagnosis;

import lombok.Data;

import java.util.List;

@Data
public class BugDiagnosisModelResponseDTO {
    private String problemSummary;
    private List<BugDiagnosisCauseDTO> causeHypotheses;
    private List<BugDiagnosisEvidenceDTO> supportingEvidence;
    private List<String> verificationSteps;
    private List<String> fixSuggestions;
    private List<String> regressionPlan;
}
