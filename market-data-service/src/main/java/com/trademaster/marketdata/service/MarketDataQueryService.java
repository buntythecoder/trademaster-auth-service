package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.HistoricalDataResponse;
import com.trademaster.marketdata.dto.RealTimeDataResponse;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.repository.MarketDataRepository;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Market Data Query Service
 *
 * Handles all read/query operations for market data with circuit breaker protection.
 * Follows RULE #2 (Single Responsibility) - focused on query operations only.
 *
 * Features:
 * - Real-time price queries with caching
 * - Historical OHLC data retrieval
 * - Bulk price data operations
 * - Active symbols listing
 * - Circuit breaker protection (Rule #25)
 * - Virtual thread optimization (Rule #12)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataQueryService {

    private final MarketDataRepository marketDataRepository;
    private final MarketDataCacheService cacheService;
    private final CircuitBreakerService circuitBreakerService;

    /**
     * Get current price for a symbol with circuit breaker protection
     * RULE #5: Max 15 lines, RULE #25: Circuit breaker, RULE #11: Functional error handling
     */
    public CompletableFuture<Optional<MarketDataPoint>> getCurrentPrice(String symbol, String exchange) {
        return circuitBreakerService.executeRedisCacheOperationWithFallback(
                () -> cacheService.getCurrentPrice(symbol, exchange),
                () -> Optional.<MarketDataCacheService.CachedPrice>empty()
            )
            .thenCompose(cachedResult -> cachedResult
                .map(this::convertCachedPriceToDataPoint)
                .map(dataPoint -> CompletableFuture.completedFuture(Optional.of(dataPoint)))
                .orElseGet(() -> fetchPriceFromDatabase(symbol, exchange))
            )
            .exceptionally(ex -> {
                log.error("Failed to get current price for {}:{}: {}", symbol, exchange, ex.getMessage());
                return Optional.empty();
            });
    }

    /**
     * Get historical OHLC data with circuit breaker protection
     * RULE #5: Max 15 lines, RULE #25: Circuit breaker
     */
    public CompletableFuture<List<MarketDataPoint>> getHistoricalData(
            String symbol, String exchange, Instant from, Instant to, String interval) {
        return fetchOHLCFromCache(symbol, exchange, interval)
            .thenCompose(cachedData -> cachedData
                .map(cached -> {
                    log.debug("Retrieved OHLC data from cache for {}:{}", symbol, exchange);
                    return CompletableFuture.completedFuture(convertCachedOHLCToDataPoints(cached));
                })
                .orElseGet(() -> fetchOHLCFromDatabaseWithCaching(symbol, exchange, from, to, interval))
            )
            .exceptionally(ex -> {
                log.error("Failed to get historical data for {}:{}: {}", symbol, exchange, ex.getMessage());
                return List.of();
            });
    }

    /**
     * Get bulk price data for multiple symbols (Rule #5: Max 15 lines)
     * Rule #12: Virtual threads for parallel processing
     */
    public CompletableFuture<Map<String, MarketDataPoint>> getBulkPriceData(List<String> symbols, String exchange) {
        Map<String, MarketDataPoint> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> tasks = createBulkPriceTasks(symbols, exchange, results);
        return combineBulkPriceResults(tasks, results, symbols.size());
    }

    /**
     * Get active symbols for an exchange (Rule #5: Max 15 lines)
     */
    public CompletableFuture<List<String>> getActiveSymbols(String exchange, int minutes) {
        return circuitBreakerService.<List<String>>executeDatabaseOperationWithFallback(
            () -> marketDataRepository.getActiveSymbols(exchange, minutes),
            () -> List.<String>of()
        ).thenApply(symbols -> {
            log.debug("Found {} active symbols for exchange {}", symbols.size(), exchange);
            return symbols;
        }).exceptionally(ex -> {
            log.error("Failed to get active symbols for {}: {}", exchange, ex.getMessage());
            return List.of();
        });
    }

    /**
     * Get real-time data for multiple symbols (AgentOS compatibility)
     * RULE #9: Returns typed RealTimeDataResponse
     */
    public CompletableFuture<RealTimeDataResponse> getRealTimeData(List<String> symbols) {
        long startTime = System.currentTimeMillis();
        log.info("Getting real-time data for {} symbols", symbols.size());

        return getBulkPriceData(symbols, "NSE")
            .thenApply(dataMap -> buildRealTimeResponse(symbols, dataMap, startTime))
            .exceptionally(ex -> {
                log.error("Failed to get real-time data: {}", ex.getMessage(), ex);
                return RealTimeDataResponse.empty(symbols, ex.getMessage());
            });
    }

    /**
     * Get historical data for symbols with timeframe (AgentOS compatibility)
     * RULE #12: Parallel processing with virtual threads
     */
    public CompletableFuture<HistoricalDataResponse> getHistoricalDataByTimeframe(
            List<String> symbols, String timeframe) {
        long startTime = System.currentTimeMillis();
        log.info("Getting historical data for {} symbols with timeframe: {}", symbols.size(), timeframe);

        return fetchHistoricalDataForSymbols(symbols, timeframe)
            .thenApply(dataMap -> buildHistoricalResponse(symbols, timeframe, dataMap, startTime))
            .exceptionally(ex -> {
                log.error("Failed to get historical data: {}", ex.getMessage(), ex);
                return HistoricalDataResponse.empty(symbols, timeframe);
            });
    }

    // Helper methods (RULE #5: Each ≤15 lines)

    private MarketDataPoint convertCachedPriceToDataPoint(MarketDataCacheService.CachedPrice cached) {
        return MarketDataPoint.builder()
            .symbol(cached.symbol())
            .exchange(cached.exchange())
            .price(cached.price())
            .volume(cached.volume())
            .change(cached.change())
            .changePercent(cached.changePercent())
            .timestamp(cached.marketTime())
            .build();
    }

    private CompletableFuture<Optional<MarketDataPoint>> fetchPriceFromDatabase(String symbol, String exchange) {
        return circuitBreakerService.executeDatabaseOperationWithFallback(
            () -> marketDataRepository.getLatestPrice(symbol, exchange),
            () -> Optional.<MarketDataPoint>empty()
        );
    }

    private CompletableFuture<Optional<List<MarketDataCacheService.CachedOHLC>>> fetchOHLCFromCache(
            String symbol, String exchange, String interval) {
        return circuitBreakerService.executeRedisCacheOperationWithFallback(
            () -> cacheService.getOHLCData(symbol, exchange, interval),
            () -> Optional.<List<MarketDataCacheService.CachedOHLC>>empty()
        );
    }

    private CompletableFuture<List<MarketDataPoint>> fetchOHLCFromDatabaseWithCaching(
            String symbol, String exchange, Instant from, Instant to, String interval) {
        return circuitBreakerService.<List<MarketDataPoint>>executeDatabaseOperationWithFallback(
                () -> marketDataRepository.getOHLCData(symbol, exchange, from, to, interval),
                () -> List.<MarketDataPoint>of()
            )
            .thenCompose(data -> data.isEmpty()
                ? CompletableFuture.completedFuture(data)
                : cacheOHLCDataWithFallback(symbol, exchange, interval, data)
            );
    }

    private CompletableFuture<List<MarketDataPoint>> cacheOHLCDataWithFallback(
            String symbol, String exchange, String interval, List<MarketDataPoint> data) {
        return circuitBreakerService.<List<MarketDataPoint>>executeRedisCacheOperationWithFallback(
            () -> {
                cacheService.cacheOHLCData(symbol, exchange, interval, data);
                log.debug("Retrieved and cached {} OHLC records for {}:{}", data.size(), symbol, exchange);
                return data;
            },
            () -> data
        );
    }

    private List<MarketDataPoint> convertCachedOHLCToDataPoints(List<MarketDataCacheService.CachedOHLC> cachedData) {
        return cachedData.stream()
            .map(cached -> MarketDataPoint.builder()
                .symbol(cached.symbol()).exchange(cached.exchange()).dataType("OHLC")
                .open(cached.open()).high(cached.high()).low(cached.low())
                .price(cached.close()).volume(cached.volume()).timestamp(cached.timestamp())
                .build())
            .toList();
    }

    private List<CompletableFuture<Void>> createBulkPriceTasks(
            List<String> symbols, String exchange, Map<String, MarketDataPoint> results) {
        return symbols.stream()
            .map(symbol -> getCurrentPrice(symbol, exchange)
                .thenAccept(dataPoint -> dataPoint.ifPresent(point -> results.put(symbol, point)))
                .exceptionally(ex -> {
                    log.warn("Failed to get price for symbol {} in bulk request: {}", symbol, ex.getMessage());
                    return null;
                })
            )
            .toList();
    }

    private CompletableFuture<Map<String, MarketDataPoint>> combineBulkPriceResults(
            List<CompletableFuture<Void>> tasks, Map<String, MarketDataPoint> results, int totalSymbols) {
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                log.info("Bulk price data retrieved for {}/{} symbols", results.size(), totalSymbols);
                return Map.copyOf(results);
            });
    }

    private RealTimeDataResponse buildRealTimeResponse(
            List<String> symbols, Map<String, MarketDataPoint> dataMap, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        return RealTimeDataResponse.builder()
            .symbols(symbols).data(dataMap)
            .quality(determineDataQuality(processingTime)).timestamp(Instant.now())
            .metadata(RealTimeDataResponse.ResponseMetadata.builder()
                .requestedSymbols(symbols.size()).successfulSymbols(dataMap.size())
                .failedSymbols(symbols.size() - dataMap.size()).processingTimeMs(processingTime)
                .source("CACHE_AND_DATABASE").build())
            .build();
    }

    private RealTimeDataResponse.DataQuality determineDataQuality(long processingTimeMs) {
        return switch ((int) processingTimeMs) {
            case int t when t < 50 -> RealTimeDataResponse.DataQuality.EXCELLENT;
            case int t when t < 200 -> RealTimeDataResponse.DataQuality.GOOD;
            case int t when t < 1000 -> RealTimeDataResponse.DataQuality.FAIR;
            case int t when t < 3000 -> RealTimeDataResponse.DataQuality.POOR;
            default -> RealTimeDataResponse.DataQuality.STALE;
        };
    }

    private CompletableFuture<Map<String, List<MarketDataPoint>>> fetchHistoricalDataForSymbols(
            List<String> symbols, String timeframe) {
        Map<String, List<MarketDataPoint>> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> tasks = symbols.stream()
            .map(symbol -> fetchSymbolHistoricalData(symbol, timeframe, results))
            .toList();
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> Map.copyOf(results));
    }

    private CompletableFuture<Void> fetchSymbolHistoricalData(
            String symbol, String timeframe, Map<String, List<MarketDataPoint>> results) {
        Instant endTime = Instant.now();
        Instant startTime = calculateStartTimeFromTimeframe(timeframe, endTime);
        return getHistoricalData(symbol, "NSE", startTime, endTime, timeframe)
            .thenAccept(data -> { if (!data.isEmpty()) results.put(symbol, data); })
            .exceptionally(ex -> {
                log.warn("Failed to get historical data for {}: {}", symbol, ex.getMessage());
                return null;
            });
    }

    private HistoricalDataResponse buildHistoricalResponse(
            List<String> symbols, String timeframe, Map<String, List<MarketDataPoint>> dataMap, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        int totalPoints = dataMap.values().stream().mapToInt(List::size).sum();
        return HistoricalDataResponse.builder()
            .symbols(symbols).timeframe(timeframe).data(dataMap)
            .completeness(HistoricalDataResponse.DataCompleteness.calculate(symbols.size(), dataMap.size()))
            .timestamp(Instant.now())
            .metadata(HistoricalDataResponse.HistoricalMetadata.builder()
                .requestedSymbols(symbols.size()).successfulSymbols(dataMap.size())
                .totalDataPoints(totalPoints).processingTimeMs(processingTime)
                .dataSource("DATABASE_WITH_CACHE").build())
            .build();
    }

    private Instant calculateStartTimeFromTimeframe(String timeframe, Instant endTime) {
        long millisToSubtract = switch (timeframe) {
            case String t when t.endsWith("m") ->
                Long.parseLong(t.substring(0, t.length() - 1)) * 60_000L * 100;
            case String t when t.endsWith("h") ->
                Long.parseLong(t.substring(0, t.length() - 1)) * 3_600_000L * 100;
            case String t when t.endsWith("d") || t.endsWith("D") ->
                Long.parseLong(t.substring(0, t.length() - 1)) * 86_400_000L * 100;
            default -> 86_400_000L * 30;
        };
        return endTime.minusMillis(millisToSubtract);
    }
}
