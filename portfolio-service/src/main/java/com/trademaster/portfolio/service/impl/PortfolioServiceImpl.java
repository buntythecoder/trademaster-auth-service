package com.trademaster.portfolio.service.impl;

import com.trademaster.portfolio.config.PortfolioLogger;
import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.PortfolioSummary;
import com.trademaster.portfolio.dto.PortfolioValuationResult;
import com.trademaster.portfolio.dto.RebalancingResult;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.functional.Result;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.repository.PositionRepository;
import com.trademaster.portfolio.service.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    // Rule #17: Constants for circuit breakers, executors, and business rules
    private static final String DATABASE_CB = "database";
    private static final String MARKET_DATA_CB = "marketData";
    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Rule #17: Business rule constant - Minimum portfolio value for rebalancing operations
     * Value: $1,000 minimum ensures sufficient liquidity for rebalancing transactions
     */
    private static final BigDecimal MIN_REBALANCING_VALUE = new BigDecimal("1000");

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioLogger portfolioLogger;
    private final PositionService positionService;
    private final PnLCalculationService pnlCalculationService;
    private final PortfolioAnalyticsService analyticsService;
    private final PortfolioRiskService riskService;
    private final PortfolioEventPublisher eventPublisher;
    private final com.trademaster.portfolio.config.PortfolioMetrics portfolioMetrics;
    
    @Override
    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "createPortfolioFallback")
    @Timed(value = "portfolio.creation", description = "Time to create new portfolio")
    public Portfolio createPortfolio(Long userId, CreatePortfolioRequest request) {
        long startTime = System.currentTimeMillis();
        Timer.Sample metricsTimer = portfolioMetrics.startCreationTimer();

        // Rule #11: Functional error handling with Result type - eliminates try-catch
        return Result.safely(() -> {
                portfolioLogger.setCorrelationId();
                portfolioLogger.setUserContext(userId);
                return executePortfolioCreation(userId, request, startTime, metricsTimer);
            })
            .onFailure(e -> handlePortfolioCreationError(userId, startTime, e))
            .mapError(e -> new RuntimeException("Failed to create portfolio for user: " + userId, e))
            .onSuccess(portfolio -> portfolioLogger.clearContext())
            .onFailure(e -> portfolioLogger.clearContext())
            .getOrThrow();
    }

    /**
     * Execute portfolio creation with validation and persistence
     * Rule #5: Extracted method - complexity: 6 (orchestration)
     * Rule #11: Layered extraction pattern
     */
    private Portfolio executePortfolioCreation(Long userId, CreatePortfolioRequest request,
                                               long startTime, Timer.Sample metricsTimer) {
        log.info("Creating new portfolio for user: {}", userId);

        validateUserHasNoActivePortfolio(userId);
        Portfolio portfolio = buildNewPortfolio(userId, request);
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        recordCreationSuccessMetrics(userId, request, savedPortfolio, startTime, metricsTimer);

        log.info("Portfolio created successfully with ID: {}", savedPortfolio.getPortfolioId());
        return savedPortfolio;
    }

    /**
     * Validate user has no active portfolio
     * Rule #5: Extracted method - complexity: 2
     * Rule #3: Functional validation
     */
    private void validateUserHasNoActivePortfolio(Long userId) {
        Optional.of(portfolioRepository.existsByUserIdAndStatus(userId, PortfolioStatus.ACTIVE))
            .filter(hasActive -> !hasActive)
            .orElseThrow(() -> new IllegalStateException("User already has an active portfolio"));
    }

    /**
     * Build new portfolio with initial values
     * Rule #5: Extracted method - complexity: 1
     */
    private Portfolio buildNewPortfolio(Long userId, CreatePortfolioRequest request) {
        return Portfolio.builder()
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
            .createdAt(Instant.now())
            .lastValuationAt(Instant.now())
            .build();
    }

    /**
     * Record creation success metrics and publish event
     * Rule #5: Extracted method - complexity: 3
     */
    private void recordCreationSuccessMetrics(Long userId, CreatePortfolioRequest request,
                                             Portfolio savedPortfolio, long startTime,
                                             Timer.Sample metricsTimer) {
        long duration = System.currentTimeMillis() - startTime;

        portfolioMetrics.recordCreationTime(metricsTimer);
        portfolioMetrics.incrementPortfoliosCreated();
        portfolioMetrics.updateTotalAUM(request.initialCashBalance());

        portfolioLogger.logPortfolioCreated(
            userId, savedPortfolio.getPortfolioId(), request.portfolioName(),
            request.initialCashBalance(), duration
        );

        eventPublisher.publishPortfolioCreatedEvent(savedPortfolio);
    }

    /**
     * Handle portfolio creation error with metrics and logging
     * Rule #5: Extracted method - complexity: 2
     */
    private void handlePortfolioCreationError(Long userId, long startTime, Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        portfolioMetrics.incrementPortfolioCreationErrors();
        portfolioLogger.logError("portfolio_creation", "creation_failed",
            e.getMessage(), null, null, e);
    }
    
    @Override
    @Timed(value = "portfolio.lookup.user", description = "Time to find portfolio by user ID")
    public Portfolio getPortfolioByUserId(Long userId) {
        Timer.Sample lookupTimer = portfolioMetrics.startLookupTimer();
        portfolioLogger.setUserContext(userId);

        try {
            Portfolio portfolio = portfolioRepository.findByUserIdAndStatus(userId, PortfolioStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active portfolio found for user: " + userId));

            portfolioMetrics.recordLookupTime(lookupTimer);
            log.debug("Portfolio lookup completed for user: {}", userId);

            return portfolio;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(value = "portfolio.lookup.id", description = "Time to find portfolio by ID")
    public Portfolio getPortfolioById(Long portfolioId) {
        Timer.Sample lookupTimer = portfolioMetrics.startLookupTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));

            portfolioMetrics.recordLookupTime(lookupTimer);
            log.debug("Portfolio lookup completed for ID: {}", portfolioId);

            return portfolio;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.update", description = "Time to update portfolio details")
    public Portfolio updatePortfolio(Long portfolioId, UpdatePortfolioRequest request) {
        long startTime = System.currentTimeMillis();
        portfolioLogger.setPortfolioContext(portfolioId);
        
        try {
            Portfolio portfolio = getPortfolioById(portfolioId);

            // Rule #3: Functional updates with Optional
            Optional.ofNullable(request.portfolioName())
                .ifPresent(portfolio::setPortfolioName);
            Optional.ofNullable(request.riskLevel())
                .ifPresent(portfolio::setRiskLevel);

            portfolio.setUpdatedAt(Instant.now());
            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
            
            long duration = System.currentTimeMillis() - startTime;
            // Use Timer.Sample for proper timing
            var timerSample = portfolioMetrics.startUpdateTimer();
            portfolioMetrics.recordUpdateTime(timerSample);
            
            // Publish PORTFOLIO_UPDATED event to Event Bus (async, STANDARD priority)
            eventPublisher.publishPortfolioUpdatedEvent(updatedPortfolio);
            
            log.info("Portfolio {} updated successfully", portfolioId);
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.status.update", description = "Time to update portfolio status")
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
            portfolioMetrics.incrementStatusChanges(oldStatus.name(), newStatus.name());
            
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
            
            // Publish PORTFOLIO_STATUS_CHANGED event to Event Bus (async, HIGH priority)
            eventPublisher.publishPortfolioStatusChangedEvent(updatedPortfolio, oldStatus, newStatus, reason);
            
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(value = "portfolio.positions.get", description = "Time to retrieve portfolio positions")
    public List<Position> getPortfolioPositions(Long portfolioId) {
        Timer.Sample retrievalTimer = portfolioMetrics.startPositionRetrievalTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            List<Position> positions = positionRepository.findByPortfolioId(portfolioId);

            portfolioMetrics.recordPositionRetrievalTime(retrievalTimer);
            log.debug("Retrieved {} positions for portfolio: {}", positions.size(), portfolioId);

            return positions;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(value = "portfolio.positions.open", description = "Time to retrieve open positions")
    public List<Position> getOpenPositions(Long portfolioId) {
        Timer.Sample retrievalTimer = portfolioMetrics.startPositionRetrievalTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            List<Position> openPositions = positionRepository.findOpenPositionsByPortfolioId(portfolioId);

            portfolioMetrics.recordPositionRetrievalTime(retrievalTimer);
            log.debug("Retrieved {} open positions for portfolio: {}", openPositions.size(), portfolioId);

            return openPositions;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(value = "portfolio.position.get", description = "Time to retrieve specific position")
    public Position getPosition(Long portfolioId, String symbol) {
        Timer.Sample retrievalTimer = portfolioMetrics.startPositionRetrievalTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Position position = positionRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
                .orElseThrow(() -> new RuntimeException("Position not found for symbol: " + symbol));

            portfolioMetrics.recordPositionRetrievalTime(retrievalTimer);
            log.debug("Retrieved position for symbol: {} in portfolio: {}", symbol, portfolioId);

            return position;
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.valuation.update", description = "Time to update portfolio valuation")
    public Portfolio updatePortfolioValuation(Long portfolioId) {
        long startTime = System.currentTimeMillis();
        Timer.Sample valuationTimer = portfolioMetrics.startValuationTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Portfolio updatedPortfolio = executePortfolioValuation(portfolioId, startTime, valuationTimer);
            return updatedPortfolio;
        } finally {
            portfolioLogger.clearContext();
        }
    }

    /**
     * Execute portfolio valuation with calculation and persistence
     * Rule #5: Extracted method - complexity: 5 (orchestration)
     * Rule #11: Layered extraction pattern
     */
    private Portfolio executePortfolioValuation(Long portfolioId, long startTime, Timer.Sample valuationTimer) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        BigDecimal oldValue = portfolio.getTotalValue();

        var valuationResult = pnlCalculationService.calculatePortfolioValuation(portfolioId);
        Portfolio updatedPortfolio = applyValuationAndSave(portfolio, valuationResult);

        recordValuationMetrics(portfolioId, oldValue, valuationResult, startTime, valuationTimer);

        return updatedPortfolio;
    }

    /**
     * Apply valuation results to portfolio and persist
     * Rule #5: Extracted method - complexity: 2
     */
    private Portfolio applyValuationAndSave(Portfolio portfolio, PortfolioValuationResult valuationResult) {
        portfolio.setTotalValue(valuationResult.totalValue());
        portfolio.setUnrealizedPnl(valuationResult.unrealizedPnl());
        portfolio.setLastValuationAt(Instant.now());
        portfolio.setUpdatedAt(Instant.now());

        return portfolioRepository.save(portfolio);
    }

    /**
     * Record valuation metrics and logging
     * Rule #5: Extracted method - complexity: 2
     */
    private void recordValuationMetrics(Long portfolioId, BigDecimal oldValue,
                                       PortfolioValuationResult valuationResult, long startTime,
                                       Timer.Sample valuationTimer) {
        long duration = System.currentTimeMillis() - startTime;

        portfolioMetrics.recordValuationTime(valuationTimer);
        portfolioMetrics.updateTotalAUM(valuationResult.totalValue());

        portfolioLogger.logPortfolioValuation(
            portfolioId, oldValue, valuationResult.totalValue(),
            valuationResult.unrealizedPnl(), duration
        );
    }
    
    @Override
    @Timed(value = "portfolio.valuation.async", description = "Time for async portfolio valuation")
    public CompletableFuture<Portfolio> updatePortfolioValuationAsync(Long portfolioId) {
        return CompletableFuture.supplyAsync(() -> {
            return updatePortfolioValuation(portfolioId);
        }, VIRTUAL_EXECUTOR);
    }
    
    @Override
    @Timed(value = "portfolio.valuation.bulk", description = "Time for bulk portfolio valuations")
    public CompletableFuture<Integer> bulkUpdateValuations(List<Long> portfolioIds) {
        return CompletableFuture.supplyAsync(() ->
            executeBulkValuations(portfolioIds), VIRTUAL_EXECUTOR);
    }

    /**
     * Execute bulk portfolio valuations
     * Rule #5: Extracted method - complexity: 5
     * Rule #11: Functional error handling with Result type
     */
    private Integer executeBulkValuations(List<Long> portfolioIds) {
        Timer.Sample bulkTimer = portfolioMetrics.startBulkValuationTimer();

        // Rule #11: Functional error handling - eliminates try-catch
        return Result.safely(() -> {
                List<CompletableFuture<Void>> futures = createValuationFutures(portfolioIds);
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                return portfolioIds.size();
            })
            .onSuccess(count -> {
                portfolioMetrics.recordBulkValuationTime(bulkTimer);
                log.info("Bulk valuation completed for {} portfolios", count);
            })
            .onFailure(e -> {
                portfolioMetrics.incrementBulkValuationErrors();
                log.error("Bulk valuation failed", e);
            })
            .mapError(e -> new RuntimeException("Bulk valuation failed", e))
            .getOrThrow();
    }

    /**
     * Create valuation futures for parallel processing
     * Rule #5: Extracted method - complexity: 3
     */
    private List<CompletableFuture<Void>> createValuationFutures(List<Long> portfolioIds) {
        return portfolioIds.stream()
            .map(portfolioId -> CompletableFuture.runAsync(() ->
                updatePortfolioValuationSafely(portfolioId), VIRTUAL_EXECUTOR))
            .toList();
    }

    /**
     * Safely update portfolio valuation with error handling
     * Rule #5: Extracted method - complexity: 2
     */
    private void updatePortfolioValuationSafely(Long portfolioId) {
        try {
            updatePortfolioValuation(portfolioId);
        } catch (Exception e) {
            log.error("Failed to update valuation for portfolio: {}", portfolioId, e);
        }
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.cash.update", description = "Time to update cash balance")
    public Portfolio updateCashBalance(Long portfolioId, BigDecimal amount, String description) {
        long startTime = System.currentTimeMillis();
        Timer.Sample cashUpdateTimer = portfolioMetrics.startCashUpdateTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            BigDecimal oldBalance = portfolio.getCashBalance();
            BigDecimal newBalance = oldBalance.add(amount);

            // Rule #3: Functional validation - sufficient funds check
            Optional.of(amount.compareTo(BigDecimal.ZERO) < 0 && newBalance.compareTo(BigDecimal.ZERO) < 0)
                .filter(insufficientFunds -> !insufficientFunds)
                .orElseThrow(() -> new IllegalArgumentException("Insufficient cash balance for withdrawal"));

            // Update cash balance and total value
            portfolio.setCashBalance(newBalance);
            portfolio.setTotalValue(portfolio.getTotalValue().add(amount));
            portfolio.setUpdatedAt(Instant.now());

            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

            long duration = System.currentTimeMillis() - startTime;
            portfolioMetrics.recordCashUpdateTime(cashUpdateTimer);
            log.debug("Cash update completed for portfolio: {}", portfolioId);
            
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
            
            // Publish CASH_BALANCE_UPDATED event to Event Bus (async, HIGH priority)
            eventPublisher.publishCashBalanceUpdatedEvent(updatedPortfolio, oldBalance, newBalance, description);
            
            log.info("Cash balance updated for portfolio {}: {} -> {}", 
                portfolioId, oldBalance, newBalance);
            
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.realized.pnl.add", description = "Time to add realized P&L")
    public Portfolio addRealizedPnl(Long portfolioId, BigDecimal realizedPnl) {
        Timer.Sample pnlTimer = portfolioMetrics.startPnlUpdateTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            BigDecimal oldRealizedPnl = portfolio.getRealizedPnl();

            // Add realized P&L and update cash balance
            portfolio.setRealizedPnl(oldRealizedPnl.add(realizedPnl));
            portfolio.setCashBalance(portfolio.getCashBalance().add(realizedPnl));
            portfolio.setUpdatedAt(Instant.now());

            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

            portfolioMetrics.recordPnlUpdateTime(pnlTimer);
            log.debug("Realized P&L updated for portfolio: {}", portfolioId);
            
            // Publish PNL_REALIZED event to Event Bus (async, HIGH priority)
            eventPublisher.publishPnlRealizedEvent(updatedPortfolio, realizedPnl, updatedPortfolio.getRealizedPnl());
            
            log.info("Realized P&L added to portfolio {}: {}", portfolioId, realizedPnl);
            return updatedPortfolio;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(value = "portfolio.summary.get", description = "Time to generate portfolio summary")
    public PortfolioSummary getPortfolioSummary(Long portfolioId) {
        Timer.Sample summaryTimer = portfolioMetrics.startSummaryGenerationTimer();
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

            portfolioMetrics.recordSummaryGenerationTime(summaryTimer);
            log.debug("Summary generated for portfolio: {}", portfolioId);
            
            return summary;
            
        } finally {
            portfolioLogger.clearContext();
        }
    }
    
    @Override
    @Timed(value = "portfolio.requiring.valuation", description = "Time to find portfolios requiring valuation")
    public List<Portfolio> getPortfoliosRequiringValuation(Instant cutoffTime) {
        Timer.Sample lookupTimer = portfolioMetrics.startValuationLookupTimer();

        try {
            List<Portfolio> portfolios = portfolioRepository.findPortfoliosRequiringValuation(cutoffTime);

            portfolioMetrics.recordValuationLookupTime(lookupTimer);
            log.debug("Found {} portfolios requiring valuation", portfolios.size());
            
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
    @Timed(value = "portfolio.metrics.calculate", description = "Time to calculate portfolio metrics")
    public CompletableFuture<PortfolioMetrics> calculatePortfolioMetrics(Long portfolioId) {
        return CompletableFuture.supplyAsync(() -> {
            return analyticsService.calculatePortfolioMetrics(portfolioId);
        }, VIRTUAL_EXECUTOR);
    }
    
    @Override
    @Timed(value = "portfolio.performance.get", description = "Time to get portfolio performance")
    public PortfolioPerformance getPortfolioPerformance(Long portfolioId, Instant fromDate, Instant toDate) {
        return analyticsService.calculatePortfolioPerformance(portfolioId, fromDate, toDate);
    }
    
    @Override
    public List<Portfolio> getTopPerformingPortfolios(int limit) {
        return portfolioRepository.findTopPerformingPortfolios()
            .stream()
            .limit(limit)
            .toList();
    }

    @Override
    public List<Portfolio> getUnderperformingPortfolios(int limit) {
        return portfolioRepository.findUnderperformingPortfolios()
            .stream()
            .limit(limit)
            .toList();
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.reset.day.trades", description = "Time to reset day trades count")
    public int resetDayTradesCount() {
        Timer.Sample resetTimer = portfolioMetrics.startDayTradesResetTimer();

        try {
            int updatedCount = portfolioRepository.resetDayTradesCount();

            portfolioMetrics.recordDayTradesResetTime(resetTimer);
            log.info("Day trades count reset for {} portfolios", updatedCount);

            return updatedCount;

        } catch (Exception e) {
            portfolioMetrics.incrementDayTradesResetErrors();
            log.error("Day trades reset failed", e);
            throw new RuntimeException("Failed to reset day trades count", e);
        }
    }
    
    @Override
    @Timed(value = "portfolio.total.aum", description = "Time to calculate total AUM")
    public BigDecimal getTotalAssetsUnderManagement() {
        Timer.Sample aumTimer = portfolioMetrics.startAumCalculationTimer();

        try {
            BigDecimal totalAUM = portfolioRepository.calculateTotalAUM();

            portfolioMetrics.recordAumCalculationTime(aumTimer);
            portfolioMetrics.updateTotalAUM(totalAUM);
            log.debug("Total AUM calculated: {}", totalAUM);

            return totalAUM;
        } catch (Exception e) {
            portfolioMetrics.incrementAumCalculationErrors();
            log.error("AUM calculation failed", e);
            throw new RuntimeException("Failed to calculate total AUM", e);
        }
    }
    
    @Override
    @Timed(value = "portfolio.statistics.get", description = "Time to get portfolio statistics")
    public PortfolioStatistics getPortfolioStatistics() {
        return analyticsService.calculatePortfolioStatistics();
    }
    
    @Override
    public boolean validatePortfolioOperation(Long portfolioId, String operation) {
        // Rule #3: Ternary operator instead of if-else
        var result = riskService.validatePortfolioOperation(portfolioId, operation);
        return result.isSuccess()
            ? result.getOrThrow()
            : Optional.of(false)
                .map(val -> {
                    log.warn("Portfolio operation validation failed: portfolioId={}, operation={}", portfolioId, operation);
                    return val;
                })
                .orElse(false);
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.archive", description = "Time to archive portfolio")
    public Portfolio archivePortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.CLOSED, "Portfolio archived");
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.restore", description = "Time to restore portfolio")
    public Portfolio restorePortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.ACTIVE, "Portfolio restored");
    }
    
    @Override
    @Transactional
    @Timed(value = "portfolio.delete", description = "Time to delete portfolio")
    public void deletePortfolio(Long portfolioId, Long adminUserId, String reason) {
        long startTime = System.currentTimeMillis();
        Timer.Sample deletionTimer = portfolioMetrics.startDeletionTimer();
        portfolioLogger.setPortfolioContext(portfolioId);
        portfolioLogger.setUserContext(adminUserId);

        try {
            Portfolio portfolio = executePortfolioDeletion(portfolioId, adminUserId, reason);
            long duration = System.currentTimeMillis() - startTime;

            portfolioMetrics.recordDeletionTime(deletionTimer);
            logPortfolioDeletionSuccess(portfolioId, adminUserId, reason, duration, portfolio.getUserId());

        } finally {
            portfolioLogger.clearContext();
        }
    }

    /**
     * Execute portfolio deletion with validation
     * Rule #5: Extracted method - complexity: 4
     */
    private Portfolio executePortfolioDeletion(Long portfolioId, Long adminUserId, String reason) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioDeletion(portfolio, portfolioId);

        eventPublisher.publishPortfolioDeletedEvent(portfolioId, portfolio.getUserId(), adminUserId, reason);
        portfolioRepository.delete(portfolio);

        return portfolio;
    }

    /**
     * Validate portfolio can be deleted
     * Rule #5: Extracted method - complexity: 4
     */
    private void validatePortfolioDeletion(Portfolio portfolio, Long portfolioId) {
        Optional.of(portfolio.getStatus())
            .filter(status -> status == PortfolioStatus.CLOSED)
            .orElseThrow(() -> new IllegalStateException("Portfolio must be closed before deletion"));

        List<Position> openPositions = getOpenPositions(portfolioId);
        Optional.of(openPositions)
            .filter(List::isEmpty)
            .orElseThrow(() -> new IllegalStateException("Portfolio has open positions and cannot be deleted"));
    }

    /**
     * Log portfolio deletion success
     * Rule #5: Extracted method - complexity: 2
     */
    private void logPortfolioDeletionSuccess(Long portfolioId, Long adminUserId, String reason, long duration, Long userId) {
        log.info("Portfolio deletion completed for portfolio: {}", portfolioId);

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
                "user_id", userId
            )
        );

        log.info("Portfolio {} deleted by admin {} - Reason: {}", portfolioId, adminUserId, reason);
    }
    
    @Override
    public Page<PortfolioSummary> getPortfoliosForUser(Long userId, String status, Pageable pageable) {
        log.info("Getting portfolios for user: {} with status: {} (page: {}, size: {})",
            userId, status, pageable.getPageNumber(), pageable.getPageSize());

        // Use functional pattern matching for status-based repository selection
        Page<Portfolio> portfolioPage = Optional.ofNullable(status)
            .flatMap(s -> parsePortfolioStatus(s)
                .map(portfolioStatus -> portfolioRepository.findByUserIdAndStatusPageable(userId, portfolioStatus, pageable)))
            .orElseGet(() -> portfolioRepository.findAllByUserIdPageable(userId, pageable));

        // Transform portfolios to summaries using functional stream processing
        List<PortfolioSummary> summaries = portfolioPage.getContent().stream()
            .map(p -> getPortfolioSummary(p.getPortfolioId()))
            .toList();

        log.debug("Retrieved {} portfolios for user: {} with status: {}",
            summaries.size(), userId, status);

        return new org.springframework.data.domain.PageImpl<>(
            summaries,
            pageable,
            portfolioPage.getTotalElements()
        );
    }

    /**
     * Parse status string to PortfolioStatus enum
     *
     * Rule #3: Functional parsing with Optional for error handling
     * Rule #11: No exceptions for control flow
     */
    private Optional<PortfolioStatus> parsePortfolioStatus(String status) {
        return Optional.ofNullable(status)
            .flatMap(s -> {
                try {
                    return Optional.of(PortfolioStatus.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid portfolio status: {}", status);
                    return Optional.empty();
                }
            });
    }
    
    @Override
    public CompletableFuture<RebalancingResult> initiateRebalancing(Long portfolioId, String strategy) {
        // Rule #11: Functional error handling with Result type inside CompletableFuture
        return CompletableFuture.supplyAsync(() ->
            Result.safely(() -> executeRebalancingInitiation(portfolioId, strategy))
                .onSuccess(result -> portfolioLogger.clearContext())
                .onFailure(error -> {
                    portfolioLogger.clearContext();
                    log.error("Rebalancing initiation failed for portfolio: {}", portfolioId, error);
                })
                .mapError(error -> new RuntimeException(
                    "Failed to initiate rebalancing: " + error.getMessage(), error))
                .getOrThrow(),
            VIRTUAL_EXECUTOR
        );
    }

    /**
     * Execute rebalancing initiation with functional error handling
     * Rule #5: Extracted method - complexity: 6 (orchestration)
     * Rule #11: Functional error handling with Optional
     */
    private RebalancingResult executeRebalancingInitiation(Long portfolioId, String strategy) {
        portfolioLogger.setPortfolioContext(portfolioId);
        log.info("Initiating rebalancing for portfolio: {} with strategy: {}", portfolioId, strategy);

        String rebalancingId = generateRebalancingId(portfolioId);
        Portfolio portfolio = getPortfolioById(portfolioId);
        validateRebalancingEligibility(portfolio);

        RebalancingResult result = buildInitiatedResult(rebalancingId, portfolioId, strategy, portfolio);
        publishRebalancingEvent(portfolioId, rebalancingId, strategy);

        log.info("Rebalancing initiated: {} for portfolio: {}", rebalancingId, portfolioId);
        return result;
    }

    /**
     * Generate unique rebalancing ID
     * Rule #5: Extracted method - complexity: 1
     * Rule #17: Uses constant prefix
     */
    private String generateRebalancingId(Long portfolioId) {
        return "RB-" + portfolioId + "-" + System.currentTimeMillis();
    }

    /**
     * Build initiated rebalancing result
     * Rule #5: Extracted method - complexity: 2
     */
    private RebalancingResult buildInitiatedResult(
            String rebalancingId, Long portfolioId, String strategy, Portfolio portfolio) {
        return RebalancingResult.initiated(
            rebalancingId,
            portfolioId,
            strategy,
            portfolio.getTotalValue()
        );
    }

    /**
     * Publish rebalancing initiated event
     * Rule #5: Extracted method - complexity: 1
     */
    private void publishRebalancingEvent(Long portfolioId, String rebalancingId, String strategy) {
        eventPublisher.publishRebalancingInitiatedEvent(portfolioId, rebalancingId, strategy);
    }

    /**
     * Validate portfolio is eligible for rebalancing
     *
     * Rule #3: Functional validation with guard clauses
     * Rule #5: Reduced complexity by extracting validation methods
     * Rule #11: Either pattern for error handling
     */
    private void validateRebalancingEligibility(Portfolio portfolio) {
        validatePortfolioIsActive(portfolio);
        validatePortfolioHasPositions(portfolio.getPortfolioId());
        validatePortfolioMinimumValue(portfolio);
    }

    /**
     * Validate portfolio status is ACTIVE
     * Rule #5: Extracted method - complexity: 2
     */
    private void validatePortfolioIsActive(Portfolio portfolio) {
        Optional.of(portfolio.getStatus())
            .filter(status -> status == PortfolioStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException(
                "Portfolio must be ACTIVE for rebalancing, current status: " + portfolio.getStatus()
            ));
    }

    /**
     * Validate portfolio has positions to rebalance
     * Rule #5: Extracted method - complexity: 2
     */
    private void validatePortfolioHasPositions(Long portfolioId) {
        List<Position> positions = getOpenPositions(portfolioId);
        Optional.of(positions)
            .filter(list -> !list.isEmpty())
            .orElseThrow(() -> new IllegalStateException(
                "Portfolio has no positions to rebalance"
            ));
    }

    /**
     * Validate portfolio has minimum value for rebalancing
     * Rule #5: Extracted method - complexity: 2
     * Rule #17: Uses MIN_REBALANCING_VALUE constant
     */
    private void validatePortfolioMinimumValue(Portfolio portfolio) {
        Optional.of(portfolio.getTotalValue())
            .filter(value -> value.compareTo(MIN_REBALANCING_VALUE) >= 0)
            .orElseThrow(() -> new IllegalStateException(
                "Portfolio value must be at least $" + MIN_REBALANCING_VALUE + " for rebalancing, current value: $" +
                portfolio.getTotalValue()
            ));
    }
    
    @Override
    public void deletePortfolio(Long portfolioId) {
        // Simple delete without admin tracking
        deletePortfolio(portfolioId, 0L, "User requested deletion");
    }

    @Override
    public long getActivePortfoliosCount() {
        log.debug("Retrieving count of active portfolios");
        return portfolioRepository.countActivePortfolios();
    }

    @Override
    @Transactional
    @Timed(value = "portfolio.valuation.manual.update", description = "Time to manually update portfolio valuation")
    public Portfolio updateValuation(Long portfolioId, BigDecimal newValue, BigDecimal newUnrealizedPnl) {
        Timer.Sample valuationTimer = portfolioMetrics.startValuationTimer();
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            BigDecimal oldValue = portfolio.getTotalValue();

            // Update valuation fields
            portfolio.setTotalValue(newValue);
            portfolio.setUnrealizedPnl(newUnrealizedPnl);
            portfolio.setLastValuationAt(Instant.now());
            portfolio.setUpdatedAt(Instant.now());

            Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

            portfolioMetrics.recordValuationTime(valuationTimer);
            portfolioMetrics.updateTotalAUM(newValue);

            log.info("Manual valuation update for portfolio {}: {} -> {}",
                portfolioId, oldValue, newValue);

            return updatedPortfolio;

        } finally {
            portfolioLogger.clearContext();
        }
    }

    @Override
    @Transactional
    public Portfolio updateCashBalance(Long portfolioId, BigDecimal amount) {
        return updateCashBalance(portfolioId, amount, "Cash balance update");
    }

    @Override
    @Transactional
    @Timed(value = "portfolio.day.trades.increment", description = "Time to increment day trades count")
    public int incrementDayTradesCount(Long portfolioId) {
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            int updatedCount = portfolioRepository.incrementDayTradesCount(portfolioId);

            log.info("Day trades count incremented for portfolio {}: {}", portfolioId, updatedCount);

            return updatedCount;

        } finally {
            portfolioLogger.clearContext();
        }
    }

    @Override
    public boolean isApproachingDayTradeLimit(Long portfolioId) {
        portfolioLogger.setPortfolioContext(portfolioId);

        try {
            Portfolio portfolio = getPortfolioById(portfolioId);
            boolean approaching = portfolio.getDayTradesCount() >= 3;

            log.debug("Day trade limit check for portfolio {}: {} trades (approaching: {})",
                portfolioId, portfolio.getDayTradesCount(), approaching);

            return approaching;

        } finally {
            portfolioLogger.clearContext();
        }
    }

    @Override
    @Transactional
    @Timed(value = "portfolio.activate", description = "Time to activate portfolio")
    public Portfolio activatePortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.ACTIVE, "Portfolio activated");
    }

    @Override
    @Transactional
    @Timed(value = "portfolio.close", description = "Time to close portfolio")
    public Portfolio closePortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.CLOSED, "Portfolio closed");
    }

    @Override
    @Transactional
    @Timed(value = "portfolio.suspend", description = "Time to suspend portfolio")
    public Portfolio suspendPortfolio(Long portfolioId) {
        return updatePortfolioStatus(portfolioId, PortfolioStatus.SUSPENDED, "Portfolio suspended");
    }

    @Override
    public boolean hasMinimumCashBalance(Portfolio portfolio) {
        // Rule #17: Minimum cash balance constant (1000)
        BigDecimal minimumBalance = new BigDecimal("1000");
        boolean hasMinimum = portfolio.getCashBalance().compareTo(minimumBalance) >= 0;

        log.debug("Minimum cash balance check for portfolio {}: {} (required: {}, has: {})",
            portfolio.getPortfolioId(), hasMinimum, minimumBalance, portfolio.getCashBalance());

        return hasMinimum;
    }

    @Override
    public boolean canTrade(Portfolio portfolio) {
        // Rule #3: Functional composition for multiple checks
        boolean isActive = portfolio.getStatus() == PortfolioStatus.ACTIVE;
        boolean hasMinimumCash = hasMinimumCashBalance(portfolio);
        boolean notAtDayTradeLimit = portfolio.getDayTradesCount() < 4;

        boolean canTrade = isActive && hasMinimumCash && notAtDayTradeLimit;

        log.debug("Trade eligibility check for portfolio {}: {} (active: {}, cash: {}, day trades: {}/4)",
            portfolio.getPortfolioId(), canTrade, isActive, hasMinimumCash, portfolio.getDayTradesCount());

        return canTrade;
    }

    /**
     * Fallback method for database circuit breaker failures during portfolio creation
     */
    public Portfolio createPortfolioFallback(Long userId, CreatePortfolioRequest request, Exception ex) {
        log.error("Database circuit breaker activated for portfolio creation - user: {}", userId, ex);
        portfolioMetrics.incrementPortfolioCreationErrors();
        throw new RuntimeException("Portfolio service temporarily unavailable - database connection issues", ex);
    }
}