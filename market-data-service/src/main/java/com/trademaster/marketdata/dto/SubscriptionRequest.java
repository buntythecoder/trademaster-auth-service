package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

import static com.trademaster.marketdata.dto.MarketDataConstants.*;

/**
 * WebSocket Subscription Request DTO
 *
 * Handles subscription requests for real-time market data including:
 * - Symbol subscriptions
 * - Data type filtering
 * - Exchange filtering
 * - Update frequency control
 *
 * Converted to immutable record for MANDATORY RULE #9 compliance.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionRequest(
    /**
     * List of symbols to subscribe to
     */
    @NotNull(message = "Symbols cannot be null")
    @NotEmpty(message = "At least one symbol must be provided")
    @Size(max = 100, message = "Maximum 100 symbols allowed per subscription")
    List<String> symbols,

    /**
     * Types of market data to receive
     * Default: TICK, OHLC (set in compact constructor)
     */
    Set<MarketDataMessage.MarketDataType> dataTypes,

    /**
     * Exchanges to filter by (optional)
     */
    Set<String> exchanges,

    /**
     * Update frequency in milliseconds
     * Default: 1000ms (set in compact constructor)
     */
    Integer updateFrequency,

    /**
     * Whether to receive historical data on subscription
     * Default: true (set in compact constructor)
     */
    Boolean includeSnapshot,

    /**
     * Depth of order book data (for ORDER_BOOK type)
     * Default: 5 (set in compact constructor)
     */
    Integer orderBookDepth,

    /**
     * Whether to include extended market hours data
     * Default: false (set in compact constructor)
     */
    Boolean includeExtendedHours,

    /**
     * Filter for minimum price change to send updates
     */
    Double minPriceChange,

    /**
     * Filter for minimum volume to send updates
     */
    Long minVolume,

    /**
     * Maximum number of updates per second
     * Default: 100 (set in compact constructor)
     */
    Integer maxUpdatesPerSecond,

    /**
     * Subscription preferences
     */
    SubscriptionPreferences preferences
) {

    // Compact constructor for default values
    // Rule #9: Records with validation and defaults in compact constructor
    // Rule #17: Using MarketDataConstants instead of magic numbers
    public SubscriptionRequest {
        if (dataTypes == null) {
            dataTypes = Set.of(
                MarketDataMessage.MarketDataType.TICK,
                MarketDataMessage.MarketDataType.OHLC
            );
        }
        if (updateFrequency == null) {
            updateFrequency = DEFAULT_UPDATE_FREQUENCY_MS;
        }
        if (includeSnapshot == null) {
            includeSnapshot = true;
        }
        if (orderBookDepth == null) {
            orderBookDepth = DEFAULT_ORDER_BOOK_DEPTH;
        }
        if (includeExtendedHours == null) {
            includeExtendedHours = false;
        }
        if (maxUpdatesPerSecond == null) {
            maxUpdatesPerSecond = DEFAULT_MAX_UPDATES_PER_SECOND;
        }
    }

    /**
     * Subscription preferences nested record
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubscriptionPreferences(
        /**
         * Enable data compression
         * Default: true (set in compact constructor)
         */
        Boolean compression,

        /**
         * Data format preference
         * Default: JSON (set in compact constructor)
         */
        DataFormat format,

        /**
         * Precision for decimal values
         * Default: 4 (set in compact constructor)
         */
        Integer decimalPrecision,

        /**
         * Time zone for timestamps
         * Default: "Asia/Kolkata" (set in compact constructor)
         */
        String timeZone,

        /**
         * Include metadata in responses
         * Default: false (set in compact constructor)
         */
        Boolean includeMetadata,

        /**
         * Batch updates for better performance
         * Default: false (set in compact constructor)
         */
        Boolean batchUpdates,

        /**
         * Batch size for updates
         * Default: 10 (set in compact constructor)
         */
        Integer batchSize
    ) {

        // Compact constructor for default values
        // Rule #17: Using MarketDataConstants instead of magic numbers
        public SubscriptionPreferences {
            if (compression == null) {
                compression = true;
            }
            if (format == null) {
                format = DataFormat.JSON;
            }
            if (decimalPrecision == null) {
                decimalPrecision = DEFAULT_DECIMAL_PRECISION;
            }
            if (timeZone == null) {
                timeZone = DEFAULT_TIMEZONE;
            }
            if (includeMetadata == null) {
                includeMetadata = false;
            }
            if (batchUpdates == null) {
                batchUpdates = false;
            }
            if (batchSize == null) {
                batchSize = DEFAULT_BATCH_SIZE;
            }
        }

        /**
         * Data format enumeration
         */
        public enum DataFormat {
            JSON,
            BINARY,
            COMPRESSED_JSON
        }
    }

    /**
     * Validate the subscription request
     * Rule #17: Using MarketDataConstants for all validation thresholds
     */
    public boolean isValid() {
        if (symbols == null || symbols.isEmpty()) {
            return false;
        }

        if (symbols.size() > MAX_SYMBOLS_PER_SUBSCRIPTION) {
            return false;
        }

        if (updateFrequency != null && (updateFrequency < MIN_UPDATE_FREQUENCY_MS
                || updateFrequency > MAX_UPDATE_FREQUENCY_MS)) {
            return false;
        }

        if (orderBookDepth != null && (orderBookDepth < MIN_ORDER_BOOK_DEPTH
                || orderBookDepth > MAX_ORDER_BOOK_DEPTH)) {
            return false;
        }

        if (maxUpdatesPerSecond != null && (maxUpdatesPerSecond < MIN_UPDATES_PER_SECOND
                || maxUpdatesPerSecond > MAX_UPDATES_PER_SECOND)) {
            return false;
        }

        return true;
    }

    /**
     * Check if subscription includes specific data type
     */
    public boolean includesDataType(MarketDataMessage.MarketDataType dataType) {
        return dataTypes != null && dataTypes.contains(dataType);
    }

    /**
     * Check if subscription includes specific exchange
     */
    public boolean includesExchange(String exchange) {
        return exchanges == null || exchanges.isEmpty() || exchanges.contains(exchange);
    }

    /**
     * Get effective update frequency based on data type
     * Rule #3: Switch expression for conditionals
     * Rule #17: Using MarketDataConstants for frequency limits
     */
    public int getEffectiveUpdateFrequency(MarketDataMessage.MarketDataType dataType) {
        return switch (dataType) {
            case TICK -> Math.min(updateFrequency, TICK_MAX_UPDATE_FREQUENCY_MS);
            case ORDER_BOOK -> Math.min(updateFrequency, ORDER_BOOK_MAX_UPDATE_FREQUENCY_MS);
            default -> updateFrequency;
        };
    }

    /**
     * Create default subscription for symbols
     * Rule #17: Using MarketDataConstants for all default values
     */
    public static SubscriptionRequest createDefault(List<String> symbols) {
        return SubscriptionRequest.builder()
            .symbols(symbols)
            .dataTypes(Set.of(
                MarketDataMessage.MarketDataType.TICK,
                MarketDataMessage.MarketDataType.OHLC
            ))
            .updateFrequency(DEFAULT_UPDATE_FREQUENCY_MS)
            .includeSnapshot(true)
            .orderBookDepth(DEFAULT_ORDER_BOOK_DEPTH)
            .includeExtendedHours(false)
            .maxUpdatesPerSecond(DEFAULT_MAX_UPDATES_PER_SECOND)
            .preferences(SubscriptionPreferences.builder()
                .compression(true)
                .format(SubscriptionPreferences.DataFormat.JSON)
                .decimalPrecision(DEFAULT_DECIMAL_PRECISION)
                .timeZone(DEFAULT_TIMEZONE)
                .includeMetadata(false)
                .batchUpdates(false)
                .build())
            .build();
    }

    /**
     * Get primary exchange (convenience method for backward compatibility)
     */
    public String getExchange() {
        return exchanges != null && !exchanges.isEmpty() ? exchanges.iterator().next() : null;
    }

    /**
     * Create high-frequency subscription for trading
     * Rule #17: Using MarketDataConstants for high-frequency defaults
     */
    public static SubscriptionRequest createHighFrequency(List<String> symbols) {
        return SubscriptionRequest.builder()
            .symbols(symbols)
            .dataTypes(Set.of(
                MarketDataMessage.MarketDataType.TICK,
                MarketDataMessage.MarketDataType.ORDER_BOOK,
                MarketDataMessage.MarketDataType.TRADE
            ))
            .updateFrequency(HIGH_FREQ_UPDATE_FREQUENCY_MS)
            .includeSnapshot(true)
            .orderBookDepth(HIGH_FREQ_ORDER_BOOK_DEPTH)
            .includeExtendedHours(true)
            .maxUpdatesPerSecond(HIGH_FREQ_MAX_UPDATES_PER_SECOND)
            .preferences(SubscriptionPreferences.builder()
                .compression(true)
                .format(SubscriptionPreferences.DataFormat.COMPRESSED_JSON)
                .decimalPrecision(DEFAULT_DECIMAL_PRECISION)
                .timeZone(DEFAULT_TIMEZONE)
                .includeMetadata(true)
                .batchUpdates(true)
                .batchSize(HIGH_FREQ_BATCH_SIZE)
                .build())
            .build();
    }
}