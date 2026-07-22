package com.github.paicoding.forum.service.chatai.eval;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "codemate.evaluation")
public class EvaluationProperties {
    private long fixedRandomSeed = 20260722L;
    /** Must remain false in normal unit-test and application profiles. */
    private boolean externalModelEnabled;
}
