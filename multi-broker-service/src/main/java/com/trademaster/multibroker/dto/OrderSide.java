package com.trademaster.multibroker.dto;

/**
 * Order Side Enumeration
 * 
 * MANDATORY: Java 24 + Pattern Matching Excellence - Rule #14
 * 
 * Defines the order side (buy/sell) for trading operations.
 */
public enum OrderSide {
    
    /**
     * Buy order - Acquire securities
     */
    BUY("B", "Buy", "Acquire securities"),
    
    /**
     * Sell order - Dispose securities
     */
    SELL("S", "Sell", "Dispose securities");
    
    private final String code;
    private final String displayName;
    private final String description;
    
    OrderSide(String code, String displayName, String description) {
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
     * Get order side from code
     */
    public static OrderSide fromCode(String code) {
        return switch (code.toUpperCase()) {
            case "B", "BUY" -> BUY;
            case "S", "SELL" -> SELL;
            default -> throw new IllegalArgumentException("Unknown order side code: " + code);
        };
    }
}