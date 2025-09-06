package com.trademaster.portfolio.model;

/**
 * Risk Level Enumeration
 * 
 * Defines risk tolerance levels for portfolio management.
 */
public enum RiskLevel {
    VERY_LOW("Very Low Risk", 0.05),
    LOW("Low Risk", 0.15),
    MODERATE("Moderate Risk", 0.25),
    HIGH("High Risk", 0.35),
    VERY_HIGH("Very High Risk", 0.50);
    
    private final String displayName;
    private final double maxDrawdownThreshold;
    
    RiskLevel(String displayName, double maxDrawdownThreshold) {
        this.displayName = displayName;
        this.maxDrawdownThreshold = maxDrawdownThreshold;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getMaxDrawdownThreshold() {
        return maxDrawdownThreshold;
    }
}