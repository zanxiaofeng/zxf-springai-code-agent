package com.codeinsight.web.metrics;

import com.codeinsight.model.enums.ScenarioType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class AgentMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeIndexingTasks;

    public AgentMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.activeIndexingTasks = registry.gauge("codeinsight.indexing.active",
                new AtomicInteger(0));
    }

    public void recordChatRequest(ScenarioType scenario) {
        Counter.builder("codeinsight.chat.requests")
                .tag("scenario", scenario.name())
                .description("Total chat requests")
                .register(registry).increment();
    }

    public Timer.Sample startModelCall() {
        return Timer.start(registry);
    }

    public void stopModelCall(Timer.Sample sample, String model) {
        sample.stop(Timer.builder("codeinsight.model.call.duration")
                .tag("model", model)
                .description("AI model call duration")
                .register(registry));
    }

    public void incrementActiveIndexing() {
        activeIndexingTasks.incrementAndGet();
    }

    public void decrementActiveIndexing() {
        activeIndexingTasks.decrementAndGet();
    }

    public void recordIndexingComplete(String projectId, long durationMs) {
        Timer.builder("codeinsight.indexing.duration")
                .description("Indexing pipeline duration")
                .register(registry)
                .record(java.time.Duration.ofMillis(durationMs));
    }
}
