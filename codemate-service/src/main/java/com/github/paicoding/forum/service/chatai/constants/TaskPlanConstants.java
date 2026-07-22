package com.github.paicoding.forum.service.chatai.constants;

public final class TaskPlanConstants {
    private TaskPlanConstants() {
    }

    public static final String REQUEST_PREFIX = "__codemate_task_plan__:";
    public static final String DISPLAY_PREFIX = "任务规划：";
    public static final String SYSTEM_PROMPT = "你是 CodeMate 任务规划 Agent。根据用户目标生成可执行、可持久化的任务计划。"
            + "只返回合法 JSON，不要 Markdown 代码块，不要额外解释。"
            + "JSON 必须包含 title、goal、scope、summary、steps、risks、validationMethods、acceptanceCriteria。"
            + "steps 是数组，每项必须包含 stepNumber、title、description、expectedOutput、validationMethod、riskNote。"
            + "步骤必须按实际依赖顺序排列，每一步必须可独立验收，不要把多个大型任务混在同一步。";

    public static boolean isTaskPlanRequest(String question) {
        return question != null && question.startsWith(REQUEST_PREFIX);
    }

    public static String goalOf(String question) {
        return question.substring(REQUEST_PREFIX.length()).trim();
    }
}
