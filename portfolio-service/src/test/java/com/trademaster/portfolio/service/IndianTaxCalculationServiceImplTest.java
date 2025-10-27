package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.TaxCalculationRequest;
import com.trademaster.portfolio.dto.TaxReport;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.service.impl.IndianTaxCalculationServiceImpl;
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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Indian Tax Calculation Service Unit Tests
 *
 * Comprehensive test suite for Indian tax calculation service with STCG, LTCG, and STT calculations.
 *
 * Rule #20: Testing Standards - >80% coverage with functional builders
 * Rule #3: Functional programming patterns in tests
 * Rule #12: Virtual Thread testing patterns
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IndianTaxCalculationService Unit Tests")
class IndianTaxCalculationServiceImplTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private IndianTaxCalculationServiceImpl taxService;

    private TaxCalculationRequest shortTermRequest;
    private TaxCalculationRequest longTermRequest;
    private Position shortTermPosition;
    private Position longTermPosition;

    @BeforeEach
    void setUp() {
        Instant shortTermDate = Instant.now().minus(180, ChronoUnit.DAYS); // 180 days ago
        Instant longTermDate = Instant.now().minus(400, ChronoUnit.DAYS); // 400 days ago

        shortTermRequest = new TaxCalculationRequest(
            "AAPL",
            100,
            BigDecimal.valueOf(180),
            BigDecimal.valueOf(150),
            shortTermDate,
            "EQUITY",
            "DELIVERY"
        );

        longTermRequest = new TaxCalculationRequest(
            "GOOGL",
            50,
            BigDecimal.valueOf(160),
            BigDecimal.valueOf(140),
            longTermDate,
            "EQUITY",
            "DELIVERY"
        );

        shortTermPosition = Position.builder()
            .symbol("AAPL")
            .quantity(100)
            .averageCost(BigDecimal.valueOf(150))
            .marketValue(BigDecimal.valueOf(18000))
            .openedAt(shortTermDate)
            .build();

        longTermPosition = Position.builder()
            .symbol("GOOGL")
            .quantity(50)
            .averageCost(BigDecimal.valueOf(140))
            .marketValue(BigDecimal.valueOf(8000))
            .openedAt(longTermDate)
            .build();
    }

    @Nested
    @DisplayName("Calculate Tax Impact Tests - STCG")
    class STCGTaxTests {

        @Test
        @DisplayName("Should calculate STCG tax at 15% for short-term holdings")
        void shouldCalculateSTCGTaxCorrectly() {
            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(shortTermRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // Gross PnL = (180 - 150) * 100 = 3000
            // Capital Gain = 3000 (no exemption for STCG)
            // Tax = 3000 * 0.15 = 450
            // STT = 18000 * 0.00025 = 4.50
            assertThat(impact).isNotNull();
            assertThat(impact.symbol()).isEqualTo("AAPL");
            assertThat(impact.taxCategory()).isEqualTo("STCG");
            assertThat(impact.grossPnL()).isEqualByComparingTo(BigDecimal.valueOf(3000));
            assertThat(impact.capitalGain()).isEqualByComparingTo(BigDecimal.valueOf(3000));
            assertThat(impact.taxRate()).isEqualByComparingTo(BigDecimal.valueOf(0.15));
            assertThat(impact.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(450.00));
            assertThat(impact.sttAmount()).isCloseTo(BigDecimal.valueOf(4.50), within(BigDecimal.valueOf(0.01)));
            assertThat(impact.isLongTerm()).isFalse();
        }

        @Test
        @DisplayName("Should calculate zero tax for STCG losses")
        void shouldCalculateZeroTaxForSTCGLosses() {
            TaxCalculationRequest lossRequest = new TaxCalculationRequest(
                "AAPL",
                100,
                BigDecimal.valueOf(140), // Selling at loss
                BigDecimal.valueOf(150),
                Instant.now().minus(180, ChronoUnit.DAYS),
                "EQUITY",
                "DELIVERY"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(lossRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            assertThat(impact.grossPnL()).isLessThan(BigDecimal.ZERO);
            assertThat(impact.capitalGain()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(impact.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(impact.isLoss()).isTrue();
        }

        @Test
        @DisplayName("Should include STT for STCG delivery transactions")
        void shouldIncludeSTTForDeliveryTransactions() {
            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(shortTermRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // STT = sellPrice * quantity * 0.00025 = 180 * 100 * 0.00025 = 4.50
            assertThat(impact.sttAmount()).isCloseTo(BigDecimal.valueOf(4.50), within(BigDecimal.valueOf(0.01)));
            assertThat(impact.totalTax()).isEqualByComparingTo(impact.taxAmount().add(impact.sttAmount()));
        }
    }

    @Nested
    @DisplayName("Calculate Tax Impact Tests - LTCG")
    class LTCGTaxTests {

        @Test
        @DisplayName("Should calculate LTCG tax at 10% with ₹1L exemption")
        void shouldCalculateLTCGTaxWithExemption() {
            // Create request with large gain to test exemption
            TaxCalculationRequest largeGainRequest = new TaxCalculationRequest(
                "GOOGL",
                1000,
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(150),
                Instant.now().minus(400, ChronoUnit.DAYS),
                "EQUITY",
                "DELIVERY"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(largeGainRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // Gross PnL = (250 - 150) * 1000 = 100000
            // Capital Gain after exemption = 100000 - 100000 = 0 (exactly at exemption limit)
            // Tax = 0 * 0.10 = 0
            assertThat(impact.taxCategory()).isEqualTo("LTCG");
            assertThat(impact.grossPnL()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(impact.capitalGain()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(impact.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(impact.isLongTerm()).isTrue();
        }

        @Test
        @DisplayName("Should calculate LTCG tax correctly for gains above ₹1L exemption")
        void shouldCalculateLTCGTaxAboveExemption() {
            // Create request with gain > 1L
            TaxCalculationRequest highGainRequest = new TaxCalculationRequest(
                "GOOGL",
                1000,
                BigDecimal.valueOf(270),
                BigDecimal.valueOf(150),
                Instant.now().minus(400, ChronoUnit.DAYS),
                "EQUITY",
                "DELIVERY"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(highGainRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // Gross PnL = (270 - 150) * 1000 = 120000
            // Capital Gain after exemption = 120000 - 100000 = 20000
            // Tax = 20000 * 0.10 = 2000
            assertThat(impact.taxCategory()).isEqualTo("LTCG");
            assertThat(impact.grossPnL()).isEqualByComparingTo(BigDecimal.valueOf(120000));
            assertThat(impact.capitalGain()).isEqualByComparingTo(BigDecimal.valueOf(20000));
            assertThat(impact.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
        }

        @Test
        @DisplayName("Should calculate zero tax for LTCG losses")
        void shouldCalculateZeroTaxForLTCGLosses() {
            TaxCalculationRequest lossRequest = new TaxCalculationRequest(
                "GOOGL",
                50,
                BigDecimal.valueOf(130), // Selling at loss
                BigDecimal.valueOf(140),
                Instant.now().minus(400, ChronoUnit.DAYS),
                "EQUITY",
                "DELIVERY"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(lossRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            assertThat(impact.grossPnL()).isLessThan(BigDecimal.ZERO);
            assertThat(impact.capitalGain()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(impact.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(impact.isLoss()).isTrue();
        }
    }

    @Nested
    @DisplayName("STT Calculation Tests")
    class STTCalculationTests {

        @Test
        @DisplayName("Should calculate STT for DELIVERY transactions at 0.025%")
        void shouldCalculateSTTForDelivery() {
            TaxCalculationRequest deliveryRequest = new TaxCalculationRequest(
                "AAPL",
                100,
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(150),
                Instant.now().minus(180, ChronoUnit.DAYS),
                "EQUITY",
                "DELIVERY"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(deliveryRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // STT = 180 * 100 * 0.00025 = 4.50
            assertThat(impact.sttAmount()).isCloseTo(BigDecimal.valueOf(4.50), within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should calculate STT for INTRADAY transactions at 0.025%")
        void shouldCalculateSTTForIntraday() {
            TaxCalculationRequest intradayRequest = new TaxCalculationRequest(
                "AAPL",
                100,
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(150),
                Instant.now().minus(180, ChronoUnit.DAYS),
                "EQUITY",
                "INTRADAY"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(intradayRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // STT = 180 * 100 * 0.00025 = 4.50
            assertThat(impact.sttAmount()).isCloseTo(BigDecimal.valueOf(4.50), within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should calculate STT for OPTIONS transactions at 0.1%")
        void shouldCalculateSTTForOptions() {
            TaxCalculationRequest optionsRequest = new TaxCalculationRequest(
                "AAPL",
                100,
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(150),
                Instant.now().minus(180, ChronoUnit.DAYS),
                "EQUITY",
                "OPTIONS"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(optionsRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // STT = 180 * 100 * 0.001 = 18.00
            assertThat(impact.sttAmount()).isCloseTo(BigDecimal.valueOf(18.00), within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should calculate STT for FUTURES transactions at 0.01%")
        void shouldCalculateSTTForFutures() {
            TaxCalculationRequest futuresRequest = new TaxCalculationRequest(
                "AAPL",
                100,
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(150),
                Instant.now().minus(180, ChronoUnit.DAYS),
                "EQUITY",
                "FUTURES"
            );

            CompletableFuture<IndianTaxCalculationService.TaxImpact> future =
                taxService.calculateTaxImpact(futuresRequest);

            IndianTaxCalculationService.TaxImpact impact = future.join();

            // STT = 180 * 100 * 0.0001 = 1.80
            assertThat(impact.sttAmount()).isCloseTo(BigDecimal.valueOf(1.80), within(BigDecimal.valueOf(0.01)));
        }
    }

    @Nested
    @DisplayName("Generate Tax Report Tests")
    class GenerateTaxReportTests {

        @Test
        @DisplayName("Should generate empty tax report for portfolio with no transactions")
        void shouldGenerateEmptyTaxReport() {
            CompletableFuture<TaxReport> future = taxService.generateTaxReport(1L, "FY2024-25");

            TaxReport report = future.join();

            assertThat(report).isNotNull();
            assertThat(report.portfolioId()).isEqualTo(1L);
            assertThat(report.financialYear()).isEqualTo("FY2024-25");
            assertThat(report.transactions()).isEmpty();
            assertThat(report.totalTaxLiability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate effective tax rate correctly")
        void shouldCalculateEffectiveTaxRate() {
            TaxReport report = TaxReport.empty(1L, "FY2024-25");

            // Override with actual values for testing
            TaxReport reportWithValues = new TaxReport(
                1L,
                "FY2024-25",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(40000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(5500),
                BigDecimal.valueOf(34500),
                java.util.List.of(),
                report.generatedAt()
            );

            BigDecimal effectiveRate = reportWithValues.getEffectiveTaxRate();

            // Effective rate = (5500 / 40000) * 100 = 13.75%
            assertThat(effectiveRate).isCloseTo(BigDecimal.valueOf(13.75), within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should calculate LTCG exemption benefit")
        void shouldCalculateLTCGExemptionBenefit() {
            TaxReport report = new TaxReport(
                1L,
                "FY2024-25",
                BigDecimal.valueOf(200000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(200000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(150000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(5500),
                BigDecimal.valueOf(194500),
                java.util.List.of(),
                Instant.now()
            );

            BigDecimal exemptionBenefit = report.getLtcgExemptionBenefit();
            BigDecimal taxableLTCG = report.getTaxableLongTermGains();

            // Exemption benefit = min(150000, 100000) = 100000
            // Taxable LTCG = 150000 - 100000 = 50000
            assertThat(exemptionBenefit).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(taxableLTCG).isEqualByComparingTo(BigDecimal.valueOf(50000));
        }
    }

    @Nested
    @DisplayName("Calculate Realized Gains Tax Tests")
    class CalculateRealizedGainsTaxTests {

        @Test
        @DisplayName("Should calculate realized gains tax for position sale")
        void shouldCalculateRealizedGainsTax() {
            when(positionService.getPosition(1L, "AAPL")).thenReturn(shortTermPosition);

            CompletableFuture<BigDecimal> future = taxService.calculateRealizedGainsTax(
                1L, "AAPL", 50, BigDecimal.valueOf(180)
            );

            BigDecimal tax = future.join();

            // Gross PnL = (180 - 150) * 50 = 1500
            // Tax = 1500 * 0.15 = 225
            assertThat(tax).isCloseTo(BigDecimal.valueOf(225.00), within(BigDecimal.valueOf(0.01)));

            verify(positionService).getPosition(1L, "AAPL");
        }

        @Test
        @DisplayName("Should calculate realized gains tax for long-term position")
        void shouldCalculateRealizedGainsTaxForLongTerm() {
            when(positionService.getPosition(1L, "GOOGL")).thenReturn(longTermPosition);

            CompletableFuture<BigDecimal> future = taxService.calculateRealizedGainsTax(
                1L, "GOOGL", 50, BigDecimal.valueOf(160)
            );

            BigDecimal tax = future.join();

            // Gross PnL = (160 - 140) * 50 = 1000
            // Capital Gain after exemption = max(1000 - 100000, 0) = 0
            // Tax = 0 * 0.10 = 0
            assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);

            verify(positionService).getPosition(1L, "GOOGL");
        }
    }

    @Nested
    @DisplayName("Tax Calculation Request Validation Tests")
    class TaxCalculationRequestValidationTests {

        @Test
        @DisplayName("Should throw exception for null symbol")
        void shouldThrowExceptionForNullSymbol() {
            assertThatThrownBy(() -> new TaxCalculationRequest(
                null,
                100,
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(150),
                Instant.now(),
                "EQUITY",
                "DELIVERY"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Symbol cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        void shouldThrowExceptionForNegativeQuantity() {
            assertThatThrownBy(() -> new TaxCalculationRequest(
                "AAPL",
                -100,
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(150),
                Instant.now(),
                "EQUITY",
                "DELIVERY"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Quantity must be positive");
        }

        @Test
        @DisplayName("Should throw exception for negative sell price")
        void shouldThrowExceptionForNegativeSellPrice() {
            assertThatThrownBy(() -> new TaxCalculationRequest(
                "AAPL",
                100,
                BigDecimal.valueOf(-180),
                BigDecimal.valueOf(150),
                Instant.now(),
                "EQUITY",
                "DELIVERY"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Sell price cannot be negative");
        }

        @Test
        @DisplayName("Should correctly identify long-term vs short-term holdings")
        void shouldCorrectlyIdentifyHoldingPeriod() {
            assertThat(shortTermRequest.isLongTerm()).isFalse();
            assertThat(longTermRequest.isLongTerm()).isTrue();
            assertThat(shortTermRequest.getHoldingDays()).isLessThan(365);
            assertThat(longTermRequest.getHoldingDays()).isGreaterThan(365);
        }
    }

    @Nested
    @DisplayName("Virtual Thread Integration Tests")
    class VirtualThreadTests {

        @Test
        @DisplayName("Should handle concurrent tax calculation requests")
        void shouldHandleConcurrentTaxCalculationRequests() {
            CompletableFuture<IndianTaxCalculationService.TaxImpact> future1 =
                taxService.calculateTaxImpact(shortTermRequest);
            CompletableFuture<IndianTaxCalculationService.TaxImpact> future2 =
                taxService.calculateTaxImpact(longTermRequest);
            CompletableFuture<TaxReport> future3 =
                taxService.generateTaxReport(1L, "FY2024-25");

            CompletableFuture.allOf(future1, future2, future3).join();

            assertThat(future1).isCompleted();
            assertThat(future2).isCompleted();
            assertThat(future3).isCompleted();
        }
    }
}
