package com.trademaster.marketdata.provider.impl;

import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.dto.ProviderMetrics;
import com.trademaster.marketdata.dto.MarketDataRequest;
import com.trademaster.marketdata.dto.ValidationResult;
import com.trademaster.marketdata.dto.Valid;
import com.trademaster.marketdata.dto.Invalid;
import com.trademaster.marketdata.provider.MarketDataProvider;
import com.trademaster.marketdata.config.AlphaVantageProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Functional Alpha Vantage Provider - Java 24 Architecture
 * 
 * Advanced Design Patterns Applied:
 * - Strategy Pattern: URL builders, response parsers, pricing strategies
 * - Command Pattern: Connection operations, data retrieval commands
 * - Chain of Responsibility: Validation chain
 * - Observer Pattern: Subscription management with functional callbacks
 * - Template Method: Request processing pipeline
 * - Factory Pattern: Result type creation
 * 
 * Architectural Principles:
 * - Virtual Threads: All async operations use virtual threads for scalable concurrency
 * - Railway Oriented Programming: Result types for error handling
 * - Lock-free Concurrency: Atomic operations and concurrent collections
 * - Functional Programming: Higher-order functions, immutable data (where architecturally beneficial)
 * - Structured Concurrency: Coordinated task execution with proper lifecycle management
 * - SOLID Principles: Single responsibility, dependency inversion
 * - Architectural Fitness: Functional patterns applied where they improve maintainability
 * 
 * @author TradeMaster Development Team
 * @version 3.0.0 - Architectural Excellence with Virtual Threads
 */
@Slf4j
@Component
public class FunctionalAlphaVantageProvider implements MarketDataProvider {

    private static final String PROVIDER_ID = "alphavantage";
    private static final String PROVIDER_NAME = "Alpha Vantage";
    private static final String VERSION = "3.0.0-virtual-threads";
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final Set<String> SUPPORTED_EXCHANGES = Set.of("NYSE", "NASDAQ", "FOREX", "CRYPTO");
    private static final Set<String> SUPPORTED_DATA_TYPES = Set.of("PRICE", "OHLCV", "HISTORICAL", "INTRADAY");
    
    // Virtual Thread Executor for all async operations
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler;
    
    // Lock-free state management with atomic references
    private final AtomicReference<MarketDataProvider.ProviderConfig> config = new AtomicReference<>();
    private final AtomicReference<Boolean> connected = new AtomicReference<>(false);
    private final ConcurrentHashMap<String, Consumer<MarketDataMessage>> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    // Metrics with atomic counters for lock-free updates
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastRequestTime = new AtomicReference<>();
    private final AtomicReference<Double> lastLatencyMs = new AtomicReference<>(0.0);
    
    // Strategy Pattern: URL Builders (Functional approach)
    private final Map<String, Function<String, String>> urlBuilders;
    private final Map<String, Function<Map<String, Object>, List<MarketDataMessage>>> responseParsers;
    private final Function<MarketDataRequest, ValidationResult> validationChain;
    private final Function<MarketDataProvider.ProviderConfig, Boolean> configValidator;
    private final Function<String, Boolean> exchangeValidator;

    // Railway Oriented Programming: Result type
    public sealed interface Result<T, E> permits Success, Failure {
        record Success<T, E>(T value) implements Result<T, E> {}
        record Failure<T, E>(E error) implements Result<T, E> {}
        
        static <T, E> Result<T, E> success(T value) { return new Success<>(value); }
        static <T, E> Result<T, E> failure(E error) { return new Failure<>(error); }
        
        default <U> Result<U, E> map(Function<T, U> mapper) {
            return switch (this) {
                case Success<T, E>(var value) -> success(mapper.apply(value));
                case Failure<T, E>(var error) -> failure(error);
            };
        }
        
        default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return switch (this) {
                case Success<T, E>(var value) -> mapper.apply(value);
                case Failure<T, E>(var error) -> failure(error);
            };
        }
        
        default T orElse(T defaultValue) {
            return switch (this) {
                case Success<T, E>(var value) -> value;
                case Failure<T, E>(var ignored) -> defaultValue;
            };
        }
        
        default boolean isSuccess() {
            return this instanceof Success;
        }
    }

    // Strategy Pattern - URL Building Strategies
    public enum UrlBuildingStrategy {
        HISTORICAL_DATA((symbol, apiKey) -> BASE_URL + "?function=TIME_SERIES_DAILY&symbol=" + symbol + "&apikey=" + apiKey),
        CURRENT_PRICE((symbol, apiKey) -> BASE_URL + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey),
        CONNECTION_TEST((symbol, apiKey) -> BASE_URL + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey);
        
        private final Function<String, Function<String, String>> urlBuilder;
        
        UrlBuildingStrategy(Function<String, Function<String, String>> urlBuilder) {
            this.urlBuilder = urlBuilder;
        }
        
        public String buildUrl(String symbol, String apiKey) {
            return urlBuilder.apply(symbol).apply(apiKey);
        }
    }

    // Constructor with Dependency Injection
    @Autowired
    public FunctionalAlphaVantageProvider(RestTemplateBuilder restTemplateBuilder, 
                                          AlphaVantageProviderConfig providerConfig) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(providerConfig.getTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(providerConfig.getTimeoutMs()))
            .build();
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.config.set(providerConfig);
        
        // Initialize functional components
        this.urlBuilders = initializeUrlBuilders();
        this.responseParsers = initializeResponseParsers();
        this.validationChain = initializeValidationChain();
        this.configValidator = initializeConfigValidator();
        this.exchangeValidator = initializeExchangeValidator();
    }

    // Functional component initialization methods
    private Map<String, Function<String, String>> initializeUrlBuilders() {
        return Map.of(
            "historical", symbol -> UrlBuildingStrategy.HISTORICAL_DATA.buildUrl(symbol, config.get().getApiKey()),
            "current", symbol -> UrlBuildingStrategy.CURRENT_PRICE.buildUrl(symbol, config.get().getApiKey()),
            "test", symbol -> UrlBuildingStrategy.CONNECTION_TEST.buildUrl(symbol, config.get().getApiKey())
        );
    }

    private Map<String, Function<Map<String, Object>, List<MarketDataMessage>>> initializeResponseParsers() {
        return Map.of(
            "historical", this::parseHistoricalDataFunctional,
            "current", response -> List.of(parseCurrentPriceFunctional(response))
        );
    }

    private Function<MarketDataRequest, ValidationResult> initializeValidationChain() {
        return request -> validateSymbol(request)
            .flatMap(r -> validateExchange(r))
            .flatMap(r -> validateTimeRange(r));
    }

    private Function<MarketDataProvider.ProviderConfig, Boolean> initializeConfigValidator() {
        return config -> Optional.ofNullable(config)
            .map(ProviderConfig::getApiKey)
            .filter(key -> !key.isBlank())
            .isPresent();
    }

    private Function<String, Boolean> initializeExchangeValidator() {
        return exchange -> getSupportedExchanges().contains(exchange);
    }

    // Validation methods (Chain of Responsibility pattern)
    private Result<MarketDataRequest, String> validateSymbol(MarketDataRequest request) {
        return Optional.ofNullable(request.symbol())
            .filter(s -> !s.isBlank())
            .filter(s -> s.length() <= 10)
            .filter(s -> s.matches("[A-Z]+"))
            .map(s -> Result.<MarketDataRequest, String>success(request))
            .orElse(Result.failure("Invalid symbol: " + request.symbol()));
    }

    private Result<MarketDataRequest, String> validateExchange(MarketDataRequest request) {
        return exchangeValidator.apply(request.exchange()) 
            ? Result.success(request)
            : Result.failure("Unsupported exchange: " + request.exchange());
    }

    private Result<MarketDataRequest, String> validateTimeRange(MarketDataRequest request) {
        return Optional.of(request)
            .filter(r -> r.from().isBefore(r.to()))
            .map(Result::<MarketDataRequest, String>success)
            .orElse(Result.failure("Invalid time range"));
    }

    // Provider identification methods
    @Override public String getProviderId() { return PROVIDER_ID; }
    @Override public String getProviderName() { return PROVIDER_NAME; }
    @Override public String getVersion() { return VERSION; }
    @Override public Set<String> getSupportedExchanges() { return SUPPORTED_EXCHANGES; }
    @Override public Set<String> getSupportedDataTypes() { return SUPPORTED_DATA_TYPES; }
    @Override public boolean supportsRealtime() { return true; }
    @Override public boolean supportsHistorical() { return true; }

    // Cost and rate limiting
    @Override
    public int getDailyRateLimit() {
        return Optional.ofNullable(config.get())
            .map(ProviderConfig::getApiKey)
            .filter(key -> !"demo".equals(key))
            .map(key -> 500) // Premium
            .orElse(25); // Free tier
    }

    @Override
    public double getCostPerRequest() {
        return isFreeTier() ? 0.0 : 0.10;
    }

    @Override
    public boolean isFreeTier() {
        return Optional.ofNullable(config.get())
            .map(ProviderConfig::getApiKey)
            .filter(key -> !key.equals("demo") && !key.isBlank())
            .isEmpty();
    }

    // Connection management
    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            return Optional.ofNullable(config.get())
                .filter(configValidator)
                .map(validConfig -> {
                    try {
                        String testUrl = UrlBuildingStrategy.CONNECTION_TEST.buildUrl("IBM", validConfig.getApiKey());
                        ResponseEntity<Map> response = restTemplate.getForEntity(testUrl, Map.class);
                        boolean success = response.getStatusCode() == HttpStatus.OK;
                        connected.set(success);
                        
                        if (success) {
                            log.info("Successfully connected to Alpha Vantage API");
                        } else {
                            log.error("Failed to connect to Alpha Vantage API");
                        }
                        return success;
                    } catch (Exception e) {
                        connected.set(false);
                        log.error("Connection failed: {}", e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
        }, virtualExecutor);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            connected.set(false);
            subscriptions.clear();
            scheduledTasks.values().forEach(future -> future.cancel(false));
            scheduledTasks.clear();
            log.info("Disconnected from Alpha Vantage API");
        }, virtualExecutor);
    }

    @Override
    public boolean isConnected() { 
        return connected.get(); 
    }

    // Subscription management
    @Override
    public void subscribeToSymbol(String symbol, String exchange, Consumer<MarketDataMessage> callback) {
        if (!connected.get()) {
            throw new IllegalStateException("Provider not connected");
        }
        
        Optional.of(symbol)
            .filter(s -> !s.isBlank() && SUPPORTED_EXCHANGES.contains(exchange))
            .ifPresentOrElse(
                validSymbol -> {
                    String subscriptionKey = createSubscriptionKey(validSymbol, exchange);
                    subscriptions.put(subscriptionKey, callback);
                    
                    ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> {
                        getCurrentPrice(validSymbol, exchange)
                            .thenAccept(callback)
                            .exceptionally(throwable -> {
                                log.error("Error polling data for {}: {}", validSymbol, throwable.getMessage());
                                return null;
                            });
                    }, 0, 60, TimeUnit.SECONDS);
                    
                    scheduledTasks.put(subscriptionKey, scheduledTask);
                    log.debug("Subscribed to {} on {}", validSymbol, exchange);
                },
                () -> { throw new IllegalArgumentException("Invalid symbol or exchange"); }
            );
    }

    @Override
    public CompletableFuture<Void> unsubscribeFromSymbol(String symbol, String exchange) {
        return CompletableFuture.runAsync(() -> {
            String subscriptionKey = createSubscriptionKey(symbol, exchange);
            subscriptions.remove(subscriptionKey);
            Optional.ofNullable(scheduledTasks.remove(subscriptionKey))
                .ifPresent(future -> future.cancel(false));
            log.debug("Unsubscribed from {} on {}", symbol, exchange);
        }, virtualExecutor);
    }

    @Override
    public void subscribeToMultipleSymbols(Set<String> symbols, String exchange, Consumer<MarketDataMessage> callback) {
        CompletableFuture.runAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                symbols.stream()
                    .map(symbol -> scope.fork(() -> {
                        subscribeToSymbol(symbol, exchange, callback);
                        return symbol;
                    }))
                    .toList();
                
                scope.join();
                scope.throwIfFailed();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted during multiple subscription setup");
            } catch (Exception e) {
                log.error("Failed to set up multiple subscriptions: {}", e.getMessage());
            }
        }, virtualExecutor);
    }

    // Data retrieval methods
    @Override
    @Async
    public CompletableFuture<List<MarketDataMessage>> getHistoricalData(
            String symbol, String exchange, LocalDateTime from, LocalDateTime to) {
        
        return CompletableFuture.supplyAsync(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Provider not connected");
            }
            
            long startTime = System.currentTimeMillis();
            requestCount.incrementAndGet();
            lastRequestTime.set(LocalDateTime.now());
            
            try {
                String url = UrlBuildingStrategy.HISTORICAL_DATA.buildUrl(symbol, config.get().getApiKey());
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                
                return Optional.ofNullable(response.getBody())
                    .filter(body -> response.getStatusCode() == HttpStatus.OK)
                    .map(body -> parseHistoricalDataResponse(body, symbol, exchange))
                    .map(data -> {
                        recordSuccessfulRequest(startTime);
                        return data;
                    })
                    .orElseThrow(() -> new RuntimeException("Invalid response from Alpha Vantage API"));
                    
            } catch (Exception e) {
                recordFailedRequest();
                log.error("Failed to get historical data for {}: {}", symbol, e.getMessage());
                throw new RuntimeException("Failed to retrieve historical data", e);
            }
        }, virtualExecutor);
    }

    @Override
    @Async
    public CompletableFuture<MarketDataMessage> getCurrentPrice(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Provider not connected");
            }
            
            long startTime = System.currentTimeMillis();
            requestCount.incrementAndGet();
            lastRequestTime.set(LocalDateTime.now());
            
            try {
                String url = UrlBuildingStrategy.CURRENT_PRICE.buildUrl(symbol, config.get().getApiKey());
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                
                return Optional.ofNullable(response.getBody())
                    .filter(body -> response.getStatusCode() == HttpStatus.OK)
                    .map(body -> parseCurrentPriceResponse(body, symbol, exchange))
                    .map(data -> {
                        recordSuccessfulRequest(startTime);
                        return data;
                    })
                    .orElseThrow(() -> new RuntimeException("Invalid response from Alpha Vantage API"));
                    
            } catch (Exception e) {
                recordFailedRequest();
                log.error("Failed to get current price for {}: {}", symbol, e.getMessage());
                throw new RuntimeException("Failed to retrieve current price", e);
            }
        }, virtualExecutor);
    }

    @Override
    public CompletableFuture<List<MarketDataMessage>> getCurrentPrices(Set<String> symbols, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                List<StructuredTaskScope.Subtask<MarketDataMessage>> tasks = symbols.stream()
                    .map(symbol -> scope.fork(() -> getCurrentPrice(symbol, exchange).join()))
                    .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                return tasks.stream()
                    .map(StructuredTaskScope.Subtask::resultNow)
                    .toList();
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while fetching current prices");
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch current prices", e);
            }
        }, virtualExecutor);
    }

    // Health and monitoring
    @Override
    public CompletableFuture<ProviderMetrics> getMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            return ProviderMetrics.builder()
                .providerId(PROVIDER_ID)
                .totalRequests(requestCount.get())
                .successfulRequests(successCount.get())
                .failedRequests(failureCount.get())
                .successRate(getSuccessRate())
                .averageLatencyMs(lastLatencyMs.get())
                .lastRequestTime(lastRequestTime.get())
                .isHealthy(isHealthy())
                .dailyRateLimit(getDailyRateLimit())
                .costPerRequest(getCostPerRequest())
                .build();
        }, virtualExecutor);
    }

    @Override
    public boolean isHealthy() {
        return connected.get() && getSuccessRate() > 0.8;
    }

    @Override
    public double getLatencyMs() {
        return lastLatencyMs.get();
    }

    @Override
    public double getSuccessRate() {
        long total = requestCount.get();
        return total == 0 ? 1.0 : (double) successCount.get() / total;
    }

    // Configuration methods
    @Override
    public void configure(ProviderConfig config) {
        this.config.set(config);
    }

    @Override
    public boolean validateConfiguration() {
        return configValidator.apply(config.get());
    }

    @Override
    public int getPriority() {
        return isFreeTier() ? 3 : 1; // Premium providers get higher priority
    }

    @Override
    public ProviderType getProviderType() {
        return isFreeTier() ? ProviderType.FREE : ProviderType.PREMIUM;
    }

    // Private helper methods
    private String createSubscriptionKey(String symbol, String exchange) {
        return symbol + ":" + exchange;
    }

    private void recordSuccessfulRequest(long startTime) {
        successCount.incrementAndGet();
        lastLatencyMs.set((double) (System.currentTimeMillis() - startTime));
    }

    private void recordFailedRequest() {
        failureCount.incrementAndGet();
    }

    // Response parsing methods (Strategy Pattern)
    private List<MarketDataMessage> parseHistoricalDataFunctional(Map<String, Object> response) {
        return Optional.ofNullable(response.get("Time Series (Daily)"))
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(timeSeries -> timeSeries.entrySet().stream()
                .map(entry -> parseTimeSeriesEntry(entry.getKey(), (Map<String, Object>) entry.getValue()))
                .toList())
            .orElse(Collections.emptyList());
    }

    private MarketDataMessage parseCurrentPriceFunctional(Map<String, Object> response) {
        return Optional.ofNullable(response.get("Global Quote"))
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(this::parseGlobalQuote)
            .orElse(createDefaultMarketDataMessage());
    }

    private List<MarketDataMessage> parseHistoricalDataResponse(Map<String, Object> body, String symbol, String exchange) {
        return parseHistoricalDataFunctional(body);
    }

    private MarketDataMessage parseCurrentPriceResponse(Map<String, Object> body, String symbol, String exchange) {
        return parseCurrentPriceFunctional(body);
    }

    private MarketDataMessage parseTimeSeriesEntry(String date, Map<String, Object> data) {
        return MarketDataMessage.builder()
            .symbol(extractSymbolFromResponse())
            .exchange("NYSE") // Default
            .timestamp(parseTimestamp(date))
            .price(parsePrice(data.get("4. close")))
            .volume(parseLong(data.get("5. volume")))
            .high(parsePrice(data.get("2. high")))
            .low(parsePrice(data.get("3. low")))
            .open(parsePrice(data.get("1. open")))
            .build();
    }

    private MarketDataMessage parseGlobalQuote(Map<String, Object> quote) {
        return MarketDataMessage.builder()
            .symbol((String) quote.get("01. symbol"))
            .exchange("NYSE") // Default
            .timestamp(LocalDateTime.now())
            .price(parsePrice(quote.get("05. price")))
            .volume(parseLong(quote.get("06. volume")))
            .high(parsePrice(quote.get("03. high")))
            .low(parsePrice(quote.get("04. low")))
            .open(parsePrice(quote.get("02. open")))
            .build();
    }

    private MarketDataMessage createDefaultMarketDataMessage() {
        return MarketDataMessage.builder()
            .symbol("UNKNOWN")
            .exchange("UNKNOWN")
            .timestamp(LocalDateTime.now())
            .price(BigDecimal.ZERO)
            .build();
    }

    private String extractSymbolFromResponse() {
        return "UNKNOWN"; // Would need to extract from response context
    }

    private LocalDateTime parseTimestamp(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr + "T00:00:00");
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private BigDecimal parsePrice(Object price) {
        try {
            return new BigDecimal(price.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Long parseLong(Object value) {
        try {
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}