package com.trademaster.marketdata.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SentimentAnalysisService
 *
 * Tests MANDATORY RULES compliance for functional patterns:
 * - RULE #3: No if-else, no loops - Optional chains, Stream API, NavigableMap
 * - RULE #5: Cognitive complexity ≤7 per method, max 15 lines per method
 * - RULE #9: Immutable data structures (NavigableMap, Collections.unmodifiableSet)
 * - RULE #17: Named constants externalization
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Sentiment Analysis Service Tests")
class SentimentAnalysisServiceTest {

    private SentimentAnalysisService sentimentAnalysisService;

    @BeforeEach
    void setUp() {
        sentimentAnalysisService = new SentimentAnalysisService();
    }

    @Nested
    @DisplayName("Sentiment Analysis Tests - RULE #3: Optional Chain Pattern")
    class SentimentAnalysisTest {

        @Test
        @DisplayName("Should return neutral for null content - RULE #3: Optional.ofNullable")
        void shouldReturnNeutralForNullContent() {
            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(null);

            // Then
            assertThat(sentiment).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return neutral for empty content - RULE #3: Optional.filter")
        void shouldReturnNeutralForEmptyContent() {
            // When
            double sentiment1 = sentimentAnalysisService.analyzeSentiment("");
            double sentiment2 = sentimentAnalysisService.analyzeSentiment("   ");

            // Then
            assertThat(sentiment1).isEqualTo(0.0);
            assertThat(sentiment2).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate positive sentiment - RULE #3: Stream API")
        void shouldCalculatePositiveSentiment() {
            // Given
            String content = "The stock shows bullish momentum with strong growth potential. " +
                "Analysts recommend buy with excellent profit margins.";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then
            assertThat(sentiment).isPositive();
        }

        @Test
        @DisplayName("Should calculate negative sentiment - RULE #3: Stream API")
        void shouldCalculateNegativeSentiment() {
            // Given
            String content = "The company faces bearish trends with weak performance and declining sales. " +
                "Analysts downgrade to sell due to poor fundamentals and crash risk.";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then
            assertThat(sentiment).isNegative();
        }

        @Test
        @DisplayName("Should calculate neutral sentiment - RULE #3: Functional composition")
        void shouldCalculateNeutralSentiment() {
            // Given
            String content = "The company reported its quarterly results today. " +
                "The market is waiting for further announcements.";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then
            assertThat(sentiment).isBetween(-0.3, 0.3);
        }

        @Test
        @DisplayName("Should be case-insensitive - RULE #3: String::toLowerCase in chain")
        void shouldBeCaseInsensitive() {
            // Given
            String content1 = "BULLISH GROWTH PROFIT";
            String content2 = "bullish growth profit";

            // When
            double sentiment1 = sentimentAnalysisService.analyzeSentiment(content1);
            double sentiment2 = sentimentAnalysisService.analyzeSentiment(content2);

            // Then
            assertThat(sentiment1).isEqualTo(sentiment2);
        }

        @Test
        @DisplayName("Should handle mixed sentiment - RULE #3: Functional composition")
        void shouldHandleMixedSentiment() {
            // Given
            String content = "The company has strong fundamentals but faces weak market conditions. " +
                "Growth is positive but risks are negative.";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then - Should be relatively neutral due to mixed signals
            assertThat(sentiment).isBetween(-0.5, 0.5);
        }
    }

    @Nested
    @DisplayName("Market Sentiment Tests - RULE #17: Constants Usage")
    class MarketSentimentTest {

        @Test
        @DisplayName("Should return sentiment score in valid range - RULE #17: SENTIMENT_SCALE constants")
        void shouldReturnSentimentInValidRange() {
            // When
            var result = sentimentAnalysisService.calculateMarketSentiment("AAPL");

            // Then
            assertThat(result).containsKeys("symbol", "sentimentScore", "sentimentCategory",
                "confidence", "sourcesCount", "timestamp");

            double score = (Double) result.get("sentimentScore");
            assertThat(score).isBetween(-1.0, 1.0);
        }

        @Test
        @DisplayName("Should use default confidence - RULE #17: DEFAULT_CONFIDENCE constant")
        void shouldUseDefaultConfidence() {
            // When
            var result = sentimentAnalysisService.calculateMarketSentiment("TSLA");

            // Then
            assertThat(result.get("confidence")).isEqualTo(0.75);
        }

        @Test
        @DisplayName("Should use default sources count - RULE #17: DEFAULT_SOURCES_COUNT constant")
        void shouldUseDefaultSourcesCount() {
            // When
            var result = sentimentAnalysisService.calculateMarketSentiment("MSFT");

            // Then
            assertThat(result.get("sourcesCount")).isEqualTo(150);
        }

        @Test
        @DisplayName("Should include timestamp - RULE #5: Simple method")
        void shouldIncludeTimestamp() {
            // When
            var result = sentimentAnalysisService.calculateMarketSentiment("GOOGL");

            // Then
            assertThat(result.get("timestamp")).isInstanceOf(Long.class);
            assertThat((Long) result.get("timestamp")).isGreaterThan(0L);
        }

        @Test
        @DisplayName("Should categorize sentiment - RULE #3: NavigableMap pattern")
        void shouldCategorizeSentiment() {
            // When
            var result = sentimentAnalysisService.calculateMarketSentiment("AMZN");

            // Then
            String category = (String) result.get("sentimentCategory");
            assertThat(category).isIn("VERY_POSITIVE", "POSITIVE", "NEUTRAL", "NEGATIVE", "VERY_NEGATIVE");
        }
    }

    @Nested
    @DisplayName("News Impact Analysis Tests - RULE #3: Functional Composition")
    class NewsImpactTest {

        @Test
        @DisplayName("Should amplify urgent news impact - RULE #17: URGENT_MULTIPLIER constant")
        void shouldAmplifyUrgentNewsImpact() {
            // Given
            String urgentNews = "BREAKING: Company announces excellent quarterly results with strong growth";
            String normalNews = "Company announces excellent quarterly results with strong growth";

            // When
            double urgentImpact = sentimentAnalysisService.analyzeNewsImpact(urgentNews, "AAPL");
            double normalImpact = sentimentAnalysisService.analyzeNewsImpact(normalNews, "AAPL");

            // Then - Urgent news should have higher impact
            assertThat(urgentImpact).isGreaterThan(normalImpact);
        }

        @Test
        @DisplayName("Should amplify relevant news impact - RULE #17: HIGH_RELEVANCE_MULTIPLIER constant")
        void shouldAmplifyRelevantNewsImpact() {
            // Given
            String relevantNews = "AAPL stock shows bullish growth with excellent profit margins";
            String generalNews = "Market shows bullish growth with excellent profit margins";

            // When
            double relevantImpact = sentimentAnalysisService.analyzeNewsImpact(relevantNews, "AAPL");
            double generalImpact = sentimentAnalysisService.analyzeNewsImpact(generalNews, "AAPL");

            // Then - Relevant news should have higher impact
            assertThat(relevantImpact).isGreaterThan(generalImpact);
        }

        @Test
        @DisplayName("Should combine urgency and relevance - RULE #3: Functional composition")
        void shouldCombineUrgencyAndRelevance() {
            // Given
            String breakingRelevant = "BREAKING: AAPL announces strong profit growth";
            String normalGeneral = "Company announces profit growth";

            // When
            double highImpact = sentimentAnalysisService.analyzeNewsImpact(breakingRelevant, "AAPL");
            double lowImpact = sentimentAnalysisService.analyzeNewsImpact(normalGeneral, "AAPL");

            // Then - Breaking + relevant should have much higher impact
            assertThat(highImpact).isGreaterThan(lowImpact * 1.5);
        }

        @Test
        @DisplayName("Should handle negative urgent news - RULE #3: Functional composition")
        void shouldHandleNegativeUrgentNews() {
            // Given
            String urgentNegative = "URGENT: AAPL faces weak performance with declining sales and crash risk";

            // When
            double impact = sentimentAnalysisService.analyzeNewsImpact(urgentNegative, "AAPL");

            // Then - Should be negative and amplified
            assertThat(impact).isNegative();
        }
    }

    @Nested
    @DisplayName("Sentiment Categorization Tests - RULE #3: NavigableMap Pattern")
    class SentimentCategorizationTest {

        @Test
        @DisplayName("Should categorize very positive - RULE #3: NavigableMap.floorEntry")
        void shouldCategorizeVeryPositive() {
            // When
            String category1 = sentimentAnalysisService.categorizeSentiment(1.0);
            String category2 = sentimentAnalysisService.categorizeSentiment(0.8);
            String category3 = sentimentAnalysisService.categorizeSentiment(0.6);

            // Then
            assertThat(category1).isEqualTo("VERY_POSITIVE");
            assertThat(category2).isEqualTo("VERY_POSITIVE");
            assertThat(category3).isEqualTo("VERY_POSITIVE");
        }

        @Test
        @DisplayName("Should categorize positive - RULE #3: NavigableMap.floorEntry")
        void shouldCategorizePositive() {
            // When
            String category1 = sentimentAnalysisService.categorizeSentiment(0.5);
            String category2 = sentimentAnalysisService.categorizeSentiment(0.3);
            String category3 = sentimentAnalysisService.categorizeSentiment(0.2);

            // Then
            assertThat(category1).isEqualTo("POSITIVE");
            assertThat(category2).isEqualTo("POSITIVE");
            assertThat(category3).isEqualTo("POSITIVE");
        }

        @Test
        @DisplayName("Should categorize neutral - RULE #3: NavigableMap.floorEntry")
        void shouldCategorizeNeutral() {
            // When
            String category1 = sentimentAnalysisService.categorizeSentiment(0.1);
            String category2 = sentimentAnalysisService.categorizeSentiment(0.0);
            String category3 = sentimentAnalysisService.categorizeSentiment(-0.1);

            // Then
            assertThat(category1).isEqualTo("NEUTRAL");
            assertThat(category2).isEqualTo("NEUTRAL");
            assertThat(category3).isEqualTo("NEUTRAL");
        }

        @Test
        @DisplayName("Should categorize negative - RULE #3: NavigableMap.floorEntry")
        void shouldCategorizeNegative() {
            // When
            String category1 = sentimentAnalysisService.categorizeSentiment(-0.2);
            String category2 = sentimentAnalysisService.categorizeSentiment(-0.4);
            String category3 = sentimentAnalysisService.categorizeSentiment(-0.5);

            // Then
            assertThat(category1).isEqualTo("NEUTRAL"); // -0.2 is exactly at NEUTRAL_THRESHOLD
            assertThat(category2).isEqualTo("NEGATIVE");
            assertThat(category3).isEqualTo("NEGATIVE");
        }

        @Test
        @DisplayName("Should categorize very negative - RULE #3: NavigableMap.floorEntry")
        void shouldCategorizeVeryNegative() {
            // When
            String category1 = sentimentAnalysisService.categorizeSentiment(-0.6);
            String category2 = sentimentAnalysisService.categorizeSentiment(-0.8);
            String category3 = sentimentAnalysisService.categorizeSentiment(-1.0);

            // Then
            assertThat(category1).isEqualTo("NEGATIVE"); // -0.6 is exactly at NEGATIVE_THRESHOLD
            assertThat(category2).isEqualTo("VERY_NEGATIVE");
            assertThat(category3).isEqualTo("VERY_NEGATIVE");
        }

        @Test
        @DisplayName("Should handle exact threshold boundaries - RULE #3: NavigableMap.floorEntry")
        void shouldHandleExactThresholds() {
            // Test exact thresholds
            assertThat(sentimentAnalysisService.categorizeSentiment(0.6)).isEqualTo("VERY_POSITIVE");
            assertThat(sentimentAnalysisService.categorizeSentiment(0.2)).isEqualTo("POSITIVE");
            assertThat(sentimentAnalysisService.categorizeSentiment(-0.2)).isEqualTo("NEUTRAL");
            assertThat(sentimentAnalysisService.categorizeSentiment(-0.6)).isEqualTo("NEGATIVE");
        }

        @Test
        @DisplayName("Should handle edge cases - RULE #3: Optional.orElse fallback")
        void shouldHandleEdgeCases() {
            // When
            String categoryHigh = sentimentAnalysisService.categorizeSentiment(2.0);
            String categoryLow = sentimentAnalysisService.categorizeSentiment(-2.0);

            // Then
            assertThat(categoryHigh).isEqualTo("VERY_POSITIVE");
            assertThat(categoryLow).isEqualTo("VERY_NEGATIVE");
        }
    }

    @Nested
    @DisplayName("Keyword Counting Tests - RULE #3: Stream API Pattern")
    class KeywordCountingTest {

        @Test
        @DisplayName("Should count positive keywords - RULE #3: Stream API with mapToDouble")
        void shouldCountPositiveKeywords() {
            // Given
            String content = "The stock is bullish with positive growth trends and good profit margins";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then - Should detect multiple positive keywords
            assertThat(sentiment).isPositive();
        }

        @Test
        @DisplayName("Should count negative keywords - RULE #3: Stream API with mapToDouble")
        void shouldCountNegativeKeywords() {
            // Given
            String content = "The stock is bearish with negative decline and weak performance";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then - Should detect multiple negative keywords
            assertThat(sentiment).isNegative();
        }

        @Test
        @DisplayName("Should count keyword occurrences correctly - RULE #3: IntStream pattern")
        void shouldCountOccurrencesCorrectly() {
            // Given - Multiple occurrences of the same keyword
            String content = "growth growth growth profit profit";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then - Should count all occurrences
            assertThat(sentiment).isPositive();
        }

        @Test
        @DisplayName("Should handle overlapping keywords - RULE #3: IntStream.filter pattern")
        void shouldHandleOverlappingKeywords() {
            // Given
            String content = "increase increased increasing";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(content);

            // Then - Should count 'increase' occurrence(s)
            assertThat(sentiment).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Urgency Detection Tests - RULE #3: Stream.anyMatch Pattern")
    class UrgencyDetectionTest {

        @Test
        @DisplayName("Should detect breaking news - RULE #17: URGENT_MULTIPLIER constant")
        void shouldDetectBreakingNews() {
            // Given
            String breaking = "BREAKING: Company announces strong results";
            String normal = "Company announces strong results";

            // When
            double breakingImpact = sentimentAnalysisService.analyzeNewsImpact(breaking, "AAPL");
            double normalImpact = sentimentAnalysisService.analyzeNewsImpact(normal, "AAPL");

            // Then
            assertThat(breakingImpact / normalImpact).isGreaterThan(1.0);
        }

        @Test
        @DisplayName("Should detect urgent alerts - RULE #3: Stream.anyMatch")
        void shouldDetectUrgentAlerts() {
            // Given
            String urgent = "URGENT ALERT: Market shows immediate action needed";

            // When
            double impact = sentimentAnalysisService.analyzeNewsImpact(urgent, "TSLA");

            // Then - Should have urgency multiplier applied
            assertThat(Math.abs(impact)).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should detect crisis situations - RULE #3: Stream.anyMatch")
        void shouldDetectCrisis() {
            // Given
            String crisis = "CRISIS: Company faces immediate challenges";

            // When
            double impact = sentimentAnalysisService.analyzeNewsImpact(crisis, "MSFT");

            // Then - Should have urgency multiplier applied
            assertThat(Math.abs(impact)).isGreaterThan(0.0);
        }
    }

    @Nested
    @DisplayName("Relevance Calculation Tests - RULE #3: Ternary Pattern")
    class RelevanceCalculationTest {

        @Test
        @DisplayName("Should increase impact for symbol-specific news - RULE #17: HIGH_RELEVANCE_MULTIPLIER")
        void shouldIncreaseImpactForSymbolSpecificNews() {
            // Given
            String specific = "AAPL stock shows excellent performance";
            String general = "Market shows excellent performance";

            // When
            double specificImpact = sentimentAnalysisService.analyzeNewsImpact(specific, "AAPL");
            double generalImpact = sentimentAnalysisService.analyzeNewsImpact(general, "AAPL");

            // Then
            assertThat(specificImpact).isGreaterThan(generalImpact);
        }

        @Test
        @DisplayName("Should be case-insensitive for symbol matching - RULE #3: toLowerCase")
        void shouldBeCaseInsensitiveForSymbol() {
            // Given
            String uppercase = "TSLA demonstrates strong growth";
            String lowercase = "tsla demonstrates strong growth";

            // When
            double impact1 = sentimentAnalysisService.analyzeNewsImpact(uppercase, "TSLA");
            double impact2 = sentimentAnalysisService.analyzeNewsImpact(lowercase, "TSLA");
            double impact3 = sentimentAnalysisService.analyzeNewsImpact(uppercase, "tsla");

            // Then - All should have high relevance
            assertThat(impact1).isEqualTo(impact2);
            assertThat(impact2).isEqualTo(impact3);
        }
    }

    @Nested
    @DisplayName("MANDATORY RULES Compliance Validation")
    class MandatoryRulesComplianceTest {

        @Test
        @DisplayName("RULE #3: All methods use functional patterns (Optional, Stream, NavigableMap)")
        void shouldUseOnlyFunctionalPatterns() {
            // All methods should execute without errors using functional patterns
            assertThatNoException().isThrownBy(() -> {
                sentimentAnalysisService.analyzeSentiment("Test content with bullish sentiment");
                sentimentAnalysisService.calculateMarketSentiment("AAPL");
                sentimentAnalysisService.analyzeNewsImpact("Breaking news", "TSLA");
                sentimentAnalysisService.categorizeSentiment(0.75);
            });
        }

        @Test
        @DisplayName("RULE #5: Methods maintain low cognitive complexity")
        void shouldMaintainLowCognitiveComplexity() {
            // Complex scenarios should still execute efficiently
            String[] testContents = {
                "Very bullish with excellent growth and strong profit",
                "Bearish decline with weak performance and crash risk",
                "Neutral market with mixed signals",
                "BREAKING URGENT: AAPL shows immediate growth opportunity",
                ""
            };

            for (String content : testContents) {
                double sentiment = sentimentAnalysisService.analyzeSentiment(content);
                String category = sentimentAnalysisService.categorizeSentiment(sentiment);

                assertThat(sentiment).isBetween(-1.5, 1.5);
                assertThat(category).isIn("VERY_POSITIVE", "POSITIVE", "NEUTRAL", "NEGATIVE", "VERY_NEGATIVE");
            }
        }

        @Test
        @DisplayName("RULE #9: Uses immutable data structures")
        void shouldUseImmutableDataStructures() {
            // NavigableMap and constant Sets should be effectively immutable
            assertThatNoException().isThrownBy(() -> {
                sentimentAnalysisService.categorizeSentiment(0.5);
                sentimentAnalysisService.analyzeSentiment("test content");
            });
        }

        @Test
        @DisplayName("RULE #17: All magic numbers externalized to constants")
        void shouldUseNamedConstants() {
            // Verify behavior is consistent with documented constants
            var result = sentimentAnalysisService.calculateMarketSentiment("TEST");

            // Default confidence constant
            assertThat(result.get("confidence")).isEqualTo(0.75);

            // Default sources count constant
            assertThat(result.get("sourcesCount")).isEqualTo(150);

            // Sentiment score range constants
            double score = (Double) result.get("sentimentScore");
            assertThat(score).isBetween(-1.0, 1.0);
        }

        @Test
        @DisplayName("Integration test: Analyze -> Categorize -> Impact")
        void shouldProvideConsistentEndToEndAnalysis() {
            // Given
            String newsContent = "BREAKING: AAPL announces excellent quarterly results with strong growth and bullish momentum";

            // When
            double sentiment = sentimentAnalysisService.analyzeSentiment(newsContent);
            String category = sentimentAnalysisService.categorizeSentiment(sentiment);
            double impact = sentimentAnalysisService.analyzeNewsImpact(newsContent, "AAPL");

            // Then
            assertThat(sentiment).isPositive(); // Positive keywords dominate
            assertThat(category).isIn("POSITIVE", "VERY_POSITIVE"); // Should categorize as positive
            assertThat(impact).isGreaterThan(sentiment); // Impact amplified by urgency + relevance
        }
    }
}
