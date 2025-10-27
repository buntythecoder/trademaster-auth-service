package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.model.PortfolioStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Portfolio Repository Integration Tests
 *
 * Comprehensive test suite using TestContainers for real PostgreSQL testing.
 * Tests all repository methods following TradeMaster standards.
 *
 * Rule #20: Testing Standards - >80% coverage with TestContainers
 * Rule #24: Zero Compilation Errors - All tests compile and pass
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PortfolioRepository Integration Tests")
class PortfolioRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();
        testPortfolio = createTestPortfolio(1L, "TEST_PORTFOLIO", new BigDecimal("100000"));
    }

    /**
     * Rule #3: Functional test data builder using Builder pattern
     * Rule #9: Immutable entity construction with Lombok Builder
     */
    private Portfolio createTestPortfolio(Long id, String portfolioName, BigDecimal totalValue) {
        Portfolio portfolio = Portfolio.builder()
            .userId(1001L)
            .portfolioName(portfolioName)
            .totalValue(totalValue)
            .cashBalance(new BigDecimal("50000"))
            .totalCost(new BigDecimal("90000"))
            .realizedPnl(new BigDecimal("5000"))
            .unrealizedPnl(new BigDecimal("5000"))
            .status(PortfolioStatus.ACTIVE)
            .dayTradesCount(0)
            .currency("USD")
            .build();
        return portfolioRepository.save(portfolio);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should find portfolio by user ID")
        void shouldFindPortfolioByUserId() {
            // Given: Portfolio exists
            // When: Find by user ID
            Optional<Portfolio> result = portfolioRepository.findByUserId(testPortfolio.getUserId());

            // Then: Portfolio is found
            assertThat(result).isPresent();
            assertThat(result.get().getPortfolioName()).isEqualTo("TEST_PORTFOLIO");
        }

        @Test
        @DisplayName("Should find portfolio by user ID and status")
        void shouldFindPortfolioByUserIdAndStatus() {
            // When: Find by user ID and status
            Optional<Portfolio> result = portfolioRepository
                .findByUserIdAndStatus(testPortfolio.getUserId(), PortfolioStatus.ACTIVE);

            // Then: Portfolio is found
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should check portfolio existence by user ID and status")
        void shouldCheckPortfolioExistenceByUserIdAndStatus() {
            // When: Check existence
            boolean exists = portfolioRepository
                .existsByUserIdAndStatus(testPortfolio.getUserId(), PortfolioStatus.ACTIVE);

            // Then: Portfolio exists
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should find all portfolios for user")
        void shouldFindAllPortfoliosForUser() {
            // Given: Multiple portfolios for same user
            createTestPortfolio(2L, "TEST_PORTFOLIO_2", new BigDecimal("50000"));

            // When: Find all by user ID
            List<Portfolio> result = portfolioRepository.findAllByUserId(testPortfolio.getUserId());

            // Then: All portfolios are found
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Day Trading Operations")
    class DayTradingOperations {

        @Test
        @DisplayName("Should increment day trades count")
        void shouldIncrementDayTradesCount() {
            // When: Increment day trades count
            int updated = portfolioRepository.incrementDayTradesCount(testPortfolio.getPortfolioId());

            // Then: Count is incremented
            assertThat(updated).isEqualTo(1);
            Portfolio reloaded = portfolioRepository.findById(testPortfolio.getPortfolioId()).orElseThrow();
            assertThat(reloaded.getDayTradesCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reset day trades count for active portfolios")
        void shouldResetDayTradesCountForActivePortfolios() {
            // Given: Portfolio with day trades
            portfolioRepository.incrementDayTradesCount(testPortfolio.getPortfolioId());

            // When: Reset day trades count
            int updated = portfolioRepository.resetDayTradesCount();

            // Then: Count is reset
            assertThat(updated).isGreaterThan(0);
            Portfolio reloaded = portfolioRepository.findById(testPortfolio.getPortfolioId()).orElseThrow();
            assertThat(reloaded.getDayTradesCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should find portfolios exceeding day trade count threshold")
        void shouldFindPortfoliosExceedingDayTradeCount() {
            // Given: Portfolio with 3 day trades
            portfolioRepository.incrementDayTradesCount(testPortfolio.getPortfolioId());
            portfolioRepository.incrementDayTradesCount(testPortfolio.getPortfolioId());
            portfolioRepository.incrementDayTradesCount(testPortfolio.getPortfolioId());

            // When: Find portfolios exceeding threshold of 2
            List<Portfolio> result = portfolioRepository.findByDayTradesCountGreaterThan(2);

            // Then: Portfolio is found
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getDayTradesCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("AUM Calculations")
    class AUMCalculations {

        @Test
        @DisplayName("Should calculate total AUM across all active portfolios")
        void shouldCalculateTotalAUM() {
            // Given: Multiple active portfolios
            createTestPortfolio(2L, "PORTFOLIO_2", new BigDecimal("200000"));

            // When: Calculate total AUM
            BigDecimal totalAUM = portfolioRepository.calculateTotalAUM();

            // Then: Sum of all portfolio values plus cash
            assertThat(totalAUM).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate AUM for specific user")
        void shouldCalculateAUMByUserId() {
            // When: Calculate AUM for user
            BigDecimal userAUM = portfolioRepository.calculateAUMByUserId(testPortfolio.getUserId());

            // Then: User's AUM is calculated
            assertThat(userAUM).isEqualByComparingTo(
                testPortfolio.getTotalValue().add(testPortfolio.getCashBalance())
            );
        }

        @Test
        @DisplayName("Should find top portfolios by AUM")
        void shouldFindTopPortfoliosByAUM() {
            // Given: Multiple portfolios with different values
            createTestPortfolio(2L, "HIGH_VALUE", new BigDecimal("500000"));
            createTestPortfolio(3L, "LOW_VALUE", new BigDecimal("10000"));

            // When: Find top 2 portfolios
            List<Portfolio> topPortfolios = portfolioRepository
                .findTopPortfoliosByAUM(PageRequest.of(0, 2));

            // Then: Portfolios are sorted by AUM descending
            assertThat(topPortfolios).hasSize(2);
            assertThat(topPortfolios.getFirst().getTotalValue())
                .isGreaterThan(topPortfolios.get(1).getTotalValue());
        }
    }

    @Nested
    @DisplayName("Performance Queries")
    class PerformanceQueries {

        @Test
        @DisplayName("Should find portfolios with return greater than threshold")
        void shouldFindPortfoliosWithReturnGreaterThanThreshold() {
            // Given: Portfolio with positive return (10000 / 90000 = 11.11%)
            // When: Find portfolios with return > 10%
            List<Portfolio> result = portfolioRepository
                .findByReturnGreaterThan(new BigDecimal("10"));

            // Then: Portfolio is found
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getPortfolioId()).isEqualTo(testPortfolio.getPortfolioId());
        }

        @Test
        @DisplayName("Should calculate average return across portfolios")
        void shouldCalculateAverageReturn() {
            // Given: Multiple portfolios
            createTestPortfolio(2L, "PORTFOLIO_2", new BigDecimal("200000"));

            // When: Calculate average return
            BigDecimal avgReturn = portfolioRepository.calculateAverageReturn();

            // Then: Average is calculated
            assertThat(avgReturn).isNotNull();
        }

        @Test
        @DisplayName("Should find portfolios with losses")
        void shouldFindPortfoliosWithLoss() {
            // Given: Portfolio with loss
            Portfolio losingPortfolio = createTestPortfolio(2L, "LOSING", new BigDecimal("80000"));
            losingPortfolio.setRealizedPnl(new BigDecimal("-10000"));
            losingPortfolio.setUnrealizedPnl(new BigDecimal("-10000"));
            portfolioRepository.save(losingPortfolio);

            // When: Find portfolios with losses
            List<Portfolio> result = portfolioRepository.findPortfoliosWithLoss();

            // Then: Losing portfolio is found
            assertThat(result).hasSizeGreaterThan(0);
        }

        @Test
        @DisplayName("Should find portfolios by return range")
        void shouldFindPortfoliosByReturnRange() {
            // When: Find portfolios with return between 5% and 15%
            List<Portfolio> result = portfolioRepository
                .findByReturnRange(new BigDecimal("5"), new BigDecimal("15"));

            // Then: Portfolios in range are found
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Valuation Updates")
    class ValuationUpdates {

        @Test
        @DisplayName("Should update portfolio valuation")
        void shouldUpdatePortfolioValuation() {
            // Given: New valuation data
            BigDecimal newValue = new BigDecimal("110000");
            BigDecimal newPnl = new BigDecimal("15000");
            Instant timestamp = Instant.now();

            // When: Update valuation
            int updated = portfolioRepository.updatePortfolioValuation(
                testPortfolio.getPortfolioId(),
                newValue,
                newPnl,
                timestamp
            );

            // Then: Valuation is updated
            assertThat(updated).isEqualTo(1);
            Portfolio reloaded = portfolioRepository.findById(testPortfolio.getPortfolioId()).orElseThrow();
            assertThat(reloaded.getTotalValue()).isEqualByComparingTo(newValue);
            assertThat(reloaded.getUnrealizedPnl()).isEqualByComparingTo(newPnl);
        }

        @Test
        @DisplayName("Should update cash balance atomically")
        void shouldUpdateCashBalanceAtomically() {
            // Given: Cash deposit
            BigDecimal deposit = new BigDecimal("10000");

            // When: Update cash balance
            int updated = portfolioRepository.updateCashBalance(
                testPortfolio.getPortfolioId(),
                deposit
            );

            // Then: Cash balance is updated
            assertThat(updated).isEqualTo(1);
            Portfolio reloaded = portfolioRepository.findById(testPortfolio.getPortfolioId()).orElseThrow();
            assertThat(reloaded.getCashBalance())
                .isEqualByComparingTo(testPortfolio.getCashBalance().add(deposit));
        }

        @Test
        @DisplayName("Should add realized P&L atomically")
        void shouldAddRealizedPnLAtomically() {
            // Given: Realized P&L from trade
            BigDecimal tradePnl = new BigDecimal("1000");
            Instant timestamp = Instant.now();

            // When: Add realized P&L
            int updated = portfolioRepository.addRealizedPnl(
                testPortfolio.getPortfolioId(),
                tradePnl,
                timestamp
            );

            // Then: Realized P&L is updated
            assertThat(updated).isEqualTo(1);
            Portfolio reloaded = portfolioRepository.findById(testPortfolio.getPortfolioId()).orElseThrow();
            assertThat(reloaded.getRealizedPnl())
                .isEqualByComparingTo(testPortfolio.getRealizedPnl().add(tradePnl));
        }
    }

    @Nested
    @DisplayName("Pagination Support")
    class PaginationSupport {

        @Test
        @DisplayName("Should find portfolios with pagination")
        void shouldFindPortfoliosWithPagination() {
            // Given: Multiple portfolios
            createTestPortfolio(2L, "PORTFOLIO_2", new BigDecimal("200000"));
            createTestPortfolio(3L, "PORTFOLIO_3", new BigDecimal("300000"));

            // When: Find portfolios with pagination
            Page<Portfolio> result = portfolioRepository
                .findAllByUserIdPageable(testPortfolio.getUserId(), PageRequest.of(0, 2));

            // Then: Paginated results are returned
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("Should find portfolios by status with pagination")
        void shouldFindPortfoliosByStatusWithPagination() {
            // Given: Multiple active portfolios
            createTestPortfolio(2L, "PORTFOLIO_2", new BigDecimal("200000"));

            // When: Find active portfolios with pagination
            Page<Portfolio> result = portfolioRepository
                .findByUserIdAndStatusPageable(
                    testPortfolio.getUserId(),
                    PortfolioStatus.ACTIVE,
                    PageRequest.of(0, 10)
                );

            // Then: Active portfolios are returned
            assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result.getContent())
                .allMatch(p -> p.getStatus() == PortfolioStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Risk Monitoring")
    class RiskMonitoring {

        @Test
        @DisplayName("Should find portfolios approaching risk limits")
        void shouldFindPortfoliosApproachingRiskLimits() {
            // Given: Portfolio with high day trades and loss
            Portfolio riskyPortfolio = createTestPortfolio(2L, "RISKY", new BigDecimal("80000"));
            riskyPortfolio.setDayTradesCount(3);
            riskyPortfolio.setRealizedPnl(new BigDecimal("-15000"));
            portfolioRepository.save(riskyPortfolio);

            // When: Find portfolios approaching risk limits
            List<Portfolio> result = portfolioRepository
                .findPortfoliosApproachingRiskLimits(new BigDecimal("-10"), 2);

            // Then: Risky portfolio is found
            assertThat(result).hasSizeGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperations {

        @Test
        @DisplayName("Should reset all day trades count")
        void shouldResetAllDayTradesCount() {
            // Given: Portfolios with day trades
            portfolioRepository.incrementDayTradesCount(testPortfolio.getPortfolioId());

            // When: Reset all day trades
            int updated = portfolioRepository.resetAllDayTradesCount();

            // Then: All counts are reset
            assertThat(updated).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should bulk update portfolio statuses")
        void shouldBulkUpdatePortfolioStatuses() {
            // When: Bulk update status
            int updated = portfolioRepository.bulkUpdateStatus(
                PortfolioStatus.ACTIVE,
                PortfolioStatus.CLOSED
            );

            // Then: Statuses are updated
            assertThat(updated).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should count active portfolios")
        void shouldCountActivePortfolios() {
            // Given: Multiple active portfolios
            createTestPortfolio(2L, "PORTFOLIO_2", new BigDecimal("200000"));

            // When: Count active portfolios
            Long count = portfolioRepository.countActivePortfolios();

            // Then: Count is correct
            assertThat(count).isGreaterThanOrEqualTo(2);
        }
    }
}
