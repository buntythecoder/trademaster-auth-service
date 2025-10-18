package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.dto.ProviderMetrics;
import com.trademaster.marketdata.functional.Try;
import com.trademaster.marketdata.provider.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Alpha Vantage Market Data Provider (Refactored with SRP)
 *
 * Single Responsibility: Orchestrate market data operations by delegating to specialized components
 * Following Rule #2 (SRP), Rule #4 (Facade Pattern), Rule #11 (Functional Error Handling)
 *
 * This refactored version delegates responsibilities to:
 * - AlphaVantageHttpClient: HTTP operations and URL building
 * - AlphaVantageResponseParser: JSON parsing and data transformation
 * - AlphaVantageMetricsTracker: Performance metrics and health monitoring
 *
 * Retained responsibilities:
 * - Provider metadata and configuration (lightweight, interface-required)
 * - Subscription management (lifecycle-coupled with provider)
 * - Data retrieval orchestration (coordinates the three components)
 *
 * Benefits:
 * - Each component has ONE clear responsibility (SRP compliance)
 * - Easy to test each component independently
 * - Easy to swap implementations (e.g., different HTTP client)
 * - Clear separation of concerns
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (SRP Refactored)
 */
@Slf4j
@Component("alphaVantageProviderRefactored")
@RequiredArgsConstructor
public class AlphaVantageProviderRefactored implements MarketDataProvider {

    private static final String PROVIDER_ID = "alphavantage";
    private static final String PROVIDER_NAME = "Alpha Vantage";
    private static final String VERSION = "2.0.0";

    // Specialized components (Rule #2: Dependency Inversion)
    private final AlphaVantageHttpClient httpClient;
    private final AlphaVantageResponseParser responseParser;
    private final AlphaVantageMetricsTracker metricsTracker;

    // Provider state
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Consumer<MarketDataMessage>> subscriptions = new ConcurrentHashMap<>();
    private ProviderConfig config;
    private boolean connected = false;

    // Provider identification methods (interface-required)
    @Override
    public String getProviderId() { return PROVIDER_ID; }

    @Override
    public String getProviderName() { return PROVIDER_NAME; }

    @Override
    public String getVersion() { return VERSION; }

    @Override
    public Set<String> getSupportedExchanges() {
        return Set.of("NYSE", "NASDAQ", "FOREX", "CRYPTO");
    }

    @Override
    public Set<String> getSupportedDataTypes() {
        return Set.of("PRICE", "OHLCV", "HISTORICAL", "INTRADAY");
    }

    @Override
    public boolean supportsRealtime() { return true; }

    @Override
    public boolean supportsHistorical() { return true; }

    @Override
    public int getDailyRateLimit() {
        return isFreeTier() ? 25 : 500;
    }

    @Override
    public double getCostPerRequest() {
        return isFreeTier() ? 0.0 : 0.10;
    }

    @Override
    public boolean isFreeTier() {
        return config == null ||
               config.getApiKey() == null ||
               "demo".equals(config.getApiKey());
    }

    // Connection management (delegates to HTTP client)
    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() ->
            Optional.of(validateConfiguration())
                .filter(Boolean::booleanValue)
                .map(valid -> httpClient.testConnection(config.getApiKey())
                    .map(response -> {
                        this.connected = true;
                        log.info("Successfully connected to Alpha Vantage API");
                        return true;
                    })
                    .recover(e -> {
                        log.error("Failed to connect to Alpha Vantage API: {}", e.getMessage());
                        this.connected = false;
                        return false;
                    })
                    .getOrElse(false))
                .orElseGet(() -> {
                    log.error("Invalid configuration for Alpha Vantage provider");
                    return false;
                })
        );
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            this.connected = false;
            this.subscriptions.clear();
            log.info("Disconnected from Alpha Vantage API");
        });
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    // Subscription management (Observer Pattern)
    @Override
    public void subscribeToSymbol(String symbol, String exchange, Consumer<MarketDataMessage> callback) {
        Optional.of(isConnected())
            .filter(Boolean::booleanValue)
            .ifPresentOrElse(
                connected -> {
                    String subscriptionKey = createSubscriptionKey(symbol, exchange);
                    subscriptions.put(subscriptionKey, callback);

                    // Start polling for this symbol
                    scheduler.scheduleAtFixedRate(() -> {
                        getCurrentPrice(symbol, exchange)
                            .thenAccept(callback)
                            .exceptionally(throwable -> {
                                log.error("Error polling data for {}: {}", symbol, throwable.getMessage());
                                return null;
                            });
                    }, 0, 60, TimeUnit.SECONDS);

                    log.debug("Subscribed to {} on {}", symbol, exchange);
                },
                () -> {
                    throw new IllegalStateException("Provider not connected");
                }
            );
    }

    @Override
    public CompletableFuture<Void> unsubscribeFromSymbol(String symbol, String exchange) {
        return CompletableFuture.runAsync(() -> {
            String subscriptionKey = createSubscriptionKey(symbol, exchange);
            subscriptions.remove(subscriptionKey);
            log.debug("Unsubscribed from {} on {}", symbol, exchange);
        });
    }

    @Override
    public void subscribeToMultipleSymbols(Set<String> symbols, String exchange, Consumer<MarketDataMessage> callback) {
        symbols.forEach(symbol -> subscribeToSymbol(symbol, exchange, callback));
    }

    // Data retrieval methods (orchestrates HTTP client, parser, metrics)
    @Override
    @Async
    public CompletableFuture<List<MarketDataMessage>> getHistoricalData(
            String symbol, String exchange, LocalDateTime from, LocalDateTime to) {

        return CompletableFuture.supplyAsync(() ->
            Optional.of(isConnected())
                .filter(Boolean::booleanValue)
                .map(connected -> {
                    long startTime = System.currentTimeMillis();
                    metricsTracker.incrementRequestCount();

                    return Try.of(() -> {
                        String url = httpClient.buildHistoricalDataUrl(symbol, config.getApiKey());

                        // Extract response from Try - throws if failed
                        Try<ResponseEntity<Map>> responseTry = httpClient.executeRequest(url);
                        ResponseEntity<Map> response = responseTry.get();

                        // Parse response and extract result - throws if failed
                        Try<List<MarketDataMessage>> messagesTry = responseParser.parseHistoricalDataResponse(
                            response.getBody(), symbol, exchange);
                        List<MarketDataMessage> messages = messagesTry.get();

                        metricsTracker.recordSuccessfulRequest(startTime);
                        return messages;
                    })
                    .recover(e -> {
                        metricsTracker.recordFailedRequest();
                        log.error("Failed to get historical data for {}: {}", symbol, e.getMessage());
                        throw new RuntimeException("Failed to retrieve historical data", e);
                    })
                    .get();
                })
                .orElseThrow(() -> new IllegalStateException("Provider not connected"))
        );
    }

    @Override
    @Async
    public CompletableFuture<MarketDataMessage> getCurrentPrice(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() ->
            Optional.of(isConnected())
                .filter(Boolean::booleanValue)
                .map(connected -> {
                    long startTime = System.currentTimeMillis();
                    metricsTracker.incrementRequestCount();

                    return Try.of(() -> {
                        String url = httpClient.buildCurrentPriceUrl(symbol, config.getApiKey());

                        // Extract response from Try - throws if failed
                        Try<ResponseEntity<Map>> responseTry = httpClient.executeRequest(url);
                        ResponseEntity<Map> response = responseTry.get();

                        // Parse response and extract result - throws if failed
                        Try<MarketDataMessage> messageTry = responseParser.parseCurrentPriceResponse(
                            response.getBody(), symbol, exchange);
                        MarketDataMessage message = messageTry.get();

                        metricsTracker.recordSuccessfulRequest(startTime);
                        return message;
                    })
                    .recover(e -> {
                        metricsTracker.recordFailedRequest();
                        log.error("Failed to get current price for {}: {}", symbol, e.getMessage());
                        throw new RuntimeException("Failed to retrieve current price", e);
                    })
                    .get();
                })
                .orElseThrow(() -> new IllegalStateException("Provider not connected"))
        );
    }

    @Override
    public CompletableFuture<List<MarketDataMessage>> getCurrentPrices(Set<String> symbols, String exchange) {
        List<CompletableFuture<MarketDataMessage>> futures = symbols.stream()
            .map(symbol -> getCurrentPrice(symbol, exchange))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    // Metrics and monitoring (delegates to metrics tracker)
    @Override
    public CompletableFuture<ProviderMetrics> getMetrics() {
        return CompletableFuture.supplyAsync(() ->
            metricsTracker.getMetrics(isConnected(), isFreeTier(),
                getDailyRateLimit(), getCostPerRequest(), subscriptions.size()));
    }

    @Override
    public boolean isHealthy() {
        return metricsTracker.isHealthy(isConnected());
    }

    @Override
    public double getLatencyMs() {
        return metricsTracker.getLatencyMs();
    }

    @Override
    public double getSuccessRate() {
        return metricsTracker.getSuccessRate();
    }

    // Configuration management
    @Override
    public void configure(ProviderConfig config) {
        this.config = config;
    }

    @Override
    public boolean validateConfiguration() {
        return config != null &&
               config.getApiKey() != null &&
               !config.getApiKey().trim().isEmpty();
    }

    @Override
    public int getPriority() {
        return isFreeTier() ? 10 : 5;
    }

    @Override
    public ProviderType getProviderType() {
        return isFreeTier() ? ProviderType.FREE : ProviderType.PREMIUM;
    }

    // Helper methods
    private String createSubscriptionKey(String symbol, String exchange) {
        return symbol + ":" + exchange;
    }
}
