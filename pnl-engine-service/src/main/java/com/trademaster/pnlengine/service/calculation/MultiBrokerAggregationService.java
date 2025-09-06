package com.trademaster.pnlengine.service.calculation;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.functional.Validation;
import com.trademaster.pnlengine.config.PnLEngineConfiguration;
import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import com.trademaster.pnlengine.service.BrokerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

/**
 * Multi-Broker P&L Aggregation Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for aggregating P&L calculations across multiple brokers
 * using structured concurrency and functional patterns.
 * 
 * Single Responsibility: Multi-broker P&L aggregation and coordination
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class MultiBrokerAggregationService {
    
    private final BrokerCalculationService brokerCalculationService;
    private final SectorAnalysisService sectorAnalysisService;
    private final PnLEngineConfiguration config;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    
    // ============================================================================
    // CORE AGGREGATION METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Aggregate P&L across all brokers for user and portfolio
     * Max 15 lines per method rule
     */
    public CompletableFuture<Result<MultiBrokerPnLResult, Exception>> aggregateMultiBrokerPnL(
            String userId, Long portfolioId) {
        
        return Validation.USER_ID.apply(userId)
            .map(validUserId -> calculateAggregatedPnLInternal(validUserId, portfolioId))
            .recover(error -> CompletableFuture.completedFuture(
                Result.failure(new IllegalArgumentException(Validation.formatValidationError(error)))))
            .getOrElse(() -> CompletableFuture.completedFuture(
                Result.failure(new IllegalArgumentException("Invalid user ID"))));
    }
    
    /**
     * Calculate consolidated P&L metrics
     * Functional composition with no if-else statements
     */
    public Result<ConsolidatedMetrics, Exception> calculateConsolidatedMetrics(
            List<BrokerPnLResult> brokerResults) {
        
        return Result.of(() -> brokerResults.stream()
            .reduce(ConsolidatedMetrics.empty(), this::accumulateMetrics, ConsolidatedMetrics::combine))
            .flatMap(metrics -> validateConsolidatedMetrics(metrics));
    }
    
    /**
     * Get broker breakdown summary
     */
    public Result<List<BrokerBreakdown>, Exception> createBrokerBreakdown(
            List<BrokerPnLResult> brokerResults) {
        
        return Result.of(() -> brokerResults.stream()
            .map(this::convertToBreakdown)
            .toList());
    }
    
    // ============================================================================
    // INTERNAL CALCULATION METHODS
    // ============================================================================
    
    private CompletableFuture<Result<MultiBrokerPnLResult, Exception>> calculateAggregatedPnLInternal(
            String userId, Long portfolioId) {
        
        return config.getCalculation().isParallelEnabled() ? 
            calculateWithStructuredConcurrency(userId, portfolioId) :
            calculateSequentially(userId, portfolioId);
    }
    
    private CompletableFuture<Result<MultiBrokerPnLResult, Exception>> calculateWithStructuredConcurrency(
            String userId, Long portfolioId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                var brokerTasks = createBrokerCalculationTasks(scope, userId);
                var sectorTask = scope.fork(() -> sectorAnalysisService.analyzeSectorBreakdown(userId));
                
                scope.join().throwIfFailed();
                
                return aggregateResults(userId, portfolioId, brokerTasks, sectorTask);
            } catch (Exception e) {
                return Result.failure(e);
            }
        }, Thread.ofVirtual().factory());
    }
    
    private Map<BrokerType, StructuredTaskScope.Subtask<Result<BrokerPnLResult, Exception>>> createBrokerCalculationTasks(
            StructuredTaskScope.ShutdownOnFailure scope, String userId) {
        
        return Stream.of(BrokerType.values())
            .collect(java.util.stream.Collectors.toMap(
                brokerType -> brokerType,
                brokerType -> scope.fork(() -> brokerCalculationService.calculateBrokerPnL(userId, brokerType))
            ));
    }
    
    private CompletableFuture<Result<MultiBrokerPnLResult, Exception>> calculateSequentially(
            String userId, Long portfolioId) {
        
        return CompletableFuture.supplyAsync(() -> 
            Result.of(() -> Stream.of(BrokerType.values())
                .map(broker -> brokerCalculationService.calculateBrokerPnL(userId, broker))
                .filter(Result::isSuccess)
                .map(result -> result.getOrThrow())
                .toList())
            .flatMap(brokerResults -> createMultiBrokerResult(userId, portfolioId, brokerResults)),
        Thread.ofVirtual().factory());
    }
    
    // ============================================================================
    // HELPER METHODS (FUNCTIONAL COMPOSITION)
    // ============================================================================
    
    private ConsolidatedMetrics accumulateMetrics(ConsolidatedMetrics acc, BrokerPnLResult broker) {
        return new ConsolidatedMetrics(
            acc.totalValue().add(broker.portfolioValue()),
            acc.totalUnrealizedPnL().add(broker.unrealizedPnL()),
            acc.totalRealizedPnL().add(broker.realizedPnL()),
            acc.totalDayPnL().add(broker.dayPnL()),
            acc.totalPositions() + broker.positionsCount(),
            acc.activeBrokers() + 1
        );
    }
    
    private Result<ConsolidatedMetrics, Exception> validateConsolidatedMetrics(ConsolidatedMetrics metrics) {
        return Validation.validate(metrics.totalValue(), Validation.NON_NEGATIVE_AMOUNT)
            .mapError(error -> new IllegalStateException("Invalid consolidated metrics: " + 
                Validation.formatValidationError(error)))
            .map(ignored -> metrics);
    }
    
    private BrokerBreakdown convertToBreakdown(BrokerPnLResult result) {
        return new BrokerBreakdown(
            result.brokerType(), result.portfolioValue(), result.unrealizedPnL(),
            result.realizedPnL(), result.dayPnL(), result.returnPercent(),
            result.positionsCount(), result.connectionStatus(), result.lastSyncTime()
        );
    }
    
    // ============================================================================
    // RESULT CREATION (IMMUTABLE PATTERNS)
    // ============================================================================
    
    private Result<MultiBrokerPnLResult, Exception> aggregateResults(
            String userId, Long portfolioId,
            Map<BrokerType, StructuredTaskScope.Subtask<Result<BrokerPnLResult, Exception>>> brokerTasks,
            StructuredTaskScope.Subtask<Result<SectorBreakdown, Exception>> sectorTask) {
        
        return Result.of(() -> brokerTasks.values().stream()
            .map(task -> task.get().getOrThrow())
            .toList())
            .flatMap(brokerResults -> createMultiBrokerResult(userId, portfolioId, brokerResults));
    }
    
    private Result<MultiBrokerPnLResult, Exception> createMultiBrokerResult(
            String userId, Long portfolioId, List<BrokerPnLResult> brokerResults) {
        
        return calculateConsolidatedMetrics(brokerResults)
            .flatMap(metrics -> createBrokerBreakdown(brokerResults)
                .map(breakdown -> new MultiBrokerPnLResult(
                    userId, portfolioId, metrics.totalValue(), BigDecimal.ZERO,
                    BigDecimal.ZERO, metrics.totalUnrealizedPnL(), metrics.totalRealizedPnL(),
                    metrics.totalDayPnL(), calculateReturnPercent(metrics), 
                    metrics.totalUnrealizedPnL().add(metrics.totalRealizedPnL()),
                    metrics.totalPositions(), metrics.activeBrokers(), breakdown,
                    null, null, null, Instant.now(),
                    System.currentTimeMillis() % 1000, UUID.randomUUID().toString()
                )));
    }
    
    private BigDecimal calculateReturnPercent(ConsolidatedMetrics metrics) {
        var investedAmount = metrics.totalValue().subtract(metrics.totalUnrealizedPnL());
        return investedAmount.compareTo(BigDecimal.ZERO) > 0 ?
            metrics.totalUnrealizedPnL().divide(investedAmount, PRECISION).multiply(BigDecimal.valueOf(100)) :
            BigDecimal.ZERO;
    }
    
    record ConsolidatedMetrics(
        BigDecimal totalValue,
        BigDecimal totalUnrealizedPnL, 
        BigDecimal totalRealizedPnL,
        BigDecimal totalDayPnL,
        Integer totalPositions,
        Integer activeBrokers
    ) {
        static ConsolidatedMetrics empty() {
            return new ConsolidatedMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                                         BigDecimal.ZERO, 0, 0);
        }
        
        ConsolidatedMetrics combine(ConsolidatedMetrics other) {
            return new ConsolidatedMetrics(
                totalValue.add(other.totalValue()),
                totalUnrealizedPnL.add(other.totalUnrealizedPnL()),
                totalRealizedPnL.add(other.totalRealizedPnL()),
                totalDayPnL.add(other.totalDayPnL()),
                totalPositions + other.totalPositions(),
                activeBrokers + other.activeBrokers()
            );
        }
    }
}