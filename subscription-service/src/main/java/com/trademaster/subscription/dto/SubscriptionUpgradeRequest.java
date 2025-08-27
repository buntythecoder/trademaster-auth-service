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
 * Subscription Upgrade Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionUpgradeRequest {

    @NotNull(message = "Target tier is required")
    private SubscriptionTier targetTier;

    private BillingCycle billingCycle;
    
    private Boolean immediateUpgrade = true;
    
    private String promotionCode;
}