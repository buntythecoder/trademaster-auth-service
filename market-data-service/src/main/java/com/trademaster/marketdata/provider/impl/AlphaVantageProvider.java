package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.dto.ProviderMetrics;
import com.trademaster.marketdata.functional.Try;
import com.trademaster.marketdata.provider.MarketDataProvider;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
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
 * Alpha Vantage Market Data Provider Implementation with Circuit Breaker Protection
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
 * - Circuit Breaker: Resilience4j for fault tolerance (Rule #25)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0 (Circuit Breaker Enhanced)
 */
@Slf4j
@Component
public class AlphaVantageProvider implements MarketDataProvider {

    private static final String PROVIDER_ID = "alphavantage";
    private static final String PROVIDER_NAME = "Alpha Vantage";
    private static final String VERSION = "1.0.0";
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    private final RestTemplate restTemplate;
    private final CircuitBreakerService circuitBreakerService;
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

    // Dependency Injection following DIP + Circuit Breaker
    public AlphaVantageProvider(RestTemplateBuilder restTemplateBuilder,
                                CircuitBreakerService circuitBreakerService) {
        // Modern Spring Boot 3.5+ timeout configuration (non-deprecated)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restTemplate = restTemplateBuilder
            .requestFactory(() -> factory)
            .build();
        this.circuitBreakerService = circuitBreakerService;
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
    // Rule #11: Functional error handling with Try monad
    // Rule #3: Pattern matching instead of if-else
    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() ->
            Optional.of(validateConfiguration())
                .filter(Boolean::booleanValue)
                .map(valid -> Try.of(() -> {
                    testConnection();
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
    // Rule #11: Functional error handling with Try monad
    // Rule #3: Pattern matching instead of if-else
    // Rule #25: Circuit breaker on all external API calls
    @Override
    @Async
    public CompletableFuture<List<MarketDataMessage>> getHistoricalData(
            String symbol, String exchange, LocalDateTime from, LocalDateTime to) {

        return CompletableFuture.supplyAsync(() ->
            Optional.of(isConnected())
                .filter(Boolean::booleanValue)
                .map(connected -> {
                    long startTime = System.currentTimeMillis();
                    requestCount.incrementAndGet();
                    lastRequestTime = LocalDateTime.now();

                    return Try.of(() -> {
                        String url = buildHistoricalDataUrl(symbol);

                        // Wrap with circuit breaker for resilience
                        ResponseEntity<Map> response = circuitBreakerService.executeAlphaVantageCall(
                            () -> restTemplate.getForEntity(url, Map.class)
                        ).join();

                        return Optional.ofNullable(response)
                            .filter(r -> r.getStatusCode() == HttpStatus.OK && r.getBody() != null)
                            .map(r -> {
                                List<MarketDataMessage> result = parseHistoricalDataResponse(r.getBody(), symbol, exchange);
                                recordSuccessfulRequest(startTime);
                                return result;
                            })
                            .orElseThrow(() -> new RuntimeException("Invalid response from Alpha Vantage API"));
                    })
                    .recover(e -> {
                        recordFailedRequest();
                        log.error("Failed to get historical data for {}: {}", symbol, e.getMessage());
                        throw new RuntimeException("Failed to retrieve historical data", e);
                    })
                    .get();
                })
                .orElseThrow(() -> new IllegalStateException("Provider not connected"))
        );
    }

    // Rule #11: Functional error handling with Try monad
    // Rule #3: Pattern matching instead of if-else
    // Rule #25: Circuit breaker on all external API calls
    @Override
    @Async
    public CompletableFuture<MarketDataMessage> getCurrentPrice(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() ->
            Optional.of(isConnected())
                .filter(Boolean::booleanValue)
                .map(connected -> {
                    long startTime = System.currentTimeMillis();
                    requestCount.incrementAndGet();
                    lastRequestTime = LocalDateTime.now();

                    return Try.of(() -> {
                        String url = buildCurrentPriceUrl(symbol);

                        // Wrap with circuit breaker for resilience
                        ResponseEntity<Map> response = circuitBreakerService.executeAlphaVantageCall(
                            () -> restTemplate.getForEntity(url, Map.class)
                        ).join();

                        return Optional.ofNullable(response)
                            .filter(r -> r.getStatusCode() == HttpStatus.OK && r.getBody() != null)
                            .map(r -> {
                                MarketDataMessage result = parseCurrentPriceResponse(r.getBody(), symbol, exchange);
                                recordSuccessfulRequest(startTime);
                                return result;
                            })
                            .orElseThrow(() -> new RuntimeException("Invalid response from Alpha Vantage API"));
                    })
                    .recover(e -> {
                        recordFailedRequest();
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
    // Rule #25: Circuit breaker on all external API calls
    private void testConnection() {
        String testUrl = UriComponentsBuilder.fromUriString(BASE_URL)
            .queryParam("function", "GLOBAL_QUOTE")
            .queryParam("symbol", "IBM")
            .queryParam("apikey", config.getApiKey())
            .toUriString();

        // Wrap with circuit breaker for resilience
        ResponseEntity<Map> response = circuitBreakerService.executeAlphaVantageCall(
            () -> restTemplate.getForEntity(testUrl, Map.class)
        ).join(); // Block for connection test

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Connection test failed");
        }
    }

    private String buildHistoricalDataUrl(String symbol) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
            .queryParam("function", "TIME_SERIES_DAILY")
            .queryParam("symbol", symbol)
            .queryParam("apikey", config.getApiKey())
            .queryParam("outputsize", "full")
            .toUriString();
    }

    private String buildCurrentPriceUrl(String symbol) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
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
    // Rule #11: Functional error handling with Try monad
    // Rule #13: Stream API instead of loops
    // Rule #3: Pattern matching instead of if-else
    private List<MarketDataMessage> parseHistoricalDataResponse(Map<String, Object> response, String symbol, String exchange) {
        return Try.of(() -> {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> timeSeries =
                (Map<String, Map<String, String>>) response.get("Time Series (Daily)");

            return Optional.ofNullable(timeSeries)
                .map(series -> series.entrySet().stream()
                    .map(entry -> Try.of(() -> MarketDataMessage.builder()
                        .symbol(symbol)
                        .exchange(exchange)
                        .timestamp(LocalDateTime.parse(entry.getKey() + "T16:00:00"))
                        .price(new BigDecimal(entry.getValue().get("4. close")))
                        .open(new BigDecimal(entry.getValue().get("1. open")))
                        .high(new BigDecimal(entry.getValue().get("2. high")))
                        .low(new BigDecimal(entry.getValue().get("3. low")))
                        .volume(Long.parseLong(entry.getValue().get("5. volume")))
                        .type(MarketDataMessage.MarketDataType.OHLC)
                        .build())
                    .recover(e -> {
                        log.warn("Failed to parse entry for {}: {}", entry.getKey(), e.getMessage());
                        return null;
                    })
                    .toOptional()
                    .orElse(null))
                    .filter(Objects::nonNull)
                    .toList())
                .orElse(List.of());
        })
        .recover(e -> {
            log.error("Error parsing historical data response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse historical data", e);
        })
        .get();
    }

    // Rule #11: Functional error handling with Try monad
    // Rule #3: Pattern matching instead of if-else
    private MarketDataMessage parseCurrentPriceResponse(Map<String, Object> response, String symbol, String exchange) {
        return Try.of(() -> {
            @SuppressWarnings("unchecked")
            Map<String, String> quote = (Map<String, String>) response.get("Global Quote");

            return Optional.ofNullable(quote)
                .map(q -> MarketDataMessage.builder()
                    .symbol(symbol)
                    .exchange(exchange)
                    .timestamp(LocalDateTime.now())
                    .price(new BigDecimal(q.get("05. price")))
                    .change(new BigDecimal(q.get("09. change")))
                    .changePercent(new BigDecimal(q.get("10. change percent").replace("%", "")))
                    .volume(Long.parseLong(q.get("06. volume")))
                    .type(MarketDataMessage.MarketDataType.TICK)
                    .build())
                .orElseThrow(() -> new RuntimeException("Unable to parse price data from Alpha Vantage response"));
        })
        .recover(e -> {
            log.error("Error parsing current price response: {}", e.getMessage());
            throw new RuntimeException("Unable to parse price data from Alpha Vantage response", e);
        })
        .get();
    }
}