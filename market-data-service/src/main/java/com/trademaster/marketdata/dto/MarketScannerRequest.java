package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.constants.ResponseMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Market Scanner Request DTO
 * 
 * Supports complex filtering and scanning criteria for discovering
 * stocks and securities based on technical and fundamental parameters.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record MarketScannerRequest(
    
    // Basic Filters
    @NotEmpty(message = ResponseMessages.EXCHANGE_REQUIRED)
    Set<String> exchanges,
    
    Set<String> sectors,
    Set<String> industries,
    Set<String> marketCaps,
    
    // Price Filters
    @Valid
    RangeFilter priceRange,
    
    @Valid
    RangeFilter volumeRange,
    
    @Valid
    RangeFilter marketCapRange,
    
    // Technical Indicators
    @Valid
    List<TechnicalIndicatorFilter> technicalFilters,
    
    // Performance Filters
    @Valid
    RangeFilter dayChangePercent,
    
    @Valid
    RangeFilter weekChangePercent,
    
    @Valid
    RangeFilter monthChangePercent,
    
    @Valid
    RangeFilter yearChangePercent,
    
    // Volume Analysis
    @Valid
    RangeFilter avgVolumeRatio, // Current volume vs average
    
    @Valid
    RangeFilter volumeSpikeRatio, // Volume vs previous day
    
    // Volatility Filters
    @Valid
    RangeFilter volatility,
    
    @Valid
    RangeFilter beta,
    
    // Fundamental Filters
    @Valid
    RangeFilter peRatio,
    
    @Valid
    RangeFilter divYield,
    
    @Valid
    RangeFilter epsGrowth,
    
    @Valid
    RangeFilter revenueGrowth,
    
    // Pattern Recognition
    Set<String> chartPatterns,
    Set<String> candlestickPatterns,
    
    // Breakout Detection
    Boolean nearResistance,
    Boolean nearSupport,
    Boolean volumeBreakout,
    Boolean priceBreakout,
    
    // Sorting and Pagination
    @NotNull(message = ResponseMessages.SORT_FIELD_REQUIRED)
    String sortBy,
    
    @NotNull(message = "Sort direction is required") 
    SortDirection sortDirection,
    
    @Positive(message = "Page size must be positive")
    Integer pageSize,
    
    @Positive(message = "Page number must be positive")
    Integer pageNumber,
    
    // Advanced Options
    Boolean includePreMarket,
    Boolean includeAfterHours,
    Set<String> excludeSymbols,
    Integer minDataQuality // 0-100 quality score threshold
    
) {
    
    public MarketScannerRequest {
        // Set defaults
        if (pageSize == null) pageSize = 50;
        if (pageNumber == null) pageNumber = 1;
        if (sortBy == null) sortBy = "volume";
        if (sortDirection == null) sortDirection = SortDirection.DESC;
        if (includePreMarket == null) includePreMarket = false;
        if (includeAfterHours == null) includeAfterHours = false;
        if (minDataQuality == null) minDataQuality = 80;
    }
    
    /**
     * Range filter for numeric values
     */
    @Builder
    public record RangeFilter(
        BigDecimal min,
        BigDecimal max,
        Boolean includeMin,
        Boolean includeMax
    ) {
        
        public RangeFilter {
            if (includeMin == null) includeMin = true;
            if (includeMax == null) includeMax = true;
        }
        
        public boolean isInRange(BigDecimal value) {
            if (value == null) return false;
            
            boolean withinMin = min == null || 
                (includeMin ? value.compareTo(min) >= 0 : value.compareTo(min) > 0);
            boolean withinMax = max == null || 
                (includeMax ? value.compareTo(max) <= 0 : value.compareTo(max) < 0);
                
            return withinMin && withinMax;
        }
    }
    
    /**
     * Technical indicator filter
     */
    @Builder
    public record TechnicalIndicatorFilter(
        @NotNull(message = "Indicator type is required")
        String indicatorType, // RSI, MACD, SMA, EMA, BB, etc.
        
        @NotNull(message = "Condition is required")
        String condition, // ABOVE, BELOW, CROSSING_ABOVE, CROSSING_BELOW
        
        BigDecimal value,
        BigDecimal secondValue, // For indicators like MACD that have multiple values
        Integer period, // Period for moving averages, RSI, etc.
        String timeframe // 1m, 5m, 15m, 1h, 1d
    ) {
        
        public TechnicalIndicatorFilter {
            if (timeframe == null) timeframe = "1d";
            if (period == null) period = switch(indicatorType.toUpperCase()) {
                case "RSI" -> 14;
                case "SMA", "EMA" -> 20;
                case "MACD" -> 12;
                default -> 14;
            };
        }
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    /**
     * Validation methods
     */
    public boolean isValid() {
        return exchanges != null && !exchanges.isEmpty() &&
               pageSize != null && pageSize > 0 && pageSize <= 1000 &&
               pageNumber != null && pageNumber > 0 &&
               sortBy != null && !sortBy.trim().isEmpty();
    }
    
    public boolean hasVolumeFilters() {
        return volumeRange != null || avgVolumeRatio != null || volumeSpikeRatio != null;
    }
    
    public boolean hasTechnicalFilters() {
        return technicalFilters != null && !technicalFilters.isEmpty();
    }
    
    public boolean hasPerformanceFilters() {
        return dayChangePercent != null || weekChangePercent != null || 
               monthChangePercent != null || yearChangePercent != null;
    }
    
    public boolean hasFundamentalFilters() {
        return peRatio != null || divYield != null || 
               epsGrowth != null || revenueGrowth != null;
    }
    
    public boolean hasPatternFilters() {
        return (chartPatterns != null && !chartPatterns.isEmpty()) ||
               (candlestickPatterns != null && !candlestickPatterns.isEmpty());
    }
    
    public boolean hasBreakoutFilters() {
        return nearResistance != null || nearSupport != null ||
               volumeBreakout != null || priceBreakout != null;
    }
    
    /**
     * Get total number of active filters
     */
    public int getActiveFilterCount() {
        int count = 0;
        
        if (priceRange != null) count++;
        if (volumeRange != null) count++;
        if (marketCapRange != null) count++;
        if (hasTechnicalFilters()) count += technicalFilters.size();
        if (hasPerformanceFilters()) count++;
        if (hasFundamentalFilters()) count++;
        if (hasPatternFilters()) count++;
        if (hasBreakoutFilters()) count++;
        if (sectors != null && !sectors.isEmpty()) count++;
        if (industries != null && !industries.isEmpty()) count++;
        
        return count;
    }
    
    /**
     * Create preset scanner configurations
     */
    public static MarketScannerRequest momentum() {
        return MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .dayChangePercent(RangeFilter.builder().min(new BigDecimal("2.0")).build())
            .volumeSpikeRatio(RangeFilter.builder().min(new BigDecimal("1.5")).build())
            .sortBy("dayChangePercent")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketScannerRequest highVolume() {
        return MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .avgVolumeRatio(RangeFilter.builder().min(new BigDecimal("2.0")).build())
            .priceRange(RangeFilter.builder().min(new BigDecimal("10.0")).build())
            .sortBy("volume")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketScannerRequest breakout() {
        return MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .volumeBreakout(true)
            .priceBreakout(true)
            .sortBy("volumeSpikeRatio")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketScannerRequest oversold() {
        return MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .technicalFilters(List.of(
                TechnicalIndicatorFilter.builder()
                    .indicatorType("RSI")
                    .condition("BELOW")
                    .value(new BigDecimal("30"))
                    .period(14)
                    .build()
            ))
            .sortBy("rsi")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
    public static MarketScannerRequest overbought() {
        return MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .technicalFilters(List.of(
                TechnicalIndicatorFilter.builder()
                    .indicatorType("RSI")
                    .condition("ABOVE")
                    .value(new BigDecimal("70"))
                    .period(14)
                    .build()
            ))
            .sortBy("rsi")
            .sortDirection(SortDirection.DESC)
            .build();
    }
}