package com.trademaster.marketdata.pattern;

import com.trademaster.common.functional.Result;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.dto.MarketDataRequest;
import com.trademaster.marketdata.dto.MarketDataResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy pattern implementations for market data operations
 * Eliminates if-else logic with functional strategy selection
 */
public interface MarketDataStrategy {
    
    // Data source strategy
    enum DataSourceStrategy {
        REAL_TIME(request -> fetchRealTimeData(request)),
        CACHED(request -> fetchCachedData(request)),
        DELAYED(request -> fetchDelayedData(request)),
        FALLBACK(request -> fetchFallbackData(request)),
        SIMULATION(request -> fetchSimulatedData(request));
        
        private final Function<MarketDataRequest, CompletableFuture<MarketDataResponse>> fetcher;
        
        DataSourceStrategy(Function<MarketDataRequest, CompletableFuture<MarketDataResponse>> fetcher) {
            this.fetcher = fetcher;
        }
        
        public CompletableFuture<MarketDataResponse> fetch(MarketDataRequest request) {
            return fetcher.apply(request);
        }
        
        // Strategy selection based on request properties
        public static DataSourceStrategy selectFor(MarketDataRequest request) {
            return switch (request.priority()) {
                case REAL_TIME -> REAL_TIME;
                case HIGH -> CACHED;
                case NORMAL -> DELAYED;
                case LOW -> FALLBACK;
            };
        }
        
        private static CompletableFuture<MarketDataResponse> fetchRealTimeData(MarketDataRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                // Real-time data fetching logic
                return MarketDataResponse.success(Map.of("source", "real-time"));
            });
        }
        
        private static CompletableFuture<MarketDataResponse> fetchCachedData(MarketDataRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                // Cached data fetching logic
                return MarketDataResponse.success(Map.of("source", "cached"));
            });
        }
        
        private static CompletableFuture<MarketDataResponse> fetchDelayedData(MarketDataRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                // Delayed data fetching logic
                return MarketDataResponse.success(Map.of("source", "delayed"));
            });
        }
        
        private static CompletableFuture<MarketDataResponse> fetchFallbackData(MarketDataRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                // Fallback data fetching logic
                return MarketDataResponse.success(Map.of("source", "fallback"));
            });
        }
        
        private static CompletableFuture<MarketDataResponse> fetchSimulatedData(MarketDataRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                // Simulated data for testing
                return MarketDataResponse.success(Map.of("source", "simulation"));
            });
        }
    }
    
    // Price calculation strategy
    enum PriceCalculationStrategy {
        LAST_TRADE(data -> data.stream()
            .map(MarketDataPoint::price)
            .reduce((first, second) -> second)
            .orElse(BigDecimal.ZERO)),
            
        VOLUME_WEIGHTED(data -> calculateVWAP(data)),
        
        TIME_WEIGHTED(data -> calculateTWAP(data)),
        
        MEDIAN(data -> calculateMedian(data)),
        
        AVERAGE(data -> data.stream()
            .map(MarketDataPoint::price)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(data.size()), java.math.MathContext.DECIMAL128));
        
        private final Function<List<MarketDataPoint>, BigDecimal> calculator;
        
        PriceCalculationStrategy(Function<List<MarketDataPoint>, BigDecimal> calculator) {
            this.calculator = calculator;
        }
        
        public BigDecimal calculate(List<MarketDataPoint> data) {
            return calculator.apply(data);
        }
        
        // Strategy selection based on data characteristics
        public static PriceCalculationStrategy selectFor(List<MarketDataPoint> data, String context) {
            return switch (context.toUpperCase()) {
                case "EXECUTION" -> VOLUME_WEIGHTED;
                case "BENCHMARK" -> TIME_WEIGHTED;
                case "CLOSING" -> LAST_TRADE;
                case "AVERAGE" -> AVERAGE;
                default -> data.size() > 100 ? VOLUME_WEIGHTED : LAST_TRADE;
            };
        }
        
        private static BigDecimal calculateVWAP(List<MarketDataPoint> data) {
            if (data.isEmpty()) return BigDecimal.ZERO;
            
            BigDecimal totalVolumePrice = data.stream()
                .map(point -> point.price().multiply(new BigDecimal(point.volume())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal totalVolume = data.stream()
                .map(point -> new BigDecimal(point.volume()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            return totalVolume.compareTo(BigDecimal.ZERO) > 0 ?
                totalVolumePrice.divide(totalVolume, java.math.MathContext.DECIMAL128) :
                BigDecimal.ZERO;
        }
        
        private static BigDecimal calculateTWAP(List<MarketDataPoint> data) {
            if (data.isEmpty()) return BigDecimal.ZERO;
            
            // Simple TWAP - equal weighting for each time period
            return data.stream()
                .map(MarketDataPoint::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(data.size()), java.math.MathContext.DECIMAL128);
        }
        
        private static BigDecimal calculateMedian(List<MarketDataPoint> data) {
            if (data.isEmpty()) return BigDecimal.ZERO;
            
            List<BigDecimal> prices = data.stream()
                .map(MarketDataPoint::price)
                .sorted()
                .toList();
                
            int size = prices.size();
            if (size % 2 == 1) {
                return prices.get(size / 2);
            } else {
                BigDecimal mid1 = prices.get(size / 2 - 1);
                BigDecimal mid2 = prices.get(size / 2);
                return mid1.add(mid2).divide(new BigDecimal("2"), java.math.MathContext.DECIMAL128);
            }
        }
    }
    
    // Validation strategy
    enum ValidationStrategy {
        STRICT(data -> validateStrict(data)),
        LENIENT(data -> validateLenient(data)),
        BASIC(data -> validateBasic(data)),
        NONE(data -> Result.success(data));
        
        private final Function<MarketDataPoint, Result<MarketDataPoint, String>> validator;
        
        ValidationStrategy(Function<MarketDataPoint, Result<MarketDataPoint, String>> validator) {
            this.validator = validator;
        }
        
        public Result<MarketDataPoint, String> validate(MarketDataPoint data) {
            return validator.apply(data);
        }
        
        public static ValidationStrategy selectFor(String environment, String dataSource) {
            return switch (environment.toUpperCase()) {
                case "PRODUCTION" -> dataSource.equals("EXTERNAL") ? STRICT : LENIENT;
                case "STAGING" -> LENIENT;
                case "DEVELOPMENT" -> BASIC;
                case "TEST" -> NONE;
                default -> STRICT;
            };
        }
        
        private static Result<MarketDataPoint, String> validateStrict(MarketDataPoint data) {
            return ValidationChain.<MarketDataPoint>builder()
                .notNull("Market data cannot be null")
                .add(point -> point.price() != null, "Price cannot be null")
                .add(point -> point.price().compareTo(BigDecimal.ZERO) > 0, "Price must be positive")
                .add(point -> point.volume() != null && point.volume() >= 0, "Volume must be non-negative")
                .add(point -> point.symbol() != null && !point.symbol().isBlank(), "Symbol cannot be blank")
                .add(point -> point.timestamp() != null, "Timestamp cannot be null")
                .build()
                .validate(data);
        }
        
        private static Result<MarketDataPoint, String> validateLenient(MarketDataPoint data) {
            return ValidationChain.<MarketDataPoint>builder()
                .notNull("Market data cannot be null")
                .add(point -> point.price() != null && point.price().compareTo(BigDecimal.ZERO) >= 0, 
                     "Price must be non-negative")
                .add(point -> point.symbol() != null && !point.symbol().isBlank(), 
                     "Symbol cannot be blank")
                .build()
                .validate(data);
        }
        
        private static Result<MarketDataPoint, String> validateBasic(MarketDataPoint data) {
            return ValidationChain.<MarketDataPoint>builder()
                .notNull("Market data cannot be null")
                .add(point -> point.price() != null, "Price cannot be null")
                .add(point -> point.symbol() != null, "Symbol cannot be null")
                .build()
                .validate(data);
        }
    }
    
    // Aggregation strategy
    enum AggregationStrategy {
        SUM(values -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)),
        AVERAGE(values -> {
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(new BigDecimal(values.size()), java.math.MathContext.DECIMAL128);
        }),
        MAX(values -> values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)),
        MIN(values -> values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO)),
        WEIGHTED_AVERAGE(values -> calculateWeightedAverage(values)),
        GEOMETRIC_MEAN(values -> calculateGeometricMean(values));
        
        private final Function<List<BigDecimal>, BigDecimal> aggregator;
        
        AggregationStrategy(Function<List<BigDecimal>, BigDecimal> aggregator) {
            this.aggregator = aggregator;
        }
        
        public BigDecimal aggregate(List<BigDecimal> values) {
            return values.isEmpty() ? BigDecimal.ZERO : aggregator.apply(values);
        }
        
        private static BigDecimal calculateWeightedAverage(List<BigDecimal> values) {
            // Simple equal weighting for now - could be enhanced with actual weights
            return AVERAGE.aggregate(values);
        }
        
        private static BigDecimal calculateGeometricMean(List<BigDecimal> values) {
            if (values.isEmpty()) return BigDecimal.ZERO;
            
            double product = values.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .reduce(1.0, (a, b) -> a * b);
                
            return new BigDecimal(Math.pow(product, 1.0 / values.size()));
        }
    }
}