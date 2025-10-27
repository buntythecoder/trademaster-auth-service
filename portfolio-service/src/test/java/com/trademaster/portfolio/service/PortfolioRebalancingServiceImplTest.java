package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.RebalancingPlan;
import com.trademaster.portfolio.dto.RebalancingPlan.AllocationComparison;
import com.trademaster.portfolio.dto.RebalancingPlan.TradeRecommendation;
import com.trademaster.portfolio.dto.TargetAllocation;
import com.trademaster.portfolio.service.PortfolioRebalancingService.RebalancingResult;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.repository.PositionRepository;
import com.trademaster.portfolio.service.impl.PortfolioRebalancingServiceImpl;
import com.trademaster.portfolio.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Portfolio Rebalancing Service Unit Tests
 *
 * Comprehensive test suite for portfolio rebalancing service.
 *
 * Rule #20: Testing Standards - >80% coverage with functional builders
 * Rule #3: Functional programming patterns in tests
 * Rule #12: Virtual Thread testing patterns
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioRebalancingService Unit Tests")
class PortfolioRebalancingServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PositionService positionService;

    @InjectMocks
    private PortfolioRebalancingServiceImpl rebalancingService;

    private Portfolio testPortfolio;
    private List<Position> testPositions;
    private List<TargetAllocation> targetAllocations;

    @BeforeEach
    void setUp() {
        testPortfolio = Portfolio.builder()
            .portfolioId(1L)
            .userId(123L)
            .portfolioName("Test Portfolio")
            .totalValue(BigDecimal.valueOf(100000))
            .build();

        testPositions = List.of(
            Position.builder()
                .symbol("AAPL")
                .quantity(100)
                .averageCost(BigDecimal.valueOf(150))
                .marketValue(BigDecimal.valueOf(17500))
                .openedAt(Instant.now().minusSeconds(86400 * 30))
                .build(),
            Position.builder()
                .symbol("GOOGL")
                .quantity(50)
                .averageCost(BigDecimal.valueOf(140))
                .marketValue(BigDecimal.valueOf(7500))
                .openedAt(Instant.now().minusSeconds(86400 * 60))
                .build()
        );

        targetAllocations = List.of(
            new TargetAllocation("AAPL", BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(25), "EQUITY", "TECH"),
            new TargetAllocation("GOOGL", BigDecimal.valueOf(10), BigDecimal.valueOf(5), BigDecimal.valueOf(15), "EQUITY", "TECH"),
            new TargetAllocation("MSFT", BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(20), "EQUITY", "TECH")
        );
    }

    @Nested
    @DisplayName("Generate Rebalancing Plan Tests")
    class GenerateRebalancingPlanTests {

        @Test
        @DisplayName("Should generate rebalancing plan with valid allocations")
        void shouldGenerateRebalancingPlanSuccessfully() {
            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(testPositions);

            CompletableFuture<RebalancingPlan> future = rebalancingService.generateRebalancingPlan(
                1L, targetAllocations, "BALANCED"
            );

            RebalancingPlan plan = future.join();

            assertThat(plan).isNotNull();
            assertThat(plan.portfolioId()).isEqualTo(1L);
            assertThat(plan.strategy()).isEqualTo("BALANCED");
            assertThat(plan.totalPortfolioValue()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(plan.allocations()).isNotEmpty();
            assertThat(plan.tradeRecommendations()).isNotNull();

            verify(portfolioService).getPortfolioById(1L);
            verify(positionService).getOpenPositions(1L);
        }

        @Test
        @DisplayName("Should throw exception for non-existent portfolio")
        void shouldThrowExceptionForNonExistentPortfolio() {
            when(portfolioService.getPortfolioById(999L))
                .thenThrow(new IllegalArgumentException("Portfolio not found"));

            CompletableFuture<RebalancingPlan> future = rebalancingService.generateRebalancingPlan(
                999L, targetAllocations, "BALANCED"
            );

            assertThatThrownBy(future::join)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Portfolio not found");

            verify(portfolioService).getPortfolioById(999L);
        }

        @Test
        @DisplayName("Should return empty plan when no rebalancing needed")
        void shouldReturnEmptyPlanWhenNoRebalancingNeeded() {
            List<TargetAllocation> perfectAllocations = List.of(
                new TargetAllocation("AAPL", BigDecimal.valueOf(17.5), BigDecimal.valueOf(15), BigDecimal.valueOf(25), "EQUITY", "TECH"),
                new TargetAllocation("GOOGL", BigDecimal.valueOf(7.5), BigDecimal.valueOf(5), BigDecimal.valueOf(15), "EQUITY", "TECH")
            );

            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(testPositions);

            CompletableFuture<RebalancingPlan> future = rebalancingService.generateRebalancingPlan(
                1L, perfectAllocations, "BALANCED"
            );

            RebalancingPlan plan = future.join();

            assertThat(plan).isNotNull();
            assertThat(plan.tradeRecommendations()).isEmpty();
            assertThat(plan.netRebalancingCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should generate buy recommendations for underweight positions")
        void shouldGenerateBuyRecommendationsForUnderweightPositions() {
            List<TargetAllocation> targetWithMSFT = List.of(
                new TargetAllocation("AAPL", BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(20), "EQUITY", "TECH"),
                new TargetAllocation("MSFT", BigDecimal.valueOf(30), BigDecimal.valueOf(25), BigDecimal.valueOf(35), "EQUITY", "TECH")
            );

            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(testPositions);

            CompletableFuture<RebalancingPlan> future = rebalancingService.generateRebalancingPlan(
                1L, targetWithMSFT, "BALANCED"
            );

            RebalancingPlan plan = future.join();

            assertThat(plan.tradeRecommendations())
                .isNotEmpty()
                .anyMatch(rec -> rec.symbol().equals("MSFT") && rec.action().equals("BUY"));
        }

        @Test
        @DisplayName("Should generate sell recommendations for overweight positions")
        void shouldGenerateSellRecommendationsForOverweightPositions() {
            List<TargetAllocation> lowTargets = List.of(
                new TargetAllocation("AAPL", BigDecimal.valueOf(5), BigDecimal.valueOf(0), BigDecimal.valueOf(10), "EQUITY", "TECH"),
                new TargetAllocation("GOOGL", BigDecimal.valueOf(3), BigDecimal.valueOf(0), BigDecimal.valueOf(5), "EQUITY", "TECH")
            );

            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(testPositions);

            CompletableFuture<RebalancingPlan> future = rebalancingService.generateRebalancingPlan(
                1L, lowTargets, "BALANCED"
            );

            RebalancingPlan plan = future.join();

            assertThat(plan.tradeRecommendations())
                .isNotEmpty()
                .allMatch(rec -> rec.action().equals("SELL"));
        }

        @Test
        @DisplayName("Should calculate estimated trading costs")
        void shouldCalculateEstimatedTradingCosts() {
            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(testPositions);

            CompletableFuture<RebalancingPlan> future = rebalancingService.generateRebalancingPlan(
                1L, targetAllocations, "BALANCED"
            );

            RebalancingPlan plan = future.join();

            if (!plan.tradeRecommendations().isEmpty()) {
                assertThat(plan.estimatedTradingCosts()).isGreaterThan(BigDecimal.ZERO);
                assertThat(plan.netRebalancingCost()).isGreaterThan(BigDecimal.ZERO);
            }
        }
    }

    @Nested
    @DisplayName("Execute Rebalancing Tests")
    class ExecuteRebalancingTests {

        private RebalancingPlan validPlan;

        @BeforeEach
        void setUpPlan() {
            AllocationComparison comparison = new AllocationComparison(
                "AAPL",
                BigDecimal.valueOf(17.5),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(17500),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(2500),
                true
            );

            TradeRecommendation trade = new TradeRecommendation(
                "AAPL",
                "BUY",
                15,
                BigDecimal.valueOf(175),
                BigDecimal.valueOf(2625),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                "Underweight by 2.5%",
                1
            );

            validPlan = new RebalancingPlan(
                1L,
                "BALANCED",
                BigDecimal.valueOf(100000),
                Map.of("AAPL", comparison),
                List.of(trade),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                "LOW",
                Instant.now()
            );
        }

        @Test
        @DisplayName("Should execute rebalancing successfully")
        void shouldExecuteRebalancingSuccessfully() {
            CompletableFuture<RebalancingResult> future = rebalancingService.executeRebalancing(1L, validPlan);

            RebalancingResult result = future.join();

            assertThat(result).isNotNull();
            assertThat(result.portfolioId()).isEqualTo(1L);
            assertThat(result.status()).isIn("COMPLETED", "PENDING", "INITIATED");
            assertThat(result.orderIds()).isNotNull();
            assertThat(result.orderIds().size()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should throw exception for null plan")
        void shouldThrowExceptionForNullPlan() {
            CompletableFuture<RebalancingResult> future = rebalancingService.executeRebalancing(1L, null);

            assertThatThrownBy(future::join)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plan cannot be null");

            verify(portfolioService, never()).getPortfolioById(any());
        }

        @Test
        @DisplayName("Should throw exception for portfolio ID mismatch")
        void shouldThrowExceptionForPortfolioIdMismatch() {
            CompletableFuture<RebalancingResult> future = rebalancingService.executeRebalancing(999L, validPlan);

            assertThatThrownBy(future::join)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Portfolio ID mismatch");

            verify(portfolioService, never()).getPortfolioById(any());
        }
    }

    @Nested
    @DisplayName("Get Current Allocation Tests")
    class GetCurrentAllocationTests {

        @Test
        @DisplayName("Should get current allocation successfully")
        void shouldGetCurrentAllocationSuccessfully() {
            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(testPositions);

            CompletableFuture<Map<String, BigDecimal>> future = rebalancingService.getCurrentAllocation(1L);

            Map<String, BigDecimal> allocation = future.join();

            assertThat(allocation).isNotEmpty();
            assertThat(allocation).containsKeys("AAPL", "GOOGL");
            assertThat(allocation.get("AAPL")).isGreaterThan(BigDecimal.ZERO);
            assertThat(allocation.get("GOOGL")).isGreaterThan(BigDecimal.ZERO);

            verify(portfolioService).getPortfolioById(1L);
            verify(positionService).getOpenPositions(1L);
        }

        @Test
        @DisplayName("Should return empty map for portfolio with no positions")
        void shouldReturnEmptyMapForPortfolioWithNoPositions() {
            when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(1L)).thenReturn(List.of());

            CompletableFuture<Map<String, BigDecimal>> future = rebalancingService.getCurrentAllocation(1L);

            Map<String, BigDecimal> allocation = future.join();

            assertThat(allocation).isEmpty();

            verify(portfolioService).getPortfolioById(1L);
            verify(positionService).getOpenPositions(1L);
        }

        @Test
        @DisplayName("Should throw exception for non-existent portfolio")
        void shouldThrowExceptionForNonExistentPortfolio() {
            when(portfolioService.getPortfolioById(999L)).thenThrow(new IllegalArgumentException("Portfolio not found"));

            CompletableFuture<Map<String, BigDecimal>> future = rebalancingService.getCurrentAllocation(999L);

            assertThatThrownBy(future::join)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Portfolio not found");

            verify(portfolioService).getPortfolioById(999L);
            verify(positionService, never()).getOpenPositions(any());
        }
    }

    @Nested
    @DisplayName("Validate Target Allocations Tests")
    class ValidateTargetAllocationsTests {

        @Test
        @DisplayName("Should validate correct target allocations")
        void shouldValidateCorrectTargetAllocations() {
            List<TargetAllocation> validAllocations = List.of(
                new TargetAllocation("AAPL", BigDecimal.valueOf(40), BigDecimal.valueOf(30), BigDecimal.valueOf(50), "EQUITY", "TECH"),
                new TargetAllocation("GOOGL", BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(40), "EQUITY", "TECH"),
                new TargetAllocation("MSFT", BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(40), "EQUITY", "TECH")
            );

            boolean isValid = rebalancingService.validateTargetAllocations(validAllocations);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject target allocations not summing to 100%")
        void shouldRejectTargetAllocationsNotSummingTo100() {
            List<TargetAllocation> invalidAllocations = List.of(
                new TargetAllocation("AAPL", BigDecimal.valueOf(40), BigDecimal.valueOf(30), BigDecimal.valueOf(50), "EQUITY", "TECH"),
                new TargetAllocation("GOOGL", BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(40), "EQUITY", "TECH")
            );

            boolean isValid = rebalancingService.validateTargetAllocations(invalidAllocations);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject empty target allocations")
        void shouldRejectEmptyTargetAllocations() {
            boolean isValid = rebalancingService.validateTargetAllocations(List.of());

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject null target allocations")
        void shouldRejectNullTargetAllocations() {
            boolean isValid = rebalancingService.validateTargetAllocations(null);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Virtual Thread Integration Tests")
    class VirtualThreadTests {

        @Test
        @DisplayName("Should handle concurrent rebalancing requests")
        void shouldHandleConcurrentRebalancingRequests() {
            when(portfolioService.getPortfolioById(any())).thenReturn(testPortfolio);
            when(positionService.getOpenPositions(any())).thenReturn(testPositions);

            List<CompletableFuture<RebalancingPlan>> futures = List.of(
                rebalancingService.generateRebalancingPlan(1L, targetAllocations, "BALANCED"),
                rebalancingService.generateRebalancingPlan(1L, targetAllocations, "AGGRESSIVE"),
                rebalancingService.generateRebalancingPlan(1L, targetAllocations, "CONSERVATIVE")
            );

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            futures.forEach(future -> assertThat(future).isCompleted());
        }
    }
}
