package com.trademaster.subscription.dto;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Subscription Response DTO
 * 
 * Response object containing subscription details and status.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    /**
     * Subscription ID
     */
    private UUID id;

    /**
     * User ID
     */
    private UUID userId;

    /**
     * Current subscription tier
     */
    private SubscriptionTier tier;

    /**
     * Current subscription status
     */
    private SubscriptionStatus status;

    /**
     * Billing cycle
     */
    private BillingCycle billingCycle;

    /**
     * Monthly price (before discounts)
     */
    private BigDecimal monthlyPrice;

    /**
     * Actual billing amount (after discounts)
     */
    private BigDecimal billingAmount;

    /**
     * Currency
     */
    private String currency;

    /**
     * Subscription start date
     */
    private LocalDateTime startDate;

    /**
     * Subscription end date (if applicable)
     */
    private LocalDateTime endDate;

    /**
     * Next billing date
     */
    private LocalDateTime nextBillingDate;

    /**
     * Trial end date (if in trial)
     */
    private LocalDateTime trialEndDate;

    /**
     * Auto-renewal enabled
     */
    private Boolean autoRenewal;

    /**
     * Applied promotion code
     */
    private String promotionCode;

    /**
     * Promotion discount percentage
     */
    private BigDecimal promotionDiscount;

    /**
     * Features available in this tier
     */
    private List<String> features;

    /**
     * Usage limits for this tier
     */
    private SubscriptionLimitsResponse limits;

    /**
     * Monthly savings compared to monthly billing
     */
    private BigDecimal monthlySavings;

    /**
     * Days remaining in current billing cycle
     */
    private Long daysRemainingInCycle;

    /**
     * Whether subscription is currently active
     */
    private Boolean isActive;

    /**
     * Whether subscription is in trial period
     */
    private Boolean isInTrial;

    /**
     * Whether subscription can be upgraded
     */
    private Boolean canUpgrade;

    /**
     * Whether subscription can be cancelled
     */
    private Boolean canCancel;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Nested class for subscription limits
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionLimitsResponse {
        private Integer maxWatchlists;
        private Integer maxAlerts;
        private Integer apiCallsPerDay;
        private Integer maxPortfolios;
        private Integer aiAnalysisPerMonth;
        private Integer maxSubAccounts;
        private Integer maxCustomIndicators;
        private Integer dataRetentionDays;
        private Integer maxWebSocketConnections;
        
        /**
         * Check if a feature is unlimited
         */
        public boolean isUnlimited(String feature) {
            return switch (feature.toLowerCase()) {
                case "watchlists" -> maxWatchlists == -1;
                case "alerts" -> maxAlerts == -1;
                case "api_calls" -> apiCallsPerDay == -1;
                case "portfolios" -> maxPortfolios == -1;
                case "ai_analysis" -> aiAnalysisPerMonth == -1;
                case "sub_accounts" -> maxSubAccounts == -1;
                case "custom_indicators" -> maxCustomIndicators == -1;
                case "data_retention" -> dataRetentionDays == -1;
                default -> false;
            };
        }
    }
}