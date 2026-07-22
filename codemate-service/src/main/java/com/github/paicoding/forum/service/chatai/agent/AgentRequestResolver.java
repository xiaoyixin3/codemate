package com.github.paicoding.forum.service.chatai.agent;

import com.github.paicoding.forum.service.chatai.constants.TaskPlanConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Parses the wire format without changing ordinary chat requests. */
@Component
public class AgentRequestResolver {
    public static final String REQUEST_PREFIX = "__codemate_agent__:";
    private static final String BUG_DISPLAY_PREFIX = "Bug 排查：";

    public AgentRequest resolve(String question) {
        if (TaskPlanConstants.isTaskPlanRequest(question)) {
            return new AgentRequest(AgentMode.TASK_PLANNING, TaskPlanConstants.goalOf(question));
        }
        if (StringUtils.startsWith(question, TaskPlanConstants.DISPLAY_PREFIX)) {
            return new AgentRequest(AgentMode.TASK_PLANNING, question.substring(TaskPlanConstants.DISPLAY_PREFIX.length()));
        }
        if (StringUtils.startsWith(question, BUG_DISPLAY_PREFIX)) {
            return new AgentRequest(AgentMode.BUG_DIAGNOSIS, question.substring(BUG_DISPLAY_PREFIX.length()));
        }
        if (!StringUtils.startsWith(question, REQUEST_PREFIX)) {
            return new AgentRequest(AgentMode.CHAT, question);
        }
        String payload = question.substring(REQUEST_PREFIX.length());
        int separator = payload.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("Agent 模式请求格式错误");
        }
        try {
            return new AgentRequest(AgentMode.valueOf(payload.substring(0, separator)), payload.substring(separator + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的 Agent 模式");
        }
    }
}
