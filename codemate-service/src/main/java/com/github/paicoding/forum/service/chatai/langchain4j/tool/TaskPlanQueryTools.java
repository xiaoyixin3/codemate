package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanDetailVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanListVo;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskPlanQueryTools implements CodeMateTool {
    private static final int MAX_RESULTS = 20;
    private final AiTaskPlanService taskPlanService;
    private final TrustedToolContextRegistry contextRegistry;
    private final SafeToolExecutor executor;

    public TaskPlanQueryTools(AiTaskPlanService taskPlanService, TrustedToolContextRegistry contextRegistry,
                              SafeToolExecutor executor) {
        this.taskPlanService = taskPlanService;
        this.contextRegistry = contextRegistry;
        this.executor = executor;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ_ONLY;
    }

    @Tool("查询当前登录用户自己的任务计划。用户身份由后端会话确定，不接受 userId 参数。")
    public String listCurrentUserTaskPlans(@ToolMemoryId String memoryId) {
        Long userId = trustedUser(memoryId);
        return executor.execute(memoryId, "listCurrentUserTaskPlans", riskLevel(), ToolAccess.AUTHENTICATED,
                userId, "current-user", () -> {
            List<AiTaskPlanListVo> plans = taskPlanService.listPlans(userId);
            if (plans.size() > MAX_RESULTS) {
                plans = plans.subList(0, MAX_RESULTS);
            }
            return ToolResult.success("listCurrentUserTaskPlans", riskLevel(), plans,
                    plans.isEmpty() ? "当前用户没有任务计划" : "返回当前用户的 " + plans.size() + " 个任务计划");
        });
    }

    @Tool("查询当前登录用户自己的指定任务计划详情。用户身份由后端会话确定，不接受 userId 参数。")
    public String getCurrentUserTaskPlan(@ToolMemoryId String memoryId,
                                         @P("正整数任务计划 ID") Long planId) {
        Long userId = trustedUser(memoryId);
        return executor.execute(memoryId, "getCurrentUserTaskPlan", riskLevel(), ToolAccess.AUTHENTICATED,
                userId, String.valueOf(planId), () -> {
            if (planId == null || planId <= 0) {
                throw new ToolExecutionException("INVALID_ARGUMENT", "planId 必须是正整数");
            }
            try {
                AiTaskPlanDetailVo detail = taskPlanService.queryPlanDetail(userId, planId);
                return ToolResult.success("getCurrentUserTaskPlan", riskLevel(), detail, "任务计划详情已读取");
            } catch (RuntimeException e) {
                throw new ToolExecutionException("ACCESS_DENIED_OR_NOT_FOUND", "任务计划不存在或不属于当前用户");
            }
        });
    }

    private Long trustedUser(String memoryId) {
        return contextRegistry.findUserId(memoryId);
    }
}
