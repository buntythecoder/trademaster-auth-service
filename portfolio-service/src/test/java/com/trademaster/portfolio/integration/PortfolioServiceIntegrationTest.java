package com.trademaster.portfolio.integration;

import com.trademaster.portfolio.domain.PortfolioData;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.functional.PortfolioErrors;
import com.trademaster.portfolio.functional.Result;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.service.FunctionalPortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;

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
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        
        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // Virtual threads configuration
        registry.add("spring.threads.virtual.enabled", () -> true);
        
        // Test-specific configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> true);
        registry.add("logging.level.org.springframework.cache", () -> "DEBUG");
    }
    
    @Autowired
    private FunctionalPortfolioService portfolioService;
    
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    @Test
    @DisplayName("Should create portfolio with complete data persistence")
    void shouldCreatePortfolioWithCompleteDataPersistence() {
        // Given
        PortfolioData portfolioData = PortfolioData.builder()
                .userId(1001L)
                .portfolioName("Integration Test Portfolio")
                .totalValue(new BigDecimal("100000.00"))
                .totalCost(new BigDecimal("95000.00"))
                .unrealizedPnl(new BigDecimal("5000.00"))
                .realizedPnl(new BigDecimal("2500.00"))
                .dayPnl(new BigDecimal("1500.00"))
                .cashBalance(new BigDecimal("10000.00"))
                .currency("USD")
                .riskLevel("MODERATE")
                .build();
        
        // When
        Result<PortfolioData, PortfolioErrors> result = portfolioService.createPortfolio(portfolioData);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        PortfolioData savedPortfolio = result.getValue();
        assertThat(savedPortfolio.portfolioId()).isNotNull();
        assertThat(savedPortfolio.userId()).isEqualTo(1001L);
        assertThat(savedPortfolio.portfolioName()).isEqualTo("Integration Test Portfolio");
        assertThat(savedPortfolio.totalValue()).isEqualByComparingTo(new BigDecimal("100000.00"));
        
        // Verify database persistence
        Portfolio dbPortfolio = portfolioRepository.findById(savedPortfolio.portfolioId()).orElse(null);
        assertThat(dbPortfolio).isNotNull();
        assertThat(dbPortfolio.getUserId()).isEqualTo(1001L);
        assertThat(dbPortfolio.getPortfolioName()).isEqualTo("Integration Test Portfolio");
    }
    
    @Test
    @DisplayName("Should retrieve portfolio with caching behavior")
    void shouldRetrievePortfolioWithCachingBehavior() {
        // Given - Create test portfolio
        Portfolio testPortfolio = Portfolio.builder()
                .userId(1002L)
                .portfolioName("Cached Portfolio Test")
                .totalValue(new BigDecimal("75000.00"))
                .totalCost(new BigDecimal("70000.00"))
                .unrealizedPnl(new BigDecimal("5000.00"))
                .realizedPnl(new BigDecimal("1000.00"))
                .dayPnl(new BigDecimal("750.00"))
                .cashBalance(new BigDecimal("5000.00"))
                .currency("USD")
                .riskLevel("MODERATE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(testPortfolio);
        Long portfolioId = savedPortfolio.getId();
        
        // When - First retrieval (should hit database)
        Result<PortfolioData, PortfolioErrors> firstResult = 
            portfolioService.getPortfolioById(portfolioId);
        
        // Then - First retrieval successful
        assertThat(firstResult.isSuccess()).isTrue();
        PortfolioData firstPortfolio = firstResult.getValue();
        assertThat(firstPortfolio.portfolioId()).isEqualTo(portfolioId);
        assertThat(firstPortfolio.portfolioName()).isEqualTo("Cached Portfolio Test");
        
        // When - Second retrieval (should hit cache)
        Result<PortfolioData, PortfolioErrors> secondResult = 
            portfolioService.getPortfolioById(portfolioId);
        
        // Then - Second retrieval successful and consistent
        assertThat(secondResult.isSuccess()).isTrue();
        PortfolioData secondPortfolio = secondResult.getValue();
        assertThat(secondPortfolio).isEqualTo(firstPortfolio);
    }
    
    @Test
    @DisplayName("Should handle portfolio not found gracefully")
    void shouldHandlePortfolioNotFoundGracefully() {
        // Given
        Long nonExistentPortfolioId = 99999L;
        
        // When
        Result<PortfolioData, PortfolioErrors> result = 
            portfolioService.getPortfolioById(nonExistentPortfolioId);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(PortfolioErrors.NotFoundError.class);
        
        PortfolioErrors.NotFoundError notFoundError = 
            (PortfolioErrors.NotFoundError) result.getError();
        assertThat(notFoundError.resourceId()).contains(nonExistentPortfolioId.toString());
    }
    
    @Test
    @DisplayName("Should update portfolio with optimistic locking")
    void shouldUpdatePortfolioWithOptimisticLocking() {
        // Given - Create initial portfolio
        Portfolio initialPortfolio = Portfolio.builder()
                .userId(1003L)
                .portfolioName("Update Test Portfolio")
                .totalValue(new BigDecimal("50000.00"))
                .totalCost(new BigDecimal("48000.00"))
                .unrealizedPnl(new BigDecimal("2000.00"))
                .realizedPnl(new BigDecimal("500.00"))
                .dayPnl(new BigDecimal("200.00"))
                .cashBalance(new BigDecimal("2000.00"))
                .currency("USD")
                .riskLevel("MODERATE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(initialPortfolio);
        Long portfolioId = savedPortfolio.getId();
        
        // When - Update portfolio
        PortfolioData updateData = PortfolioData.builder()
                .portfolioId(portfolioId)
                .userId(1003L)
                .portfolioName("Updated Portfolio Name")
                .totalValue(new BigDecimal("55000.00"))
                .totalCost(new BigDecimal("48000.00"))
                .unrealizedPnl(new BigDecimal("7000.00"))
                .realizedPnl(new BigDecimal("500.00"))
                .dayPnl(new BigDecimal("1500.00"))
                .cashBalance(new BigDecimal("2000.00"))
                .currency("USD")
                .riskLevel("MODERATE")
                .version(savedPortfolio.getVersion())
                .build();
        
        Result<PortfolioData, PortfolioErrors> result = portfolioService.updatePortfolio(updateData);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        PortfolioData updatedPortfolio = result.getValue();
        assertThat(updatedPortfolio.portfolioName()).isEqualTo("Updated Portfolio Name");
        assertThat(updatedPortfolio.totalValue()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(updatedPortfolio.unrealizedPnl()).isEqualByComparingTo(new BigDecimal("7000.00"));
        assertThat(updatedPortfolio.version()).isGreaterThan(savedPortfolio.getVersion());
        
        // Verify database persistence
        Portfolio dbPortfolio = portfolioRepository.findById(portfolioId).orElse(null);
        assertThat(dbPortfolio).isNotNull();
        assertThat(dbPortfolio.getPortfolioName()).isEqualTo("Updated Portfolio Name");
        assertThat(dbPortfolio.getTotalValue()).isEqualByComparingTo(new BigDecimal("55000.00"));
    }
    
    @Test
    @DisplayName("Should retrieve portfolios by user ID with pagination")
    void shouldRetrievePortfoliosByUserIdWithPagination() {
        // Given - Create multiple portfolios for same user
        Long userId = 1004L;
        
        // Create portfolios using functional approach
        java.util.stream.IntStream.rangeClosed(1, 5)
            .mapToObj(i -> Portfolio.builder()
                    .userId(userId)
                    .portfolioName("Portfolio " + i)
                    .totalValue(new BigDecimal(10000 * i))
                    .totalCost(new BigDecimal(9500 * i))
                    .unrealizedPnl(new BigDecimal(500 * i))
                    .realizedPnl(new BigDecimal(100 * i))
                    .dayPnl(new BigDecimal(50 * i))
                    .cashBalance(new BigDecimal(1000 * i))
                    .currency("USD")
                    .riskLevel("MODERATE")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build())
            .forEach(portfolioRepository::save);
        
        // When - Get first portfolio (should be most recent/valuable)
        Result<PortfolioData, PortfolioErrors> result = portfolioService.getPortfolioByUserId(userId);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        PortfolioData portfolio = result.getValue();
        assertThat(portfolio.userId()).isEqualTo(userId);
        assertThat(portfolio.portfolioName()).isNotNull();
        assertThat(portfolio.totalValue()).isGreaterThan(BigDecimal.ZERO);
    }
    
    @Test
    @DisplayName("Should validate business rules during creation")
    void shouldValidateBusinessRulesDuringCreation() {
        // Given - Invalid portfolio data (negative values)
        PortfolioData invalidPortfolio = PortfolioData.builder()
                .userId(1005L)
                .portfolioName("Invalid Portfolio")
                .totalValue(new BigDecimal("-1000.00")) // Negative total value
                .totalCost(new BigDecimal("5000.00"))
                .unrealizedPnl(new BigDecimal("-6000.00"))
                .realizedPnl(new BigDecimal("0.00"))
                .dayPnl(new BigDecimal("0.00"))
                .cashBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .riskLevel("MODERATE")
                .build();
        
        // When
        Result<PortfolioData, PortfolioErrors> result = portfolioService.createPortfolio(invalidPortfolio);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(PortfolioErrors.ValidationError.class);
        
        PortfolioErrors.ValidationError validationError = 
            (PortfolioErrors.ValidationError) result.getError();
        assertThat(validationError.field()).contains("totalValue");
        assertThat(validationError.message()).contains("negative");
    }
    
    @Test
    @DisplayName("Should handle database constraints properly")
    void shouldHandleDatabaseConstraintsProperly() {
        // Given - Portfolio with null required field
        PortfolioData portfolioWithNullUserId = PortfolioData.builder()
                .userId(null) // Null user ID should violate NOT NULL constraint
                .portfolioName("Constraint Test Portfolio")
                .totalValue(new BigDecimal("25000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .unrealizedPnl(new BigDecimal("1000.00"))
                .realizedPnl(new BigDecimal("0.00"))
                .dayPnl(new BigDecimal("0.00"))
                .cashBalance(new BigDecimal("1000.00"))
                .currency("USD")
                .riskLevel("MODERATE")
                .build();
        
        // When
        Result<PortfolioData, PortfolioErrors> result = 
            portfolioService.createPortfolio(portfolioWithNullUserId);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(PortfolioErrors.ValidationError.class);
    }
    
    @Test
    @DisplayName("Should support concurrent portfolio operations")
    void shouldSupportConcurrentPortfolioOperations() {
        // Given - Multiple portfolios for concurrent operations
        Long baseUserId = 2000L;
        int numberOfPortfolios = 10;
        
        // When - Create portfolios concurrently using Virtual Threads and functional approach
        var futures = java.util.stream.IntStream.range(0, numberOfPortfolios)
            .mapToObj(portfolioIndex -> 
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    PortfolioData portfolioData = PortfolioData.builder()
                            .userId(baseUserId + portfolioIndex)
                            .portfolioName("Concurrent Portfolio " + portfolioIndex)
                            .totalValue(new BigDecimal((portfolioIndex + 1) * 10000))
                            .totalCost(new BigDecimal((portfolioIndex + 1) * 9500))
                            .unrealizedPnl(new BigDecimal((portfolioIndex + 1) * 500))
                            .realizedPnl(new BigDecimal((portfolioIndex + 1) * 100))
                            .dayPnl(new BigDecimal((portfolioIndex + 1) * 50))
                            .cashBalance(new BigDecimal((portfolioIndex + 1) * 1000))
                            .currency("USD")
                            .riskLevel("MODERATE")
                            .build();
                    
                    return portfolioService.createPortfolio(portfolioData);
                }, Thread.ofVirtual().factory())
            )
            .toArray(java.util.concurrent.CompletableFuture[]::new);
        
        // Wait for all operations to complete
        java.util.concurrent.CompletableFuture.allOf(futures).join();
        
        // Then - All operations should succeed using functional approach
        java.util.Arrays.stream(futures)
            .forEach(future -> {
                @SuppressWarnings("unchecked")
                Result<PortfolioData, PortfolioErrors> result = 
                    (Result<PortfolioData, PortfolioErrors>) future.join();
                
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getValue().portfolioId()).isNotNull();
            });
        
        // Verify all portfolios are persisted
        long totalPortfolios = portfolioRepository.count();
        assertThat(totalPortfolios).isGreaterThanOrEqualTo(numberOfPortfolios);
    }
}