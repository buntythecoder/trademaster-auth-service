package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.dto.ProviderMetrics;
import com.trademaster.marketdata.provider.MarketDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Alpha Vantage Market Data Provider Implementation
 * 
 * Non-reactive implementation following SOLID principles:
 * - Single Responsibility: Handles only Alpha Vantage API integration
 * - Open/Closed: Extensible through configuration, closed for modification
 * - Liskov Substitution: Fully substitutable MarketDataProvider implementation
 * - Interface Segregation: Implements only required MarketDataProvider methods
 * - Dependency Inversion: Depends on RestTemplate abstraction, not concrete HTTP client
 * 
 * Design Patterns Applied:
 * - Template Method: Base provider behavior with specific implementations
 * - Strategy Pattern: Configurable request/response handling
 * - Observer Pattern: Callback-based subscription mechanism
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AlphaVantageProvider implements MarketDataProvider {

    private static final String PROVIDER_ID = "alphavantage";
    private static final String PROVIDER_NAME = "Alpha Vantage";
    private static final String VERSION = "1.0.0";
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Consumer<MarketDataMessage>> subscriptions;
    
    // Metrics tracking (Observer Pattern)
    private final AtomicLong requestCount;
    private final AtomicLong successCount;
    private final AtomicLong failureCount;
    
    private ProviderConfig config;
    private boolean connected;
    private LocalDateTime lastRequestTime;
    private double lastLatencyMs;

    // Dependency Injection following DIP
    public AlphaVantageProvider(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(60))
            .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.subscriptions = new ConcurrentHashMap<>();
        this.requestCount = new AtomicLong(0);
        this.successCount = new AtomicLong(0);
        this.failureCount = new AtomicLong(0);
        this.connected = false;
    }

    // Provider identification methods
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
        return isFreeTier() ? 25 : 500; // Free: 25, Premium: 500
    }

    @Override
    public double getCostPerRequest() {
        return isFreeTier() ? 0.0 : 0.10; // $0.10 per request for premium
    }

    @Override
    public boolean isFreeTier() {
        return config == null || 
               config.getApiKey() == null || 
               "demo".equals(config.getApiKey());
    }

    // Connection management (Template Method Pattern)
    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            if (!validateConfiguration()) {
                log.error("Invalid configuration for Alpha Vantage provider");
                return false;
            }
            
            try {
                // Test connection with a simple API call
                testConnection();
                this.connected = true;
                log.info("Successfully connected to Alpha Vantage API");
                return true;
            } catch (Exception e) {
                log.error("Failed to connect to Alpha Vantage API: {}", e.getMessage());
                this.connected = false;
                return false;
            }
        });
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
        if (!isConnected()) {
            throw new IllegalStateException("Provider not connected");
        }
        
        String subscriptionKey = createSubscriptionKey(symbol, exchange);
        subscriptions.put(subscriptionKey, callback);
        
        // Start polling for this symbol (Alpha Vantage doesn't support real-time streaming)
        scheduler.scheduleAtFixedRate(() -> {
            getCurrentPrice(symbol, exchange)
                .thenAccept(callback)
                .exceptionally(throwable -> {
                    log.error("Error polling data for {}: {}", symbol, throwable.getMessage());
                    return null;
                });
        }, 0, 60, TimeUnit.SECONDS); // Poll every minute
        
        log.debug("Subscribed to {} on {}", symbol, exchange);
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

    // Data retrieval methods (Strategy Pattern for different data types)
    @Override
    @Async
    public CompletableFuture<List<MarketDataMessage>> getHistoricalData(
            String symbol, String exchange, LocalDateTime from, LocalDateTime to) {
        
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new IllegalStateException("Provider not connected");
            }

            long startTime = System.currentTimeMillis();
            requestCount.incrementAndGet();
            lastRequestTime = LocalDateTime.now();

            try {
                String url = buildHistoricalDataUrl(symbol);
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<MarketDataMessage> result = parseHistoricalDataResponse(response.getBody(), symbol, exchange);
                    recordSuccessfulRequest(startTime);
                    return result;
                } else {
                    throw new RuntimeException("Invalid response from Alpha Vantage API");
                }
            } catch (Exception e) {
                recordFailedRequest();
                log.error("Failed to get historical data for {}: {}", symbol, e.getMessage());
                throw new RuntimeException("Failed to retrieve historical data", e);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<MarketDataMessage> getCurrentPrice(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new IllegalStateException("Provider not connected");
            }

            long startTime = System.currentTimeMillis();
            requestCount.incrementAndGet();
            lastRequestTime = LocalDateTime.now();

            try {
                String url = buildCurrentPriceUrl(symbol);
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    MarketDataMessage result = parseCurrentPriceResponse(response.getBody(), symbol, exchange);
                    recordSuccessfulRequest(startTime);
                    return result;
                } else {
                    throw new RuntimeException("Invalid response from Alpha Vantage API");
                }
            } catch (Exception e) {
                recordFailedRequest();
                log.error("Failed to get current price for {}: {}", symbol, e.getMessage());
                throw new RuntimeException("Failed to retrieve current price", e);
            }
        });
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

    // Metrics and monitoring
    @Override
    public CompletableFuture<ProviderMetrics> getMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            long total = requestCount.get();
            long success = successCount.get();
            long failure = failureCount.get();
            
            return ProviderMetrics.builder()
                .providerId(PROVIDER_ID)
                .providerName(PROVIDER_NAME)
                .timestamp(LocalDateTime.now())
                .avgLatencyMs(lastLatencyMs)
                .minLatencyMs(lastLatencyMs)
                .maxLatencyMs(lastLatencyMs)
                .totalRequests(total)
                .successfulRequests(success)
                .failedRequests(failure)
                .successRate(total > 0 ? (success * 100.0) / total : 0.0)
                .errorRate(total > 0 ? (failure * 100.0) / total : 0.0)
                .dailyRequestsUsed(total)
                .dailyRequestsLimit(getDailyRateLimit())
                .dailyUsagePercentage(total > 0 ? (total * 100.0) / getDailyRateLimit() : 0.0)
                .costPerRequest(getCostPerRequest())
                .dailyCost(total * getCostPerRequest())
                .isFreeTier(isFreeTier())
                .isConnected(isConnected())
                .isHealthy(isHealthy())
                .lastSuccessfulRequest(lastRequestTime)
                .subscribedSymbols(subscriptions.size())
                .build();
        });
    }

    @Override
    public boolean isHealthy() {
        return isConnected() && 
               (requestCount.get() == 0 || 
                (successCount.get() * 100.0) / requestCount.get() >= 80.0);
    }

    @Override
    public double getLatencyMs() { return lastLatencyMs; }

    @Override
    public double getSuccessRate() {
        long total = requestCount.get();
        return total > 0 ? (successCount.get() * 100.0) / total : 0.0;
    }

    // Configuration management
    @Override
    public void configure(ProviderConfig config) { this.config = config; }

    @Override
    public boolean validateConfiguration() {
        return config != null && 
               config.getApiKey() != null && 
               !config.getApiKey().trim().isEmpty();
    }

    @Override
    public int getPriority() { return isFreeTier() ? 10 : 5; }

    @Override
    public ProviderType getProviderType() {
        return isFreeTier() ? ProviderType.FREE : ProviderType.PREMIUM;
    }

    // Private helper methods (Template Method Pattern)
    private void testConnection() {
        String testUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL)
            .queryParam("function", "GLOBAL_QUOTE")
            .queryParam("symbol", "IBM")
            .queryParam("apikey", config.getApiKey())
            .toUriString();
        
        ResponseEntity<Map> response = restTemplate.getForEntity(testUrl, Map.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Connection test failed");
        }
    }

    private String buildHistoricalDataUrl(String symbol) {
        return UriComponentsBuilder.fromHttpUrl(BASE_URL)
            .queryParam("function", "TIME_SERIES_DAILY")
            .queryParam("symbol", symbol)
            .queryParam("apikey", config.getApiKey())
            .queryParam("outputsize", "full")
            .toUriString();
    }

    private String buildCurrentPriceUrl(String symbol) {
        return UriComponentsBuilder.fromHttpUrl(BASE_URL)
            .queryParam("function", "GLOBAL_QUOTE")
            .queryParam("symbol", symbol)
            .queryParam("apikey", config.getApiKey())
            .toUriString();
    }

    private String createSubscriptionKey(String symbol, String exchange) {
        return symbol + ":" + exchange;
    }

    private void recordSuccessfulRequest(long startTime) {
        successCount.incrementAndGet();
        lastLatencyMs = System.currentTimeMillis() - startTime;
    }

    private void recordFailedRequest() {
        failureCount.incrementAndGet();
    }

    // Response parsing methods (Strategy Pattern)
    private List<MarketDataMessage> parseHistoricalDataResponse(Map<String, Object> response, String symbol, String exchange) {
        List<MarketDataMessage> messages = new ArrayList<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> timeSeries = 
                (Map<String, Map<String, String>>) response.get("Time Series (Daily)");
            
            if (timeSeries != null) {
                for (Map.Entry<String, Map<String, String>> entry : timeSeries.entrySet()) {
                    String date = entry.getKey();
                    Map<String, String> data = entry.getValue();
                    
                    MarketDataMessage message = MarketDataMessage.builder()
                        .symbol(symbol)
                        .exchange(exchange)
                        .timestamp(LocalDateTime.parse(date + "T16:00:00"))
                        .price(Double.parseDouble(data.get("4. close")))
                        .open(Double.parseDouble(data.get("1. open")))
                        .high(Double.parseDouble(data.get("2. high")))
                        .low(Double.parseDouble(data.get("3. low")))
                        .volume(Long.parseLong(data.get("5. volume")))
                        .dataType(MarketDataMessage.DataType.OHLCV)
                        .source(PROVIDER_NAME)
                        .build();
                    
                    messages.add(message);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing historical data response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse historical data", e);
        }
        
        return messages;
    }

    private MarketDataMessage parseCurrentPriceResponse(Map<String, Object> response, String symbol, String exchange) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> quote = (Map<String, String>) response.get("Global Quote");
            
            if (quote != null) {
                return MarketDataMessage.builder()
                    .symbol(symbol)
                    .exchange(exchange)
                    .timestamp(LocalDateTime.now())
                    .price(Double.parseDouble(quote.get("05. price")))
                    .change(Double.parseDouble(quote.get("09. change")))
                    .changePercent(Double.parseDouble(quote.get("10. change percent").replace("%", "")))
                    .volume(Long.parseLong(quote.get("06. volume")))
                    .dataType(MarketDataMessage.DataType.TICK)
                    .source(PROVIDER_NAME)
                    .build();
            }
        } catch (Exception e) {
            log.error("Error parsing current price response: {}", e.getMessage());
        }
        
        throw new RuntimeException("Unable to parse price data from Alpha Vantage response");
    }
}