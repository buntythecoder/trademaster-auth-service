package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.dto.MarketScannerRequest;
import com.trademaster.marketdata.dto.MarketScannerResult;
import com.trademaster.marketdata.service.MarketScannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Market Scanner REST Controller
 * 
 * Provides endpoints for advanced market scanning with comprehensive
 * filtering capabilities including technical analysis, fundamental
 * analysis, pattern recognition, and breakout detection.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/market-scanner")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Market Scanner", description = "Advanced market scanning and filtering capabilities")
public class MarketScannerController {
    
    private final MarketScannerService marketScannerService;
    
    @PostMapping("/scan")
    @Operation(
        summary = "Execute Market Scan",
        description = "Execute comprehensive market scan with advanced filtering capabilities",
        responses = {
            @ApiResponse(responseCode = "200", description = "Scan completed successfully",
                content = @Content(schema = @Schema(implementation = MarketScannerResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid scan request"),
            @ApiResponse(responseCode = "500", description = "Scan execution failed")
        }
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> executeScan(
            @Valid @RequestBody MarketScannerRequest request) {
        
        log.info("Executing market scan with {} active filters", request.getActiveFilterCount());
        
        return marketScannerService.scan(request)
            .thenApply(result -> {
                log.info("Market scan completed: {} symbols found in {}ms",
                    result.statistics().symbolsMatched(),
                    result.statistics().executionTimeMs());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Market scan failed: {}", throwable.getMessage(), throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @PostMapping("/scan/momentum")
    @Operation(
        summary = "Momentum Scanner",
        description = "Preset scanner for stocks with strong momentum (price change > 2%, volume spike > 1.5x)"
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> momentumScan(
            @Parameter(description = "Additional exchanges to include")
            @RequestParam(required = false) Set<String> exchanges,
            
            @Parameter(description = "Minimum day change percentage", example = "2.0")
            @RequestParam(defaultValue = "2.0") BigDecimal minDayChange,
            
            @Parameter(description = "Minimum volume spike ratio", example = "1.5")
            @RequestParam(defaultValue = "1.5") BigDecimal minVolumeSpike,
            
            @Parameter(description = "Page size", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        
        MarketScannerRequest.RangeFilter dayChangeFilter = MarketScannerRequest.RangeFilter.builder()
            .min(minDayChange)
            .build();
            
        MarketScannerRequest.RangeFilter volumeSpikeFilter = MarketScannerRequest.RangeFilter.builder()
            .min(minVolumeSpike)
            .build();
        
        MarketScannerRequest request = MarketScannerRequest.builder()
            .exchanges(exchanges != null ? exchanges : Set.of("NSE", "BSE"))
            .dayChangePercent(dayChangeFilter)
            .volumeSpikeRatio(volumeSpikeFilter)
            .sortBy("dayChangePercent")
            .sortDirection(MarketScannerRequest.SortDirection.DESC)
            .pageSize(pageSize)
            .build();
        
        return marketScannerService.scan(request)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/scan/breakout")
    @Operation(
        summary = "Breakout Scanner",
        description = "Scanner for potential breakout candidates with volume and price breakouts"
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> breakoutScan(
            @Parameter(description = "Minimum price for screening", example = "10.0")
            @RequestParam(defaultValue = "10.0") BigDecimal minPrice,
            
            @Parameter(description = "Include volume breakouts")
            @RequestParam(defaultValue = "true") Boolean volumeBreakout,
            
            @Parameter(description = "Include price breakouts")
            @RequestParam(defaultValue = "true") Boolean priceBreakout,
            
            @Parameter(description = "Page size", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        
        MarketScannerRequest.RangeFilter priceFilter = MarketScannerRequest.RangeFilter.builder()
            .min(minPrice)
            .build();
        
        MarketScannerRequest request = MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .priceRange(priceFilter)
            .volumeBreakout(volumeBreakout)
            .priceBreakout(priceBreakout)
            .sortBy("volumeSpikeRatio")
            .sortDirection(MarketScannerRequest.SortDirection.DESC)
            .pageSize(pageSize)
            .build();
        
        return marketScannerService.scan(request)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/scan/oversold")
    @Operation(
        summary = "Oversold Scanner",
        description = "Scanner for potentially oversold stocks with RSI < 30"
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> oversoldScan(
            @Parameter(description = "RSI threshold", example = "30")
            @RequestParam(defaultValue = "30") BigDecimal rsiThreshold,
            
            @Parameter(description = "Minimum volume", example = "100000")
            @RequestParam(required = false) Long minVolume,
            
            @Parameter(description = "Page size", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        
        MarketScannerRequest.TechnicalIndicatorFilter rsiFilter = 
            MarketScannerRequest.TechnicalIndicatorFilter.builder()
                .indicatorType("RSI")
                .condition("BELOW")
                .value(rsiThreshold)
                .period(14)
                .build();
        
        var requestBuilder = MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .technicalFilters(List.of(rsiFilter))
            .sortBy("rsi")
            .sortDirection(MarketScannerRequest.SortDirection.ASC)
            .pageSize(pageSize);
        
        if (minVolume != null) {
            MarketScannerRequest.RangeFilter volumeFilter = MarketScannerRequest.RangeFilter.builder()
                .min(new BigDecimal(minVolume))
                .build();
            requestBuilder.volumeRange(volumeFilter);
        }
        
        return marketScannerService.scan(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/scan/overbought")
    @Operation(
        summary = "Overbought Scanner",
        description = "Scanner for potentially overbought stocks with RSI > 70"
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> overboughtScan(
            @Parameter(description = "RSI threshold", example = "70")
            @RequestParam(defaultValue = "70") BigDecimal rsiThreshold,
            
            @Parameter(description = "Minimum volume", example = "100000")
            @RequestParam(required = false) Long minVolume,
            
            @Parameter(description = "Page size", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        
        MarketScannerRequest.TechnicalIndicatorFilter rsiFilter = 
            MarketScannerRequest.TechnicalIndicatorFilter.builder()
                .indicatorType("RSI")
                .condition("ABOVE")
                .value(rsiThreshold)
                .period(14)
                .build();
        
        var requestBuilder = MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .technicalFilters(List.of(rsiFilter))
            .sortBy("rsi")
            .sortDirection(MarketScannerRequest.SortDirection.DESC)
            .pageSize(pageSize);
        
        if (minVolume != null) {
            MarketScannerRequest.RangeFilter volumeFilter = MarketScannerRequest.RangeFilter.builder()
                .min(new BigDecimal(minVolume))
                .build();
            requestBuilder.volumeRange(volumeFilter);
        }
        
        return marketScannerService.scan(requestBuilder.build())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/scan/high-volume")
    @Operation(
        summary = "High Volume Scanner",
        description = "Scanner for stocks with high volume activity (volume ratio > 2x average)"
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> highVolumeScan(
            @Parameter(description = "Minimum volume ratio vs average", example = "2.0")
            @RequestParam(defaultValue = "2.0") BigDecimal minVolumeRatio,
            
            @Parameter(description = "Minimum price for screening", example = "10.0")
            @RequestParam(defaultValue = "10.0") BigDecimal minPrice,
            
            @Parameter(description = "Page size", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        
        MarketScannerRequest.RangeFilter volumeRatioFilter = MarketScannerRequest.RangeFilter.builder()
            .min(minVolumeRatio)
            .build();
            
        MarketScannerRequest.RangeFilter priceFilter = MarketScannerRequest.RangeFilter.builder()
            .min(minPrice)
            .build();
        
        MarketScannerRequest request = MarketScannerRequest.builder()
            .exchanges(Set.of("NSE", "BSE"))
            .avgVolumeRatio(volumeRatioFilter)
            .priceRange(priceFilter)
            .sortBy("volume")
            .sortDirection(MarketScannerRequest.SortDirection.DESC)
            .pageSize(pageSize)
            .build();
        
        return marketScannerService.scan(request)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/scan/custom")
    @Operation(
        summary = "Custom Technical Scanner",
        description = "Create custom scans with multiple technical indicators"
    )
    public CompletableFuture<ResponseEntity<MarketScannerResult>> customTechnicalScan(
            @Parameter(description = "Technical indicators to filter by")
            @RequestBody List<MarketScannerRequest.TechnicalIndicatorFilter> technicalFilters,
            
            @Parameter(description = "Exchanges to scan")
            @RequestParam(defaultValue = "NSE,BSE") Set<String> exchanges,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "scanScore") String sortBy,
            
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") MarketScannerRequest.SortDirection sortDirection,
            
            @Parameter(description = "Page size", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        
        MarketScannerRequest request = MarketScannerRequest.builder()
            .exchanges(exchanges)
            .technicalFilters(technicalFilters)
            .sortBy(sortBy)
            .sortDirection(sortDirection)
            .pageSize(pageSize)
            .build();
        
        return marketScannerService.scan(request)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/presets")
    @Operation(
        summary = "Get Scanner Presets",
        description = "Get available preset scanner configurations"
    )
    public ResponseEntity<Map<String, Object>> getScannerPresets() {
        return ResponseEntity.ok(Map.of(
            "momentum", Map.of(
                "name", "Momentum Scanner",
                "description", "Stocks with strong price momentum and volume surge",
                "filters", List.of("dayChangePercent > 2%", "volumeRatio > 1.5x")
            ),
            "breakout", Map.of(
                "name", "Breakout Scanner", 
                "description", "Stocks breaking through resistance with volume",
                "filters", List.of("priceBreakout", "volumeBreakout", "price > ₹10")
            ),
            "oversold", Map.of(
                "name", "Oversold Scanner",
                "description", "Potentially oversold stocks for reversal plays",
                "filters", List.of("RSI < 30", "volume > 100K")
            ),
            "overbought", Map.of(
                "name", "Overbought Scanner",
                "description", "Potentially overbought stocks for short plays",
                "filters", List.of("RSI > 70", "volume > 100K")
            ),
            "highVolume", Map.of(
                "name", "High Volume Scanner",
                "description", "Stocks with unusual volume activity",
                "filters", List.of("avgVolumeRatio > 2x", "price > ₹10")
            )
        ));
    }
    
    @GetMapping("/filters/available")
    @Operation(
        summary = "Get Available Filters",
        description = "Get all available filter types and their specifications"
    )
    public ResponseEntity<Map<String, Object>> getAvailableFilters() {
        return ResponseEntity.ok(Map.of(
            "priceFilters", Map.of(
                "priceRange", "Min/max price filtering",
                "marketCapRange", "Market capitalization filtering"
            ),
            "volumeFilters", Map.of(
                "volumeRange", "Absolute volume filtering",
                "avgVolumeRatio", "Volume vs average volume ratio",
                "volumeSpikeRatio", "Volume vs previous day ratio"
            ),
            "technicalIndicators", Map.of(
                "RSI", "Relative Strength Index (14-period default)",
                "MACD", "Moving Average Convergence Divergence",
                "SMA", "Simple Moving Average",
                "EMA", "Exponential Moving Average",
                "BB", "Bollinger Bands",
                "STOCH", "Stochastic Oscillator"
            ),
            "performanceFilters", Map.of(
                "dayChangePercent", "1-day price change percentage",
                "weekChangePercent", "1-week price change percentage", 
                "monthChangePercent", "1-month price change percentage",
                "yearChangePercent", "1-year price change percentage"
            ),
            "fundamentalFilters", Map.of(
                "peRatio", "Price-to-Earnings ratio",
                "divYield", "Dividend yield percentage",
                "epsGrowth", "Earnings per share growth",
                "revenueGrowth", "Revenue growth percentage"
            ),
            "patternFilters", Map.of(
                "chartPatterns", List.of("HEAD_AND_SHOULDERS", "DOUBLE_TOP", "DOUBLE_BOTTOM", "TRIANGLE", "FLAG"),
                "candlestickPatterns", List.of("DOJI", "HAMMER", "SHOOTING_STAR", "ENGULFING", "HARAMI")
            ),
            "breakoutFilters", Map.of(
                "priceBreakout", "Price breaking through resistance/support",
                "volumeBreakout", "Volume surge with price movement",
                "nearResistance", "Price near resistance levels",
                "nearSupport", "Price near support levels"
            )
        ));
    }
    
    @GetMapping("/markets/active")
    @Operation(
        summary = "Get Active Markets",
        description = "Get currently active exchanges and trading sessions"
    )
    public ResponseEntity<Map<String, Object>> getActiveMarkets() {
        return ResponseEntity.ok(Map.of(
            "exchanges", Map.of(
                "NSE", Map.of("name", "National Stock Exchange", "status", "OPEN", "timezone", "Asia/Kolkata"),
                "BSE", Map.of("name", "Bombay Stock Exchange", "status", "OPEN", "timezone", "Asia/Kolkata")
            ),
            "tradingSessions", Map.of(
                "regular", Map.of("start", "09:15", "end", "15:30"),
                "preMarket", Map.of("start", "09:00", "end", "09:15"),
                "afterHours", Map.of("start", "15:30", "end", "16:00")
            ),
            "defaultFilters", Map.of(
                "exchanges", List.of("NSE", "BSE"),
                "minDataQuality", 80,
                "includePreMarket", false,
                "includeAfterHours", false
            )
        ));
    }
}