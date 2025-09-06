package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * Position Sync Result DTO
 * 
 * MANDATORY: Java 24 + Immutability & Records Usage - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Comprehensive result of position synchronization across multiple brokers
 * with detailed analytics and reconciliation information.
 */
@Builder
@Jacksonized
public record PositionSyncResult(
    String userId,
    List<BrokerPositionData> brokerPositions,
    List<NormalizedBrokerPosition> normalizedPositions,
    List<ReconciledPosition> reconciledPositions,
    PositionAnalytics analytics,
    Instant syncTime,
    boolean success,
    String errorMessage,
    List<BrokerType> activeBrokers,
    boolean staleData
) {
    
    public PositionSyncResult {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (syncTime == null) {
            throw new IllegalArgumentException("Sync time cannot be null");
        }
    }
    
    /**
     * Create successful sync result
     */
    public static PositionSyncResult success(String userId, List<BrokerPositionData> brokerPositions,
                                           List<NormalizedBrokerPosition> normalizedPositions,
                                           List<ReconciledPosition> reconciledPositions,
                                           PositionAnalytics analytics,
                                           List<BrokerType> activeBrokers,
                                           Instant syncTime) {
        return PositionSyncResult.builder()
            .userId(userId)
            .brokerPositions(brokerPositions)
            .normalizedPositions(normalizedPositions)
            .reconciledPositions(reconciledPositions)
            .analytics(analytics)
            .syncTime(syncTime)
            .success(true)
            .activeBrokers(activeBrokers)
            .staleData(false)
            .build();
    }
    
    /**
     * Create failure sync result
     */
    public static PositionSyncResult failure(String userId, String errorMessage, Instant syncTime) {
        return PositionSyncResult.builder()
            .userId(userId)
            .brokerPositions(List.of())
            .normalizedPositions(List.of())
            .reconciledPositions(List.of())
            .analytics(null)
            .syncTime(syncTime)
            .success(false)
            .errorMessage(errorMessage)
            .activeBrokers(List.of())
            .staleData(false)
            .build();
    }
    
    /**
     * Create no active brokers result
     */
    public static PositionSyncResult noActiveBrokers(String userId, Instant syncTime) {
        return PositionSyncResult.builder()
            .userId(userId)
            .brokerPositions(List.of())
            .normalizedPositions(List.of())
            .reconciledPositions(List.of())
            .analytics(null)
            .syncTime(syncTime)
            .success(false)
            .errorMessage("No active broker connections found")
            .activeBrokers(List.of())
            .staleData(false)
            .build();
    }
    
    /**
     * Mark result as stale data
     */
    public PositionSyncResult withStaleDataWarning() {
        return PositionSyncResult.builder()
            .userId(userId)
            .brokerPositions(brokerPositions)
            .normalizedPositions(normalizedPositions)
            .reconciledPositions(reconciledPositions)
            .analytics(analytics)
            .syncTime(syncTime)
            .success(success)
            .errorMessage(errorMessage)
            .activeBrokers(activeBrokers)
            .staleData(true)
            .build();
    }
    
    /**
     * Get total number of positions
     */
    public int getTotalPositions() {
        return reconciledPositions != null ? reconciledPositions.size() : 0;
    }
    
    /**
     * Get total number of brokers
     */
    public int getTotalBrokers() {
        return activeBrokers != null ? activeBrokers.size() : 0;
    }
    
    /**
     * Check if sync is recent (within last 10 minutes)
     */
    public boolean isRecent() {
        return syncTime.isAfter(Instant.now().minusSeconds(600));
    }
}