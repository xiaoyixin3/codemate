package com.github.paicoding.forum.service.bugdiagnosis.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.paicoding.forum.service.bugdiagnosis.repository.entity.AiBugDiagnosisDO;
import com.github.paicoding.forum.service.bugdiagnosis.repository.mapper.AiBugDiagnosisMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AiBugDiagnosisDao extends ServiceImpl<AiBugDiagnosisMapper, AiBugDiagnosisDO> {
    public AiBugDiagnosisDO getByRunId(Long runId) {
        return lambdaQuery().eq(AiBugDiagnosisDO::getRunId, runId).one();
    }

    public AiBugDiagnosisDO getOwned(Long diagnosisId, Long userId) {
        return lambdaQuery().eq(AiBugDiagnosisDO::getId, diagnosisId)
                .eq(AiBugDiagnosisDO::getUserId, userId).one();
    }

    public AiBugDiagnosisDO getOwnedForUpdate(Long diagnosisId, Long userId) {
        return lambdaQuery().eq(AiBugDiagnosisDO::getId, diagnosisId)
                .eq(AiBugDiagnosisDO::getUserId, userId).last("FOR UPDATE").one();
    }
}
