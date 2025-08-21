package com.trademaster.portfolio.model;

/**
 * Position Type Enumeration
 * 
 * Represents the type of position held in a portfolio.
 * Determines P&L calculation methods and risk management rules.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public enum PositionType {
    
    /**
     * Long position - ownership of the security
     * - Benefits from price appreciation
     * - Limited downside risk (can't lose more than invested)
     * - Receives dividends/distributions
     */
    LONG("long"),
    
    /**
     * Short position - borrowed and sold security
     * - Benefits from price depreciation
     * - Unlimited theoretical upside risk
     * - Pays dividends/borrowing costs
     */
    SHORT("short"),
    
    /**
     * Hedged position - combination of long and short
     * - Reduced directional risk exposure
     * - May include options or futures for hedging
     * - Complex P&L calculation requirements
     */
    HEDGED("hedged");
    
    private final String value;
    
    PositionType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Check if position benefits from price increases
     */
    public boolean benefitsFromPriceIncrease() {
        return this == LONG;
    }
    
    /**
     * Check if position has unlimited risk
     */
    public boolean hasUnlimitedRisk() {
        return this == SHORT;
    }
    
    /**
     * Check if position requires margin
     */
    public boolean requiresMargin() {
        return this == SHORT || this == HEDGED;
    }
    
    /**
     * Get P&L multiplier for price changes
     * @return 1 for long, -1 for short, 0 for hedged (requires complex calculation)
     */
    public int getPnLMultiplier() {
        return switch (this) {
            case LONG -> 1;
            case SHORT -> -1;
            case HEDGED -> 0; // Requires complex calculation
        };
    }
}