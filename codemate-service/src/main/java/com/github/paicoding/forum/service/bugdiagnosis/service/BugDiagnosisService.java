package com.github.paicoding.forum.service.bugdiagnosis.service;

import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisPreviewVo;

public interface BugDiagnosisService {
    Long createPreview(Long userId, Long runId, String chatId, String rawJson);

    BugDiagnosisPreviewVo preview(Long userId, Long diagnosisId);

    BugDiagnosisConfirmVo confirm(Long userId, Long diagnosisId, String idempotencyKey);
}
