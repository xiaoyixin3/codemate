package com.github.paicoding.forum.web.front.agentrun.rest;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunDetailVo;
import com.github.paicoding.forum.api.model.vo.agentrun.AgentRunListVo;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.agentrun.service.AgentRunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Permission(role = UserRole.LOGIN)
@RequestMapping("/agent-run/api")
public class AgentRunRestController {
    private final AgentRunService agentRunService;

    public AgentRunRestController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @GetMapping
    public ResVo<List<AgentRunListVo>> list(@RequestParam(defaultValue = "20") int limit) {
        return ResVo.ok(agentRunService.listRuns(currentUserId(), limit));
    }

    @GetMapping("/{runId}")
    public ResVo<AgentRunDetailVo> detail(@PathVariable Long runId) {
        return ResVo.ok(agentRunService.detail(currentUserId(), runId));
    }

    @PutMapping("/{runId}/cancel")
    public ResVo<Boolean> cancel(@PathVariable Long runId) {
        agentRunService.cancel(currentUserId(), runId);
        return ResVo.ok(true);
    }

    private Long currentUserId() {
        return ReqInfoContext.getReqInfo().getUserId();
    }
}
