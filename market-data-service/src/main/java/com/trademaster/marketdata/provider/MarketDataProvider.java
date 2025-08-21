package com.trademaster.marketdata.provider;

import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.dto.ProviderMetrics;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Market Data Provider Interface
 * 
 * Abstract interface for all market data providers enabling:
 * - Vendor-agnostic implementation
 * - Hot-swappable provider system
 * - Cost-effective provider integration
 * - Consistent error handling and metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface MarketDataProvider {

    /**
     * Provider identification
     */
    String getProviderId();
    String getProviderName();
    String getVersion();
    
    /**
     * Provider capabilities
     */
    Set<String> getSupportedExchanges();
    Set<String> getSupportedDataTypes();
    boolean supportsRealtime();
    boolean supportsHistorical();
    
    /**
     * Cost and rate limiting information
     */
    int getDailyRateLimit();
    double getCostPerRequest();
    boolean isFreeTier();
    
    /**
     * Connection management
     */
    CompletableFuture<Boolean> connect();
    CompletableFuture<Void> disconnect();
    boolean isConnected();
    
    /**
     * Real-time data streaming
     */
    void subscribeToSymbol(String symbol, String exchange, Consumer<MarketDataMessage> callback);
    CompletableFuture<Void> unsubscribeFromSymbol(String symbol, String exchange);
    void subscribeToMultipleSymbols(Set<String> symbols, String exchange, Consumer<MarketDataMessage> callback);
    
    /**
     * Historical data retrieval
     */
    CompletableFuture<List<MarketDataMessage>> getHistoricalData(
        String symbol, 
        String exchange, 
        LocalDateTime from, 
        LocalDateTime to
    );
    
    /**
     * Current price data
     */
    CompletableFuture<MarketDataMessage> getCurrentPrice(String symbol, String exchange);
    CompletableFuture<List<MarketDataMessage>> getCurrentPrices(Set<String> symbols, String exchange);
    
    /**
     * Health and monitoring
     */
    CompletableFuture<ProviderMetrics> getMetrics();
    boolean isHealthy();
    double getLatencyMs();
    double getSuccessRate();
    
    /**
     * Configuration and validation
     */
    void configure(ProviderConfig config);
    boolean validateConfiguration();
    
    /**
     * Provider priority for fallback scenarios
     */
    int getPriority();
    
    /**
     * Provider type for categorization
     */
    ProviderType getProviderType();
    
    /**
     * Provider configuration class
     */
    interface ProviderConfig {
        String getApiKey();
        String getApiSecret();
        String getBaseUrl();
        int getTimeoutMs();
        int getRetryAttempts();
        boolean isEnabled();
    }
    
    /**
     * Provider type enumeration
     */
    enum ProviderType {
        FREE,           // Free tier providers (Alpha Vantage, Yahoo Finance)
        PREMIUM,        // Paid providers (NSE, BSE official)
        BROKER,         // Broker APIs (Zerodha, Upstox)
        CRYPTOCURRENCY, // Crypto exchanges (Binance, CoinGecko)
        COMMODITIES    // Commodity exchanges (MCX)
    }
}