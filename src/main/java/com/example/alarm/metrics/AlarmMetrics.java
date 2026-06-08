package com.example.alarm.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class AlarmMetrics {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

    public AlarmMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAnalysis(String status, String model) {
        Counter c = counters.computeIfAbsent("analysis_" + status,
                k -> Counter.builder("alarm.analysis.total")
                        .description("Total analysis count")
                        .tag("status", status)
                        .tag("model", model)
                        .register(registry));
        c.increment();
    }

    public void recordStepLatency(String step, long latencyMs) {
        Timer t = timers.computeIfAbsent("step_" + step,
                k -> Timer.builder("alarm.analysis.latency")
                        .description("Step latency")
                        .tag("step", step)
                        .register(registry));
        t.record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordModelCall(String provider, String status) {
        Counter c = counters.computeIfAbsent("model_" + provider + "_" + status,
                k -> Counter.builder("alarm.model.calls")
                        .description("Model call count")
                        .tag("provider", provider)
                        .tag("status", status)
                        .register(registry));
        c.increment();
    }

    public void recordToolCall(String tool, String status) {
        Counter c = counters.computeIfAbsent("tool_" + tool + "_" + status,
                k -> Counter.builder("alarm.tool.calls")
                        .description("Tool call count")
                        .tag("tool", tool)
                        .tag("status", status)
                        .register(registry));
        c.increment();
    }

    public void recordFallback(String reason) {
        Counter c = counters.computeIfAbsent("fallback_" + reason,
                k -> Counter.builder("alarm.fallback.activated")
                        .description("Fallback activation count")
                        .tag("reason", reason)
                        .register(registry));
        c.increment();
    }
}
