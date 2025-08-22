package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.dto.OHLCVData;
import com.trademaster.marketdata.entity.ChartData;
import com.trademaster.marketdata.service.ChartingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Charting Data REST API Controller
 * 
 * Provides comprehensive charting data with technical indicators,
 * pattern recognition, and advanced analytics for trading applications.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/charts")
@RequiredArgsConstructor
@Tag(name = "Chart Data", description = "Advanced charting and technical analysis API")
public class ChartingController {
    
    private final ChartingService chartingService;
    
    /**
     * Get OHLCV data for charting
     */
    @GetMapping("/{symbol}/ohlcv")
    @Operation(summary = "Get OHLCV data", description = "Retrieve OHLCV data for charting applications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chart data retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getOHLCVData(
            @Parameter(description = "Trading symbol", example = "AAPL")
            @PathVariable String symbol,
            
            @Parameter(description = "Timeframe", example = "H1")
            @RequestParam ChartData.Timeframe timeframe,
            
            @Parameter(description = "Start time")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            
            @Parameter(description = "End time")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("OHLCV request for {} {} from {} to {} by user: {}", 
            symbol, timeframe, startTime, endTime, userDetails.getUsername());
        
        var ohlcvData = chartingService.getOHLCVData(symbol, timeframe, startTime, endTime);
        
        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "timeframe", timeframe,
            "startTime", startTime,
            "endTime", endTime,
            "data", ohlcvData,
            "count", ohlcvData.size()
        ));
    }
    
    /**
     * Get complete chart data with technical indicators
     */
    @GetMapping("/{symbol}/complete")
    @Operation(summary = "Get complete chart data", 
               description = "Retrieve complete chart data with technical indicators")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getCompleteChartData(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Complete chart data request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        var chartData = chartingService.getCompleteChartData(symbol, timeframe, startTime, endTime);
        
        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "timeframe", timeframe,
            "startTime", startTime,
            "endTime", endTime,
            "data", chartData,
            "count", chartData.size(),
            "hasIndicators", !chartData.isEmpty() && chartData.get(0).hasAllIndicators()
        ));
    }
    
    /**
     * Get technical indicators data
     */
    @GetMapping("/{symbol}/indicators")
    @Operation(summary = "Get technical indicators", 
               description = "Retrieve technical indicators for analysis")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getTechnicalIndicators(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Technical indicators request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        var indicators = chartingService.getTechnicalIndicators(symbol, timeframe, startTime, endTime);
        
        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "timeframe", timeframe,
            "startTime", startTime,
            "endTime", endTime,
            "indicators", indicators,
            "availableIndicators", indicators.keySet()
        ));
    }
    
    /**
     * Get volume analysis
     */
    @GetMapping("/{symbol}/volume")
    @Operation(summary = "Get volume analysis", description = "Retrieve volume analysis and metrics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChartingService.VolumeAnalysis> getVolumeAnalysis(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Volume analysis request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        var volumeAnalysis = chartingService.getVolumeAnalysis(symbol, timeframe, startTime, endTime);
        
        return ResponseEntity.ok(volumeAnalysis);
    }
    
    /**
     * Get candlestick patterns
     */
    @GetMapping("/{symbol}/patterns")
    @Operation(summary = "Get candlestick patterns", 
               description = "Detect and retrieve candlestick patterns")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getCandlestickPatterns(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Candlestick patterns request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        var patterns = chartingService.getCandlestickPatterns(symbol, timeframe, startTime, endTime);
        
        // Extract unique pattern types
        var patternTypes = patterns.stream()
            .flatMap(p -> p.patterns().stream())
            .distinct()
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "timeframe", timeframe,
            "startTime", startTime,
            "endTime", endTime,
            "patterns", patterns,
            "patternCount", patterns.size(),
            "patternTypes", patternTypes
        ));
    }
    
    /**
     * Get support and resistance levels
     */
    @GetMapping("/{symbol}/levels")
    @Operation(summary = "Get support/resistance levels", 
               description = "Calculate and retrieve support and resistance levels")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChartingService.SupportResistanceLevels> getSupportResistanceLevels(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Support/resistance levels request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        var levels = chartingService.getSupportResistanceLevels(symbol, timeframe, startTime, endTime);
        
        return ResponseEntity.ok(levels);
    }
    
    /**
     * Get period statistics
     */
    @GetMapping("/{symbol}/statistics")
    @Operation(summary = "Get period statistics", 
               description = "Calculate comprehensive statistics for a period")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChartingService.PeriodStatistics> getPeriodStatistics(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Period statistics request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        var statistics = chartingService.getPeriodStatistics(symbol, timeframe, startTime, endTime);
        
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Get multi-symbol data for correlation analysis
     */
    @PostMapping("/multi-symbol")
    @Operation(summary = "Get multi-symbol data", 
               description = "Retrieve data for multiple symbols for correlation analysis")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getMultiSymbolData(
            @RequestBody List<String> symbols,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Multi-symbol data request for {} symbols, timeframe {} by user: {}", 
            symbols.size(), timeframe, userDetails.getUsername());
        
        return CompletableFuture.supplyAsync(() -> {
            var multiSymbolData = chartingService.getMultiSymbolData(symbols, timeframe, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "symbols", symbols,
                "timeframe", timeframe,
                "startTime", startTime,
                "endTime", endTime,
                "data", multiSymbolData,
                "symbolCount", symbols.size(),
                "totalDataPoints", multiSymbolData.values().stream()
                    .mapToInt(List::size)
                    .sum()
            ));
        });
    }
    
    /**
     * Get data quality report
     */
    @GetMapping("/{symbol}/quality")
    @Operation(summary = "Get data quality report", 
               description = "Get data quality metrics and completeness report")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChartingService.DataQualityReport> getDataQualityReport(
            @PathVariable String symbol,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Data quality report request for {} by user: {}", 
            symbol, userDetails.getUsername());
        
        var qualityReport = chartingService.getDataQualityReport(symbol);
        
        return ResponseEntity.ok(qualityReport);
    }
    
    /**
     * Get chart data with pagination
     */
    @GetMapping("/{symbol}/paged")
    @Operation(summary = "Get paged chart data", description = "Retrieve chart data with pagination")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getPagedChartData(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Paged chart data request for {} {} page {} size {} by user: {}", 
            symbol, timeframe, page, size, userDetails.getUsername());
        
        var pageable = PageRequest.of(page, size);
        var chartDataPage = chartingService.getChartDataPaged(symbol, timeframe, startTime, endTime, pageable);
        
        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "timeframe", timeframe,
            "startTime", startTime,
            "endTime", endTime,
            "data", chartDataPage.getContent(),
            "page", chartDataPage.getNumber(),
            "size", chartDataPage.getSize(),
            "totalElements", chartDataPage.getTotalElements(),
            "totalPages", chartDataPage.getTotalPages(),
            "hasNext", chartDataPage.hasNext(),
            "hasPrevious", chartDataPage.hasPrevious()
        ));
    }
    
    /**
     * Get available timeframes
     */
    @GetMapping("/timeframes")
    @Operation(summary = "Get available timeframes", description = "Get list of supported timeframes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAvailableTimeframes() {
        
        var timeframes = Arrays.stream(ChartData.Timeframe.values())
            .map(tf -> Map.of(
                "value", tf.name(),
                "description", tf.getDescription(),
                "seconds", tf.getSecondsInterval(),
                "intraday", tf.isIntraday(),
                "daily", tf.isDaily(),
                "weekly", tf.isWeeklyOrHigher()
            ))
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "timeframes", timeframes,
            "count", timeframes.size(),
            "intradayOptions", Arrays.stream(ChartData.Timeframe.values())
                .filter(ChartData.Timeframe::isIntraday)
                .map(Enum::name)
                .toList(),
            "dailyAndAbove", Arrays.stream(ChartData.Timeframe.values())
                .filter(tf -> !tf.isIntraday())
                .map(Enum::name)
                .toList()
        ));
    }
    
    /**
     * Get chart data for specific pattern analysis
     */
    @GetMapping("/{symbol}/pattern-analysis")
    @Operation(summary = "Get pattern analysis data", 
               description = "Get optimized data for pattern analysis algorithms")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getPatternAnalysisData(
            @PathVariable String symbol,
            @RequestParam ChartData.Timeframe timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "false") boolean includeVolume,
            @RequestParam(defaultValue = "false") boolean includeIndicators,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Pattern analysis data request for {} {} by user: {}", 
            symbol, timeframe, userDetails.getUsername());
        
        if (includeIndicators) {
            // Get complete data with indicators
            var completeData = chartingService.getCompleteChartData(symbol, timeframe, startTime, endTime);
            return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "timeframe", timeframe,
                "dataType", "complete",
                "data", completeData,
                "count", completeData.size()
            ));
        } else if (includeVolume) {
            // Get OHLCV data
            var ohlcvData = chartingService.getOHLCVData(symbol, timeframe, startTime, endTime);
            return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "timeframe", timeframe,
                "dataType", "ohlcv",
                "data", ohlcvData,
                "count", ohlcvData.size()
            ));
        } else {
            // Get basic OHLC data (lightweight)
            var ohlcvData = chartingService.getOHLCVData(symbol, timeframe, startTime, endTime);
            var ohlcData = ohlcvData.stream()
                .map(ohlcv -> Map.of(
                    "timestamp", ohlcv.timestamp(),
                    "open", ohlcv.open(),
                    "high", ohlcv.high(),
                    "low", ohlcv.low(),
                    "close", ohlcv.close()
                ))
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "timeframe", timeframe,
                "dataType", "ohlc",
                "data", ohlcData,
                "count", ohlcData.size()
            ));
        }
    }
}