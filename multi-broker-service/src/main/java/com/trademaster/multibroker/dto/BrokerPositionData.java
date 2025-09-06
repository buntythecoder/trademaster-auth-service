package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * Broker Position Data DTO
 * 
 * MANDATORY: Java 24 + Immutability & Records Usage - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Contains position data fetched from a specific broker with metadata.
 */
@Builder
@Jacksonized
public record BrokerPositionData(
    BrokerType broker,
    String userId,
    List<BrokerPosition> positions,
    Instant fetchTime,
    boolean success,
    String errorMessage
) {
    
    public BrokerPositionData {
        if (broker == null) {
            throw new IllegalArgumentException("Broker cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (fetchTime == null) {
            throw new IllegalArgumentException("Fetch time cannot be null");
        }
        if (positions == null) {
            positions = List.of();
        }
    }
    
    /**
     * Get number of positions
     */
    public int getPositionCount() {
        return positions.size();
    }
    
    /**
     * Check if fetch was recent (within last 5 minutes)
     */
    public boolean isRecent() {
        return fetchTime.isAfter(Instant.now().minusSeconds(300));
    }
}