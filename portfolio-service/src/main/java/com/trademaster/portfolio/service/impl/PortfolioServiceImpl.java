package com.trademaster.portfolio.service.impl;

import com.trademaster.portfolio.config.LoggingConfiguration.PortfolioLogger;
import com.trademaster.portfolio.config.MetricsConfiguration.PortfolioMetrics;
import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.PortfolioSummary;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.repository.PositionRepository;
import com.trademaster.portfolio.service.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Service Implementation
 * 
 * Core portfolio management service using Java 24 Virtual Threads for unlimited scalability.
 * Provides high-performance portfolio operations with comprehensive monitoring and structured logging.
 * 
 * Key Features:
 * - Virtual Thread-based concurrent processing (millions of threads vs thousands)
 * - Sub-50ms portfolio valuation with parallel market data updates
 * - Real-time P&L tracking with atomic position updates
 * - Comprehensive Prometheus metrics for Grafana dashboards
 * - Structured JSON logging for ELK stack integration
 * - Risk management integration with threshold monitoring
 * - Transaction audit trail for financial compliance
 * 
 * Performance Targets:
 * - Portfolio creation: <100ms
 * - Valuation update: <50ms
 * - Position retrieval: <25ms
 * - Bulk operations: <200ms
 * - Concurrent operations: 10,000+ per second
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {
    
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioLogger portfolioLogger;
    private final PortfolioMetrics portfolioMetrics;
    private final PositionService positionService;
    private final PnLCalculationService pnlCalculationService;
    private final PortfolioAnalyticsService analyticsService;
    private final PortfolioRiskService riskService;
    
    @Override
    @Transactional
    @Timed(name = "portfolio.creation", description = "Time to create new portfolio")
    public Portfolio createPortfolio(Long userId, CreatePortfolioRequest request) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setCorrelationId();
        portfolioLogger.setUserContext(userId);
        
        try {
            log.info("Creating new portfolio for user: {}", userId);
            
            // Validate user doesn't already have active portfolio
            if (portfolioRepository.existsByUserIdAndStatus(userId, PortfolioStatus.ACTIVE)) {
                throw new IllegalStateException("User already has an active portfolio");
            }
            
            // Create new portfolio with initial values
            Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .portfolioName(request.portfolioName())
                .status(PortfolioStatus.ACTIVE)
                .cashBalance(request.initialCashBalance())
                .totalValue(request.initialCashBalance())
                .totalCost(request.initialCashBalance())
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .dayPnl(BigDecimal.ZERO)
                .dayTradesCount(0)
                .riskLevel(request.riskLevel())
                .currency(request.currency())
                .accountType(request.accountType())
                .marginEnabled(request.marginEnabled())
                .createdAt(Instant.now())
                .lastValuationAt(Instant.now())
                .build();
            
            // Save portfolio to database
            Portfolio savedPortfolio = portfolioRepository.save(portfolio);
            
            // Update metrics
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.incrementPortfolioCreations();
            portfolioMetrics.recordPortfolioCreationTime(duration);
            portfolioMetrics.updateTotalAUM(request.initialCashBalance());
            
            // Log successful creation
            portfolioLogger.logPortfolioCreated(
                userId, 
                savedPortfolio.getPortfolioId(), 
                request.portfolioName(),
                request.initialCashBalance(), 
                duration
            );
            
            log.info("Portfolio created successfully with ID: {}", savedPortfolio.getPortfolioId());
            return savedPortfolio;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.incrementPortfolioCreationErrors();
            portfolioLogger.logError("portfolio_creation", "creation_failed", e.getMessage(), null, null, e);
            throw new RuntimeException("Failed to create portfolio for user: " + userId, e);
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.lookup.user", description = "Time to find portfolio by user ID")
    public Portfolio getPortfolioByUserId(Long userId) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setUserContext(userId);
        
        try {
            Portfolio portfolio = portfolioRepository.findByUserIdAndStatus(userId, PortfolioStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active portfolio found for user: " + userId));
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordPortfolioLookupTime(duration);
            
            return portfolio;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.lookup.id", description = "Time to find portfolio by ID")
    public Portfolio getPortfolioById(Long portfolioId) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordPortfolioLookupTime(duration);
            
            return portfolio;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.update", description = "Time to update portfolio details")
    public Portfolio updatePortfolio(Long portfolioId, UpdatePortfolioRequest request) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            
            // Update modifiable fields
            if (request.portfolioName() != null) {
                portfolio.setPortfolioName(request.portfolioName());
            }
            if (request.riskLevel() != null) {
                portfolio.setRiskLevel(request.riskLevel());
            }
            
            portfolio.setUpdatedAt(Instant.now());
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordPortfolioUpdateTime(duration);
            
            log.info("Portfolio {} updated successfully", portfolioId);
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.status.update", description = "Time to update portfolio status")
    public Portfolio updatePortfolioStatus(Long portfolioId, PortfolioStatus newStatus, String reason) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            PortfolioStatus oldStatus = portfolio.getStatus();
            
            portfolio.setStatus(newStatus);
            portfolio.setUpdatedAt(Instant.now());
            
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
            
            // Update metrics based on status change
            if (newStatus == PortfolioStatus.SUSPENDED) {
                portfolioMetrics.incrementPortfolioSuspensions();
            } else if (newStatus == PortfolioStatus.CLOSED) {
                portfolioMetrics.incrementPortfolioClosure();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Portfolio {} status changed from {} to {} - Reason: {}", 
                portfolioId, oldStatus, newStatus, reason);
            
            // Log audit event for compliance
            portfolioLogger.logAuditEvent(
                "portfolio_status_change",
                portfolio.getUserId(),
                portfolioId,
                "status_update",
                "portfolio",
                "success",
                Map.of(
                    "old_status", oldStatus.toString(),
                    "new_status", newStatus.toString(),
                    "reason", reason,
                    "duration_ms", duration
                )
            );
            
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.positions.get", description = "Time to retrieve portfolio positions")
    public List<Position> getPortfolioPositions(Long portfolioId) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            List<Position> positions = positionRepository.findByPortfolioId(portfolioId);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordPositionRetrievalTime(duration);
            
            return positions;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.positions.open", description = "Time to retrieve open positions")
    public List<Position> getOpenPositions(Long portfolioId) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            List<Position> openPositions = positionRepository.findOpenPositionsByPortfolioId(portfolioId);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordPositionRetrievalTime(duration);
            
            return openPositions;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.position.get", description = "Time to retrieve specific position")
    public Position getPosition(Long portfolioId, String symbol) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Position position = positionRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
                .orElseThrow(() -> new RuntimeException("Position not found for symbol: " + symbol));
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordPositionRetrievalTime(duration);
            
            return position;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.valuation.update", description = "Time to update portfolio valuation")
    public Portfolio updatePortfolioValuation(Long portfolioId) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            BigDecimal oldValue = portfolio.getTotalValue();
            
            // Calculate new portfolio value using PnL service
            var valuationResult = pnlCalculationService.calculatePortfolioValuation(portfolioId);
            
            // Update portfolio with new values
            portfolio.setTotalValue(valuationResult.totalValue());
            portfolio.setUnrealizedPnl(valuationResult.unrealizedPnl());
            portfolio.setLastValuationAt(Instant.now());
            portfolio.setUpdatedAt(Instant.now());
            
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordValuationUpdateTime(duration);
            portfolioMetrics.updateTotalAUM(valuationResult.totalValue());
            
            // Log valuation update
            portfolioLogger.logPortfolioValuation(
                portfolioId, 
                oldValue, 
                valuationResult.totalValue(),
                valuationResult.unrealizedPnl(), 
                duration
            );
            
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.valuation.async", description = "Time for async portfolio valuation")
    public CompletableFuture<Portfolio> updatePortfolioValuationAsync(Long portfolioId) {
        return CompletableFuture.supplyAsync(() -> {
            return updatePortfolioValuation(portfolioId);
        }, Thread.ofVirtual().factory());
    }
    
    @Override
    @Timed(name = "portfolio.valuation.bulk", description = "Time for bulk portfolio valuations")
    public CompletableFuture<Integer> bulkUpdateValuations(List<Long> portfolioIds) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Process portfolios in parallel using Virtual Threads
                List<CompletableFuture<Void>> futures = portfolioIds.stream()
                    .map(portfolioId -> CompletableFuture.runAsync(() -> {
                        try {
                            updatePortfolioValuation(portfolioId);
                        } catch (Exception e) {
                            log.error("Failed to update valuation for portfolio: {}", portfolioId, e);
                        }
                    }, Thread.ofVirtual().factory()))
                    .toList();
                
                // Wait for all valuations to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                long duration = System.currentTimeMillis() - startTime;
                portfolioMetrics.recordBulkValuationTime(duration);
                
                log.info("Bulk valuation completed for {} portfolios in {}ms", 
                    portfolioIds.size(), duration);
                
                return portfolioIds.size();
                
            } catch (Exception e) {
                portfolioMetrics.incrementBulkValuationErrors();
                throw new RuntimeException("Bulk valuation failed", e);
            }
        }, Thread.ofVirtual().factory());
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.cash.update", description = "Time to update cash balance")
    public Portfolio updateCashBalance(Long portfolioId, BigDecimal amount, String description) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            BigDecimal oldBalance = portfolio.getCashBalance();
            BigDecimal newBalance = oldBalance.add(amount);
            
            // Validate sufficient funds for withdrawals
            if (amount.compareTo(BigDecimal.ZERO) < 0 && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Insufficient cash balance for withdrawal");
            }
            
            // Update cash balance and total value
            portfolio.setCashBalance(newBalance);
            portfolio.setTotalValue(portfolio.getTotalValue().add(amount));
            portfolio.setUpdatedAt(Instant.now());
            
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordCashUpdateTime(duration);
            
            // Log cash transaction
            portfolioLogger.logTransactionCreated(
                portfolioId,
                amount.compareTo(BigDecimal.ZERO) > 0 ? "DEPOSIT" : "WITHDRAWAL",
                null,
                null,
                amount,
                null,
                duration
            );
            
            log.info("Cash balance updated for portfolio {}: {} -> {}", 
                portfolioId, oldBalance, newBalance);
            
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.realized.pnl.add", description = "Time to add realized P&L")
    public Portfolio addRealizedPnl(Long portfolioId, BigDecimal realizedPnl) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            BigDecimal oldRealizedPnl = portfolio.getRealizedPnl();
            
            // Add realized P&L and update cash balance
            portfolio.setRealizedPnl(oldRealizedPnl.add(realizedPnl));
            portfolio.setCashBalance(portfolio.getCashBalance().add(realizedPnl));
            portfolio.setUpdatedAt(Instant.now());
            
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordRealizedPnlUpdate(duration);
            
            log.info("Realized P&L added to portfolio {}: {}", portfolioId, realizedPnl);
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.summary.get", description = "Time to generate portfolio summary")
    public PortfolioSummary getPortfolioSummary(Long portfolioId) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            List<Position> positions = getOpenPositions(portfolioId);
            
            // Calculate summary metrics
            int totalPositions = positions.size();
            int profitablePositions = (int) positions.stream()
                .filter(p -> p.getUnrealizedPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
            int losingPositions = totalPositions - profitablePositions;
            
            BigDecimal largestPosition = positions.stream()
                .map(Position::getMarketValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            
            PortfolioSummary summary = new PortfolioSummary(
                portfolio.getPortfolioId(),
                portfolio.getPortfolioName(),
                portfolio.getStatus(),
                portfolio.getTotalValue(),
                portfolio.getCashBalance(),
                portfolio.getRealizedPnl(),
                portfolio.getUnrealizedPnl(),
                portfolio.getDayPnl(),
                totalPositions,
                profitablePositions,
                losingPositions,
                largestPosition,
                portfolio.getLastValuationAt(),
                Instant.now()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordSummaryGenerationTime(duration);
            
            return summary;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(name = "portfolio.requiring.valuation", description = "Time to find portfolios requiring valuation")
    public List<Portfolio> getPortfoliosRequiringValuation(Instant cutoffTime) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<Portfolio> portfolios = portfolioRepository.findPortfoliosRequiringValuation(cutoffTime);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordRequiringValuationTime(duration);
            
            return portfolios;
        } finally {
            // No specific portfolio context for batch operations
        }
    }
    
    @Override
    public List<Portfolio> getPortfoliosWithSymbol(String symbol) {
        return portfolioRepository.findPortfoliosWithSymbol(symbol);
    }
    
    @Override
    @Timed(name = "portfolio.metrics.calculate", description = "Time to calculate portfolio metrics")
    public CompletableFuture<com.trademaster.portfolio.service.PortfolioMetrics> calculatePortfolioMetrics(Long portfolioId) {
        return CompletableFuture.supplyAsync(() -> {
            return analyticsService.calculatePortfolioMetrics(portfolioId);
        }, Thread.ofVirtual().factory());
    }
    
    @Override
    @Timed(name = "portfolio.performance.get", description = "Time to get portfolio performance")
    public PortfolioPerformance getPortfolioPerformance(Long portfolioId, Instant fromDate, Instant toDate) {
        return analyticsService.calculatePortfolioPerformance(portfolioId, fromDate, toDate);
    }
    
    @Override
    public List<Portfolio> getTopPerformingPortfolios(int limit) {
        return portfolioRepository.findTopPerformingPortfolios(limit);
    }
    
    @Override
    public List<Portfolio> getUnderperformingPortfolios(int limit) {
        return portfolioRepository.findUnderperformingPortfolios(limit);
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.reset.day.trades", description = "Time to reset day trades count")
    public int resetDayTradesCount() {
        long startTime = System.currentTimeMillis();
        
        try {
            int updatedCount = portfolioRepository.resetDayTradesCount();
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordDayTradesResetTime(duration);
            
            log.info("Day trades count reset for {} portfolios", updatedCount);
            return updatedCount;
            
        } catch (Exception e) {
            portfolioMetrics.incrementDayTradesResetErrors();
            throw new RuntimeException("Failed to reset day trades count", e);
        }
    }
    
    @Override
    @Timed(name = "portfolio.total.aum", description = "Time to calculate total AUM")
    public BigDecimal getTotalAssetsUnderManagement() {
        long startTime = System.currentTimeMillis();
        
        try {
            BigDecimal totalAUM = portfolioRepository.calculateTotalAUM();
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordAUMCalculationTime(duration);
            portfolioMetrics.updateTotalAUM(totalAUM);
            
            return totalAUM;
        } catch (Exception e) {
            portfolioMetrics.incrementAUMCalculationErrors();
            throw new RuntimeException("Failed to calculate total AUM", e);
        }
    }
    
    @Override
    @Timed(name = "portfolio.statistics.get", description = "Time to get portfolio statistics")
    public PortfolioStatistics getPortfolioStatistics() {
        return analyticsService.calculatePortfolioStatistics();
    }
    
    @Override
    public boolean validatePortfolioOperation(Long portfolioId, String operation) {
        return riskService.validatePortfolioOperation(portfolioId, operation);
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.archive", description = "Time to archive portfolio")
    public Portfolio archivePortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.CLOSED, "Portfolio archived");
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.restore", description = "Time to restore portfolio")
    public Portfolio restorePortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.ACTIVE, "Portfolio restored");
    }
    
    @Override
    @Transactional
    @Timed(name = "portfolio.delete", description = "Time to delete portfolio")
    public void deletePortfolio(Long portfolioId, Long adminUserId, String reason) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        portfolioLogger.setUserContext(adminUserId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            
            // Validate portfolio can be deleted (must be closed and have no open positions)
            if (portfolio.getStatus() != PortfolioStatus.CLOSED) {
                throw new IllegalStateException("Portfolio must be closed before deletion");
            }
            
            List<Position> openPositions = getOpenPositions(portfolioId);
            if (!openPositions.isEmpty()) {
                throw new IllegalStateException("Portfolio has open positions and cannot be deleted");
            }
            
            // Delete portfolio
            portfolioRepository.delete(portfolio);
            
            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.incrementPortfolioDeletions();
            
            // Log audit event for compliance
            portfolioLogger.logAuditEvent(
                "portfolio_deletion",
                adminUserId,
                portfolioId,
                "delete",
                "portfolio",
                "success",
                Map.of(
                    "reason", reason,
                    "duration_ms", duration,
                    "user_id", portfolio.getUserId()
                )
            );
            
            log.info("Portfolio {} deleted by admin {} - Reason: {}", portfolioId, adminUserId, reason);
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
}