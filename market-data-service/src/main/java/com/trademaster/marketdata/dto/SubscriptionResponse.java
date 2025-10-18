package com.trademaster.marketdata.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * WebSocket Subscription Response DTO
 *
 * Response for subscription operations with status tracking and metadata.
 * RULE #9 COMPLIANT: Immutable record with builder pattern.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record SubscriptionResponse(
    String subscriptionId,
    List<String> symbols,
    SubscriptionStatus status,
    Integer updateFrequencyMs,
    Instant createdAt,
    String message,
    SubscriptionMetadata metadata
) {
    /**
     * Compact constructor with validation and defaults
     * RULE #9 COMPLIANT: Immutable validation in constructor
     */
    public SubscriptionResponse {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription ID cannot be blank");
        }
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be empty");
        }

        symbols = List.copyOf(symbols);  // Defensive copy
        status = (status != null) ? status : SubscriptionStatus.ACTIVE;
        createdAt = (createdAt != null) ? createdAt : Instant.now();
        message = (message != null) ? message : "Subscription created successfully";
    }

    /**
     * Subscription status enum
     */
    public enum SubscriptionStatus {
        ACTIVE("Subscription is active and receiving updates"),
        PENDING("Subscription is pending activation"),
        PAUSED("Subscription is temporarily paused"),
        CANCELLED("Subscription has been cancelled"),
        FAILED("Subscription failed to activate");

        private final String description;

        SubscriptionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Subscription metadata record
     * RULE #9 COMPLIANT: Nested immutable record
     */
    @Builder
    public record SubscriptionMetadata(
        Integer symbolCount,
        Integer estimatedUpdatesPerSecond,
        Long estimatedBandwidthBytesPerSecond,
        Instant expiresAt
    ) {
        public SubscriptionMetadata {
            symbolCount = (symbolCount != null) ? symbolCount : 0;
            estimatedUpdatesPerSecond = (estimatedUpdatesPerSecond != null) ? estimatedUpdatesPerSecond : 0;
            estimatedBandwidthBytesPerSecond = (estimatedBandwidthBytesPerSecond != null) ? estimatedBandwidthBytesPerSecond : 0L;
        }
    }

    /**
     * Factory methods for common responses
     */
    public static SubscriptionResponse success(String subscriptionId, List<String> symbols, Integer updateFrequencyMs) {
        return SubscriptionResponse.builder()
            .subscriptionId(subscriptionId)
            .symbols(symbols)
            .status(SubscriptionStatus.ACTIVE)
            .updateFrequencyMs(updateFrequencyMs)
            .createdAt(Instant.now())
            .message("Subscription created successfully")
            .metadata(SubscriptionMetadata.builder()
                .symbolCount(symbols.size())
                .build())
            .build();
    }

    public static SubscriptionResponse failed(String subscriptionId, String errorMessage) {
        return SubscriptionResponse.builder()
            .subscriptionId(subscriptionId)
            .symbols(List.of())
            .status(SubscriptionStatus.FAILED)
            .updateFrequencyMs(0)
            .createdAt(Instant.now())
            .message(errorMessage)
            .build();
    }
}
