package com.github.paicoding.forum.service.chatai.memory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveMemoryGuard {
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|api[ _-]?key|access[ _-]?key|token|cookie|private[ _-]?key|密码|密钥)\\s*[:=：]\\s*\\S+"
                    + "|-----BEGIN [A-Z ]*PRIVATE KEY-----|\\bsk-[A-Za-z0-9_-]{12,}");
    private static final Pattern JSON_CREDENTIAL = Pattern.compile(
            "(?i)(\\\"(?:password|passwd|pwd|secret|api[ _-]?key|access[ _-]?key|token|cookie|private[ _-]?key)\\\"\\s*:\\s*\\\")[^\\\"]+");
    private static final Pattern INLINE_CREDENTIAL = Pattern.compile(
            "(?i)((?:password|passwd|pwd|secret|api[ _-]?key|access[ _-]?key|token|cookie|private[ _-]?key|密码|密钥)\\s*[:=：]\\s*)[^\\s,;\\\"'}]+");
    private static final Pattern TOKEN_PREFIX = Pattern.compile("\\bsk-[A-Za-z0-9_-]{12,}");

    public void validate(String content) {
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("Memory content is required");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Memory content is too long");
        }
        if (SENSITIVE.matcher(content).find()) {
            throw new IllegalArgumentException("Sensitive credentials must not be stored in memory");
        }
    }

    public String redact(String content) {
        if (content == null) return null;
        String redacted = JSON_CREDENTIAL.matcher(content).replaceAll("$1[REDACTED]");
        redacted = INLINE_CREDENTIAL.matcher(redacted).replaceAll("$1[REDACTED]");
        redacted = TOKEN_PREFIX.matcher(redacted).replaceAll("[REDACTED]");
        return redacted.replaceAll("-----BEGIN [A-Z ]*PRIVATE KEY-----.*?-----END [A-Z ]*PRIVATE KEY-----",
                "[REDACTED PRIVATE KEY]");
    }
}
