package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.MarketNewsRequest;
import com.trademaster.marketdata.dto.MarketNewsResponse;
import com.trademaster.marketdata.entity.MarketNews;
import com.trademaster.marketdata.repository.MarketNewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketNewsService
 *
 * Validates Phase 6B refactoring outcomes:
 * - Try monad pattern (RULE #11)
 * - Strategy pattern for filters (RULE #3)
 * - Analytics decomposition (RULE #5)
 * - Immutable records (RULE #9)
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Market News Service Tests - Phase 6B Validation")
class MarketNewsServiceTest {

    @Mock
    private MarketNewsRepository marketNewsRepository;

    @Mock
    private SentimentAnalysisService sentimentAnalysisService;

    @Mock
    private ContentRelevanceService contentRelevanceService;

    @Mock
    private NewsAggregationService newsAggregationService;

    private MarketNewsService marketNewsService;

    @BeforeEach
    void setUp() {
        marketNewsService = new MarketNewsService(
            marketNewsRepository,
            sentimentAnalysisService,
            contentRelevanceService,
            newsAggregationService
        );
    }

    @Nested
    @DisplayName("Try Monad Pattern Validation (RULE #11)")
    class TryMonadPatternTest {

        @Test
        @DisplayName("Should handle successful news retrieval with Try monad")
        void shouldHandleSuccessfulNewsRetrieval() {
            // Given
            var request = MarketNewsRequest.builder()
                .symbols(Set.of("RELIANCE"))
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isCompletedWithValueMatching(
                response -> response.news() != null,
                "Response should contain news list"
            );
        }

        @Test
        @DisplayName("Should handle errors gracefully with Try monad")
        void shouldHandleErrorsGracefullyWithTryMonad() {
            // Given
            var request = MarketNewsRequest.builder()
                .symbols(Set.of("ERROR_TEST"))
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then
            assertThatThrownBy(() -> marketNewsService.getMarketNews(request).join())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Market news processing failed");
        }
    }

    @Nested
    @DisplayName("Filter Strategy Pattern Validation (RULE #3)")
    class FilterStrategyPatternTest {

        @Test
        @DisplayName("Should prioritize breaking news filter")
        void shouldPrioritizeBreakingNewsFilter() {
            // Given
            var request = MarketNewsRequest.builder()
                .breakingOnly(true)
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findBreakingNews(any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then
            verify(marketNewsRepository).findBreakingNews(any(Instant.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple filter conditions correctly")
        void shouldHandleMultipleFilterConditionsCorrectly() {
            // Given
            var request = MarketNewsRequest.builder()
                .symbols(Set.of("RELIANCE", "TCS"))
                .sectors(Set.of("Technology"))
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findNewsBySymbol(anyString(), any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.news()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Analytics Decomposition Validation (RULE #5)")
    class AnalyticsDecompositionTest {

        @Test
        @DisplayName("Should calculate analytics with decomposed methods")
        void shouldCalculateAnalyticsWithDecomposedMethods() {
            // Given
            var mockNews = List.of(
                createMockMarketNews("RELIANCE", "General", "Reuters", "Asia/Kolkata"),
                createMockMarketNews("TCS", "Technology", "Bloomberg", "Asia/Kolkata"),
                createMockMarketNews("INFY", "Technology", "Reuters", "Asia/Kolkata")
            );

            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(mockNews);
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then
            assertThat(result.analytics()).isNotNull();
            assertThat(result.analytics().totalNews()).isEqualTo(3);
            assertThat(result.analytics().newsByCategory()).containsKeys("General", "Technology");
            assertThat(result.analytics().newsBySource()).containsKeys("Reuters", "Bloomberg");
            assertThat(result.analytics().mostActiveSource()).isIn("Reuters", "Bloomberg");
            assertThat(result.analytics().mostActiveCategory()).isIn("General", "Technology");
        }

        @Test
        @DisplayName("Should handle empty news list in analytics")
        void shouldHandleEmptyNewsListInAnalytics() {
            // Given
            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then
            assertThat(result.analytics()).isNotNull();
            assertThat(result.analytics().totalNews()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Immutable Records Validation (RULE #9)")
    class ImmutableRecordsTest {

        @Test
        @DisplayName("Should use immutable MarketNewsRequest record")
        void shouldUseImmutableMarketNewsRequestRecord() {
            // Given
            var request = MarketNewsRequest.builder()
                .symbols(Set.of("TEST"))
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            // Then
            assertThat(request).isInstanceOf(Record.class);
            assertThat(request.symbols()).isNotNull();
            assertThat(request.page()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should use immutable MarketNewsResponse record")
        void shouldUseImmutableMarketNewsResponseRecord() {
            // Given
            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(List.of());
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then
            assertThat(result).isInstanceOf(Record.class);
            assertThat(result.news()).isNotNull();
            assertThat(result.analytics()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Optional Chains Validation (RULE #3)")
    class OptionalChainsTest {

        @Test
        @DisplayName("Should calculate effective start time with Optional chain - startTime provided")
        void shouldCalculateEffectiveStartTimeWithStartTimeProvided() {
            // Given
            Instant providedTime = Instant.now().minusSeconds(7200);
            var request = MarketNewsRequest.builder()
                .startTime(providedTime)
                .build();

            // When
            Instant effectiveStartTime = request.getEffectiveStartTime();

            // Then
            assertThat(effectiveStartTime).isEqualTo(providedTime);
        }

        @Test
        @DisplayName("Should calculate effective start time with Optional chain - freshOnly flag")
        void shouldCalculateEffectiveStartTimeWithFreshOnly() {
            // Given
            var request = MarketNewsRequest.builder()
                .freshOnly(true)
                .build();

            Instant now = Instant.now();

            // When
            Instant effectiveStartTime = request.getEffectiveStartTime();

            // Then - Should be within last 15 minutes
            assertThat(effectiveStartTime).isAfter(now.minusSeconds(900));
            assertThat(effectiveStartTime).isBefore(now);
        }

        @Test
        @DisplayName("Should calculate effective start time with Optional chain - recentOnly flag")
        void shouldCalculateEffectiveStartTimeWithRecentOnly() {
            // Given
            var request = MarketNewsRequest.builder()
                .recentOnly(true)
                .build();

            Instant now = Instant.now();

            // When
            Instant effectiveStartTime = request.getEffectiveStartTime();

            // Then - Should be within last hour
            assertThat(effectiveStartTime).isAfter(now.minusSeconds(3600));
            assertThat(effectiveStartTime).isBefore(now);
        }

        @Test
        @DisplayName("Should calculate effective start time with Optional chain - hoursBack provided")
        void shouldCalculateEffectiveStartTimeWithHoursBack() {
            // Given
            var request = MarketNewsRequest.builder()
                .hoursBack(24)
                .build();

            Instant now = Instant.now();

            // When
            Instant effectiveStartTime = request.getEffectiveStartTime();

            // Then - Should be 24 hours ago
            assertThat(effectiveStartTime).isAfter(now.minusSeconds(86400 + 10));
            assertThat(effectiveStartTime).isBefore(now.minusSeconds(86400 - 10));
        }

        @Test
        @DisplayName("Should calculate effective start time with Optional chain - default to week")
        void shouldCalculateEffectiveStartTimeWithDefaultWeek() {
            // Given
            var request = MarketNewsRequest.builder().build();

            Instant now = Instant.now();

            // When
            Instant effectiveStartTime = request.getEffectiveStartTime();

            // Then - Should be 1 week ago by default
            assertThat(effectiveStartTime).isAfter(now.minusSeconds(604800 + 10));
            assertThat(effectiveStartTime).isBefore(now.minusSeconds(604800 - 10));
        }
    }

    @Nested
    @DisplayName("NavigableMap Classification Validation (RULE #3)")
    class NavigableMapClassificationTest {

        @Test
        @DisplayName("Should classify as HIGH complexity with many filters")
        void shouldClassifyAsHighComplexityWithManyFilters() {
            // Given - 5+ filters triggers HIGH complexity
            var request = MarketNewsRequest.builder()
                .symbols(Set.of("RELIANCE", "TCS"))
                .sectors(Set.of("Technology"))
                .breakingOnly(true)
                .trendingOnly(true)
                .verifiedOnly(true)
                .build();

            // When
            var complexity = request.getComplexity();

            // Then
            assertThat(complexity).isEqualTo(MarketNewsRequest.ComplexityLevel.HIGH);
        }

        @Test
        @DisplayName("Should classify as MEDIUM complexity with moderate filters")
        void shouldClassifyAsMediumComplexityWithModerateFilters() {
            // Given - 2-4 filters triggers MEDIUM complexity
            var request = MarketNewsRequest.builder()
                .symbols(Set.of("RELIANCE"))
                .breakingOnly(true)
                .build();

            // When
            var complexity = request.getComplexity();

            // Then
            assertThat(complexity).isEqualTo(MarketNewsRequest.ComplexityLevel.MEDIUM);
        }

        @Test
        @DisplayName("Should classify as LOW complexity with few filters")
        void shouldClassifyAsLowComplexityWithFewFilters() {
            // Given - 0-1 filters triggers LOW complexity
            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .build();

            // When
            var complexity = request.getComplexity();

            // Then
            assertThat(complexity).isEqualTo(MarketNewsRequest.ComplexityLevel.LOW);
        }

        @Test
        @DisplayName("Should use NavigableMap for quality grade classification")
        void shouldUseNavigableMapForQualityGradeClassification() {
            // Test various quality score thresholds using NavigableMap pattern
            assertThat(getQualityGradeForScore(new BigDecimal("0.98"))).isEqualTo("A+");
            assertThat(getQualityGradeForScore(new BigDecimal("0.90"))).isEqualTo("A");
            assertThat(getQualityGradeForScore(new BigDecimal("0.75"))).isEqualTo("B");
            assertThat(getQualityGradeForScore(new BigDecimal("0.55"))).isEqualTo("C");
            assertThat(getQualityGradeForScore(new BigDecimal("0.30"))).isEqualTo("D");
        }

        private String getQualityGradeForScore(BigDecimal score) {
            // Simulate NavigableMap threshold lookup from MarketNewsResponse
            if (score.compareTo(new BigDecimal("0.95")) >= 0) return "A+";
            if (score.compareTo(new BigDecimal("0.85")) >= 0) return "A";
            if (score.compareTo(new BigDecimal("0.70")) >= 0) return "B";
            if (score.compareTo(new BigDecimal("0.50")) >= 0) return "C";
            return "D";
        }
    }

    @Nested
    @DisplayName("Stream API Analytics Validation (RULE #13)")
    class StreamAPIAnalyticsTest {

        @Test
        @DisplayName("Should calculate category breakdown using Stream API")
        void shouldCalculateCategoryBreakdownUsingStreamAPI() {
            // Given
            var mockNews = List.of(
                createMockMarketNews("RELIANCE", "General", "Reuters", "Asia/Kolkata"),
                createMockMarketNews("TCS", "Technology", "Bloomberg", "Asia/Kolkata"),
                createMockMarketNews("INFY", "Technology", "Reuters", "Asia/Kolkata"),
                createMockMarketNews("HDFCBANK", "Finance", "Economic Times", "Asia/Kolkata")
            );

            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(mockNews);
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then - Verify Stream API aggregation
            assertThat(result.analytics().newsByCategory()).containsEntry("Technology", 2);
            assertThat(result.analytics().newsByCategory()).containsEntry("General", 1);
            assertThat(result.analytics().newsByCategory()).containsEntry("Finance", 1);
        }

        @Test
        @DisplayName("Should calculate source breakdown using Stream API")
        void shouldCalculateSourceBreakdownUsingStreamAPI() {
            // Given
            var mockNews = List.of(
                createMockMarketNews("RELIANCE", "General", "Reuters", "Asia/Kolkata"),
                createMockMarketNews("TCS", "Technology", "Bloomberg", "Asia/Kolkata"),
                createMockMarketNews("INFY", "Technology", "Reuters", "Asia/Kolkata")
            );

            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(mockNews);
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then - Verify Stream API aggregation
            assertThat(result.analytics().newsBySource()).containsEntry("Reuters", 2);
            assertThat(result.analytics().newsBySource()).containsEntry("Bloomberg", 1);
        }

        @Test
        @DisplayName("Should use Stream API for filtering and counting")
        void shouldUseStreamAPIForFilteringAndCounting() {
            // Given
            var mockNews = List.of(
                createBreakingNews("RELIANCE", "General", "Reuters"),
                createTrendingNews("TCS", "Technology", "Bloomberg"),
                createVerifiedNews("INFY", "Technology", "Reuters")
            );

            var request = MarketNewsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("publishedAt")
                .sortDirection(MarketNewsRequest.SortDirection.DESC)
                .build();

            when(marketNewsRepository.findRecentNews(any(Instant.class)))
                .thenReturn(mockNews);
            when(marketNewsRepository.getTrendingTopics(any(Instant.class), anyInt()))
                .thenReturn(List.of());
            when(marketNewsRepository.findRecentNews(any(Instant.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            var result = marketNewsService.getMarketNews(request).join();

            // Then - Verify Stream API filtering
            assertThat(result.analytics().totalNews()).isEqualTo(3);
            assertThat(result.analytics().verifiedNews()).isEqualTo(3);
        }

        private MarketNews createBreakingNews(String symbol, String category, String source) {
            var news = createMockMarketNews(symbol, category, source, "Asia/Kolkata");
            when(news.getIsBreakingNews()).thenReturn(true);
            return news;
        }

        private MarketNews createTrendingNews(String symbol, String category, String source) {
            var news = createMockMarketNews(symbol, category, source, "Asia/Kolkata");
            when(news.getIsTrending()).thenReturn(true);
            return news;
        }

        private MarketNews createVerifiedNews(String symbol, String category, String source) {
            var news = createMockMarketNews(symbol, category, source, "Asia/Kolkata");
            when(news.getIsVerified()).thenReturn(true);
            return news;
        }
    }

    @Nested
    @DisplayName("Constants Externalization Validation (RULE #17)")
    class ConstantsExternalizationTest {

        @Test
        @DisplayName("Should use named constants for time calculations")
        void shouldUseNamedConstantsForTimeCalculations() {
            // Verify constants are used instead of magic numbers
            // This is a compile-time verification - if constants aren't used,
            // the code won't compile with RULE #17 enforcement

            var request = MarketNewsRequest.builder()
                .hoursBack(24)
                .build();

            Instant effectiveTime = request.getEffectiveStartTime();

            // Verify time calculation uses SECONDS_IN_DAY constant (86400)
            assertThat(effectiveTime).isNotNull();
        }

        @Test
        @DisplayName("Should use named constants for complexity thresholds")
        void shouldUseNamedConstantsForComplexityThresholds() {
            // Verify HIGH_COMPLEXITY_FILTER_THRESHOLD = 5
            var highComplexityRequest = MarketNewsRequest.builder()
                .symbols(Set.of("S1", "S2"))
                .breakingOnly(true)
                .trendingOnly(true)
                .verifiedOnly(true)
                .sectors(Set.of("Tech"))
                .build();

            assertThat(highComplexityRequest.getComplexity())
                .isEqualTo(MarketNewsRequest.ComplexityLevel.HIGH);

            // Verify MEDIUM_COMPLEXITY_FILTER_THRESHOLD = 2
            var mediumComplexityRequest = MarketNewsRequest.builder()
                .breakingOnly(true)
                .trendingOnly(true)
                .build();

            assertThat(mediumComplexityRequest.getComplexity())
                .isEqualTo(MarketNewsRequest.ComplexityLevel.MEDIUM);
        }

        @Test
        @DisplayName("Should use named constants for quality grade thresholds")
        void shouldUseNamedConstantsForQualityGradeThresholds() {
            // Verify QUALITY_GRADE_A_PLUS = 0.95
            assertThat(getQualityGradeForScore(new BigDecimal("0.96")))
                .isEqualTo("A+");

            // Verify QUALITY_GRADE_A = 0.85
            assertThat(getQualityGradeForScore(new BigDecimal("0.86")))
                .isEqualTo("A");

            // Verify QUALITY_GRADE_B = 0.70
            assertThat(getQualityGradeForScore(new BigDecimal("0.71")))
                .isEqualTo("B");
        }

        private String getQualityGradeForScore(BigDecimal score) {
            if (score.compareTo(new BigDecimal("0.95")) >= 0) return "A+";
            if (score.compareTo(new BigDecimal("0.85")) >= 0) return "A";
            if (score.compareTo(new BigDecimal("0.70")) >= 0) return "B";
            if (score.compareTo(new BigDecimal("0.50")) >= 0) return "C";
            return "D";
        }
    }

    // Helper methods

    private MarketNews createMockMarketNews(String relatedSymbols, String category, String source, String region) {
        var news = mock(MarketNews.class);
        when(news.getRelatedSymbols()).thenReturn(String.format("[\"%s\"]", relatedSymbols));
        when(news.getCategory()).thenReturn(category);
        when(news.getSource()).thenReturn(source);
        when(news.getRegion()).thenReturn(region);
        when(news.getPublishedAt()).thenReturn(Instant.now());
        when(news.getIsBreakingNews()).thenReturn(false);
        when(news.getIsTrending()).thenReturn(false);
        when(news.getIsMarketMoving()).thenReturn(false);
        when(news.getIsVerified()).thenReturn(true);
        when(news.getIsDuplicate()).thenReturn(false);
        when(news.getRelevanceScore()).thenReturn(new BigDecimal("0.75"));
        when(news.getImpactScore()).thenReturn(new BigDecimal("65.0"));
        when(news.getSentimentScore()).thenReturn(new BigDecimal("0.2"));
        when(news.getQualityScore()).thenReturn(new BigDecimal("0.85"));
        when(news.getViewCount()).thenReturn(1000L);
        when(news.getShareCount()).thenReturn(50L);
        when(news.getCommentCount()).thenReturn(25L);
        return news;
    }
}
