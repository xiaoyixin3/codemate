package com.github.paicoding.forum.service.chatai.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Bug diagnosis is advisory only: it never executes supplied code or commands. */
@Component
public class BugDiagnosisAgentModeHandler implements AgentModeHandler {
    private static final int MAX_INPUT_LENGTH = 16_000;
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._~+/-]{8,}");
    private static final Pattern SECRET_VALUE = Pattern.compile("(?i)((?:api[_-]?key|token|secret|password|passwd)\\s*[:=]\\s*[\\\"']?)[^\\s,;\\\"']+");
    private static final String PROMPT = "你是 CodeMate Bug 诊断 Agent。先使用站内文章检索和详情工具查找可核验依据，再形成诊断。"
            + "绝不执行用户代码、命令或系统操作，也不能自行创建或修改任务计划；必须等待用户在界面明确确认。"
            + "不要声称已经复现、验证或修改项目，不要输出或猜测密钥、令牌、密码、Cookie 或个人数据。"
            + "最终只输出一个 JSON 对象，不要 Markdown、解释或代码围栏。结构必须严格为："
            + "{\"problemSummary\":\"问题摘要\",\"causeHypotheses\":[{\"hypothesis\":\"原因假设\","
            + "\"supportingEvidence\":[\"支持该假设的事实或待核验依据\"],\"confidence\":0.0}],"
            + "\"supportingEvidence\":[{\"articleId\":1,\"title\":\"证据标题\",\"excerpt\":\"证据摘要\"}],"
            + "\"verificationSteps\":[\"验证步骤\"],\"fixSuggestions\":[\"修复建议\"],"
            + "\"regressionPlan\":[\"回归验证方案\"]}。"
            + "confidence 必须在 0 到 1 之间；没有站内文章证据时 articleId 可为 null，但必须明确证据不足，不能伪造文章。";

    @Override
    public AgentMode mode() {
        return AgentMode.BUG_DIAGNOSIS;
    }

    @Override
    public String systemPrompt() {
        return PROMPT;
    }

    @Override
    public String validateAndNormalize(String input) {
        if (StringUtils.isBlank(input)) {
            throw new IllegalArgumentException("Bug 排查需要提供问题描述、日志或相关代码");
        }
        String sanitized = SECRET_VALUE.matcher(BEARER_TOKEN.matcher(input).replaceAll("$1[REDACTED]")).replaceAll("$1[REDACTED]");
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH)
                    + "\n\n[日志/代码内容过长，已截断为前 " + MAX_INPUT_LENGTH + " 个字符；请补充最相关的异常栈和代码片段。]";
        }
        return sanitized;
    }

    @Override
    public boolean requiresStructuredOutput() {
        return true;
    }

    @Override
    public String displayQuestion(String normalizedInput) {
        return "Bug 排查：" + normalizedInput;
    }
}
