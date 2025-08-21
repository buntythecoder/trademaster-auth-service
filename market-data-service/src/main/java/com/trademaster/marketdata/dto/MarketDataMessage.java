package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Market Data Message DTO
 * 
 * Represents real-time market data including:
 * - Tick data (price, volume)
 * - OHLC data (open, high, low, close)
 * - Order book updates
 * - Trade events
 * - Market status updates
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketDataMessage {

    /**
     * Market data type
     */
    private MarketDataType type;

    /**
     * Symbol/Instrument identifier
     */
    private String symbol;

    /**
     * Exchange identifier
     */
    private String exchange;

    /**
     * Current price
     */
    private BigDecimal price;

    /**
     * Volume
     */
    private Long volume;

    /**
     * Bid price
     */
    private BigDecimal bid;

    /**
     * Ask price
     */
    private BigDecimal ask;

    /**
     * Bid size
     */
    private Long bidSize;

    /**
     * Ask size
     */
    private Long askSize;

    /**
     * Open price
     */
    private BigDecimal open;

    /**
     * High price
     */
    private BigDecimal high;

    /**
     * Low price
     */
    private BigDecimal low;

    /**
     * Close price
     */
    private BigDecimal close;

    /**
     * Previous close price
     */
    private BigDecimal previousClose;

    /**
     * Change amount
     */
    private BigDecimal change;

    /**
     * Change percentage
     */
    private BigDecimal changePercent;

    /**
     * Total traded volume for the day
     */
    private Long totalVolume;

    /**
     * Total traded value for the day
     */
    private BigDecimal totalValue;

    /**
     * Number of trades
     */
    private Long tradeCount;

    /**
     * Average trade price
     */
    private BigDecimal averagePrice;

    /**
     * 52-week high
     */
    private BigDecimal yearHigh;

    /**
     * 52-week low
     */
    private BigDecimal yearLow;

    /**
     * Market capitalization
     */
    private BigDecimal marketCap;

    /**
     * Price-to-earnings ratio
     */
    private BigDecimal peRatio;

    /**
     * Timestamp of the data
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * Sequence number for ordering
     */
    private Long sequenceNumber;

    /**
     * Market status
     */
    private MarketStatus marketStatus;

    /**
     * Circuit breaker information
     */
    private CircuitBreakerInfo circuitBreaker;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Market data type enumeration
     */
    public enum MarketDataType {
        TICK,           // Real-time tick data
        OHLC,           // OHLC candlestick data
        ORDER_BOOK,     // Order book updates
        TRADE,          // Trade execution data
        MARKET_STATUS,  // Market status updates
        INDEX,          // Index data
        DERIVATIVE,     // Futures/Options data
        NEWS,           // News and announcements
        CORPORATE_ACTION // Corporate actions
    }

    /**
     * Market status enumeration
     */
    public enum MarketStatus {
        PRE_OPEN,       // Pre-market session
        OPEN,           // Normal trading hours
        CLOSED,         // Market closed
        HALT,           // Trading halt
        SUSPENDED,      // Trading suspended
        CIRCUIT_BREAKER, // Circuit breaker triggered
        AUCTION,        // Auction mode
        POST_CLOSE      // Post-market session
    }

    /**
     * Circuit breaker information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CircuitBreakerInfo {
        private boolean active;
        private String level; // L1, L2, L3
        private BigDecimal triggerPrice;
        private LocalDateTime triggerTime;
        private LocalDateTime resumeTime;
        private String reason;
    }

    /**
     * Create tick data message
     */
    public static MarketDataMessage createTickData(String symbol, String exchange, 
            BigDecimal price, Long volume, LocalDateTime timestamp) {
        return MarketDataMessage.builder()
            .type(MarketDataType.TICK)
            .symbol(symbol)
            .exchange(exchange)
            .price(price)
            .volume(volume)
            .timestamp(timestamp)
            .build();
    }

    /**
     * Create OHLC data message
     */
    public static MarketDataMessage createOHLCData(String symbol, String exchange,
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
            Long volume, LocalDateTime timestamp) {
        return MarketDataMessage.builder()
            .type(MarketDataType.OHLC)
            .symbol(symbol)
            .exchange(exchange)
            .open(open)
            .high(high)
            .low(low)
            .close(close)
            .volume(volume)
            .timestamp(timestamp)
            .build();
    }

    /**
     * Create order book update message
     */
    public static MarketDataMessage createOrderBookUpdate(String symbol, String exchange,
            BigDecimal bid, BigDecimal ask, Long bidSize, Long askSize, LocalDateTime timestamp) {
        return MarketDataMessage.builder()
            .type(MarketDataType.ORDER_BOOK)
            .symbol(symbol)
            .exchange(exchange)
            .bid(bid)
            .ask(ask)
            .bidSize(bidSize)
            .askSize(askSize)
            .timestamp(timestamp)
            .build();
    }

    /**
     * Create market status update message
     */
    public static MarketDataMessage createMarketStatusUpdate(String symbol, String exchange,
            MarketStatus status, LocalDateTime timestamp) {
        return MarketDataMessage.builder()
            .type(MarketDataType.MARKET_STATUS)
            .symbol(symbol)
            .exchange(exchange)
            .marketStatus(status)
            .timestamp(timestamp)
            .build();
    }

    /**
     * Check if this is real-time data (less than 1 second old)
     */
    public boolean isRealTime() {
        if (timestamp == null) {
            return false;
        }
        return LocalDateTime.now().minusSeconds(1).isBefore(timestamp);
    }

    /**
     * Calculate spread for order book data
     */
    public BigDecimal getSpread() {
        if (bid != null && ask != null) {
            return ask.subtract(bid);
        }
        return null;
    }

    /**
     * Calculate spread percentage
     */
    public BigDecimal getSpreadPercent() {
        BigDecimal spread = getSpread();
        if (spread != null && bid != null && bid.compareTo(BigDecimal.ZERO) > 0) {
            return spread.divide(bid, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        return null;
    }
}