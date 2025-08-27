package com.trademaster.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Usage Tracking Response DTO
 * 
 * Response object containing feature usage information and limits.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTrackingResponse {

    /**
     * Usage tracking ID
     */
    private UUID id;

    /**
     * User ID
     */
    private UUID userId;

    /**
     * Associated subscription ID
     */
    private UUID subscriptionId;

    /**
     * Feature name being tracked
     */
    private String featureName;

    /**
     * Current usage count in this period
     */
    private Long usageCount;

    /**
     * Maximum allowed usage for this feature (-1 for unlimited)
     */
    private Long usageLimit;

    /**
     * Period start date
     */
    private LocalDateTime periodStart;

    /**
     * Period end date
     */
    private LocalDateTime periodEnd;

    /**
     * When usage counter resets
     */
    private LocalDateTime resetDate;

    /**
     * Reset frequency in days
     */
    private Integer resetFrequencyDays;

    /**
     * Whether limit has been exceeded
     */
    private Boolean limitExceeded;

    /**
     * Number of times limit was exceeded
     */
    private Integer exceededCount;

    /**
     * First time limit was exceeded in this period
     */
    private LocalDateTime firstExceededAt;

    /**
     * Whether feature has unlimited usage
     */
    private Boolean isUnlimited;

    /**
     * Whether usage is within limits
     */
    private Boolean isWithinLimit;

    /**
     * Remaining usage allowance
     */
    private Long remainingUsage;

    /**
     * Usage percentage (0-100)
     */
    private Double usagePercentage;

    /**
     * Whether period is currently active
     */
    private Boolean isPeriodActive;

    /**
     * Whether usage is approaching limit (>80%)
     */
    private Boolean isApproachingLimit;

    /**
     * Whether usage is at soft limit (>90%)
     */
    private Boolean isAtSoftLimit;

    /**
     * Warning level based on usage
     */
    private WarningLevel warningLevel;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Usage warning levels
     */
    public enum WarningLevel {
        NONE("No Warning", 0),
        LOW("Low Usage", 60),
        MEDIUM("Medium Usage", 80),
        HIGH("High Usage", 90),
        CRITICAL("Limit Exceeded", 100);

        private final String description;
        private final int threshold;

        WarningLevel(String description, int threshold) {
            this.description = description;
            this.threshold = threshold;
        }

        public String getDescription() {
            return description;
        }

        public int getThreshold() {
            return threshold;
        }
    }
}