package com.trademaster.portfolio.service;

import com.trademaster.portfolio.functional.Result;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Benchmark Data Service Interface (Future Integration)
 * 
 * Defines contract for benchmark data retrieval and analysis.
 * Supports major Indian market indices and international benchmarks.
 * 
 * Features:
 * - Real-time benchmark prices
 * - Historical benchmark performance
 * - Index composition data
 * - Sector allocation information
 * - Performance attribution analysis
 * 
 * Supported Benchmarks:
 * - NIFTY 50, NIFTY 100, NIFTY 500
 * - SENSEX, BSE 100, BSE 500
 * - Sectoral indices (Banking, IT, Pharma, etc.)
 * - International indices (S&P 500, NASDAQ, etc.)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Service Interface)
 */
public interface BenchmarkDataService {
    
    /**
     * Get current benchmark value
     */
    CompletableFuture<Result<BenchmarkData, BenchmarkDataError>> 
        getCurrentBenchmarkData(String benchmarkSymbol);
    
    /**
     * Get multiple benchmark data
     */
    CompletableFuture<Result<List<BenchmarkData>, BenchmarkDataError>> 
        getMultipleBenchmarkData(List<String> benchmarkSymbols);
    
    /**
     * Get historical benchmark performance
     */
    CompletableFuture<Result<List<BenchmarkHistoryData>, BenchmarkDataError>> 
        getBenchmarkHistory(String benchmarkSymbol, String period);
    
    /**
     * Get benchmark composition (constituent stocks)
     */
    CompletableFuture<Result<BenchmarkComposition, BenchmarkDataError>> 
        getBenchmarkComposition(String benchmarkSymbol);
    
    /**
     * Get benchmark sector allocation
     */
    CompletableFuture<Result<List<SectorAllocation>, BenchmarkDataError>> 
        getBenchmarkSectorAllocation(String benchmarkSymbol);
    
    /**
     * Calculate benchmark statistics for a period
     */
    CompletableFuture<Result<BenchmarkStatistics, BenchmarkDataError>> 
        getBenchmarkStatistics(String benchmarkSymbol, String period);
    
    /**
     * Get list of available benchmarks
     */
    CompletableFuture<Result<List<BenchmarkInfo>, BenchmarkDataError>> 
        getAvailableBenchmarks();
    
    /**
     * Current benchmark data
     */
    record BenchmarkData(
        String symbol,
        String name,
        BigDecimal currentValue,
        BigDecimal dayChange,
        BigDecimal dayChangePercent,
        BigDecimal high52Week,
        BigDecimal low52Week,
        java.time.Instant timestamp
    ) {}
    
    /**
     * Historical benchmark data point
     */
    record BenchmarkHistoryData(
        String symbol,
        BigDecimal value,
        BigDecimal change,
        BigDecimal changePercent,
        Long volume,
        java.time.LocalDate date
    ) {}
    
    /**
     * Benchmark composition data
     */
    record BenchmarkComposition(
        String benchmarkSymbol,
        String benchmarkName,
        List<ConstituentStock> constituents,
        java.time.LocalDate lastUpdated
    ) {}
    
    /**
     * Constituent stock in benchmark
     */
    record ConstituentStock(
        String symbol,
        String companyName,
        BigDecimal weight,
        String sector,
        BigDecimal marketCap,
        Integer rank
    ) {}
    
    /**
     * Sector allocation in benchmark
     */
    record SectorAllocation(
        String sector,
        BigDecimal weight,
        BigDecimal returns1Y,
        Integer numberOfStocks
    ) {}
    
    /**
     * Benchmark statistics
     */
    record BenchmarkStatistics(
        String symbol,
        String period,
        BigDecimal totalReturn,
        BigDecimal annualizedReturn,
        BigDecimal volatility,
        BigDecimal maxDrawdown,
        BigDecimal sharpeRatio,
        BigDecimal beta,
        java.time.LocalDate startDate,
        java.time.LocalDate endDate
    ) {}
    
    /**
     * Benchmark information
     */
    record BenchmarkInfo(
        String symbol,
        String name,
        String description,
        String category, // EQUITY, DEBT, COMMODITY, etc.
        String geography, // INDIA, US, GLOBAL, etc.
        boolean active
    ) {}
    
    /**
     * Benchmark data error types
     */
    sealed interface BenchmarkDataError {
        
        record BenchmarkNotFound(String symbol) implements BenchmarkDataError {}
        
        record DataProviderUnavailable(String provider) implements BenchmarkDataError {}
        
        record InvalidPeriod(String period) implements BenchmarkDataError {}
        
        record InsufficientData(String symbol, String period) implements BenchmarkDataError {}
        
        record RateLimitExceeded(String message) implements BenchmarkDataError {}
        
        record ServiceError(String message, Throwable cause) implements BenchmarkDataError {}
    }
}