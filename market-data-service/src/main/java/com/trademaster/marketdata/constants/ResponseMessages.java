package com.trademaster.marketdata.constants;

/**
 * Response Message Constants
 * 
 * Centralized constants for all API response messages to ensure consistency
 * and maintainability across the market data service.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class ResponseMessages {
    
    // Private constructor to prevent instantiation
    private ResponseMessages() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
    
    // Success Messages
    public static final String ALERTS_RETRIEVED_SUCCESS = "Alerts retrieved successfully";
    public static final String ALERT_PROCESSED_SUCCESS = "Alert processed successfully";
    public static final String ALERT_CREATED_SUCCESS = "Alert created successfully";
    public static final String ALERT_UPDATED_SUCCESS = "Alert updated successfully";
    public static final String ALERT_DELETED_SUCCESS = "Alert deleted successfully";
    public static final String ALERTS_WITH_ANALYTICS_SUCCESS = "Alerts with analytics retrieved successfully";
    public static final String ALERTS_WITH_CONTEXT_SUCCESS = "Alerts with market context retrieved successfully";
    
    // Error Messages
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String PROCESSING_ERROR = "Processing error occurred";
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";
    public static final String RATE_LIMIT_EXCEEDED = "Rate limit exceeded";
    
    // Validation Messages
    public static final String EXCHANGE_REQUIRED = "At least one exchange must be specified";
    public static final String SORT_FIELD_REQUIRED = "Sort field is required";
    public static final String SYMBOL_REQUIRED = "Symbol is required";
    public static final String INVALID_DATE_RANGE = "Invalid date range specified";
    public static final String INVALID_PARAMETERS = "Invalid parameters provided";
    
    // Market Data Messages
    public static final String MARKET_DATA_RETRIEVED = "Market data retrieved successfully";
    public static final String MARKET_DATA_UNAVAILABLE = "Market data temporarily unavailable";
    public static final String REAL_TIME_DATA_ACTIVE = "Real-time data subscription active";
    public static final String SUBSCRIPTION_CREATED = "Subscription created successfully";
    public static final String SUBSCRIPTION_CANCELLED = "Subscription cancelled successfully";
    
    // News Messages
    public static final String NEWS_RETRIEVED_SUCCESS = "News retrieved successfully";
    public static final String NEWS_ANALYSIS_COMPLETE = "News analysis completed";
    public static final String SENTIMENT_ANALYSIS_COMPLETE = "Sentiment analysis completed";
    
    // Economic Calendar Messages
    public static final String ECONOMIC_EVENTS_RETRIEVED = "Economic events retrieved successfully";
    public static final String ECONOMIC_ANALYSIS_COMPLETE = "Economic analysis completed";
    
    // Chart Data Messages
    public static final String CHART_DATA_RETRIEVED = "Chart data retrieved successfully";
    public static final String TECHNICAL_ANALYSIS_COMPLETE = "Technical analysis completed";
    public static final String PATTERN_ANALYSIS_COMPLETE = "Pattern analysis completed";
}