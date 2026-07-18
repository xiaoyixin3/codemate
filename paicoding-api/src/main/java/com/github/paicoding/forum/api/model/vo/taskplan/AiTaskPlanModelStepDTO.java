package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

/** DeepSeek任务规划步骤输出，不直接映射数据库实体。 */
@Data
public class AiTaskPlanModelStepDTO {
    private Integer stepNumber;
    private String title;
    private String description;
    private String expectedOutput;
    private String validationMethod;
    private String riskNote;
}
