package com.trademaster.marketdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Price Alert Entity
 * 
 * Represents user-defined price alerts with intelligent triggering,
 * multi-condition support, and advanced notification management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "price_alerts", indexes = {
    @Index(name = "idx_price_alerts_user", columnList = "userId"),
    @Index(name = "idx_price_alerts_symbol", columnList = "symbol"),
    @Index(name = "idx_price_alerts_status", columnList = "status"),
    @Index(name = "idx_price_alerts_type", columnList = "alertType"),
    @Index(name = "idx_price_alerts_triggered", columnList = "isTriggered"),
    @Index(name = "idx_price_alerts_expiry", columnList = "expiresAt"),
    @Index(name = "idx_price_alerts_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId; // User who created the alert
    
    @Column(nullable = false)
    private String symbol; // Stock/instrument symbol
    
    @Column(nullable = false)
    private String exchange; // Exchange identifier
    
    // Alert configuration
    @Column(nullable = false, length = 200)
    private String name; // User-defined alert name
    
    @Column(columnDefinition = "TEXT")
    private String description; // Optional description
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TriggerCondition triggerCondition;
    
    // Price conditions
    @Column(precision = 15, scale = 6)
    private BigDecimal targetPrice;
    
    @Column(precision = 15, scale = 6)
    private BigDecimal stopPrice; // For range alerts
    
    @Column(precision = 15, scale = 6)
    private BigDecimal baselinePrice; // Reference price for percentage alerts
    
    @Column(precision = 8, scale = 4)
    private BigDecimal percentageChange; // For percentage-based alerts
    
    // Technical condition parameters
    @Column(precision = 15, scale = 6)
    private BigDecimal movingAveragePrice; // For MA crossover alerts
    
    @Column
    private Integer movingAveragePeriod; // MA period (e.g., 20, 50, 200)
    
    @Column(precision = 8, scale = 4)
    private BigDecimal rsiThreshold; // For RSI alerts
    
    @Column(precision = 15, scale = 6)
    private BigDecimal volumeThreshold; // For volume alerts
    
    @Column(precision = 8, scale = 4)
    private BigDecimal volatilityThreshold; // For volatility alerts
    
    // Advanced conditions
    @Column(columnDefinition = "JSON")
    private String multiConditions; // JSON for complex multi-condition alerts
    
    @Column(columnDefinition = "JSON")
    private String customParameters; // JSON for custom alert parameters
    
    // Status and lifecycle
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;
    
    @Column
    @Builder.Default
    private Boolean isTriggered = false;
    
    @Column
    @Builder.Default
    private Boolean isActive = true;
    
    @Column
    @Builder.Default
    private Boolean isRecurring = false;
    
    // Timing
    @Column
    private LocalDateTime triggeredAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime lastCheckedAt;
    
    @Column
    private LocalDateTime nextCheckAt;
    
    // Trigger details
    @Column(precision = 15, scale = 6)
    private BigDecimal triggeredPrice;
    
    @Column
    private Long triggeredVolume;
    
    @Column(columnDefinition = "JSON")
    private String triggerContext; // JSON with market context at trigger time
    
    @Column(columnDefinition = "TEXT")
    private String triggerReason; // Human-readable trigger explanation
    
    // Notification settings
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationMethod notificationMethod;
    
    @Column(columnDefinition = "JSON")
    private String notificationSettings; // JSON with notification preferences
    
    @Column
    @Builder.Default
    private Boolean emailSent = false;
    
    @Column
    @Builder.Default
    private Boolean smsSent = false;
    
    @Column
    @Builder.Default
    private Boolean pushSent = false;
    
    @Column
    @Builder.Default
    private Integer notificationAttempts = 0;
    
    // Performance tracking
    @Column
    @Builder.Default
    private Integer timesTriggered = 0;
    
    @Column
    @Builder.Default
    private Integer falsePositives = 0;
    
    @Column(precision = 8, scale = 4)
    private BigDecimal accuracyScore; // 0-100 accuracy rating
    
    @Column
    private Long averageResponseTimeMs; // Avg time from trigger to notification
    
    // Market context
    @Column(precision = 15, scale = 6)
    private BigDecimal marketPrice; // Last known market price
    
    @Column
    private Long marketVolume; // Last known volume
    
    @Column
    private Instant lastPriceUpdate; // When price was last updated
    
    @Column(columnDefinition = "JSON")
    private String marketIndicators; // JSON with technical indicators
    
    // Audit fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    /**
     * Alert types
     */
    public enum AlertType {
        PRICE_TARGET("Price reaches target level"),
        PRICE_RANGE("Price breaks out of range"),
        PERCENTAGE_CHANGE("Price changes by percentage"),
        VOLUME_SPIKE("Volume exceeds threshold"),
        MA_CROSSOVER("Moving average crossover"),
        RSI_THRESHOLD("RSI crosses threshold"),
        VOLATILITY_SPIKE("Volatility exceeds threshold"),
        SUPPORT_RESISTANCE("Support/Resistance break"),
        PATTERN_RECOGNITION("Technical pattern detected"),
        NEWS_IMPACT("News-driven price movement"),
        MULTI_CONDITION("Multiple conditions met"),
        CUSTOM("Custom condition logic");
        
        private final String description;
        
        AlertType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Trigger conditions
     */
    public enum TriggerCondition {
        GREATER_THAN("Price above target"),
        LESS_THAN("Price below target"),
        EQUALS("Price equals target"),
        BETWEEN("Price within range"),
        OUTSIDE_RANGE("Price outside range"),
        PERCENTAGE_UP("Price up by percentage"),
        PERCENTAGE_DOWN("Price down by percentage"),
        CROSSES_ABOVE("Crosses above level"),
        CROSSES_BELOW("Crosses below level"),
        MA_GOLDEN_CROSS("Golden cross (fast MA > slow MA)"),
        MA_DEATH_CROSS("Death cross (fast MA < slow MA)"),
        RSI_OVERBOUGHT("RSI above threshold"),
        RSI_OVERSOLD("RSI below threshold"),
        VOLUME_BREAKOUT("Volume breakout"),
        VOLATILITY_EXPANSION("Volatility expansion"),
        CUSTOM_LOGIC("Custom condition logic");
        
        private final String description;
        
        TriggerCondition(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Alert status
     */
    public enum AlertStatus {
        ACTIVE("Active and monitoring"),
        TRIGGERED("Triggered and notified"),
        EXPIRED("Expired without triggering"),
        CANCELLED("Cancelled by user"),
        PAUSED("Temporarily paused"),
        ERROR("Error in processing"),
        PENDING("Pending activation");
        
        private final String description;
        
        AlertStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Priority levels
     */
    public enum Priority {
        LOW("Low priority - routine monitoring"),
        NORMAL("Normal priority - standard alerts"),
        HIGH("High priority - important levels"),
        URGENT("Urgent - critical market events"),
        CRITICAL("Critical - immediate attention required");
        
        private final String description;
        
        Priority(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getWeight() {
            return switch (this) {
                case LOW -> 1;
                case NORMAL -> 2;
                case HIGH -> 3;
                case URGENT -> 4;
                case CRITICAL -> 5;
            };
        }
    }
    
    /**
     * Notification methods
     */
    public enum NotificationMethod {
        EMAIL("Email notification"),
        SMS("SMS notification"),
        PUSH("Push notification"),
        WEBHOOK("Webhook callback"),
        IN_APP("In-app notification"),
        MULTIPLE("Multiple methods"),
        NONE("No notification");
        
        private final String description;
        
        NotificationMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Business logic methods
     */
    
    /**
     * Check if alert is currently active and monitoring
     */
    public boolean isActivelyMonitoring() {
        return status == AlertStatus.ACTIVE && 
               Boolean.TRUE.equals(isActive) && 
               Boolean.FALSE.equals(isTriggered) &&
               (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
    
    /**
     * Check if alert has expired
     */
    public boolean hasExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if alert is due for checking
     */
    public boolean isDueForCheck() {
        return nextCheckAt == null || nextCheckAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if alert needs immediate attention
     */
    public boolean needsImmediateAttention() {
        return priority == Priority.URGENT || priority == Priority.CRITICAL;
    }
    
    /**
     * Get time until expiry in hours
     */
    public Long getHoursUntilExpiry() {
        if (expiresAt == null) return null;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
    }
    
    /**
     * Get time since last check in minutes
     */
    public Long getMinutesSinceLastCheck() {
        if (lastCheckedAt == null) return null;
        return java.time.Duration.between(lastCheckedAt, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Calculate distance from target (percentage)
     */
    public BigDecimal getDistanceFromTarget() {
        if (targetPrice == null || marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        return marketPrice.subtract(targetPrice)
            .divide(marketPrice, 6, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Check if alert should trigger based on current conditions
     */
    public boolean shouldTrigger(BigDecimal currentPrice, Long currentVolume, 
            java.util.Map<String, BigDecimal> technicalIndicators) {
        
        if (!isActivelyMonitoring()) {
            return false;
        }
        
        return switch (triggerCondition) {
            case GREATER_THAN -> currentPrice.compareTo(targetPrice) > 0;
            case LESS_THAN -> currentPrice.compareTo(targetPrice) < 0;
            case EQUALS -> currentPrice.compareTo(targetPrice) == 0;
            case BETWEEN -> stopPrice != null && 
                currentPrice.compareTo(targetPrice) >= 0 && 
                currentPrice.compareTo(stopPrice) <= 0;
            case OUTSIDE_RANGE -> stopPrice != null && 
                (currentPrice.compareTo(targetPrice) < 0 || currentPrice.compareTo(stopPrice) > 0);
            case PERCENTAGE_UP -> baselinePrice != null && percentageChange != null &&
                currentPrice.subtract(baselinePrice).divide(baselinePrice, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).compareTo(percentageChange) >= 0;
            case PERCENTAGE_DOWN -> baselinePrice != null && percentageChange != null &&
                baselinePrice.subtract(currentPrice).divide(baselinePrice, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).compareTo(percentageChange) >= 0;
            case VOLUME_BREAKOUT -> volumeThreshold != null && currentVolume != null &&
                currentVolume.compareTo(volumeThreshold.longValue()) > 0;
            case RSI_OVERBOUGHT -> rsiThreshold != null && technicalIndicators.containsKey("RSI") &&
                technicalIndicators.get("RSI").compareTo(rsiThreshold) > 0;
            case RSI_OVERSOLD -> rsiThreshold != null && technicalIndicators.containsKey("RSI") &&
                technicalIndicators.get("RSI").compareTo(rsiThreshold) < 0;
            default -> false; // Complex conditions handled elsewhere
        };
    }
    
    /**
     * Mark alert as triggered
     */
    public void trigger(BigDecimal triggerPrice, Long triggerVolume, String reason) {
        this.isTriggered = true;
        this.status = AlertStatus.TRIGGERED;
        this.triggeredAt = LocalDateTime.now();
        this.triggeredPrice = triggerPrice;
        this.triggeredVolume = triggerVolume;
        this.triggerReason = reason;
        this.timesTriggered = (timesTriggered != null ? timesTriggered : 0) + 1;
    }
    
    /**
     * Reset alert for recurring use
     */
    public void reset() {
        if (Boolean.TRUE.equals(isRecurring)) {
            this.isTriggered = false;
            this.status = AlertStatus.ACTIVE;
            this.triggeredAt = null;
            this.triggeredPrice = null;
            this.triggeredVolume = null;
            this.triggerReason = null;
            this.triggerContext = null;
            this.emailSent = false;
            this.smsSent = false;
            this.pushSent = false;
        }
    }
    
    /**
     * Update market context
     */
    public void updateMarketContext(BigDecimal price, Long volume, 
            java.util.Map<String, BigDecimal> indicators) {
        this.marketPrice = price;
        this.marketVolume = volume;
        this.lastPriceUpdate = Instant.now();
        this.lastCheckedAt = LocalDateTime.now();
        // Convert indicators to JSON string - simplified
        this.marketIndicators = indicators.toString();
    }
    
    /**
     * Calculate next check time based on priority and volatility
     */
    public LocalDateTime calculateNextCheckTime() {
        int intervalSeconds = switch (priority) {
            case CRITICAL -> 10;    // 10 seconds for critical
            case URGENT -> 30;     // 30 seconds for urgent
            case HIGH -> 60;       // 1 minute for high
            case NORMAL -> 300;    // 5 minutes for normal
            case LOW -> 900;       // 15 minutes for low
        };
        
        return LocalDateTime.now().plusSeconds(intervalSeconds);
    }
    
    /**
     * Get alert summary for display
     */
    public String getAlertSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(symbol).append(" ");
        
        switch (triggerCondition) {
            case GREATER_THAN -> summary.append("above ").append(targetPrice);
            case LESS_THAN -> summary.append("below ").append(targetPrice);
            case PERCENTAGE_UP -> summary.append("up ").append(percentageChange).append("%");
            case PERCENTAGE_DOWN -> summary.append("down ").append(percentageChange).append("%");
            case VOLUME_BREAKOUT -> summary.append("volume > ").append(volumeThreshold);
            case RSI_OVERBOUGHT -> summary.append("RSI > ").append(rsiThreshold);
            case RSI_OVERSOLD -> summary.append("RSI < ").append(rsiThreshold);
            default -> summary.append(triggerCondition.getDescription());
        }
        
        return summary.toString();
    }
    
    /**
     * Get urgency level for notifications
     */
    public String getUrgencyLevel() {
        if (needsImmediateAttention()) return "HIGH";
        if (priority == Priority.HIGH) return "MEDIUM";
        return "LOW";
    }
}