package com.trademaster.marketdata.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OHLCV Data DTO
 * 
 * Lightweight data transfer object for Open, High, Low, Close, Volume data
 * optimized for charting applications and technical analysis.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record OHLCVData(
    Instant timestamp,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    Long volume
) {
    
    /**
     * Calculate price change from open to close
     */
    public BigDecimal getPriceChange() {
        return close.subtract(open);
    }
    
    /**
     * Calculate price change percentage
     */
    public BigDecimal getPriceChangePercent() {
        if (open.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getPriceChange()
            .divide(open, 6, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Calculate trading range (high - low)
     */
    public BigDecimal getTradingRange() {
        return high.subtract(low);
    }
    
    /**
     * Check if this is a bullish candle
     */
    public boolean isBullish() {
        return close.compareTo(open) > 0;
    }
    
    /**
     * Check if this is a bearish candle
     */
    public boolean isBearish() {
        return close.compareTo(open) < 0;
    }
    
    /**
     * Get typical price (HLC/3)
     */
    public BigDecimal getTypicalPrice() {
        return high.add(low).add(close)
            .divide(new BigDecimal("3"), 6, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Format for display
     */
    public String getDisplayString() {
        return String.format("O:%.2f H:%.2f L:%.2f C:%.2f V:%,d", 
            open.doubleValue(), high.doubleValue(), 
            low.doubleValue(), close.doubleValue(), volume);
    }
}