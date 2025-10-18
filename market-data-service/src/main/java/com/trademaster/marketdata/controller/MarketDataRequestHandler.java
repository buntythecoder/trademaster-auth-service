package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.dto.MarketDataResponse;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Request Handler
 *
 * Single Responsibility: Orchestrate market data requests with cache-first strategy
 * Following Rule #2 (SRP), Rule #3 (Functional Programming), Rule #4 (Facade Pattern)
 *
 * Responsibilities:
 * - Implement cache-first strategy for data retrieval
 * - Coordinate between cache service and orchestration service
 * - Handle cache hits and misses with appropriate fallback
 * - Apply access policy rules during request processing
 *
 * Benefits:
 * - Isolates request orchestration from HTTP concerns
 * - Easy to test cache-first strategy independently
 * - Clear separation of concerns
 * - Functional programming patterns (no if-else, Railway-oriented)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataRequestHandler {

    private final MarketDataOrchestrationService orchestrationService;
    private final MarketDataCacheService cacheService;
    private final MarketDataResponseMapper responseMapper;
    private final MarketDataAccessPolicy accessPolicy;

    /**
     * Handle price request with cache-first strategy
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> handlePriceRequest(
            String symbol, String exchange, UserDetails userDetails) {
        var cachedPrice = cacheService.getCurrentPrice(symbol, exchange);
        return handleCachedPrice(cachedPrice, symbol, exchange, userDetails);
    }

    /**
     * Handle cached price with access policy validation
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Max 15 lines per method
     */
    private ResponseEntity<MarketDataResponse> handleCachedPrice(
            Optional<MarketDataCacheService.CachedPrice> cached,
            String symbol, String exchange, UserDetails userDetails) {
        return cached
            .filter(price -> accessPolicy.canAccessRealtimeData(price, userDetails))
            .map(responseMapper::buildCachedPriceResponse)
            .orElseGet(() -> handleRealtimeRestriction(cached, symbol, exchange, userDetails));
    }

    /**
     * Handle realtime data access restriction for free tier
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Max 15 lines per method
     */
    private ResponseEntity<MarketDataResponse> handleRealtimeRestriction(
            Optional<MarketDataCacheService.CachedPrice> cached,
            String symbol, String exchange, UserDetails userDetails) {
        return cached
            .filter(price -> accessPolicy.shouldRestrictRealtimeAccess(price, userDetails))
            .map(price -> responseMapper.buildForbiddenResponse())
            .orElseGet(() -> fetchLivePrice(symbol, exchange));
    }

    /**
     * Fetch live price from orchestration service
     * Rule #5: Max 15 lines per method
     */
    private ResponseEntity<MarketDataResponse> fetchLivePrice(String symbol, String exchange) {
        return orchestrationService.getCurrentPrice(symbol, exchange)
            .thenApply(this::handleLivePriceResult)
            .join();
    }

    /**
     * Handle live price fetch result
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Max 15 lines per method
     */
    private ResponseEntity<MarketDataResponse> handleLivePriceResult(
            Optional<MarketDataPoint> dataPoint) {
        return dataPoint
            .map(point -> {
                cacheService.cacheCurrentPrice(point);
                return responseMapper.buildLivePriceResponse(point);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Handle historical data request with validation and cache-first strategy
     * Rule #3: Functional pattern with Optional
     * Rule #5: Max 15 lines per method
     */
    public CompletableFuture<ResponseEntity<MarketDataResponse>> handleHistoricalDataRequest(
            String symbol, String exchange, Instant from, Instant to,
            String interval, UserDetails userDetails) {
        return accessPolicy.validateHistoricalDateRange(userDetails, from, to)
            .map(error -> CompletableFuture.completedFuture(responseMapper.buildDateRangeError()))
            .orElseGet(() -> handleHistoricalCacheData(symbol, exchange, from, to, interval));
    }

    /**
     * Handle historical cache data lookup
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Max 15 lines per method
     */
    private CompletableFuture<ResponseEntity<MarketDataResponse>> handleHistoricalCacheData(
            String symbol, String exchange, Instant from, Instant to, String interval) {
        var cachedData = cacheService.getOHLCData(symbol, exchange, interval);
        return cachedData
            .map(data -> CompletableFuture.completedFuture(
                responseMapper.buildCachedHistoricalResponse(symbol, exchange, interval, data)))
            .orElseGet(() -> fetchHistoricalDataFromService(symbol, exchange, from, to, interval));
    }

    /**
     * Fetch historical data from orchestration service
     * Rule #5: Max 15 lines per method
     */
    private CompletableFuture<ResponseEntity<MarketDataResponse>> fetchHistoricalDataFromService(
            String symbol, String exchange, Instant from, Instant to, String interval) {
        return orchestrationService.getHistoricalData(symbol, exchange, from, to, interval)
            .thenApply(data -> {
                cacheService.cacheOHLCData(symbol, exchange, interval, data);
                return responseMapper.buildLiveHistoricalResponse(
                    symbol, exchange, from, to, interval, data);
            });
    }

    /**
     * Handle bulk price data request with validation
     * Rule #3: Functional pattern with Optional
     * Rule #5: Max 15 lines per method
     */
    public CompletableFuture<ResponseEntity<MarketDataResponse>> handleBulkPriceRequest(
            List<String> symbols, String exchange, UserDetails userDetails) {
        return accessPolicy.validateBulkRequestSize(userDetails, symbols.size())
            .map(error -> CompletableFuture.completedFuture(responseMapper.buildSymbolCountError()))
            .orElseGet(() -> fetchBulkPriceData(symbols, exchange));
    }

    /**
     * Fetch bulk price data from orchestration service
     * Rule #5: Max 15 lines per method
     */
    private CompletableFuture<ResponseEntity<MarketDataResponse>> fetchBulkPriceData(
            List<String> symbols, String exchange) {
        return orchestrationService.getBulkPriceData(symbols, exchange)
            .thenApply(priceData ->
                responseMapper.buildBulkPriceResponse(exchange, symbols, priceData));
    }

    /**
     * Handle order book request with cache lookup
     * Rule #3: Functional pattern, no if-else
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> handleOrderBookRequest(
            String symbol, String exchange) {
        var cachedOrderBook = cacheService.getOrderBook(symbol, exchange);
        return cachedOrderBook
            .map(responseMapper::buildOrderBookResponse)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Handle active symbols request
     * Rule #5: Max 15 lines per method
     */
    public CompletableFuture<ResponseEntity<MarketDataResponse>> handleActiveSymbolsRequest(
            String exchange, int windowMinutes) {
        return orchestrationService.getActiveSymbols(exchange, windowMinutes)
            .thenApply(symbols ->
                responseMapper.buildActiveSymbolsResponse(exchange, symbols));
    }
}
