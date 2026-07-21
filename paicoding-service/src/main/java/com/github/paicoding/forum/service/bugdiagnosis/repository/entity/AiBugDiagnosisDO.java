package com.github.paicoding.forum.service.bugdiagnosis.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.paicoding.forum.api.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_bug_diagnosis")
public class AiBugDiagnosisDO extends BaseDO {
    private Long runId;
    private Long userId;
    private String chatId;
    private String problemSummary;
    private String diagnosisJson;
    private String status;
    private String confirmIdempotencyKey;
    private Long planId;
    private Date confirmedTime;
}
