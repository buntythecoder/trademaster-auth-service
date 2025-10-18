package com.trademaster.marketdata.dto;

/**
 * Market Data Service Constants
 *
 * Centralized constants for validation, defaults, and business logic limits.
 * Following RULE #17: Replace all magic numbers with named constants.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class MarketDataConstants {

    // Private constructor to prevent instantiation
    private MarketDataConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== Subscription Limits ==========

    /**
     * Maximum number of symbols allowed per subscription request
     */
    public static final int MAX_SYMBOLS_PER_SUBSCRIPTION = 100;

    /**
     * Minimum update frequency in milliseconds (100ms)
     */
    public static final int MIN_UPDATE_FREQUENCY_MS = 100;

    /**
     * Maximum update frequency in milliseconds (60 seconds)
     */
    public static final int MAX_UPDATE_FREQUENCY_MS = 60000;

    /**
     * Minimum order book depth
     */
    public static final int MIN_ORDER_BOOK_DEPTH = 1;

    /**
     * Maximum order book depth
     */
    public static final int MAX_ORDER_BOOK_DEPTH = 20;

    /**
     * Minimum updates per second
     */
    public static final int MIN_UPDATES_PER_SECOND = 1;

    /**
     * Maximum updates per second
     */
    public static final int MAX_UPDATES_PER_SECOND = 1000;

    // ========== Subscription Defaults ==========

    /**
     * Default update frequency in milliseconds (1 second)
     */
    public static final int DEFAULT_UPDATE_FREQUENCY_MS = 1000;

    /**
     * Default order book depth
     */
    public static final int DEFAULT_ORDER_BOOK_DEPTH = 5;

    /**
     * Default maximum updates per second
     */
    public static final int DEFAULT_MAX_UPDATES_PER_SECOND = 100;

    /**
     * Default decimal precision for price values
     */
    public static final int DEFAULT_DECIMAL_PRECISION = 4;

    /**
     * Default batch size for batched updates
     */
    public static final int DEFAULT_BATCH_SIZE = 10;

    /**
     * Default time zone for timestamps
     */
    public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    // ========== Update Frequency Limits by Data Type ==========

    /**
     * Maximum update frequency for TICK data (100ms minimum interval)
     */
    public static final int TICK_MAX_UPDATE_FREQUENCY_MS = 100;

    /**
     * Maximum update frequency for ORDER_BOOK data (500ms minimum interval)
     */
    public static final int ORDER_BOOK_MAX_UPDATE_FREQUENCY_MS = 500;

    // ========== High-Frequency Trading Defaults ==========

    /**
     * High-frequency subscription update frequency (100ms)
     */
    public static final int HIGH_FREQ_UPDATE_FREQUENCY_MS = 100;

    /**
     * High-frequency subscription order book depth
     */
    public static final int HIGH_FREQ_ORDER_BOOK_DEPTH = 10;

    /**
     * High-frequency subscription max updates per second
     */
    public static final int HIGH_FREQ_MAX_UPDATES_PER_SECOND = 500;

    /**
     * High-frequency batch size for updates
     */
    public static final int HIGH_FREQ_BATCH_SIZE = 20;

    // ========== Cache TTL Values (seconds) ==========

    /**
     * Short cache TTL for real-time data (5 seconds)
     */
    public static final int CACHE_TTL_SHORT_SECONDS = 5;

    /**
     * Medium cache TTL for frequently accessed data (60 seconds)
     */
    public static final int CACHE_TTL_MEDIUM_SECONDS = 60;

    /**
     * Long cache TTL for stable data (5 minutes)
     */
    public static final int CACHE_TTL_LONG_SECONDS = 300;

    // ========== Timeout Values (milliseconds) ==========

    /**
     * Short timeout for fast operations (5 seconds)
     */
    public static final int TIMEOUT_SHORT_MS = 5000;

    /**
     * Standard timeout for normal operations (30 seconds)
     */
    public static final int TIMEOUT_STANDARD_MS = 30000;

    /**
     * Long timeout for batch/expensive operations (60 seconds)
     */
    public static final int TIMEOUT_LONG_MS = 60000;

    // ========== Rate Limits ==========

    /**
     * Basic rate limit (requests per minute)
     */
    public static final int RATE_LIMIT_BASIC = 100;

    /**
     * Premium rate limit (requests per minute)
     */
    public static final int RATE_LIMIT_PREMIUM = 10000;

    // ========== Data Quality Thresholds ==========

    /**
     * Excellent data quality threshold (95% or higher)
     */
    public static final double QUALITY_THRESHOLD_EXCELLENT = 0.95;

    /**
     * Good data quality threshold (80% or higher)
     */
    public static final double QUALITY_THRESHOLD_GOOD = 0.80;

    /**
     * Acceptable data quality threshold (60% or higher)
     */
    public static final double QUALITY_THRESHOLD_ACCEPTABLE = 0.60;
}
