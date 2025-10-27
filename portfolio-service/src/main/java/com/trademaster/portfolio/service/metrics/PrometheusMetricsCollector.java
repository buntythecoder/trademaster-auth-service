package com.trademaster.portfolio.service.metrics;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;

/**
 * Prometheus Metrics Collector Implementation
 *
 * Production-ready Prometheus metrics collector using Micrometer.
 * Provides high-performance metrics recording with support for virtual threads.
 *
 * Key Features:
 * - Micrometer integration for Prometheus exposition
 * - Thread-safe metrics recording
 * - Virtual thread support for async operations
 * - Tag-based metric categorization
 * - Sub-5ms recording overhead
 *
 * Design Patterns:
 * - Strategy Pattern: Prometheus-specific implementation
 * - Singleton Pattern: Single MeterRegistry per application
 * - Builder Pattern: Dynamic tag construction
 *
 * Rule Compliance:
 * - Rule #3: Functional programming (no if-else)
 * - Rule #12: Virtual threads for async operations
 * - Rule #15: Structured logging with correlation
 * - Rule #22: Performance targets (<5ms)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusMetricsCollector implements MetricsCollector {

    private final MeterRegistry meterRegistry;

    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_NAME = "portfolio";

    // ==================== CORE METRICS OPERATIONS ====================

    @Override
    public void recordEvent(String eventName, Map<String, String> tags) {
        buildCounter("portfolio.events", eventName, tags).increment();
        log.trace("Recorded event: {} with tags: {}", eventName, tags);
    }

    @Override
    public void recordTiming(String operation, long durationMs, Map<String, String> tags) {
        buildTimer(operation, tags).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.trace("Recorded timing: {} = {}ms with tags: {}", operation, durationMs, tags);
    }

    @Override
    public void incrementCounter(String counterName, Map<String, String> tags) {
        buildCounter(counterName, null, tags).increment();
        log.trace("Incremented counter: {} with tags: {}", counterName, tags);
    }

    @Override
    public void incrementCounter(String counterName, double amount, Map<String, String> tags) {
        buildCounter(counterName, null, tags).increment(amount);
        log.trace("Incremented counter: {} by {} with tags: {}", counterName, amount, tags);
    }

    @Override
    public void recordGauge(String gaugeName, double value, Map<String, String> tags) {
        buildGauge(gaugeName, value, tags);
        log.trace("Recorded gauge: {} = {} with tags: {}", gaugeName, value, tags);
    }

    @Override
    public void recordHistogram(String histogramName, double value, Map<String, String> tags) {
        buildDistributionSummary(histogramName, tags).record(value);
        log.trace("Recorded histogram: {} = {} with tags: {}", histogramName, value, tags);
    }

    // ==================== ASYNC OPERATIONS ====================

    @Override
    public CompletableFuture<Void> recordEventAsync(String eventName, Map<String, String> tags) {
        return CompletableFuture.runAsync(
            () -> recordEvent(eventName, tags),
            VIRTUAL_EXECUTOR
        );
    }

    @Override
    public CompletableFuture<Void> recordTimingAsync(String operation, long durationMs, Map<String, String> tags) {
        return CompletableFuture.runAsync(
            () -> recordTiming(operation, durationMs, tags),
            VIRTUAL_EXECUTOR
        );
    }

    // ==================== TIMER OPERATIONS ====================

    @Override
    public TimerSample startTimer() {
        return new PrometheusTimerSample(Timer.start(meterRegistry));
    }

    // ==================== ERROR TRACKING ====================

    @Override
    public void recordError(String errorType, String errorCode, Map<String, String> tags) {
        Map<String, String> errorTags = enrichTags(tags, Map.of(
            "error_type", errorType,
            "error_code", errorCode
        ));
        buildCounter("portfolio.errors", null, errorTags).increment();
        log.debug("Recorded error: type={}, code={} with tags: {}", errorType, errorCode, tags);
    }

    @Override
    public void recordError(String errorType, String errorCode, Throwable exception, Map<String, String> tags) {
        Map<String, String> errorTags = enrichTags(tags, Map.of(
            "error_type", errorType,
            "error_code", errorCode,
            "exception_class", exception.getClass().getSimpleName()
        ));
        buildCounter("portfolio.errors", null, errorTags).increment();
        log.debug("Recorded error: type={}, code={}, exception={} with tags: {}",
            errorType, errorCode, exception.getMessage(), tags);
    }

    // ==================== OPERATION STATUS ====================

    @Override
    public void recordOperationStatus(String operation, boolean success, Map<String, String> tags) {
        Map<String, String> statusTags = enrichTags(tags, Map.of(
            "operation", operation,
            "status", success ? "success" : "failure"
        ));
        buildCounter("portfolio.operations", null, statusTags).increment();
        log.trace("Recorded operation status: {} = {} with tags: {}", operation, success, tags);
    }

    // ==================== BUSINESS METRICS ====================

    @Override
    public void recordBusinessMetric(String metricName, double value, Map<String, String> tags) {
        buildGauge(metricName, value, tags);
        log.trace("Recorded business metric: {} = {} with tags: {}", metricName, value, tags);
    }

    // ==================== METRIC BUILDERS ====================

    /**
     * Build or retrieve a counter with tags
     *
     * Rule #3: Functional builder pattern with immutable configuration
     * Rule #22: Cached counter instances for performance
     */
    private Counter buildCounter(String name, String eventName, Map<String, String> tags) {
        Counter.Builder builder = Counter.builder(name)
            .tag(SERVICE_TAG, SERVICE_NAME);

        // Add event name if provided (functional pattern - switch expression)
        builder = switch (eventName) {
            case null -> builder;
            case String event -> builder.tag("event", event);
        };

        // Add all custom tags
        tags.forEach(builder::tag);

        return builder.register(meterRegistry);
    }

    /**
     * Build or retrieve a timer with tags
     *
     * Rule #3: Functional builder pattern
     * Rule #22: High-performance timer registration
     */
    private Timer buildTimer(String operation, Map<String, String> tags) {
        Timer.Builder builder = Timer.builder(operation)
            .tag(SERVICE_TAG, SERVICE_NAME);

        // Add all custom tags
        tags.forEach(builder::tag);

        return builder.register(meterRegistry);
    }

    /**
     * Build or retrieve a gauge with tags
     *
     * Rule #3: Functional gauge builder
     * Rule #22: Real-time gauge registration
     */
    private Gauge buildGauge(String gaugeName, double value, Map<String, String> tags) {
        var builder = Gauge.builder(gaugeName, () -> value)
            .tag(SERVICE_TAG, SERVICE_NAME);

        // Add all custom tags
        tags.forEach(builder::tag);

        return builder.register(meterRegistry);
    }

    /**
     * Build or retrieve a distribution summary (histogram) with tags
     *
     * Rule #3: Functional distribution summary builder
     * Rule #22: Statistical distribution tracking
     */
    private DistributionSummary buildDistributionSummary(String histogramName, Map<String, String> tags) {
        DistributionSummary.Builder builder = DistributionSummary.builder(histogramName)
            .tag(SERVICE_TAG, SERVICE_NAME);

        // Add all custom tags
        tags.forEach(builder::tag);

        return builder.register(meterRegistry);
    }

    /**
     * Enrich tags with additional key-value pairs
     *
     * Rule #3: Functional tag enrichment with immutable maps
     */
    private Map<String, String> enrichTags(Map<String, String> baseTags, Map<String, String> additionalTags) {
        return java.util.stream.Stream.concat(
            baseTags.entrySet().stream(),
            additionalTags.entrySet().stream()
        ).collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (v1, v2) -> v2  // In case of duplicates, use new value
        ));
    }

    // ==================== TIMER SAMPLE IMPLEMENTATION ====================

    /**
     * Prometheus Timer Sample Implementation
     *
     * Wraps Micrometer Timer.Sample with MetricsCollector interface.
     *
     * Rule #3: Immutable timer sample
     * Rule #9: Functional design pattern
     */
    private class PrometheusTimerSample implements TimerSample {
        private final Timer.Sample sample;
        private final long startTime;
        private boolean stopped = false;

        PrometheusTimerSample(Timer.Sample sample) {
            this.sample = sample;
            this.startTime = System.nanoTime();
        }

        @Override
        public void stop(String operation, Map<String, String> tags) {
            // Use switch expression for stopped status (Rule #3)
            switch (stopped) {
                case true -> log.warn("Timer already stopped for operation: {}", operation);
                case false -> {
                    sample.stop(buildTimer(operation, tags));
                    stopped = true;
                    log.trace("Stopped timer for operation: {} after {}ms", operation, getElapsedMillis());
                }
            }
        }

        @Override
        public long getElapsedMillis() {
            return (System.nanoTime() - startTime) / 1_000_000;
        }

        @Override
        public boolean isRunning() {
            return !stopped;
        }
    }

}
