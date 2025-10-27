package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.model.AccountType;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.model.RiskLevel;
import com.trademaster.portfolio.repository.PortfolioRepository;
import com.trademaster.portfolio.service.impl.PortfolioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Portfolio Service Unit Tests
 *
 * Comprehensive test suite for portfolio service business logic.
 *
 * Rule #20: Testing Standards - >80% coverage
 * Rule #3: Functional programming patterns in tests
 * Rule #24: Zero compilation errors
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Unit Tests")
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private com.trademaster.portfolio.repository.PositionRepository positionRepository;

    @Mock
    private com.trademaster.portfolio.config.PortfolioLogger portfolioLogger;

    @Mock
    private com.trademaster.portfolio.service.PositionService positionService;

    @Mock
    private com.trademaster.portfolio.service.PnLCalculationService pnlCalculationService;

    @Mock
    private com.trademaster.portfolio.service.PortfolioAnalyticsService analyticsService;

    @Mock
    private com.trademaster.portfolio.service.PortfolioRiskService riskService;

    @Mock
    private com.trademaster.portfolio.service.PortfolioEventPublisher eventPublisher;

    @Mock
    private com.trademaster.portfolio.config.PortfolioMetrics portfolioMetrics;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private Portfolio testPortfolio;
    private CreatePortfolioRequest validRequest;

    @BeforeEach
    void setUp() {
        testPortfolio = Portfolio.builder()
            .portfolioId(1L)
            .userId(1001L)
            .portfolioName("Test Portfolio")
            .totalValue(new BigDecimal("100000"))
            .cashBalance(new BigDecimal("50000"))
            .status(PortfolioStatus.ACTIVE)
            .currency("USD")
            .build();

        validRequest = new CreatePortfolioRequest(
            "New Portfolio",
            new BigDecimal("100000"),
            RiskLevel.MODERATE,
            "USD",
            AccountType.INDIVIDUAL,
            false
        );

        // Setup common mock behaviors for logger and metrics (lenient to avoid UnnecessaryStubbingException)
        io.micrometer.core.instrument.Timer.Sample mockTimerSample = mock(io.micrometer.core.instrument.Timer.Sample.class);
        lenient().when(portfolioMetrics.startCreationTimer()).thenReturn(mockTimerSample);
        lenient().when(portfolioMetrics.startLookupTimer()).thenReturn(mockTimerSample);
        lenient().when(portfolioMetrics.startCashUpdateTimer()).thenReturn(mockTimerSample);
    }

    @Nested
    @DisplayName("Portfolio Creation")
    class PortfolioCreation {

        @Test
        @DisplayName("Should create portfolio with valid request")
        void shouldCreatePortfolioWithValidRequest() {
            // Given
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

            // When
            Portfolio result = portfolioService.createPortfolio(1001L, validRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPortfolioId()).isEqualTo(1L);
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when user already has active portfolio")
        void shouldThrowExceptionWhenUserHasActivePortfolio() {
            // Given
            when(portfolioRepository.existsByUserIdAndStatus(eq(1001L), eq(PortfolioStatus.ACTIVE)))
                .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> portfolioService.createPortfolio(1001L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create portfolio for user");
        }

        @Test
        @DisplayName("Should initialize portfolio with correct default values")
        void shouldInitializePortfolioWithDefaults() {
            // Given
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Portfolio result = portfolioService.createPortfolio(1001L, validRequest);

            // Then
            assertThat(result.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
            assertThat(result.getDayTradesCount()).isZero();
            assertThat(result.getRealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Portfolio Retrieval")
    class PortfolioRetrieval {

        @Test
        @DisplayName("Should find portfolio by user ID")
        void shouldFindPortfolioByUserId() {
            // Given
            when(portfolioRepository.findByUserIdAndStatus(eq(1001L), eq(PortfolioStatus.ACTIVE)))
                .thenReturn(Optional.of(testPortfolio));

            // When
            Portfolio result = portfolioService.getPortfolioByUserId(1001L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1001L);
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            when(portfolioRepository.findByUserIdAndStatus(eq(1001L), eq(PortfolioStatus.ACTIVE)))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.getPortfolioByUserId(1001L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No active portfolio found for user");
        }

        @Test
        @DisplayName("Should find portfolio by ID")
        void shouldFindPortfolioById() {
            // Given
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));

            // When
            Portfolio result = portfolioService.getPortfolioById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPortfolioId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Portfolio Updates")
    class PortfolioUpdates {

        @Test
        @DisplayName("Should update portfolio valuation")
        void shouldUpdatePortfolioValuation() {
            // Given
            BigDecimal newValue = new BigDecimal("110000");
            BigDecimal newPnl = new BigDecimal("10000");
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));
            when(portfolioRepository.save(any(Portfolio.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            Portfolio result = portfolioService.updateValuation(1L, newValue, newPnl);

            // Then
            assertThat(result.getTotalValue()).isEqualByComparingTo(newValue);
            assertThat(result.getUnrealizedPnl()).isEqualByComparingTo(newPnl);
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should update cash balance")
        void shouldUpdateCashBalance() {
            // Given
            BigDecimal deposit = new BigDecimal("10000");
            BigDecimal originalBalance = testPortfolio.getCashBalance();  // Capture original before service call
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));
            when(portfolioRepository.save(any(Portfolio.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            Portfolio result = portfolioService.updateCashBalance(1L, deposit);

            // Then
            BigDecimal expectedBalance = originalBalance.add(deposit);
            assertThat(result.getCashBalance()).isEqualByComparingTo(expectedBalance);
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found for update")
        void shouldThrowExceptionWhenPortfolioNotFoundForUpdate() {
            // Given
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.updateValuation(
                1L, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
        }
    }

    @Nested
    @DisplayName("Day Trading Operations")
    class DayTradingOperations {

        @Test
        @DisplayName("Should increment day trades count")
        void shouldIncrementDayTradesCount() {
            // Given
            when(portfolioRepository.incrementDayTradesCount(eq(1L)))
                .thenReturn(1);

            // When
            int result = portfolioService.incrementDayTradesCount(1L);

            // Then
            assertThat(result).isEqualTo(1);
            verify(portfolioRepository).incrementDayTradesCount(eq(1L));
        }

        @Test
        @DisplayName("Should reset day trades count")
        void shouldResetDayTradesCount() {
            // Given
            when(portfolioRepository.resetDayTradesCount())
                .thenReturn(5);

            // When
            int result = portfolioService.resetDayTradesCount();

            // Then
            assertThat(result).isEqualTo(5);
            verify(portfolioRepository).resetDayTradesCount();
        }

        @Test
        @DisplayName("Should check if portfolio approaching day trade limit")
        void shouldCheckDayTradeLimit() {
            // Given
            testPortfolio.setDayTradesCount(3);
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));

            // When
            boolean result = portfolioService.isApproachingDayTradeLimit(1L);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Portfolio Status Management")
    class PortfolioStatusManagement {

        @Test
        @DisplayName("Should activate portfolio")
        void shouldActivatePortfolio() {
            // Given
            testPortfolio.setStatus(PortfolioStatus.SUSPENDED);
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));
            when(portfolioRepository.save(any(Portfolio.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            Portfolio result = portfolioService.activatePortfolio(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should close portfolio")
        void shouldClosePortfolio() {
            // Given
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));
            when(portfolioRepository.save(any(Portfolio.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            Portfolio result = portfolioService.closePortfolio(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo(PortfolioStatus.CLOSED);
        }

        @Test
        @DisplayName("Should suspend portfolio")
        void shouldSuspendPortfolio() {
            // Given
            when(portfolioRepository.findById(eq(1L)))
                .thenReturn(Optional.of(testPortfolio));
            when(portfolioRepository.save(any(Portfolio.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            Portfolio result = portfolioService.suspendPortfolio(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo(PortfolioStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("Business Rules Validation")
    class BusinessRulesValidation {

        @Test
        @DisplayName("Should validate minimum cash balance")
        void shouldValidateMinimumCashBalance() {
            // Given
            testPortfolio.setCashBalance(new BigDecimal("100"));

            // When
            boolean result = portfolioService.hasMinimumCashBalance(testPortfolio);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should validate portfolio can trade")
        void shouldValidatePortfolioCanTrade() {
            // When
            boolean result = portfolioService.canTrade(testPortfolio);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should not allow trading when portfolio suspended")
        void shouldNotAllowTradingWhenSuspended() {
            // Given
            testPortfolio.setStatus(PortfolioStatus.SUSPENDED);

            // When
            boolean result = portfolioService.canTrade(testPortfolio);

            // Then
            assertThat(result).isFalse();
        }
    }
}
