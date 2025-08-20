package com.trademaster.userprofile.entity;

import lombok.Builder;

import java.time.Instant;

@Builder
public record BrokerConfiguration(
    String brokerName,
    String brokerCode,
    String clientId,
    String apiKey,
    String apiSecret,
    boolean isActive,
    boolean isPrimaryBroker,
    Instant configuredAt,
    Instant lastUsedAt,
    String connectionStatus,
    String brokerType // FULL_SERVICE, DISCOUNT, DIRECT_ACCESS
) {
    
    public boolean isConnected() {
        return "CONNECTED".equalsIgnoreCase(connectionStatus);
    }
    
    public boolean isFullServiceBroker() {
        return "FULL_SERVICE".equalsIgnoreCase(brokerType);
    }
    
    public boolean isDiscountBroker() {
        return "DISCOUNT".equalsIgnoreCase(brokerType);
    }
}