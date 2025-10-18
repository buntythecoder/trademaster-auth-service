package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Internal API Controller
 *
 * Provides internal service-to-service API endpoints for market data operations.
 * Follows Rule #6 (Zero Trust Security) - Internal access pattern with API key authentication.
 *
 * Features:
 * - Service-to-service market data operations
 * - API key authentication via DefaultServiceApiKeyFilter (common library)
 * - Circuit breaker protection (Rule #25)
 * - Virtual thread optimization (Rule #12)
 * - Structured logging with correlation IDs (Rule #15)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/market-data")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE')")
public class InternalController {

    private final MarketDataService marketDataService;

    /**
     * Get current price for a symbol (Rule #5: Max 15 lines per method)
     */
    @GetMapping("/price/{exchange}/{symbol}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getCurrentPrice(
            @PathVariable String exchange,
            @PathVariable String symbol) {

        log.debug("Internal API: getCurrentPrice - Symbol: {}, Exchange: {}", symbol, exchange);

        return marketDataService.getCurrentPrice(symbol, exchange)
            .thenApply(result -> result
                .<ResponseEntity<Map<String, Object>>>map(data -> ResponseEntity.ok(Map.of(
                    "symbol", data.symbol(),
                    "exchange", data.exchange(),
                    "price", data.price(),
                    "volume", data.volume(),
                    "timestamp", data.timestamp()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build())
            );
    }

    /**
     * Get bulk price data for multiple symbols (Rule #5: Max 15 lines)
     */
    @PostMapping("/price/bulk")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getBulkPrices(
            @RequestBody BulkPriceRequest request) {

        log.debug("Internal API: getBulkPrices - Symbols: {}, Exchange: {}",
            request.symbols(), request.exchange());

        return marketDataService.getBulkPriceData(request.symbols(), request.exchange())
            .thenApply(prices -> ResponseEntity.ok(Map.of(
                "exchange", request.exchange(),
                "count", prices.size(),
                "prices", prices,
                "timestamp", Instant.now()
            )));
    }

    /**
     * Get historical OHLC data (Rule #5: Max 15 lines)
     */
    @GetMapping("/historical/{exchange}/{symbol}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getHistoricalData(
            @PathVariable String exchange,
            @PathVariable String symbol,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "1m") String interval) {

        Instant fromTime = Instant.parse(from);
        Instant toTime = Instant.parse(to);

        log.debug("Internal API: getHistoricalData - Symbol: {}, Exchange: {}, From: {}, To: {}",
            symbol, exchange, from, to);

        return marketDataService.getHistoricalData(symbol, exchange, fromTime, toTime, interval)
            .thenApply(data -> ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "exchange", exchange,
                "interval", interval,
                "count", data.size(),
                "data", data
            )));
    }

    /**
     * Get active symbols for an exchange (Rule #5: Max 15 lines)
     */
    @GetMapping("/active-symbols/{exchange}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getActiveSymbols(
            @PathVariable String exchange,
            @RequestParam(defaultValue = "60") int minutes) {

        log.debug("Internal API: getActiveSymbols - Exchange: {}, Minutes: {}", exchange, minutes);

        return marketDataService.getActiveSymbols(exchange, minutes)
            .thenApply(symbols -> ResponseEntity.ok(Map.of(
                "exchange", exchange,
                "count", symbols.size(),
                "symbols", symbols,
                "timestamp", Instant.now()
            )));
    }

    /**
     * Write market data point (Rule #5: Max 15 lines)
     */
    @PostMapping("/write")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> writeMarketData(
            @RequestBody MarketDataPoint dataPoint) {

        log.debug("Internal API: writeMarketData - Symbol: {}, Exchange: {}",
            dataPoint.symbol(), dataPoint.exchange());

        return marketDataService.writeMarketData(dataPoint)
            .thenApply(success -> success
                ? ResponseEntity.ok(Map.of("status", "success", "timestamp", Instant.now()))
                : ResponseEntity.internalServerError().body(Map.of("status", "failed"))
            );
    }

    /**
     * Batch write market data points (Rule #5: Max 15 lines)
     */
    @PostMapping("/write/batch")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> batchWriteMarketData(
            @RequestBody List<MarketDataPoint> dataPoints) {

        log.debug("Internal API: batchWriteMarketData - Count: {}", dataPoints.size());

        return marketDataService.batchWriteMarketData(dataPoints)
            .thenApply(result -> ResponseEntity.ok(Map.of(
                "successful", result.successful(),
                "failed", result.failed(),
                "durationMs", result.durationMs(),
                "cacheUpdates", result.cacheUpdates()
            )));
    }

    /**
     * Get data quality report (Rule #5: Max 15 lines)
     */
    @GetMapping("/quality-report/{exchange}/{symbol}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getQualityReport(
            @PathVariable String exchange,
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1") int hours) {

        log.debug("Internal API: getQualityReport - Symbol: {}, Exchange: {}, Hours: {}",
            symbol, exchange, hours);

        return marketDataService.generateQualityReport(symbol, exchange, hours)
            .thenApply(report -> ResponseEntity.ok(Map.of(
                "symbol", report.symbol(),
                "exchange", report.exchange(),
                "totalRecords", report.totalRecords(),
                "dataGaps", report.dataGaps(),
                "qualityScore", report.qualityScore(),
                "qualityLevel", report.qualityLevel().name(),
                "generatedAt", report.generatedAt()
            )));
    }

    // Request DTOs (Rule #9: Records for immutability)
    public record BulkPriceRequest(List<String> symbols, String exchange) {}
}
