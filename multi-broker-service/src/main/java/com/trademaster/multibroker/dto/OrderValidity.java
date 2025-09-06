package com.trademaster.multibroker.dto;

/**
 * Order Validity Enumeration
 * 
 * MANDATORY: Java 24 + Pattern Matching Excellence - Rule #14
 * 
 * Defines the validity period for orders in the trading system.
 */
public enum OrderValidity {
    
    /**
     * Day order - Valid until end of trading day
     */
    DAY("DAY", "Day", "Valid until end of current trading day"),
    
    /**
     * Immediate or Cancel - Execute immediately or cancel
     */
    IOC("IOC", "Immediate or Cancel", "Execute immediately, cancel unfilled portion"),
    
    /**
     * Good Till Cancelled - Valid until explicitly cancelled
     */
    GTC("GTC", "Good Till Cancelled", "Valid until explicitly cancelled"),
    
    /**
     * Fill or Kill - Execute completely or cancel
     */
    FOK("FOK", "Fill or Kill", "Execute complete quantity or cancel entirely");
    
    private final String code;
    private final String displayName;
    private final String description;
    
    OrderValidity(String code, String displayName, String description) {
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
     * Get order validity from code
     */
    public static OrderValidity fromCode(String code) {
        return switch (code.toUpperCase()) {
            case "DAY" -> DAY;
            case "IOC" -> IOC;
            case "GTC" -> GTC;
            case "FOK" -> FOK;
            default -> throw new IllegalArgumentException("Unknown order validity code: " + code);
        };
    }
}