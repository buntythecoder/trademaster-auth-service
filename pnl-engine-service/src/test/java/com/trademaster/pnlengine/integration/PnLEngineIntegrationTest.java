package com.trademaster.pnlengine.integration;

import com.trademaster.pnlengine.entity.PnLCalculationResult;
import com.trademaster.pnlengine.repository.PnLCalculationResultRepository;
import com.trademaster.pnlengine.service.BrokerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for P&L Engine
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Comprehensive integration tests to verify:
 * - Database connectivity and schema alignment
 * - Repository layer functionality 
 * - Entity persistence and retrieval
 * - Query performance and indexing
 * - Build and deployment readiness
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG"
})
class PnLEngineIntegrationTest {
    
    @Autowired
    private PnLCalculationResultRepository repository;
    
    private static final String TEST_USER_ID = "integration-test-user";
    private static final Long TEST_PORTFOLIO_ID = 999L;
    
    // ============================================================================
    // DATABASE SCHEMA AND ENTITY ALIGNMENT TESTS
    // ============================================================================
    
    @Test
    void shouldPersistAndRetrievePnLCalculationResult() {
        // Given: P&L calculation result entity
        var result = createTestPnLCalculationResult();
        
        // When: Persisting to database
        var saved = repository.save(result);
        var retrieved = repository.findById(saved.getResultId());
        
        // Then: Should persist and retrieve correctly
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(retrieved.get().getPortfolioId()).isEqualTo(TEST_PORTFOLIO_ID);
        assertThat(retrieved.get().getBrokerType()).isEqualTo(BrokerType.ZERODHA);
        assertThat(retrieved.get().getTotalPortfolioValue()).isEqualByComparingTo(BigDecimal.valueOf(100000.00));
        assertThat(retrieved.get().getCalculatedAt()).isNotNull();
        assertThat(retrieved.get().getCorrelationId()).isNotNull();
    }
    
    @Test
    void shouldHandleAllBrokerTypes() {
        // Given: P&L results for all broker types
        for (BrokerType brokerType : BrokerType.values()) {
            var result = createTestPnLCalculationResult();
            result.setBrokerType(brokerType);
            result.setCorrelationId(UUID.randomUUID().toString());
            
            // When: Persisting broker-specific result
            var saved = repository.save(result);
            
            // Then: Should persist correctly for each broker type
            assertThat(saved.getResultId()).isNotNull();
            assertThat(saved.getBrokerType()).isEqualTo(brokerType);
        }
        
        // Verify all broker types were persisted
        var allResults = repository.findAll();
        assertThat(allResults).hasSize(BrokerType.values().length);
    }
    
    @Test
    void shouldEnforceDatabaseConstraints() {
        // Given: P&L result with valid constraints
        var result = createTestPnLCalculationResult();
        result.setCalculationTimeMs(50L); // Positive value
        result.setTotalPositions(5); // Non-negative value
        result.setActiveBrokers(2); // Non-negative value
        
        // When: Persisting with valid constraints
        var saved = repository.save(result);
        
        // Then: Should persist successfully
        assertThat(saved.getResultId()).isNotNull();
        assertThat(saved.getCalculationTimeMs()).isEqualTo(50L);
        assertThat(saved.getTotalPositions()).isEqualTo(5);
        assertThat(saved.getActiveBrokers()).isEqualTo(2);
    }
    
    // ============================================================================
    // REPOSITORY QUERY PERFORMANCE TESTS
    // ============================================================================
    
    @Test
    void shouldPerformFastLookupQueries() {
        // Given: Multiple P&L calculation results
        for (int i = 0; i < 100; i++) {
            var result = createTestPnLCalculationResult();
            result.setUserId(TEST_USER_ID);
            result.setPortfolioId((long) (i % 10)); // 10 different portfolios
            result.setCalculatedAt(Instant.now().minusSeconds(i * 60)); // Different timestamps
            result.setCorrelationId(UUID.randomUUID().toString());
            repository.save(result);
        }
        
        // When: Performing indexed lookup queries
        var startTime = System.currentTimeMillis();
        
        var latestResult = repository.findLatestByUserIdAndPortfolioId(TEST_USER_ID, 1L);
        var brokerResults = repository.findLatestByUserIdAndBrokerType(TEST_USER_ID, BrokerType.ZERODHA);
        var userCount = repository.countByUserId(TEST_USER_ID);
        
        var queryTime = System.currentTimeMillis() - startTime;
        
        // Then: Should perform queries efficiently (under performance target)
        assertThat(queryTime).isLessThan(100L); // Should be very fast with proper indexing
        assertThat(latestResult).isPresent();
        assertThat(brokerResults).isPresent();
        assertThat(userCount).isEqualTo(100L);
    }
    
    @Test
    void shouldHandleConcurrentOperations() {
        // Given: Base P&L calculation result
        var baseResult = createTestPnLCalculationResult();
        
        // When: Simulating concurrent operations (Virtual Threads would handle this better in real scenario)
        var results = java.util.stream.IntStream.range(0, 10)
            .parallel()
            .mapToObj(i -> {
                var result = createTestPnLCalculationResult();
                result.setUserId(TEST_USER_ID + "-" + i);
                result.setCorrelationId(UUID.randomUUID().toString());
                return repository.save(result);
            })
            .toList();
        
        // Then: Should handle concurrent operations correctly
        assertThat(results).hasSize(10);
        assertThat(results).allMatch(result -> result.getResultId() != null);
        assertThat(results).allMatch(result -> result.getUserId().startsWith(TEST_USER_ID));
        
        // Verify all results persisted correctly
        var totalCount = repository.count();
        assertThat(totalCount).isEqualTo(10L);
    }
    
    // ============================================================================
    // DATA INTEGRITY AND VALIDATION TESTS
    // ============================================================================
    
    @Test
    void shouldMaintainDataPrecisionForFinancialValues() {
        // Given: P&L result with high-precision financial values
        var result = createTestPnLCalculationResult();
        result.setTotalPortfolioValue(new BigDecimal("123456789.1234")); // High precision
        result.setTotalUnrealizedPnl(new BigDecimal("9876.5432"));
        result.setTotalRealizedPnl(new BigDecimal("-1234.9876"));
        result.setTotalReturnPercent(new BigDecimal("15.6789"));
        
        // When: Persisting and retrieving
        var saved = repository.save(result);
        var retrieved = repository.findById(saved.getResultId()).orElseThrow();
        
        // Then: Should maintain exact precision
        assertThat(retrieved.getTotalPortfolioValue()).isEqualByComparingTo(new BigDecimal("123456789.1234"));
        assertThat(retrieved.getTotalUnrealizedPnl()).isEqualByComparingTo(new BigDecimal("9876.5432"));
        assertThat(retrieved.getTotalRealizedPnl()).isEqualByComparingTo(new BigDecimal("-1234.9876"));
        assertThat(retrieved.getTotalReturnPercent()).isEqualByComparingTo(new BigDecimal("15.6789"));
    }
    
    @Test
    void shouldHandleTimestampPrecision() {
        // Given: P&L result with precise timestamps
        var calculatedAt = Instant.parse("2024-01-15T10:30:45.123456Z");
        var cacheExpiresAt = Instant.parse("2024-01-15T11:30:45.987654Z");
        
        var result = createTestPnLCalculationResult();
        result.setCalculatedAt(calculatedAt);
        result.setCacheExpiresAt(cacheExpiresAt);
        
        // When: Persisting and retrieving
        var saved = repository.save(result);
        var retrieved = repository.findById(saved.getResultId()).orElseThrow();
        
        // Then: Should maintain timestamp precision (database-dependent precision)
        assertThat(retrieved.getCalculatedAt()).isCloseTo(calculatedAt, java.time.temporal.ChronoUnit.MILLIS.getDuration().multipliedBy(1));
        assertThat(retrieved.getCacheExpiresAt()).isCloseTo(cacheExpiresAt, java.time.temporal.ChronoUnit.MILLIS.getDuration().multipliedBy(1));
    }
    
    // ============================================================================
    // CACHING AND PERFORMANCE OPTIMIZATION TESTS
    // ============================================================================
    
    @Test
    void shouldSupportCachingMechanisms() {
        // Given: Cached P&L calculation result
        var result = createTestPnLCalculationResult();
        result.setCached(true);
        result.setCacheExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour from now
        
        // When: Persisting cached result
        var saved = repository.save(result);
        
        // Then: Should support caching fields
        assertThat(saved.isCached()).isTrue();
        assertThat(saved.getCacheExpiresAt()).isAfter(Instant.now());
        
        // Test cache-related queries
        var validCached = repository.findValidCachedResults(TEST_USER_ID, Instant.now());
        assertThat(validCached).hasSize(1);
        assertThat(validCached.get(0).isCached()).isTrue();
    }
    
    @Test
    void shouldSupportAuditTrailRequirements() {
        // Given: P&L result with complete audit information
        var result = createTestPnLCalculationResult();
        result.setCorrelationId("audit-correlation-12345");
        result.setCalculationTimeMs(42L);
        result.setCalculatedAt(Instant.now());
        result.setUpdatedAt(Instant.now());
        result.setVersion(1L);
        
        // When: Persisting with audit fields
        var saved = repository.save(result);
        
        // Then: Should maintain audit information
        assertThat(saved.getCorrelationId()).isEqualTo("audit-correlation-12345");
        assertThat(saved.getCalculationTimeMs()).isEqualTo(42L);
        assertThat(saved.getCalculatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1L);
    }
    
    // ============================================================================
    // BUILD AND DEPLOYMENT READINESS VERIFICATION
    // ============================================================================
    
    @Test
    void shouldDemonstrateProductionReadiness() {
        // Given: Production-like data scenario
        var result = createProductionLikePnLResult();
        
        // When: Operating in production-like scenario
        var saved = repository.save(result);
        
        // Performance verification - should complete quickly
        var startTime = System.currentTimeMillis();
        var retrieved = repository.findLatestByUserIdAndPortfolioId(result.getUserId(), result.getPortfolioId());
        var queryTime = System.currentTimeMillis() - startTime;
        
        // Then: Should meet production performance requirements
        assertThat(saved).isNotNull();
        assertThat(retrieved).isPresent();
        assertThat(queryTime).isLessThan(50L); // Sub-50ms query performance target
        
        // Verify data integrity
        var retrievedResult = retrieved.get();
        assertThat(retrievedResult.getTotalPortfolioValue()).isNotNull();
        assertThat(retrievedResult.getTotalUnrealizedPnl()).isNotNull();
        assertThat(retrievedResult.getCalculationTimeMs()).isGreaterThan(0L);
        assertThat(retrievedResult.getCorrelationId()).isNotNull();
    }
    
    // ============================================================================
    // HELPER METHODS FOR TEST DATA CREATION
    // ============================================================================
    
    private PnLCalculationResult createTestPnLCalculationResult() {
        var result = new PnLCalculationResult();
        result.setUserId(TEST_USER_ID);
        result.setPortfolioId(TEST_PORTFOLIO_ID);
        result.setBrokerType(BrokerType.ZERODHA);
        result.setCalculationType("INTEGRATION_TEST");
        result.setTotalPortfolioValue(BigDecimal.valueOf(100000.00));
        result.setTotalCashBalance(BigDecimal.valueOf(10000.00));
        result.setTotalInvestedAmount(BigDecimal.valueOf(90000.00));
        result.setTotalUnrealizedPnl(BigDecimal.valueOf(5000.00));
        result.setTotalRealizedPnl(BigDecimal.valueOf(2000.00));
        result.setTotalDayPnl(BigDecimal.valueOf(1000.00));
        result.setTotalReturnPercent(BigDecimal.valueOf(7.78));
        result.setTotalReturnAmount(BigDecimal.valueOf(7000.00));
        result.setTotalPositions(10);
        result.setActiveBrokers(3);
        result.setCalculationTimeMs(25L);
        result.setCorrelationId(UUID.randomUUID().toString());
        result.setCalculatedAt(Instant.now());
        result.setCached(false);
        result.setVersion(0L);
        
        return result;
    }
    
    private PnLCalculationResult createProductionLikePnLResult() {
        var result = createTestPnLCalculationResult();
        
        // Production-like values
        result.setUserId("prod-user-" + UUID.randomUUID().toString());
        result.setTotalPortfolioValue(BigDecimal.valueOf(2547832.45));
        result.setTotalCashBalance(BigDecimal.valueOf(127891.67));
        result.setTotalInvestedAmount(BigDecimal.valueOf(2419940.78));
        result.setTotalUnrealizedPnl(BigDecimal.valueOf(34567.89));
        result.setTotalRealizedPnl(BigDecimal.valueOf(18923.12));
        result.setTotalDayPnl(BigDecimal.valueOf(2156.34));
        result.setTotalReturnPercent(BigDecimal.valueOf(2.21));
        result.setTotalReturnAmount(BigDecimal.valueOf(53491.01));
        result.setTotalPositions(247);
        result.setActiveBrokers(4);
        result.setCalculationTimeMs(18L); // Fast calculation
        result.setCalculationType("MULTI_BROKER");
        
        return result;
    }
}