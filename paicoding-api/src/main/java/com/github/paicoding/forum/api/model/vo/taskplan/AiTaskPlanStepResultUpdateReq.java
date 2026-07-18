package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

/**
 * 保存任务计划步骤执行结果请求
 */
@Data
public class AiTaskPlanStepResultUpdateReq {
    private String actualOutput;
}
