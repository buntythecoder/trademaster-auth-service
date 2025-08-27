package com.trademaster.subscription.dto;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Subscription Creation Request DTO
 * 
 * Request object for creating new subscriptions with validation.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    /**
     * User ID for the subscription
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * Subscription tier
     */
    @NotNull(message = "Subscription tier is required")
    private SubscriptionTier tier;

    /**
     * Billing cycle preference
     */
    @NotNull(message = "Billing cycle is required")
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    /**
     * Start with trial period
     */
    @Builder.Default
    private Boolean startTrial = false;

    /**
     * Promotion code (optional)
     */
    private String promotionCode;

    /**
     * Payment method ID from payment service
     */
    private UUID paymentMethodId;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Auto-renewal preference
     */
    @Builder.Default
    private Boolean autoRenewal = true;
}