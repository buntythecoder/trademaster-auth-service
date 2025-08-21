package com.trademaster.marketdata.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Builder;

import java.math.BigDecimal;
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
@Builder
public record MarketDataPoint(
    
    // Tags (indexed)
    @Column(tag = true)
    String symbol,
    
    @Column(tag = true)
    String exchange,
    
    @Column(tag = true)
    String dataType,
    
    @Column(tag = true)
    String source,
    
    // Fields (values)
    @Column
    BigDecimal price,
    
    @Column
    Long volume,
    
    @Column
    BigDecimal bid,
    
    @Column
    BigDecimal ask,
    
    @Column
    BigDecimal high,
    
    @Column
    BigDecimal low,
    
    @Column
    BigDecimal open,
    
    @Column
    BigDecimal previousClose,
    
    @Column
    BigDecimal change,
    
    @Column
    BigDecimal changePercent,
    
    @Column
    Long bidSize,
    
    @Column
    Long askSize,
    
    @Column
    String marketStatus,
    
    @Column
    Double qualityScore,
    
    // Timestamp
    @Column(timestamp = true)
    Instant timestamp
) {
    
    /**
     * Create tick data point (real-time price update)
     */
    public static MarketDataPoint createTickData(String symbol, String exchange, 
            BigDecimal price, Long volume, Instant timestamp) {
        return MarketDataPoint.builder()
            .symbol(symbol)
            .exchange(exchange)
            .dataType("TICK")
            .source("REALTIME")
            .price(price)
            .volume(volume)
            .timestamp(timestamp)
            .qualityScore(1.0)
            .build();
    }
    
    /**
     * Create OHLC data point (aggregated candle data)
     */
    public static MarketDataPoint createOHLCData(String symbol, String exchange,
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, 
            Long volume, Instant timestamp) {
        return MarketDataPoint.builder()
            .symbol(symbol)
            .exchange(exchange)
            .dataType("OHLC")
            .source("AGGREGATED")
            .open(open)
            .high(high)
            .low(low)
            .price(close) // Close price as primary price field
            .volume(volume)
            .timestamp(timestamp)
            .qualityScore(1.0)
            .build();
    }
    
    /**
     * Create order book data point (bid/ask spreads)
     */
    public static MarketDataPoint createOrderBookData(String symbol, String exchange,
            BigDecimal bid, BigDecimal ask, Long bidSize, Long askSize, Instant timestamp) {
        return MarketDataPoint.builder()
            .symbol(symbol)
            .exchange(exchange)
            .dataType("ORDER_BOOK")
            .source("REALTIME")
            .bid(bid)
            .ask(ask)
            .bidSize(bidSize)
            .askSize(askSize)
            .price(bid.add(ask).divide(BigDecimal.valueOf(2))) // Mid price
            .timestamp(timestamp)
            .qualityScore(1.0)
            .build();
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
    
    public BigDecimal getSpread() {
        return hasOrderBookData() ? ask.subtract(bid) : null;
    }
    
    public BigDecimal getSpreadPercentage() {
        if (!hasOrderBookData() || bid.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return getSpread().divide(bid, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Data quality assessment
     */
    public QualityAssessment assessQuality() {
        double quality = qualityScore != null ? qualityScore : 0.0;
        
        // Adjust quality based on data completeness
        if (price == null) quality -= 0.3;
        if (volume == null || volume == 0) quality -= 0.2;
        if (timestamp.isBefore(Instant.now().minusMinutes(5))) quality -= 0.1;
        
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