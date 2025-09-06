package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Attribution Analysis Value Object
 * 
 * Performance attribution breakdown by different dimensions.
 * Part of FE-016 Performance Analytics Dashboard feature.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-016)
 */
public record AttributionAnalysis(
    List<SectorContribution> sectorAttribution,
    List<HoldingContribution> holdingAttribution,
    List<TimeContribution> timeAttribution,
    List<BrokerContribution> brokerAttribution
) {
    
    /**
     * Compact constructor with validation
     */
    public AttributionAnalysis {
        sectorAttribution = defaultIfNull(sectorAttribution, List.of());
        holdingAttribution = defaultIfNull(holdingAttribution, List.of());
        timeAttribution = defaultIfNull(timeAttribution, List.of());
        brokerAttribution = defaultIfNull(brokerAttribution, List.of());
    }
    
    /**
     * Get top contributing sector
     */
    public String getTopContributingSector() {
        return sectorAttribution.stream()
            .max((a, b) -> a.returnContribution().compareTo(b.returnContribution()))
            .map(SectorContribution::sectorName)
            .orElse("N/A");
    }
    
    /**
     * Get worst contributing sector
     */
    public String getWorstContributingSector() {
        return sectorAttribution.stream()
            .min((a, b) -> a.returnContribution().compareTo(b.returnContribution()))
            .map(SectorContribution::sectorName)
            .orElse("N/A");
    }
    
    /**
     * Get top contributing holding
     */
    public String getTopContributingHolding() {
        return holdingAttribution.stream()
            .max((a, b) -> a.returnContribution().compareTo(b.returnContribution()))
            .map(HoldingContribution::symbol)
            .orElse("N/A");
    }
    
    /**
     * Get total sector attribution
     */
    public BigDecimal getTotalSectorAttribution() {
        return sectorAttribution.stream()
            .map(SectorContribution::returnContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Get total holding attribution
     */
    public BigDecimal getTotalHoldingAttribution() {
        return holdingAttribution.stream()
            .map(HoldingContribution::returnContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if attribution is balanced (no single source >30%)
     */
    public boolean isBalanced() {
        BigDecimal threshold = new BigDecimal("30.0");
        
        boolean sectorBalanced = sectorAttribution.stream()
            .allMatch(s -> s.returnContribution().abs().compareTo(threshold) <= 0);
            
        boolean holdingBalanced = holdingAttribution.stream()
            .allMatch(h -> h.returnContribution().abs().compareTo(threshold) <= 0);
            
        return sectorBalanced && holdingBalanced;
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Create builder for AttributionAnalysis
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for AttributionAnalysis
     */
    public static class Builder {
        private List<SectorContribution> sectorAttribution = List.of();
        private List<HoldingContribution> holdingAttribution = List.of();
        private List<TimeContribution> timeAttribution = List.of();
        private List<BrokerContribution> brokerAttribution = List.of();
        
        public Builder sectorAttribution(List<SectorContribution> sectorAttribution) {
            this.sectorAttribution = sectorAttribution;
            return this;
        }
        
        public Builder holdingAttribution(List<HoldingContribution> holdingAttribution) {
            this.holdingAttribution = holdingAttribution;
            return this;
        }
        
        public Builder timeAttribution(List<TimeContribution> timeAttribution) {
            this.timeAttribution = timeAttribution;
            return this;
        }
        
        public Builder brokerAttribution(List<BrokerContribution> brokerAttribution) {
            this.brokerAttribution = brokerAttribution;
            return this;
        }
        
        public AttributionAnalysis build() {
            return new AttributionAnalysis(
                sectorAttribution, holdingAttribution, 
                timeAttribution, brokerAttribution
            );
        }
    }
}