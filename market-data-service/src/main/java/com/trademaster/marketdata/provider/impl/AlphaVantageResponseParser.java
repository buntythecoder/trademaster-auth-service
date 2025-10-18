package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.functional.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Alpha Vantage Response Parser (Parsing Operations Only)
 *
 * Single Responsibility: Parse Alpha Vantage API responses to domain objects
 * Following Rule #2 (SRP), Rule #11 (Functional Error Handling), Rule #13 (Stream API)
 *
 * Features:
 * - Parse historical time series data (OHLC)
 * - Parse current price quotes (TICK)
 * - Transform JSON responses to MarketDataMessage
 * - Functional error handling with Try monad
 * - Stream API for collection processing
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AlphaVantageResponseParser {

    /**
     * Parse historical data response from Alpha Vantage TIME_SERIES_DAILY endpoint
     * Rule #11: Functional error handling with Try monad
     * Rule #13: Stream API instead of loops
     */
    public Try<List<MarketDataMessage>> parseHistoricalDataResponse(
            Map<String, Object> response, String symbol, String exchange) {
        return Try.of(() -> {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> timeSeries =
                (Map<String, Map<String, String>>) response.get("Time Series (Daily)");

            return Optional.ofNullable(timeSeries)
                .map(series -> series.entrySet().stream()
                    .map(entry -> parseTimeSeriesEntry(entry, symbol, exchange))
                    .filter(Objects::nonNull)
                    .toList())
                .orElse(List.of());
        });
    }

    /**
     * Parse current price response from Alpha Vantage GLOBAL_QUOTE endpoint
     * Rule #11: Functional error handling with Try monad
     */
    public Try<MarketDataMessage> parseCurrentPriceResponse(
            Map<String, Object> response, String symbol, String exchange) {
        return Try.of(() -> {
            @SuppressWarnings("unchecked")
            Map<String, String> quote = (Map<String, String>) response.get("Global Quote");

            return Optional.ofNullable(quote)
                .map(q -> buildTickMessage(q, symbol, exchange))
                .orElseThrow(() -> new RuntimeException("Unable to parse price data from Alpha Vantage response"));
        });
    }

    // Helper methods (Rule #5: Max 15 lines per method)

    /**
     * Parse single time series entry to MarketDataMessage
     */
    private MarketDataMessage parseTimeSeriesEntry(
            Map.Entry<String, Map<String, String>> entry, String symbol, String exchange) {
        return Try.of(() -> MarketDataMessage.builder()
            .symbol(symbol)
            .exchange(exchange)
            .timestamp(LocalDateTime.parse(entry.getKey() + "T16:00:00"))
            .price(new BigDecimal(entry.getValue().get("4. close")))
            .open(new BigDecimal(entry.getValue().get("1. open")))
            .high(new BigDecimal(entry.getValue().get("2. high")))
            .low(new BigDecimal(entry.getValue().get("3. low")))
            .volume(Long.parseLong(entry.getValue().get("5. volume")))
            .type(MarketDataMessage.MarketDataType.OHLC)
            .build())
        .recover(e -> {
            log.warn("Failed to parse entry for {}: {}", entry.getKey(), e.getMessage());
            return null;
        })
        .toOptional()
        .orElse(null);
    }

    /**
     * Build tick message from quote data
     */
    private MarketDataMessage buildTickMessage(
            Map<String, String> quote, String symbol, String exchange) {
        return MarketDataMessage.builder()
            .symbol(symbol)
            .exchange(exchange)
            .timestamp(LocalDateTime.now())
            .price(new BigDecimal(quote.get("05. price")))
            .change(new BigDecimal(quote.get("09. change")))
            .changePercent(new BigDecimal(quote.get("10. change percent").replace("%", "")))
            .volume(Long.parseLong(quote.get("06. volume")))
            .type(MarketDataMessage.MarketDataType.TICK)
            .build();
    }
}
