package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Market Data Update DTO
 * 
 * Data transfer object containing market price updates for securities.
 * Used for bulk price updates from market data feeds.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record MarketDataUpdate(
    String symbol,
    String exchange,
    BigDecimal price,
    BigDecimal bidPrice,
    BigDecimal askPrice,
    Long volume,
    Instant timestamp,
    String source
) {
    public MarketDataUpdate {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        
        // Set default values for optional fields
        if (exchange == null) exchange = "UNKNOWN";
        if (volume == null) volume = 0L;
        if (source == null) source = "MARKET_DATA_FEED";
    }
}