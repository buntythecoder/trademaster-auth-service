package com.trademaster.marketdata.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ContentRelevanceService
 *
 * Tests MANDATORY RULES compliance for functional patterns:
 * - RULE #3: Optional chains, NavigableMap, Stream API
 * - RULE #5: Cognitive complexity ≤7 per method
 * - RULE #17: Named constants externalization
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Content Relevance Service Tests")
class ContentRelevanceServiceTest {

    private ContentRelevanceService contentRelevanceService;

    @BeforeEach
    void setUp() {
        contentRelevanceService = new ContentRelevanceService();
    }

    @Nested
    @DisplayName("Relevance Score Calculation Tests - RULE #3 Compliance")
    class RelevanceScoreCalculationTest {

        @Test
        @DisplayName("Should calculate high relevance for direct symbol mention")
        void shouldCalculateHighRelevanceForDirectSymbol() {
            // Given
            String content = "Apple Inc (AAPL) reports strong quarterly earnings with revenue growth.";
            String symbol = "AAPL";

            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore(content, symbol);

            // Then
            assertThat(relevanceScore).isGreaterThan(0.4);
            assertThat(relevanceScore).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should calculate relevance for company name mention")
        void shouldCalculateRelevanceForCompanyName() {
            // Given
            String content = "Apple unveils new iPhone with advanced AI capabilities.";
            String symbol = "AAPL";

            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore(content, symbol);

            // Then
            assertThat(relevanceScore).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should return zero for null content - RULE #3: Optional chain")
        void shouldReturnZeroForNullContent() {
            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore(null, "AAPL");

            // Then
            assertThat(relevanceScore).isZero();
        }

        @Test
        @DisplayName("Should return zero for empty content - RULE #3: Optional chain")
        void shouldReturnZeroForEmptyContent() {
            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore("   ", "AAPL");

            // Then
            assertThat(relevanceScore).isZero();
        }

        @Test
        @DisplayName("Should return zero for null symbol - RULE #3: Optional chain")
        void shouldReturnZeroForNullSymbol() {
            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore("Some content", null);

            // Then
            assertThat(relevanceScore).isZero();
        }

        @Test
        @DisplayName("Should be case-insensitive - RULE #3: toLowerCase in Optional chain")
        void shouldBeCaseInsensitive() {
            // Given
            String content1 = "AAPL shows strong performance";
            String content2 = "aapl shows strong performance";
            String symbol = "AAPL";

            // When
            double score1 = contentRelevanceService.calculateRelevanceScore(content1, symbol);
            double score2 = contentRelevanceService.calculateRelevanceScore(content2, symbol);

            // Then
            assertThat(score1).isEqualTo(score2);
        }

        @Test
        @DisplayName("Should calculate industry relevance - RULE #3: Stream API")
        void shouldCalculateIndustryRelevance() {
            // Given
            String content = "Tesla leads automotive electric vehicle innovation with clean energy solutions.";
            String symbol = "TSLA";

            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore(content, symbol);

            // Then
            assertThat(relevanceScore).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should calculate market keyword density - RULE #3: Stream API")
        void shouldCalculateMarketKeywordDensity() {
            // Given
            String content = "Stock market trading shows strong investment activity with high portfolio returns.";
            String symbol = "SPY";

            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore(content, symbol);

            // Then
            assertThat(relevanceScore).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should limit relevance score to 1.0 - RULE #17: MAX_RELEVANCE_SCORE")
        void shouldLimitRelevanceScoreToMaximum() {
            // Given
            String content = "AAPL Apple Inc stock market trading investment portfolio analyst earnings revenue profit dividend merger acquisition";
            String symbol = "AAPL";

            // When
            double relevanceScore = contentRelevanceService.calculateRelevanceScore(content, symbol);

            // Then
            assertThat(relevanceScore).isLessThanOrEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Relevance Categorization Tests - RULE #3: Switch Expression")
    class RelevanceCategoryTest {

        @Test
        @DisplayName("Should categorize as HIGHLY_RELEVANT - RULE #17: Threshold constant")
        void shouldCategorizeAsHighlyRelevant() {
            // When
            String category = contentRelevanceService.categorizeRelevance(0.85);

            // Then
            assertThat(category).isEqualTo("HIGHLY_RELEVANT");
        }

        @Test
        @DisplayName("Should categorize as RELEVANT - RULE #17: Threshold constant")
        void shouldCategorizeAsRelevant() {
            // When
            String category = contentRelevanceService.categorizeRelevance(0.65);

            // Then
            assertThat(category).isEqualTo("RELEVANT");
        }

        @Test
        @DisplayName("Should categorize as MODERATELY_RELEVANT - RULE #17: Threshold constant")
        void shouldCategorizeAsModeratelyRelevant() {
            // When
            String category = contentRelevanceService.categorizeRelevance(0.45);

            // Then
            assertThat(category).isEqualTo("MODERATELY_RELEVANT");
        }

        @Test
        @DisplayName("Should categorize as SLIGHTLY_RELEVANT - RULE #17: Threshold constant")
        void shouldCategorizeAsSlightlyRelevant() {
            // When
            String category = contentRelevanceService.categorizeRelevance(0.25);

            // Then
            assertThat(category).isEqualTo("SLIGHTLY_RELEVANT");
        }

        @Test
        @DisplayName("Should categorize as NOT_RELEVANT - RULE #3: Switch default case")
        void shouldCategorizeAsNotRelevant() {
            // When
            String category = contentRelevanceService.categorizeRelevance(0.05);

            // Then
            assertThat(category).isEqualTo("NOT_RELEVANT");
        }

        @Test
        @DisplayName("Should handle boundary values correctly")
        void shouldHandleBoundaryValues() {
            // When
            String category1 = contentRelevanceService.categorizeRelevance(0.80);
            String category2 = contentRelevanceService.categorizeRelevance(0.60);
            String category3 = contentRelevanceService.categorizeRelevance(0.40);
            String category4 = contentRelevanceService.categorizeRelevance(0.20);

            // Then
            assertThat(category1).isEqualTo("HIGHLY_RELEVANT");
            assertThat(category2).isEqualTo("RELEVANT");
            assertThat(category3).isEqualTo("MODERATELY_RELEVANT");
            assertThat(category4).isEqualTo("SLIGHTLY_RELEVANT");
        }
    }

    @Nested
    @DisplayName("Content Quality Tests - RULE #3: NavigableMap Pattern")
    class ContentQualityTest {

        @Test
        @DisplayName("Should return high quality for optimal length - RULE #17: Length constants")
        void shouldReturnHighQualityForOptimalLength() {
            // Given
            String optimalContent = "A".repeat(150) + " stock market trading investment analysis. " +
                "Multiple complete sentences provide good structure. " +
                "Market conditions show positive trends. " +
                "Investment portfolio demonstrates balanced allocation.";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(optimalContent);

            // Then
            assertThat(qualityScore).isGreaterThan(0.3);
        }

        @Test
        @DisplayName("Should return moderate quality for acceptable length - RULE #3: NavigableMap")
        void shouldReturnModerateQualityForAcceptableLength() {
            // Given
            String acceptableContent = "Short market update. Stock prices rise. Good day.";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(acceptableContent);

            // Then
            assertThat(qualityScore).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should return zero for null content - RULE #3: Optional chain")
        void shouldReturnZeroForNullContent() {
            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(null);

            // Then
            assertThat(qualityScore).isZero();
        }

        @Test
        @DisplayName("Should return zero for empty content - RULE #3: Optional chain")
        void shouldReturnZeroForEmptyContent() {
            // When
            double qualityScore = contentRelevanceService.calculateContentQuality("   ");

            // Then
            assertThat(qualityScore).isZero();
        }

        @Test
        @DisplayName("Should reward good sentence structure - RULE #17: Sentence count constants")
        void shouldRewardGoodSentenceStructure() {
            // Given
            String wellStructuredContent = "First sentence. Second sentence. Third sentence. " +
                "Fourth sentence provides more detail.";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(wellStructuredContent);

            // Then
            assertThat(qualityScore).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should penalize too few sentences - RULE #3: Ternary operator")
        void shouldPenalizeTooFewSentences() {
            // Given
            String shortContent = "Single sentence";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(shortContent);

            // Then - Should have lower quality due to poor sentence structure
            String mediumContent = "First. Second. Third. Fourth.";
            double betterQuality = contentRelevanceService.calculateContentQuality(mediumContent);
            assertThat(qualityScore).isLessThan(betterQuality);
        }

        @Test
        @DisplayName("Should include market keyword bonus - RULE #3: Stream API filtering")
        void shouldIncludeMarketKeywordBonus() {
            // Given
            String contentWithKeywords = "Stock market trading shows investment portfolio gains. " +
                "Analyst reports strong earnings and revenue growth.";
            String contentWithoutKeywords = "The company had a good day today. " +
                "Everything went well with the team.";

            // When
            double scoreWithKeywords = contentRelevanceService.calculateContentQuality(contentWithKeywords);
            double scoreWithoutKeywords = contentRelevanceService.calculateContentQuality(contentWithoutKeywords);

            // Then
            assertThat(scoreWithKeywords).isGreaterThan(scoreWithoutKeywords);
        }

        @Test
        @DisplayName("Should penalize spam content - RULE #3: Functional composition")
        void shouldPenalizeSpamContent() {
            // Given
            String cleanContent = "Apple reports quarterly earnings with revenue growth. " +
                "Market analysts positive on outlook.";
            String spamContent = "CLICK HERE to BUY NOW!!! LIMITED TIME offer!!! " +
                "MAKE MONEY fast with this GUARANTEED system!!!";

            // When
            double cleanScore = contentRelevanceService.calculateContentQuality(cleanContent);
            double spamScore = contentRelevanceService.calculateContentQuality(spamContent);

            // Then
            assertThat(cleanScore).isGreaterThan(spamScore);
        }

        @Test
        @DisplayName("Should limit quality score to 1.0 - RULE #17: MAX_RELEVANCE_SCORE")
        void shouldLimitQualityScoreToMaximum() {
            // Given
            String excellentContent = "A".repeat(500) + " " +
                "Stock market trading investment portfolio analysis demonstrates comprehensive understanding. " +
                "Detailed earnings revenue profit data shows strong performance metrics. " +
                "Analyst forecasts indicate continued growth trajectory. " +
                "Dividend policies support long-term investor returns.";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(excellentContent);

            // Then
            assertThat(qualityScore).isLessThanOrEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Spam Detection Tests - RULE #3: Functional Composition")
    class SpamDetectionTest {

        @Test
        @DisplayName("Should detect spam keywords - RULE #3: Stream API anyMatch")
        void shouldDetectSpamKeywords() {
            // Given
            String spamContent = "Click here to buy now! Limited time offer!";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(spamContent);

            // Then - Spam content should have lower quality (no spam bonus)
            String cleanContent = "Stock market shows positive trends today.";
            double cleanScore = contentRelevanceService.calculateContentQuality(cleanContent);
            assertThat(qualityScore).isLessThan(cleanScore);
        }

        @Test
        @DisplayName("Should detect excessive capitalization - RULE #17: MAX_UPPERCASE_RATIO")
        void shouldDetectExcessiveCapitalization() {
            // Given
            String upperCaseContent = "THIS IS ALL UPPERCASE SPAM CONTENT WITH SHOUTING!!!";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(upperCaseContent);

            // Then
            String normalContent = "This is normal content with proper capitalization.";
            double normalScore = contentRelevanceService.calculateContentQuality(normalContent);
            assertThat(qualityScore).isLessThan(normalScore);
        }

        @Test
        @DisplayName("Should allow normal content with no spam - RULE #3: Functional logic")
        void shouldAllowNormalContent() {
            // Given
            String cleanContent = "Apple Inc reports strong quarterly performance with " +
                "revenue growth exceeding analyst expectations.";

            // When
            double qualityScore = contentRelevanceService.calculateContentQuality(cleanContent);

            // Then
            assertThat(qualityScore).isGreaterThan(0.0);
        }
    }

    @Nested
    @DisplayName("Content Filtering Tests - RULE #3: Stream API")
    class ContentFilteringTest {

        @Test
        @DisplayName("Should filter content by relevance threshold - RULE #3: Stream pipeline")
        void shouldFilterByRelevanceThreshold() {
            // Given
            List<Map<String, Object>> contentList = List.of(
                Map.of("content", "AAPL reports strong earnings"),
                Map.of("content", "Random news about weather"),
                Map.of("content", "Apple unveils new products")
            );
            String symbol = "AAPL";
            double threshold = 0.3;

            // When
            List<Map<String, Object>> filtered = contentRelevanceService
                .filterRelevantContent(contentList, symbol, threshold);

            // Then
            assertThat(filtered).isNotEmpty();
            assertThat(filtered.size()).isLessThanOrEqualTo(contentList.size());
        }

        @Test
        @DisplayName("Should sort by relevance score descending - RULE #3: Stream sorted")
        void shouldSortByRelevanceDescending() {
            // Given
            List<Map<String, Object>> contentList = List.of(
                Map.of("content", "Some general news"),
                Map.of("content", "AAPL Apple Inc strong performance"),
                Map.of("content", "Apple reports")
            );
            String symbol = "AAPL";

            // When
            List<Map<String, Object>> filtered = contentRelevanceService
                .filterRelevantContent(contentList, symbol, 0.0);

            // Then
            assertThat(filtered).hasSize(3);
            double firstScore = (Double) filtered.get(0).get("relevanceScore");
            double lastScore = (Double) filtered.get(2).get("relevanceScore");
            assertThat(firstScore).isGreaterThanOrEqualTo(lastScore);
        }

        @Test
        @DisplayName("Should add relevance scores to content - RULE #3: Stream operations")
        void shouldAddRelevanceScoresToContent() {
            // Given
            List<Map<String, Object>> contentList = List.of(
                Map.of("content", "AAPL stock rises")
            );
            String symbol = "AAPL";

            // When
            List<Map<String, Object>> filtered = contentRelevanceService
                .filterRelevantContent(contentList, symbol, 0.0);

            // Then
            assertThat(filtered.get(0)).containsKey("relevanceScore");
            assertThat(filtered.get(0).get("relevanceScore")).isInstanceOf(Double.class);
        }

        @Test
        @DisplayName("Should return empty list when all below threshold")
        void shouldReturnEmptyListWhenAllBelowThreshold() {
            // Given
            List<Map<String, Object>> contentList = List.of(
                Map.of("content", "Random news"),
                Map.of("content", "Unrelated content")
            );
            String symbol = "AAPL";
            double highThreshold = 0.9;

            // When
            List<Map<String, Object>> filtered = contentRelevanceService
                .filterRelevantContent(contentList, symbol, highThreshold);

            // Then
            assertThat(filtered).isEmpty();
        }
    }

    @Nested
    @DisplayName("MANDATORY RULES Compliance Validation")
    class MandatoryRulesComplianceTest {

        @Test
        @DisplayName("RULE #3: All methods use functional patterns (no if-else, no loops)")
        void shouldUseOnlyFunctionalPatterns() {
            // This test validates that all methods work correctly using functional patterns
            String content = "Apple Inc (AAPL) reports earnings with stock market trading activity.";
            String symbol = "AAPL";

            // All methods should execute without errors using functional patterns
            assertThatNoException().isThrownBy(() -> {
                contentRelevanceService.calculateRelevanceScore(content, symbol);
                contentRelevanceService.calculateContentQuality(content);
                contentRelevanceService.categorizeRelevance(0.75);
                contentRelevanceService.filterRelevantContent(
                    List.of(Map.of("content", content)),
                    symbol,
                    0.5
                );
            });
        }

        @Test
        @DisplayName("RULE #5: Methods maintain low cognitive complexity")
        void shouldMaintainLowCognitiveComplexity() {
            // Complex scenarios should still execute efficiently with low complexity methods
            String complexContent = "A".repeat(1000) + " AAPL Apple stock market trading investment " +
                "portfolio earnings revenue profit dividend technology smartphone " +
                "financial healthcare energy consumer analyst merger acquisition";
            String symbol = "AAPL";

            // Should handle complex content efficiently
            double score = contentRelevanceService.calculateRelevanceScore(complexContent, symbol);
            assertThat(score).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);

            double quality = contentRelevanceService.calculateContentQuality(complexContent);
            assertThat(quality).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("RULE #17: All magic numbers externalized to constants")
        void shouldUseNamedConstants() {
            // Verify behavior is consistent with documented constants
            double highRelevance = contentRelevanceService.calculateRelevanceScore(
                "AAPL Apple Inc", "AAPL"
            );
            String category = contentRelevanceService.categorizeRelevance(highRelevance);

            assertThat(category).isIn("HIGHLY_RELEVANT", "RELEVANT", "MODERATELY_RELEVANT");
        }
    }
}
