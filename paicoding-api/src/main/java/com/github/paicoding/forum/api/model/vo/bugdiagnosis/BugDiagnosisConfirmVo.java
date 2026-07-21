package com.github.paicoding.forum.api.model.vo.bugdiagnosis;

import lombok.Data;

@Data
public class BugDiagnosisConfirmVo {
    private Long diagnosisId;
    private Long planId;
    private String planUrl;
}
