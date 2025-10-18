package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Real-Time Data Response DTO
 *
 * Response containing real-time market data for requested symbols.
 * RULE #9 COMPLIANT: Immutable record with builder pattern.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record RealTimeDataResponse(
    List<String> symbols,
    Map<String, MarketDataPoint> data,
    DataQuality quality,
    Instant timestamp,
    ResponseMetadata metadata
) {
    /**
     * Compact constructor with validation and defaults
     * RULE #9 COMPLIANT: Immutable validation in constructor
     */
    public RealTimeDataResponse {
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be empty");
        }

        symbols = List.copyOf(symbols);  // Defensive copy
        data = (data != null) ? Map.copyOf(data) : Map.of();  // Defensive copy
        quality = (quality != null) ? quality : DataQuality.GOOD;
        timestamp = (timestamp != null) ? timestamp : Instant.now();
        metadata = (metadata != null) ? metadata : ResponseMetadata.builder().build();
    }

    /**
     * Data quality assessment
     */
    public enum DataQuality {
        EXCELLENT("Real-time data with <50ms latency"),
        GOOD("Real-time data with <200ms latency"),
        FAIR("Near real-time data with <1s latency"),
        POOR("Delayed data with >1s latency"),
        STALE("Data is stale or unavailable");

        private final String description;

        DataQuality(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Response metadata record
     * RULE #9 COMPLIANT: Nested immutable record
     */
    @Builder
    public record ResponseMetadata(
        Integer requestedSymbols,
        Integer successfulSymbols,
        Integer failedSymbols,
        Long processingTimeMs,
        String source
    ) {
        public ResponseMetadata {
            requestedSymbols = (requestedSymbols != null) ? requestedSymbols : 0;
            successfulSymbols = (successfulSymbols != null) ? successfulSymbols : 0;
            failedSymbols = (failedSymbols != null) ? failedSymbols : 0;
            processingTimeMs = (processingTimeMs != null) ? processingTimeMs : 0L;
            source = (source != null && !source.isBlank()) ? source : "CACHE_AND_DATABASE";
        }
    }

    /**
     * Factory methods for common responses
     */
    public static RealTimeDataResponse success(List<String> symbols, Map<String, MarketDataPoint> data) {
        return RealTimeDataResponse.builder()
            .symbols(symbols)
            .data(data)
            .quality(DataQuality.GOOD)
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestedSymbols(symbols.size())
                .successfulSymbols(data.size())
                .failedSymbols(symbols.size() - data.size())
                .build())
            .build();
    }

    public static RealTimeDataResponse empty(List<String> symbols, String reason) {
        return RealTimeDataResponse.builder()
            .symbols(symbols)
            .data(Map.of())
            .quality(DataQuality.STALE)
            .timestamp(Instant.now())
            .metadata(ResponseMetadata.builder()
                .requestedSymbols(symbols.size())
                .successfulSymbols(0)
                .failedSymbols(symbols.size())
                .build())
            .build();
    }
}
