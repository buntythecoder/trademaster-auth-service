package com.trademaster.marketdata.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;

/**
 * Repository for Market Data time-series operations
 * 
 * Features:
 * - High-performance batch writes with virtual threads
 * - Optimized queries for real-time and historical data
 * - Data quality monitoring and validation
 * - Automatic downsampling and aggregation
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MarketDataRepository {
    
    private final InfluxDBClient influxDBClient;
    private final WriteApi writeApi;
    private final QueryApi queryApi;
    
    @Value("${influxdb.bucket}")
    private String bucket;
    
    @Value("${influxdb.org}")
    private String organization;

    /**
     * Write single market data point
     */
    public CompletableFuture<WriteResult> writeMarketData(MarketDataPoint dataPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!dataPoint.isValid()) {
                    return new WriteResult.Failed("Invalid data point: " + dataPoint.symbol());
                }
                
                writeApi.writeMeasurement(WritePrecision.MS, dataPoint);
                log.trace("Written market data point: {} - {}", 
                    dataPoint.symbol(), dataPoint.price());
                
                return new WriteResult.Success(1, dataPoint.timestamp());
                
            } catch (Exception e) {
                log.error("Failed to write market data for {}: {}", 
                    dataPoint.symbol(), e.getMessage());
                return new WriteResult.Failed("Write error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Batch write market data points using virtual threads
     */
    public CompletableFuture<WriteResult> batchWriteMarketData(List<MarketDataPoint> dataPoints) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Validate all points
                var validPoints = dataPoints.stream()
                    .filter(MarketDataPoint::isValid)
                    .toList();
                
                if (validPoints.isEmpty()) {
                    return new WriteResult.Failed("No valid data points in batch");
                }
                
                // Partition into optimal batch sizes for virtual threads
                int batchSize = 1000;
                var writeTask = scope.fork(() -> {
                    for (int i = 0; i < validPoints.size(); i += batchSize) {
                        int endIndex = Math.min(i + batchSize, validPoints.size());
                        var batch = validPoints.subList(i, endIndex);
                        
                        writeApi.writeMeasurements(WritePrecision.MS, batch);
                        log.debug("Written batch of {} market data points", batch.size());
                    }
                    return validPoints.size();
                });
                
                scope.join();
                scope.throwIfFailed();
                
                int written = writeTask.get();
                log.info("Successfully wrote {} market data points", written);
                
                return new WriteResult.Success(written, Instant.now());
                
            } catch (Exception e) {
                log.error("Batch write failed: {}", e.getMessage());
                return new WriteResult.Failed("Batch write error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get latest price for symbol
     */
    public Optional<MarketDataPoint> getLatestPrice(String symbol, String exchange) {
        try {
            String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: -1h)
                  |> filter(fn: (r) => r._measurement == "market_data")
                  |> filter(fn: (r) => r.symbol == "%s")
                  |> filter(fn: (r) => r.exchange == "%s")
                  |> filter(fn: (r) => r._field == "price")
                  |> last()
                """, bucket, symbol, exchange);
            
            var tables = queryApi.query(flux, organization);
            
            return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .findFirst()
                .map(record -> MarketDataPoint.builder()
                    .symbol(record.getValueByKey("symbol").toString())
                    .exchange(record.getValueByKey("exchange").toString())
                    .price(new BigDecimal(record.getValue().toString()))
                    .timestamp((Instant) record.getTime())
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get latest price for {}:{}: {}", 
                symbol, exchange, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Get historical OHLC data
     */
    public List<MarketDataPoint> getOHLCData(String symbol, String exchange, 
            Instant start, Instant end, String interval) {
        try {
            String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "market_data")
                  |> filter(fn: (r) => r.symbol == "%s")
                  |> filter(fn: (r) => r.exchange == "%s")
                  |> filter(fn: (r) => r.dataType == "OHLC")
                  |> aggregateWindow(every: %s, fn: mean)
                  |> yield(name: "ohlc")
                """, bucket, start, end, symbol, exchange, interval);
            
            var tables = queryApi.query(flux, organization);
            
            return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(record -> MarketDataPoint.builder()
                    .symbol(record.getValueByKey("symbol").toString())
                    .exchange(record.getValueByKey("exchange").toString())
                    .dataType("OHLC")
                    .price(new BigDecimal(record.getValue().toString()))
                    .timestamp((Instant) record.getTime())
                    .build())
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to get OHLC data for {}:{}: {}", 
                symbol, exchange, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Get real-time tick data for the last hour
     */
    public List<MarketDataPoint> getRecentTickData(String symbol, String exchange, int minutes) {
        try {
            String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: -%dm)
                  |> filter(fn: (r) => r._measurement == "market_data")
                  |> filter(fn: (r) => r.symbol == "%s")
                  |> filter(fn: (r) => r.exchange == "%s")
                  |> filter(fn: (r) => r.dataType == "TICK")
                  |> sort(columns: ["_time"])
                """, bucket, minutes, symbol, exchange);
            
            var tables = queryApi.query(flux, organization);
            
            return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(record -> MarketDataPoint.builder()
                    .symbol(record.getValueByKey("symbol").toString())
                    .exchange(record.getValueByKey("exchange").toString())
                    .dataType("TICK")
                    .price(new BigDecimal(record.getValue().toString()))
                    .volume(Long.valueOf(record.getValueByKey("volume").toString()))
                    .timestamp((Instant) record.getTime())
                    .build())
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to get tick data for {}:{}: {}", 
                symbol, exchange, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Get symbols with recent activity
     */
    public List<String> getActiveSymbols(String exchange, int minutes) {
        try {
            String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: -%dm)
                  |> filter(fn: (r) => r._measurement == "market_data")
                  |> filter(fn: (r) => r.exchange == "%s")
                  |> group(columns: ["symbol"])
                  |> count()
                  |> filter(fn: (r) => r._value > 0)
                """, bucket, minutes, exchange);
            
            var tables = queryApi.query(flux, organization);
            
            return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(record -> record.getValueByKey("symbol").toString())
                .distinct()
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to get active symbols for {}: {}", exchange, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Data quality monitoring - detect gaps and anomalies
     */
    public DataQualityReport generateQualityReport(String symbol, String exchange, int hours) {
        try {
            Instant start = Instant.now().minus(hours, ChronoUnit.HOURS);
            
            // Count total records
            String countFlux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s)
                  |> filter(fn: (r) => r._measurement == "market_data")
                  |> filter(fn: (r) => r.symbol == "%s")
                  |> filter(fn: (r) => r.exchange == "%s")
                  |> count()
                """, bucket, start, symbol, exchange);
            
            var countResult = queryApi.query(countFlux, organization);
            long totalRecords = countResult.stream()
                .flatMap(table -> table.getRecords().stream())
                .findFirst()
                .map(record -> Long.valueOf(record.getValue().toString()))
                .orElse(0L);
            
            // Detect data gaps
            String gapFlux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s)
                  |> filter(fn: (r) => r._measurement == "market_data")
                  |> filter(fn: (r) => r.symbol == "%s")
                  |> filter(fn: (r) => r.exchange == "%s")
                  |> sort(columns: ["_time"])
                  |> difference(nonNegative: false, columns: ["_time"])
                  |> filter(fn: (r) => r._value > 300000) // Gaps > 5 minutes
                """, bucket, start, symbol, exchange);
            
            var gapResult = queryApi.query(gapFlux, organization);
            long dataGaps = gapResult.stream()
                .flatMap(table -> table.getRecords().stream())
                .count();
            
            double qualityScore = totalRecords > 0 ? 
                Math.max(0.0, 1.0 - (dataGaps * 0.1)) : 0.0;
            
            return new DataQualityReport(
                symbol, 
                exchange, 
                totalRecords, 
                dataGaps, 
                qualityScore, 
                Instant.now()
            );
            
        } catch (Exception e) {
            log.error("Failed to generate quality report for {}:{}: {}", 
                symbol, exchange, e.getMessage());
            return new DataQualityReport(symbol, exchange, 0L, 0L, 0.0, Instant.now());
        }
    }
    
    /**
     * Write result sealed interface
     */
    public sealed interface WriteResult permits WriteResult.Success, WriteResult.Failed {
        
        record Success(int recordsWritten, Instant timestamp) implements WriteResult {}
        
        record Failed(String error) implements WriteResult {}
        
        default boolean isSuccess() {
            return this instanceof Success;
        }
        
        default boolean isFailed() {
            return this instanceof Failed;
        }
    }
    
    /**
     * Data quality report
     */
    public record DataQualityReport(
        String symbol,
        String exchange,
        long totalRecords,
        long dataGaps,
        double qualityScore,
        Instant generatedAt
    ) {
        public QualityLevel getQualityLevel() {
            return switch (Double.compare(qualityScore, 0.8)) {
                case 1, 0 -> QualityLevel.HIGH;
                case -1 -> Double.compare(qualityScore, 0.5) >= 0 
                          ? QualityLevel.MEDIUM 
                          : QualityLevel.LOW;
                default -> QualityLevel.LOW;
            };
        }
        
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
    }
}