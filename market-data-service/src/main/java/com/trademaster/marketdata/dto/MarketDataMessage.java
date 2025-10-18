package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

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
 * Converted to immutable record for MANDATORY RULE #9 compliance.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketDataMessage(
    /**
     * Market data type
     */
    MarketDataType type,

    /**
     * Symbol/Instrument identifier
     */
    String symbol,

    /**
     * Exchange identifier
     */
    String exchange,

    /**
     * Current price
     */
    BigDecimal price,

    /**
     * Volume
     */
    Long volume,

    /**
     * Bid price
     */
    BigDecimal bid,

    /**
     * Ask price
     */
    BigDecimal ask,

    /**
     * Bid size
     */
    Long bidSize,

    /**
     * Ask size
     */
    Long askSize,

    /**
     * Open price
     */
    BigDecimal open,

    /**
     * High price
     */
    BigDecimal high,

    /**
     * Low price
     */
    BigDecimal low,

    /**
     * Close price
     */
    BigDecimal close,

    /**
     * Previous close price
     */
    BigDecimal previousClose,

    /**
     * Change amount
     */
    BigDecimal change,

    /**
     * Change percentage
     */
    BigDecimal changePercent,

    /**
     * Total traded volume for the day
     */
    Long totalVolume,

    /**
     * Total traded value for the day
     */
    BigDecimal totalValue,

    /**
     * Number of trades
     */
    Long tradeCount,

    /**
     * Average trade price
     */
    BigDecimal averagePrice,

    /**
     * 52-week high
     */
    BigDecimal yearHigh,

    /**
     * 52-week low
     */
    BigDecimal yearLow,

    /**
     * Market capitalization
     */
    BigDecimal marketCap,

    /**
     * Price-to-earnings ratio
     */
    BigDecimal peRatio,

    /**
     * Timestamp of the data
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime timestamp,

    /**
     * Sequence number for ordering
     */
    Long sequenceNumber,

    /**
     * Market status
     */
    MarketStatus marketStatus,

    /**
     * Circuit breaker information
     */
    CircuitBreakerInfo circuitBreaker,

    /**
     * Additional metadata
     */
    Map<String, Object> metadata
) {

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
     * Circuit breaker information nested record
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CircuitBreakerInfo(
        boolean active,
        String level, // L1, L2, L3
        BigDecimal triggerPrice,
        LocalDateTime triggerTime,
        LocalDateTime resumeTime,
        String reason
    ) {}

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