package com.trademaster.marketdata.dto;

import java.time.LocalDateTime;

/**
 * Market Data Request Record
 * 
 * Immutable request object for market data operations following Java 24 patterns.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record MarketDataRequest(
    String symbol,
    String exchange,
    LocalDateTime from,
    LocalDateTime to,
    String interval,
    String dataType
) {
    public MarketDataRequest {
        // Validation can be added here if needed
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("Exchange cannot be null or empty");
        }
    }
    
    public static MarketDataRequestBuilder builder() {
        return new MarketDataRequestBuilder();
    }
    
    public static class MarketDataRequestBuilder {
        private String symbol;
        private String exchange;
        private LocalDateTime from;
        private LocalDateTime to;
        private String interval = "1min";
        private String dataType = "PRICE";
        
        public MarketDataRequestBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public MarketDataRequestBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }
        
        public MarketDataRequestBuilder from(LocalDateTime from) {
            this.from = from;
            return this;
        }
        
        public MarketDataRequestBuilder to(LocalDateTime to) {
            this.to = to;
            return this;
        }
        
        public MarketDataRequestBuilder interval(String interval) {
            this.interval = interval;
            return this;
        }
        
        public MarketDataRequestBuilder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }
        
        public MarketDataRequest build() {
            return new MarketDataRequest(symbol, exchange, from, to, interval, dataType);
        }
    }
}