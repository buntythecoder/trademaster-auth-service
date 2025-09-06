package com.trademaster.pnlengine.service.persistence;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.functional.Validation;
import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import com.trademaster.pnlengine.entity.PnLCalculationResult;
import com.trademaster.pnlengine.repository.PnLCalculationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * P&L Data Persistence Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for database operations and data persistence
 * using functional patterns and transaction management.
 * 
 * Single Responsibility: P&L data persistence and retrieval operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class PnLPersistenceService {
    
    private final PnLCalculationResultRepository repository;
    
    @Value("${pnl.persistence.batch-size:100}")
    private int batchSize;
    
    @Value("${pnl.persistence.retention-days:365}")
    private int retentionDays;
    
    @Value("${pnl.persistence.async-enabled:true}")
    private boolean asyncPersistenceEnabled;
    
    // ============================================================================
    // CORE PERSISTENCE METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Save P&L calculation result with validation
     * Max 15 lines per method rule
     */
    @Transactional
    public Result<PnLCalculationResult, Exception> savePnLResult(MultiBrokerPnLResult pnlResult) {
        return validatePnLResult(pnlResult)
            .map(this::convertToEntity)
            .flatMap(this::persistEntity)
            .map(this::auditSaveOperation);
    }
    
    /**
     * Retrieve P&L history for user and date range
     */
    @Transactional(readOnly = true)
    public Result<List<PnLCalculationResult>, Exception> getPnLHistory(
            String userId, LocalDate startDate, LocalDate endDate) {
        
        return Validation.USER_ID.apply(userId)
            .flatMap(validUserId -> validateDateRange(startDate, endDate))
            .map(ignored -> repository.findByUserIdAndCalculationDateBetween(
                userId, startDate, endDate, createPageRequest()))
            .map(this::auditRetrievalOperation);
    }
    
    /**
     * Get latest P&L result for user
     */
    @Transactional(readOnly = true)
    public Result<Optional<PnLCalculationResult>, Exception> getLatestPnLResult(String userId) {
        return Validation.USER_ID.apply(userId)
            .map(validUserId -> repository.findLatestByUserId(validUserId))
            .map(this::logLatestResultRetrieval);
    }
    
    /**
     * Save multiple P&L results in batch
     */
    @Transactional
    public CompletableFuture<Result<List<PnLCalculationResult>, Exception>> savePnLResultsBatch(
            List<MultiBrokerPnLResult> pnlResults) {
        
        return asyncPersistenceEnabled ?
            saveAsync(pnlResults) :
            CompletableFuture.completedFuture(saveSync(pnlResults));
    }
    
    /**
     * Delete old P&L records based on retention policy
     */
    @Transactional
    public Result<Integer, Exception> cleanupOldRecords() {
        var cutoffDate = LocalDate.now().minusDays(retentionDays);
        
        return Result.of(() -> repository.deleteByCalculationDateBefore(cutoffDate))
            .map(this::auditCleanupOperation);
    }
    
    /**
     * Get P&L statistics for user
     */
    @Transactional(readOnly = true)
    public Result<PnLStatistics, Exception> getPnLStatistics(String userId) {
        return Validation.USER_ID.apply(userId)
            .map(validUserId -> repository.findStatisticsByUserId(validUserId))
            .map(this::convertToStatistics)
            .flatMap(this::validateStatistics);
    }
    
    // ============================================================================
    // INTERNAL PERSISTENCE METHODS
    // ============================================================================
    
    private CompletableFuture<Result<List<PnLCalculationResult>, Exception>> saveAsync(
            List<MultiBrokerPnLResult> pnlResults) {
        
        return CompletableFuture.supplyAsync(() -> 
            Result.of(() -> pnlResults.stream()
                .map(this::convertToEntity)
                .map(entity -> entity.getOrElse(() -> null))
                .filter(entity -> entity != null)
                .toList())
                .map(repository::saveAll)
                .map(this::auditBatchSaveOperation),
            Thread.ofVirtual().factory());
    }
    
    private Result<List<PnLCalculationResult>, Exception> saveSync(List<MultiBrokerPnLResult> pnlResults) {
        return Result.of(() -> pnlResults.stream()
            .map(this::convertToEntity)
            .map(result -> result.getOrElse(() -> null))
            .filter(entity -> entity != null)
            .toList())
            .map(repository::saveAll);
    }
    
    private Result<PnLCalculationResult, Exception> convertToEntity(MultiBrokerPnLResult pnlResult) {
        return Result.of(() -> PnLCalculationResult.builder()
            .userId(pnlResult.userId())
            .portfolioId(pnlResult.portfolioId())
            .totalValue(pnlResult.totalValue())
            .unrealizedPnL(pnlResult.unrealizedPnL())
            .realizedPnL(pnlResult.realizedPnL())
            .dayPnL(pnlResult.dayPnL())
            .returnPercent(pnlResult.returnPercent())
            .totalPnL(pnlResult.totalPnL())
            .positionsCount(pnlResult.positionsCount())
            .brokersCount(pnlResult.brokersCount())
            .calculationDate(LocalDate.now())
            .calculatedAt(pnlResult.calculatedAt())
            .correlationId(pnlResult.correlationId())
            .build());
    }
    
    private Result<PnLCalculationResult, Exception> persistEntity(PnLCalculationResult entity) {
        return Result.of(() -> repository.save(entity))
            .mapError(this::handlePersistenceError);
    }
    
    // ============================================================================
    // VALIDATION AND AUDIT METHODS
    // ============================================================================
    
    private Result<MultiBrokerPnLResult, Exception> validatePnLResult(MultiBrokerPnLResult pnlResult) {
        return Optional.ofNullable(pnlResult)
            .filter(result -> result.userId() != null && !result.userId().trim().isEmpty())
            .filter(result -> result.totalValue() != null)
            .map(Result::success)
            .orElse(Result.failure(new IllegalArgumentException("Invalid P&L result data")));
    }
    
    private Result<LocalDate, Exception> validateDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate.isBefore(endDate) || startDate.isEqual(endDate) ?
            Result.success(endDate) :
            Result.failure(new IllegalArgumentException("Invalid date range: start date must be before or equal to end date"));
    }
    
    private PnLCalculationResult auditSaveOperation(PnLCalculationResult saved) {
        log.info("P&L result saved successfully: userId={}, portfolioId={}, correlationId={}", 
            saved.getUserId(), saved.getPortfolioId(), saved.getCorrelationId());
        return saved;
    }
    
    private List<PnLCalculationResult> auditRetrievalOperation(List<PnLCalculationResult> results) {
        log.debug("Retrieved {} P&L results from database", results.size());
        return results;
    }
    
    private Optional<PnLCalculationResult> logLatestResultRetrieval(Optional<PnLCalculationResult> result) {
        log.debug("Latest P&L result retrieval: {}", result.isPresent() ? "found" : "not found");
        return result;
    }
    
    private List<PnLCalculationResult> auditBatchSaveOperation(List<PnLCalculationResult> saved) {
        log.info("Batch saved {} P&L results successfully", saved.size());
        return saved;
    }
    
    private Integer auditCleanupOperation(Integer deletedCount) {
        log.info("Cleaned up {} old P&L records based on retention policy", deletedCount);
        return deletedCount;
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    private PageRequest createPageRequest() {
        return PageRequest.of(0, batchSize, Sort.by("calculatedAt").descending());
    }
    
    private Exception handlePersistenceError(Exception error) {
        log.error("P&L persistence operation failed", error);
        return new RuntimeException("Failed to persist P&L data", error);
    }
    
    private PnLStatistics convertToStatistics(Object[] rawStats) {
        // Assuming repository returns array: [count, avgReturn, totalPnL, maxDrawdown]
        return rawStats.length >= 4 ? 
            new PnLStatistics(
                ((Number) rawStats[0]).intValue(),
                ((Number) rawStats[1]).doubleValue(),
                ((Number) rawStats[2]).doubleValue(),
                ((Number) rawStats[3]).doubleValue()
            ) : PnLStatistics.empty();
    }
    
    private Result<PnLStatistics, Exception> validateStatistics(PnLStatistics stats) {
        return stats.calculationCount() >= 0 ?
            Result.success(stats) :
            Result.failure(new IllegalStateException("Invalid P&L statistics"));
    }
    
    // ============================================================================
    // IMMUTABLE RESULT RECORDS
    // ============================================================================
    
    public record PnLStatistics(
        Integer calculationCount,
        Double averageReturn,
        Double totalPnL,
        Double maxDrawdown
    ) {
        public static PnLStatistics empty() {
            return new PnLStatistics(0, 0.0, 0.0, 0.0);
        }
    }
}