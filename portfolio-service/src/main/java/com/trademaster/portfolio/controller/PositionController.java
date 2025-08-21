package com.trademaster.portfolio.controller;

import com.trademaster.portfolio.dto.MarketDataUpdate;
import com.trademaster.portfolio.dto.PositionUpdateRequest;
import com.trademaster.portfolio.dto.TaxLotInfo;
import com.trademaster.portfolio.dto.TradeExecutionRequest;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.service.PositionService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Position Management REST API Controller
 * 
 * Provides comprehensive position management endpoints including:
 * - Position lifecycle management and updates
 * - Real-time market data integration
 * - Trade execution and settlement
 * - Tax lot tracking and cost basis calculations
 * - Position-level risk and performance analytics
 * 
 * Security:
 * - JWT-based authentication required for all endpoints
 * - Portfolio-level access control enforcement
 * - Position ownership validation
 * 
 * Performance:
 * - Virtual Threads for concurrent position updates
 * - Real-time market data integration
 * - Optimized queries for large position sets
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/positions")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Position Management", description = "Portfolio position operations and analytics")
public class PositionController {
    
    private final PositionService positionService;
    
    /**
     * Get all positions in a portfolio
     */
    @GetMapping
    @Operation(summary = "List portfolio positions", 
              description = "Retrieves all positions within the specified portfolio")
    @ApiResponse(responseCode = "200", description = "Positions retrieved successfully")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Page<Position>> getPositions(
            @Parameter(description = "Portfolio ID") 
            @PathVariable UUID portfolioId,
            @Parameter(description = "Filter by symbol")
            @RequestParam(required = false) String symbol,
            @Parameter(description = "Filter by position type")
            @RequestParam(required = false) String positionType,
            @Parameter(description = "Include only active positions")
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<Position> positions = positionService.getPositions(
            portfolioId, symbol, positionType, activeOnly, pageable);
        
        return ResponseEntity.ok(positions);
    }
    
    /**
     * Get specific position details
     */
    @GetMapping("/{positionId}")
    @Operation(summary = "Get position details", 
              description = "Retrieves detailed information for a specific position")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Position found",
                    content = @Content(schema = @Schema(implementation = Position.class))),
        @ApiResponse(responseCode = "404", description = "Position not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Position> getPosition(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Position ID") 
            @PathVariable UUID positionId) {
        
        Position position = positionService.getPositionById(portfolioId, positionId);
        return ResponseEntity.ok(position);
    }
    
    /**
     * Get position by symbol
     */
    @GetMapping("/symbol/{symbol}")
    @Operation(summary = "Get position by symbol", 
              description = "Retrieves position details for a specific symbol")
    @ApiResponse(responseCode = "200", description = "Position retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Position> getPositionBySymbol(
            @PathVariable UUID portfolioId,
            @Parameter(description = "Security symbol") 
            @PathVariable String symbol) {
        
        Position position = positionService.getPositionBySymbol(portfolioId, symbol);
        return ResponseEntity.ok(position);
    }
    
    /**
     * Update position manually
     */
    @PutMapping("/{positionId}")
    @Operation(summary = "Update position", 
              description = "Manually updates position details and configurations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Position updated successfully"),
        @ApiResponse(responseCode = "404", description = "Position not found"),
        @ApiResponse(responseCode = "400", description = "Invalid update data")
    })
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Position> updatePosition(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Valid @RequestBody PositionUpdateRequest request) {
        
        log.info("Updating position: {} in portfolio: {}", positionId, portfolioId);
        Position position = positionService.updatePosition(portfolioId, positionId, request);
        
        return ResponseEntity.ok(position);
    }
    
    /**
     * Process trade execution for position
     */
    @PostMapping("/{positionId}/trades")
    @Operation(summary = "Execute trade", 
              description = "Processes a trade execution and updates position accordingly")
    @ApiResponse(responseCode = "201", description = "Trade executed successfully")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Position> executeTrade(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Valid @RequestBody TradeExecutionRequest request) {
        
        log.info("Executing trade for position: {} in portfolio: {}", positionId, portfolioId);
        Position updatedPosition = positionService.executeTrade(portfolioId, positionId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedPosition);
    }
    
    /**
     * Update position with market data
     */
    @PutMapping("/{positionId}/market-data")
    @Operation(summary = "Update position market data", 
              description = "Updates position with real-time market data")
    @ApiResponse(responseCode = "200", description = "Market data updated")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Position> updateMarketData(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Valid @RequestBody MarketDataUpdate marketData) {
        
        Position position = positionService.updateMarketData(portfolioId, positionId, marketData);
        return ResponseEntity.ok(position);
    }
    
    /**
     * Get position tax lots
     */
    @GetMapping("/{positionId}/tax-lots")
    @Operation(summary = "Get position tax lots", 
              description = "Retrieves tax lot information for position cost basis calculations")
    @ApiResponse(responseCode = "200", description = "Tax lots retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<List<TaxLotInfo>> getTaxLots(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Parameter(description = "Cost basis method")
            @RequestParam(defaultValue = "FIFO") String costBasisMethod) {
        
        List<TaxLotInfo> taxLots = positionService.getTaxLots(
            portfolioId, positionId, costBasisMethod);
        
        return ResponseEntity.ok(taxLots);
    }
    
    /**
     * Get position performance metrics
     */
    @GetMapping("/{positionId}/performance")
    @Operation(summary = "Get position performance", 
              description = "Retrieves detailed performance metrics for the position")
    @ApiResponse(responseCode = "200", description = "Performance metrics retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> getPositionPerformance(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Parameter(description = "Start date for performance analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for performance analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Object performance = positionService.getPositionPerformance(
            portfolioId, positionId, startDate, endDate);
        
        return ResponseEntity.ok(performance);
    }
    
    /**
     * Get position risk metrics
     */
    @GetMapping("/{positionId}/risk")
    @Operation(summary = "Get position risk metrics", 
              description = "Retrieves risk analysis for the specific position")
    @ApiResponse(responseCode = "200", description = "Risk metrics retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> getPositionRisk(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId) {
        
        Object riskMetrics = positionService.getPositionRisk(portfolioId, positionId);
        return ResponseEntity.ok(riskMetrics);
    }
    
    /**
     * Close position
     */
    @PostMapping("/{positionId}/close")
    @Operation(summary = "Close position", 
              description = "Initiates closing of the entire position")
    @ApiResponse(responseCode = "202", description = "Position close initiated")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> closePosition(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Parameter(description = "Execution strategy for closing")
            @RequestParam(defaultValue = "MARKET") String strategy) {
        
        log.info("Closing position: {} in portfolio: {} with strategy: {}", 
                positionId, portfolioId, strategy);
        
        Object closeResult = positionService.closePosition(portfolioId, positionId, strategy);
        return ResponseEntity.accepted().body(closeResult);
    }
    
    /**
     * Reduce position size
     */
    @PostMapping("/{positionId}/reduce")
    @Operation(summary = "Reduce position size", 
              description = "Reduces the position size by specified quantity or percentage")
    @ApiResponse(responseCode = "200", description = "Position reduced successfully")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Position> reducePosition(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @Parameter(description = "Quantity to reduce")
            @RequestParam(required = false) Double quantity,
            @Parameter(description = "Percentage to reduce")
            @RequestParam(required = false) Double percentage,
            @Parameter(description = "Execution strategy")
            @RequestParam(defaultValue = "MARKET") String strategy) {
        
        log.info("Reducing position: {} in portfolio: {}", positionId, portfolioId);
        Position position = positionService.reducePosition(
            portfolioId, positionId, quantity, percentage, strategy);
        
        return ResponseEntity.ok(position);
    }
    
    /**
     * Get position exposure
     */
    @GetMapping("/{positionId}/exposure")
    @Operation(summary = "Get position exposure", 
              description = "Calculates position exposure and concentration metrics")
    @ApiResponse(responseCode = "200", description = "Exposure metrics retrieved")
    @PreAuthorize("@portfolioSecurityService.canAccessPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> getPositionExposure(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId) {
        
        Object exposure = positionService.getPositionExposure(portfolioId, positionId);
        return ResponseEntity.ok(exposure);
    }
    
    /**
     * Set position alerts
     */
    @PostMapping("/{positionId}/alerts")
    @Operation(summary = "Set position alerts", 
              description = "Configures price and risk alerts for the position")
    @ApiResponse(responseCode = "201", description = "Alerts configured")
    @PreAuthorize("@portfolioSecurityService.canModifyPortfolio(#portfolioId, authentication.name)")
    public ResponseEntity<Object> setPositionAlerts(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            @RequestBody Object alertConfiguration) {
        
        log.info("Setting alerts for position: {} in portfolio: {}", positionId, portfolioId);
        Object alerts = positionService.setPositionAlerts(portfolioId, positionId, alertConfiguration);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(alerts);
    }
}