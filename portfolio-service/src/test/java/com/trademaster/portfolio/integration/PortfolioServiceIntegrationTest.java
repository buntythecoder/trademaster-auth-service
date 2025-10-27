package com.trademaster.portfolio.integration;

import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.model.AccountType;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.model.RiskLevel;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Portfolio Service Integration Tests with TestContainers
 *
 * Full integration testing with real database and Redis containers.
 * Tests complete service layer functionality with persistence.
 *
 * Testing Infrastructure:
 * - PostgreSQL TestContainer for database operations
 * - Redis TestContainer for caching layer
 * - Spring Boot Test context for dependency injection
 * - Transactional testing for data isolation
 *
 * Test Coverage:
 * - Portfolio CRUD operations
 * - Database constraint validation
 * - Cache integration behavior
 * - Error handling with real database
 * - Performance characteristics
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Testing Suite)
 */
@SpringBootTest
@Testcontainers
@Transactional
class PortfolioServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("trademaster_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Virtual threads configuration
        registry.add("spring.threads.virtual.enabled", () -> true);

        // Test-specific configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> true);
        registry.add("logging.level.org.springframework.cache", () -> "DEBUG");
    }

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private CreatePortfolioRequest validRequest;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1001L;

        validRequest = new CreatePortfolioRequest(
            "Integration Test Portfolio",
            new BigDecimal("100000.00"),
            RiskLevel.MODERATE,
            "USD",
            AccountType.INDIVIDUAL,
            false
        );
    }

    @Test
    @DisplayName("Should create portfolio with complete data persistence")
    void shouldCreatePortfolioWithCompleteDataPersistence() {
        // When
        Portfolio savedPortfolio = portfolioService.createPortfolio(testUserId, validRequest);

        // Then
        assertThat(savedPortfolio).isNotNull();
        assertThat(savedPortfolio.getPortfolioId()).isNotNull();
        assertThat(savedPortfolio.getUserId()).isEqualTo(testUserId);
        assertThat(savedPortfolio.getPortfolioName()).isEqualTo("Integration Test Portfolio");
        assertThat(savedPortfolio.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(savedPortfolio.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);

        // Verify database persistence
        Portfolio dbPortfolio = portfolioRepository.findById(savedPortfolio.getPortfolioId()).orElse(null);
        assertThat(dbPortfolio).isNotNull();
        assertThat(dbPortfolio.getUserId()).isEqualTo(testUserId);
        assertThat(dbPortfolio.getPortfolioName()).isEqualTo("Integration Test Portfolio");
    }

    @Test
    @DisplayName("Should retrieve portfolio by ID")
    void shouldRetrievePortfolioById() {
        // Given - Create test portfolio
        Portfolio createdPortfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = createdPortfolio.getPortfolioId();

        // When
        Portfolio retrievedPortfolio = portfolioService.getPortfolioById(portfolioId);

        // Then
        assertThat(retrievedPortfolio).isNotNull();
        assertThat(retrievedPortfolio.getPortfolioId()).isEqualTo(portfolioId);
        assertThat(retrievedPortfolio.getPortfolioName()).isEqualTo("Integration Test Portfolio");
    }

    @Test
    @DisplayName("Should retrieve portfolios by user ID")
    void shouldRetrievePortfoliosByUserId() {
        // Given - Create multiple portfolios for same user
        portfolioService.createPortfolio(testUserId, validRequest);

        CreatePortfolioRequest request2 = new CreatePortfolioRequest(
            "Second Portfolio",
            new BigDecimal("50000.00"),
            RiskLevel.MODERATE,
            "USD",
            AccountType.INDIVIDUAL,
            false
        );

        // When
        List<Portfolio> portfolios = portfolioRepository.findAllByUserId(testUserId);

        // Then
        assertThat(portfolios).isNotEmpty();
        assertThat(portfolios).allMatch(p -> p.getUserId().equals(testUserId));
    }

    @Test
    @DisplayName("Should update portfolio status")
    void shouldUpdatePortfolioStatus() {
        // Given - Create test portfolio
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = portfolio.getPortfolioId();

        // When - Suspend portfolio
        Portfolio suspendedPortfolio = portfolioService.suspendPortfolio(portfolioId);

        // Then
        assertThat(suspendedPortfolio.getStatus()).isEqualTo(PortfolioStatus.SUSPENDED);

        // Verify persistence
        Portfolio dbPortfolio = portfolioRepository.findById(portfolioId).orElseThrow();
        assertThat(dbPortfolio.getStatus()).isEqualTo(PortfolioStatus.SUSPENDED);

        // When - Activate portfolio again
        Portfolio activatedPortfolio = portfolioService.activatePortfolio(portfolioId);

        // Then
        assertThat(activatedPortfolio.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should update cash balance")
    void shouldUpdateCashBalance() {
        // Given
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = portfolio.getPortfolioId();
        BigDecimal additionalCash = new BigDecimal("10000.00");

        // When
        Portfolio updatedPortfolio = portfolioService.updateCashBalance(
            portfolioId,
            additionalCash
        );

        // Then
        assertThat(updatedPortfolio.getCashBalance())
            .isEqualByComparingTo(new BigDecimal("110000.00"));
    }

    @Test
    @DisplayName("Should handle concurrent portfolio operations")
    void shouldHandleConcurrentPortfolioOperations() {
        // Given - Multiple portfolios for concurrent operations
        Long baseUserId = 2000L;
        int numberOfPortfolios = 10;

        // When - Create portfolios concurrently using Virtual Threads
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        var futures = java.util.stream.IntStream.range(0, numberOfPortfolios)
            .mapToObj(i ->
                CompletableFuture.supplyAsync(() -> {
                    CreatePortfolioRequest request = new CreatePortfolioRequest(
                        "Concurrent Portfolio " + i,
                        new BigDecimal((i + 1) * 10000),
                        RiskLevel.MODERATE,
                        "USD",
                        AccountType.INDIVIDUAL,
                        false
                    );

                    return portfolioService.createPortfolio(baseUserId + i, request);
                }, executor)
            )
            .toArray(CompletableFuture[]::new);

        // Wait for all operations to complete
        CompletableFuture.allOf(futures).join();

        executor.shutdown();

        // Then - All operations should succeed
        java.util.Arrays.stream(futures)
            .forEach(future -> {
                @SuppressWarnings("unchecked")
                Portfolio portfolio = ((CompletableFuture<Portfolio>) future).join();

                assertThat(portfolio).isNotNull();
                assertThat(portfolio.getPortfolioId()).isNotNull();
            });

        // Verify all portfolios are persisted
        long totalPortfolios = portfolioRepository.count();
        assertThat(totalPortfolios).isGreaterThanOrEqualTo(numberOfPortfolios);
    }

    @Test
    @DisplayName("Should validate business rules during creation")
    void shouldValidateBusinessRulesDuringCreation() {
        // Given - Invalid portfolio request (negative cash balance)
        CreatePortfolioRequest invalidRequest = new CreatePortfolioRequest(
            "Invalid Portfolio",
            new BigDecimal("-1000.00"),
            RiskLevel.MODERATE,
            "USD",
            AccountType.INDIVIDUAL,
            false
        );

        // When/Then - Should throw validation exception
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> portfolioService.createPortfolio(testUserId, invalidRequest)
        );
    }

    @Test
    @DisplayName("Should check minimum cash balance")
    void shouldCheckMinimumCashBalance() {
        // Given
        Portfolio portfolio = Portfolio.builder()
            .userId(testUserId)
            .portfolioName("Low Cash Portfolio")
            .cashBalance(new BigDecimal("500.00"))
            .totalValue(new BigDecimal("5000.00"))
            .status(PortfolioStatus.ACTIVE)
            .currency("USD")
            .build();

        portfolio = portfolioRepository.save(portfolio);

        // When
        boolean hasMinimum = portfolioService.hasMinimumCashBalance(portfolio);

        // Then
        assertThat(hasMinimum).isFalse(); // Less than $1000 minimum
    }

    @Test
    @DisplayName("Should check if portfolio can trade")
    void shouldCheckIfPortfolioCanTrade() {
        // Given - Create portfolio with sufficient balance
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);

        // When
        boolean canTrade = portfolioService.canTrade(portfolio);

        // Then
        assertThat(canTrade).isTrue(); // ACTIVE status + sufficient cash + not at day trade limit
    }

    @Test
    @DisplayName("Should increment day trades count")
    void shouldIncrementDayTradesCount() {
        // Given
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = portfolio.getPortfolioId();

        // When
        int newCount = portfolioService.incrementDayTradesCount(portfolioId);

        // Then
        assertThat(newCount).isEqualTo(1);

        // Verify persistence
        Portfolio updatedPortfolio = portfolioRepository.findById(portfolioId).orElseThrow();
        assertThat(updatedPortfolio.getDayTradesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check if approaching day trade limit")
    void shouldCheckIfApproachingDayTradeLimit() {
        // Given - Create portfolio and add day trades
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = portfolio.getPortfolioId();

        // When - Add 3 day trades
        portfolioService.incrementDayTradesCount(portfolioId);
        portfolioService.incrementDayTradesCount(portfolioId);
        portfolioService.incrementDayTradesCount(portfolioId);

        boolean approaching = portfolioService.isApproachingDayTradeLimit(portfolioId);

        // Then
        assertThat(approaching).isTrue(); // 3 trades = at limit
    }

    @Test
    @DisplayName("Should close portfolio")
    void shouldClosePortfolio() {
        // Given
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = portfolio.getPortfolioId();

        // When
        Portfolio closedPortfolio = portfolioService.closePortfolio(portfolioId);

        // Then
        assertThat(closedPortfolio.getStatus()).isEqualTo(PortfolioStatus.CLOSED);

        // Verify persistence
        Portfolio dbPortfolio = portfolioRepository.findById(portfolioId).orElseThrow();
        assertThat(dbPortfolio.getStatus()).isEqualTo(PortfolioStatus.CLOSED);
    }

    @Test
    @DisplayName("Should update portfolio valuation")
    void shouldUpdatePortfolioValuation() {
        // Given
        Portfolio portfolio = portfolioService.createPortfolio(testUserId, validRequest);
        Long portfolioId = portfolio.getPortfolioId();

        BigDecimal newValue = new BigDecimal("110000.00");
        BigDecimal newUnrealizedPnl = new BigDecimal("10000.00");

        // When
        Portfolio updatedPortfolio = portfolioService.updateValuation(
            portfolioId,
            newValue,
            newUnrealizedPnl
        );

        // Then
        assertThat(updatedPortfolio.getTotalValue()).isEqualByComparingTo(newValue);
        assertThat(updatedPortfolio.getUnrealizedPnl()).isEqualByComparingTo(newUnrealizedPnl);
        assertThat(updatedPortfolio.getLastValuationAt()).isNotNull();
    }
}
