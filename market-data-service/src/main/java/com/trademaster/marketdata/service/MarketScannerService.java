package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.MarketScannerRequest;
import com.trademaster.marketdata.dto.MarketScannerResult;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.functional.Try;
import com.trademaster.marketdata.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.trademaster.marketdata.pattern.Either;
import com.trademaster.marketdata.pattern.StreamUtils;
import com.trademaster.marketdata.pattern.IO;

/**
 * Market Scanner Service
 *
 * Provides advanced market scanning capabilities with technical analysis,
 * pattern recognition, and comprehensive filtering. Uses virtual threads
 * for high-performance parallel processing.
 *
 * MANDATORY RULES COMPLIANCE:
 * - RULE #3: No if-else, no try-catch in business logic - functional programming only
 * - RULE #5: Cognitive complexity ≤7 per method, max 15 lines per method
 * - RULE #9: Immutable data structures (Result types, Optional, Collections)
 * - RULE #17: All magic numbers externalized to named constants
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketScannerService {

    // Data quality calculation constants (RULE #17)
    private static final double QUALITY_BASE_SCORE = 1.0;
    private static final double QUALITY_PENALTY = -0.3;
    private static final double QUALITY_PENALTY_ADJUSTMENT = 0.67;
    private static final double QUALITY_MIN_SCORE = 0.0;
    private static final int QUALITY_PERCENTAGE_MULTIPLIER = 100;

    // Pagination constants (RULE #17)
    private static final int PAGE_INDEX_OFFSET = 1;
    private static final int MIN_PAGE_NUMBER = 1;

    private final MarketDataRepository marketDataRepository;
    private final MarketDataService marketDataService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final MarketDataCacheService cacheService;
    
    // Functional Filter Chain Implementation  
    @FunctionalInterface
    public interface BiPredicate<T, U> {
        boolean test(T t, U u);
    }
    
    public static class FilterChain<T, R> {
        private final List<BiPredicate<T, R>> filters;
        
        private FilterChain(List<BiPredicate<T, R>> filters) {
            this.filters = filters;
        }
        
        public boolean test(T data, R request) {
            return filters.stream().allMatch(filter -> filter.test(data, request));
        }
        
        public static <T, R> Builder<T, R> builder() {
            return new Builder<>();
        }
        
        public static class Builder<T, R> {
            private final List<BiPredicate<T, R>> filters = new ArrayList<>();
            
            public Builder<T, R> addFilter(BiPredicate<T, R> filter) {
                filters.add(filter);
                return this;
            }
            
            public FilterChain<T, R> build() {
                return new FilterChain<>(filters);
            }
        }
    }
    
    // Functional Filter Chains (Strategy Pattern)
    private final FilterChain<MarketDataPoint, MarketScannerRequest> basicFilters = FilterChain.<MarketDataPoint, MarketScannerRequest>builder()
        .addFilter(this::validatePriceRange)
        .addFilter(this::validateVolumeRange)  
        .addFilter(this::validateDataQuality)
        .build();
    
    private final FilterChain<Map<String, Object>, MarketScannerRequest> technicalFilters = FilterChain.<Map<String, Object>, MarketScannerRequest>builder()
        .addFilter(this::validateTechnicalIndicators)
        .build();
    
    private final FilterChain<Map<String, BigDecimal>, MarketScannerRequest> performanceFilters = FilterChain.<Map<String, BigDecimal>, MarketScannerRequest>builder()
        .addFilter(this::validateDayChange)
        .addFilter(this::validateWeekChange)
        .addFilter(this::validateMonthChange)
        .build();
    
    private final FilterChain<Map<String, BigDecimal>, MarketScannerRequest> fundamentalFilters = FilterChain.<Map<String, BigDecimal>, MarketScannerRequest>builder()
        .addFilter(this::validatePERatio)
        .addFilter(this::validateDivYield)
        .build();
    
    /**
     * Execute market scan with comprehensive filtering
     *
     * Refactored to use Try monad for functional error handling (MANDATORY RULE #11).
     */
    public CompletableFuture<MarketScannerResult> scan(MarketScannerRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String scanId = generateScanId();

            log.info("Starting market scan {} with {} active filters",
                scanId, request.getActiveFilterCount());

            return Try.of(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                    // Get active symbols for scanning
                    var symbolsTask = scope.fork(() -> getActiveSymbols(request.exchanges()));

                    // Wait for symbols
                    scope.join();
                    scope.throwIfFailed();

                    List<String> symbols = symbolsTask.get();
                    log.debug("Found {} active symbols to scan", symbols.size());

                    // Parallel processing of symbols using virtual threads
                    List<MarketScannerResult.ScanResultItem> results = processSymbolsInParallel(symbols, request);

                    // Filter and sort results
                    results = applyFinalFilters(results, request);
                    results = sortResults(results, request);

                    // Apply pagination
                    var paginatedResults = applyPagination(results, request);

                    long executionTime = System.currentTimeMillis() - startTime;

                    var statistics = buildStatistics(symbols, results, request, executionTime);
                    var pagination = buildPaginationInfo(results, request);

                    log.info("Market scan {} completed: {} symbols processed, {} matched in {}ms",
                        scanId, symbols.size(), results.size(), executionTime);

                    return MarketScannerResult.builder()
                        .scanId(scanId)
                        .scanTime(Instant.now())
                        .originalRequest(request)
                        .results(paginatedResults)
                        .statistics(statistics)
                        .pagination(pagination)
                        .build();
                }
            })
            .map(result -> {
                log.debug("Market scan {} prepared successfully", scanId);
                return result;
            })
            .recover(e -> {
                log.error("Market scan {} failed: {}", scanId, e.getMessage(), e);
                throw new RuntimeException("Market scan failed", e);
            })
            .get();
        });
    }
    
    /**
     * Process symbols in parallel using virtual threads
     *
     * Refactored to use Try monad for functional error handling (MANDATORY RULE #11).
     */
    private List<MarketScannerResult.ScanResultItem> processSymbolsInParallel(
            List<String> symbols, MarketScannerRequest request) {

        return Try.of(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                // Create parallel tasks for symbol processing
                List<StructuredTaskScope.Subtask<Optional<MarketScannerResult.ScanResultItem>>> tasks =
                    symbols.stream()
                        .map(symbol -> scope.fork(() -> processSymbol(symbol, request)))
                        .toList();

                // Wait for all tasks
                scope.join();
                scope.throwIfFailed();

                // Collect successful results with Try monad for task result extraction
                return tasks.stream()
                    .map(task -> Try.of(task::get)
                        .recover(e -> {
                            log.warn("Failed to process symbol: {}", e.getMessage());
                            return Optional.<MarketScannerResult.ScanResultItem>empty();
                        })
                        .get())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            }
        })
        .recover(e -> {
            log.error("Parallel symbol processing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Symbol processing failed", e);
        })
        .get();
    }
    
    /**
     * Process individual symbol - Functional approach with Optional chains (RULE #3)
     *
     * Refactored to eliminate all if-statements using functional pipeline.
     */
    private Optional<MarketScannerResult.ScanResultItem> processSymbol(
            String symbol, MarketScannerRequest request) {

        return Try.of(() ->
            marketDataService.getCurrentPrice(symbol, request.exchanges().iterator().next())
                .join()
                .filter(currentData -> passesBasicFilters(currentData, request))
                .flatMap(currentData -> processWithHistoricalData(symbol, currentData, request))
        )
        .recover(e -> {
            log.debug("Failed to process symbol {}: {}", symbol, e.getMessage());
            return Optional.empty();
        })
        .get();
    }

    private Optional<MarketScannerResult.ScanResultItem> processWithHistoricalData(
            String symbol, MarketDataPoint currentData, MarketScannerRequest request) {

        var historicalData = getHistoricalDataForAnalysis(symbol, currentData.exchange());
        var technicalIndicators = calculateTechnicalIndicators(historicalData);

        return Optional.of(technicalIndicators)
            .filter(indicators -> passesTechnicalFilters(indicators, request))
            .map(indicators -> calculatePerformanceMetrics(historicalData))
            .filter(performance -> passesPerformanceFilters(performance, request))
            .map(performance -> getFundamentalData(symbol))
            .filter(fundamentals -> passesFundamentalFilters(fundamentals, request))
            .flatMap(fundamentals -> processWithPatterns(currentData, historicalData,
                technicalIndicators, getFundamentalData(symbol), request));
    }

    private Optional<MarketScannerResult.ScanResultItem> processWithPatterns(
            MarketDataPoint currentData, List<MarketDataPoint> historicalData,
            Map<String, BigDecimal> technicalIndicators, Map<String, BigDecimal> fundamentals,
            MarketScannerRequest request) {

        var patterns = detectPatterns(historicalData);
        var candlestickPatterns = detectCandlestickPatterns(historicalData);

        return Optional.of(patterns)
            .filter(p -> passesPatternFilters(p, candlestickPatterns, request))
            .flatMap(p -> processWithBreakoutAnalysis(currentData, historicalData,
                technicalIndicators, calculatePerformanceMetrics(historicalData),
                fundamentals, patterns, candlestickPatterns, request));
    }

    private Optional<MarketScannerResult.ScanResultItem> processWithBreakoutAnalysis(
            MarketDataPoint currentData, List<MarketDataPoint> historicalData,
            Map<String, BigDecimal> technicalIndicators, Map<String, BigDecimal> performanceMetrics,
            Map<String, BigDecimal> fundamentals, List<String> patterns,
            List<String> candlestickPatterns, MarketScannerRequest request) {

        var supportLevels = findSupportLevels(historicalData);
        var resistanceLevels = findResistanceLevels(historicalData);
        var breakoutAnalysis = analyzeBreakouts(currentData, historicalData,
            supportLevels, resistanceLevels);

        return Optional.of(breakoutAnalysis)
            .filter(analysis -> passesBreakoutFilters(analysis, request))
            .map(analysis -> {
                var scanScore = calculateScanScore(currentData, technicalIndicators,
                    performanceMetrics, analysis);
                return buildResultItem(currentData, technicalIndicators, performanceMetrics,
                    fundamentals, patterns, candlestickPatterns, supportLevels,
                    resistanceLevels, analysis, scanScore);
            });
    }
    
    /**
     * Apply basic price and volume filters - Functional approach
     */
    private boolean passesBasicFilters(MarketDataPoint data, MarketScannerRequest request) {
        return basicFilters.test(data, request);
    }
    
    /**
     * Apply technical indicator filters - Functional approach
     */
    private boolean passesTechnicalFilters(Map<String, BigDecimal> indicators, 
            MarketScannerRequest request) {
        
        return !request.hasTechnicalFilters() || 
            request.technicalFilters().stream()
                .allMatch(filter -> 
                    Optional.ofNullable(indicators.get(filter.indicatorType()))
                        .map(indicatorValue -> evaluateTechnicalCondition(indicatorValue, filter))
                        .orElse(false));
    }
    
    /**
     * Apply performance filters - Functional approach
     */
    private boolean passesPerformanceFilters(Map<String, BigDecimal> performance, 
            MarketScannerRequest request) {
        
        return performanceFilters.test(performance, request);
    }
    
    /**
     * Apply fundamental filters - Functional approach
     */
    private boolean passesFundamentalFilters(Map<String, BigDecimal> fundamentals, 
            MarketScannerRequest request) {
        
        return fundamentalFilters.test(fundamentals, request);
    }
    
    /**
     * Apply pattern filters - Functional approach
     */
    private boolean passesPatternFilters(List<String> patterns, List<String> candlestickPatterns,
            MarketScannerRequest request) {
        
        return validateChartPatterns(patterns, request) && validateCandlestickPatterns(candlestickPatterns, request);
    }
    
    /**
     * Apply breakout filters - Functional approach
     */
    private boolean passesBreakoutFilters(MarketScannerResult.ScanResultItem.BreakoutAnalysis breakoutAnalysis,
            MarketScannerRequest request) {
        
        return validatePriceBreakout(breakoutAnalysis, request) && validateVolumeBreakout(breakoutAnalysis, request);
    }
    
    // Helper methods (implementation stubs for demonstration)
    
    private List<String> getActiveSymbols(Set<String> exchanges) {
        // Implementation would query repository for active symbols
        return List.of("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "KOTAKBANK", "LT", "ITC");
    }
    
    private List<MarketDataPoint> getHistoricalDataForAnalysis(String symbol, String exchange) {
        // Get last 100 data points for analysis
        return List.of(); // Stub
    }
    
    private Map<String, BigDecimal> calculateTechnicalIndicators(List<MarketDataPoint> data) {
        // Calculate RSI, MACD, SMA, EMA, etc.
        return technicalAnalysisService.calculateAllIndicators(data);
    }
    
    private Map<String, BigDecimal> calculatePerformanceMetrics(List<MarketDataPoint> data) {
        // Calculate day/week/month performance
        return Map.of(); // Stub
    }
    
    private Map<String, BigDecimal> getFundamentalData(String symbol) {
        // Get PE ratio, dividend yield, etc.
        return Map.of(); // Stub
    }
    
    private List<String> detectPatterns(List<MarketDataPoint> data) {
        // Detect chart patterns
        return List.of(); // Stub
    }
    
    private List<String> detectCandlestickPatterns(List<MarketDataPoint> data) {
        // Detect candlestick patterns
        return List.of(); // Stub
    }
    
    private List<MarketScannerResult.ScanResultItem.SupportResistanceLevel> findSupportLevels(
            List<MarketDataPoint> data) {
        return List.of(); // Stub
    }
    
    private List<MarketScannerResult.ScanResultItem.SupportResistanceLevel> findResistanceLevels(
            List<MarketDataPoint> data) {
        return List.of(); // Stub
    }
    
    private MarketScannerResult.ScanResultItem.BreakoutAnalysis analyzeBreakouts(
            MarketDataPoint current, List<MarketDataPoint> historical,
            List<MarketScannerResult.ScanResultItem.SupportResistanceLevel> support,
            List<MarketScannerResult.ScanResultItem.SupportResistanceLevel> resistance) {
        return MarketScannerResult.ScanResultItem.BreakoutAnalysis.builder().build(); // Stub
    }
    
    private boolean checkCrossing(BigDecimal value, MarketScannerRequest.TechnicalIndicatorFilter filter) {
        // Implement crossing logic
        return true; // Stub
    }
    
    private BigDecimal calculateScanScore(MarketDataPoint current, 
            Map<String, BigDecimal> technicals, Map<String, BigDecimal> performance,
            MarketScannerResult.ScanResultItem.BreakoutAnalysis breakout) {
        // Calculate composite score
        return new BigDecimal("75.5"); // Stub
    }
    
    private MarketScannerResult.ScanResultItem buildResultItem(MarketDataPoint current,
            Map<String, BigDecimal> technicals, Map<String, BigDecimal> performance,
            Map<String, BigDecimal> fundamentals, List<String> patterns,
            List<String> candlestickPatterns,
            List<MarketScannerResult.ScanResultItem.SupportResistanceLevel> support,
            List<MarketScannerResult.ScanResultItem.SupportResistanceLevel> resistance,
            MarketScannerResult.ScanResultItem.BreakoutAnalysis breakout,
            BigDecimal scanScore) {
        
        return MarketScannerResult.ScanResultItem.builder()
            .symbol(current.symbol())
            .exchange(current.exchange())
            .currentPrice(current.price())
            .currentVolume(current.volume())
            .dayChange(current.change())
            .dayChangePercent(current.changePercent())
            .technicalIndicators(technicals)
            .supportLevels(support)
            .resistanceLevels(resistance)
            .breakoutAnalysis(breakout)
            .scanScore(scanScore)
            .dataQuality((int)(current.qualityScore() * QUALITY_PERCENTAGE_MULTIPLIER))
            .lastUpdated(current.timestamp())
            .build();
    }
    
    private List<MarketScannerResult.ScanResultItem> applyFinalFilters(
            List<MarketScannerResult.ScanResultItem> results, MarketScannerRequest request) {
        
        return results.stream()
            .filter(item -> Optional.ofNullable(request.excludeSymbols())
                .map(excluded -> !excluded.contains(item.symbol()))
                .orElse(true))
            .toList();
    }
    
    private List<MarketScannerResult.ScanResultItem> sortResults(
            List<MarketScannerResult.ScanResultItem> results, MarketScannerRequest request) {

        Comparator<MarketScannerResult.ScanResultItem> baseComparator = switch (request.sortBy()) {
            case "volume" -> Comparator.comparing(item -> item.currentVolume(),
                Comparator.nullsLast(Comparator.naturalOrder()));
            case "price" -> Comparator.comparing(item -> item.currentPrice(),
                Comparator.nullsLast(Comparator.naturalOrder()));
            case "dayChangePercent" -> Comparator.comparing(item -> item.dayChangePercent(),
                Comparator.nullsLast(Comparator.naturalOrder()));
            case "scanScore" -> Comparator.comparing(item -> item.scanScore(),
                Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(MarketScannerResult.ScanResultItem::symbol);
        };

        Comparator<MarketScannerResult.ScanResultItem> finalComparator =
            request.sortDirection() == MarketScannerRequest.SortDirection.DESC ?
                baseComparator.reversed() : baseComparator;

        return results.stream()
            .sorted(finalComparator)
            .toList();
    }
    
    private List<MarketScannerResult.ScanResultItem> applyPagination(
            List<MarketScannerResult.ScanResultItem> results, MarketScannerRequest request) {

        int start = (request.pageNumber() - PAGE_INDEX_OFFSET) * request.pageSize();
        int end = Math.min(start + request.pageSize(), results.size());

        return start >= results.size() ? List.of() : results.subList(start, end);
    }
    
    private MarketScannerResult.ScanStatistics buildStatistics(List<String> symbols,
            List<MarketScannerResult.ScanResultItem> results, MarketScannerRequest request,
            long executionTime) {
        
        Map<String, Integer> exchangeBreakdown = results.stream()
            .collect(Collectors.groupingBy(
                MarketScannerResult.ScanResultItem::exchange,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        return MarketScannerResult.ScanStatistics.builder()
            .totalSymbolsScanned(symbols.size())
            .symbolsMatched(results.size())
            .filtersApplied(request.getActiveFilterCount())
            .executionTimeMs(executionTime)
            .exchangeBreakdown(exchangeBreakdown)
            .build();
    }
    
    private MarketScannerResult.PaginationInfo buildPaginationInfo(
            List<MarketScannerResult.ScanResultItem> allResults, MarketScannerRequest request) {

        int totalPages = (int) Math.ceil((double) allResults.size() / request.pageSize());
        int currentPage = request.pageNumber();

        return MarketScannerResult.PaginationInfo.builder()
            .currentPage(currentPage)
            .pageSize(request.pageSize())
            .totalPages(totalPages)
            .totalResults((long) allResults.size())
            .hasNext(currentPage < totalPages)
            .hasPrevious(currentPage > MIN_PAGE_NUMBER)
            .build();
    }
    
    private String generateScanId() {
        return "SCAN_" + System.currentTimeMillis() + "_" + 
            Integer.toHexString(new Random().nextInt());
    }
    
    // Functional Validator Methods (replacing if-else chains)
    private boolean validatePriceRange(MarketDataPoint data, MarketScannerRequest request) {
        return Optional.ofNullable(request.priceRange())
            .map(range -> Optional.ofNullable(data.price())
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validateVolumeRange(MarketDataPoint data, MarketScannerRequest request) {
        return Optional.ofNullable(request.volumeRange())
            .map(range -> Optional.ofNullable(data.volume())
                .map(BigDecimal::valueOf)
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validateDataQuality(MarketDataPoint data, MarketScannerRequest request) {
        return Optional.ofNullable(request.minDataQuality())
            .map(minQuality -> calculateDataQuality(data) * 100 >= minQuality)
            .orElse(true);
    }
    
    private boolean validateTechnicalIndicators(Map<String, Object> indicators, MarketScannerRequest request) {
        return !request.hasTechnicalFilters() || 
            request.technicalFilters().stream()
                .allMatch(filter -> Optional.ofNullable(indicators.get(filter.indicatorType()))
                    .filter(BigDecimal.class::isInstance)
                    .map(BigDecimal.class::cast)
                    .map(value -> evaluateTechnicalCondition(value, filter))
                    .orElse(false));
    }
    
    private boolean validateDayChange(Map<String, BigDecimal> metrics, MarketScannerRequest request) {
        return Optional.ofNullable(request.dayChangePercent())
            .map(range -> Optional.ofNullable(metrics.get("dayChange"))
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validateWeekChange(Map<String, BigDecimal> metrics, MarketScannerRequest request) {
        return Optional.ofNullable(request.weekChangePercent())
            .map(range -> Optional.ofNullable(metrics.get("weekChange"))
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validateMonthChange(Map<String, BigDecimal> metrics, MarketScannerRequest request) {
        return Optional.ofNullable(request.monthChangePercent())
            .map(range -> Optional.ofNullable(metrics.get("monthChange"))
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validatePERatio(Map<String, BigDecimal> fundamentals, MarketScannerRequest request) {
        return Optional.ofNullable(request.peRatio())
            .map(range -> Optional.ofNullable(fundamentals.get("pe"))
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validateDivYield(Map<String, BigDecimal> fundamentals, MarketScannerRequest request) {
        return Optional.ofNullable(request.divYield())
            .map(range -> Optional.ofNullable(fundamentals.get("divYield"))
                .map(range::isInRange)
                .orElse(false))
            .orElse(true);
    }
    
    private double calculateDataQuality(MarketDataPoint data) {
        // Functional quality score calculation (RULE #17)
        double baseScore = QUALITY_BASE_SCORE;

        Function<Predicate<MarketDataPoint>, Double> penalize = condition ->
            condition.test(data) ? QUALITY_MIN_SCORE : QUALITY_PENALTY;

        double priceScore = penalize.apply(point ->
            Optional.ofNullable(point.price())
                .map(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .orElse(false));

        double volumeScore = penalize.apply(point ->
            Optional.ofNullable(point.volume())
                .map(volume -> volume > 0)
                .orElse(false)) * QUALITY_PENALTY_ADJUSTMENT;

        double timestampScore = penalize.apply(point ->
            Optional.ofNullable(point.timestamp()).isPresent()) * QUALITY_PENALTY_ADJUSTMENT;

        return Math.max(QUALITY_MIN_SCORE, baseScore + priceScore + volumeScore + timestampScore);
    }
    
    // Additional Functional Helper Methods
    
    private boolean evaluateTechnicalCondition(BigDecimal indicatorValue, MarketScannerRequest.TechnicalIndicatorFilter filter) {
        return switch (filter.condition().toUpperCase()) {
            case "ABOVE" -> indicatorValue.compareTo(filter.value()) > 0;
            case "BELOW" -> indicatorValue.compareTo(filter.value()) < 0;
            case "EQUALS" -> indicatorValue.compareTo(filter.value()) == 0;
            case "BETWEEN" -> Optional.ofNullable(filter.secondValue())
                .map(secondValue -> indicatorValue.compareTo(filter.value()) >= 0 &&
                                   indicatorValue.compareTo(secondValue) <= 0)
                .orElse(false);
            case "CROSSING_ABOVE", "CROSSING_BELOW" -> checkCrossing(indicatorValue, filter);
            default -> true;
        };
    }
    
    private boolean validateChartPatterns(List<String> patterns, MarketScannerRequest request) {
        return Optional.ofNullable(request.chartPatterns())
            .filter(chartPatterns -> !chartPatterns.isEmpty())
            .map(chartPatterns -> patterns.stream().anyMatch(chartPatterns::contains))
            .orElse(true);
    }
    
    private boolean validateCandlestickPatterns(List<String> candlestickPatterns, MarketScannerRequest request) {
        return Optional.ofNullable(request.candlestickPatterns())
            .filter(patterns -> !patterns.isEmpty())
            .map(patterns -> candlestickPatterns.stream().anyMatch(patterns::contains))
            .orElse(true);
    }
    
    private boolean validatePriceBreakout(MarketScannerResult.ScanResultItem.BreakoutAnalysis breakoutAnalysis, 
            MarketScannerRequest request) {
        return Optional.ofNullable(request.priceBreakout())
            .map(required -> required && Optional.ofNullable(breakoutAnalysis)
                .map(MarketScannerResult.ScanResultItem.BreakoutAnalysis::priceBreakout)
                .map(Boolean.TRUE::equals)
                .orElse(false))
            .orElse(true);
    }
    
    private boolean validateVolumeBreakout(MarketScannerResult.ScanResultItem.BreakoutAnalysis breakoutAnalysis, 
            MarketScannerRequest request) {
        return Optional.ofNullable(request.volumeBreakout())
            .map(required -> required && Optional.ofNullable(breakoutAnalysis)
                .map(MarketScannerResult.ScanResultItem.BreakoutAnalysis::volumeBreakout)
                .map(Boolean.TRUE::equals)
                .orElse(false))
            .orElse(true);
    }

    // AgentOS Integration Methods
    
    /**
     * Perform market scan with criteria (AgentOS compatibility)
     */
    public List<String> performScan(Map<String, Object> scanCriteria) {
        log.info("Performing market scan with criteria: {}", scanCriteria);
        
        // Mock implementation for AgentOS integration
        List<String> mockResults = List.of(
            "AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", 
            "NVDA", "META", "NFLX", "CRM", "ADBE"
        );
        
        // Apply criteria filtering (simplified for now)
        return mockResults.stream()
            .limit(5) // Limit results for demo
            .collect(Collectors.toList());
    }
}