package com.trademaster.marketdata.pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sealed class hierarchy for type-safe market events
 * Enables exhaustive pattern matching without default cases
 */
public sealed interface MarketEvent 
    permits MarketEvent.PriceUpdate, MarketEvent.VolumeUpdate, MarketEvent.TradingHalt, 
            MarketEvent.MarketOpen, MarketEvent.MarketClose, MarketEvent.NewsEvent {
    
    // Common properties
    String symbol();
    LocalDateTime timestamp();
    
    // Price update event
    record PriceUpdate(
        String symbol,
        BigDecimal price,
        BigDecimal previousPrice,
        BigDecimal change,
        BigDecimal changePercent,
        LocalDateTime timestamp
    ) implements MarketEvent {
        
        public boolean isSignificantMove() {
            return changePercent.abs().compareTo(new BigDecimal("5.0")) > 0;
        }
        
        public boolean isPriceIncrease() {
            return change.compareTo(BigDecimal.ZERO) > 0;
        }
        
        public PriceCategory getCategory() {
            return switch (changePercent.abs().compareTo(new BigDecimal("2.0"))) {
                case 1 -> changePercent.compareTo(BigDecimal.ZERO) > 0 ? 
                    PriceCategory.STRONG_BULLISH : PriceCategory.STRONG_BEARISH;
                case 0, -1 -> changePercent.compareTo(BigDecimal.ZERO) > 0 ?
                    PriceCategory.BULLISH : PriceCategory.BEARISH;
                default -> PriceCategory.NEUTRAL;
            };
        }
    }
    
    // Volume update event
    record VolumeUpdate(
        String symbol,
        Long volume,
        Long previousVolume,
        Long averageVolume,
        LocalDateTime timestamp
    ) implements MarketEvent {
        
        public boolean isHighVolume() {
            return volume > averageVolume * 2;
        }
        
        public VolumeCategory getCategory() {
            double ratio = (double) volume / averageVolume;
            return switch (Double.compare(ratio, 2.0)) {
                case 1 -> VolumeCategory.VERY_HIGH;
                case 0 -> VolumeCategory.HIGH;
                case -1 -> ratio > 0.5 ? VolumeCategory.NORMAL : VolumeCategory.LOW;
                default -> VolumeCategory.NORMAL;
            };
        }
    }
    
    // Trading halt event
    record TradingHalt(
        String symbol,
        String reason,
        HaltType haltType,
        LocalDateTime haltTime,
        LocalDateTime expectedResumption,
        LocalDateTime timestamp
    ) implements MarketEvent {
        
        public boolean isVolatilityHalt() {
            return haltType == HaltType.VOLATILITY;
        }
        
        public boolean isNewsHalt() {
            return haltType == HaltType.NEWS_PENDING;
        }
    }
    
    // Market open event
    record MarketOpen(
        String symbol,
        String exchange,
        BigDecimal openingPrice,
        Long openingVolume,
        LocalDateTime timestamp
    ) implements MarketEvent {}
    
    // Market close event
    record MarketClose(
        String symbol,
        String exchange,
        BigDecimal closingPrice,
        Long totalVolume,
        LocalDateTime timestamp
    ) implements MarketEvent {}
    
    // News event
    record NewsEvent(
        String symbol,
        String headline,
        String content,
        NewsSentiment sentiment,
        Double impactScore,
        LocalDateTime timestamp
    ) implements MarketEvent {
        
        public boolean isHighImpact() {
            return impactScore > 0.7;
        }
        
        public boolean isPositive() {
            return sentiment == NewsSentiment.POSITIVE;
        }
    }
    
    // Supporting enums
    enum PriceCategory {
        STRONG_BULLISH, BULLISH, NEUTRAL, BEARISH, STRONG_BEARISH
    }
    
    enum VolumeCategory {
        VERY_HIGH, HIGH, NORMAL, LOW, VERY_LOW
    }
    
    enum HaltType {
        VOLATILITY, NEWS_PENDING, REGULATORY, TECHNICAL, OTHER
    }
    
    enum NewsSentiment {
        POSITIVE, NEUTRAL, NEGATIVE
    }
    
    // Pattern matching methods
    default String analyze() {
        return switch (this) {
            case PriceUpdate(var symbol, var price, var prevPrice, var change, var changePercent, var timestamp) 
                when changePercent.abs().compareTo(new BigDecimal("5.0")) > 0 -> 
                "Significant " + (change.compareTo(BigDecimal.ZERO) > 0 ? "gain" : "loss") + 
                " for " + symbol + ": " + changePercent + "%";
                
            case PriceUpdate(var symbol, var price, var prevPrice, var change, var changePercent, var timestamp) ->
                "Price update for " + symbol + ": $" + price + " (" + 
                (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + changePercent + "%)";
                
            case VolumeUpdate(var symbol, var volume, var prevVolume, var avgVolume, var timestamp) 
                when volume > avgVolume * 3 -> 
                "Exceptional volume spike for " + symbol + ": " + volume + " (3x average)";
                
            case VolumeUpdate(var symbol, var volume, var prevVolume, var avgVolume, var timestamp) ->
                "Volume update for " + symbol + ": " + volume;
                
            case TradingHalt(var symbol, var reason, var haltType, var haltTime, var expectedResumption, var timestamp) ->
                "Trading halted for " + symbol + " (" + haltType + "): " + reason;
                
            case MarketOpen(var symbol, var exchange, var openingPrice, var openingVolume, var timestamp) ->
                exchange + " opened: " + symbol + " at $" + openingPrice;
                
            case MarketClose(var symbol, var exchange, var closingPrice, var totalVolume, var timestamp) ->
                exchange + " closed: " + symbol + " at $" + closingPrice + ", volume: " + totalVolume;
                
            case NewsEvent(var symbol, var headline, var content, var sentiment, var impactScore, var timestamp) 
                when impactScore > 0.8 -> 
                "High-impact " + sentiment.name().toLowerCase() + " news for " + symbol + ": " + headline;
                
            case NewsEvent(var symbol, var headline, var content, var sentiment, var impactScore, var timestamp) ->
                "News for " + symbol + " (" + sentiment.name().toLowerCase() + "): " + headline;
        };
    }
    
    default EventPriority getPriority() {
        return switch (this) {
            case PriceUpdate(var symbol, var price, var prevPrice, var change, var changePercent, var timestamp) 
                when changePercent.abs().compareTo(new BigDecimal("10.0")) > 0 -> EventPriority.CRITICAL;
                
            case VolumeUpdate(var symbol, var volume, var prevVolume, var avgVolume, var timestamp) 
                when volume > avgVolume * 5 -> EventPriority.CRITICAL;
                
            case TradingHalt(var symbol, var reason, var haltType, var haltTime, var expectedResumption, var timestamp) ->
                EventPriority.CRITICAL;
                
            case NewsEvent(var symbol, var headline, var content, var sentiment, var impactScore, var timestamp) 
                when impactScore > 0.8 -> EventPriority.HIGH;
                
            case PriceUpdate(var symbol, var price, var prevPrice, var change, var changePercent, var timestamp) 
                when changePercent.abs().compareTo(new BigDecimal("2.0")) > 0 -> EventPriority.HIGH;
                
            default -> EventPriority.NORMAL;
        };
    }
    
    enum EventPriority {
        CRITICAL, HIGH, NORMAL, LOW
    }
    
    // Factory methods for creating events
    static PriceUpdate priceUpdate(String symbol, BigDecimal price, BigDecimal previousPrice) {
        BigDecimal change = price.subtract(previousPrice);
        BigDecimal changePercent = previousPrice.compareTo(BigDecimal.ZERO) != 0 ?
            change.divide(previousPrice, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
            BigDecimal.ZERO;
            
        return new PriceUpdate(symbol, price, previousPrice, change, changePercent, LocalDateTime.now());
    }
    
    static VolumeUpdate volumeUpdate(String symbol, Long volume, Long previousVolume, Long averageVolume) {
        return new VolumeUpdate(symbol, volume, previousVolume, averageVolume, LocalDateTime.now());
    }
    
    static TradingHalt tradingHalt(String symbol, String reason, HaltType haltType, LocalDateTime expectedResumption) {
        return new TradingHalt(symbol, reason, haltType, LocalDateTime.now(), expectedResumption, LocalDateTime.now());
    }
}