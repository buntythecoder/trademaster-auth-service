package com.trademaster.marketdata.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * Historical Data Request DTO
 *
 * Request parameters for historical market data retrieval.
 * RULE #9 COMPLIANT: Immutable record with builder pattern.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record HistoricalDataRequest(
    @NotNull(message = "Symbols cannot be null")
    @NotEmpty(message = "At least one symbol must be provided")
    List<String> symbols,

    @NotNull(message = "Timeframe is required")
    String timeframe,

    String exchange,

    HistoricalDataType dataType,

    Integer limit
) {
    /**
     * Compact constructor with validation and defaults
     * RULE #9 COMPLIANT: Immutable validation in constructor
     */
    public HistoricalDataRequest {
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be empty");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("Timeframe cannot be blank");
        }

        symbols = List.copyOf(symbols);  // Defensive copy
        dataType = (dataType != null) ? dataType : HistoricalDataType.OHLC;
        limit = (limit != null && limit > 0) ? limit : 100;  // Default 100 records

        // Validate timeframe format
        if (!isValidTimeframe(timeframe)) {
            throw new IllegalArgumentException(
                "Invalid timeframe format. Expected: 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w, 1M"
            );
        }
    }

    /**
     * Historical data type enum
     */
    public enum HistoricalDataType {
        OHLC("Open, High, Low, Close data"),
        TICK("Tick-by-tick data"),
        TRADE("Individual trade data"),
        QUOTE("Bid/Ask quote data");

        private final String description;

        HistoricalDataType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Validate timeframe format (e.g., "1m", "5m", "1h", "1d")
     */
    private static boolean isValidTimeframe(String timeframe) {
        return timeframe != null && timeframe.matches("^\\d+[mhDwM]$");
    }

    /**
     * Parse timeframe to milliseconds
     */
    public long getTimeframeMillis() {
        if (timeframe == null || timeframe.length() < 2) {
            return 60000; // Default 1 minute
        }

        char unit = timeframe.charAt(timeframe.length() - 1);
        int value = Integer.parseInt(timeframe.substring(0, timeframe.length() - 1));

        return switch (unit) {
            case 'm' -> value * 60_000L;           // Minutes
            case 'h' -> value * 3_600_000L;        // Hours
            case 'D', 'd' -> value * 86_400_000L;  // Days
            case 'w' -> value * 604_800_000L;      // Weeks
            case 'M' -> value * 2_592_000_000L;    // Months (30 days)
            default -> 60_000L;                    // Default 1 minute
        };
    }

    /**
     * Factory methods for common requests
     */
    public static HistoricalDataRequest intraday(String symbol, String interval) {
        return HistoricalDataRequest.builder()
            .symbols(List.of(symbol))
            .timeframe(interval)
            .dataType(HistoricalDataType.OHLC)
            .limit(100)
            .build();
    }

    public static HistoricalDataRequest daily(String symbol, int days) {
        return HistoricalDataRequest.builder()
            .symbols(List.of(symbol))
            .timeframe("1d")
            .dataType(HistoricalDataType.OHLC)
            .limit(days)
            .build();
    }

    public static HistoricalDataRequest multiSymbol(List<String> symbols, String timeframe) {
        return HistoricalDataRequest.builder()
            .symbols(symbols)
            .timeframe(timeframe)
            .dataType(HistoricalDataType.OHLC)
            .limit(100)
            .build();
    }
}
