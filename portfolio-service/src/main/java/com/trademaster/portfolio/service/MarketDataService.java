package com.trademaster.portfolio.service;

import com.trademaster.portfolio.functional.Result;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Service Interface (Future Integration)
 * 
 * Defines contract for real-time and historical market data operations.
 * Supports integration with external market data providers.
 * 
 * Features:
 * - Real-time price feeds
 * - Historical data retrieval
 * - Market statistics and indicators
 * - Circuit breaker integration
 * - Async operations with Virtual Threads
 * 
 * Integration Points:
 * - External market data providers (NSE, BSE, Reuters, Bloomberg)
 * - Real-time WebSocket feeds
 * - Historical data warehouses
 * - Price validation and normalization
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Service Interface)
 */
public interface MarketDataService {
    
    /**
     * Get current market price for a symbol
     */
    CompletableFuture<Result<BigDecimal, MarketDataError>> getCurrentPrice(String symbol);
    
    /**
     * Get current prices for multiple symbols
     */
    CompletableFuture<Result<Map<String, BigDecimal>, MarketDataError>> getCurrentPrices(List<String> symbols);
    
    /**
     * Get historical price data for analysis
     */
    CompletableFuture<Result<List<PriceData>, MarketDataError>> getHistoricalPrices(
            String symbol, String period);
    
    /**
     * Get market statistics for a symbol
     */
    CompletableFuture<Result<MarketStats, MarketDataError>> getMarketStats(String symbol);
    
    /**
     * Check if market is open for trading
     */
    CompletableFuture<Result<Boolean, MarketDataError>> isMarketOpen();
    
    /**
     * Subscribe to real-time price updates
     */
    CompletableFuture<Result<String, MarketDataError>> subscribeToPriceUpdates(
            List<String> symbols, PriceUpdateCallback callback);
    
    /**
     * Unsubscribe from price updates
     */
    CompletableFuture<Result<Void, MarketDataError>> unsubscribeFromPriceUpdates(String subscriptionId);

    /**
     * Calculate return percentage for a symbol between two dates
     *
     * @param symbol Stock symbol
     * @param fromDate Start date
     * @param toDate End date
     * @return Return percentage as BigDecimal
     */
    BigDecimal calculateReturn(String symbol, java.time.Instant fromDate, java.time.Instant toDate);

    /**
     * Price data record for historical analysis
     */
    record PriceData(
        String symbol,
        BigDecimal price,
        BigDecimal high,
        BigDecimal low,
        Long volume,
        java.time.Instant timestamp
    ) {}
    
    /**
     * Market statistics record
     */
    record MarketStats(
        String symbol,
        BigDecimal currentPrice,
        BigDecimal dayChange,
        BigDecimal dayChangePercent,
        BigDecimal weekHigh,
        BigDecimal weekLow,
        Long avgVolume,
        BigDecimal marketCap
    ) {}
    
    /**
     * Price update callback interface
     */
    @FunctionalInterface
    interface PriceUpdateCallback {
        void onPriceUpdate(String symbol, BigDecimal newPrice, java.time.Instant timestamp);
    }
    
    /**
     * Market data error types
     */
    sealed interface MarketDataError {
        
        record SymbolNotFound(String symbol) implements MarketDataError {}
        
        record ServiceUnavailable(String reason) implements MarketDataError {}
        
        record RateLimitExceeded(String message) implements MarketDataError {}
        
        record InvalidData(String symbol, String reason) implements MarketDataError {}
        
        record ConnectionError(String provider, String message) implements MarketDataError {}
    }
}