package com.github.paicoding.forum.service.chatai.langchain4j.tool;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ToolMetrics {
    private final MeterRegistry registry;
    private final MeterRegistry fallbackRegistry = new SimpleMeterRegistry();

    @Autowired
    public ToolMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this.registry = registryProvider.getIfAvailable();
    }

    ToolMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String tool, ToolRiskLevel risk, boolean success, String errorType, Duration duration) {
        String outcome = success ? "success" : "failure";
        Counter.builder("codemate.agent.tool.calls")
                .tag("tool", tool).tag("risk", risk.name()).tag("outcome", outcome)
                .tag("error", errorType == null ? "none" : errorType)
                .register(activeRegistry()).increment();
        Timer.builder("codemate.agent.tool.duration")
                .tag("tool", tool).tag("risk", risk.name()).tag("outcome", outcome)
                .register(activeRegistry()).record(duration);
    }

    private MeterRegistry activeRegistry() {
        return registry == null ? fallbackRegistry : registry;
    }
}
