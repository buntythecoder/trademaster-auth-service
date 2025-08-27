package com.trademaster.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Usage Statistics Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageStatsResponse {

    private UUID userId;
    
    private Map<String, Long> currentUsage;
    
    private Map<String, Long> monthlyLimits;
    
    private Map<String, Double> utilizationPercentage;
    
    private LocalDateTime resetDate;
    
    private String subscriptionTier;
    
    private Long totalRequests;
    
    private LocalDateTime periodStart;
    
    private LocalDateTime periodEnd;
}