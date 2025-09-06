package com.trademaster.multibroker.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Broker Type Enumeration
 * 
 * MANDATORY: Functional Programming + Pattern Matching + Zero Placeholders
 * 
 * Defines supported Indian brokers with OAuth endpoints and API configurations.
 * Each broker type contains real production API endpoints for OAuth flows.
 * 
 * Supported Brokers (Production APIs):
 * - Zerodha: Kite Connect API v3
 * - Upstox: Pro API v2
 * - Angel One: SmartAPI v2
 * - ICICI Direct: Breeze API v2
 * - Fyers: Trade API v3
 * - IIFL: TT Blaze API v1
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Production Broker Integration)
 */
@Getter
@RequiredArgsConstructor
public enum BrokerType {
    
    ZERODHA(
        "Zerodha", 
        "https://api.kite.trade",
        "https://kite.zerodha.com/connect/login",
        "zerodha-oauth",
        "https://developers.kite.trade/",
        true,
        60, // API calls per minute
        30000 // Rate limit window in ms
    ),
    
    UPSTOX(
        "Upstox", 
        "https://api.upstox.com/v2",
        "https://api.upstox.com/v2/login/authorization/dialog",
        "upstox-oauth",
        "https://upstox.com/developer/api-documentation/",
        true,
        100, // API calls per minute
        60000 // Rate limit window in ms
    ),
    
    ANGEL_ONE(
        "Angel One", 
        "https://apiconnect.angelbroking.com",
        "https://smartapi.angelbroking.com/publisher-login",
        "angel-oauth",
        "https://smartapi.angelbroking.com/docs/",
        true,
        200, // API calls per minute
        60000 // Rate limit window in ms
    ),
    
    ICICI_DIRECT(
        "ICICI Direct", 
        "https://api.icicidirect.com",
        "https://api.icicidirect.com/breezeapi/api/v1/customerlogin",
        "icici-oauth",
        "https://www.icicidirect.com/apiuser/",
        true,
        120, // API calls per minute
        60000 // Rate limit window in ms
    ),
    
    FYERS(
        "Fyers", 
        "https://api-t1.fyers.in/api/v3",
        "https://api-t1.fyers.in/api/v3/generate-authcode",
        "fyers-oauth",
        "https://myapi.fyers.in/docsv3/",
        true,
        180, // API calls per minute
        60000 // Rate limit window in ms
    ),
    
    IIFL(
        "IIFL", 
        "https://ttblaze.iifl.com/apimarketdata",
        "https://ttblaze.iifl.com/interactivelogin",
        "iifl-oauth",
        "https://www.iifl.com/knowledge-center/trading/iifl-markets-tt-web-api",
        false, // Currently in beta
        50, // API calls per minute
        60000 // Rate limit window in ms
    );

    private final String displayName;
    private final String apiBaseUrl;
    private final String oauthUrl;
    private final String oauthProvider;
    private final String documentationUrl;
    private final boolean active;
    private final int rateLimitPerMinute;
    private final long rateLimitWindowMs;
    
    /**
     * Get broker type by name using pattern matching
     * 
     * @param name Broker name or display name
     * @return Optional BrokerType
     */
    public static java.util.Optional<BrokerType> fromName(String name) {
        return java.util.Arrays.stream(values())
            .filter(broker -> broker.name().equalsIgnoreCase(name) || 
                             broker.displayName.equalsIgnoreCase(name))
            .findFirst();
    }
    
    /**
     * Check if broker supports OAuth 2.0 flow
     * 
     * @return true if OAuth supported
     */
    public boolean supportsOAuth() {
        return switch (this) {
            case ZERODHA, UPSTOX, ANGEL_ONE, ICICI_DIRECT, FYERS -> true;
            case IIFL -> false; // Uses different auth mechanism
        };
    }
    
    /**
     * Get OAuth scope for broker
     * 
     * @return OAuth scope string
     */
    public String getOAuthScope() {
        return switch (this) {
            case ZERODHA -> "read:user read:orders read:positions read:holdings";
            case UPSTOX -> "read_profile read_portfolio read_orders";
            case ANGEL_ONE -> "profile portfolio orders positions";
            case ICICI_DIRECT -> "read:profile read:portfolio read:orders read:positions";
            case FYERS -> "openapi-profile openapi-quotes openapi-funds openapi-orders";
            case IIFL -> "api-access"; // Different auth mechanism
        };
    }
    
    /**
     * Get redirect URI pattern for broker OAuth
     * 
     * @param baseUrl Application base URL
     * @return Formatted redirect URI
     */
    public String getRedirectUri(String baseUrl) {
        return String.format("%s/api/v1/brokers/oauth/callback/%s", 
                           baseUrl, name().toLowerCase());
    }
    
    /**
     * Check if broker is currently active and supported
     * 
     * @return true if broker is active
     */
    public boolean isActive() {
        return this.active;
    }
    
    /**
     * Get documentation URL for broker
     * 
     * @return Documentation URL
     */
    public String getDocumentationUrl() {
        return switch (this) {
            case ZERODHA -> "https://kite.trade/docs/connect/v3/";
            case UPSTOX -> "https://upstox.com/developer/api-documentation";
            case ANGEL_ONE -> "https://smartapi.angelbroking.com/docs";
            case ICICI_DIRECT -> "https://api.icicidirect.com/docs";
            case FYERS -> "https://myapi.fyers.in/docs";
            case IIFL -> "https://ttblaze.iifl.com/doc";
        };
    }
    
    /**
     * Get all active brokers
     * 
     * @return List of active BrokerType values
     */
    public static java.util.List<BrokerType> getActiveBrokers() {
        return java.util.Arrays.stream(values())
                .filter(BrokerType::isActive)
                .collect(java.util.stream.Collectors.toList());
    }
}