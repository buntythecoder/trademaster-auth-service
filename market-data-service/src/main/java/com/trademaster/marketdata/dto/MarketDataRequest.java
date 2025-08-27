package com.trademaster.marketdata.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Market Data Request Record
 * 
 * Immutable request object for market data operations following Java 24 patterns.
 * Enhanced for AgentOS multi-symbol and multi-operation support.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record MarketDataRequest(
    Long requestId,
    List<String> symbols,
    String timeframe,
    boolean includeIndicators,
    Object scanCriteria,
    String dataType,
    LocalDateTime from,
    LocalDateTime to,
    String exchange
) {
    public MarketDataRequest {
        // Validation for AgentOS compatibility
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be null or empty");
        }
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
    }
    
    public static MarketDataRequestBuilder builder() {
        return new MarketDataRequestBuilder();
    }
    
    public static class MarketDataRequestBuilder {
        private Long requestId;
        private List<String> symbols;
        private String timeframe = "1D";
        private boolean includeIndicators = false;
        private Object scanCriteria;
        private String dataType = "PRICE";
        private LocalDateTime from;
        private LocalDateTime to;
        private String exchange;
        
        public MarketDataRequestBuilder requestId(Long requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public MarketDataRequestBuilder symbols(List<String> symbols) {
            this.symbols = symbols;
            return this;
        }
        
        public MarketDataRequestBuilder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }
        
        public MarketDataRequestBuilder includeIndicators(boolean includeIndicators) {
            this.includeIndicators = includeIndicators;
            return this;
        }
        
        public MarketDataRequestBuilder scanCriteria(Object scanCriteria) {
            this.scanCriteria = scanCriteria;
            return this;
        }
        
        public MarketDataRequestBuilder dataType(String dataType) {
            this.dataType = dataType;
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
        
        public MarketDataRequestBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }
        
        public MarketDataRequest build() {
            return new MarketDataRequest(requestId, symbols, timeframe, includeIndicators, 
                                       scanCriteria, dataType, from, to, exchange);
        }
    }
}