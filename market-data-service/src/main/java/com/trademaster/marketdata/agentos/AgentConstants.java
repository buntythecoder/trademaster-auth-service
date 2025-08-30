package com.trademaster.marketdata.agentos;

/**
 * AgentOS Constants for Market Data Service
 * 
 * Centralized constants for the AgentOS framework integration following
 * TradeMaster Rule #7 - All magic numbers and strings must be extracted to constants.
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public final class AgentConstants {
    
    // Prevent instantiation
    private AgentConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Agent Identification Constants
    public static final String AGENT_ID = "market-data-agent";
    public static final String AGENT_TYPE = "MARKET_DATA";
    
    // Agent Capabilities
    public static final String CAPABILITY_REAL_TIME_DATA = "REAL_TIME_DATA";
    public static final String CAPABILITY_HISTORICAL_DATA = "HISTORICAL_DATA";
    public static final String CAPABILITY_TECHNICAL_ANALYSIS = "TECHNICAL_ANALYSIS";
    public static final String CAPABILITY_MARKET_SCANNING = "MARKET_SCANNING";
    public static final String CAPABILITY_PRICE_ALERTS = "PRICE_ALERTS";
    public static final String CAPABILITY_DATA_CACHING = "DATA_CACHING";
    public static final String CAPABILITY_SYMBOL_LOOKUP = "SYMBOL_LOOKUP";
    public static final String CAPABILITY_EXCHANGE_INTEGRATION = "EXCHANGE_INTEGRATION";
    
    // Proficiency Levels
    public static final String PROFICIENCY_BEGINNER = "BEGINNER";
    public static final String PROFICIENCY_INTERMEDIATE = "INTERMEDIATE";
    public static final String PROFICIENCY_ADVANCED = "ADVANCED";
    public static final String PROFICIENCY_EXPERT = "EXPERT";
    
    // Performance Profiles
    public static final String PERFORMANCE_STANDARD = "STANDARD";
    public static final String PERFORMANCE_HIGH = "HIGH";
    public static final String PERFORMANCE_CRITICAL = "CRITICAL";
    
    // Health Score Thresholds
    public static final double HEALTH_SCORE_EXCELLENT = 0.9;
    public static final double HEALTH_SCORE_GOOD = 0.7;
    public static final double HEALTH_SCORE_WARNING = 0.5;
    public static final double HEALTH_SCORE_CRITICAL = 0.3;
    
    // Timeout Constants (milliseconds)
    public static final int DEFAULT_OPERATION_TIMEOUT_MS = 10000;
    public static final int HEALTH_CHECK_INTERVAL_MS = 30000;
    public static final int CAPABILITY_METRICS_INTERVAL_MS = 60000;
    
    // Default Event Handler Priority
    public static final int DEFAULT_EVENT_PRIORITY = 0;
}