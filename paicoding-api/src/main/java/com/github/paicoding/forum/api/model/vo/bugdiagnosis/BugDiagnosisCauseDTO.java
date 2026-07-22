package com.github.paicoding.forum.api.model.vo.bugdiagnosis;

import lombok.Data;

import java.util.List;

@Data
public class BugDiagnosisCauseDTO {
    private String hypothesis;
    private List<String> supportingEvidence;
    /** 0-1 confidence score supplied by the model. */
    private Double confidence;
}
