package com.trademaster.portfolio.service.metrics;

import java.util.Map;

/**
 * Timer Sample for duration measurement
 *
 * Functional timer pattern for measuring operation durations
 * with automatic metrics recording.
 *
 * Rule #3: Immutable timer interface
 * Rule #9: Functional design pattern
 * Rule #22: Sub-millisecond precision timing
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface TimerSample {

    /**
     * Stop timer and record duration
     *
     * Rule #3: Functional completion with automatic recording
     * Rule #22: Precise duration measurement
     *
     * @param operation Operation identifier for metrics
     * @param tags Additional context tags
     */
    void stop(String operation, Map<String, String> tags);

    /**
     * Get elapsed time without stopping timer
     *
     * @return Elapsed milliseconds since timer started
     */
    long getElapsedMillis();

    /**
     * Check if timer is still running
     *
     * @return true if timer hasn't been stopped yet
     */
    boolean isRunning();
}
