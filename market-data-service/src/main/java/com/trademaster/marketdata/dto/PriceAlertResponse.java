package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.constants.ResponseMessages;
import com.trademaster.marketdata.entity.PriceAlert;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Price Alert Response DTO
 * 
 * Response structure for price alert operations with comprehensive
 * analytics, performance metrics, and system insights.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record PriceAlertResponse(
    
    // Response metadata
    boolean success,
    String message,
    Instant timestamp,
    String requestId,
    
    // Alert data
    List<PriceAlertDto> alerts,
    PriceAlertDto singleAlert,
    
    // Pagination
    PaginationInfo pagination,
    
    // Analytics and insights
    AlertAnalytics analytics,
    PerformanceMetrics performance,
    SystemHealth systemHealth,
    List<AlertRecommendation> recommendations,
    
    // Market context
    MarketContext marketContext,
    
    // Validation and errors
    List<ValidationError> validationErrors,
    List<String> warnings
    
) {
    
    /**
     * Individual Price Alert DTO
     */
    @Builder
    public record PriceAlertDto(
        Long id,
        String userId,
        String name,
        String description,
        String symbol,
        String exchange,
        
        // Configuration
        PriceAlert.AlertType alertType,
        String alertTypeDescription,
        PriceAlert.TriggerCondition triggerCondition,
        String triggerConditionDescription,
        PriceAlert.Priority priority,
        String priorityDescription,
        
        // Price conditions
        BigDecimal targetPrice,
        BigDecimal stopPrice,
        BigDecimal baselinePrice,
        BigDecimal percentageChange,
        
        // Technical conditions
        BigDecimal movingAveragePrice,
        Integer movingAveragePeriod,
        BigDecimal rsiThreshold,
        BigDecimal volumeThreshold,
        BigDecimal volatilityThreshold,
        
        // Advanced conditions
        Map<String, Object> multiConditions,
        Map<String, Object> customParameters,
        
        // Status and lifecycle
        PriceAlert.AlertStatus status,
        String statusDescription,
        Boolean isTriggered,
        Boolean isActive,
        Boolean isRecurring,
        
        // Timing
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime triggeredAt,
        LocalDateTime expiresAt,
        LocalDateTime lastCheckedAt,
        LocalDateTime nextCheckAt,
        
        // Trigger details
        BigDecimal triggeredPrice,
        Long triggeredVolume,
        Map<String, Object> triggerContext,
        String triggerReason,
        
        // Notification settings
        PriceAlert.NotificationMethod notificationMethod,
        String notificationMethodDescription,
        Map<String, Object> notificationSettings,
        Boolean emailSent,
        Boolean smsSent,
        Boolean pushSent,
        Integer notificationAttempts,
        
        // Performance tracking
        Integer timesTriggered,
        Integer falsePositives,
        BigDecimal accuracyScore,
        Long averageResponseTimeMs,
        
        // Market context
        BigDecimal marketPrice,
        Long marketVolume,
        Instant lastPriceUpdate,
        Map<String, Object> marketIndicators,
        
        // Derived fields
        String alertSummary,
        String urgencyLevel,
        Long hoursUntilExpiry,
        Long minutesSinceLastCheck,
        BigDecimal distanceFromTarget,
        Boolean isDueForCheck,
        Boolean hasExpired,
        Boolean needsImmediateAttention
    ) {
        
        public static PriceAlertDto fromEntity(PriceAlert alert, boolean includePerformanceMetrics,
                boolean includeMarketContext, boolean includeTriggerHistory) {
            
            var builder = PriceAlertDto.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .name(alert.getName())
                .description(alert.getDescription())
                .symbol(alert.getSymbol())
                .exchange(alert.getExchange())
                .alertType(alert.getAlertType())
                .alertTypeDescription(alert.getAlertType().getDescription())
                .triggerCondition(alert.getTriggerCondition())
                .triggerConditionDescription(alert.getTriggerCondition().getDescription())
                .priority(alert.getPriority())
                .priorityDescription(alert.getPriority().getDescription())
                .targetPrice(alert.getTargetPrice())
                .stopPrice(alert.getStopPrice())
                .baselinePrice(alert.getBaselinePrice())
                .percentageChange(alert.getPercentageChange())
                .movingAveragePrice(alert.getMovingAveragePrice())
                .movingAveragePeriod(alert.getMovingAveragePeriod())
                .rsiThreshold(alert.getRsiThreshold())
                .volumeThreshold(alert.getVolumeThreshold())
                .volatilityThreshold(alert.getVolatilityThreshold())
                .status(alert.getStatus())
                .statusDescription(alert.getStatus().getDescription())
                .isTriggered(alert.getIsTriggered())
                .isActive(alert.getIsActive())
                .isRecurring(alert.getIsRecurring())
                .createdAt(alert.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime())
                .updatedAt(alert.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime())
                .triggeredAt(alert.getTriggeredAt())
                .expiresAt(alert.getExpiresAt())
                .lastCheckedAt(alert.getLastCheckedAt())
                .nextCheckAt(alert.getNextCheckAt())
                .notificationMethod(alert.getNotificationMethod())
                .notificationMethodDescription(alert.getNotificationMethod().getDescription())
                .emailSent(alert.getEmailSent())
                .smsSent(alert.getSmsSent())
                .pushSent(alert.getPushSent())
                .notificationAttempts(alert.getNotificationAttempts())
                .alertSummary(alert.getAlertSummary())
                .urgencyLevel(alert.getUrgencyLevel())
                .hoursUntilExpiry(alert.getHoursUntilExpiry())
                .minutesSinceLastCheck(alert.getMinutesSinceLastCheck())
                .distanceFromTarget(alert.getDistanceFromTarget())
                .isDueForCheck(alert.isDueForCheck())
                .hasExpired(alert.hasExpired())
                .needsImmediateAttention(alert.needsImmediateAttention());
            
            // Include performance metrics if requested
            if (includePerformanceMetrics) {
                builder.timesTriggered(alert.getTimesTriggered())
                    .falsePositives(alert.getFalsePositives())
                    .accuracyScore(alert.getAccuracyScore())
                    .averageResponseTimeMs(alert.getAverageResponseTimeMs());
            }
            
            // Include market context if requested
            if (includeMarketContext) {
                builder.marketPrice(alert.getMarketPrice())
                    .marketVolume(alert.getMarketVolume())
                    .lastPriceUpdate(alert.getLastPriceUpdate());
                
                // Parse market indicators JSON (simplified)
                if (alert.getMarketIndicators() != null) {
                    try {
                        // In a real implementation, use Jackson to parse JSON
                        builder.marketIndicators(Map.of("raw", alert.getMarketIndicators()));
                    } catch (Exception e) {
                        builder.marketIndicators(Map.of());
                    }
                }
            }
            
            // Include trigger history if requested
            if (includeTriggerHistory && alert.getIsTriggered()) {
                builder.triggeredPrice(alert.getTriggeredPrice())
                    .triggeredVolume(alert.getTriggeredVolume())
                    .triggerReason(alert.getTriggerReason());
                
                // Parse trigger context JSON (simplified)
                if (alert.getTriggerContext() != null) {
                    try {
                        // In a real implementation, use Jackson to parse JSON
                        builder.triggerContext(Map.of("raw", alert.getTriggerContext()));
                    } catch (Exception e) {
                        builder.triggerContext(Map.of());
                    }
                }
            }
            
            return builder.build();
        }
    }
    
    /**
     * Pagination Information
     */
    @Builder
    public record PaginationInfo(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious,
        boolean isFirst,
        boolean isLast
    ) {}
    
    /**
     * Alert Analytics
     */
    @Builder
    public record AlertAnalytics(
        long totalAlerts,
        long activeAlerts,
        long triggeredAlerts,
        long expiredAlerts,
        long pausedAlerts,
        
        // Priority distribution
        Map<PriceAlert.Priority, Long> alertsByPriority,
        
        // Type distribution
        Map<PriceAlert.AlertType, Long> alertsByType,
        
        // Status distribution
        Map<PriceAlert.AlertStatus, Long> alertsByStatus,
        
        // Symbol distribution
        Map<String, Long> alertsBySymbol,
        
        // Exchange distribution
        Map<String, Long> alertsByExchange,
        
        // Time-based analytics
        long alertsCreatedToday,
        long alertsTriggeredToday,
        long alertsExpiringToday,
        Map<Integer, Long> alertsByHour, // Hour of day → count
        
        // Performance insights
        BigDecimal averageAccuracyScore,
        Long averageResponseTime,
        BigDecimal triggerSuccessRate,
        
        // Trending
        List<String> trendingSymbols,
        List<PriceAlert.AlertType> trendingAlertTypes,
        List<String> mostActiveUsers
    ) {}
    
    /**
     * Performance Metrics
     */
    @Builder
    public record PerformanceMetrics(
        long totalProcessingTime,
        long averageProcessingTime,
        long queryExecutionTime,
        long cacheHitRate,
        
        // System performance
        int activeConnections,
        long memoryUsage,
        double cpuUsage,
        
        // Alert processing stats
        long alertsProcessedPerSecond,
        long alertsCheckedPerMinute,
        long notificationsSentPerHour,
        
        // Quality metrics
        BigDecimal systemAccuracy,
        Long systemResponseTime,
        BigDecimal uptime,
        
        // Error rates
        BigDecimal errorRate,
        Long failedNotifications,
        Long timeouts
    ) {}
    
    /**
     * System Health
     */
    @Builder
    public record SystemHealth(
        String status, // HEALTHY, WARNING, CRITICAL
        BigDecimal healthScore, // 0-100
        
        // Component health
        Map<String, String> componentStatus,
        
        // Alerts about system health
        List<SystemAlert> systemAlerts,
        
        // Resource usage
        ResourceUsage resourceUsage,
        
        // Recent issues
        List<SystemIssue> recentIssues
    ) {
        
        @Builder
        public record SystemAlert(
            String component,
            String severity,
            String message,
            Instant timestamp
        ) {}
        
        @Builder
        public record ResourceUsage(
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            double networkUsage,
            int activeThreads,
            int databaseConnections
        ) {}
        
        @Builder
        public record SystemIssue(
            String issueType,
            String description,
            String severity,
            Instant occurredAt,
            Instant resolvedAt,
            boolean isResolved
        ) {}
    }
    
    /**
     * Alert Recommendations
     */
    @Builder
    public record AlertRecommendation(
        String type, // OPTIMIZATION, CONFIGURATION, MAINTENANCE
        String title,
        String description,
        String priority, // HIGH, MEDIUM, LOW
        List<String> actionItems,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Market Context
     */
    @Builder
    public record MarketContext(
        String marketStatus, // OPEN, CLOSED, PRE_MARKET, AFTER_HOURS
        Instant marketTime,
        
        // Market overview
        Map<String, BigDecimal> majorIndices,
        String marketSentiment, // BULLISH, BEARISH, NEUTRAL
        BigDecimal marketVolatility,
        
        // Relevant to alerts
        List<MarketEvent> relevantEvents,
        Map<String, BigDecimal> symbolPrices,
        Map<String, String> symbolStatus
    ) {
        
        @Builder
        public record MarketEvent(
            String eventType,
            String title,
            String description,
            Instant eventTime,
            List<String> affectedSymbols,
            String impact // HIGH, MEDIUM, LOW
        ) {}
    }
    
    /**
     * Validation Error
     */
    @Builder
    public record ValidationError(
        String field,
        Object rejectedValue,
        String message,
        String errorCode
    ) {}
    
    /**
     * Factory methods for different response types
     */
    public static PriceAlertResponse success(List<PriceAlertDto> alerts, PaginationInfo pagination,
            AlertAnalytics analytics) {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERTS_RETRIEVED_SUCCESS)
            .timestamp(Instant.now())
            .alerts(alerts)
            .pagination(pagination)
            .analytics(analytics)
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse success(PriceAlertDto alert) {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERT_PROCESSED_SUCCESS)
            .timestamp(Instant.now())
            .singleAlert(alert)
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse created(PriceAlertDto alert) {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERT_CREATED_SUCCESS)
            .timestamp(Instant.now())
            .singleAlert(alert)
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse updated(PriceAlertDto alert) {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERT_UPDATED_SUCCESS)
            .timestamp(Instant.now())
            .singleAlert(alert)
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse deleted() {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERT_DELETED_SUCCESS)
            .timestamp(Instant.now())
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse validationError(List<ValidationError> errors) {
        return PriceAlertResponse.builder()
            .success(false)
            .message(ResponseMessages.VALIDATION_FAILED)
            .timestamp(Instant.now())
            .validationErrors(errors)
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse error(String message) {
        return PriceAlertResponse.builder()
            .success(false)
            .message(message)
            .timestamp(Instant.now())
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse withAnalytics(List<PriceAlertDto> alerts, 
            AlertAnalytics analytics, PerformanceMetrics performance, 
            SystemHealth systemHealth, List<AlertRecommendation> recommendations) {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERTS_WITH_ANALYTICS_SUCCESS)
            .timestamp(Instant.now())
            .alerts(alerts)
            .analytics(analytics)
            .performance(performance)
            .systemHealth(systemHealth)
            .recommendations(recommendations)
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
    
    public static PriceAlertResponse withMarketContext(List<PriceAlertDto> alerts,
            MarketContext marketContext) {
        return PriceAlertResponse.builder()
            .success(true)
            .message(ResponseMessages.ALERTS_WITH_CONTEXT_SUCCESS)
            .timestamp(Instant.now())
            .alerts(alerts)
            .marketContext(marketContext)
            .validationErrors(List.of())
            .warnings(List.of())
            .build();
    }
}