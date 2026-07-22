package com.github.paicoding.forum.web.front.bugdiagnosis.rest;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmReq;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmVo;
import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisPreviewVo;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.bugdiagnosis.service.BugDiagnosisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Permission(role = UserRole.LOGIN)
@RequestMapping("/bug-diagnosis/api")
public class BugDiagnosisRestController {
    @Resource
    private BugDiagnosisService bugDiagnosisService;

    @GetMapping("/{diagnosisId}")
    public ResVo<BugDiagnosisPreviewVo> preview(@PathVariable Long diagnosisId) {
        return ResVo.ok(bugDiagnosisService.preview(currentUserId(), diagnosisId));
    }

    @PostMapping("/{diagnosisId}/confirm")
    public ResVo<BugDiagnosisConfirmVo> confirm(@PathVariable Long diagnosisId,
                                                @RequestBody BugDiagnosisConfirmReq req) {
        return ResVo.ok(bugDiagnosisService.confirm(currentUserId(), diagnosisId,
                req == null ? null : req.getIdempotencyKey()));
    }

    private Long currentUserId() {
        return ReqInfoContext.getReqInfo().getUserId();
    }
}
