package com.trademaster.userprofile.entity;

public enum OrderType {
    MARKET("Market", "Execute immediately at current market price"),
    LIMIT("Limit", "Execute only at specified price or better"),
    STOP_LOSS("Stop Loss", "Execute when price reaches stop price"),
    STOP_LOSS_MARKET("Stop Loss Market", "Market order triggered at stop price"),
    STOP_LOSS_LIMIT("Stop Loss Limit", "Limit order triggered at stop price"),
    BRACKET_ORDER("Bracket Order", "Order with built-in stop loss and target"),
    COVER_ORDER("Cover Order", "Order with mandatory stop loss"),
    ICEBERG("Iceberg", "Large order broken into smaller visible quantities"),
    AFTER_MARKET_ORDER("AMO", "Order placed after market hours");
    
    private final String displayName;
    private final String description;
    
    OrderType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isMarketOrder() {
        return this == MARKET || this == STOP_LOSS_MARKET;
    }
    
    public boolean isLimitOrder() {
        return this == LIMIT || this == STOP_LOSS_LIMIT;
    }
    
    public boolean requiresPrice() {
        return this == LIMIT || this == STOP_LOSS || 
               this == STOP_LOSS_LIMIT || this == ICEBERG;
    }
    
    public boolean hasRiskManagement() {
        return this == BRACKET_ORDER || this == COVER_ORDER ||
               this.name().contains("STOP");
    }
}