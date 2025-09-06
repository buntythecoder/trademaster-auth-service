package com.trademaster.pnlengine.repository;

import com.trademaster.pnlengine.entity.PnLCalculationResult;
import com.trademaster.pnlengine.service.BrokerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PnL Calculation Result Repository
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Comprehensive test coverage for repository operations including:
 * - Basic CRUD operations
 * - Custom query methods
 * - Performance optimization queries
 * - Caching and aggregation methods
 * - Data quality validation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
@DataJpaTest
class PnLCalculationResultRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PnLCalculationResultRepository repository;
    
    private static final String TEST_USER_ID = "test-user-123";
    private static final Long TEST_PORTFOLIO_ID = 1L;
    private static final String TEST_CORRELATION_ID = "test-correlation-123";
    
    private PnLCalculationResult testResult;
    
    @BeforeEach
    void setUp() {
        testResult = createTestPnLCalculationResult(
            TEST_USER_ID, TEST_PORTFOLIO_ID, BrokerType.ZERODHA, "MULTI_BROKER",
            BigDecimal.valueOf(100000.00), BigDecimal.valueOf(10000.00), BigDecimal.valueOf(90000.00),
            BigDecimal.valueOf(5000.00), BigDecimal.valueOf(2000.00), BigDecimal.valueOf(1000.00),
            BigDecimal.valueOf(7.78), BigDecimal.valueOf(7000.00), 10, 3
        );
    }
    
    // ============================================================================
    // CORE QUERY METHOD TESTS
    // ============================================================================
    
    @Test
    void findLatestByUserIdAndPortfolioId_ShouldReturnLatestResult() {
        // Given: Multiple P&L calculation results for same user/portfolio
        var olderResult = createTestPnLCalculationResult(
            TEST_USER_ID, TEST_PORTFOLIO_ID, BrokerType.UPSTOX, "MULTI_BROKER",
            BigDecimal.valueOf(95000.00), BigDecimal.valueOf(9000.00), BigDecimal.valueOf(86000.00),
            BigDecimal.valueOf(4000.00), BigDecimal.valueOf(1500.00), BigDecimal.valueOf(500.00),
            BigDecimal.valueOf(6.98), BigDecimal.valueOf(5500.00), 8, 2
        );
        olderResult.setCalculatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        
        testResult.setCalculatedAt(Instant.now());
        
        entityManager.persistAndFlush(olderResult);
        entityManager.persistAndFlush(testResult);
        
        // When: Finding latest calculation
        var result = repository.findLatestByUserIdAndPortfolioId(TEST_USER_ID, TEST_PORTFOLIO_ID);
        
        // Then: Should return the most recent result
        assertThat(result).isPresent();
        assertThat(result.get().getResultId()).isEqualTo(testResult.getResultId());
        assertThat(result.get().getTotalPortfolioValue()).isEqualByComparingTo(BigDecimal.valueOf(100000.00));
    }
    
    @Test
    void findLatestByUserIdAndBrokerType_ShouldReturnLatestBrokerResult() {
        // Given: P&L calculation result for specific broker
        testResult.setBrokerType(BrokerType.ZERODHA);
        entityManager.persistAndFlush(testResult);
        
        // When: Finding latest broker-specific calculation
        var result = repository.findLatestByUserIdAndBrokerType(TEST_USER_ID, BrokerType.ZERODHA);
        
        // Then: Should return the result for that broker
        assertThat(result).isPresent();
        assertThat(result.get().getBrokerType()).isEqualTo(BrokerType.ZERODHA);
        assertThat(result.get().getUserId()).isEqualTo(TEST_USER_ID);
    }
    
    @Test
    void findByUserIdAndCalculatedAtBetween_ShouldReturnResultsInDateRange() {
        // Given: Multiple results with different calculation times
        var startDate = Instant.now().minus(2, ChronoUnit.DAYS);
        var endDate = Instant.now();
        
        testResult.setCalculatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entityManager.persistAndFlush(testResult);
        
        // When: Finding results in date range
        var results = repository.findByUserIdAndCalculatedAtBetween(
            TEST_USER_ID, startDate, endDate, PageRequest.of(0, 10));
        
        // Then: Should return results within date range
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getUserId()).isEqualTo(TEST_USER_ID);
    }
    
    @Test
    void findByUserIdAndCalculationType_ShouldReturnResultsByType() {
        // Given: P&L calculation result with specific calculation type
        testResult.setCalculationType("MULTI_BROKER");
        entityManager.persistAndFlush(testResult);
        
        // When: Finding results by calculation type
        var results = repository.findByUserIdAndCalculationType(
            TEST_USER_ID, "MULTI_BROKER", PageRequest.of(0, 10));
        
        // Then: Should return results of that calculation type
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCalculationType()).isEqualTo("MULTI_BROKER");
    }
    
    // ============================================================================
    // PERFORMANCE AND CACHING TESTS
    // ============================================================================
    
    @Test
    void findValidCachedResults_ShouldReturnOnlyValidCache() {
        // Given: Cached result that is still valid
        testResult.setCached(true);
        testResult.setCacheExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        entityManager.persistAndFlush(testResult);
        
        // When: Finding valid cached results
        var results = repository.findValidCachedResults(TEST_USER_ID, Instant.now());
        
        // Then: Should return the valid cached result
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isCached()).isTrue();
        assertThat(results.get(0).getCacheExpiresAt()).isAfter(Instant.now());
    }
    
    @Test
    void findFastCalculations_ShouldReturnOnlyFastResults() {
        // Given: P&L calculation with fast execution time
        testResult.setCalculationTimeMs(25L); // Fast calculation
        entityManager.persistAndFlush(testResult);
        
        var slowResult = createTestPnLCalculationResult(
            TEST_USER_ID, 2L, BrokerType.ANGEL_ONE, "BROKER_SPECIFIC",
            BigDecimal.valueOf(50000.00), BigDecimal.valueOf(5000.00), BigDecimal.valueOf(45000.00),
            BigDecimal.valueOf(2500.00), BigDecimal.valueOf(1000.00), BigDecimal.valueOf(500.00),
            BigDecimal.valueOf(5.56), BigDecimal.valueOf(3500.00), 5, 1
        );
        slowResult.setCalculationTimeMs(150L); // Slow calculation
        entityManager.persistAndFlush(slowResult);
        
        // When: Finding calculations under 50ms
        var results = repository.findFastCalculations(TEST_USER_ID, 50L);
        
        // Then: Should return only the fast calculation
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCalculationTimeMs()).isLessThanOrEqualTo(50L);
    }
    
    // ============================================================================
    // AGGREGATION AND ANALYTICS TESTS
    // ============================================================================
    
    @Test
    void countCalculationsByBrokerType_ShouldReturnCorrectCounts() {
        // Given: Multiple calculations for different brokers
        testResult.setBrokerType(BrokerType.ZERODHA);
        entityManager.persistAndFlush(testResult);
        
        var upstoxResult = createTestPnLCalculationResult(
            TEST_USER_ID, 2L, BrokerType.UPSTOX, "BROKER_SPECIFIC",
            BigDecimal.valueOf(75000.00), BigDecimal.valueOf(7500.00), BigDecimal.valueOf(67500.00),
            BigDecimal.valueOf(3750.00), BigDecimal.valueOf(1250.00), BigDecimal.valueOf(750.00),
            BigDecimal.valueOf(5.56), BigDecimal.valueOf(5000.00), 7, 1
        );
        entityManager.persistAndFlush(upstoxResult);
        
        // When: Counting calculations by broker type
        var counts = repository.countCalculationsByBrokerType(TEST_USER_ID);
        
        // Then: Should return correct counts for each broker
        assertThat(counts).hasSize(2);
        var brokerTypeCounts = counts.stream()
            .collect(java.util.stream.Collectors.toMap(
                arr -> (BrokerType) arr[0], 
                arr -> (Long) arr[1]
            ));
        
        assertThat(brokerTypeCounts.get(BrokerType.ZERODHA)).isEqualTo(1L);
        assertThat(brokerTypeCounts.get(BrokerType.UPSTOX)).isEqualTo(1L);
    }
    
    @Test
    void getAverageCalculationTimeByType_ShouldReturnCorrectAverages() {
        // Given: Multiple calculations with different times for same type
        testResult.setCalculationType("MULTI_BROKER");
        testResult.setCalculationTimeMs(30L);
        entityManager.persistAndFlush(testResult);
        
        var anotherResult = createTestPnLCalculationResult(
            TEST_USER_ID, 3L, BrokerType.ANGEL_ONE, "MULTI_BROKER",
            BigDecimal.valueOf(120000.00), BigDecimal.valueOf(12000.00), BigDecimal.valueOf(108000.00),
            BigDecimal.valueOf(6000.00), BigDecimal.valueOf(2500.00), BigDecimal.valueOf(1200.00),
            BigDecimal.valueOf(5.56), BigDecimal.valueOf(8500.00), 12, 1
        );
        anotherResult.setCalculationTimeMs(50L);
        entityManager.persistAndFlush(anotherResult);
        
        // When: Getting average calculation time by type
        var averages = repository.getAverageCalculationTimeByType(TEST_USER_ID);
        
        // Then: Should return correct average (30 + 50) / 2 = 40
        assertThat(averages).hasSize(1);
        var calculationType = (String) averages.get(0)[0];
        var averageTime = (Double) averages.get(0)[1];
        
        assertThat(calculationType).isEqualTo("MULTI_BROKER");
        assertThat(averageTime).isEqualTo(40.0);
    }
    
    @Test
    void getPortfolioValueTrend_ShouldReturnTrendData() {
        // Given: Multiple portfolio values over time
        var startDate = Instant.now().minus(7, ChronoUnit.DAYS);
        var endDate = Instant.now();
        
        testResult.setCalculationType("MULTI_BROKER");
        testResult.setTotalPortfolioValue(BigDecimal.valueOf(100000.00));
        testResult.setCalculatedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        entityManager.persistAndFlush(testResult);
        
        var recentResult = createTestPnLCalculationResult(
            TEST_USER_ID, TEST_PORTFOLIO_ID, BrokerType.ZERODHA, "MULTI_BROKER",
            BigDecimal.valueOf(105000.00), BigDecimal.valueOf(10500.00), BigDecimal.valueOf(94500.00),
            BigDecimal.valueOf(5250.00), BigDecimal.valueOf(2100.00), BigDecimal.valueOf(1050.00),
            BigDecimal.valueOf(5.56), BigDecimal.valueOf(7350.00), 11, 3
        );
        recentResult.setCalculatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entityManager.persistAndFlush(recentResult);
        
        // When: Getting portfolio value trend
        var trendData = repository.getPortfolioValueTrend(TEST_USER_ID, startDate, endDate);
        
        // Then: Should return trend data points
        assertThat(trendData).hasSize(2);
        var firstPoint = trendData.get(0);
        var secondPoint = trendData.get(1);
        
        assertThat((BigDecimal) firstPoint[1]).isEqualByComparingTo(BigDecimal.valueOf(100000.00));
        assertThat((BigDecimal) secondPoint[1]).isEqualByComparingTo(BigDecimal.valueOf(105000.00));
    }
    
    // ============================================================================
    // AUDIT AND MAINTENANCE TESTS
    // ============================================================================
    
    @Test
    void findByCorrelationId_ShouldReturnRelatedCalculations() {
        // Given: P&L calculation with specific correlation ID
        testResult.setCorrelationId(TEST_CORRELATION_ID);
        entityManager.persistAndFlush(testResult);
        
        // When: Finding calculations by correlation ID
        var results = repository.findByCorrelationId(TEST_CORRELATION_ID);
        
        // Then: Should return related calculations
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    }
    
    @Test
    void findExpiredCachedResults_ShouldReturnExpiredCache() {
        // Given: Cached result that has expired
        testResult.setCached(true);
        testResult.setCacheExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        entityManager.persistAndFlush(testResult);
        
        // When: Finding expired cached results
        var results = repository.findExpiredCachedResults(Instant.now());
        
        // Then: Should return the expired cached result
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isCached()).isTrue();
        assertThat(results.get(0).getCacheExpiresAt()).isBefore(Instant.now());
    }
    
    // ============================================================================
    // VALIDATION AND QUALITY TESTS
    // ============================================================================
    
    @Test
    void findCalculationsWithQualityIssues_ShouldReturnProblematicResults() {
        // Given: P&L calculation with quality issues (negative portfolio value)
        testResult.setTotalPortfolioValue(BigDecimal.valueOf(-1000.00)); // Quality issue
        testResult.setTotalReturnPercent(BigDecimal.valueOf(1500.00)); // Over 1000% return - quality issue
        entityManager.persistAndFlush(testResult);
        
        var goodResult = createTestPnLCalculationResult(
            TEST_USER_ID, 2L, BrokerType.UPSTOX, "BROKER_SPECIFIC",
            BigDecimal.valueOf(50000.00), BigDecimal.valueOf(5000.00), BigDecimal.valueOf(45000.00),
            BigDecimal.valueOf(2500.00), BigDecimal.valueOf(1000.00), BigDecimal.valueOf(500.00),
            BigDecimal.valueOf(5.56), BigDecimal.valueOf(3500.00), 5, 1
        );
        entityManager.persistAndFlush(goodResult);
        
        // When: Finding calculations with quality issues
        var results = repository.findCalculationsWithQualityIssues(TEST_USER_ID);
        
        // Then: Should return only the problematic result
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalPortfolioValue()).isLessThan(BigDecimal.ZERO);
    }
    
    // ============================================================================
    // COUNT AND EXISTS TESTS  
    // ============================================================================
    
    @Test
    void existsByUserIdAndPortfolioIdAndCalculatedAtAfter_ShouldDetectRecentCalculations() {
        // Given: Recent P&L calculation
        testResult.setCalculatedAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        entityManager.persistAndFlush(testResult);
        
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        
        // When: Checking if recent calculation exists
        var exists = repository.existsByUserIdAndPortfolioIdAndCalculatedAtAfter(
            TEST_USER_ID, TEST_PORTFOLIO_ID, oneHourAgo);
        
        // Then: Should detect the recent calculation
        assertThat(exists).isTrue();
    }
    
    @Test
    void countByUserId_ShouldReturnCorrectTotalCount() {
        // Given: Multiple calculations for user
        entityManager.persistAndFlush(testResult);
        
        var anotherResult = createTestPnLCalculationResult(
            TEST_USER_ID, 2L, BrokerType.UPSTOX, "BROKER_SPECIFIC",
            BigDecimal.valueOf(75000.00), BigDecimal.valueOf(7500.00), BigDecimal.valueOf(67500.00),
            BigDecimal.valueOf(3750.00), BigDecimal.valueOf(1250.00), BigDecimal.valueOf(750.00),
            BigDecimal.valueOf(5.00), BigDecimal.valueOf(5000.00), 7, 1
        );
        entityManager.persistAndFlush(anotherResult);
        
        // When: Counting total calculations for user
        var count = repository.countByUserId(TEST_USER_ID);
        
        // Then: Should return correct count
        assertThat(count).isEqualTo(2L);
    }
    
    @Test
    void countByUserIdAndBrokerType_ShouldReturnBrokerSpecificCount() {
        // Given: Calculations for different brokers
        testResult.setBrokerType(BrokerType.ZERODHA);
        entityManager.persistAndFlush(testResult);
        
        var upstoxResult = createTestPnLCalculationResult(
            TEST_USER_ID, 2L, BrokerType.UPSTOX, "BROKER_SPECIFIC",
            BigDecimal.valueOf(50000.00), BigDecimal.valueOf(5000.00), BigDecimal.valueOf(45000.00),
            BigDecimal.valueOf(2500.00), BigDecimal.valueOf(1000.00), BigDecimal.valueOf(500.00),
            BigDecimal.valueOf(5.56), BigDecimal.valueOf(3500.00), 5, 1
        );
        entityManager.persistAndFlush(upstoxResult);
        
        // When: Counting calculations for specific broker
        var zerodhaCount = repository.countByUserIdAndBrokerType(TEST_USER_ID, BrokerType.ZERODHA);
        var upstoxCount = repository.countByUserIdAndBrokerType(TEST_USER_ID, BrokerType.UPSTOX);
        
        // Then: Should return correct counts for each broker
        assertThat(zerodhaCount).isEqualTo(1L);
        assertThat(upstoxCount).isEqualTo(1L);
    }
    
    // ============================================================================
    // HELPER METHODS FOR TEST DATA CREATION
    // ============================================================================
    
    private PnLCalculationResult createTestPnLCalculationResult(
            String userId, Long portfolioId, BrokerType brokerType, String calculationType,
            BigDecimal totalPortfolioValue, BigDecimal totalCashBalance, BigDecimal totalInvestedAmount,
            BigDecimal totalUnrealizedPnL, BigDecimal totalRealizedPnL, BigDecimal totalDayPnL,
            BigDecimal totalReturnPercent, BigDecimal totalReturnAmount,
            Integer totalPositions, Integer activeBrokers) {
        
        var result = new PnLCalculationResult();
        result.setUserId(userId);
        result.setPortfolioId(portfolioId);
        result.setBrokerType(brokerType);
        result.setCalculationType(calculationType);
        result.setTotalPortfolioValue(totalPortfolioValue);
        result.setTotalCashBalance(totalCashBalance);
        result.setTotalInvestedAmount(totalInvestedAmount);
        result.setTotalUnrealizedPnl(totalUnrealizedPnL);
        result.setTotalRealizedPnl(totalRealizedPnL);
        result.setTotalDayPnl(totalDayPnL);
        result.setTotalReturnPercent(totalReturnPercent);
        result.setTotalReturnAmount(totalReturnAmount);
        result.setTotalPositions(totalPositions);
        result.setActiveBrokers(activeBrokers);
        result.setCalculationTimeMs(25L); // Default fast calculation time
        result.setCorrelationId(UUID.randomUUID().toString());
        result.setCalculatedAt(Instant.now());
        result.setCached(false);
        
        return result;
    }
}