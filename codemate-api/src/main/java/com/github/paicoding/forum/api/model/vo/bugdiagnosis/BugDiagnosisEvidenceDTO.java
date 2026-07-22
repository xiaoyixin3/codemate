package com.github.paicoding.forum.api.model.vo.bugdiagnosis;

import lombok.Data;

@Data
public class BugDiagnosisEvidenceDTO {
    private Long articleId;
    private String title;
    private String excerpt;
}
