package com.trademaster.portfolio.service.metrics;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Metrics Collector Service Interface
 *
 * Generic abstraction for metrics collection and monitoring across all portfolio services.
 * Provides a unified API for recording business events, performance timings, and operational metrics
 * with support for async operations using Java 24 Virtual Threads.
 *
 * Key Features:
 * - Event tracking with custom tags for categorization
 * - Performance timing measurements with sub-millisecond precision
 * - Counter-based metrics for operational tracking
 * - Gauge metrics for real-time state monitoring
 * - Histogram metrics for distribution analysis
 * - Async metrics recording with virtual threads
 * - Error tracking with contextual information
 *
 * Performance Targets:
 * - Metrics recording: <5ms overhead per operation
 * - Async recording: <1ms latency
 * - Zero blocking operations
 * - Thread-safe concurrent access
 *
 * Design Patterns:
 * - Interface Segregation: Focused metrics operations
 * - Strategy Pattern: Different implementations (Prometheus, DataDog, etc.)
 * - Observer Pattern: Event-driven metrics collection
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface MetricsCollector {

    /**
     * Record a business event occurrence
     *
     * Rule #3: Functional approach with immutable Map for tags
     * Rule #22: Performance optimized with <5ms overhead
     *
     * @param eventName Event identifier (e.g., "portfolio.created", "order.executed", "position.closed")
     * @param tags Additional context tags for categorization (e.g., {"userId": "123", "portfolioId": "456"})
     */
    void recordEvent(String eventName, Map<String, String> tags);

    /**
     * Record operation timing for performance monitoring
     *
     * Rule #3: Functional approach with immutable timing data
     * Rule #22: Performance tracking with microsecond precision
     *
     * @param operation Operation identifier (e.g., "portfolio.valuation", "risk.assessment", "pnl.calculation")
     * @param durationMs Operation duration in milliseconds
     * @param tags Additional context tags for categorization
     */
    void recordTiming(String operation, long durationMs, Map<String, String> tags);

    /**
     * Increment a counter metric
     *
     * Rule #3: Functional counter increment with tags
     * Rule #22: High-performance counter operations
     *
     * @param counterName Counter identifier (e.g., "api.calls", "cache.hits", "db.queries")
     * @param tags Additional context tags for categorization
     */
    void incrementCounter(String counterName, Map<String, String> tags);

    /**
     * Increment counter by specific amount
     *
     * Rule #3: Functional increment with value
     * Rule #22: Batch counter updates
     *
     * @param counterName Counter identifier
     * @param amount Amount to increment by
     * @param tags Additional context tags
     */
    void incrementCounter(String counterName, double amount, Map<String, String> tags);

    /**
     * Record gauge value (current state measurement)
     *
     * Rule #3: Functional gauge recording
     * Rule #22: Real-time metric tracking
     *
     * @param gaugeName Gauge identifier (e.g., "active.portfolios", "total.positions", "memory.usage")
     * @param value Current gauge value
     * @param tags Additional context tags for categorization
     */
    void recordGauge(String gaugeName, double value, Map<String, String> tags);

    /**
     * Record histogram value for distribution analysis
     *
     * Rule #3: Functional histogram recording
     * Rule #22: Statistical distribution tracking
     *
     * @param histogramName Histogram identifier (e.g., "order.size", "portfolio.value", "api.latency")
     * @param value Value to record in histogram
     * @param tags Additional context tags for categorization
     */
    void recordHistogram(String histogramName, double value, Map<String, String> tags);

    /**
     * Record event asynchronously with virtual threads
     *
     * Rule #12: Virtual thread async operations
     * Rule #3: Functional async pattern
     * Rule #22: Non-blocking metrics recording
     *
     * @param eventName Event identifier
     * @param tags Additional context tags
     * @return CompletableFuture completing when event is recorded
     */
    CompletableFuture<Void> recordEventAsync(String eventName, Map<String, String> tags);

    /**
     * Record timing asynchronously with virtual threads
     *
     * Rule #12: Virtual thread async operations
     * Rule #3: Functional async pattern
     *
     * @param operation Operation identifier
     * @param durationMs Operation duration
     * @param tags Additional context tags
     * @return CompletableFuture completing when timing is recorded
     */
    CompletableFuture<Void> recordTimingAsync(String operation, long durationMs, Map<String, String> tags);

    /**
     * Start a timer for operation measurement
     *
     * Rule #3: Functional timer pattern
     * Rule #22: Zero-overhead timing start
     *
     * @return Timer sample that can be stopped to record duration
     */
    TimerSample startTimer();

    /**
     * Record error occurrence with context
     *
     * Rule #11: Error tracking with functional pattern
     * Rule #15: Structured error logging
     *
     * @param errorType Error type identifier (e.g., "validation.error", "service.timeout", "db.connection")
     * @param errorCode Specific error code for classification
     * @param tags Additional context tags (e.g., {"service": "portfolio", "operation": "create"})
     */
    void recordError(String errorType, String errorCode, Map<String, String> tags);

    /**
     * Record error with exception details
     *
     * Rule #11: Comprehensive error tracking
     * Rule #15: Structured error logging with exception context
     *
     * @param errorType Error type identifier
     * @param errorCode Specific error code
     * @param exception Exception that occurred
     * @param tags Additional context tags
     */
    void recordError(String errorType, String errorCode, Throwable exception, Map<String, String> tags);

    /**
     * Record success/failure status of an operation
     *
     * Rule #3: Functional success tracking
     * Rule #22: Operation outcome monitoring
     *
     * @param operation Operation identifier
     * @param success Whether operation succeeded
     * @param tags Additional context tags
     */
    void recordOperationStatus(String operation, boolean success, Map<String, String> tags);

    /**
     * Record business metric value (financial calculations, etc.)
     *
     * Rule #3: Functional business metric recording
     * Rule #22: Domain-specific metrics
     *
     * @param metricName Business metric identifier (e.g., "total.pnl", "portfolio.value", "realized.gains")
     * @param value Metric value (use BigDecimal.doubleValue() for financial values)
     * @param tags Additional context tags
     */
    void recordBusinessMetric(String metricName, double value, Map<String, String> tags);
}

