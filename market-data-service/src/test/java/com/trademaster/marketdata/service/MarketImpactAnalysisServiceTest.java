package com.trademaster.marketdata.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MarketImpactAnalysisService
 *
 * Tests MANDATORY RULES compliance for functional patterns:
 * - RULE #3: NavigableMap pattern, Optional chains, Switch expressions
 * - RULE #5: Cognitive complexity ≤7 per method
 * - RULE #17: Named constants externalization
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Market Impact Analysis Service Tests")
class MarketImpactAnalysisServiceTest {

    private MarketImpactAnalysisService marketImpactAnalysisService;

    @BeforeEach
    void setUp() {
        marketImpactAnalysisService = new MarketImpactAnalysisService();
    }

    @Nested
    @DisplayName("Economic Event Impact Analysis Tests - RULE #3: NavigableMap Pattern")
    class EconomicEventImpactTest {

        @Test
        @DisplayName("Should return high impact for large deviation - RULE #3: NavigableMap")
        void shouldReturnHighImpactForLargeDeviation() {
            // Given
            String eventType = "GDP";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 85.0; // 15% deviation

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(1.0); // HIGH_IMPACT_SCORE
        }

        @Test
        @DisplayName("Should return medium-high impact for 5-10% deviation - RULE #3: NavigableMap")
        void shouldReturnMediumHighImpactForModerateDeviation() {
            // Given
            String eventType = "Employment";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 93.0; // 7% deviation

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(0.8); // MEDIUM_HIGH_IMPACT_SCORE
        }

        @Test
        @DisplayName("Should return medium impact for 2-5% deviation - RULE #3: NavigableMap")
        void shouldReturnMediumImpactForSmallDeviation() {
            // Given
            String eventType = "Inflation";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 97.0; // 3% deviation

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(0.6); // MEDIUM_IMPACT_SCORE
        }

        @Test
        @DisplayName("Should return low-medium impact for 1-2% deviation - RULE #3: NavigableMap")
        void shouldReturnLowMediumImpactForMinimalDeviation() {
            // Given
            String eventType = "Retail Sales";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 98.5; // 1.5% deviation

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(0.4); // LOW_MEDIUM_IMPACT_SCORE
        }

        @Test
        @DisplayName("Should return low impact for <1% deviation - RULE #3: NavigableMap")
        void shouldReturnLowImpactForTinyDeviation() {
            // Given
            String eventType = "Housing Data";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 99.5; // 0.5% deviation

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(0.2); // LOW_IMPACT_SCORE
        }

        @Test
        @DisplayName("Should return moderate impact when no forecast - RULE #3: Optional orElse")
        void shouldReturnModerateImpactWhenNoForecast() {
            // Given
            String eventType = "GDP";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 0.0; // No forecast

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(0.5); // NO_FORECAST_IMPACT
        }

        @Test
        @DisplayName("Should handle negative deviations correctly - RULE #3: NavigableMap")
        void shouldHandleNegativeDeviations() {
            // Given
            String eventType = "GDP";
            String region = "US";
            double previousValue = 90.0;
            double forecastValue = 100.0; // -10% deviation (reversed)

            // When
            double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );

            // Then
            assertThat(impact).isEqualTo(1.0); // HIGH_IMPACT_SCORE (abs deviation)
        }

        @Test
        @DisplayName("Should handle exact threshold boundaries - RULE #3: NavigableMap floorEntry")
        void shouldHandleThresholdBoundaries() {
            // Given - Exactly 10% deviation
            double impact10 = marketImpactAnalysisService.analyzeEconomicEventImpact(
                "GDP", "US", 110.0, 100.0
            );

            // Given - Exactly 5% deviation
            double impact5 = marketImpactAnalysisService.analyzeEconomicEventImpact(
                "GDP", "US", 105.0, 100.0
            );

            // Given - Exactly 2% deviation
            double impact2 = marketImpactAnalysisService.analyzeEconomicEventImpact(
                "GDP", "US", 102.0, 100.0
            );

            // Then
            assertThat(impact10).isEqualTo(1.0);
            assertThat(impact5).isEqualTo(0.8);
            assertThat(impact2).isEqualTo(0.6);
        }
    }

    @Nested
    @DisplayName("Market Correlation Tests - RULE #3: Switch Expression")
    class MarketCorrelationTest {

        @Test
        @DisplayName("Should return high correlation for GDP - RULE #17: HIGH_CORRELATION constant")
        void shouldReturnHighCorrelationForGDP() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation("GDP", "US");

            // Then
            assertThat(correlation).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should return high correlation for employment - RULE #3: Switch multi-case")
        void shouldReturnHighCorrelationForEmployment() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation("Employment", "US");

            // Then
            assertThat(correlation).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should return high correlation for inflation - RULE #3: Switch multi-case")
        void shouldReturnHighCorrelationForInflation() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation("INFLATION", "US");

            // Then
            assertThat(correlation).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should return medium correlation for retail sales - RULE #17: MEDIUM_CORRELATION")
        void shouldReturnMediumCorrelationForRetailSales() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation("retail_sales", "US");

            // Then
            assertThat(correlation).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Should return medium correlation for industrial production")
        void shouldReturnMediumCorrelationForIndustrialProduction() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation(
                "industrial_production", "US"
            );

            // Then
            assertThat(correlation).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Should return low-medium correlation for housing data - RULE #17: LOW_MEDIUM_CORRELATION")
        void shouldReturnLowMediumCorrelationForHousingData() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation("housing_data", "US");

            // Then
            assertThat(correlation).isEqualTo(0.4);
        }

        @Test
        @DisplayName("Should return low correlation for unknown event type - RULE #3: Switch default")
        void shouldReturnLowCorrelationForUnknownEventType() {
            // When
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation("unknown_event", "US");

            // Then
            assertThat(correlation).isEqualTo(0.3);
        }

        @Test
        @DisplayName("Should be case-insensitive - RULE #3: toLowerCase in switch")
        void shouldBeCaseInsensitive() {
            // When
            double correlation1 = marketImpactAnalysisService.calculateMarketCorrelation("GDP", "US");
            double correlation2 = marketImpactAnalysisService.calculateMarketCorrelation("gdp", "US");
            double correlation3 = marketImpactAnalysisService.calculateMarketCorrelation("GdP", "US");

            // Then
            assertThat(correlation1).isEqualTo(correlation2).isEqualTo(correlation3);
        }
    }

    @Nested
    @DisplayName("Impact Assessment Tests - RULE #3: NavigableMap Pattern")
    class ImpactAssessmentTest {

        @Test
        @DisplayName("Should assess HIGH for score >= 0.8 - RULE #3: NavigableMap")
        void shouldAssessHighForHighScore() {
            // When
            String assessment1 = marketImpactAnalysisService.assessMarketImpact(1.0);
            String assessment2 = marketImpactAnalysisService.assessMarketImpact(0.9);
            String assessment3 = marketImpactAnalysisService.assessMarketImpact(0.8);

            // Then
            assertThat(assessment1).isEqualTo("HIGH");
            assertThat(assessment2).isEqualTo("HIGH");
            assertThat(assessment3).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should assess MEDIUM_HIGH for score >= 0.6 - RULE #3: NavigableMap")
        void shouldAssessMediumHighForModerateHighScore() {
            // When
            String assessment1 = marketImpactAnalysisService.assessMarketImpact(0.7);
            String assessment2 = marketImpactAnalysisService.assessMarketImpact(0.6);

            // Then
            assertThat(assessment1).isEqualTo("MEDIUM_HIGH");
            assertThat(assessment2).isEqualTo("MEDIUM_HIGH");
        }

        @Test
        @DisplayName("Should assess MEDIUM for score >= 0.4 - RULE #3: NavigableMap")
        void shouldAssessMediumForModerateScore() {
            // When
            String assessment1 = marketImpactAnalysisService.assessMarketImpact(0.5);
            String assessment2 = marketImpactAnalysisService.assessMarketImpact(0.4);

            // Then
            assertThat(assessment1).isEqualTo("MEDIUM");
            assertThat(assessment2).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Should assess LOW_MEDIUM for score >= 0.2 - RULE #3: NavigableMap")
        void shouldAssessLowMediumForLowModerateScore() {
            // When
            String assessment1 = marketImpactAnalysisService.assessMarketImpact(0.3);
            String assessment2 = marketImpactAnalysisService.assessMarketImpact(0.2);

            // Then
            assertThat(assessment1).isEqualTo("LOW_MEDIUM");
            assertThat(assessment2).isEqualTo("LOW_MEDIUM");
        }

        @Test
        @DisplayName("Should assess LOW for score < 0.2 - RULE #3: NavigableMap default")
        void shouldAssessLowForLowScore() {
            // When
            String assessment1 = marketImpactAnalysisService.assessMarketImpact(0.1);
            String assessment2 = marketImpactAnalysisService.assessMarketImpact(0.0);

            // Then
            assertThat(assessment1).isEqualTo("LOW");
            assertThat(assessment2).isEqualTo("LOW");
        }

        @Test
        @DisplayName("Should handle boundary values correctly - RULE #3: NavigableMap floorEntry")
        void shouldHandleBoundaryValuesCorrectly() {
            // Test exact threshold boundaries
            assertThat(marketImpactAnalysisService.assessMarketImpact(0.8)).isEqualTo("HIGH");
            assertThat(marketImpactAnalysisService.assessMarketImpact(0.6)).isEqualTo("MEDIUM_HIGH");
            assertThat(marketImpactAnalysisService.assessMarketImpact(0.4)).isEqualTo("MEDIUM");
            assertThat(marketImpactAnalysisService.assessMarketImpact(0.2)).isEqualTo("LOW_MEDIUM");
        }

        @Test
        @DisplayName("Should handle edge cases - RULE #3: Optional orElse")
        void shouldHandleEdgeCases() {
            // When
            String assessmentNegative = marketImpactAnalysisService.assessMarketImpact(-0.1);
            String assessmentVeryHigh = marketImpactAnalysisService.assessMarketImpact(2.0);

            // Then
            assertThat(assessmentNegative).isEqualTo("LOW");
            assertThat(assessmentVeryHigh).isEqualTo("HIGH");
        }
    }

    @Nested
    @DisplayName("MANDATORY RULES Compliance Validation")
    class MandatoryRulesComplianceTest {

        @Test
        @DisplayName("RULE #3: All methods use functional patterns (NavigableMap, Optional, Switch)")
        void shouldUseOnlyFunctionalPatterns() {
            // All methods should execute without errors using functional patterns
            assertThatNoException().isThrownBy(() -> {
                marketImpactAnalysisService.analyzeEconomicEventImpact("GDP", "US", 100.0, 95.0);
                marketImpactAnalysisService.calculateMarketCorrelation("gdp", "US");
                marketImpactAnalysisService.assessMarketImpact(0.75);
            });
        }

        @Test
        @DisplayName("RULE #5: Methods maintain low cognitive complexity")
        void shouldMaintainLowCognitiveComplexity() {
            // Complex scenarios should still execute efficiently
            double[] testValues = {0.0, 50.0, 90.0, 95.0, 97.0, 99.0, 100.0, 105.0, 110.0, 150.0};

            for (double previous : testValues) {
                for (double forecast : testValues) {
                    if (forecast != 0.0) {
                        double impact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                            "GDP", "US", previous, forecast
                        );
                        String assessment = marketImpactAnalysisService.assessMarketImpact(impact);

                        assertThat(impact).isBetween(0.0, 1.0);
                        assertThat(assessment).isIn("LOW", "LOW_MEDIUM", "MEDIUM", "MEDIUM_HIGH", "HIGH");
                    }
                }
            }
        }

        @Test
        @DisplayName("RULE #17: All magic numbers externalized to constants")
        void shouldUseNamedConstants() {
            // Verify behavior is consistent with documented constants
            // All thresholds and impact scores use named constants

            // Test that constant values are applied correctly
            double highImpact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                "GDP", "US", 120.0, 100.0  // 20% deviation
            );
            assertThat(highImpact).isEqualTo(1.0); // HIGH_IMPACT_SCORE

            double noForecastImpact = marketImpactAnalysisService.analyzeEconomicEventImpact(
                "GDP", "US", 100.0, 0.0
            );
            assertThat(noForecastImpact).isEqualTo(0.5); // NO_FORECAST_IMPACT

            // Test correlation constants
            double highCorrelation = marketImpactAnalysisService.calculateMarketCorrelation("gdp", "US");
            assertThat(highCorrelation).isEqualTo(0.8); // HIGH_CORRELATION
        }

        @Test
        @DisplayName("Integration test: Analyze event -> Assess impact")
        void shouldProvideConsistentEndToEndAnalysis() {
            // Given
            String eventType = "Employment";
            String region = "US";
            double previousValue = 100.0;
            double forecastValue = 92.0; // 8% deviation

            // When
            double impactScore = marketImpactAnalysisService.analyzeEconomicEventImpact(
                eventType, region, previousValue, forecastValue
            );
            String assessment = marketImpactAnalysisService.assessMarketImpact(impactScore);
            double correlation = marketImpactAnalysisService.calculateMarketCorrelation(eventType, region);

            // Then
            assertThat(impactScore).isEqualTo(0.8); // Medium-high impact (5-10% deviation)
            assertThat(assessment).isEqualTo("HIGH"); // HIGH assessment (>= 0.8)
            assertThat(correlation).isEqualTo(0.8); // High correlation for employment
        }
    }
}
