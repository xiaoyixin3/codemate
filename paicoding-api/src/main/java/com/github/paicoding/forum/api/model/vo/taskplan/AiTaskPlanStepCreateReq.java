package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

/**
 * 创建任务计划步骤请求
 */
@Data
public class AiTaskPlanStepCreateReq {
    private String title;
    private String content;
    private String expectedOutput;
    private String risk;
    private String verificationMethod;
}
