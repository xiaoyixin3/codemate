package com.github.paicoding.forum.api.model.vo.taskplan;

import lombok.Data;

import java.util.List;

/** DeepSeek任务规划结构化输出，不直接映射数据库实体。 */
@Data
public class AiTaskPlanModelResponseDTO {
    private String title;
    private String goal;
    private String scope;
    private String summary;
    private List<AiTaskPlanModelStepDTO> steps;
    private List<String> risks;
    private List<String> validationMethods;
    private List<String> acceptanceCriteria;
}
