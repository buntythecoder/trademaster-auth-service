package com.trademaster.portfolio.controller;

import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.PnLBreakdown;
import com.trademaster.portfolio.dto.PortfolioOptimizationSuggestion;
import com.trademaster.portfolio.dto.PortfolioSummary;
import com.trademaster.portfolio.dto.RiskAlert;
import com.trademaster.portfolio.dto.RiskAssessmentRequest;
import com.trademaster.portfolio.dto.RiskAssessmentResult;
import com.trademaster.portfolio.dto.RiskLimitConfiguration;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.error.Result;
import com.trademaster.portfolio.security.JwtTokenExtractor;
import com.trademaster.portfolio.service.*;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

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
@Tag(name = "Portfolio Management", description = "Portfolio lifecycle management, analytics, risk monitoring, and performance tracking")
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PortfolioController {
    
    private final PortfolioService portfolioService;
    private final PortfolioAnalyticsService analyticsService;
    private final PortfolioRiskService riskService;
    private final PnLCalculationService pnlService;
    private final JwtTokenExtractor jwtTokenExtractor;
    
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
            @Valid @RequestBody CreatePortfolioRequest request,
            HttpServletRequest httpRequest) {
        
        // Extract userId from JWT token or request context
        Long userId = extractUserIdFromRequest(httpRequest);
        
        log.info("Creating new portfolio: {} for user: {}", request.portfolioName(), userId);
        Portfolio portfolio = portfolioService.createPortfolio(userId, request);
        
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
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        
        Long userId = extractUserIdFromRequest(request);
        Page<PortfolioSummary> portfolios = portfolioService.getPortfoliosForUser(userId, status, pageable);
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
            @PathVariable Long portfolioId) {
        
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
            @PathVariable Long portfolioId,
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
            @PathVariable Long portfolioId) {
        
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
            @PathVariable Long portfolioId) {
        
        PortfolioSummary summary = portfolioService.getPortfolioSummary(portfolioId);
        return ResponseEntity.ok(summary);
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
            @PathVariable Long portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Convert LocalDate to Instant
        var startInstant = startDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant();
        var endInstant = endDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant();
        
        PnLBreakdown pnlBreakdown = pnlService.calculatePnLBreakdown(portfolioId, startInstant, endInstant);
        return ResponseEntity.ok(pnlBreakdown);
    }
    
    /**
     * Get risk assessment.
     *
     * Pattern: Service delegation with functional transformation
     * Rule #3: No if-else - Direct service method call with transformation
     *
     * Transforms RiskAssessmentResult to alerts by checking violations and warnings.
     *
     * @param portfolioId Portfolio identifier
     * @param request Risk assessment parameters
     * @return List of risk alerts based on assessment
     */
    @PostMapping("/{portfolioId}/risk/assess")
    @Operation(summary = "Assess portfolio risk",
              description = "Performs comprehensive risk analysis on the portfolio")
    @ApiResponse(responseCode = "200", description = "Risk assessment completed")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<RiskAlert>> assessRisk(
            @PathVariable Long portfolioId,
            @Valid @RequestBody RiskAssessmentRequest request) {

        return switch (riskService.assessTradeRisk(portfolioId, request)) {
            case Result.Success(RiskAssessmentResult result) ->
                ResponseEntity.ok(convertAssessmentToAlerts(portfolioId, result));
            case Result.Failure(var error) ->
                ResponseEntity.badRequest().body(List.of());
        };
    }

    /**
     * Convert risk assessment result to risk alerts.
     *
     * Pattern: Functional transformation with stream operations
     * Rule #3: No if-else - Uses stream filtering and mapping
     *
     * @param portfolioId Portfolio identifier
     * @param result Risk assessment result
     * @return List of risk alerts
     */
    private List<RiskAlert> convertAssessmentToAlerts(Long portfolioId, RiskAssessmentResult result) {
        return result.violations().stream()
            .map(violation -> new RiskAlert(
                UUID.randomUUID().toString(),
                portfolioId,
                "VIOLATION",
                "HIGH",
                violation,
                violation,
                result.riskScore(),
                null,
                null,
                null,
                null,
                result.assessmentTime(),
                null,
                false,
                "Review and address risk violation"
            ))
            .toList();
    }
    
    /**
     * Configure risk limits.
     *
     * Pattern: Service delegation with authentication context
     * Rule #3: No if-else - Direct service method invocation
     *
     * @param portfolioId Portfolio identifier
     * @param config Risk limit configuration
     * @param request HTTP request for user extraction
     * @return Updated risk limit configuration
     */
    @PutMapping("/{portfolioId}/risk/limits")
    @Operation(summary = "Configure risk limits",
              description = "Sets risk limits and monitoring thresholds for the portfolio")
    @ApiResponse(responseCode = "200", description = "Risk limits updated")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<RiskLimitConfiguration> configureRiskLimits(
            @PathVariable Long portfolioId,
            @Valid @RequestBody RiskLimitConfiguration config,
            HttpServletRequest request) {

        log.info("Configuring risk limits for portfolio: {}", portfolioId);

        var userId = extractUserIdFromRequest(request);

        return switch (riskService.updateRiskConfiguration(portfolioId, config, userId)) {
            case Result.Success(RiskLimitConfiguration result) ->
                ResponseEntity.ok(result);
            case Result.Failure(var error) ->
                ResponseEntity.badRequest().build();
        };
    }
    
    /**
     * Get portfolio optimization suggestions.
     *
     * Pattern: Asynchronous service delegation with CompletableFuture
     * Rule #12: Virtual threads with async operations
     *
     * Uses analytics service to generate optimization recommendations based on objective.
     *
     * @param portfolioId Portfolio identifier
     * @param objective Optimization objective (SHARPE_RATIO, RETURN, RISK)
     * @return Portfolio optimization suggestions
     */
    @GetMapping("/{portfolioId}/optimize")
    @Operation(summary = "Get optimization suggestions",
              description = "Generates portfolio optimization recommendations")
    @ApiResponse(responseCode = "200", description = "Optimization suggestions generated")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<PortfolioOptimizationSuggestion>> getOptimizationSuggestions(
            @PathVariable Long portfolioId,
            @Parameter(description = "Optimization objective")
            @RequestParam(defaultValue = "SHARPE_RATIO") String objective) {

        var suggestionsFuture = analyticsService.generateOptimizationSuggestions(portfolioId, objective);
        var suggestions = suggestionsFuture.join();

        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * Get active risk alerts.
     *
     * Pattern: Service delegation with optional filtering
     * Rule #3: No if-else - Uses Optional and stream filtering
     *
     * Retrieves alerts from risk monitoring service and optionally filters by severity.
     *
     * @param portfolioId Portfolio identifier
     * @param severity Optional severity filter (HIGH, MEDIUM, LOW)
     * @return List of active risk alerts
     */
    @GetMapping("/{portfolioId}/risk/alerts")
    @Operation(summary = "Get active risk alerts",
              description = "Retrieves all active risk alerts for the portfolio")
    @ApiResponse(responseCode = "200", description = "Risk alerts retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<RiskAlert>> getRiskAlerts(
            @PathVariable Long portfolioId,
            @Parameter(description = "Filter by alert severity")
            @RequestParam(required = false) String severity) {

        return switch (riskService.monitorRiskLimits(portfolioId)) {
            case Result.Success(List<RiskAlert> alerts) ->
                ResponseEntity.ok(filterAlertsBySeverity(alerts, severity));
            case Result.Failure(var error) ->
                ResponseEntity.badRequest().body(List.of());
        };
    }

    /**
     * Filter risk alerts by severity.
     *
     * Pattern: Functional filtering with Optional
     * Rule #3: No if-else - Uses Optional to handle null severity
     *
     * @param alerts All alerts
     * @param severity Optional severity filter
     * @return Filtered alerts
     */
    private List<RiskAlert> filterAlertsBySeverity(List<RiskAlert> alerts, String severity) {
        return Optional.ofNullable(severity)
            .map(s -> alerts.stream()
                .filter(alert -> s.equalsIgnoreCase(alert.severity()))
                .toList())
            .orElse(alerts);
    }
    
    /**
     * Get portfolio analytics dashboard data.
     *
     * Pattern: Composite aggregation with functional composition
     * Rule #3: No if-else - Uses functional composition to build dashboard
     *
     * Aggregates multiple analytics data points into comprehensive dashboard.
     * Includes metrics, performance, risk, and diversification analysis.
     *
     * @param portfolioId Portfolio identifier
     * @param period Time period for analytics (1D, 1W, 1M, 3M, 1Y)
     * @return Comprehensive analytics dashboard data
     */
    @GetMapping("/{portfolioId}/analytics")
    @Operation(summary = "Get analytics dashboard data",
              description = "Retrieves comprehensive analytics data for dashboard display")
    @ApiResponse(responseCode = "200", description = "Analytics data retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Map<String, Object>> getAnalyticsDashboard(
            @PathVariable Long portfolioId,
            @Parameter(description = "Time period for analytics")
            @RequestParam(defaultValue = "1M") String period) {

        var dashboard = createAnalyticsDashboard(portfolioId, period);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Create analytics dashboard by aggregating multiple data sources.
     *
     * Pattern: Functional aggregation with Map composition
     * Rule #3: No if-else - Direct map construction
     *
     * @param portfolioId Portfolio identifier
     * @param period Analysis period
     * @return Dashboard data map
     */
    private Map<String, Object> createAnalyticsDashboard(Long portfolioId, String period) {
        var metrics = analyticsService.calculatePortfolioMetrics(portfolioId);
        var diversification = analyticsService.analyzeDiversification(portfolioId);
        var sectorAnalysis = analyticsService.analyzeSectorAllocation(portfolioId);

        return Map.of(
            "portfolioId", portfolioId,
            "period", period,
            "metrics", metrics,
            "diversification", diversification,
            "sectorAllocation", sectorAnalysis,
            "lastUpdated", Instant.now()
        );
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
            @PathVariable Long portfolioId,
            @Parameter(description = "Rebalancing strategy")
            @RequestParam(defaultValue = "TARGET_ALLOCATION") String strategy) {
        
        log.info("Initiating portfolio rebalancing: {} with strategy: {}", portfolioId, strategy);
        // Return the CompletableFuture result
        var rebalanceResult = portfolioService.initiateRebalancing(portfolioId, strategy);
        
        return ResponseEntity.accepted().body(rebalanceResult);
    }
    
    /**
     * Map PortfolioError to HTTP status code.
     *
     * Pattern: Functional error mapping with pattern matching
     * Rule #14: Pattern matching for error severity mapping
     *
     * Maps error severity and type to appropriate HTTP status:
     * - NOT_FOUND errors → 404
     * - VALIDATION errors → 400
     * - AUTHORIZATION errors → 403
     * - CRITICAL severity → 500
     * - Other errors → 400
     *
     * @param error Portfolio error to map
     * @return HTTP status code
     */
    private HttpStatus mapErrorToHttpStatus(com.trademaster.portfolio.error.PortfolioError error) {
        return switch (error) {
            case PORTFOLIO_NOT_FOUND, POSITION_NOT_FOUND, TRANSACTION_NOT_FOUND, USER_NOT_FOUND ->
                HttpStatus.NOT_FOUND;
            case UNAUTHORIZED_ACCESS ->
                HttpStatus.FORBIDDEN;
            case INSUFFICIENT_FUNDS, INSUFFICIENT_BUYING_POWER, INSUFFICIENT_MARGIN,
                 MARGIN_CALL_TRIGGERED, NEGATIVE_BALANCE,
                 POSITION_CONCENTRATION_EXCEEDED, SECTOR_CONCENTRATION_EXCEEDED,
                 LEVERAGE_LIMIT_EXCEEDED, DAILY_LOSS_LIMIT_EXCEEDED,
                 MAX_POSITION_SIZE_EXCEEDED, MAX_PORTFOLIO_SIZE_EXCEEDED,
                 RISK_ASSESSMENT_FAILED, DAY_TRADING_LIMIT_EXCEEDED ->
                HttpStatus.BAD_REQUEST;
            case DATABASE_ERROR, BROKER_API_UNAVAILABLE, INTERNAL_ERROR ->
                HttpStatus.INTERNAL_SERVER_ERROR;
            case CIRCUIT_BREAKER_OPEN, MARKET_DATA_UNAVAILABLE, PRICE_FEED_TIMEOUT ->
                HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Extract user ID from HTTP request using JWT or fallback header.
     *
     * Pattern: Functional composition with chain of responsibility
     * Rule #3: No if-else - Uses Optional chaining with orElseThrow
     * Rule #11: Error handling with functional approach
     *
     * Strategy:
     * 1. Try Authorization header (JWT token) - primary authentication
     * 2. Try X-User-ID header (fallback for development/testing)
     * 3. Throw security exception if both fail
     *
     * @param request HTTP request containing authentication headers
     * @return Extracted user ID
     * @throws SecurityException if authentication fails
     */
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        return jwtTokenExtractor.extractUserId(
            request.getHeader("Authorization"),
            request.getHeader("X-User-ID")
        ).orElseThrow(() -> {
            log.error("User authentication failed - no valid JWT token or user ID header");
            return new SecurityException("User authentication required");
        });
    }
}