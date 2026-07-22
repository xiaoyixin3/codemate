package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.github.paicoding.forum.api.model.vo.bugdiagnosis.BugDiagnosisConfirmVo;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepAddReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepResultUpdateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepStatusUpdateReq;
import com.github.paicoding.forum.service.agentrun.service.AgentRunContextRegistry;
import com.github.paicoding.forum.service.bugdiagnosis.service.BugDiagnosisService;
import com.github.paicoding.forum.service.taskplan.service.TaskPlanWriteService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

@Component
public class TaskPlanWriteTools implements CodeMateTool {
    private final BugDiagnosisService diagnosisService;
    private final TaskPlanWriteService writeService;
    private final TrustedToolContextRegistry userRegistry;
    private final AgentRunContextRegistry runRegistry;
    private final SafeToolExecutor executor;

    public TaskPlanWriteTools(BugDiagnosisService diagnosisService, TaskPlanWriteService writeService,
                              TrustedToolContextRegistry userRegistry, AgentRunContextRegistry runRegistry,
                              SafeToolExecutor executor) {
        this.diagnosisService = diagnosisService;
        this.writeService = writeService;
        this.userRegistry = userRegistry;
        this.runRegistry = runRegistry;
        this.executor = executor;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.REVERSIBLE_WRITE;
    }

    @Tool("仅在用户已明确确认 Bug 诊断预览后创建修复计划。diagnosisId 和幂等键必须来自后端确认请求。")
    public String createTaskPlan(@ToolMemoryId String memoryId,
                                 @P("Bug 诊断预览 ID") Long diagnosisId,
                                 @P("8-64 位幂等键") String idempotencyKey) {
        Long userId = userRegistry.requireUserId(memoryId);
        return executor.execute(memoryId, "createTaskPlan", riskLevel(), ToolAccess.AUTHENTICATED, userId,
                diagnosisId + ":" + idempotencyKey, () -> {
                    BugDiagnosisConfirmVo result = diagnosisService.confirm(userId, diagnosisId, idempotencyKey);
                    return ToolResult.success("createTaskPlan", riskLevel(), result, "修复计划已创建");
                });
    }

    @Tool("为当前用户自己的任务计划新增步骤；必须提供幂等键。")
    public String addTaskPlanStep(@ToolMemoryId String memoryId, @P("计划 ID") Long planId,
                                  @P("步骤标题") String title, @P("步骤说明") String content,
                                  @P("预期输出") String expectedOutput, @P("验证方式") String verificationMethod,
                                  @P("风险") String risk, @P("8-64 位幂等键") String idempotencyKey) {
        Long userId = userRegistry.requireUserId(memoryId);
        Long runId = runRegistry.findRunId(memoryId);
        return executor.execute(memoryId, "addTaskPlanStep", riskLevel(), ToolAccess.AUTHENTICATED, userId,
                planId + ":" + idempotencyKey, () -> {
                    AiTaskPlanStepAddReq req = new AiTaskPlanStepAddReq();
                    req.setTitle(title); req.setContent(content); req.setExpectedOutput(expectedOutput);
                    req.setVerificationMethod(verificationMethod); req.setRisk(risk); req.setIdempotencyKey(idempotencyKey);
                    Long stepId = writeService.addStep(userId, runId, planId, req);
                    return ToolResult.success("addTaskPlanStep", riskLevel(), stepId, "任务步骤已新增");
                });
    }

    @Tool("更新当前用户自己的任务步骤状态；必须提供幂等键。")
    public String updateTaskStepStatus(@ToolMemoryId String memoryId, @P("计划 ID") Long planId,
                                       @P("步骤 ID") Long stepId, @P("状态码") Integer status,
                                       @P("阻塞或跳过原因，可为空") String reason,
                                       @P("8-64 位幂等键") String idempotencyKey) {
        Long userId = userRegistry.requireUserId(memoryId);
        Long runId = runRegistry.findRunId(memoryId);
        return executor.execute(memoryId, "updateTaskStepStatus", riskLevel(), ToolAccess.AUTHENTICATED, userId,
                planId + ":" + stepId + ":" + status + ":" + idempotencyKey, () -> {
                    AiTaskPlanStepStatusUpdateReq req = new AiTaskPlanStepStatusUpdateReq();
                    req.setStatus(status); req.setIdempotencyKey(idempotencyKey);
                    if (Integer.valueOf(3).equals(status)) req.setBlockedReason(reason);
                    if (Integer.valueOf(4).equals(status)) req.setSkippedReason(reason);
                    writeService.updateStepStatus(userId, runId, planId, stepId, req);
                    return ToolResult.success("updateTaskStepStatus", riskLevel(), true, "任务步骤状态已更新");
                });
    }

    @Tool("记录当前用户自己的任务步骤实际结果；必须提供幂等键。")
    public String recordTaskStepResult(@ToolMemoryId String memoryId, @P("计划 ID") Long planId,
                                       @P("步骤 ID") Long stepId, @P("实际结果") String actualOutput,
                                       @P("8-64 位幂等键") String idempotencyKey) {
        Long userId = userRegistry.requireUserId(memoryId);
        Long runId = runRegistry.findRunId(memoryId);
        return executor.execute(memoryId, "recordTaskStepResult", riskLevel(), ToolAccess.AUTHENTICATED, userId,
                planId + ":" + stepId + ":" + idempotencyKey, () -> {
                    AiTaskPlanStepResultUpdateReq req = new AiTaskPlanStepResultUpdateReq();
                    req.setActualOutput(actualOutput); req.setIdempotencyKey(idempotencyKey);
                    writeService.recordStepResult(userId, runId, planId, stepId, req);
                    return ToolResult.success("recordTaskStepResult", riskLevel(), true, "任务步骤结果已记录");
                });
    }
}
