package com.trademaster.marketdata.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * InfluxDB measurement for market data points
 * 
 * Schema Design:
 * - Tags: symbol, exchange, data_type (indexed for fast queries)
 * - Fields: price, volume, bid, ask (numeric values)
 * - Timestamp: Millisecond precision for real-time data
 * 
 * @author TradeMaster Development Team  
 * @version 1.0.0
 */
@Measurement(name = "market_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataPoint {
    
    // Tags (indexed)
    @Column(tag = true)
    private String symbol;
    
    @Column(tag = true)
    private String exchange;
    
    @Column(tag = true)
    private String dataType;
    
    @Column(tag = true)
    private String source;
    
    // Fields (values)
    @Column
    private BigDecimal price;
    
    @Column
    private Long volume;
    
    @Column
    private BigDecimal bid;
    
    @Column
    private BigDecimal ask;
    
    @Column
    private BigDecimal high;
    
    @Column
    private BigDecimal low;
    
    @Column
    private BigDecimal open;
    
    @Column
    private BigDecimal previousClose;
    
    @Column
    private BigDecimal change;
    
    @Column
    private BigDecimal changePercent;
    
    @Column
    private Long bidSize;
    
    @Column
    private Long askSize;
    
    @Column
    private String marketStatus;
    
    @Column
    private Double qualityScore;
    
    // Timestamp
    @Column(timestamp = true)
    private Instant timestamp;
    
    /**
     * Create tick data point (real-time price update)
     */
    public static MarketDataPoint createTickData(String symbol, String exchange, 
            BigDecimal price, Long volume, Instant timestamp) {
        return new MarketDataPoint(
            symbol, exchange, "TICK", "REALTIME",
            price, volume, null, null, null, null, null, null, null, null,
            null, null, null, 1.0, timestamp
        );
    }
    
    /**
     * Create OHLC data point (aggregated candle data)
     */
    public static MarketDataPoint createOHLCData(String symbol, String exchange,
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, 
            Long volume, Instant timestamp) {
        return new MarketDataPoint(
            symbol, exchange, "OHLC", "AGGREGATED",
            close, volume, null, null, high, low, open, null, null, null,
            null, null, null, 1.0, timestamp
        );
    }
    
    /**
     * Create order book data point (bid/ask spreads)
     */
    public static MarketDataPoint createOrderBookData(String symbol, String exchange,
            BigDecimal bid, BigDecimal ask, Long bidSize, Long askSize, Instant timestamp) {
        return new MarketDataPoint(
            symbol, exchange, "ORDER_BOOK", "REALTIME",
            bid.add(ask).divide(BigDecimal.valueOf(2)), null, // Mid price
            bid, ask, null, null, null, null, null, null,
            bidSize, askSize, null, 1.0, timestamp
        );
    }
    
    /**
     * Validation methods
     */
    public boolean isValid() {
        return symbol != null && !symbol.trim().isEmpty() &&
               exchange != null && !exchange.trim().isEmpty() &&
               timestamp != null &&
               (price != null || (bid != null && ask != null));
    }
    
    public boolean isRealtime() {
        return "REALTIME".equals(source) && 
               timestamp.isAfter(Instant.now().minusSeconds(60));
    }
    
    public boolean hasOrderBookData() {
        return bid != null && ask != null && 
               bidSize != null && askSize != null;
    }
    
    public BigDecimal getMarketSpread() {
        return hasOrderBookData() ? ask.subtract(bid) : null;
    }
    
    public BigDecimal getSpreadPercentage() {
        if (!hasOrderBookData() || bid.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return getMarketSpread().divide(bid, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    // Convenience methods for services expecting these names
    public BigDecimal getSpread() {
        return getMarketSpread();
    }
    
    public BigDecimal close() {
        return price;
    }
    
    // Record-style accessors for functional programming compatibility
    public String symbol() {
        return this.symbol;
    }
    
    public String exchange() {
        return this.exchange;
    }
    
    public String dataType() {
        return this.dataType;
    }
    
    public String source() {
        return this.source;
    }
    
    public BigDecimal price() {
        return this.price;
    }
    
    public Long volume() {
        return this.volume;
    }
    
    public BigDecimal bid() {
        return this.bid;
    }
    
    public BigDecimal ask() {
        return this.ask;
    }
    
    public BigDecimal high() {
        return this.high;
    }
    
    public BigDecimal low() {
        return this.low;
    }
    
    public BigDecimal open() {
        return this.open;
    }
    
    public BigDecimal previousClose() {
        return this.previousClose;
    }
    
    public BigDecimal change() {
        return this.change;
    }
    
    public BigDecimal changePercent() {
        return this.changePercent;
    }
    
    public Long bidSize() {
        return this.bidSize;
    }
    
    public Long askSize() {
        return this.askSize;
    }
    
    public String marketStatus() {
        return this.marketStatus;
    }
    
    public Double qualityScore() {
        return this.qualityScore;
    }
    
    public Instant timestamp() {
        return this.timestamp;
    }
    
    /**
     * Data quality assessment
     */
    public QualityAssessment assessQuality() {
        double quality = qualityScore != null ? qualityScore : 0.0;
        
        // Adjust quality based on data completeness
        if (price == null) quality -= 0.3;
        if (volume == null || volume == 0) quality -= 0.2;
        if (timestamp.isBefore(Instant.now().minus(Duration.ofMinutes(5)))) quality -= 0.1;
        
        return switch (Double.compare(quality, 0.8)) {
            case 1, 0 -> QualityAssessment.HIGH;
            case -1 -> Double.compare(quality, 0.5) >= 0 
                      ? QualityAssessment.MEDIUM 
                      : QualityAssessment.LOW;
            default -> QualityAssessment.LOW;
        };
    }
    
    public enum QualityAssessment {
        HIGH("High quality data with complete fields"),
        MEDIUM("Good quality data with minor gaps"),
        LOW("Low quality data requiring validation");
        
        private final String description;
        
        QualityAssessment(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}