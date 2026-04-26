package com.codeinsight.web.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics for CodeInsight.
 * Spring AI auto-configures gen_ai.* metrics (model call duration, token usage)
 * via ObservationRegistry, so we only track application-specific metrics here.
 */
@Component
@Slf4j
public class AgentMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeIndexingTasks;

    public AgentMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.activeIndexingTasks = registry.gauge("codeinsight.indexing.active", new AtomicInteger(0));
    }

    public void incrementActiveIndexing() {
        activeIndexingTasks.incrementAndGet();
    }

    public void decrementActiveIndexing() {
        activeIndexingTasks.decrementAndGet();
    }

    public void recordIndexingComplete(String projectId, long durationMs) {
        Timer.builder("codeinsight.indexing.duration")
                .tag("projectId", projectId)
                .description("Indexing pipeline duration")
                .register(registry)
                .record(Duration.ofMillis(durationMs));
    }
}
