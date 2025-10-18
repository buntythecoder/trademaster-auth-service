package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

/**
 * Standardized API response for market data endpoints
 * 
 * Features:
 * - Consistent response format across all endpoints
 * - Success/error state management
 * - Performance metadata inclusion
 * - Subscription tier information
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketDataResponse(
    Long requestId,
    String status,
    Object data,
    java.util.List<String> processingResults,
    String errorMessage,
    Long processingTimeMs,
    boolean success,
    String message,
    ResponseMetadata metadata,
    ErrorDetails error,
    Instant timestamp
) {
    
    /**
     * Create successful response with data
     */
    public static MarketDataResponse success(Object data) {
        return MarketDataResponse.builder()
            .success(true)
            .data(data)
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestId(generateRequestId())
                .responseTime(System.currentTimeMillis())
                .build())
            .build();
    }
    
    /**
     * Create successful response with data and custom metadata
     */
    public static MarketDataResponse success(Object data, ResponseMetadata metadata) {
        return MarketDataResponse.builder()
            .success(true)
            .data(data)
            .metadata(metadata)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Create error response with message
     */
    public static MarketDataResponse error(String message) {
        return MarketDataResponse.builder()
            .success(false)
            .message(message)
            .error(ErrorDetails.builder()
                .code("MARKET_DATA_ERROR")
                .message(message)
                .timestamp(Instant.now())
                .build())
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestId(generateRequestId())
                .responseTime(System.currentTimeMillis())
                .build())
            .build();
    }
    
    /**
     * Create error response with detailed error information
     */
    public static MarketDataResponse error(String code, String message, String details) {
        return MarketDataResponse.builder()
            .success(false)
            .message(message)
            .error(ErrorDetails.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(Instant.now())
                .build())
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestId(generateRequestId())
                .responseTime(System.currentTimeMillis())
                .build())
            .build();
    }
    
    /**
     * Create subscription tier error response
     */
    public static MarketDataResponse subscriptionError(String requiredTier, String currentTier) {
        return MarketDataResponse.builder()
            .success(false)
            .message("Subscription tier insufficient")
            .error(ErrorDetails.builder()
                .code("SUBSCRIPTION_TIER_ERROR")
                .message("Access denied: subscription tier insufficient")
                .details(String.format("Required: %s, Current: %s", requiredTier, currentTier))
                .timestamp(Instant.now())
                .build())
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Create rate limit error response
     */
    public static MarketDataResponse rateLimitError(int requestCount, int limit, long resetTime) {
        return MarketDataResponse.builder()
            .success(false)
            .message("Rate limit exceeded")
            .error(ErrorDetails.builder()
                .code("RATE_LIMIT_EXCEEDED")
                .message("API rate limit exceeded")
                .details(String.format("Requests: %d/%d, Reset: %d", requestCount, limit, resetTime))
                .timestamp(Instant.now())
                .build())
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestId(generateRequestId())
                .responseTime(System.currentTimeMillis())
                .rateLimitInfo(RateLimitInfo.builder()
                    .requestCount(requestCount)
                    .limit(limit)
                    .resetTime(resetTime)
                    .build())
                .build())
            .build();
    }
    
    /**
     * Create performance warning response
     */
    public static MarketDataResponse performanceWarning(Object data, long responseTimeMs) {
        return MarketDataResponse.builder()
            .success(true)
            .data(data)
            .message(responseTimeMs > 100 ? "Response time exceeded target" : null)
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestId(generateRequestId())
                .responseTime(responseTimeMs)
                .performanceWarning(responseTimeMs > 50) // 50ms warning threshold
                .build())
            .build();
    }
    
    private static String generateRequestId() {
        return "req_" + System.nanoTime() + "_" + Thread.currentThread().threadId();
    }
    
    /**
     * Response metadata for performance monitoring and debugging
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResponseMetadata(
        String requestId,
        long responseTime,
        String cacheStatus,
        String dataSource,
        RateLimitInfo rateLimitInfo,
        SubscriptionInfo subscriptionInfo,
        Boolean performanceWarning,
        Instant timestamp
    ) {}
    
    /**
     * Rate limiting information
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RateLimitInfo(
        int requestCount,
        int limit,
        long resetTime,
        String tier
    ) {}
    
    /**
     * Subscription tier information
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubscriptionInfo(
        String tier,
        boolean realtimeAccess,
        boolean historicalAccess,
        boolean orderBookAccess,
        int maxSymbolsPerRequest,
        int maxHistoricalDays
    ) {}
    
    /**
     * Error details for troubleshooting
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetails(
        String code,
        String message,
        String details,
        String suggestion,
        Instant timestamp
    ) {}
    
    /**
     * Data quality information
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DataQuality(
        double qualityScore,
        String qualityLevel,
        boolean isRealtime,
        long dataAge,
        String source
    ) {}
    
    // Compatibility methods for tests expecting JavaBean-style getters
    public Long getRequestId() {
        return requestId;
    }
    
    public String getStatus() {
        return status;
    }
}