package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

/**
 * 更新AI任务计划请求
 */
@Data
public class AiTaskPlanUpdateReq {
    private String title;
    private String goal;
}
