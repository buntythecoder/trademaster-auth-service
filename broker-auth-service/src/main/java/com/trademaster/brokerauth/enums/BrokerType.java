package com.trademaster.brokerauth.enums;

import lombok.Getter;

/**
 * Supported Broker Types
 * 
 * Represents all supported brokers in the TradeMaster platform.
 * Each broker has specific authentication mechanisms and API characteristics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public enum BrokerType {
    
    ZERODHA("zerodha", "Zerodha Kite", "https://api.kite.trade", AuthType.REQUEST_TOKEN),
    UPSTOX("upstox", "Upstox Pro", "https://api.upstox.com/v2", AuthType.OAUTH2),
    ANGEL_ONE("angel-one", "Angel One SmartAPI", "https://apiconnect.angelbroking.com", AuthType.API_KEY_WITH_TOTP),
    ICICI_DIRECT("icici-direct", "ICICI Direct", "https://api.icicidirect.com", AuthType.SESSION_TOKEN);
    
    private final String code;
    private final String displayName;
    private final String baseUrl;
    private final AuthType authType;
    
    BrokerType(String code, String displayName, String baseUrl, AuthType authType) {
        this.code = code;
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.authType = authType;
    }
    
    /**
     * Get broker type by code
     */
    public static BrokerType fromCode(String code) {
        for (BrokerType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown broker code: " + code);
    }
    
    /**
     * Check if broker supports real-time data
     */
    public boolean supportsRealTimeData() {
        return switch (this) {
            case ZERODHA, UPSTOX -> true;
            case ANGEL_ONE, ICICI_DIRECT -> false;
        };
    }
    
    /**
     * Check if broker supports order placement
     */
    public boolean supportsOrderPlacement() {
        return true; // All supported brokers support order placement
    }
    
    /**
     * Get maximum sessions allowed per user
     */
    public int getMaxSessionsPerUser() {
        return switch (this) {
            case ZERODHA -> 1;      // Only one active session
            case UPSTOX -> 3;       // Multiple sessions allowed
            case ANGEL_ONE -> 1;    // Only one active session
            case ICICI_DIRECT -> 2; // Limited sessions
        };
    }
    
    /**
     * Get session validity in seconds
     */
    public long getSessionValiditySeconds() {
        return switch (this) {
            case ZERODHA -> 86400;      // 24 hours
            case UPSTOX -> 86400;       // 24 hours  
            case ANGEL_ONE -> 43200;    // 12 hours
            case ICICI_DIRECT -> 28800; // 8 hours
        };
    }
    
    /**
     * Check if token refresh is supported
     */
    public boolean supportsTokenRefresh() {
        return switch (this) {
            case ZERODHA -> false;      // Need to re-authenticate daily
            case UPSTOX -> true;        // Supports refresh token
            case ANGEL_ONE -> true;     // Can regenerate tokens
            case ICICI_DIRECT -> false; // Manual re-authentication needed
        };
    }
    
    /**
     * Authentication Type for each broker
     */
    public enum AuthType {
        OAUTH2("OAuth 2.0"),
        REQUEST_TOKEN("Request Token"),
        API_KEY_WITH_TOTP("API Key with TOTP"),
        SESSION_TOKEN("Session Token");
        
        @Getter
        private final String description;
        
        AuthType(String description) {
            this.description = description;
        }
    }
}