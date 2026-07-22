package com.github.paicoding.forum.service.chatai.rag.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RagMetrics {
    private final MeterRegistry registry;
    private final MeterRegistry fallback = new SimpleMeterRegistry();

    public RagMetrics(ObjectProvider<MeterRegistry> provider) {
        this.registry = provider.getIfAvailable();
    }

    public void retrieval(int vectorCandidates, int keywordCandidates, int results, Duration duration) {
        Timer.builder("codemate.rag.retrieval.duration").register(active()).record(duration);
        DistributionSummary.builder("codemate.rag.candidates").tag("source", "vector")
                .register(active()).record(vectorCandidates);
        DistributionSummary.builder("codemate.rag.candidates").tag("source", "keyword")
                .register(active()).record(keywordCandidates);
        DistributionSummary.builder("codemate.rag.results").register(active()).record(results);
    }

    public void index(boolean skipped, int embeddedChunks) {
        Counter.builder("codemate.rag.index.operations").tag("result", skipped ? "skipped" : "updated")
                .register(active()).increment();
        if (embeddedChunks > 0) Counter.builder("codemate.rag.index.embeddings").register(active()).increment(embeddedChunks);
    }

    private MeterRegistry active() {
        return registry == null ? fallback : registry;
    }
}
