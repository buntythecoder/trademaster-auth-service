package com.trademaster.portfolio.service;

import com.trademaster.portfolio.domain.PortfolioData;
import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.functional.PortfolioErrors;
import com.trademaster.portfolio.functional.Result;
import com.trademaster.portfolio.model.PortfolioStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Functional Portfolio Service Interface
 * 
 * Follows Rule #11 (Error Handling Patterns) - functional error handling with Result types.
 * All methods return Result<T, PortfolioErrors> instead of throwing exceptions.
 * 
 * Features:
 * - Railway Programming pattern with flatMap/map chains
 * - No try-catch blocks in business logic
 * - Immutable PortfolioData operations
 * - CompletableFuture with Virtual Threads for async operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Functional Programming)
 */
public interface FunctionalPortfolioService {
    
    /**
     * Create new portfolio with functional error handling
     */
    Result<PortfolioData, PortfolioErrors> createPortfolio(
        Long userId, 
        CreatePortfolioRequest request
    );
    
    /**
     * Get portfolio by user ID
     */
    Result<PortfolioData, PortfolioErrors> getPortfolioByUserId(Long userId);
    
    /**
     * Get portfolio by portfolio ID
     */
    Result<PortfolioData, PortfolioErrors> getPortfolioById(Long portfolioId);
    
    /**
     * Update portfolio details
     */
    Result<PortfolioData, PortfolioErrors> updatePortfolio(
        Long portfolioId, 
        UpdatePortfolioRequest request
    );
    
    /**
     * Update portfolio status
     */
    Result<PortfolioData, PortfolioErrors> updatePortfolioStatus(
        Long portfolioId, 
        PortfolioStatus newStatus, 
        String reason
    );
    
    /**
     * Update portfolio valuation with circuit breaker protection
     */
    Result<PortfolioData, PortfolioErrors> updatePortfolioValuation(Long portfolioId);
    
    /**
     * Async portfolio valuation with Virtual Threads
     */
    CompletableFuture<Result<PortfolioData, PortfolioErrors>> updatePortfolioValuationAsync(
        Long portfolioId
    );
    
    /**
     * Bulk update valuations with parallel processing
     */
    CompletableFuture<Result<Integer, PortfolioErrors>> bulkUpdateValuations(
        List<Long> portfolioIds
    );
    
    /**
     * Update cash balance with validation
     */
    Result<PortfolioData, PortfolioErrors> updateCashBalance(
        Long portfolioId, 
        BigDecimal amount, 
        String description
    );
    
    /**
     * Add realized P&L
     */
    Result<PortfolioData, PortfolioErrors> addRealizedPnl(
        Long portfolioId, 
        BigDecimal realizedPnl
    );
    
    /**
     * Get portfolios requiring valuation
     */
    Result<List<PortfolioData>, PortfolioErrors> getPortfoliosRequiringValuation(
        Instant cutoffTime
    );
    
    /**
     * Get portfolios holding specific symbol
     */
    Result<List<PortfolioData>, PortfolioErrors> getPortfoliosWithSymbol(String symbol);
    
    /**
     * Calculate total assets under management
     */
    Result<BigDecimal, PortfolioErrors> getTotalAssetsUnderManagement();
    
    /**
     * Reset day trades count for all portfolios
     */
    Result<Integer, PortfolioErrors> resetDayTradesCount();
    
    /**
     * Archive portfolio (set to CLOSED status)
     */
    Result<PortfolioData, PortfolioErrors> archivePortfolio(Long portfolioId);
    
    /**
     * Restore portfolio (set to ACTIVE status)
     */
    Result<PortfolioData, PortfolioErrors> restorePortfolio(Long portfolioId);
    
    /**
     * Delete portfolio with validation
     */
    Result<Void, PortfolioErrors> deletePortfolio(
        Long portfolioId, 
        Long adminUserId, 
        String reason
    );
    
    /**
     * Validate portfolio operation with risk checks
     */
    Result<Boolean, PortfolioErrors> validatePortfolioOperation(
        Long portfolioId, 
        String operation
    );
}