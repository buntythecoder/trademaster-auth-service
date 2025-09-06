package com.trademaster.multibroker.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Market Price Data Transfer Object
 * 
 * MANDATORY: Java 24 + Virtual Threads Architecture - Rule #1
 * MANDATORY: Immutability & Records Usage - Rule #9  
 * MANDATORY: Lombok Standards - Rule #10
 * 
 * Represents real-time market price data from various brokers
 * with comprehensive market data including OHLC, volume, and derived metrics.
 */
@Data
@Builder
@Jacksonized
public class MarketPrice {
    
    private final String symbol;
    private final String exchange;
    private final String brokerSource;
    
    // Price Data
    private final BigDecimal lastTradedPrice;
    private final BigDecimal openPrice;
    private final BigDecimal highPrice;
    private final BigDecimal lowPrice;
    private final BigDecimal closePrice;
    private final BigDecimal previousClose;
    
    // Volume and Turnover
    private final Long volume;
    private final Long totalTradedValue;
    private final Long totalBuyQuantity;
    private final Long totalSellQuantity;
    
    // Price Changes
    private final BigDecimal change;
    private final BigDecimal changePercent;
    
    // Bid/Ask Data
    private final BigDecimal bestBid;
    private final BigDecimal bestAsk;
    private final Long bidQuantity;
    private final Long askQuantity;
    
    // Market Depth (Top 5 levels)
    private final Map<String, Object> marketDepth;
    
    // Timestamps
    private final Instant timestamp;
    private final Instant lastTradeTime;
    
    // Market Status
    private final String marketStatus; // OPEN, CLOSED, PRE_OPEN, POST_CLOSE
    private final Boolean isCircuitLimitHit;
    private final String circuitLimitType; // UPPER, LOWER, NONE
    
    // Derived Metrics
    private final BigDecimal vwap; // Volume Weighted Average Price
    private final BigDecimal averageTradePrice;
    private final Long numberOfTrades;
    
    /**
     * Create market price with essential data only
     */
    public static MarketPrice createBasic(String symbol, String exchange, 
                                        BigDecimal lastPrice, Long volume, 
                                        Instant timestamp) {
        return MarketPrice.builder()
            .symbol(symbol)
            .exchange(exchange)
            .lastTradedPrice(lastPrice)
            .volume(volume)
            .timestamp(timestamp)
            .marketStatus("OPEN")
            .isCircuitLimitHit(false)
            .circuitLimitType("NONE")
            .build();
    }
    
    /**
     * Check if price data is stale (older than 5 seconds)
     */
    public boolean isStale() {
        if (timestamp == null) return true;
        return Instant.now().isAfter(timestamp.plusSeconds(5));
    }
    
    /**
     * Calculate price change from previous close
     */
    public BigDecimal calculatePriceChange() {
        if (lastTradedPrice == null || previousClose == null) {
            return BigDecimal.ZERO;
        }
        return lastTradedPrice.subtract(previousClose);
    }
    
    /**
     * Calculate percentage change from previous close
     */
    public BigDecimal calculatePercentageChange() {
        if (lastTradedPrice == null || previousClose == null || 
            previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return lastTradedPrice.subtract(previousClose)
            .divide(previousClose, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    // Methods expected by MarketDataHandler
    public BigDecimal currentPrice() {
        return lastTradedPrice;
    }
    
    public BigDecimal dayChange() {
        return change != null ? change : calculatePriceChange();
    }
    
    public BigDecimal dayChangePercent() {
        return changePercent != null ? changePercent : calculatePercentageChange();
    }
    
    public Instant lastUpdated() {
        return timestamp;
    }
    
    public Long volume() {
        return volume;
    }
}