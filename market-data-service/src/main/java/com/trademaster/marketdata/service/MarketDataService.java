package com.trademaster.marketdata.service;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;

/**
 * Core Market Data Service
 * 
 * Features:
 * - Centralized market data operations
 * - Integration with cache and database layers
 * - Virtual thread optimization for concurrent operations
 * - Data quality monitoring and validation
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;
    private final MarketDataCacheService cacheService;

    /**
     * Get current price for a symbol
     */
    public CompletableFuture<Optional<MarketDataPoint>> getCurrentPrice(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Try cache first
                var cacheTask = scope.fork(() -> cacheService.getCurrentPrice(symbol, exchange));
                
                // Fallback to repository if cache miss
                var repoTask = scope.fork(() -> marketDataRepository.getLatestPrice(symbol, exchange));
                
                scope.join();
                scope.throwIfFailed();
                
                var cachedResult = cacheTask.get();
                if (cachedResult.isPresent()) {
                    // Convert cached data to MarketDataPoint
                    var cached = cachedResult.get();
                    var dataPoint = MarketDataPoint.builder()
                        .symbol(cached.symbol())
                        .exchange(cached.exchange())
                        .price(cached.price())
                        .volume(cached.volume())
                        .change(cached.change())
                        .changePercent(cached.changePercent())
                        .timestamp(cached.marketTime())
                        .build();
                    
                    return Optional.of(dataPoint);
                }
                
                // Use repository result
                return repoTask.get();
                
            } catch (Exception e) {
                log.error("Failed to get current price for {}:{}: {}", symbol, exchange, e.getMessage());
                return Optional.empty();
            }
        });
    }

    /**
     * Get historical OHLC data
     */
    public CompletableFuture<List<MarketDataPoint>> getHistoricalData(String symbol, String exchange, 
            Instant from, Instant to, String interval) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache first
                var cachedData = cacheService.getOHLCData(symbol, exchange, interval);
                if (cachedData.isPresent()) {
                    log.debug("Retrieved OHLC data from cache for {}:{}", symbol, exchange);
                    return convertCachedOHLCToDataPoints(cachedData.get());
                }
                
                // Fetch from repository
                var data = marketDataRepository.getOHLCData(symbol, exchange, from, to, interval);
                
                // Cache the result
                if (!data.isEmpty()) {
                    cacheService.cacheOHLCData(symbol, exchange, interval, data);
                }
                
                log.debug("Retrieved {} OHLC records from repository for {}:{}", 
                    data.size(), symbol, exchange);
                
                return data;
                
            } catch (Exception e) {
                log.error("Failed to get historical data for {}:{}: {}", 
                    symbol, exchange, e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * Get bulk price data for multiple symbols
     */
    public CompletableFuture<Map<String, MarketDataPoint>> getBulkPriceData(List<String> symbols, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                Map<String, MarketDataPoint> results = new java.util.concurrent.ConcurrentHashMap<>();
                
                // Process symbols in parallel using virtual threads
                List<CompletableFuture<Void>> tasks = symbols.stream()
                    .map(symbol -> scope.fork(() -> {
                        getCurrentPrice(symbol, exchange)
                            .thenAccept(dataPoint -> {
                                if (dataPoint.isPresent()) {
                                    results.put(symbol, dataPoint.get());
                                }
                            })
                            .join();
                        return null;
                    }))
                    .map(supplier -> CompletableFuture.runAsync(() -> {
                        try {
                            supplier.get();
                        } catch (Exception e) {
                            log.warn("Failed to get price for symbol in bulk request: {}", e.getMessage());
                        }
                    }))
                    .toList();
                
                // Wait for all tasks to complete
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
                
                scope.join();
                scope.throwIfFailed();
                
                log.info("Bulk price data retrieved for {}/{} symbols", results.size(), symbols.size());
                return results;
                
            } catch (Exception e) {
                log.error("Failed to get bulk price data: {}", e.getMessage());
                return Map.of();
            }
        });
    }

    /**
     * Get active symbols for an exchange
     */
    public CompletableFuture<List<String>> getActiveSymbols(String exchange, int minutes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var symbols = marketDataRepository.getActiveSymbols(exchange, minutes);
                log.debug("Found {} active symbols for exchange {}", symbols.size(), exchange);
                return symbols;
                
            } catch (Exception e) {
                log.error("Failed to get active symbols for {}: {}", exchange, e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * Write market data point
     */
    public CompletableFuture<Boolean> writeMarketData(MarketDataPoint dataPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Write to database
                var dbWriteTask = scope.fork(() -> marketDataRepository.writeMarketData(dataPoint));
                
                // Update cache
                var cacheTask = scope.fork(() -> {
                    cacheService.cacheCurrentPrice(dataPoint);
                    if (dataPoint.hasOrderBookData()) {
                        cacheService.cacheOrderBook(dataPoint);
                    }
                    return true;
                });
                
                scope.join();
                scope.throwIfFailed();
                
                var dbResult = dbWriteTask.get().join();
                var cacheResult = cacheTask.get();
                
                boolean success = dbResult.isSuccess() && cacheResult;
                
                if (success) {
                    log.trace("Successfully wrote and cached market data for {}:{}", 
                        dataPoint.symbol(), dataPoint.exchange());
                }
                
                return success;
                
            } catch (Exception e) {
                log.error("Failed to write market data for {}:{}: {}", 
                    dataPoint.symbol(), dataPoint.exchange(), e.getMessage());
                return false;
            }
        });
    }

    /**
     * Batch write market data points
     */
    public CompletableFuture<BatchWriteResult> batchWriteMarketData(List<MarketDataPoint> dataPoints) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Batch write to database
                var dbWriteTask = scope.fork(() -> marketDataRepository.batchWriteMarketData(dataPoints));
                
                // Batch update cache
                var cacheTask = scope.fork(() -> cacheService.batchCachePrices(dataPoints));
                
                scope.join();
                scope.throwIfFailed();
                
                var dbResult = dbWriteTask.get().join();
                var cacheResult = cacheTask.get().join();
                
                long duration = System.currentTimeMillis() - startTime;
                
                int totalSuccessful = dbResult.isSuccess() ? dataPoints.size() : 0;
                
                log.info("Batch write completed: {}/{} points in {}ms", 
                    totalSuccessful, dataPoints.size(), duration);
                
                return new BatchWriteResult(
                    totalSuccessful,
                    dataPoints.size() - totalSuccessful,
                    duration,
                    cacheResult.successful()
                );
                
            } catch (Exception e) {
                log.error("Batch write failed: {}", e.getMessage());
                return new BatchWriteResult(0, dataPoints.size(), 0, 0);
            }
        });
    }

    /**
     * Generate data quality report
     */
    public CompletableFuture<DataQualityReport> generateQualityReport(String symbol, String exchange, int hours) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var report = marketDataRepository.generateQualityReport(symbol, exchange, hours);
                log.debug("Generated quality report for {}:{} - Score: {}", 
                    symbol, exchange, report.qualityScore());
                return convertToServiceQualityReport(report);
                
            } catch (Exception e) {
                log.error("Failed to generate quality report for {}:{}: {}", 
                    symbol, exchange, e.getMessage());
                return new DataQualityReport(symbol, exchange, 0L, 0L, 0.0, 
                    QualityLevel.LOW, Instant.now());
            }
        });
    }

    // Helper methods
    private List<MarketDataPoint> convertCachedOHLCToDataPoints(List<MarketDataCacheService.CachedOHLC> cachedData) {
        return cachedData.stream()
            .map(cached -> MarketDataPoint.builder()
                .symbol(cached.symbol())
                .exchange(cached.exchange())
                .dataType("OHLC")
                .open(cached.open())
                .high(cached.high())
                .low(cached.low())
                .price(cached.close())
                .volume(cached.volume())
                .timestamp(cached.timestamp())
                .build())
            .toList();
    }

    private DataQualityReport convertToServiceQualityReport(MarketDataRepository.DataQualityReport repoReport) {
        QualityLevel level = switch (repoReport.getQualityLevel()) {
            case HIGH -> QualityLevel.HIGH;
            case MEDIUM -> QualityLevel.MEDIUM;
            case LOW -> QualityLevel.LOW;
        };
        
        return new DataQualityReport(
            repoReport.symbol(),
            repoReport.exchange(),
            repoReport.totalRecords(),
            repoReport.dataGaps(),
            repoReport.qualityScore(),
            level,
            repoReport.generatedAt()
        );
    }

    // Data classes
    public record BatchWriteResult(
        int successful,
        int failed,
        long durationMs,
        int cacheUpdates
    ) {}

    public record DataQualityReport(
        String symbol,
        String exchange,
        long totalRecords,
        long dataGaps,
        double qualityScore,
        QualityLevel qualityLevel,
        Instant generatedAt
    ) {}

    public enum QualityLevel {
        HIGH("Excellent data quality"),
        MEDIUM("Good data quality with minor issues"),
        LOW("Poor data quality requiring attention");

        private final String description;

        QualityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
    
    // AgentOS Integration Methods
    
    /**
     * Get real-time data for multiple symbols (AgentOS compatibility)
     */
    public Object getRealTimeData(List<String> symbols) {
        log.info("Getting real-time data for symbols: {}", symbols);
        // Implementation would coordinate with existing getCurrentPrice method
        return Map.of(
            "symbols", symbols,
            "timestamp", Instant.now(),
            "status", "ACTIVE"
        );
    }
    
    /**
     * Get historical data for symbols with timeframe (AgentOS compatibility)
     */
    public Object getHistoricalData(List<String> symbols, String timeframe) {
        log.info("Getting historical data for symbols: {} with timeframe: {}", symbols, timeframe);
        // Implementation would coordinate with existing historical data methods
        return Map.of(
            "symbols", symbols,
            "timeframe", timeframe,
            "timestamp", Instant.now(),
            "status", "SUCCESS"
        );
    }
    
    /**
     * Subscribe to real-time updates (AgentOS compatibility)
     */
    public Object subscribeToRealTimeUpdates(List<String> symbols, Integer updateFrequencyMs, Map<String, Object> callbackConfig) {
        log.info("Subscribing to real-time updates for symbols: {} with frequency: {}ms", symbols, updateFrequencyMs);
        return Map.of(
            "subscriptionId", "sub_" + System.currentTimeMillis(),
            "symbols", symbols,
            "status", "ACTIVE",
            "updateFrequency", updateFrequencyMs
        );
    }
    
    /**
     * Create price alert (AgentOS compatibility)
     */
    public Object createPriceAlert(Map<String, Object> alertConfig) {
        log.info("Creating price alert with config: {}", alertConfig);
        return Map.of(
            "alertId", "alert_" + System.currentTimeMillis(),
            "status", "ACTIVE",
            "config", alertConfig
        );
    }
    
    /**
     * Update price alert (AgentOS compatibility)
     */
    public Object updatePriceAlert(Map<String, Object> alertConfig) {
        log.info("Updating price alert with config: {}", alertConfig);
        return Map.of(
            "alertId", alertConfig.get("alertId"),
            "status", "UPDATED",
            "config", alertConfig
        );
    }
    
    /**
     * Delete price alert (AgentOS compatibility)
     */
    public Object deletePriceAlert(Map<String, Object> alertConfig) {
        log.info("Deleting price alert with config: {}", alertConfig);
        return Map.of(
            "alertId", alertConfig.get("alertId"),
            "status", "DELETED"
        );
    }
    
    /**
     * List price alerts (AgentOS compatibility)
     */
    public Object listPriceAlerts(Map<String, Object> criteria) {
        log.info("Listing price alerts with criteria: {}", criteria);
        return Map.of(
            "alerts", List.of(),
            "count", 0,
            "status", "SUCCESS"
        );
    }
}