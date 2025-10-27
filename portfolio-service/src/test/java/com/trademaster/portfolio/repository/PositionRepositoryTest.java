package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.model.PositionType;
import com.trademaster.portfolio.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
 * Position Repository Integration Tests
 *
 * Tests repository methods with TestContainers PostgreSQL.
 *
 * Testing Coverage:
 * - Basic CRUD operations
 * - Custom query methods
 * - Position calculations
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Testing Suite)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PositionRepository Integration Tests")
class PositionRepositoryTest {

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
    private PositionRepository positionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Portfolio testPortfolio;
    private Position testPosition;

    @BeforeEach
    void setUp() {
        // Create test portfolio
        testPortfolio = Portfolio.builder()
                .userId(1001L)
                .portfolioName("Test Portfolio")
                .totalValue(new BigDecimal("100000"))
                .totalCost(new BigDecimal("95000"))
                .unrealizedPnl(new BigDecimal("5000"))
                .realizedPnl(new BigDecimal("1000"))
                .dayPnl(new BigDecimal("500"))
                .cashBalance(new BigDecimal("10000"))
                .currency("USD")
                .riskLevel(RiskLevel.MODERATE)
                .status(PortfolioStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testPortfolio = portfolioRepository.save(testPortfolio);

        // Create test position
        testPosition = Position.builder()
                .portfolio(testPortfolio)
                .symbol("AAPL")
                .quantity(100)
                .averageCost(new BigDecimal("150.00"))
                .currentPrice(new BigDecimal("155.00"))
                .marketValue(new BigDecimal("15500.00"))
                .totalCost(new BigDecimal("15000.00"))
                .unrealizedPnl(new BigDecimal("500.00"))
                .realizedPnl(new BigDecimal("100.00"))
                .dayPnl(new BigDecimal("50.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testPosition = positionRepository.save(testPosition);
        entityManager.flush();
        entityManager.clear();

        // Small delay to ensure timestamps in test methods are later than @BeforeEach timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Should find position by portfolio ID and symbol")
    void shouldFindPositionByPortfolioIdAndSymbol() {
        // When
        Optional<Position> result = positionRepository
                .findByPortfolioIdAndSymbol(testPortfolio.getPortfolioId(), "AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        assertThat(result.get().getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should find all positions by portfolio ID")
    void shouldFindAllPositionsByPortfolioId() {
        // Given - Add another position
        Position position2 = Position.builder()
                .portfolio(testPortfolio)
                .symbol("GOOGL")
                .quantity(50)
                .averageCost(new BigDecimal("2800.00"))
                .currentPrice(new BigDecimal("2850.00"))
                .marketValue(new BigDecimal("142500.00"))
                .totalCost(new BigDecimal("140000.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        positionRepository.save(position2);
        entityManager.flush();

        // When
        List<Position> positions = positionRepository
                .findByPortfolioId(testPortfolio.getPortfolioId());

        // Then
        assertThat(positions).hasSize(2);
        assertThat(positions).extracting(Position::getSymbol).containsExactlyInAnyOrder("AAPL", "GOOGL");
    }

    @Test
    @DisplayName("Should find open positions by portfolio ID")
    void shouldFindOpenPositionsByPortfolioId() {
        // Given - Position is already open (has quantity > 0)

        // When
        List<Position> positions = positionRepository
                .findOpenPositionsByPortfolioId(testPortfolio.getPortfolioId());

        // Then
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(positions.get(0).getQuantity()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should find position by symbol")
    void shouldFindPositionBySymbol() {
        // When
        List<Position> positions = positionRepository.findBySymbol("AAPL");

        // Then
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("Should find positions by multiple symbols")
    void shouldFindPositionsBySymbols() {
        // Given - Add another position
        Position position2 = Position.builder()
                .portfolio(testPortfolio)
                .symbol("MSFT")
                .quantity(75)
                .averageCost(new BigDecimal("300.00"))
                .currentPrice(new BigDecimal("310.00"))
                .marketValue(new BigDecimal("23250.00"))
                .totalCost(new BigDecimal("22500.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        positionRepository.save(position2);
        entityManager.flush();

        // When
        List<Position> positions = positionRepository
                .findBySymbolIn(List.of("AAPL", "MSFT"));

        // Then
        assertThat(positions).hasSize(2);
        assertThat(positions).extracting(Position::getSymbol).containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    @DisplayName("Should find positions by position type")
    void shouldFindPositionsByPositionType() {
        // When
        List<Position> positions = positionRepository
                .findByPortfolioIdAndPositionType(testPortfolio.getPortfolioId(), PositionType.LONG);

        // Then
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getPositionType()).isEqualTo(PositionType.LONG);
    }

    @Test
    @DisplayName("Should calculate total invested amount for portfolio")
    void shouldCalculateTotalInvestedAmount() {
        // Given - Add another position
        Position position2 = Position.builder()
                .portfolio(testPortfolio)
                .symbol("TSLA")
                .quantity(30)
                .averageCost(new BigDecimal("700.00"))
                .currentPrice(new BigDecimal("720.00"))
                .totalCost(new BigDecimal("21000.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        positionRepository.save(position2);
        entityManager.flush();

        // When
        BigDecimal totalInvested = positionRepository
                .calculateTotalInvestedAmount(testPortfolio.getPortfolioId());

        // Then
        // Total cost = 15000 (AAPL) + 21000 (TSLA) = 36000
        assertThat(totalInvested).isEqualByComparingTo(new BigDecimal("36000.00"));
    }

    @Test
    @DisplayName("Should find top gainers in portfolio")
    void shouldFindTopGainersInPortfolio() {
        // Given - Add more positions with different P&L
        Position loser = Position.builder()
                .portfolio(testPortfolio)
                .symbol("LOSER")
                .quantity(50)
                .averageCost(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("5000.00"))
                .currentPrice(new BigDecimal("90.00"))
                .marketValue(new BigDecimal("4500.00"))
                .unrealizedPnl(new BigDecimal("-500.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        Position bigGainer = Position.builder()
                .portfolio(testPortfolio)
                .symbol("WINNER")
                .quantity(100)
                .averageCost(new BigDecimal("50.00"))
                .totalCost(new BigDecimal("5000.00"))
                .currentPrice(new BigDecimal("75.00"))
                .marketValue(new BigDecimal("7500.00"))
                .unrealizedPnl(new BigDecimal("2500.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        positionRepository.save(loser);
        positionRepository.save(bigGainer);
        entityManager.flush();

        // When
        List<Position> topGainers = positionRepository
                .findTopGainers(testPortfolio.getPortfolioId());

        // Then
        assertThat(topGainers).isNotEmpty();
        assertThat(topGainers.get(0).getSymbol()).isEqualTo("WINNER");
        assertThat(topGainers.get(0).getUnrealizedPnl()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should find top losers in portfolio")
    void shouldFindTopLosersInPortfolio() {
        // Given - Add more positions
        Position loser = Position.builder()
                .portfolio(testPortfolio)
                .symbol("LOSER")
                .quantity(50)
                .averageCost(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("5000.00"))
                .currentPrice(new BigDecimal("80.00"))
                .marketValue(new BigDecimal("4000.00"))
                .unrealizedPnl(new BigDecimal("-1000.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        positionRepository.save(loser);
        entityManager.flush();

        // When
        List<Position> topLosers = positionRepository
                .findTopLosers(testPortfolio.getPortfolioId());

        // Then
        assertThat(topLosers).isNotEmpty();
        assertThat(topLosers.get(0).getSymbol()).isEqualTo("LOSER");
        assertThat(topLosers.get(0).getUnrealizedPnl()).isLessThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should count positions by symbol")
    void shouldCountPositionsBySymbol() {
        // Given - Add another AAPL position in a different portfolio
        Portfolio portfolio2 = Portfolio.builder()
                .userId(1002L)
                .portfolioName("Portfolio 2")
                .totalValue(new BigDecimal("50000"))
                .cashBalance(new BigDecimal("10000"))
                .status(PortfolioStatus.ACTIVE)
                .currency("USD")
                .build();

        portfolio2 = portfolioRepository.save(portfolio2);

        Position position2 = Position.builder()
                .portfolio(portfolio2)
                .symbol("AAPL")
                .quantity(50)
                .averageCost(new BigDecimal("155.00"))
                .totalCost(new BigDecimal("7750.00"))
                .currentPrice(new BigDecimal("160.00"))
                .marketValue(new BigDecimal("8000.00"))
                .positionType(PositionType.LONG)
                .exchange("NASDAQ")
                .sector("Technology")
                .openedAt(Instant.now())
                .build();

        positionRepository.save(position2);
        entityManager.flush();

        // When
        Long count = positionRepository.countPositionsBySymbol("AAPL");

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update position price")
    void shouldUpdatePositionPrice() {
        // Given
        BigDecimal newPrice = new BigDecimal("160.00");
        Instant updateTimestamp = Instant.now();

        // When
        int updated = positionRepository.updatePositionPrice(
                testPosition.getPositionId(),
                newPrice,
                updateTimestamp
        );

        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updated).isEqualTo(1);

        Position reloaded = positionRepository.findById(testPosition.getPositionId()).orElseThrow();
        assertThat(reloaded.getCurrentPrice()).isEqualByComparingTo(newPrice);
        // Note: Not testing updatedAt as it's auto-managed by @UpdateTimestamp annotation
    }
}
