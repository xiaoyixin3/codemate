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
    private static final String PROMPT = "你是 CodeMate Bug 排查 Agent。基于用户提供的问题描述、报错日志、相关代码、运行环境和已尝试方法进行静态分析。"
            + "绝不执行用户代码、命令或系统操作；不要声称已经复现、验证或修改了项目。不要输出或猜测任何密钥、令牌、密码、Cookie 或个人数据。"
            + "使用中文 Markdown，且必须严格按以下标题输出：\n"
            + "## 问题摘要\n## 可能原因列表\n## 最可能原因\n## 排查步骤\n## 修复步骤\n## 代码修改建议\n## 验证步骤\n## 潜在风险\n"
            + "对事实、推测和需要用户验证的内容明确区分；日志或代码不完整时说明缺失信息。";

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
