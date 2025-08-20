package com.trademaster.userprofile.entity;

public enum OrderValidity {
    DAY("Day", "Valid for current trading session only"),
    IOC("IOC", "Immediate or Cancel - execute immediately or cancel"),
    FOK("FOK", "Fill or Kill - execute completely or cancel"),
    GTD("GTD", "Good Till Date - valid until specified date"),
    GTC("GTC", "Good Till Cancelled - valid until manually cancelled");
    
    private final String displayName;
    private final String description;
    
    OrderValidity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isImmediateExecution() {
        return this == IOC || this == FOK;
    }
    
    public boolean requiresDate() {
        return this == GTD;
    }
    
    public boolean isDefault() {
        return this == DAY;
    }
}

enum ProductType {
    INTRADAY("MIS", "Margin Intraday Square-off"),
    DELIVERY("CNC", "Cash and Carry for delivery"),
    NORMAL("NRML", "Normal for F&O positions");
    
    private final String code;
    private final String description;
    
    ProductType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isIntraday() {
        return this == INTRADAY;
    }
    
    public boolean requiresMargin() {
        return this == INTRADAY || this == NORMAL;
    }
}