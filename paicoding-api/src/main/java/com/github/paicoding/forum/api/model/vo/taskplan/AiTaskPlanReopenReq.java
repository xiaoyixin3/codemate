package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

/**
 * 重新开启已完成任务计划请求
 */
@Data
public class AiTaskPlanReopenReq {
    private String reason;
}
