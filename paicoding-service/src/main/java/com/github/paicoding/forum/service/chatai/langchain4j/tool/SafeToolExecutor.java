package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.service.chatai.langchain4j.config.LangChain4jProperties;
import com.github.paicoding.forum.service.agentrun.service.AgentRunToolRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class SafeToolExecutor {
    private final ObjectMapper objectMapper;
    private final LangChain4jProperties properties;
    private final ToolMetrics metrics;
    private final ExecutorService executor;
    private final AgentRunToolRecorder traceRecorder;

    @Autowired
    public SafeToolExecutor(ObjectMapper objectMapper, LangChain4jProperties properties, ToolMetrics metrics,
                            AgentRunToolRecorder traceRecorder) {
        this(objectMapper, properties, metrics, traceRecorder,
                Executors.newFixedThreadPool(4, daemonThreadFactory()));
    }

    SafeToolExecutor(ObjectMapper objectMapper, LangChain4jProperties properties, ToolMetrics metrics,
                     ExecutorService executor) {
        this(objectMapper, properties, metrics, null, executor);
    }

    SafeToolExecutor(ObjectMapper objectMapper, LangChain4jProperties properties, ToolMetrics metrics,
                     AgentRunToolRecorder traceRecorder, ExecutorService executor) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
        this.traceRecorder = traceRecorder;
        this.executor = executor;
    }

    public String execute(String toolName, ToolRiskLevel risk, ToolAccess access, Long trustedUserId,
                          Callable<ToolResult<?>> operation) {
        return execute(null, toolName, risk, access, trustedUserId, "", operation);
    }

    public String execute(String memoryId, String toolName, ToolRiskLevel risk, ToolAccess access,
                          Long trustedUserId, String canonicalArguments,
                          Callable<ToolResult<?>> operation) {
        Instant startedAt = Instant.now();
        Future<ToolResult<?>> future = null;
        ToolResult<?> result;
        String errorType = null;
        Long traceStepId = null;
        try {
            checkPermission(access, trustedUserId);
            if (traceRecorder != null) {
                traceStepId = traceRecorder.begin(memoryId, toolName, canonicalArguments);
            }
            future = executor.submit(operation);
            result = future.get(properties.getToolTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            errorType = "TIMEOUT";
            if (future != null) {
                future.cancel(true);
            }
            result = ToolResult.failure(toolName, risk, errorType, "工具执行超时，请缩小查询范围后重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorType = "INTERRUPTED";
            result = ToolResult.failure(toolName, risk, errorType, "工具执行被中断");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            errorType = errorCode(cause);
            result = ToolResult.failure(toolName, risk, errorType, safeMessage(cause));
        } catch (RuntimeException e) {
            errorType = errorCode(e);
            result = ToolResult.failure(toolName, risk, errorType, safeMessage(e));
        }

        Duration duration = Duration.between(startedAt, Instant.now());
        boolean success = result.isSuccess();
        if (!success && errorType == null) {
            errorType = result.getErrorCode();
        }
        metrics.record(toolName, risk, success, errorType, duration);
        if (traceRecorder != null) {
            traceRecorder.finish(traceStepId, success, result.getSummary(), errorType, duration.toMillis());
        }
        log.info("Agent tool completed: tool={}, risk={}, elapsedMs={}, success={}, errorType={}",
                toolName, risk, duration.toMillis(), success, errorType);
        return serializeWithLimit(toolName, risk, result);
    }

    private void checkPermission(ToolAccess access, Long trustedUserId) {
        if (access == ToolAccess.AUTHENTICATED && (trustedUserId == null || trustedUserId <= 0)) {
            throw new ToolExecutionException("AUTHENTICATION_REQUIRED", "请登录后再查询个人任务计划");
        }
    }

    private String serializeWithLimit(String toolName, ToolRiskLevel risk, ToolResult<?> result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            if (json.length() <= properties.getToolMaxOutputChars()) {
                return json;
            }
            ToolResult<Void> limited = ToolResult.success(toolName, risk, null,
                    "结果超过安全长度限制，已省略正文；请缩小查询范围");
            limited.setTruncated(true);
            return objectMapper.writeValueAsString(limited);
        } catch (JsonProcessingException e) {
            return "{\"success\":false,\"tool\":\"" + safeJson(toolName)
                    + "\",\"risk\":\"" + risk.name()
                    + "\",\"summary\":\"结果序列化失败\",\"errorCode\":\"SERIALIZATION_ERROR\",\"truncated\":false}";
        }
    }

    private String errorCode(Throwable error) {
        if (error instanceof ToolExecutionException) {
            return ((ToolExecutionException) error).getErrorCode();
        }
        return "TOOL_EXECUTION_ERROR";
    }

    private String safeMessage(Throwable error) {
        if (error instanceof ToolExecutionException && error.getMessage() != null) {
            return error.getMessage();
        }
        return "工具暂时不可用，请稍后重试";
    }

    private String safeJson(String value) {
        return value == null ? "unknown" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "codemate-agent-tool");
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
