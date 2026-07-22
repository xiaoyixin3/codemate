package com.github.paicoding.forum.api.model.vo.bugdiagnosis;

import lombok.Data;

@Data
public class BugDiagnosisPreviewVo extends BugDiagnosisModelResponseDTO {
    private Long diagnosisId;
    private Long agentRunId;
    private String status;
    private Long planId;
}
