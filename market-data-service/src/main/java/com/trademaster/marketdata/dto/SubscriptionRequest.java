package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * WebSocket Subscription Request DTO
 * 
 * Handles subscription requests for real-time market data including:
 * - Symbol subscriptions
 * - Data type filtering
 * - Exchange filtering
 * - Update frequency control
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionRequest {

    /**
     * List of symbols to subscribe to
     */
    @NotNull(message = "Symbols cannot be null")
    @NotEmpty(message = "At least one symbol must be provided")
    @Size(max = 100, message = "Maximum 100 symbols allowed per subscription")
    private List<String> symbols;

    /**
     * Types of market data to receive
     */
    @Builder.Default
    private Set<MarketDataMessage.MarketDataType> dataTypes = Set.of(
        MarketDataMessage.MarketDataType.TICK,
        MarketDataMessage.MarketDataType.OHLC
    );

    /**
     * Exchanges to filter by (optional)
     */
    private Set<String> exchanges;

    /**
     * Update frequency in milliseconds
     */
    @Builder.Default
    private Integer updateFrequency = 1000; // 1 second default

    /**
     * Whether to receive historical data on subscription
     */
    @Builder.Default
    private Boolean includeSnapshot = true;

    /**
     * Depth of order book data (for ORDER_BOOK type)
     */
    @Builder.Default
    private Integer orderBookDepth = 5;

    /**
     * Whether to include extended market hours data
     */
    @Builder.Default
    private Boolean includeExtendedHours = false;

    /**
     * Filter for minimum price change to send updates
     */
    private Double minPriceChange;

    /**
     * Filter for minimum volume to send updates
     */
    private Long minVolume;

    /**
     * Maximum number of updates per second
     */
    @Builder.Default
    private Integer maxUpdatesPerSecond = 100;

    /**
     * Subscription preferences
     */
    private SubscriptionPreferences preferences;

    /**
     * Subscription preferences nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionPreferences {
        
        /**
         * Enable data compression
         */
        @Builder.Default
        private Boolean compression = true;

        /**
         * Data format preference
         */
        @Builder.Default
        private DataFormat format = DataFormat.JSON;

        /**
         * Precision for decimal values
         */
        @Builder.Default
        private Integer decimalPrecision = 4;

        /**
         * Time zone for timestamps
         */
        @Builder.Default
        private String timeZone = "Asia/Kolkata";

        /**
         * Include metadata in responses
         */
        @Builder.Default
        private Boolean includeMetadata = false;

        /**
         * Batch updates for better performance
         */
        @Builder.Default
        private Boolean batchUpdates = false;

        /**
         * Batch size for updates
         */
        @Builder.Default
        private Integer batchSize = 10;

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
     */
    public boolean isValid() {
        if (symbols == null || symbols.isEmpty()) {
            return false;
        }
        
        if (symbols.size() > 100) {
            return false;
        }
        
        if (updateFrequency != null && (updateFrequency < 100 || updateFrequency > 60000)) {
            return false;
        }
        
        if (orderBookDepth != null && (orderBookDepth < 1 || orderBookDepth > 20)) {
            return false;
        }
        
        if (maxUpdatesPerSecond != null && (maxUpdatesPerSecond < 1 || maxUpdatesPerSecond > 1000)) {
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
     */
    public int getEffectiveUpdateFrequency(MarketDataMessage.MarketDataType dataType) {
        if (dataType == MarketDataMessage.MarketDataType.TICK) {
            return Math.min(updateFrequency, 100); // Max 10 updates per second for tick data
        } else if (dataType == MarketDataMessage.MarketDataType.ORDER_BOOK) {
            return Math.min(updateFrequency, 500); // Max 2 updates per second for order book
        }
        return updateFrequency;
    }

    /**
     * Create default subscription for symbols
     */
    public static SubscriptionRequest createDefault(List<String> symbols) {
        return SubscriptionRequest.builder()
            .symbols(symbols)
            .dataTypes(Set.of(
                MarketDataMessage.MarketDataType.TICK,
                MarketDataMessage.MarketDataType.OHLC
            ))
            .updateFrequency(1000)
            .includeSnapshot(true)
            .orderBookDepth(5)
            .includeExtendedHours(false)
            .maxUpdatesPerSecond(100)
            .preferences(SubscriptionPreferences.builder()
                .compression(true)
                .format(SubscriptionPreferences.DataFormat.JSON)
                .decimalPrecision(4)
                .timeZone("Asia/Kolkata")
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
     */
    public static SubscriptionRequest createHighFrequency(List<String> symbols) {
        return SubscriptionRequest.builder()
            .symbols(symbols)
            .dataTypes(Set.of(
                MarketDataMessage.MarketDataType.TICK,
                MarketDataMessage.MarketDataType.ORDER_BOOK,
                MarketDataMessage.MarketDataType.TRADE
            ))
            .updateFrequency(100) // 100ms updates
            .includeSnapshot(true)
            .orderBookDepth(10)
            .includeExtendedHours(true)
            .maxUpdatesPerSecond(500)
            .preferences(SubscriptionPreferences.builder()
                .compression(true)
                .format(SubscriptionPreferences.DataFormat.COMPRESSED_JSON)
                .decimalPrecision(4)
                .timeZone("Asia/Kolkata")
                .includeMetadata(true)
                .batchUpdates(true)
                .batchSize(20)
                .build())
            .build();
    }
}