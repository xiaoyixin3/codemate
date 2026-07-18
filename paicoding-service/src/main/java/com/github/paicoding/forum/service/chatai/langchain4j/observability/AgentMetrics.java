package com.github.paicoding.forum.service.chatai.langchain4j.observability;

import com.github.paicoding.forum.service.chatai.agent.AgentMode;
import dev.langchain4j.model.output.TokenUsage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AgentMetrics {
    private final MeterRegistry registry;
    private final MeterRegistry fallbackRegistry = new SimpleMeterRegistry();

    public AgentMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this.registry = registryProvider.getIfAvailable();
    }

    public void request(AgentMode mode) {
        counter("codemate.agent.requests", mode).increment();
    }

    public void success(AgentMode mode, Duration duration, TokenUsage tokenUsage) {
        counter("codemate.agent.success", mode).increment();
        timer("codemate.agent.duration", mode).record(duration);
        if (tokenUsage != null && tokenUsage.totalTokenCount() != null) {
            Counter.builder("codemate.agent.tokens")
                    .tag("mode", mode.name())
                    .register(activeRegistry())
                    .increment(tokenUsage.totalTokenCount());
        }
    }

    public void error(AgentMode mode) {
        counter("codemate.agent.errors", mode).increment();
    }

    public void retrieved(AgentMode mode, int count) {
        if (count > 0) {
            Counter.builder("codemate.agent.rag.retrieved")
                    .tag("mode", mode.name())
                    .register(activeRegistry())
                    .increment(count);
        }
    }

    public void toolExecuted(AgentMode mode, boolean failed) {
        Counter.builder("codemate.agent.tools")
                .tag("mode", mode.name())
                .tag("result", failed ? "failed" : "success")
                .register(activeRegistry())
                .increment();
    }

    private Counter counter(String name, AgentMode mode) {
        return Counter.builder(name).tag("mode", mode.name()).register(activeRegistry());
    }

    private Timer timer(String name, AgentMode mode) {
        return Timer.builder(name).tag("mode", mode.name()).register(activeRegistry());
    }

    private MeterRegistry activeRegistry() {
        return registry == null ? fallbackRegistry : registry;
    }
}
