package com.trademaster.pnlengine.controller;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import com.trademaster.pnlengine.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-Broker P&L Engine REST Controller
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Enterprise-grade REST API for multi-broker P&L calculations providing
 * real-time portfolio valuation, performance attribution, tax optimization,
 * and comprehensive analytics across multiple brokerage platforms.
 * 
 * Key Features:
 * - Real-time multi-broker P&L aggregation (<50ms response times)
 * - Advanced performance attribution analysis
 * - Tax-optimized P&L calculations with compliance reporting
 * - WebSocket streaming for real-time updates
 * - Comprehensive risk metrics and volatility analysis
 * - Historical trending and benchmarking
 * 
 * Security Features:
 * - JWT-based authentication and authorization
 * - Role-based access control with method-level security
 * - Request/response logging with correlation IDs
 * - Rate limiting and circuit breaker protection
 * - Input validation and sanitization
 * 
 * Performance Features:
 * - Virtual Thread-based async processing
 * - Redis caching for sub-50ms responses
 * - Parallel broker calculations
 * - Structured concurrency for coordinated operations
 * - Circuit breaker patterns for resilience
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
@RestController
@RequestMapping("/api/v2/pnl-engine")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "P&L Engine", description = "Multi-Broker Profit & Loss Calculation Engine")
public class PnLEngineController {
    
    private final PnLCalculationEngine pnlCalculationEngine;
    
    // ============================================================================
    // CORE P&L CALCULATION ENDPOINTS
    // ============================================================================
    
    @GetMapping("/portfolio/{portfolioId}/multi-broker")
    @Operation(summary = "Calculate Multi-Broker Portfolio P&L", 
              description = "Comprehensive P&L calculation across all connected brokers for specified portfolio")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "P&L calculation successful"),
        @ApiResponse(responseCode = "404", description = "Portfolio not found"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PreAuthorize("hasAuthority('SCOPE_portfolio:read')")
    public CompletableFuture<ResponseEntity<MultiBrokerPnLResult>> calculateMultiBrokerPnL(
            @Parameter(description = "Portfolio ID", required = true) @PathVariable Long portfolioId,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Multi-broker P&L calculation requested - user: {}, portfolio: {}", userId, portfolioId);
        
        return pnlCalculationEngine.calculateMultiBrokerPnL(userId, portfolioId)
            .thenApply(result -> {
                log.info("Multi-broker P&L calculation completed - user: {}, portfolio: {}, time: {}ms", 
                        userId, portfolioId, result.calculationTimeMs());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Multi-broker P&L calculation failed - user: {}, portfolio: {}", 
                         userId, portfolioId, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/broker/{brokerType}")
    @Operation(summary = "Calculate Broker-Specific P&L", 
              description = "P&L calculation for specific broker with detailed position breakdown")
    @PreAuthorize("hasAuthority('SCOPE_portfolio:read')")
    public CompletableFuture<ResponseEntity<BrokerPnLResult>> calculateBrokerPnL(
            @Parameter(description = "Broker Type", required = true) @PathVariable BrokerType brokerType,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Broker P&L calculation requested - user: {}, broker: {}", userId, brokerType);
        
        return pnlCalculationEngine.calculateBrokerPnL(userId, brokerType)
            .thenApply(result -> {
                log.info("Broker P&L calculation completed - user: {}, broker: {}, time: {}ms", 
                        userId, brokerType, result.calculationTimeMs());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Broker P&L calculation failed - user: {}, broker: {}", 
                         userId, brokerType, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/position")
    @Operation(summary = "Calculate Position-Level P&L", 
              description = "Aggregated P&L calculation for specific position across all brokers")
    @PreAuthorize("hasAuthority('SCOPE_portfolio:read')")
    public CompletableFuture<ResponseEntity<List<PositionPnLResult>>> calculatePositionPnL(
            @Parameter(description = "Symbol (optional - all positions if not specified)") 
            @RequestParam(required = false) String symbol,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Position P&L calculation requested - user: {}, symbol: {}", userId, symbol);
        
        return pnlCalculationEngine.calculatePositionPnL(userId, symbol)
            .thenApply(result -> {
                log.info("Position P&L calculation completed - user: {}, symbol: {}, positions: {}", 
                        userId, symbol, result.size());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Position P&L calculation failed - user: {}, symbol: {}", 
                         userId, symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/unrealized")
    @Operation(summary = "Calculate Unrealized P&L", 
              description = "Current unrealized gains/losses using live market prices")
    @PreAuthorize("hasAuthority('SCOPE_portfolio:read')")
    public CompletableFuture<ResponseEntity<BigDecimal>> calculateUnrealizedPnL(
            @Parameter(description = "Broker Type (optional - all brokers if not specified)")
            @RequestParam(required = false) BrokerType brokerType,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Unrealized P&L calculation requested - user: {}, broker: {}", userId, brokerType);
        
        return pnlCalculationEngine.calculateUnrealizedPnL(userId, brokerType)
            .thenApply(result -> {
                log.info("Unrealized P&L calculation completed - user: {}, broker: {}, pnl: {}", 
                        userId, brokerType, result);
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Unrealized P&L calculation failed - user: {}, broker: {}", 
                         userId, brokerType, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/realized")
    @Operation(summary = "Calculate Realized P&L", 
              description = "Realized gains/losses from completed trades with tax implications")
    @PreAuthorize("hasAuthority('SCOPE_portfolio:read')")
    public CompletableFuture<ResponseEntity<RealizedPnLResult>> calculateRealizedPnL(
            @Parameter(description = "Start date for calculation", required = true) 
            @RequestParam @NotNull Instant fromDate,
            @Parameter(description = "End date for calculation", required = true) 
            @RequestParam @NotNull Instant toDate,
            @Parameter(description = "Broker Type (optional - all brokers if not specified)")
            @RequestParam(required = false) BrokerType brokerType,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Realized P&L calculation requested - user: {}, broker: {}, period: {} to {}", 
                userId, brokerType, fromDate, toDate);
        
        return pnlCalculationEngine.calculateRealizedPnL(userId, fromDate, toDate, brokerType)
            .thenApply(result -> {
                log.info("Realized P&L calculation completed - user: {}, broker: {}, trades: {}", 
                        userId, brokerType, result.tradesCount());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Realized P&L calculation failed - user: {}, broker: {}", 
                         userId, brokerType, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/day-pnl")
    @Operation(summary = "Calculate Day P&L", 
              description = "Daily profit/loss using previous trading day's closing prices")
    @PreAuthorize("hasAuthority('SCOPE_portfolio:read')")
    public CompletableFuture<ResponseEntity<DayPnLResult>> calculateDayPnL(
            @Parameter(description = "Trading date (optional - today if not specified)")
            @RequestParam(required = false) Instant tradingDate,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Day P&L calculation requested - user: {}, date: {}", userId, tradingDate);
        
        return pnlCalculationEngine.calculateDayPnL(userId, tradingDate)
            .thenApply(result -> {
                log.info("Day P&L calculation completed - user: {}, date: {}, dayPnL: {}", 
                        userId, result.tradingDate(), result.totalDayPnL());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Day P&L calculation failed - user: {}, date: {}", 
                         userId, tradingDate, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    // ============================================================================
    // ADVANCED ANALYTICS AND ATTRIBUTION
    // ============================================================================
    
    @GetMapping("/analytics/performance-attribution")
    @Operation(summary = "Calculate Performance Attribution", 
              description = "Comprehensive performance attribution analysis with security selection and asset allocation effects")
    @PreAuthorize("hasAuthority('SCOPE_analytics:read')")
    public CompletableFuture<ResponseEntity<PerformanceAttributionResult>> calculatePerformanceAttribution(
            @Parameter(description = "Analysis start date", required = true) 
            @RequestParam @NotNull Instant fromDate,
            @Parameter(description = "Analysis end date", required = true) 
            @RequestParam @NotNull Instant toDate,
            @Parameter(description = "Benchmark symbol", required = true) 
            @RequestParam @NotBlank String benchmarkSymbol,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Performance attribution requested - user: {}, benchmark: {}, period: {} to {}", 
                userId, benchmarkSymbol, fromDate, toDate);
        
        return pnlCalculationEngine.calculatePerformanceAttribution(userId, fromDate, toDate, benchmarkSymbol)
            .thenApply(result -> {
                log.info("Performance attribution completed - user: {}, benchmark: {}, activeReturn: {}", 
                        userId, benchmarkSymbol, result.activeReturn());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Performance attribution failed - user: {}, benchmark: {}", 
                         userId, benchmarkSymbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/analytics/risk-metrics")
    @Operation(summary = "Calculate Risk Metrics", 
              description = "Comprehensive risk analysis including Sharpe ratio, maximum drawdown, and volatility metrics")
    @PreAuthorize("hasAuthority('SCOPE_analytics:read')")
    public CompletableFuture<ResponseEntity<RiskMetricsResult>> calculateRiskMetrics(
            @Parameter(description = "Analysis period in days", required = true) 
            @RequestParam @Min(1) @Max(3650) Integer periodDays,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Risk metrics calculation requested - user: {}, period: {} days", userId, periodDays);
        
        return pnlCalculationEngine.calculateRiskMetrics(userId, periodDays)
            .thenApply(result -> {
                log.info("Risk metrics calculation completed - user: {}, period: {} days, sharpe: {}", 
                        userId, periodDays, result.sharpeRatio());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Risk metrics calculation failed - user: {}, period: {}", 
                         userId, periodDays, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/analytics/correlation")
    @Operation(summary = "Calculate Correlation Analysis", 
              description = "Portfolio correlation and beta analysis against benchmark indices")
    @PreAuthorize("hasAuthority('SCOPE_analytics:read')")
    public CompletableFuture<ResponseEntity<CorrelationAnalysisResult>> calculateCorrelationAnalysis(
            @Parameter(description = "Benchmark symbol", required = true) 
            @RequestParam @NotBlank String benchmarkSymbol,
            @Parameter(description = "Analysis period in days", required = true) 
            @RequestParam @Min(30) @Max(1825) Integer periodDays,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Correlation analysis requested - user: {}, benchmark: {}, period: {} days", 
                userId, benchmarkSymbol, periodDays);
        
        return pnlCalculationEngine.calculateCorrelationAnalysis(userId, benchmarkSymbol, periodDays)
            .thenApply(result -> {
                log.info("Correlation analysis completed - user: {}, benchmark: {}, correlation: {}", 
                        userId, benchmarkSymbol, result.correlation());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Correlation analysis failed - user: {}, benchmark: {}", 
                         userId, benchmarkSymbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    // ============================================================================
    // TAX OPTIMIZATION AND COMPLIANCE
    // ============================================================================
    
    @PostMapping("/tax/optimize")
    @Operation(summary = "Calculate Tax-Optimized P&L", 
              description = "Tax-efficient P&L calculation using optimal cost basis method selection")
    @PreAuthorize("hasAuthority('SCOPE_tax:calculate')")
    public CompletableFuture<ResponseEntity<TaxOptimizedPnLResult>> calculateTaxOptimizedPnL(
            @Parameter(description = "Tax optimization request", required = true) 
            @Valid @RequestBody TaxOptimizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Tax-optimized P&L requested - user: {}, symbol: {}, quantity: {}", 
                userId, request.symbol(), request.sellQuantity());
        
        return pnlCalculationEngine.calculateTaxOptimizedPnL(
                userId, request.symbol(), request.sellQuantity(), 
                request.currentPrice(), request.costBasisMethod())
            .thenApply(result -> {
                log.info("Tax-optimized P&L completed - user: {}, symbol: {}, optimizedPnL: {}", 
                        userId, request.symbol(), result.projectedRealizedPnL());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Tax-optimized P&L failed - user: {}, symbol: {}", 
                         userId, request.symbol(), throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @GetMapping("/tax/lots")
    @Operation(summary = "Get Consolidated Tax Lots", 
              description = "Retrieve tax lots across all brokers for specified symbol using cost basis method")
    @PreAuthorize("hasAuthority('SCOPE_tax:read')")
    public CompletableFuture<ResponseEntity<List<TaxLotInfo>>> getConsolidatedTaxLots(
            @Parameter(description = "Symbol", required = true) 
            @RequestParam @NotBlank String symbol,
            @Parameter(description = "Cost basis method", required = true) 
            @RequestParam @NotNull CostBasisMethod costBasisMethod,
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("Tax lots requested - user: {}, symbol: {}, method: {}", 
                userId, symbol, costBasisMethod);
        
        return pnlCalculationEngine.getConsolidatedTaxLots(userId, symbol, costBasisMethod)
            .thenApply(result -> {
                log.info("Tax lots retrieved - user: {}, symbol: {}, lots: {}", 
                        userId, symbol, result.size());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Tax lots retrieval failed - user: {}, symbol: {}", 
                         userId, symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    // ============================================================================
    // BATCH OPERATIONS AND REPORTING
    // ============================================================================
    
    @PostMapping("/batch/calculate")
    @Operation(summary = "Batch P&L Calculation", 
              description = "High-performance batch calculation for multiple users or portfolios")
    @PreAuthorize("hasAuthority('SCOPE_admin:batch')")
    public CompletableFuture<ResponseEntity<List<MultiBrokerPnLResult>>> calculateBatchPnL(
            @Parameter(description = "Batch calculation request", required = true) 
            @Valid @RequestBody BatchPnLRequest request) {
        
        log.info("Batch P&L calculation requested - users: {}", request.userIds().size());
        
        return pnlCalculationEngine.calculateBatchPnL(request.userIds())
            .thenApply(results -> {
                log.info("Batch P&L calculation completed - processed: {} users", results.size());
                return ResponseEntity.ok(results);
            })
            .exceptionally(throwable -> {
                log.error("Batch P&L calculation failed", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    @PostMapping("/validate")
    @Operation(summary = "Validate P&L Accuracy", 
              description = "Comprehensive P&L validation and accuracy verification")
    @PreAuthorize("hasAuthority('SCOPE_portfolio:validate')")
    public CompletableFuture<ResponseEntity<PnLValidationResult>> validatePnLAccuracy(
            @AuthenticationPrincipal Jwt jwt) {
        
        var userId = extractUserId(jwt);
        log.info("P&L validation requested - user: {}", userId);
        
        return pnlCalculationEngine.validatePnLAccuracy(userId)
            .thenApply(result -> {
                log.info("P&L validation completed - user: {}, valid: {}, variance: {}", 
                        userId, result.isValid(), result.variance());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("P&L validation failed - user: {}", userId, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }
    
    // ============================================================================
    // HELPER METHODS AND RECORDS
    // ============================================================================
    
    private String extractUserId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }
    
    // Request/Response DTOs
    
    public record TaxOptimizationRequest(
        @NotBlank String symbol,
        @NotNull @Min(1) Integer sellQuantity,
        @NotNull BigDecimal currentPrice,
        @NotNull CostBasisMethod costBasisMethod
    ) {}
    
    public record BatchPnLRequest(
        @NotNull List<@NotBlank String> userIds
    ) {}
}