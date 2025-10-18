package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.dto.ProviderMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Alpha Vantage Metrics Tracker (Metrics & Health Monitoring Only)
 *
 * Single Responsibility: Track performance metrics and health status
 * Following Rule #2 (SRP) and Rule #10 (Immutability with AtomicReference)
 *
 * Features:
 * - Request success/failure tracking with atomic counters
 * - Latency measurement and tracking
 * - Health status calculation (80% success rate threshold)
 * - ProviderMetrics generation for monitoring dashboards
 * - Thread-safe operations with atomic types
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AlphaVantageMetricsTracker {

    private static final String PROVIDER_ID = "alphavantage";
    private static final String PROVIDER_NAME = "Alpha Vantage";

    // Thread-safe counters (Rule #10: Immutability with AtomicReference)
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastRequestTime = new AtomicReference<>();
    private final AtomicReference<Double> lastLatencyMs = new AtomicReference<>(0.0);

    /**
     * Increment request counter at the start of each request
     */
    public void incrementRequestCount() {
        requestCount.incrementAndGet();
        lastRequestTime.set(LocalDateTime.now());
    }

    /**
     * Record successful request with latency
     */
    public void recordSuccessfulRequest(long startTimeMs) {
        successCount.incrementAndGet();
        double latency = System.currentTimeMillis() - startTimeMs;
        lastLatencyMs.set(latency);
        log.trace("Request successful - Latency: {}ms", latency);
    }

    /**
     * Record failed request
     */
    public void recordFailedRequest() {
        failureCount.incrementAndGet();
        log.trace("Request failed");
    }

    /**
     * Get current metrics snapshot
     */
    public ProviderMetrics getMetrics(boolean isConnected, boolean isFreeTier, int dailyRateLimit,
            double costPerRequest, int subscribedSymbols) {
        long total = requestCount.get();
        long success = successCount.get();
        long failure = failureCount.get();
        double latency = lastLatencyMs.get();

        return ProviderMetrics.builder()
            .providerId(PROVIDER_ID)
            .providerName(PROVIDER_NAME)
            .timestamp(LocalDateTime.now())
            .avgLatencyMs(latency)
            .minLatencyMs(latency)
            .maxLatencyMs(latency)
            .totalRequests(total)
            .successfulRequests(success)
            .failedRequests(failure)
            .successRate(total > 0 ? (success * 100.0) / total : 0.0)
            .errorRate(total > 0 ? (failure * 100.0) / total : 0.0)
            .dailyRequestsUsed(total)
            .dailyRequestsLimit(dailyRateLimit)
            .dailyUsagePercentage(total > 0 ? (total * 100.0) / dailyRateLimit : 0.0)
            .costPerRequest(costPerRequest)
            .dailyCost(total * costPerRequest)
            .isFreeTier(isFreeTier)
            .isConnected(isConnected)
            .isHealthy(isHealthy(isConnected))
            .lastSuccessfulRequest(lastRequestTime.get())
            .subscribedSymbols(subscribedSymbols)
            .build();
    }

    /**
     * Check health status (80% success rate threshold)
     */
    public boolean isHealthy(boolean isConnected) {
        long total = requestCount.get();
        return isConnected &&
               (total == 0 || (successCount.get() * 100.0) / total >= 80.0);
    }

    /**
     * Get current latency in milliseconds
     */
    public double getLatencyMs() {
        return lastLatencyMs.get();
    }

    /**
     * Get current success rate percentage
     */
    public double getSuccessRate() {
        long total = requestCount.get();
        return total > 0 ? (successCount.get() * 100.0) / total : 0.0;
    }

    /**
     * Get total request count
     */
    public long getTotalRequests() {
        return requestCount.get();
    }

    /**
     * Get successful request count
     */
    public long getSuccessCount() {
        return successCount.get();
    }

    /**
     * Get failed request count
     */
    public long getFailureCount() {
        return failureCount.get();
    }

    /**
     * Reset all metrics (for testing or periodic resets)
     */
    public void resetMetrics() {
        requestCount.set(0);
        successCount.set(0);
        failureCount.set(0);
        lastLatencyMs.set(0.0);
        lastRequestTime.set(null);
        log.info("Metrics reset for Alpha Vantage provider");
    }
}
