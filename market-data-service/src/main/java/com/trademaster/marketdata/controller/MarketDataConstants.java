package com.trademaster.marketdata.controller;

import java.time.temporal.ChronoUnit;

/**
 * Market Data Controller Constants (Rule #17: No Magic Numbers)
 *
 * Centralizes all constant values used in market data controller operations.
 * Improves maintainability and eliminates magic numbers.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class MarketDataConstants {

    // Prevent instantiation
    private MarketDataConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Time Window Constants
     */
    public static final class TimeWindows {
        private TimeWindows() {}

        /**
         * Realtime data threshold in minutes.
         * Data is considered realtime if within this window.
         */
        public static final long REALTIME_DATA_THRESHOLD_MINUTES = 15;

        /**
         * Active symbols time window in minutes.
         * Used to query symbols with activity in the last hour.
         */
        public static final int ACTIVE_SYMBOLS_WINDOW_MINUTES = 60;

        /**
         * ChronoUnit for time-based calculations
         */
        public static final ChronoUnit TIME_UNIT_MINUTES = ChronoUnit.MINUTES;
    }

    /**
     * HTTP Status Codes
     */
    public static final class HttpStatus {
        private HttpStatus() {}

        /**
         * 200 OK - Successful operation
         */
        public static final int OK = 200;

        /**
         * 403 Forbidden - Insufficient subscription tier or permissions
         */
        public static final int FORBIDDEN = 403;

        /**
         * 404 Not Found - Resource not found
         */
        public static final int NOT_FOUND = 404;
    }

    /**
     * Performance Thresholds
     */
    public static final class Performance {
        private Performance() {}

        /**
         * Maximum average response time in milliseconds for healthy status.
         * System is considered degraded if response time exceeds this threshold.
         */
        public static final double MAX_HEALTHY_RESPONSE_TIME_MS = 10.0;

        /**
         * Target cache response time in milliseconds.
         * Cache hits should return data within this time.
         */
        public static final double TARGET_CACHE_RESPONSE_TIME_MS = 5.0;
    }

    /**
     * Data Source Identifiers
     */
    public static final class DataSource {
        private DataSource() {}

        /**
         * Data retrieved from cache
         */
        public static final String CACHE = "cache";

        /**
         * Data retrieved from live service/database
         */
        public static final String LIVE = "live";

        /**
         * Data retrieved from database
         */
        public static final String DATABASE = "database";
    }

    /**
     * Error Messages
     */
    public static final class ErrorMessages {
        private ErrorMessages() {}

        /**
         * Insufficient subscription tier for realtime data
         */
        public static final String REALTIME_REQUIRES_PREMIUM =
            "Real-time data requires premium subscription";

        /**
         * Date range exceeds subscription tier limit
         */
        public static final String DATE_RANGE_EXCEEDS_LIMIT =
            "Date range exceeds subscription tier limit";

        /**
         * Symbol count exceeds subscription tier limit
         */
        public static final String SYMBOL_COUNT_EXCEEDS_LIMIT =
            "Symbol count exceeds subscription tier limit";
    }

    /**
     * Default Values
     */
    public static final class Defaults {
        private Defaults() {}

        /**
         * Default exchange for Indian market data
         */
        public static final String EXCHANGE = "NSE";

        /**
         * Default data interval for historical queries
         */
        public static final String INTERVAL = "1m";

        /**
         * Default update frequency for realtime subscriptions (milliseconds)
         */
        public static final int UPDATE_FREQUENCY_MS = 1000;
    }

    /**
     * Health Status Values
     */
    public static final class HealthStatus {
        private HealthStatus() {}

        /**
         * System is healthy and performing optimally
         */
        public static final String HEALTHY = "healthy";

        /**
         * System is degraded but operational
         */
        public static final String DEGRADED = "degraded";
    }
}
