package com.trademaster.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMetrics {

    // Provider identification
    private String providerId;
    private String providerName;
    private LocalDateTime timestamp;
    
    // Performance metrics
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    
    // Reliability metrics
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double successRate;
    private double errorRate;
    
    // Usage and rate limiting
    private long dailyRequestsUsed;
    private long dailyRequestsLimit;
    private double dailyUsagePercentage;
    private int remainingRequests;
    private LocalDateTime rateLimitResetTime;
    
    // Cost metrics
    private double costPerRequest;
    private double dailyCost;
    private double monthlyCost;
    private boolean isFreeTier;
    
    // Connection health
    private boolean isConnected;
    private boolean isHealthy;
    private LocalDateTime lastSuccessfulRequest;
    private LocalDateTime lastFailedRequest;
    private String lastErrorMessage;
    
    // Data quality metrics
    private long dataPointsReceived;
    private long duplicateDataPoints;
    private long outOfOrderDataPoints;
    private double dataQualityScore;
    
    // Provider-specific metrics
    private long subscribedSymbols;
    private long activeConnections;
    private double throughputPerSecond;
    
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