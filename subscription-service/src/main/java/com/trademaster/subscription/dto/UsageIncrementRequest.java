package com.trademaster.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Usage Increment Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageIncrementRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Feature name is required")
    private String feature;

    @Positive(message = "Amount must be positive")
    @Builder.Default
    private Long amount = 1L;
    
    private String description;
}