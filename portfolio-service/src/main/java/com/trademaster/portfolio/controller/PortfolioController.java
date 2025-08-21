package com.trademaster.portfolio.controller;

import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.PerformanceComparison;
import com.trademaster.portfolio.dto.PnLBreakdown;
import com.trademaster.portfolio.dto.PortfolioOptimizationSuggestion;
import com.trademaster.portfolio.dto.PortfolioSummary;
import com.trademaster.portfolio.dto.RiskAlert;
import com.trademaster.portfolio.dto.RiskAssessmentRequest;
import com.trademaster.portfolio.dto.RiskLimitConfiguration;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.service.PortfolioAnalyticsService;
import com.trademaster.portfolio.service.PortfolioRiskService;
import com.trademaster.portfolio.service.PortfolioService;
import com.trademaster.portfolio.service.PnLCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Portfolio Management REST API Controller
 * 
 * Provides comprehensive portfolio management endpoints including:
 * - Portfolio lifecycle management (create, update, delete)
 * - Real-time portfolio analytics and performance metrics
 * - Risk management and compliance monitoring
 * - P&L calculations and reporting
 * - Portfolio optimization recommendations
 * 
 * Security:
 * - JWT-based authentication required for all endpoints
 * - Role-based access control (RBAC) enforcement
 * - Account isolation and ownership validation
 * 
 * Performance:
 * - Virtual Threads for high-concurrency operations
 * - Redis caching for frequently accessed data
 * - Asynchronous processing for complex calculations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Portfolio Management", description = "Portfolio lifecycle and analytics operations")
public class PortfolioController {
    
    private final PortfolioService portfolioService;
    private final PortfolioAnalyticsService analyticsService;
    private final PortfolioRiskService riskService;
    private final PnLCalculationService pnlService;
    
    /**
     * Create a new portfolio
     */
    @PostMapping
    @Operation(summary = "Create new portfolio", 
              description = "Creates a new portfolio with specified configuration and initial allocations")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Portfolio created successfully",
                    content = @Content(schema = @Schema(implementation = Portfolio.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('TRADER') or hasRole('PORTFOLIO_MANAGER')")
    public ResponseEntity<Portfolio> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        
        log.info("Creating new portfolio: {}", request.getName());
        Portfolio portfolio = portfolioService.createPortfolio(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
    }
    
    /**
     * Get all portfolios for the authenticated user
     */
    @GetMapping
    @Operation(summary = "List user portfolios", 
              description = "Retrieves all portfolios accessible by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Portfolios retrieved successfully")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<PortfolioSummary>> getPortfolios(
            @Parameter(description = "Filter by portfolio status")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<PortfolioSummary> portfolios = portfolioService.getPortfoliosForUser(status, pageable);
        return ResponseEntity.ok(portfolios);
    }
    
    /**
     * Get portfolio by ID
     */
    @GetMapping("/{portfolioId}")
    @Operation(summary = "Get portfolio details", 
              description = "Retrieves detailed information for a specific portfolio")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Portfolio found",
                    content = @Content(schema = @Schema(implementation = Portfolio.class))),
        @ApiResponse(responseCode = "404", description = "Portfolio not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Portfolio> getPortfolio(
            @Parameter(description = "Portfolio ID") 
            @PathVariable UUID portfolioId) {
        
        Portfolio portfolio = portfolioService.getPortfolioById(portfolioId);
        return ResponseEntity.ok(portfolio);
    }
    
    /**
     * Update portfolio configuration
     */
    @PutMapping("/{portfolioId}")
    @Operation(summary = "Update portfolio", 
              description = "Updates portfolio configuration and settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Portfolio updated successfully"),
        @ApiResponse(responseCode = "404", description = "Portfolio not found"),
        @ApiResponse(responseCode = "400", description = "Invalid update data")
    })
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Portfolio> updatePortfolio(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        
        log.info("Updating portfolio: {}", portfolioId);
        Portfolio portfolio = portfolioService.updatePortfolio(portfolioId, request);
        
        return ResponseEntity.ok(portfolio);
    }
    
    /**
     * Delete portfolio
     */
    @DeleteMapping("/{portfolioId}")
    @Operation(summary = "Delete portfolio", 
              description = "Soft deletes a portfolio and all associated positions")
    @ApiResponse(responseCode = "204", description = "Portfolio deleted successfully")
    @PreAuthorize("@portfolioSecurityService.canDeletePortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Void> deletePortfolio(
            @PathVariable UUID portfolioId) {
        
        log.info("Deleting portfolio: {}", portfolioId);
        portfolioService.deletePortfolio(portfolioId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get portfolio summary with current performance metrics
     */
    @GetMapping("/{portfolioId}/summary")
    @Operation(summary = "Get portfolio summary", 
              description = "Retrieves portfolio summary with real-time performance metrics")
    @ApiResponse(responseCode = "200", description = "Portfolio summary retrieved",
                content = @Content(schema = @Schema(implementation = PortfolioSummary.class)))
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<PortfolioSummary> getPortfolioSummary(
            @PathVariable UUID portfolioId) {
        
        PortfolioSummary summary = analyticsService.getPortfolioSummary(portfolioId);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get portfolio performance comparison
     */
    @GetMapping("/{portfolioId}/performance")
    @Operation(summary = "Get portfolio performance", 
              description = "Retrieves detailed performance analysis with benchmark comparison")
    @ApiResponse(responseCode = "200", description = "Performance data retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<PerformanceComparison> getPortfolioPerformance(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Start date for performance analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for performance analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Benchmark symbol for comparison")
            @RequestParam(defaultValue = "SPY") String benchmark) {
        
        PerformanceComparison performance = analyticsService.comparePerformance(
            portfolioId, startDate, endDate, benchmark);
        
        return ResponseEntity.ok(performance);
    }
    
    /**
     * Get detailed P&L breakdown
     */
    @GetMapping("/{portfolioId}/pnl")
    @Operation(summary = "Get P&L breakdown", 
              description = "Retrieves detailed profit and loss analysis")
    @ApiResponse(responseCode = "200", description = "P&L data retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<PnLBreakdown> getPnLBreakdown(
            @PathVariable UUID portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        PnLBreakdown pnlBreakdown = pnlService.calculatePnLBreakdown(portfolioId, startDate, endDate);
        return ResponseEntity.ok(pnlBreakdown);
    }
    
    /**
     * Get risk assessment
     */
    @PostMapping("/{portfolioId}/risk/assess")
    @Operation(summary = "Assess portfolio risk", 
              description = "Performs comprehensive risk analysis on the portfolio")
    @ApiResponse(responseCode = "200", description = "Risk assessment completed")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<RiskAlert>> assessRisk(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody RiskAssessmentRequest request) {
        
        List<RiskAlert> riskAlerts = riskService.assessRisk(portfolioId, request);
        return ResponseEntity.ok(riskAlerts);
    }
    
    /**
     * Configure risk limits
     */
    @PutMapping("/{portfolioId}/risk/limits")
    @Operation(summary = "Configure risk limits", 
              description = "Sets risk limits and monitoring thresholds for the portfolio")
    @ApiResponse(responseCode = "200", description = "Risk limits updated")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<RiskLimitConfiguration> configureRiskLimits(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody RiskLimitConfiguration config) {
        
        log.info("Configuring risk limits for portfolio: {}", portfolioId);
        RiskLimitConfiguration updatedConfig = riskService.configureRiskLimits(portfolioId, config);
        
        return ResponseEntity.ok(updatedConfig);
    }
    
    /**
     * Get portfolio optimization suggestions
     */
    @GetMapping("/{portfolioId}/optimize")
    @Operation(summary = "Get optimization suggestions", 
              description = "Generates portfolio optimization recommendations")
    @ApiResponse(responseCode = "200", description = "Optimization suggestions generated")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<PortfolioOptimizationSuggestion>> getOptimizationSuggestions(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Optimization objective")
            @RequestParam(defaultValue = "SHARPE_RATIO") String objective) {
        
        List<PortfolioOptimizationSuggestion> suggestions = 
            analyticsService.generateOptimizationSuggestions(portfolioId, objective);
        
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * Get active risk alerts
     */
    @GetMapping("/{portfolioId}/risk/alerts")
    @Operation(summary = "Get active risk alerts", 
              description = "Retrieves all active risk alerts for the portfolio")
    @ApiResponse(responseCode = "200", description = "Risk alerts retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<RiskAlert>> getRiskAlerts(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Filter by alert severity")
            @RequestParam(required = false) String severity) {
        
        List<RiskAlert> alerts = riskService.getActiveAlerts(portfolioId, severity);
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Get portfolio analytics dashboard data
     */
    @GetMapping("/{portfolioId}/analytics")
    @Operation(summary = "Get analytics dashboard data", 
              description = "Retrieves comprehensive analytics data for dashboard display")
    @ApiResponse(responseCode = "200", description = "Analytics data retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> getAnalyticsDashboard(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Time period for analytics")
            @RequestParam(defaultValue = "1M") String period) {
        
        Object analyticsData = analyticsService.getDashboardData(portfolioId, period);
        return ResponseEntity.ok(analyticsData);
    }
    
    /**
     * Rebalance portfolio
     */
    @PostMapping("/{portfolioId}/rebalance")
    @Operation(summary = "Rebalance portfolio", 
              description = "Initiates portfolio rebalancing based on target allocations")
    @ApiResponse(responseCode = "202", description = "Rebalancing initiated")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> rebalancePortfolio(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Rebalancing strategy")
            @RequestParam(defaultValue = "TARGET_ALLOCATION") String strategy) {
        
        log.info("Initiating portfolio rebalancing: {} with strategy: {}", portfolioId, strategy);
        Object rebalanceResult = portfolioService.initiateRebalancing(portfolioId, strategy);
        
        return ResponseEntity.accepted().body(rebalanceResult);
    }
}