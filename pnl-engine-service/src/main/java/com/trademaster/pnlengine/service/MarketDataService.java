package com.trademaster.pnlengine.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance market data integration providing real-time pricing,
 * historical data, and market analytics for comprehensive P&L calculations.
 * 
 * Core Features:
 * - Real-time price feeds (<100ms latency)
 * - Historical price data for performance attribution
 * - Market indices and benchmark data
 * - Intraday price movements and OHLC data
 * - Corporate actions and dividend information
 * 
 * Performance Targets:
 * - Real-time price: <100ms latency
 * - Bulk price retrieval: <200ms for 100+ symbols
 * - Historical data: <500ms for 1-year dataset
 * - Market indices: <50ms for standard benchmarks
 * - Corporate actions: <24 hours for notification
 * 
 * Integration Points:
 * - External Market Data Providers: NSE, BSE, Reuters, Bloomberg
 * - Cache Layer: Redis for sub-second price lookups
 * - WebSocket Streams: Real-time price updates
 * - Notification Service: Price alerts and threshold notifications
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface MarketDataService {
    
    /**
     * Get current market price for symbol
     * 
     * @param symbol Stock symbol (e.g., RELIANCE, TCS)
     * @return Current market price with timestamp
     */
    CompletableFuture<MarketPrice> getCurrentPrice(String symbol);
    
    /**
     * Get current prices for multiple symbols (bulk operation)
     * 
     * @param symbols List of stock symbols
     * @return Map of symbol to current market price
     */
    CompletableFuture<Map<String, MarketPrice>> getCurrentPrices(List<String> symbols);
    
    /**
     * Get previous trading day's closing price
     * 
     * @param symbol Stock symbol
     * @return Previous close price with date
     */
    CompletableFuture<MarketPrice> getPreviousClosePrice(String symbol);
    
    /**
     * Get historical price data for performance analysis
     * 
     * @param symbol Stock symbol
     * @param fromDate Start date for historical data
     * @param toDate End date for historical data
     * @param granularity Data frequency (DAILY, WEEKLY, MONTHLY)
     * @return Historical price data points
     */
    CompletableFuture<List<HistoricalPrice>> getHistoricalPrices(String symbol, Instant fromDate, 
                                                                Instant toDate, DataGranularity granularity);
    
    /**
     * Get intraday OHLC data for day P&L calculations
     * 
     * @param symbol Stock symbol
     * @param tradingDate Trading date
     * @return Intraday OHLC price data
     */
    CompletableFuture<List<IntradayPrice>> getIntradayPrices(String symbol, Instant tradingDate);
    
    /**
     * Get benchmark index prices for performance attribution
     * 
     * @param benchmarkSymbol Benchmark index (NIFTY50, SENSEX, etc.)
     * @param fromDate Start date
     * @param toDate End date
     * @return Benchmark price data
     */
    CompletableFuture<List<BenchmarkPrice>> getBenchmarkPrices(String benchmarkSymbol, 
                                                              Instant fromDate, Instant toDate);
    
    /**
     * Get corporate actions affecting P&L calculations
     * 
     * @param symbol Stock symbol
     * @param fromDate Start date for corporate actions
     * @param toDate End date for corporate actions
     * @return List of corporate actions (dividends, splits, bonuses)
     */
    CompletableFuture<List<CorporateAction>> getCorporateActions(String symbol, 
                                                               Instant fromDate, Instant toDate);
    
    /**
     * Subscribe to real-time price updates via WebSocket
     * 
     * @param symbols List of symbols to subscribe
     * @param callback Price update callback function
     * @return Subscription identifier
     */
    CompletableFuture<String> subscribeToRealtimePrices(List<String> symbols, 
                                                       PriceUpdateCallback callback);
    
    /**
     * Unsubscribe from real-time price updates
     * 
     * @param subscriptionId Subscription identifier
     */
    CompletableFuture<Void> unsubscribeFromRealtimePrices(String subscriptionId);
    
    /**
     * Get market status and trading hours
     * 
     * @param exchange Exchange identifier (NSE, BSE)
     * @return Current market status
     */
    CompletableFuture<MarketStatus> getMarketStatus(String exchange);
    
    // Supporting data records and interfaces
    
    record MarketPrice(
        String symbol,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent,
        Long volume,
        Instant timestamp,
        String exchange,
        MarketPriceType priceType
    ) {}
    
    record HistoricalPrice(
        String symbol,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        Long volume,
        Instant date,
        String exchange
    ) {}
    
    record IntradayPrice(
        String symbol,
        BigDecimal price,
        Long volume,
        Instant timestamp,
        String exchange
    ) {}
    
    record BenchmarkPrice(
        String benchmarkSymbol,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent,
        Instant timestamp,
        String exchange
    ) {}
    
    record CorporateAction(
        String symbol,
        CorporateActionType actionType,
        BigDecimal value,
        String ratio,
        Instant exDate,
        Instant recordDate,
        Instant paymentDate,
        String description
    ) {}
    
    record MarketStatus(
        String exchange,
        boolean isOpen,
        String status,
        Instant nextOpenTime,
        Instant nextCloseTime,
        String timeZone,
        Instant statusTimestamp
    ) {}
    
    @FunctionalInterface
    interface PriceUpdateCallback {
        void onPriceUpdate(String symbol, MarketPrice price);
    }
    
    enum MarketPriceType {
        REAL_TIME, DELAYED, PREVIOUS_CLOSE, OPENING, CLOSING
    }
    
    enum CorporateActionType {
        DIVIDEND, STOCK_SPLIT, BONUS_ISSUE, RIGHTS_ISSUE, SPIN_OFF, MERGER, ACQUISITION
    }
    
    enum DataGranularity {
        INTRADAY, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }
}