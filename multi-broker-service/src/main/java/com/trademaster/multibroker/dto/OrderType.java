package com.trademaster.multibroker.dto;

/**
 * Order Type Enumeration
 * 
 * MANDATORY: Java 24 + Pattern Matching Excellence - Rule #14
 * 
 * Defines the various order types supported by the multi-broker system
 * with comprehensive coverage of Indian market order types.
 */
public enum OrderType {
    
    /**
     * Market order - Execute immediately at current market price
     */
    MARKET("MKT", "Market Order", "Execute at current market price"),
    
    /**
     * Limit order - Execute only at specified price or better
     */
    LIMIT("LMT", "Limit Order", "Execute only at limit price or better"),
    
    /**
     * Stop loss order - Trigger when price reaches stop level
     */
    STOP_LOSS("SL", "Stop Loss", "Trigger market order when stop price is reached"),
    
    /**
     * Bracket order - Advanced order with target and stop loss
     */
    BRACKET("BO", "Bracket Order", "Parent order with target and stop loss legs");
    
    private final String code;
    private final String displayName;
    private final String description;
    
    OrderType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if order type requires price
     */
    public boolean requiresPrice() {
        return switch (this) {
            case LIMIT, BRACKET -> true;
            case MARKET, STOP_LOSS -> false;
        };
    }
    
    /**
     * Check if order type requires stop price
     */
    public boolean requiresStopPrice() {
        return switch (this) {
            case STOP_LOSS, BRACKET -> true;
            case MARKET, LIMIT -> false;
        };
    }
    
    /**
     * Get order type from code
     */
    public static OrderType fromCode(String code) {
        return switch (code.toUpperCase()) {
            case "MKT", "MARKET" -> MARKET;
            case "LMT", "LIMIT" -> LIMIT;
            case "SL", "STOP_LOSS", "STOPLOSS" -> STOP_LOSS;
            case "BO", "BRACKET" -> BRACKET;
            default -> throw new IllegalArgumentException("Unknown order type code: " + code);
        };
    }
}