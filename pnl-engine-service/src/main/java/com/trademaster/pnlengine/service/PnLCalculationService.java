package com.trademaster.pnlengine.service;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import com.trademaster.pnlengine.security.SecurityContext;
import com.trademaster.pnlengine.security.SecurityError;
import com.trademaster.pnlengine.security.SecurityFacade;
import com.trademaster.pnlengine.service.calculation.*;
import com.trademaster.pnlengine.service.persistence.PnLPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Main P&L Calculation Orchestration Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Orchestrates all P&L calculation services through Zero Trust Security architecture.
 * Acts as the main entry point for all external P&L calculation requests.
 * 
 * Single Responsibility: Secure orchestration of P&L calculation workflow
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class PnLCalculationService {
    
    private final SecurityFacade securityFacade;
    private final MultiBrokerAggregationService multiBrokerAggregationService;
    private final SectorAnalysisService sectorAnalysisService;
    private final PerformanceAnalyticsService performanceAnalyticsService;
    private final TaxOptimizationService taxOptimizationService;
    private final PnLPersistenceService persistenceService;
    
    @Value("${pnl.calculation.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${pnl.calculation.persistence-enabled:true}")
    private boolean persistenceEnabled;
    
    @Value("${pnl.calculation.async-enabled:true}")
    private boolean asyncCalculationEnabled;
    
    // ============================================================================
    // SECURE EXTERNAL API METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Calculate comprehensive P&L for user with full security validation
     * Max 15 lines per method rule
     */
    public CompletableFuture<Result<MultiBrokerPnLResult, SecurityError>> calculateUserPnL(
            SecurityContext context, String userId, Long portfolioId) {
        
        return securityFacade.executeSecurely(context, securityContext -> 
            multiBrokerAggregationService.aggregateMultiBrokerPnL(userId, portfolioId)
                .thenCompose(result -> result.isSuccess() && persistenceEnabled ? 
                    persistCalculationResult(result.getOrThrow()) : 
                    CompletableFuture.completedFuture(result))
                .thenApply(this::convertToSecurityResult))
            .thenCompose(result -> result.isSuccess() ? 
                CompletableFuture.completedFuture(result) : 
                CompletableFuture.completedFuture(result));
    }
    
    /**
     * Get sector analysis with security validation
     */
    public Result<SectorBreakdown, SecurityError> getSectorAnalysis(
            SecurityContext context, String userId) {
        
        return securityFacade.executeWithScope(context, "pnl:read", securityContext ->
            sectorAnalysisService.analyzeSectorBreakdown(userId)
                .mapError(this::convertToSecurityError));
    }
    
    /**
     * Calculate performance analytics with security validation
     */
    public Result<PerformanceAnalyticsService.PerformanceMetrics, SecurityError> getPerformanceMetrics(
            SecurityContext context, String userId) {
        
        return securityFacade.executeWithAuthority(context, "ANALYTICS_READ", securityContext ->
            performanceAnalyticsService.calculatePerformanceMetrics(userId, getDailyReturns(userId))
                .mapError(this::convertToSecurityError));
    }
    
    /**
     * Get tax optimization recommendations with elevated security
     */
    public Result<TaxOptimizationService.TaxOptimizationRecommendations, SecurityError> getTaxOptimization(
            SecurityContext context, String userId) {
        
        return securityFacade.executePrivileged(context, "tax_optimization", securityContext ->
            taxOptimizationService.generateOptimizationRecommendations(
                getUnrealizedPositions(userId), getCurrentTaxLiability(userId))
                .mapError(this::convertToSecurityError));
    }
    
    /**
     * Get P&L history with security validation
     */
    public Result<java.util.List<com.trademaster.pnlengine.entity.PnLCalculationResult>, SecurityError> getPnLHistory(
            SecurityContext context, String userId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        
        return securityFacade.executeWithScope(context, "pnl:history", securityContext ->
            persistenceService.getPnLHistory(userId, startDate, endDate)
                .mapError(this::convertToSecurityError));
    }
    
    // ============================================================================
    // INTERNAL ORCHESTRATION METHODS
    // ============================================================================
    
    private CompletableFuture<Result<MultiBrokerPnLResult, Exception>> persistCalculationResult(
            MultiBrokerPnLResult pnlResult) {
        
        return CompletableFuture.supplyAsync(() ->
            persistenceService.savePnLResult(pnlResult)
                .map(ignored -> pnlResult),
            Thread.ofVirtual().factory());
    }
    
    private Result<MultiBrokerPnLResult, SecurityError> convertToSecurityResult(
            Result<MultiBrokerPnLResult, Exception> result) {
        
        return result.mapError(this::convertToSecurityError);
    }
    
    private SecurityError convertToSecurityError(Exception error) {
        return new SecurityError.SystemSecurityError(
            "pnl_calculation", "CALCULATION_ERROR", error.getMessage(),
            java.time.Instant.now());
    }
    
    // ============================================================================
    // HELPER METHODS FOR DATA RETRIEVAL
    // ============================================================================
    
    private java.util.List<PerformanceAnalyticsService.DailyReturn> getDailyReturns(String userId) {
        // Simulate retrieving historical daily returns
        return java.util.List.of(
            new PerformanceAnalyticsService.DailyReturn(
                java.time.LocalDate.now().minusDays(1), java.math.BigDecimal.valueOf(1.25)),
            new PerformanceAnalyticsService.DailyReturn(
                java.time.LocalDate.now().minusDays(2), java.math.BigDecimal.valueOf(-0.75))
        );
    }
    
    private java.util.List<TaxOptimizationService.UnrealizedPosition> getUnrealizedPositions(String userId) {
        // Simulate retrieving current unrealized positions
        return java.util.List.of(
            new TaxOptimizationService.UnrealizedPosition(
                "RELIANCE", java.time.LocalDate.now().minusDays(200),
                java.math.BigDecimal.valueOf(250000), java.math.BigDecimal.valueOf(255000))
        );
    }
    
    private java.math.BigDecimal getCurrentTaxLiability(String userId) {
        // Simulate retrieving current year tax liability
        return java.math.BigDecimal.valueOf(15000);
    }
}