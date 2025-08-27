package com.trademaster.subscription.dto;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Tier Change Request DTO
 * 
 * Request object for upgrading/downgrading subscription tiers.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierChangeRequest {

    /**
     * Subscription ID to modify
     */
    @NotNull(message = "Subscription ID is required")
    private UUID subscriptionId;

    /**
     * New subscription tier
     */
    @NotNull(message = "New tier is required")
    private SubscriptionTier newTier;

    /**
     * New billing cycle (optional, keeps current if not specified)
     */
    private BillingCycle newBillingCycle;

    /**
     * Reason for the tier change
     */
    private String reason;

    /**
     * Whether to apply change immediately or at next billing cycle
     */
    @Builder.Default
    private Boolean applyImmediately = true;

    /**
     * Promotion code to apply with the change (optional)
     */
    private String promotionCode;
}