package com.trademaster.marketdata.service;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.repository.MarketDataRepository;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Write Service (Write/Update Operations Only)
 *
 * Single Responsibility: Market data persistence and updates
 * Following Rule #2 (SRP) and Rule #25 (Circuit Breaker Protection)
 *
 * Features:
 * - Single point market data writes with cache updates
 * - Batch write operations for high-throughput scenarios
 * - Parallel database and cache operations
 * - Circuit breaker protection for resilience
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataWriteService {

    private final MarketDataRepository marketDataRepository;
    private final MarketDataCacheService cacheService;
    private final CircuitBreakerService circuitBreakerService;

    /**
     * Write market data point with circuit breaker protection
     * Follows Rule #25 (Circuit Breaker) and Rule #11 (Functional Error Handling)
     */
    public CompletableFuture<Boolean> writeMarketData(MarketDataPoint dataPoint) {
        // Write to database with circuit breaker protection
        CompletableFuture<Boolean> dbWrite = circuitBreakerService.<MarketDataRepository.WriteResult>executeDatabaseOperationWithFallback(
            () -> marketDataRepository.writeMarketData(dataPoint).join(),
            () -> new MarketDataRepository.WriteResult.Failed("Circuit breaker fallback")
        ).thenApply(result -> result.isSuccess());

        // Update cache with circuit breaker protection (Rule #3: Functional pattern, no if-else)
        CompletableFuture<Boolean> cacheUpdate = circuitBreakerService.<Boolean>executeRedisCacheOperationWithFallback(
            () -> {
                cacheService.cacheCurrentPrice(dataPoint);
                // Functional approach using Optional instead of if statement
                Optional.of(dataPoint)
                    .filter(MarketDataPoint::hasOrderBookData)
                    .ifPresent(cacheService::cacheOrderBook);
                return true;
            },
            () -> false  // Cache failure is non-critical
        );

        // Combine results - both operations run in parallel (Rule #3: Functional pattern, no if-else)
        return CompletableFuture.allOf(dbWrite, cacheUpdate)
            .thenApply(v -> {
                boolean dbSuccess = dbWrite.join();
                boolean cacheSuccess = cacheUpdate.join();
                boolean overallSuccess = dbSuccess && cacheSuccess;

                // Functional approach using Optional instead of if statement
                Optional.of(overallSuccess)
                    .filter(Boolean::booleanValue)
                    .ifPresent(success ->
                        log.trace("Successfully wrote and cached market data for {}:{}",
                            dataPoint.symbol(), dataPoint.exchange())
                    );
                return dbSuccess;  // Return true if DB write succeeded (cache is optional)
            })
            .exceptionally(ex -> {
                log.error("Failed to write market data for {}:{}: {}",
                    dataPoint.symbol(), dataPoint.exchange(), ex.getMessage());
                return false;
            });
    }

    /**
     * Batch write market data points (Rule #5: Max 15 lines)
     * Rule #25: Circuit breaker protection, Rule #12: Parallel operations
     */
    public CompletableFuture<BatchWriteResult> batchWriteMarketData(List<MarketDataPoint> dataPoints) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<MarketDataRepository.WriteResult> dbWrite = executeBatchDatabaseWrite(dataPoints);
        CompletableFuture<MarketDataCacheService.BatchCacheResult> cacheUpdate = executeBatchCacheUpdate(dataPoints);
        return combineBatchWriteResults(dbWrite, cacheUpdate, dataPoints.size(), startTime);
    }

    // Helper methods (Rule #5: Max 15 lines per method)

    /**
     * Execute batch database write
     */
    private CompletableFuture<MarketDataRepository.WriteResult> executeBatchDatabaseWrite(List<MarketDataPoint> dataPoints) {
        return circuitBreakerService.executeDatabaseOperationWithFallback(
            () -> marketDataRepository.batchWriteMarketData(dataPoints).join(),
            () -> new MarketDataRepository.WriteResult.Failed("Circuit breaker fallback")
        );
    }

    /**
     * Execute batch cache update
     */
    private CompletableFuture<MarketDataCacheService.BatchCacheResult> executeBatchCacheUpdate(List<MarketDataPoint> dataPoints) {
        return circuitBreakerService.executeRedisCacheOperationWithFallback(
            () -> cacheService.batchCachePrices(dataPoints).join(),
            () -> new MarketDataCacheService.BatchCacheResult(0, 0, 0)
        );
    }

    /**
     * Combine batch write results
     */
    private CompletableFuture<BatchWriteResult> combineBatchWriteResults(
            CompletableFuture<MarketDataRepository.WriteResult> dbWrite,
            CompletableFuture<MarketDataCacheService.BatchCacheResult> cacheUpdate,
            int totalDataPoints, long startTime) {
        return CompletableFuture.allOf(dbWrite, cacheUpdate)
            .thenApply(v -> {
                var dbResult = dbWrite.join();
                var cacheResult = cacheUpdate.join();
                long duration = System.currentTimeMillis() - startTime;
                int totalSuccessful = dbResult.isSuccess() ? totalDataPoints : 0;

                log.info("Batch write completed: {}/{} points in {}ms", totalSuccessful, totalDataPoints, duration);

                return new BatchWriteResult(totalSuccessful, totalDataPoints - totalSuccessful,
                    duration, cacheResult.successful());
            })
            .exceptionally(ex -> {
                log.error("Batch write failed: {}", ex.getMessage());
                return new BatchWriteResult(0, totalDataPoints, 0, 0);
            });
    }

    // Data classes (Rule #9: Records for immutability)
    public record BatchWriteResult(
        int successful,
        int failed,
        long durationMs,
        int cacheUpdates
    ) {}
}
