package com.trademaster.marketdata.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Provider Metrics DTO
 *
 * Comprehensive metrics for market data provider performance:
 * - Response times and latency tracking
 * - Success/failure rates and reliability
 * - Usage statistics and rate limiting
 * - Cost and efficiency metrics
 *
 * Converted to immutable record for MANDATORY RULE #9 compliance.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record ProviderMetrics(
    // Provider identification
    String providerId,
    String providerName,
    LocalDateTime timestamp,

    // Performance metrics
    double avgLatencyMs,
    double minLatencyMs,
    double maxLatencyMs,
    double p95LatencyMs,
    double p99LatencyMs,

    // Reliability metrics
    long totalRequests,
    long successfulRequests,
    long failedRequests,
    double successRate,
    double errorRate,

    // Usage and rate limiting
    long dailyRequestsUsed,
    long dailyRequestsLimit,
    double dailyUsagePercentage,
    int remainingRequests,
    LocalDateTime rateLimitResetTime,

    // Cost metrics
    double costPerRequest,
    double dailyCost,
    double monthlyCost,
    boolean isFreeTier,

    // Connection health
    boolean isConnected,
    boolean isHealthy,
    LocalDateTime lastSuccessfulRequest,
    LocalDateTime lastFailedRequest,
    String lastErrorMessage,

    // Data quality metrics
    long dataPointsReceived,
    long duplicateDataPoints,
    long outOfOrderDataPoints,
    double dataQualityScore,

    // Provider-specific metrics
    long subscribedSymbols,
    long activeConnections,
    double throughputPerSecond
) {

    /**
     * Calculate efficiency score based on cost, latency, and reliability
     */
    public double getEfficiencyScore() {
        if (totalRequests == 0) return 0.0;

        double costScore = isFreeTier ? 1.0 : Math.max(0.1, 1.0 / Math.max(costPerRequest, 0.001));
        double latencyScore = Math.max(0.1, 100.0 / Math.max(avgLatencyMs, 1.0));
        double reliabilityScore = successRate / 100.0;

        return (costScore * 0.4 + latencyScore * 0.3 + reliabilityScore * 0.3) * 100.0;
    }

    /**
     * Check if provider is performing within acceptable thresholds
     */
    public boolean isPerformingWell() {
        return isHealthy &&
               successRate >= 95.0 &&
               avgLatencyMs <= 1000.0 &&
               dailyUsagePercentage <= 90.0;
    }

    /**
     * Check if provider is approaching rate limits
     */
    public boolean isApproachingRateLimit() {
        return dailyUsagePercentage >= 80.0;
    }
}