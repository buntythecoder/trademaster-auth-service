package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Historical Data Response DTO
 *
 * Response containing historical market data for requested symbols and timeframe.
 * RULE #9 COMPLIANT: Immutable record with builder pattern.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record HistoricalDataResponse(
    List<String> symbols,
    String timeframe,
    Map<String, List<MarketDataPoint>> data,
    DataCompleteness completeness,
    Instant timestamp,
    HistoricalMetadata metadata
) {
    /**
     * Compact constructor with validation and defaults
     * RULE #9 COMPLIANT: Immutable validation in constructor
     */
    public HistoricalDataResponse {
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be empty");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("Timeframe cannot be blank");
        }

        symbols = List.copyOf(symbols);  // Defensive copy
        data = (data != null) ? Map.copyOf(data) : Map.of();  // Defensive copy
        completeness = (completeness != null) ? completeness : DataCompleteness.COMPLETE;
        timestamp = (timestamp != null) ? timestamp : Instant.now();
        metadata = (metadata != null) ? metadata : HistoricalMetadata.builder().build();
    }

    /**
     * Data completeness assessment
     */
    public enum DataCompleteness {
        COMPLETE("All requested data available"),
        PARTIAL("Some data points missing"),
        SPARSE("Significant gaps in data"),
        MINIMAL("Very limited data available"),
        EMPTY("No data available");

        private final String description;

        DataCompleteness(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Calculate completeness based on data availability
         */
        public static DataCompleteness calculate(int expected, int actual) {
            if (actual == 0) return EMPTY;

            double ratio = (double) actual / expected;

            return switch ((int) (ratio * 100)) {
                case int r when r >= 95 -> COMPLETE;
                case int r when r >= 70 -> PARTIAL;
                case int r when r >= 40 -> SPARSE;
                default -> MINIMAL;
            };
        }
    }

    /**
     * Historical data metadata record
     * RULE #9 COMPLIANT: Nested immutable record
     */
    @Builder
    public record HistoricalMetadata(
        Integer requestedSymbols,
        Integer successfulSymbols,
        Integer totalDataPoints,
        Integer expectedDataPoints,
        Long oldestTimestamp,
        Long newestTimestamp,
        Long processingTimeMs,
        String dataSource
    ) {
        public HistoricalMetadata {
            requestedSymbols = (requestedSymbols != null) ? requestedSymbols : 0;
            successfulSymbols = (successfulSymbols != null) ? successfulSymbols : 0;
            totalDataPoints = (totalDataPoints != null) ? totalDataPoints : 0;
            expectedDataPoints = (expectedDataPoints != null) ? expectedDataPoints : 0;
            oldestTimestamp = (oldestTimestamp != null) ? oldestTimestamp : 0L;
            newestTimestamp = (newestTimestamp != null) ? newestTimestamp : 0L;
            processingTimeMs = (processingTimeMs != null) ? processingTimeMs : 0L;
            dataSource = (dataSource != null && !dataSource.isBlank()) ? dataSource : "DATABASE";
        }

        /**
         * Calculate data completeness percentage
         */
        public double getCompletenessPercentage() {
            if (expectedDataPoints == 0) return 0.0;
            return (double) totalDataPoints / expectedDataPoints * 100.0;
        }

        /**
         * Check if data is recent (within last hour)
         */
        public boolean isRecent() {
            long currentTime = System.currentTimeMillis();
            long oneHourAgo = currentTime - 3_600_000L;
            return newestTimestamp >= oneHourAgo;
        }
    }

    /**
     * Factory methods for common responses
     */
    public static HistoricalDataResponse success(
            List<String> symbols,
            String timeframe,
            Map<String, List<MarketDataPoint>> data) {

        int totalPoints = data.values().stream()
            .mapToInt(List::size)
            .sum();

        return HistoricalDataResponse.builder()
            .symbols(symbols)
            .timeframe(timeframe)
            .data(data)
            .completeness(DataCompleteness.COMPLETE)
            .timestamp(Instant.now())
            .metadata(HistoricalMetadata.builder()
                .requestedSymbols(symbols.size())
                .successfulSymbols(data.size())
                .totalDataPoints(totalPoints)
                .build())
            .build();
    }

    public static HistoricalDataResponse empty(List<String> symbols, String timeframe) {
        return HistoricalDataResponse.builder()
            .symbols(symbols)
            .timeframe(timeframe)
            .data(Map.of())
            .completeness(DataCompleteness.EMPTY)
            .timestamp(Instant.now())
            .metadata(HistoricalMetadata.builder()
                .requestedSymbols(symbols.size())
                .successfulSymbols(0)
                .totalDataPoints(0)
                .build())
            .build();
    }

    /**
     * Check if response has data for all requested symbols
     */
    public boolean isComplete() {
        return data.size() == symbols.size() &&
               completeness == DataCompleteness.COMPLETE;
    }

    /**
     * Get total number of data points across all symbols
     */
    public int getTotalDataPoints() {
        return data.values().stream()
            .mapToInt(List::size)
            .sum();
    }
}
