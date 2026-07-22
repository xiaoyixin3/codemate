package com.github.paicoding.forum.service.chatai.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisCauseDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisEvidenceDTO;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisModelResponseDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BugDiagnosisOfflineEvaluator {
    private final ObjectMapper objectMapper;

    public BugDiagnosisOfflineEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluationGroupReport evaluate(List<BugDiagnosisEvaluationCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("bug diagnosis evaluation dataset cannot be empty");
        double parsed = 0D, hypotheses = 0D, evidence = 0D, verification = 0D, regression = 0D, complete = 0D;
        for (BugDiagnosisEvaluationCase item : cases) {
            try {
                BugDiagnosisModelResponseDTO output = objectMapper.readValue(item.getModelOutput(), BugDiagnosisModelResponseDTO.class);
                parsed++;
                boolean h = hypotheses(output), e = evidence(output), v = nonBlank(output.getVerificationSteps());
                boolean r = nonBlank(output.getRegressionPlan());
                if (h) hypotheses++;
                if (e) evidence++;
                if (v) verification++;
                if (r) regression++;
                if (h && e && v && r) complete++;
            } catch (Exception ignored) {
                // Invalid model output is measured, not propagated.
            }
        }
        double count = cases.size();
        EvaluationGroupReport report = new EvaluationGroupReport();
        report.setCaseCount(cases.size());
        return report.metric("jsonParseSuccessRate", parsed / count)
                .metric("hypothesisPresenceRate", hypotheses / count)
                .metric("evidencePresenceRate", evidence / count)
                .metric("verificationPresenceRate", verification / count)
                .metric("regressionPresenceRate", regression / count)
                .metric("completeDiagnosisRate", complete / count);
    }

    private boolean hypotheses(BugDiagnosisModelResponseDTO output) {
        return output != null && output.getCauseHypotheses() != null && !output.getCauseHypotheses().isEmpty()
                && output.getCauseHypotheses().stream().allMatch(this::validCause);
    }

    private boolean validCause(BugDiagnosisCauseDTO cause) {
        return cause != null && StringUtils.isNotBlank(cause.getHypothesis()) && cause.getConfidence() != null
                && cause.getConfidence() >= 0D && cause.getConfidence() <= 1D && nonBlank(cause.getSupportingEvidence());
    }

    private boolean evidence(BugDiagnosisModelResponseDTO output) {
        return output != null && output.getSupportingEvidence() != null && !output.getSupportingEvidence().isEmpty()
                && output.getSupportingEvidence().stream().allMatch(this::validEvidence);
    }

    private boolean validEvidence(BugDiagnosisEvidenceDTO evidence) {
        return evidence != null && StringUtils.isNotBlank(evidence.getTitle()) && StringUtils.isNotBlank(evidence.getExcerpt());
    }

    private boolean nonBlank(List<String> values) {
        return values != null && !values.isEmpty() && values.stream().allMatch(StringUtils::isNotBlank);
    }
}
