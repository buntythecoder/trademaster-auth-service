package com.trademaster.portfolio.model;

/**
 * Portfolio Status Enumeration
 * 
 * Represents the lifecycle status of a portfolio in the trading system.
 * Controls portfolio operations and access permissions.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public enum PortfolioStatus {
    
    /**
     * Portfolio is active and operational
     * - All trading operations allowed
     * - Real-time updates enabled
     * - Full access to analytics and reporting
     */
    ACTIVE("active"),
    
    /**
     * Portfolio is temporarily suspended
     * - Trading operations blocked
     * - View-only access permitted
     * - Analytics and reporting available
     */
    SUSPENDED("suspended"),
    
    /**
     * Portfolio is frozen due to risk violations
     * - All operations blocked except liquidation
     * - Emergency risk management activated
     * - Administrative oversight required
     */
    FROZEN("frozen"),
    
    /**
     * Portfolio is closed and archived
     * - No trading operations allowed
     * - Historical data preserved for reporting
     * - Read-only access for compliance
     */
    CLOSED("closed"),
    
    /**
     * Portfolio is being liquidated
     * - Only sell orders permitted
     * - Automatic position closure in progress
     * - Risk management monitoring active
     */
    LIQUIDATING("liquidating");
    
    private final String value;
    
    PortfolioStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Check if portfolio allows new trades
     */
    public boolean allowsTrading() {
        return this == ACTIVE;
    }
    
    /**
     * Check if portfolio allows position updates
     */
    public boolean allowsPositionUpdates() {
        return this == ACTIVE || this == LIQUIDATING;
    }
    
    /**
     * Check if portfolio is viewable
     */
    public boolean isViewable() {
        return this != CLOSED;
    }
    
    /**
     * Check if portfolio requires administrative approval
     */
    public boolean requiresApproval() {
        return this == FROZEN || this == LIQUIDATING;
    }
}