package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.PriceAlert;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Price Alert Request DTO
 * 
 * Request parameters for creating and configuring price alerts with
 * comprehensive triggering conditions and notification preferences.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record PriceAlertRequest(
    
    // Basic alert configuration
    @NotBlank(message = "Alert name is required")
    @Size(max = 200, message = "Alert name must not exceed 200 characters")
    String name,
    
    String description,
    
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z0-9._-]{1,20}$", message = "Invalid symbol format")
    String symbol,
    
    @NotBlank(message = "Exchange is required")
    String exchange,
    
    // Alert type and conditions
    @NotNull(message = "Alert type is required")
    PriceAlert.AlertType alertType,
    
    @NotNull(message = "Trigger condition is required")
    PriceAlert.TriggerCondition triggerCondition,
    
    @NotNull(message = "Priority is required")
    PriceAlert.Priority priority,
    
    // Price conditions
    @DecimalMin(value = "0.0", inclusive = false, message = "Target price must be positive")
    @Digits(integer = 12, fraction = 6, message = "Invalid target price format")
    BigDecimal targetPrice,
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Stop price must be positive")
    @Digits(integer = 12, fraction = 6, message = "Invalid stop price format")
    BigDecimal stopPrice,
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Baseline price must be positive")
    @Digits(integer = 12, fraction = 6, message = "Invalid baseline price format")
    BigDecimal baselinePrice,
    
    @DecimalMin(value = "0.01", message = "Percentage change must be at least 0.01%")
    @DecimalMax(value = "1000.00", message = "Percentage change cannot exceed 1000%")
    @Digits(integer = 4, fraction = 4, message = "Invalid percentage change format")
    BigDecimal percentageChange,
    
    // Technical condition parameters
    @DecimalMin(value = "0.0", inclusive = false, message = "Moving average price must be positive")
    @Digits(integer = 12, fraction = 6, message = "Invalid moving average price format")
    BigDecimal movingAveragePrice,
    
    @Min(value = 1, message = "Moving average period must be at least 1")
    @Max(value = 500, message = "Moving average period cannot exceed 500")
    Integer movingAveragePeriod,
    
    @DecimalMin(value = "0.0", message = "RSI threshold must be non-negative")
    @DecimalMax(value = "100.0", message = "RSI threshold cannot exceed 100")
    @Digits(integer = 3, fraction = 4, message = "Invalid RSI threshold format")
    BigDecimal rsiThreshold,
    
    @DecimalMin(value = "1.0", message = "Volume threshold must be at least 1")
    @Digits(integer = 12, fraction = 6, message = "Invalid volume threshold format")
    BigDecimal volumeThreshold,
    
    @DecimalMin(value = "0.0", message = "Volatility threshold must be non-negative")
    @DecimalMax(value = "1000.0", message = "Volatility threshold cannot exceed 1000%")
    @Digits(integer = 4, fraction = 4, message = "Invalid volatility threshold format")
    BigDecimal volatilityThreshold,
    
    // Advanced conditions (JSON strings)
    String multiConditions,
    String customParameters,
    
    // Timing and lifecycle
    @Future(message = "Expiry time must be in the future")
    LocalDateTime expiresAt,
    
    Boolean isRecurring,
    
    // Notification settings
    @NotNull(message = "Notification method is required")
    PriceAlert.NotificationMethod notificationMethod,
    
    String notificationSettings, // JSON string with notification preferences
    
    // Filtering and search parameters (for querying existing alerts)
    Set<String> symbols,
    Set<String> exchanges,
    Set<PriceAlert.AlertType> alertTypes,
    Set<PriceAlert.TriggerCondition> triggerConditions,
    Set<PriceAlert.AlertStatus> statuses,
    Set<PriceAlert.Priority> priorities,
    Set<PriceAlert.NotificationMethod> notificationMethods,
    
    // Status filtering
    Boolean activeOnly,
    Boolean triggeredOnly,
    Boolean expiredOnly,
    Boolean recentOnly, // Last 24 hours
    Boolean highPriorityOnly,
    Boolean criticalOnly,
    
    // Time filtering
    LocalDateTime createdAfter,
    LocalDateTime createdBefore,
    LocalDateTime triggeredAfter,
    LocalDateTime triggeredBefore,
    LocalDateTime expiresAfter,
    LocalDateTime expiresBefore,
    
    // Performance filtering
    @DecimalMin(value = "0.0", message = "Minimum accuracy score must be non-negative")
    @DecimalMax(value = "100.0", message = "Maximum accuracy score cannot exceed 100")
    BigDecimal minAccuracyScore,
    
    @Min(value = 1, message = "Minimum times triggered must be at least 1")
    Integer minTimesTriggered,
    
    @Max(value = 10000, message = "Maximum response time cannot exceed 10 seconds")
    Long maxResponseTimeMs,
    
    // Search and analysis
    String searchTerm,
    Boolean includeInactive,
    Boolean includePerformanceMetrics,
    Boolean includeMarketContext,
    Boolean includeTriggerHistory,
    
    // Sorting and pagination
    String sortBy, // createdAt, priority, accuracyScore, timesTriggered, etc.
    SortDirection sortDirection,
    
    @Min(0)
    Integer page,
    
    @Min(1) @Max(100)
    Integer size
    
) {
    
    public PriceAlertRequest {
        // Set defaults
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "createdAt";
        if (sortDirection == null) sortDirection = SortDirection.DESC;
        if (activeOnly == null) activeOnly = false;
        if (triggeredOnly == null) triggeredOnly = false;
        if (expiredOnly == null) expiredOnly = false;
        if (recentOnly == null) recentOnly = false;
        if (highPriorityOnly == null) highPriorityOnly = false;
        if (criticalOnly == null) criticalOnly = false;
        if (includeInactive == null) includeInactive = false;
        if (includePerformanceMetrics == null) includePerformanceMetrics = true;
        if (includeMarketContext == null) includeMarketContext = true;
        if (includeTriggerHistory == null) includeTriggerHistory = false;
        if (isRecurring == null) isRecurring = false;
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    /**
     * Validation methods
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               symbol != null && !symbol.trim().isEmpty() &&
               exchange != null && !exchange.trim().isEmpty() &&
               alertType != null && triggerCondition != null &&
               priority != null && notificationMethod != null &&
               validatePriceConditions() && validateTechnicalConditions();
    }
    
    private boolean validatePriceConditions() {
        return switch (triggerCondition) {
            case GREATER_THAN, LESS_THAN, EQUALS, CROSSES_ABOVE, CROSSES_BELOW -> 
                targetPrice != null && targetPrice.compareTo(BigDecimal.ZERO) > 0;
            case BETWEEN, OUTSIDE_RANGE -> 
                targetPrice != null && stopPrice != null && 
                targetPrice.compareTo(BigDecimal.ZERO) > 0 && 
                stopPrice.compareTo(BigDecimal.ZERO) > 0;
            case PERCENTAGE_UP, PERCENTAGE_DOWN -> 
                baselinePrice != null && percentageChange != null &&
                baselinePrice.compareTo(BigDecimal.ZERO) > 0 &&
                percentageChange.compareTo(BigDecimal.ZERO) > 0;
            default -> true; // Other conditions may not require price validation
        };
    }
    
    private boolean validateTechnicalConditions() {
        return switch (alertType) {
            case MA_CROSSOVER -> movingAveragePrice != null && movingAveragePeriod != null &&
                movingAveragePrice.compareTo(BigDecimal.ZERO) > 0 && movingAveragePeriod > 0;
            case RSI_THRESHOLD -> rsiThreshold != null &&
                rsiThreshold.compareTo(BigDecimal.ZERO) >= 0 &&
                rsiThreshold.compareTo(new BigDecimal("100")) <= 0;
            case VOLUME_SPIKE -> volumeThreshold != null &&
                volumeThreshold.compareTo(BigDecimal.ZERO) > 0;
            case VOLATILITY_SPIKE -> volatilityThreshold != null &&
                volatilityThreshold.compareTo(BigDecimal.ZERO) >= 0;
            default -> true; // Other alert types don't require technical validation
        };
    }
    
    public boolean hasFilters() {
        return (symbols != null && !symbols.isEmpty()) ||
               (exchanges != null && !exchanges.isEmpty()) ||
               (alertTypes != null && !alertTypes.isEmpty()) ||
               (triggerConditions != null && !triggerConditions.isEmpty()) ||
               (statuses != null && !statuses.isEmpty()) ||
               (priorities != null && !priorities.isEmpty()) ||
               (notificationMethods != null && !notificationMethods.isEmpty()) ||
               activeOnly || triggeredOnly || expiredOnly || recentOnly ||
               highPriorityOnly || criticalOnly || hasTimeFilters() ||
               hasPerformanceFilters() || hasSearchTerm();
    }
    
    public boolean hasTimeFilters() {
        return createdAfter != null || createdBefore != null ||
               triggeredAfter != null || triggeredBefore != null ||
               expiresAfter != null || expiresBefore != null;
    }
    
    public boolean hasPerformanceFilters() {
        return minAccuracyScore != null || minTimesTriggered != null ||
               maxResponseTimeMs != null;
    }
    
    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }
    
    public boolean isCreationRequest() {
        return name != null && symbol != null && alertType != null &&
               triggerCondition != null && priority != null &&
               notificationMethod != null && !hasFilters();
    }
    
    public boolean isQueryRequest() {
        return hasFilters() || page != null || size != null ||
               sortBy != null || sortDirection != null;
    }
    
    /**
     * Get complexity level based on conditions and filters
     */
    public String getComplexity() {
        int complexity = 0;
        
        // Basic conditions
        if (targetPrice != null) complexity += 1;
        if (stopPrice != null) complexity += 1;
        if (percentageChange != null) complexity += 1;
        
        // Technical conditions
        if (movingAveragePrice != null || movingAveragePeriod != null) complexity += 2;
        if (rsiThreshold != null) complexity += 2;
        if (volumeThreshold != null) complexity += 2;
        if (volatilityThreshold != null) complexity += 2;
        
        // Advanced conditions
        if (multiConditions != null && !multiConditions.trim().isEmpty()) complexity += 3;
        if (customParameters != null && !customParameters.trim().isEmpty()) complexity += 3;
        
        // Filters
        if (hasFilters()) complexity += 1;
        if (hasTimeFilters()) complexity += 1;
        if (hasPerformanceFilters()) complexity += 1;
        
        if (complexity > 8) return "HIGH";
        if (complexity > 4) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Create preset alert requests
     */
    public static PriceAlertRequest priceTarget(String symbol, String exchange, 
            BigDecimal targetPrice, PriceAlert.Priority priority) {
        return PriceAlertRequest.builder()
            .name(symbol + " Price Target")
            .symbol(symbol)
            .exchange(exchange)
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .targetPrice(targetPrice)
            .priority(priority)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }
    
    public static PriceAlertRequest stopLoss(String symbol, String exchange, 
            BigDecimal stopPrice, PriceAlert.Priority priority) {
        return PriceAlertRequest.builder()
            .name(symbol + " Stop Loss")
            .symbol(symbol)
            .exchange(exchange)
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.LESS_THAN)
            .targetPrice(stopPrice)
            .priority(priority)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }
    
    public static PriceAlertRequest percentageAlert(String symbol, String exchange,
            BigDecimal baselinePrice, BigDecimal percentageChange, boolean isUpward) {
        return PriceAlertRequest.builder()
            .name(symbol + " " + (isUpward ? "Up" : "Down") + " " + percentageChange + "%")
            .symbol(symbol)
            .exchange(exchange)
            .alertType(PriceAlert.AlertType.PERCENTAGE_CHANGE)
            .triggerCondition(isUpward ? 
                PriceAlert.TriggerCondition.PERCENTAGE_UP : 
                PriceAlert.TriggerCondition.PERCENTAGE_DOWN)
            .baselinePrice(baselinePrice)
            .percentageChange(percentageChange)
            .priority(PriceAlert.Priority.NORMAL)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }
    
    public static PriceAlertRequest volumeSpike(String symbol, String exchange,
            BigDecimal volumeThreshold) {
        return PriceAlertRequest.builder()
            .name(symbol + " Volume Spike")
            .symbol(symbol)
            .exchange(exchange)
            .alertType(PriceAlert.AlertType.VOLUME_SPIKE)
            .triggerCondition(PriceAlert.TriggerCondition.VOLUME_BREAKOUT)
            .volumeThreshold(volumeThreshold)
            .priority(PriceAlert.Priority.HIGH)
            .notificationMethod(PriceAlert.NotificationMethod.PUSH)
            .build();
    }
    
    public static PriceAlertRequest rsiAlert(String symbol, String exchange,
            BigDecimal rsiThreshold, boolean isOverbought) {
        return PriceAlertRequest.builder()
            .name(symbol + " RSI " + (isOverbought ? "Overbought" : "Oversold"))
            .symbol(symbol)
            .exchange(exchange)
            .alertType(PriceAlert.AlertType.RSI_THRESHOLD)
            .triggerCondition(isOverbought ? 
                PriceAlert.TriggerCondition.RSI_OVERBOUGHT : 
                PriceAlert.TriggerCondition.RSI_OVERSOLD)
            .rsiThreshold(rsiThreshold)
            .priority(PriceAlert.Priority.NORMAL)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }
    
    /**
     * Create query requests for different scenarios
     */
    public static PriceAlertRequest activeAlertsQuery() {
        return PriceAlertRequest.builder()
            .activeOnly(true)
            .sortBy("priority")
            .sortDirection(SortDirection.DESC)
            .includePerformanceMetrics(true)
            .build();
    }
    
    public static PriceAlertRequest recentTriggeredQuery() {
        return PriceAlertRequest.builder()
            .triggeredOnly(true)
            .recentOnly(true)
            .sortBy("triggeredAt")
            .sortDirection(SortDirection.DESC)
            .includeTriggerHistory(true)
            .build();
    }
    
    public static PriceAlertRequest highPriorityQuery() {
        return PriceAlertRequest.builder()
            .highPriorityOnly(true)
            .activeOnly(true)
            .sortBy("createdAt")
            .sortDirection(SortDirection.ASC)
            .includeMarketContext(true)
            .build();
    }
    
    public static PriceAlertRequest symbolAlertsQuery(String symbol) {
        return PriceAlertRequest.builder()
            .symbols(Set.of(symbol))
            .includeInactive(true)
            .sortBy("createdAt")
            .sortDirection(SortDirection.DESC)
            .includePerformanceMetrics(true)
            .build();
    }
}