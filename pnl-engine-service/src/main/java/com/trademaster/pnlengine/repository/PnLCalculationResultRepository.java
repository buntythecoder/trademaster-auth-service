package com.trademaster.pnlengine.repository;

import com.trademaster.pnlengine.entity.PnLCalculationResult;
import com.trademaster.pnlengine.service.BrokerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * P&L Calculation Result Repository
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance repository for P&L calculation results with optimized queries
 * for multi-broker portfolio analysis and caching support.
 * 
 * Key Features:
 * - Optimized queries with proper indexing
 * - Multi-broker aggregation support
 * - Cache-aware query methods
 * - Performance metrics tracking
 * - Audit trail maintenance
 * 
 * Performance Targets:
 * - Query execution: <10ms for indexed lookups
 * - Batch operations: <50ms for 100+ records
 * - Aggregation queries: <25ms for portfolio summaries
 * - Cache hit ratio: >90% for repeated queries
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
@Repository
public interface PnLCalculationResultRepository extends JpaRepository<PnLCalculationResult, Long> {
    
    // ============================================================================
    // CORE QUERY METHODS
    // ============================================================================
    
    /**
     * Find latest P&L calculation for user and portfolio
     * 
     * @param userId User identifier
     * @param portfolioId Portfolio identifier
     * @return Latest P&L calculation result
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND p.portfolioId = :portfolioId " +
           "ORDER BY p.calculatedAt DESC LIMIT 1")
    Optional<PnLCalculationResult> findLatestByUserIdAndPortfolioId(
        @Param("userId") String userId, 
        @Param("portfolioId") Long portfolioId);
    
    /**
     * Find latest P&L calculation for user and broker
     * 
     * @param userId User identifier
     * @param brokerType Broker type
     * @return Latest broker-specific P&L result
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND p.brokerType = :brokerType " +
           "ORDER BY p.calculatedAt DESC LIMIT 1")
    Optional<PnLCalculationResult> findLatestByUserIdAndBrokerType(
        @Param("userId") String userId, 
        @Param("brokerType") BrokerType brokerType);
    
    /**
     * Find all P&L calculations for user within date range
     * 
     * @param userId User identifier
     * @param fromDate Start date
     * @param toDate End date
     * @param pageable Pagination parameters
     * @return Page of P&L calculation results
     */
    Page<PnLCalculationResult> findByUserIdAndCalculatedAtBetween(
        String userId, Instant fromDate, Instant toDate, Pageable pageable);
    
    /**
     * Find all P&L calculations for specific calculation type
     * 
     * @param userId User identifier
     * @param calculationType Calculation type (MULTI_BROKER, BROKER_SPECIFIC, etc.)
     * @param pageable Pagination parameters
     * @return Page of P&L results by calculation type
     */
    Page<PnLCalculationResult> findByUserIdAndCalculationType(
        String userId, String calculationType, Pageable pageable);
    
    // ============================================================================
    // PERFORMANCE AND CACHING QUERIES
    // ============================================================================
    
    /**
     * Find cached P&L results that are still valid
     * 
     * @param userId User identifier
     * @param now Current timestamp
     * @return List of valid cached results
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND p.isCached = true " +
           "AND p.cacheExpiresAt > :now " +
           "ORDER BY p.calculatedAt DESC")
    List<PnLCalculationResult> findValidCachedResults(
        @Param("userId") String userId, 
        @Param("now") Instant now);
    
    /**
     * Find calculations that meet performance targets
     * 
     * @param userId User identifier
     * @param maxCalculationTime Maximum calculation time in ms
     * @return List of fast calculations
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND p.calculationTimeMs <= :maxCalculationTime " +
           "ORDER BY p.calculationTimeMs ASC")
    List<PnLCalculationResult> findFastCalculations(
        @Param("userId") String userId, 
        @Param("maxCalculationTime") Long maxCalculationTime);
    
    // ============================================================================
    // AGGREGATION AND ANALYTICS QUERIES
    // ============================================================================
    
    /**
     * Count P&L calculations by broker type for user
     * 
     * @param userId User identifier
     * @return Count of calculations per broker type
     */
    @Query("SELECT p.brokerType, COUNT(p) FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId " +
           "GROUP BY p.brokerType")
    List<Object[]> countCalculationsByBrokerType(@Param("userId") String userId);
    
    /**
     * Get average calculation time by calculation type
     * 
     * @param userId User identifier
     * @return Average calculation time per type
     */
    @Query("SELECT p.calculationType, AVG(p.calculationTimeMs) FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId " +
           "GROUP BY p.calculationType")
    List<Object[]> getAverageCalculationTimeByType(@Param("userId") String userId);
    
    /**
     * Find total portfolio value trend over time
     * 
     * @param userId User identifier
     * @param fromDate Start date for trend analysis
     * @param toDate End date for trend analysis
     * @return Portfolio value trend data
     */
    @Query("SELECT p.calculatedAt, p.totalPortfolioValue FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND p.calculationType = 'MULTI_BROKER' " +
           "AND p.calculatedAt BETWEEN :fromDate AND :toDate " +
           "ORDER BY p.calculatedAt ASC")
    List<Object[]> getPortfolioValueTrend(
        @Param("userId") String userId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate);
    
    // ============================================================================
    // AUDIT AND MAINTENANCE QUERIES
    // ============================================================================
    
    /**
     * Find calculations by correlation ID for distributed tracing
     * 
     * @param correlationId Correlation identifier
     * @return List of related calculations
     */
    List<PnLCalculationResult> findByCorrelationId(String correlationId);
    
    /**
     * Delete old calculation results for data retention compliance
     * 
     * @param cutoffDate Cutoff date for deletion
     * @return Number of deleted records
     */
    @Query("DELETE FROM PnLCalculationResult p " +
           "WHERE p.calculatedAt < :cutoffDate AND p.isCached = false")
    int deleteOldCalculations(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find calculations requiring cache refresh
     * 
     * @param now Current timestamp
     * @return List of expired cached results
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.isCached = true AND p.cacheExpiresAt <= :now " +
           "ORDER BY p.cacheExpiresAt ASC")
    List<PnLCalculationResult> findExpiredCachedResults(@Param("now") Instant now);
    
    // ============================================================================
    // VALIDATION AND QUALITY QUERIES
    // ============================================================================
    
    /**
     * Find calculations with potential data quality issues
     * 
     * @param userId User identifier
     * @return Calculations that may need validation
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND (" +
           "p.totalPortfolioValue < 0 OR " +
           "p.totalReturnPercent > 1000 OR " +
           "p.totalReturnPercent < -100 OR " +
           "p.calculationTimeMs > 10000)")
    List<PnLCalculationResult> findCalculationsWithQualityIssues(@Param("userId") String userId);
    
    /**
     * Verify calculation consistency across brokers
     * 
     * @param userId User identifier
     * @param calculatedAt Specific calculation timestamp
     * @return Related calculations for consistency check
     */
    @Query("SELECT p FROM PnLCalculationResult p " +
           "WHERE p.userId = :userId AND p.calculatedAt = :calculatedAt " +
           "ORDER BY p.brokerType")
    List<PnLCalculationResult> findCalculationsForConsistencyCheck(
        @Param("userId") String userId,
        @Param("calculatedAt") Instant calculatedAt);
    
    // ============================================================================
    // EXISTS AND COUNT METHODS
    // ============================================================================
    
    /**
     * Check if recent calculation exists for user and portfolio
     * 
     * @param userId User identifier
     * @param portfolioId Portfolio identifier
     * @param cutoffTime Recent calculation cutoff
     * @return True if recent calculation exists
     */
    boolean existsByUserIdAndPortfolioIdAndCalculatedAtAfter(
        String userId, Long portfolioId, Instant cutoffTime);
    
    /**
     * Count total calculations for user
     * 
     * @param userId User identifier
     * @return Total calculation count
     */
    long countByUserId(String userId);
    
    /**
     * Count calculations by user and broker type
     * 
     * @param userId User identifier
     * @param brokerType Broker type
     * @return Calculation count for broker
     */
    long countByUserIdAndBrokerType(String userId, BrokerType brokerType);
}