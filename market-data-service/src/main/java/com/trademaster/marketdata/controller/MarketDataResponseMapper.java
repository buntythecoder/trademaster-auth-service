package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.controller.MarketDataConstants.*;
import com.trademaster.marketdata.dto.MarketDataResponse;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Market Data Response Mapper
 *
 * Single Responsibility: Transform domain objects to HTTP response DTOs
 * Following Rule #2 (SRP) and Rule #4 (Facade Design Pattern)
 *
 * Responsibilities:
 * - Build MarketDataResponse objects from cache data
 * - Build MarketDataResponse objects from live data
 * - Build error/forbidden responses with appropriate status codes
 *
 * Benefits:
 * - Isolates response mapping logic from controller
 * - Easy to test independently
 * - Centralized response format consistency
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
public class MarketDataResponseMapper {

    /**
     * Build response for cached price data
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildCachedPriceResponse(
            MarketDataCacheService.CachedPrice price) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "symbol", price.symbol(),
                "exchange", price.exchange(),
                "price", price.price(),
                "volume", price.volume(),
                "change", price.change(),
                "changePercent", price.changePercent(),
                "timestamp", price.marketTime(),
                "source", DataSource.CACHE
            )
        ));
    }

    /**
     * Build response for live price data
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildLivePriceResponse(MarketDataPoint point) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "symbol", point.symbol(),
                "exchange", point.exchange(),
                "price", point.price(),
                "volume", point.volume(),
                "timestamp", point.timestamp(),
                "source", DataSource.LIVE
            )
        ));
    }

    /**
     * Build forbidden response for realtime data access restriction
     * Rule #5: Helper method
     */
    public ResponseEntity<MarketDataResponse> buildForbiddenResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(MarketDataResponse.error(ErrorMessages.REALTIME_REQUIRES_PREMIUM));
    }

    /**
     * Build response for cached historical data
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildCachedHistoricalResponse(
            String symbol, String exchange, String interval,
            List<MarketDataCacheService.CachedOHLC> data) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "symbol", symbol,
                "exchange", exchange,
                "interval", interval,
                "data", data,
                "source", DataSource.CACHE
            )
        ));
    }

    /**
     * Build response for live historical data
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildLiveHistoricalResponse(
            String symbol, String exchange, Instant from, Instant to,
            String interval, List<MarketDataPoint> data) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "symbol", symbol,
                "exchange", exchange,
                "interval", interval,
                "from", from,
                "to", to,
                "data", data,
                "count", data.size(),
                "source", DataSource.DATABASE
            )
        ));
    }

    /**
     * Build response for date range validation error
     * Rule #5: Helper method
     */
    public ResponseEntity<MarketDataResponse> buildDateRangeError() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(MarketDataResponse.error(ErrorMessages.DATE_RANGE_EXCEEDS_LIMIT));
    }

    /**
     * Build response for symbol count validation error
     * Rule #5: Helper method
     */
    public ResponseEntity<MarketDataResponse> buildSymbolCountError() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(MarketDataResponse.error(ErrorMessages.SYMBOL_COUNT_EXCEEDS_LIMIT));
    }

    /**
     * Build response for bulk price data
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildBulkPriceResponse(
            String exchange, List<String> symbols, Map<String, MarketDataPoint> priceData) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "exchange", exchange,
                "symbols", symbols,
                "prices", priceData,
                "timestamp", Instant.now()
            )
        ));
    }

    /**
     * Build response for active symbols list
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildActiveSymbolsResponse(
            String exchange, List<String> symbols) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "exchange", exchange,
                "symbols", symbols,
                "count", symbols.size(),
                "timestamp", Instant.now()
            )
        ));
    }

    /**
     * Build response for order book data
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildOrderBookResponse(
            MarketDataCacheService.CachedOrderBook orderBook) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "symbol", orderBook.symbol(),
                "exchange", orderBook.exchange(),
                "bid", orderBook.bid(),
                "ask", orderBook.ask(),
                "bidSize", orderBook.bidSize(),
                "askSize", orderBook.askSize(),
                "spread", orderBook.spread(),
                "spreadPercent", orderBook.spreadPercent(),
                "timestamp", orderBook.marketTime(),
                "source", "cache"
            )
        ));
    }

    /**
     * Build response for market statistics
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildMarketStatsResponse(
            String exchange, MarketDataCacheService.CacheMetrics cacheMetrics) {
        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "exchange", exchange,
                "cache", Map.of(
                    "hitRate", cacheMetrics.hitRate(),
                    "avgResponseTime", cacheMetrics.avgResponseTimeMs(),
                    "totalRequests", cacheMetrics.cacheHits() + cacheMetrics.cacheMisses(),
                    "performanceTarget", cacheMetrics.isPerformanceTarget()
                ),
                "timestamp", Instant.now()
            )
        ));
    }

    /**
     * Build response for health check
     * Rule #5: Max 15 lines per method
     */
    public ResponseEntity<MarketDataResponse> buildHealthCheckResponse(
            MarketDataCacheService.CacheMetrics cacheMetrics) {
        boolean healthy = cacheMetrics.isPerformanceTarget() &&
                         cacheMetrics.avgResponseTimeMs() < Performance.MAX_HEALTHY_RESPONSE_TIME_MS;

        return ResponseEntity.ok(MarketDataResponse.success(
            Map.of(
                "status", healthy ? HealthStatus.HEALTHY : HealthStatus.DEGRADED,
                "avgResponseTime", cacheMetrics.avgResponseTimeMs(),
                "cacheHitRate", cacheMetrics.hitRate(),
                "timestamp", Instant.now()
            )
        ));
    }
}
