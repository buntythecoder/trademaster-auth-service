package com.trademaster.marketdata.provider;

import com.trademaster.marketdata.entity.MarketDataPoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Exchange Data Providers
 * 
 * Abstraction layer for different exchange data sources including:
 * - Real exchange APIs (NSE, BSE, MCX)
 * - Data vendor APIs (Reuters, Bloomberg)
 * - Simulators for development/testing
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface ExchangeDataProvider {

    /**
     * Get exchange name
     */
    String getExchangeName();

    /**
     * Get current price for a symbol
     */
    CompletableFuture<MarketDataPoint> getCurrentPrice(String symbol);

    /**
     * Get current prices for multiple symbols
     */
    CompletableFuture<List<MarketDataPoint>> getBulkPrices(List<String> symbols);

    /**
     * Check if market is currently open
     */
    boolean isMarketOpen();

    /**
     * Get supported symbols for this exchange
     */
    default CompletableFuture<List<String>> getSupportedSymbols() {
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Subscribe to real-time data stream (for providers that support it)
     */
    default void subscribeToRealTimeData(List<String> symbols, DataStreamHandler handler) {
        throw new UnsupportedOperationException("Real-time streaming not supported by this provider");
    }

    /**
     * Unsubscribe from real-time data stream
     */
    default void unsubscribeFromRealTimeData(List<String> symbols) {
        throw new UnsupportedOperationException("Real-time streaming not supported by this provider");
    }

    /**
     * Handler for real-time data streams
     */
    @FunctionalInterface
    interface DataStreamHandler {
        void onData(MarketDataPoint data);
    }
}