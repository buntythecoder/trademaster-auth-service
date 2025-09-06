package com.trademaster.pnlengine.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Integration Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance portfolio data integration providing portfolio composition,
 * allocation analysis, and portfolio-level analytics for P&L calculations.
 * 
 * Core Features:
 * - Portfolio composition and holdings management
 * - Asset allocation analysis and drift detection
 * - Portfolio performance metrics and benchmarking
 * - Risk metrics and portfolio optimization insights
 * - Multi-portfolio aggregation and consolidation
 * 
 * Performance Targets:
 * - Portfolio data retrieval: <100ms per portfolio
 * - Holdings synchronization: <200ms for full portfolio
 * - Allocation analysis: <150ms for sector/asset class breakdown
 * - Performance calculation: <300ms for comprehensive metrics
 * - Multi-portfolio aggregation: <500ms for 10+ portfolios
 * 
 * Integration Points:
 * - Portfolio Service: Core portfolio management functionality
 * - Multi-Broker Service: Position and transaction data
 * - Market Data Service: Portfolio valuation and performance
 * - Risk Service: Portfolio risk metrics and analytics
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface PortfolioIntegrationService {
    
    /**
     * Get comprehensive portfolio data for P&L calculations
     * 
     * @param userId User identifier
     * @param portfolioId Portfolio identifier (null for all portfolios)
     * @return Portfolio data with holdings and metadata
     */
    CompletableFuture<PortfolioData> getPortfolioData(String userId, Long portfolioId);
    
    /**
     * Get all portfolios for user with summary data
     * 
     * @param userId User identifier
     * @return List of user portfolios with summary metrics
     */
    CompletableFuture<List<PortfolioSummary>> getUserPortfolios(String userId);
    
    /**
     * Get portfolio holdings with current market values
     * 
     * @param portfolioId Portfolio identifier
     * @return Current portfolio holdings
     */
    CompletableFuture<List<PortfolioHolding>> getPortfolioHoldings(Long portfolioId);
    
    /**
     * Get portfolio allocation breakdown by sector, asset class, etc.
     * 
     * @param portfolioId Portfolio identifier
     * @return Portfolio allocation analysis
     */
    CompletableFuture<PortfolioAllocation> getPortfolioAllocation(Long portfolioId);
    
    /**
     * Get portfolio performance metrics and benchmarking
     * 
     * @param portfolioId Portfolio identifier
     * @param benchmarkSymbol Benchmark for comparison
     * @param periodDays Analysis period in days
     * @return Portfolio performance analysis
     */
    CompletableFuture<PortfolioPerformance> getPortfolioPerformance(Long portfolioId, 
                                                                   String benchmarkSymbol, Integer periodDays);
    
    /**
     * Get portfolio risk metrics and analytics
     * 
     * @param portfolioId Portfolio identifier
     * @param periodDays Analysis period in days
     * @return Portfolio risk analysis
     */
    CompletableFuture<PortfolioRiskMetrics> getPortfolioRiskMetrics(Long portfolioId, Integer periodDays);
    
    /**
     * Aggregate multiple portfolios for consolidated view
     * 
     * @param userId User identifier
     * @param portfolioIds List of portfolio IDs to aggregate
     * @return Consolidated portfolio view
     */
    CompletableFuture<ConsolidatedPortfolio> aggregatePortfolios(String userId, List<Long> portfolioIds);
    
    // Supporting data records
    
    record PortfolioData(
        Long portfolioId,
        String userId,
        String portfolioName,
        String portfolioType,
        String strategy,
        BigDecimal cashBalance,
        BigDecimal totalValue,
        BigDecimal investedAmount,
        Integer holdingsCount,
        Instant createdAt,
        Instant lastUpdated,
        String status
    ) {}
    
    record PortfolioSummary(
        Long portfolioId,
        String portfolioName,
        String portfolioType,
        BigDecimal totalValue,
        BigDecimal dayPnL,
        BigDecimal dayPnLPercent,
        BigDecimal totalReturn,
        BigDecimal totalReturnPercent,
        Integer holdingsCount,
        String status
    ) {}
    
    record PortfolioHolding(
        Long holdingId,
        Long portfolioId,
        String symbol,
        String companyName,
        String sector,
        String assetClass,
        Integer quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal costBasis,
        BigDecimal unrealizedPnL,
        BigDecimal unrealizedPnLPercent,
        BigDecimal dayPnL,
        BigDecimal dayPnLPercent,
        BigDecimal allocationPercent,
        Integer holdingDays,
        Instant lastUpdated
    ) {}
    
    record PortfolioAllocation(
        Long portfolioId,
        BigDecimal totalValue,
        List<SectorAllocation> sectorAllocations,
        List<AssetClassAllocation> assetClassAllocations,
        List<GeographicAllocation> geographicAllocations,
        List<MarketCapAllocation> marketCapAllocations,
        Instant calculatedAt
    ) {}
    
    record SectorAllocation(
        String sectorName,
        BigDecimal allocationPercent,
        BigDecimal marketValue,
        BigDecimal dayPnL,
        BigDecimal dayPnLPercent,
        Integer holdingsCount
    ) {}
    
    record AssetClassAllocation(
        String assetClass,
        BigDecimal allocationPercent,
        BigDecimal marketValue,
        BigDecimal dayPnL,
        BigDecimal dayPnLPercent,
        Integer holdingsCount
    ) {}
    
    record GeographicAllocation(
        String region,
        BigDecimal allocationPercent,
        BigDecimal marketValue,
        Integer holdingsCount
    ) {}
    
    record MarketCapAllocation(
        String marketCapCategory,
        BigDecimal allocationPercent,
        BigDecimal marketValue,
        Integer holdingsCount
    ) {}
    
    record PortfolioPerformance(
        Long portfolioId,
        String benchmarkSymbol,
        Integer periodDays,
        BigDecimal totalReturn,
        BigDecimal totalReturnPercent,
        BigDecimal benchmarkReturn,
        BigDecimal benchmarkReturnPercent,
        BigDecimal activeReturn,
        BigDecimal trackingError,
        BigDecimal informationRatio,
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal maxDrawdown,
        BigDecimal maxDrawdownPercent,
        BigDecimal volatility,
        BigDecimal beta,
        BigDecimal alpha,
        Instant calculatedAt
    ) {}
    
    record PortfolioRiskMetrics(
        Long portfolioId,
        Integer periodDays,
        BigDecimal portfolioValue,
        BigDecimal volatility,
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal maxDrawdown,
        BigDecimal maxDrawdownPercent,
        BigDecimal valueAtRisk95,
        BigDecimal valueAtRisk99,
        BigDecimal expectedShortfall,
        BigDecimal concentration,
        BigDecimal diversificationRatio,
        List<PositionRiskContribution> riskContributions,
        Instant calculatedAt
    ) {}
    
    record PositionRiskContribution(
        String symbol,
        BigDecimal riskContribution,
        BigDecimal riskContributionPercent,
        BigDecimal marginalRisk,
        BigDecimal componentRisk
    ) {}
    
    record ConsolidatedPortfolio(
        String userId,
        List<Long> portfolioIds,
        BigDecimal totalValue,
        BigDecimal totalCashBalance,
        BigDecimal totalInvestedAmount,
        BigDecimal totalUnrealizedPnL,
        BigDecimal totalDayPnL,
        BigDecimal totalReturn,
        BigDecimal totalReturnPercent,
        Integer totalHoldings,
        List<ConsolidatedHolding> consolidatedHoldings,
        PortfolioAllocation consolidatedAllocation,
        Instant consolidatedAt
    ) {}
    
    record ConsolidatedHolding(
        String symbol,
        String companyName,
        Integer totalQuantity,
        BigDecimal weightedAverageCost,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnL,
        BigDecimal totalDayPnL,
        List<PortfolioHoldingBreakdown> portfolioBreakdown
    ) {}
    
    record PortfolioHoldingBreakdown(
        Long portfolioId,
        String portfolioName,
        Integer quantity,
        BigDecimal averageCost,
        BigDecimal marketValue,
        BigDecimal allocationPercent
    ) {}
}