package com.github.paicoding.forum.service.chatai.agent;

import com.github.paicoding.forum.api.model.enums.ChatAnswerTypeEnum;
import com.github.paicoding.forum.api.model.vo.chat.ChatItemVo;
import com.github.paicoding.forum.api.model.vo.chat.ChatRecordsVo;
import com.github.paicoding.forum.service.chatai.constants.TaskPlanConstants;
import com.github.paicoding.forum.service.taskplan.service.AiTaskPlanStructuredService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Adapter for the existing task-plan prompt and persistence service. */
@Slf4j
@Component
public class TaskPlanningAgentModeHandler implements AgentModeHandler {
    @Autowired
    private AiTaskPlanStructuredService aiTaskPlanStructuredService;

    @Override
    public AgentMode mode() {
        return AgentMode.TASK_PLANNING;
    }

    @Override
    public String systemPrompt() {
        return TaskPlanConstants.SYSTEM_PROMPT;
    }

    @Override
    public String validateAndNormalize(String input) {
        if (StringUtils.isBlank(input)) {
            throw new IllegalArgumentException("任务计划目标不能为空");
        }
        return input.trim();
    }

    @Override
    public boolean requiresStructuredOutput() {
        return true;
    }

    @Override
    public String displayQuestion(String normalizedInput) {
        return TaskPlanConstants.DISPLAY_PREFIX + normalizedInput;
    }

    @Override
    public void transformResult(Long userId, String chatId, ChatRecordsVo response, ChatItemVo item) {
        String rawAnswer = item.getAnswer();
        try {
            Long planId = aiTaskPlanStructuredService.parseAndSave(userId, chatId, rawAnswer);
            response.setPlanId(planId);
            item.initAnswer(aiTaskPlanStructuredService.toMarkdown(planId, aiTaskPlanStructuredService.parse(rawAnswer)))
                    .setAnswerType(ChatAnswerTypeEnum.STREAM_END);
        } catch (Exception e) {
            log.warn("任务计划 JSON 解析或保存失败，保留原始回复", e);
            item.initAnswer("任务计划解析失败，未保存任何计划。请检查原始回复后重试。\n\n```json\n"
                    + rawAnswer + "\n```").setAnswerType(ChatAnswerTypeEnum.STREAM_END);
        }
    }
}
