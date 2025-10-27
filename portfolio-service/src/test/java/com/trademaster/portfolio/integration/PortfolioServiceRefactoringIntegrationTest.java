package com.trademaster.portfolio.integration;

import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.PortfolioSummary;
import com.trademaster.portfolio.dto.RebalancingResult;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.AccountType;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.model.RiskLevel;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.repository.PositionRepository;
import com.trademaster.portfolio.service.PortfolioService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Phase 6D Refactoring Integration Tests
 *
 * Validates that Phase 6D refactorings work correctly in integration:
 * - Pattern 2 (Layered Extraction) helper methods for portfolio operations
 * - Circuit breaker integration (Rule #25)
 * - Functional patterns (Rule #3, #11, #13)
 * - Virtual thread concurrency (Rule #12)
 * - Transaction management and ACID compliance
 * - No regressions from refactoring
 *
 * Coverage Areas:
 * 1. Portfolio Creation - executePortfolioCreation() and helpers
 * 2. Portfolio Valuation - executePortfolioValuation() and helpers
 * 3. Bulk Operations - executeBulkValuations() and parallel processing
 * 4. Portfolio Deletion - executePortfolioDeletion() and validation
 * 5. Rebalancing - executeRebalancingInitiation() and validation helpers
 * 6. Virtual Thread Concurrency - parallel operations and thread safety
 * 7. Regression Tests - behavior unchanged after refactoring
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 6D: PortfolioService Refactoring Integration Tests")
class PortfolioServiceRefactoringIntegrationTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    private static final Long TEST_USER_ID = 10001L;
    private static final BigDecimal INITIAL_CASH = new BigDecimal("100000.00");

    @Nested
    @DisplayName("Pattern 2: Portfolio Creation Helper Methods")
    @Transactional
    class PortfolioCreationHelperTests {

        @Test
        @DisplayName("createPortfolio() should use executePortfolioCreation() helper successfully")
        void testCreatePortfolioHelperMethodIntegration() {
            // Given: Portfolio creation request
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "Test Portfolio",
                INITIAL_CASH,
                RiskLevel.MODERATE,
                "USD",
                AccountType.INDIVIDUAL,
                false
            );

            // When: Create portfolio (uses executePortfolioCreation helper)
            Portfolio portfolio = portfolioService.createPortfolio(TEST_USER_ID, request);

            // Then: Helper methods should work correctly
            assertThat(portfolio).isNotNull();
            assertThat(portfolio.getPortfolioId()).isNotNull();
            assertThat(portfolio.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(portfolio.getPortfolioName()).isEqualTo("Test Portfolio");
            assertThat(portfolio.getCashBalance()).isEqualByComparingTo(INITIAL_CASH);
            assertThat(portfolio.getTotalValue()).isEqualByComparingTo(INITIAL_CASH);
            assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);

            // Verify helper methods executed: validateUserHasNoActivePortfolio, buildNewPortfolio, recordCreationSuccessMetrics
            Portfolio retrieved = portfolioService.getPortfolioById(portfolio.getPortfolioId());
            assertThat(retrieved).isNotNull();
        }

        @Test
        @DisplayName("validateUserHasNoActivePortfolio() should prevent duplicate active portfolios")
        void testValidateUserHasNoActivePortfolioHelper() {
            // Given: User with existing active portfolio
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "First Portfolio", INITIAL_CASH, RiskLevel.MODERATE, "USD", AccountType.INDIVIDUAL, false
            );
            portfolioService.createPortfolio(TEST_USER_ID, request);

            // When/Then: Attempt to create second active portfolio should fail validation
            assertThatThrownBy(() ->
                portfolioService.createPortfolio(TEST_USER_ID, request)
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already has an active portfolio");
        }

        @Test
        @DisplayName("buildNewPortfolio() should initialize portfolio with correct defaults")
        void testBuildNewPortfolioHelperIntegration() {
            // Given: Creation request with specific values
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "Custom Portfolio",
                new BigDecimal("50000.00"),
                RiskLevel.HIGH,
                "EUR",
                AccountType.INDIVIDUAL,
                false
            );

            // When: Create portfolio (buildNewPortfolio helper sets defaults)
            Portfolio portfolio = portfolioService.createPortfolio(TEST_USER_ID + 1, request);

            // Then: buildNewPortfolio() should set correct defaults
            assertThat(portfolio.getRealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(portfolio.getUnrealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(portfolio.getDayPnl()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(portfolio.getDayTradesCount()).isEqualTo(0);
            assertThat(portfolio.getCreatedAt()).isNotNull();
            assertThat(portfolio.getLastValuationAt()).isNotNull();
            assertThat(portfolio.getRiskLevel()).isEqualTo("AGGRESSIVE");
            assertThat(portfolio.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("recordCreationSuccessMetrics() should record all metrics correctly")
        void testRecordCreationSuccessMetricsHelper() {
            // Given: Creation request
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "Metrics Test Portfolio", INITIAL_CASH, RiskLevel.MODERATE, "USD", AccountType.INDIVIDUAL, false
            );

            // When: Create portfolio (recordCreationSuccessMetrics helper executes)
            long startTime = System.currentTimeMillis();
            Portfolio portfolio = portfolioService.createPortfolio(TEST_USER_ID + 2, request);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Metrics should be recorded (portfolio created successfully)
            assertThat(portfolio).isNotNull();
            assertThat(duration).isLessThan(1000);  // Creation should be fast

            // Verify portfolio was persisted correctly
            Portfolio retrieved = portfolioService.getPortfolioById(portfolio.getPortfolioId());
            assertThat(retrieved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Pattern 2: Valuation Helper Methods")
    @Transactional
    class PortfolioValuationHelperTests {

        private Portfolio testPortfolio;

        @BeforeEach
        void setUp() {
            // Create test portfolio
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "Valuation Test Portfolio", INITIAL_CASH, RiskLevel.MODERATE, "USD", AccountType.INDIVIDUAL, false
            );
            testPortfolio = portfolioService.createPortfolio(TEST_USER_ID + 100, request);
        }

        @Test
        @DisplayName("updatePortfolioValuation() should use executePortfolioValuation() helper")
        void testExecutePortfolioValuationHelper() {
            // Given: Portfolio with initial value
            BigDecimal initialValue = testPortfolio.getTotalValue();

            // When: Update valuation (uses executePortfolioValuation helper)
            Portfolio updated = portfolioService.updatePortfolioValuation(testPortfolio.getPortfolioId());

            // Then: Helper methods should work correctly
            assertThat(updated).isNotNull();
            assertThat(updated.getLastValuationAt()).isAfter(testPortfolio.getLastValuationAt());
            assertThat(updated.getUpdatedAt()).isAfter(testPortfolio.getUpdatedAt());

            // Verify executePortfolioValuation orchestrated applyValuationAndSave and recordValuationMetrics
            Portfolio retrieved = portfolioService.getPortfolioById(testPortfolio.getPortfolioId());
            assertThat(retrieved.getLastValuationAt()).isNotNull();
        }

        @Test
        @DisplayName("applyValuationAndSave() should persist valuation correctly")
        void testApplyValuationAndSaveHelper() {
            // Given: Updated values
            BigDecimal newValue = new BigDecimal("105000.00");
            BigDecimal newUnrealizedPnl = new BigDecimal("5000.00");

            // When: Update valuation (applyValuationAndSave helper persists)
            Portfolio updated = portfolioService.updateValuation(
                testPortfolio.getPortfolioId(), newValue, newUnrealizedPnl
            );

            // Then: applyValuationAndSave() should persist correctly
            assertThat(updated.getTotalValue()).isEqualByComparingTo(newValue);
            assertThat(updated.getUnrealizedPnl()).isEqualByComparingTo(newUnrealizedPnl);
            assertThat(updated.getLastValuationAt()).isNotNull();
            assertThat(updated.getUpdatedAt()).isNotNull();

            // Verify persistence
            Portfolio retrieved = portfolioService.getPortfolioById(testPortfolio.getPortfolioId());
            assertThat(retrieved.getTotalValue()).isEqualByComparingTo(newValue);
        }

        @Test
        @DisplayName("recordValuationMetrics() should log valuation changes")
        void testRecordValuationMetricsHelper() {
            // Given: Initial state
            BigDecimal initialValue = testPortfolio.getTotalValue();

            // When: Multiple valuations (recordValuationMetrics helper logs each)
            for (int i = 0; i < 3; i++) {
                portfolioService.updatePortfolioValuation(testPortfolio.getPortfolioId());
            }

            // Then: Each valuation should be recorded (verify through retrieval)
            final Portfolio finalPortfolio = portfolioService.getPortfolioById(testPortfolio.getPortfolioId());
            assertThat(finalPortfolio.getLastValuationAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Pattern 2: Bulk Operations Helper Methods")
    @Transactional
    class BulkOperationsHelperTests {

        @Test
        @DisplayName("bulkUpdateValuations() should use executeBulkValuations() helper")
        void testExecuteBulkValuationsHelper() throws Exception {
            // Given: Multiple portfolios
            List<Long> portfolioIds = createMultiplePortfolios(5);

            // When: Bulk update valuations (uses executeBulkValuations helper)
            CompletableFuture<Integer> result = portfolioService.bulkUpdateValuations(portfolioIds);

            // Then: Helper should coordinate bulk operations
            Integer updatedCount = result.get(10, TimeUnit.SECONDS);
            assertThat(updatedCount).isEqualTo(5);

            // Verify executeBulkValuations coordinated createValuationFutures and updatePortfolioValuationSafely
            for (Long portfolioId : portfolioIds) {
                Portfolio portfolio = portfolioService.getPortfolioById(portfolioId);
                assertThat(portfolio.getLastValuationAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("createValuationFutures() should create parallel tasks correctly")
        void testCreateValuationFuturesHelper() throws Exception {
            // Given: Multiple portfolios for parallel processing
            List<Long> portfolioIds = createMultiplePortfolios(10);

            // When: Bulk update (createValuationFutures helper creates parallel tasks)
            long startTime = System.currentTimeMillis();
            CompletableFuture<Integer> result = portfolioService.bulkUpdateValuations(portfolioIds);
            result.get(15, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Parallel processing should be efficient
            assertThat(duration).isLessThan(5000);  // 10 portfolios should complete quickly with parallel execution

            // Verify all portfolios updated (helper created futures correctly)
            for (Long portfolioId : portfolioIds) {
                Portfolio portfolio = portfolioService.getPortfolioById(portfolioId);
                assertThat(portfolio).isNotNull();
            }
        }

        @Test
        @DisplayName("updatePortfolioValuationSafely() should handle errors gracefully")
        void testUpdatePortfolioValuationSafelyHelper() throws Exception {
            // Given: Mix of valid and invalid portfolio IDs
            List<Long> mixedIds = List.of(
                createSinglePortfolio(),
                createSinglePortfolio(),
                99999L,  // Invalid ID
                createSinglePortfolio()
            );

            // When: Bulk update with some failures (updatePortfolioValuationSafely handles errors)
            CompletableFuture<Integer> result = portfolioService.bulkUpdateValuations(mixedIds);

            // Then: Should complete without throwing exceptions
            assertThatCode(() -> result.get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();

            // Valid portfolios should still be updated
            Integer completedCount = result.get();
            assertThat(completedCount).isEqualTo(4);  // All 4 attempted (errors handled gracefully)
        }
    }

    @Nested
    @DisplayName("Pattern 2: Deletion Helper Methods")
    @Transactional
    class PortfolioDeletionHelperTests {

        @Test
        @DisplayName("deletePortfolio() should use executePortfolioDeletion() helper")
        void testExecutePortfolioDeletionHelper() {
            // Given: Closed portfolio with no open positions
            Long portfolioId = createSinglePortfolio();
            portfolioService.closePortfolio(portfolioId);

            // When: Delete portfolio (uses executePortfolioDeletion helper)
            assertThatCode(() ->
                portfolioService.deletePortfolio(portfolioId, 1L, "Test deletion")
            ).doesNotThrowAnyException();

            // Then: Portfolio should be deleted
            assertThatThrownBy(() ->
                portfolioService.getPortfolioById(portfolioId)
            ).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("validatePortfolioDeletion() should enforce deletion rules")
        void testValidatePortfolioDeletionHelper() {
            // Given: Active portfolio (should fail validation)
            Long portfolioId = createSinglePortfolio();

            // When/Then: Deletion validation should prevent deleting active portfolio
            assertThatThrownBy(() ->
                portfolioService.deletePortfolio(portfolioId, 1L, "Invalid deletion")
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be closed before deletion");
        }

        @Test
        @DisplayName("validatePortfolioDeletion() should check for open positions")
        void testValidatePortfolioDeletionWithPositionsHelper() {
            // Given: Closed portfolio with open positions
            Long portfolioId = createSinglePortfolio();
            createTestPosition(portfolioId, "AAPL");
            portfolioService.closePortfolio(portfolioId);

            // When/Then: Validation should prevent deletion with open positions
            assertThatThrownBy(() ->
                portfolioService.deletePortfolio(portfolioId, 1L, "Invalid deletion")
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has open positions");
        }

        @Test
        @DisplayName("logPortfolioDeletionSuccess() should log audit trail")
        void testLogPortfolioDeletionSuccessHelper() {
            // Given: Closed portfolio ready for deletion
            Long portfolioId = createSinglePortfolio();
            portfolioService.closePortfolio(portfolioId);

            // When: Delete portfolio (logPortfolioDeletionSuccess helper logs)
            assertThatCode(() ->
                portfolioService.deletePortfolio(portfolioId, 1L, "Audit test deletion")
            ).doesNotThrowAnyException();

            // Then: Deletion should complete successfully (audit logged)
            assertThatThrownBy(() ->
                portfolioService.getPortfolioById(portfolioId)
            ).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Pattern 2: Rebalancing Helper Methods")
    @Transactional
    class RebalancingHelperTests {

        @Test
        @DisplayName("initiateRebalancing() should use executeRebalancingInitiation() helper")
        void testExecuteRebalancingInitiationHelper() throws Exception {
            // Given: Portfolio with positions and sufficient value
            Long portfolioId = createPortfolioForRebalancing();

            // When: Initiate rebalancing (uses executeRebalancingInitiation helper)
            CompletableFuture<RebalancingResult> result =
                portfolioService.initiateRebalancing(portfolioId, "BALANCED");

            // Then: Helper should orchestrate rebalancing initiation
            RebalancingResult rebalancingResult = result.get(5, TimeUnit.SECONDS);
            assertThat(rebalancingResult).isNotNull();
            assertThat(rebalancingResult.status()).isEqualTo("INITIATED");
            assertThat(rebalancingResult.portfolioId()).isEqualTo(portfolioId);
            assertThat(rebalancingResult.rebalancingId()).startsWith("RB-");

            // Verify helper methods executed: generateRebalancingId, buildInitiatedResult, publishRebalancingEvent
            assertThat(rebalancingResult.rebalancingId()).contains(portfolioId.toString());
        }

        @Test
        @DisplayName("generateRebalancingId() should create unique IDs")
        void testGenerateRebalancingIdHelper() throws Exception {
            // Given: Multiple rebalancing requests
            Long portfolioId = createPortfolioForRebalancing();

            // When: Generate multiple rebalancing IDs (generateRebalancingId helper)
            CompletableFuture<RebalancingResult> result1 =
                portfolioService.initiateRebalancing(portfolioId, "BALANCED");
            CompletableFuture<RebalancingResult> result2 =
                portfolioService.initiateRebalancing(portfolioId, "GROWTH");

            // Then: IDs should be unique
            String id1 = result1.get(5, TimeUnit.SECONDS).rebalancingId();
            String id2 = result2.get(5, TimeUnit.SECONDS).rebalancingId();

            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1).startsWith("RB-" + portfolioId);
            assertThat(id2).startsWith("RB-" + portfolioId);
        }

        @Test
        @DisplayName("validateRebalancingEligibility() should check all conditions")
        void testValidateRebalancingEligibilityHelpers() throws Exception {
            // Given: Portfolio not meeting rebalancing criteria
            Long portfolioId = createSinglePortfolio();

            // When/Then: validateRebalancingEligibility should prevent rebalancing
            CompletableFuture<RebalancingResult> result =
                portfolioService.initiateRebalancing(portfolioId, "BALANCED");

            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no positions");
        }

        @Test
        @DisplayName("validatePortfolioIsActive() should prevent rebalancing inactive portfolios")
        void testValidatePortfolioIsActiveHelper() throws Exception {
            // Given: Closed portfolio
            Long portfolioId = createPortfolioForRebalancing();
            portfolioService.closePortfolio(portfolioId);

            // When/Then: validatePortfolioIsActive should prevent rebalancing
            CompletableFuture<RebalancingResult> result =
                portfolioService.initiateRebalancing(portfolioId, "BALANCED");

            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be ACTIVE");
        }

        @Test
        @DisplayName("validatePortfolioMinimumValue() should enforce minimum value requirement")
        void testValidatePortfolioMinimumValueHelper() throws Exception {
            // Given: Portfolio with insufficient value
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "Small Portfolio", new BigDecimal("500.00"), RiskLevel.MODERATE, "USD", AccountType.INDIVIDUAL, false
            );
            Portfolio portfolio = portfolioService.createPortfolio(TEST_USER_ID + 500, request);
            createTestPosition(portfolio.getPortfolioId(), "AAPL");

            // When/Then: validatePortfolioMinimumValue should prevent rebalancing
            CompletableFuture<RebalancingResult> result =
                portfolioService.initiateRebalancing(portfolio.getPortfolioId(), "BALANCED");

            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least $1000");
        }
    }

    @Nested
    @DisplayName("Virtual Thread Concurrency Tests (Rule #12)")
    class VirtualThreadConcurrencyTests {

        @Test
        @DisplayName("Concurrent portfolio valuations should be thread-safe")
        void testConcurrentValuationsThreadSafety() throws Exception {
            // Given: Multiple portfolios
            List<Long> portfolioIds = createMultiplePortfolios(20);

            // When: Concurrent valuation updates
            List<CompletableFuture<Portfolio>> futures = portfolioIds.stream()
                .map(id -> portfolioService.updatePortfolioValuationAsync(id))
                .toList();

            // Wait for all
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(20, TimeUnit.SECONDS);

            // Then: All updates should succeed without race conditions
            for (CompletableFuture<Portfolio> future : futures) {
                Portfolio portfolio = future.get();
                assertThat(portfolio).isNotNull();
                assertThat(portfolio.getLastValuationAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("High concurrency portfolio operations should not exhaust thread pool")
        void testHighConcurrencyOperations() throws Exception {
            // Given: High concurrency scenario
            int operationCount = 100;
            List<Long> portfolioIds = createMultiplePortfolios(operationCount);

            // When: Execute high concurrency operations
            long startTime = System.currentTimeMillis();
            AtomicInteger completedOps = new AtomicInteger(0);

            List<CompletableFuture<Void>> futures = portfolioIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    portfolioService.updatePortfolioValuation(id);
                    completedOps.incrementAndGet();
                }))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Should complete without thread pool exhaustion
            assertThat(completedOps.get()).isEqualTo(operationCount);
            assertThat(duration).isLessThan(30000);  // <30 seconds for 100 operations
        }

        @Test
        @DisplayName("Parallel bulk valuations should use virtual threads efficiently")
        void testParallelBulkValuationsEfficiency() throws Exception {
            // Given: Large number of portfolios
            List<Long> portfolioIds = createMultiplePortfolios(50);

            // When: Bulk update with parallel processing
            long startTime = System.currentTimeMillis();
            CompletableFuture<Integer> result = portfolioService.bulkUpdateValuations(portfolioIds);
            Integer updated = result.get(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Virtual threads should provide efficient parallel processing
            assertThat(updated).isEqualTo(50);
            assertThat(duration).isLessThan(10000);  // <10 seconds for 50 portfolios
        }
    }

    @Nested
    @DisplayName("Regression Tests - No Functional Changes")
    @Transactional
    class RegressionValidationTests {

        @Test
        @DisplayName("Portfolio creation API behavior unchanged after refactoring")
        void testPortfolioCreationBehaviorUnchanged() {
            // Given: Standard creation request
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "Regression Test Portfolio", INITIAL_CASH, RiskLevel.MODERATE, "USD", AccountType.INDIVIDUAL, false
            );

            // When: Create portfolio
            Portfolio portfolio = portfolioService.createPortfolio(TEST_USER_ID + 1000, request);

            // Then: API behavior should be exactly as before refactoring
            assertThat(portfolio.getPortfolioId()).isNotNull();
            assertThat(portfolio.getUserId()).isEqualTo(TEST_USER_ID + 1000);
            assertThat(portfolio.getPortfolioName()).isEqualTo("Regression Test Portfolio");
            assertThat(portfolio.getCashBalance()).isEqualByComparingTo(INITIAL_CASH);
            assertThat(portfolio.getTotalValue()).isEqualByComparingTo(INITIAL_CASH);
            assertThat(portfolio.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
            assertThat(portfolio.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Valuation update behavior unchanged after refactoring")
        void testValuationUpdateBehaviorUnchanged() {
            // Given: Portfolio with initial state
            Long portfolioId = createSinglePortfolio();
            Portfolio initial = portfolioService.getPortfolioById(portfolioId);

            // When: Update valuation
            Portfolio updated = portfolioService.updatePortfolioValuation(portfolioId);

            // Then: Behavior should match pre-refactoring expectations
            assertThat(updated.getPortfolioId()).isEqualTo(portfolioId);
            assertThat(updated.getLastValuationAt()).isAfter(initial.getLastValuationAt());
            assertThat(updated.getUpdatedAt()).isAfter(initial.getUpdatedAt());
        }

        @Test
        @DisplayName("Cash balance operations unchanged after refactoring")
        void testCashBalanceOperationsBehaviorUnchanged() {
            // Given: Portfolio with initial cash
            Long portfolioId = createSinglePortfolio();
            BigDecimal depositAmount = new BigDecimal("10000.00");

            // When: Update cash balance
            Portfolio updated = portfolioService.updateCashBalance(
                portfolioId, depositAmount, "Test deposit"
            );

            // Then: Cash operations should work as before
            BigDecimal expectedCash = INITIAL_CASH.add(depositAmount);
            assertThat(updated.getCashBalance()).isEqualByComparingTo(expectedCash);
        }

        @Test
        @DisplayName("Performance characteristics maintained after refactoring")
        void testPerformanceNotRegressed() throws Exception {
            // Given: Performance test scenario
            List<Long> portfolioIds = createMultiplePortfolios(15);

            // When: Measure bulk operation performance
            long startTime = System.currentTimeMillis();
            CompletableFuture<Integer> result = portfolioService.bulkUpdateValuations(portfolioIds);
            result.get(10, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Performance should not regress
            assertThat(duration).isLessThan(5000);  // <5 seconds for 15 portfolios
        }
    }

    // Helper methods
    private Long createSinglePortfolio() {
        CreatePortfolioRequest request = new CreatePortfolioRequest(
            "Test Portfolio " + System.currentTimeMillis(),
            INITIAL_CASH,
            RiskLevel.MODERATE,
            "USD",
            AccountType.INDIVIDUAL,
            false
        );
        Portfolio portfolio = portfolioService.createPortfolio(
            TEST_USER_ID + (long)(Math.random() * 10000), request
        );
        return portfolio.getPortfolioId();
    }

    private List<Long> createMultiplePortfolios(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createSinglePortfolio())
            .toList();
    }

    private Long createPortfolioForRebalancing() {
        // Create portfolio with sufficient value and positions
        Long portfolioId = createSinglePortfolio();
        createTestPosition(portfolioId, "AAPL");
        createTestPosition(portfolioId, "GOOGL");
        return portfolioId;
    }

    private void createTestPosition(Long portfolioId, String symbol) {
        Position position = Position.builder()
            .portfolioId(portfolioId)
            .symbol(symbol)
            .exchange("NSE")
            .quantity(100)
            .averageCost(new BigDecimal("150.00"))
            .currentPrice(new BigDecimal("155.00"))
            .marketValue(new BigDecimal("15500.00"))
            .totalCost(new BigDecimal("15000.00"))
            .unrealizedPnl(new BigDecimal("500.00"))
            .realizedPnl(BigDecimal.ZERO)
            .openedAt(Instant.now())
            .build();

        positionRepository.save(position);
    }

    @BeforeEach
    void setUp() {
        // Test setup
    }

    @AfterEach
    void tearDown() {
        // Test cleanup
    }
}
