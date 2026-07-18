package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

import java.util.List;

/**
 * 创建AI任务计划请求
 */
@Data
public class AiTaskPlanCreateReq {
    private String title;
    private String goal;
    private String chatId;
    private Integer sourceAiType;
    private String planKey;
    private String extra;
    private List<AiTaskPlanStepCreateReq> steps;
}
