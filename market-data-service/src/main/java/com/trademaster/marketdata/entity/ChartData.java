package com.trademaster.marketdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Chart Data Entity
 * 
 * Stores time-series market data optimized for charting applications
 * with multiple timeframes and technical indicators.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "chart_data", indexes = {
    @Index(name = "idx_chart_data_symbol_timeframe", columnList = "symbol, timeframe"),
    @Index(name = "idx_chart_data_timestamp", columnList = "timestamp"),
    @Index(name = "idx_chart_data_symbol_timestamp", columnList = "symbol, timestamp"),
    @Index(name = "idx_chart_data_timeframe_timestamp", columnList = "timeframe, timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 20)
    private String symbol;
    
    @Column(nullable = false, length = 10)
    private String exchange;
    
    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    private Timeframe timeframe;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    // OHLCV data
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal open;
    
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal high;
    
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal low;
    
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal close;
    
    @Column(nullable = false)
    private Long volume;
    
    // Adjusted data
    @Column(precision = 15, scale = 6)
    private BigDecimal adjustedClose;
    
    @Column(precision = 8, scale = 6)
    private BigDecimal splitCoefficient;
    
    @Column(precision = 15, scale = 6)
    private BigDecimal dividendAmount;
    
    // Technical indicators (commonly used)
    @Column(precision = 15, scale = 6)
    private BigDecimal sma20; // Simple Moving Average 20
    
    @Column(precision = 15, scale = 6)
    private BigDecimal sma50; // Simple Moving Average 50
    
    @Column(precision = 15, scale = 6)
    private BigDecimal sma200; // Simple Moving Average 200
    
    @Column(precision = 15, scale = 6)
    private BigDecimal ema12; // Exponential Moving Average 12
    
    @Column(precision = 15, scale = 6)
    private BigDecimal ema26; // Exponential Moving Average 26
    
    @Column(precision = 8, scale = 4)
    private BigDecimal rsi; // Relative Strength Index
    
    @Column(precision = 15, scale = 6)
    private BigDecimal macd; // MACD Line
    
    @Column(precision = 15, scale = 6)
    private BigDecimal macdSignal; // MACD Signal Line
    
    @Column(precision = 15, scale = 6)
    private BigDecimal macdHistogram; // MACD Histogram
    
    @Column(precision = 15, scale = 6)
    private BigDecimal bollingerUpper; // Bollinger Bands Upper
    
    @Column(precision = 15, scale = 6)
    private BigDecimal bollingerMiddle; // Bollinger Bands Middle
    
    @Column(precision = 15, scale = 6)
    private BigDecimal bollingerLower; // Bollinger Bands Lower
    
    @Column(precision = 8, scale = 4)
    private BigDecimal stochK; // Stochastic %K
    
    @Column(precision = 8, scale = 4)
    private BigDecimal stochD; // Stochastic %D
    
    @Column(precision = 15, scale = 6)
    private BigDecimal atr; // Average True Range
    
    @Column(precision = 15, scale = 6)
    private BigDecimal obv; // On Balance Volume
    
    @Column(precision = 8, scale = 4)
    private BigDecimal williamsR; // Williams %R
    
    @Column(precision = 8, scale = 4)
    private BigDecimal cci; // Commodity Channel Index
    
    // Market microstructure data
    @Column
    private Integer tradeCount;
    
    @Column(precision = 15, scale = 6)
    private BigDecimal vwap; // Volume Weighted Average Price
    
    @Column(precision = 15, scale = 6)
    private BigDecimal twap; // Time Weighted Average Price
    
    @Column(precision = 8, scale = 4)
    private BigDecimal volatility; // Historical volatility
    
    // Metadata
    @Column
    @Builder.Default
    private Boolean isComplete = true; // Is this candle complete
    
    @Column
    @Builder.Default
    private Boolean hasGaps = false; // Trading gaps detected
    
    @Column
    private String dataSource; // Source of the data
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @Version
    private Long version;
    
    /**
     * Timeframe enumeration
     */
    public enum Timeframe {
        M1("1 Minute", 60),
        M5("5 Minutes", 300),
        M15("15 Minutes", 900),
        M30("30 Minutes", 1800),
        H1("1 Hour", 3600),
        H4("4 Hours", 14400),
        D1("1 Day", 86400),
        W1("1 Week", 604800),
        MN1("1 Month", 2592000);
        
        private final String description;
        private final long secondsInterval;
        
        Timeframe(String description, long secondsInterval) {
            this.description = description;
            this.secondsInterval = secondsInterval;
        }
        
        public String getDescription() {
            return description;
        }
        
        public long getSecondsInterval() {
            return secondsInterval;
        }
        
        public boolean isIntraday() {
            return this.ordinal() < D1.ordinal();
        }
        
        public boolean isDaily() {
            return this == D1;
        }
        
        public boolean isWeeklyOrHigher() {
            return this.ordinal() >= W1.ordinal();
        }
    }
    
    /**
     * Business logic methods
     */
    
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
     * Calculate body size of the candle
     */
    public BigDecimal getBodySize() {
        return close.subtract(open).abs();
    }
    
    /**
     * Calculate upper shadow/wick
     */
    public BigDecimal getUpperShadow() {
        return high.subtract(open.max(close));
    }
    
    /**
     * Calculate lower shadow/wick
     */
    public BigDecimal getLowerShadow() {
        return open.min(close).subtract(low);
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
     * Check if this is a doji candle (open ~= close)
     */
    public boolean isDoji() {
        var threshold = getTradingRange().multiply(new BigDecimal("0.1")); // 10% of range
        return getBodySize().compareTo(threshold) <= 0;
    }
    
    /**
     * Check if this is a hammer pattern
     */
    public boolean isHammer() {
        var bodySize = getBodySize();
        var lowerShadow = getLowerShadow();
        var upperShadow = getUpperShadow();
        
        // Hammer: long lower shadow, short upper shadow, small body
        return lowerShadow.compareTo(bodySize.multiply(new BigDecimal("2"))) > 0 &&
               upperShadow.compareTo(bodySize.multiply(new BigDecimal("0.5"))) < 0;
    }
    
    /**
     * Check if this is a shooting star pattern
     */
    public boolean isShootingStar() {
        var bodySize = getBodySize();
        var lowerShadow = getLowerShadow();
        var upperShadow = getUpperShadow();
        
        // Shooting star: long upper shadow, short lower shadow, small body
        return upperShadow.compareTo(bodySize.multiply(new BigDecimal("2"))) > 0 &&
               lowerShadow.compareTo(bodySize.multiply(new BigDecimal("0.5"))) < 0;
    }
    
    /**
     * Check if volume is above average (requires historical context)
     */
    public boolean isHighVolume(Long averageVolume) {
        if (averageVolume == null || averageVolume == 0) {
            return false;
        }
        return volume > averageVolume * 1.5; // 50% above average
    }
    
    /**
     * Get typical price (HLC/3)
     */
    public BigDecimal getTypicalPrice() {
        return high.add(low).add(close)
            .divide(new BigDecimal("3"), 6, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get true range for ATR calculation
     */
    public BigDecimal getTrueRange(BigDecimal previousClose) {
        if (previousClose == null) {
            return getTradingRange();
        }
        
        var range1 = high.subtract(low);
        var range2 = high.subtract(previousClose).abs();
        var range3 = low.subtract(previousClose).abs();
        
        return range1.max(range2).max(range3);
    }
    
    /**
     * Calculate approximate dollar volume
     */
    public BigDecimal getDollarVolume() {
        return getTypicalPrice().multiply(new BigDecimal(volume));
    }
    
    /**
     * Check if all technical indicators are populated
     */
    public boolean hasAllIndicators() {
        return sma20 != null && sma50 != null && ema12 != null &&
               ema26 != null && rsi != null && macd != null &&
               bollingerUpper != null && stochK != null && atr != null;
    }
    
    /**
     * Get indicator summary for quick analysis
     */
    public String getIndicatorSummary() {
        var summary = new StringBuilder();
        
        if (rsi != null) {
            if (rsi.compareTo(new BigDecimal("70")) >= 0) {
                summary.append("RSI:Overbought ");
            } else if (rsi.compareTo(new BigDecimal("30")) <= 0) {
                summary.append("RSI:Oversold ");
            }
        }
        
        if (macd != null && macdSignal != null) {
            if (macd.compareTo(macdSignal) > 0) {
                summary.append("MACD:Bullish ");
            } else {
                summary.append("MACD:Bearish ");
            }
        }
        
        if (close != null && sma20 != null && sma50 != null) {
            if (close.compareTo(sma20) > 0 && sma20.compareTo(sma50) > 0) {
                summary.append("Trend:Strong-Bull ");
            } else if (close.compareTo(sma20) < 0 && sma20.compareTo(sma50) < 0) {
                summary.append("Trend:Strong-Bear ");
            }
        }
        
        return summary.toString().trim();
    }
    
    /**
     * Format for display purposes
     */
    public String getPriceDisplay() {
        return String.format("O:%.2f H:%.2f L:%.2f C:%.2f V:%,d", 
            open.doubleValue(), high.doubleValue(), 
            low.doubleValue(), close.doubleValue(), volume);
    }
}