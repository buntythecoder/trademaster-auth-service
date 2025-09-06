package com.trademaster.behavioralai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trading Pattern Data
 * 
 * Immutable record representing trading data points for institutional activity analysis.
 * Contains price, volume, and timestamp information with validation.
 */
public record TradingPatternData(
    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    BigDecimal price,
    
    @NotNull(message = "Volume cannot be null")
    @Positive(message = "Volume must be positive")
    BigDecimal volume,
    
    @NotNull(message = "Timestamp cannot be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    
    String tradeId,
    
    String side, // BUY, SELL
    
    String orderType, // MARKET, LIMIT, STOP, etc.
    
    BigDecimal quantity, // Original order quantity
    
    BigDecimal executedQuantity // Actually executed quantity
) {
    
    /**
     * Factory method for market data
     */
    public static TradingPatternData marketData(BigDecimal price, BigDecimal volume, LocalDateTime timestamp) {
        return new TradingPatternData(
            price,
            volume,
            timestamp,
            null,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Factory method for trade data
     */
    public static TradingPatternData tradeData(
            BigDecimal price, 
            BigDecimal volume, 
            LocalDateTime timestamp,
            String tradeId,
            String side,
            String orderType) {
        return new TradingPatternData(
            price,
            volume,
            timestamp,
            tradeId,
            side,
            orderType,
            volume, // Default quantity to volume
            volume  // Default executed to volume
        );
    }
    
    /**
     * Factory method for order data with execution details
     */
    public static TradingPatternData orderData(
            BigDecimal price,
            BigDecimal volume,
            LocalDateTime timestamp,
            String tradeId,
            String side,
            String orderType,
            BigDecimal quantity,
            BigDecimal executedQuantity) {
        return new TradingPatternData(
            price,
            volume,
            timestamp,
            tradeId,
            side,
            orderType,
            quantity,
            executedQuantity
        );
    }
    
    /**
     * Check if this is a buy order
     */
    public boolean isBuyOrder() {
        return "BUY".equalsIgnoreCase(side);
    }
    
    /**
     * Check if this is a sell order
     */
    public boolean isSellOrder() {
        return "SELL".equalsIgnoreCase(side);
    }
    
    /**
     * Check if this is a market order
     */
    public boolean isMarketOrder() {
        return "MARKET".equalsIgnoreCase(orderType);
    }
    
    /**
     * Check if this is a limit order
     */
    public boolean isLimitOrder() {
        return "LIMIT".equalsIgnoreCase(orderType);
    }
    
    /**
     * Calculate fill ratio (executed vs original quantity)
     */
    public BigDecimal getFillRatio() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        
        var executed = executedQuantity != null ? executedQuantity : volume;
        return executed.divide(quantity, 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Check if order was partially filled
     */
    public boolean isPartiallyFilled() {
        var fillRatio = getFillRatio();
        return fillRatio.compareTo(BigDecimal.ZERO) > 0 && 
               fillRatio.compareTo(BigDecimal.ONE) < 0;
    }
    
    /**
     * Get trade value (price * volume)
     */
    public BigDecimal getTradeValue() {
        return price.multiply(volume);
    }
    
    /**
     * Create a copy with updated timestamp
     */
    public TradingPatternData withTimestamp(LocalDateTime newTimestamp) {
        return new TradingPatternData(
            price, volume, newTimestamp, tradeId, side, orderType, quantity, executedQuantity
        );
    }
    
    /**
     * Create a copy with updated price
     */
    public TradingPatternData withPrice(BigDecimal newPrice) {
        return new TradingPatternData(
            newPrice, volume, timestamp, tradeId, side, orderType, quantity, executedQuantity
        );
    }
    
    /**
     * Create a copy with updated volume
     */
    public TradingPatternData withVolume(BigDecimal newVolume) {
        return new TradingPatternData(
            price, newVolume, timestamp, tradeId, side, orderType, quantity, executedQuantity
        );
    }
}