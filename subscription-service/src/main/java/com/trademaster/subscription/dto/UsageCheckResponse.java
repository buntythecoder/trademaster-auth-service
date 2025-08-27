package com.trademaster.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Usage Check Response DTO
 * 
 * Response object for feature usage checks and increments.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageCheckResponse {

    /**
     * Whether user can use the feature
     */
    private Boolean canUse;

    /**
     * User ID
     */
    private UUID userId;

    /**
     * Feature name that was checked
     */
    private String featureName;

    /**
     * Current usage count after operation
     */
    private Long currentUsage;

    /**
     * Usage limit for this feature (-1 for unlimited)
     */
    private Long usageLimit;

    /**
     * Remaining usage allowance
     */
    private Long remainingUsage;

    /**
     * Usage percentage (0-100)
     */
    private Double usagePercentage;

    /**
     * Whether the limit was exceeded by this operation
     */
    private Boolean limitExceeded;

    /**
     * Whether feature has unlimited usage
     */
    private Boolean isUnlimited;

    /**
     * Warning level based on current usage
     */
    private UsageTrackingResponse.WarningLevel warningLevel;

    /**
     * Warning message (if applicable)
     */
    private String warningMessage;

    /**
     * Subscription tier providing this feature access
     */
    private String subscriptionTier;

    /**
     * Whether usage was actually incremented
     */
    private Boolean usageIncremented;

    /**
     * Create a successful response
     */
    public static UsageCheckResponse allowed(UUID userId, String featureName, 
                                           Long currentUsage, Long usageLimit, 
                                           String subscriptionTier, boolean incremented) {
        return UsageCheckResponse.builder()
            .canUse(true)
            .userId(userId)
            .featureName(featureName)
            .currentUsage(currentUsage)
            .usageLimit(usageLimit)
            .remainingUsage(usageLimit == -1 ? Long.MAX_VALUE : Math.max(0, usageLimit - currentUsage))
            .usagePercentage(usageLimit == -1 ? 0.0 : Math.min(100.0, (currentUsage.doubleValue() / usageLimit.doubleValue()) * 100.0))
            .limitExceeded(false)
            .isUnlimited(usageLimit == -1)
            .warningLevel(calculateWarningLevel(currentUsage, usageLimit))
            .subscriptionTier(subscriptionTier)
            .usageIncremented(incremented)
            .build();
    }

    /**
     * Create a denied response
     */
    public static UsageCheckResponse denied(UUID userId, String featureName, 
                                          Long currentUsage, Long usageLimit, 
                                          String reason, String subscriptionTier) {
        return UsageCheckResponse.builder()
            .canUse(false)
            .userId(userId)
            .featureName(featureName)
            .currentUsage(currentUsage)
            .usageLimit(usageLimit)
            .remainingUsage(0L)
            .usagePercentage(100.0)
            .limitExceeded(true)
            .isUnlimited(false)
            .warningLevel(UsageTrackingResponse.WarningLevel.CRITICAL)
            .warningMessage(reason)
            .subscriptionTier(subscriptionTier)
            .usageIncremented(false)
            .build();
    }

    /**
     * Create response for no active subscription
     */
    public static UsageCheckResponse noSubscription(UUID userId, String featureName) {
        return UsageCheckResponse.builder()
            .canUse(false)
            .userId(userId)
            .featureName(featureName)
            .currentUsage(0L)
            .usageLimit(0L)
            .remainingUsage(0L)
            .usagePercentage(0.0)
            .limitExceeded(false)
            .isUnlimited(false)
            .warningLevel(UsageTrackingResponse.WarningLevel.NONE)
            .warningMessage("No active subscription found")
            .subscriptionTier("NONE")
            .usageIncremented(false)
            .build();
    }

    private static UsageTrackingResponse.WarningLevel calculateWarningLevel(Long currentUsage, Long usageLimit) {
        if (usageLimit == -1) {
            return UsageTrackingResponse.WarningLevel.NONE;
        }
        
        double percentage = (currentUsage.doubleValue() / usageLimit.doubleValue()) * 100.0;
        
        if (percentage >= 100.0) {
            return UsageTrackingResponse.WarningLevel.CRITICAL;
        } else if (percentage >= 90.0) {
            return UsageTrackingResponse.WarningLevel.HIGH;
        } else if (percentage >= 80.0) {
            return UsageTrackingResponse.WarningLevel.MEDIUM;
        } else if (percentage >= 60.0) {
            return UsageTrackingResponse.WarningLevel.LOW;
        } else {
            return UsageTrackingResponse.WarningLevel.NONE;
        }
    }
}