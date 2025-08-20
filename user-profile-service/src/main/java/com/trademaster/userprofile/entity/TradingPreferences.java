package com.trademaster.userprofile.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder
public record TradingPreferences(
    @NotEmpty(message = "At least one trading segment must be selected")
    Set<TradingSegment> preferredSegments,
    
    @NotNull(message = "Risk profile is required")
    @Valid
    RiskProfile riskProfile,
    
    @NotNull(message = "Default order settings are required")
    @Valid
    DefaultOrderSettings defaultOrderSettings,
    
    List<BrokerConfiguration> brokerConfigs,
    
    @NotNull(message = "Notification preferences are required")
    @Valid
    NotificationPreferences notifications,
    
    Map<String, Object> customSettings,
    
    Set<String> watchlistSymbols,
    
    String preferredExchange,
    
    Boolean autoSquareOffEnabled,
    
    Integer maxPositionsPerDay,
    
    BigDecimal dailyLossLimit,
    
    String tradingStrategy
) {
    
    public boolean isHighRiskTrader() {
        return riskProfile != null && riskProfile.riskLevel() == RiskLevel.HIGH;
    }
    
    public boolean supportsSegment(TradingSegment segment) {
        return preferredSegments != null && preferredSegments.contains(segment);
    }
    
    public boolean hasValidBrokerConfig() {
        return brokerConfigs != null && !brokerConfigs.isEmpty() &&
               brokerConfigs.stream().anyMatch(config -> config.isActive());
    }
}