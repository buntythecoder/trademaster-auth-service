package com.trademaster.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Usage Check Request DTO
 * 
 * Request object for checking and incrementing feature usage.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageCheckRequest {

    /**
     * User ID to check usage for
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * Feature name to check
     */
    @NotBlank(message = "Feature name is required")
    private String featureName;

    /**
     * Amount of usage to check/increment
     */
    @Min(value = 1, message = "Usage amount must be at least 1")
    @Builder.Default
    private Long usageAmount = 1L;

    /**
     * Whether to actually increment usage (false for check-only)
     */
    @Builder.Default
    private Boolean incrementUsage = false;
}