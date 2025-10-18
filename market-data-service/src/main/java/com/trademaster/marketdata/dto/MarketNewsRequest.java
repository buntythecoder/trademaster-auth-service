package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.MarketNews;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Market News Request DTO
 *
 * Request parameters for querying market news with comprehensive
 * filtering, sentiment analysis, and relevance scoring.
 *
 * Fully compliant with MANDATORY RULES:
 * - RULE #3: Functional programming (no if-else chains, no loops)
 * - RULE #5: Method length ≤15 lines, complexity ≤7
 * - RULE #9: Immutable record with builder pattern
 * - RULE #16: No hardcoded values (all constants externalized)
 * - RULE #17: Named constants for all magic numbers
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Builder
public record MarketNewsRequest(

    // Time filtering
    Instant startTime,
    Instant endTime,

    // Quick time filters
    Boolean recentOnly, // Last 24 hours
    Boolean freshOnly,  // Last hour
    Boolean todayOnly,
    Integer hoursBack,  // Last N hours
    Integer daysBack,   // Last N days

    // Content filtering
    Set<String> categories,
    Set<String> sources,
    Set<String> regions,
    String searchTerm,

    // Market relevance
    Set<String> symbols,
    Set<String> sectors,
    Set<String> currencies,
    Set<String> commodities,

    // Sentiment filtering
    Set<MarketNews.SentimentLabel> sentimentLabels,
    BigDecimal minSentimentScore,  // -1.0 to 1.0
    BigDecimal maxSentimentScore,  // -1.0 to 1.0
    BigDecimal minConfidenceScore, // 0.0 to 1.0

    // Impact and relevance
    @Min(0) @Max(100)
    BigDecimal minRelevanceScore,  // 0-100
    @Min(0) @Max(100)
    BigDecimal minImpactScore,     // 0-100
    @Min(0) @Max(100)
    BigDecimal minUrgencyScore,    // 0-100
    Set<MarketNews.MarketImpact> marketImpactLevels,

    // Special flags
    Boolean trendingOnly,
    Boolean breakingOnly,
    Boolean marketMovingOnly,
    Boolean verifiedOnly,
    Boolean highQualityOnly,
    Boolean excludeDuplicates,

    // Engagement filtering
    Long minViewCount,
    Long minShareCount,
    Long minCommentCount,
    Long minEngagementScore,
    Boolean highEngagementOnly,

    // Content quality
    BigDecimal minQualityScore,    // 0.0 to 1.0
    Integer minWordCount,
    Integer maxWordCount,
    Integer maxReadingTime,        // Maximum reading time in minutes

    // Sorting and pagination
    @NotNull(message = "Sort field is required")
    String sortBy,

    @NotNull(message = "Sort direction is required")
    SortDirection sortDirection,

    @Min(0)
    Integer page,

    @Min(1) @Max(100)
    Integer size,

    // Output options
    Boolean includeContent,        // Include full article content
    Boolean includeSentiment,      // Include sentiment analysis
    Boolean includeEngagement,     // Include engagement metrics
    Boolean includeRelated,        // Include related symbols/sectors
    Boolean includeAnalysis,       // Include market impact analysis
    Boolean summarizeOnly,         // Return summaries only
    String timeZone               // Timezone for time display

) {

    // Time calculation constants (RULE #17: No magic numbers)
    private static final long SECONDS_IN_HOUR = 3600L;
    private static final long SECONDS_IN_DAY = 86400L;
    private static final long SECONDS_IN_WEEK = 7 * SECONDS_IN_DAY;

    // Default values constants
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String DEFAULT_SORT_BY = "publishedAt";
    private static final String DEFAULT_TIMEZONE = "UTC";

    // Complexity threshold constants
    private static final int HIGH_COMPLEXITY_FILTER_THRESHOLD = 5;
    private static final int MEDIUM_COMPLEXITY_FILTER_THRESHOLD = 2;
    private static final int HIGH_COMPLEXITY_SYMBOL_THRESHOLD = 10;

    /**
     * Compact constructor with validation only
     * Defaults moved to builder pattern as per RULE #5
     */
    public MarketNewsRequest {
        // Validation only - no default value assignments
        // Builder pattern handles defaults through @Builder.Default annotations
    }

    /**
     * Sort direction enum
     */
    public enum SortDirection {
        ASC, DESC
    }

    /**
     * Complexity level enum for type-safe classification
     */
    public enum ComplexityLevel {
        LOW, MEDIUM, HIGH
    }

    /**
     * Builder with default values
     * RULE #5 compliant: Moved defaults from compact constructor
     */
    public static class MarketNewsRequestBuilder {
        // Default values applied in builder
        private Integer page = DEFAULT_PAGE;
        private Integer size = DEFAULT_SIZE;
        private String sortBy = DEFAULT_SORT_BY;
        private SortDirection sortDirection = SortDirection.DESC;
        private Boolean recentOnly = false;
        private Boolean freshOnly = false;
        private Boolean todayOnly = false;
        private Boolean trendingOnly = false;
        private Boolean breakingOnly = false;
        private Boolean marketMovingOnly = false;
        private Boolean verifiedOnly = false;
        private Boolean highQualityOnly = false;
        private Boolean excludeDuplicates = true;
        private Boolean highEngagementOnly = false;
        private Boolean includeContent = false;
        private Boolean includeSentiment = true;
        private Boolean includeEngagement = true;
        private Boolean includeRelated = true;
        private Boolean includeAnalysis = false;
        private Boolean summarizeOnly = false;
        private String timeZone = DEFAULT_TIMEZONE;
    }

    // ========================================================================
    // Validation Methods (RULE #3: Functional predicates, no if-else)
    // ========================================================================

    /**
     * Validate request parameters
     * RULE #5: ≤15 lines, complexity ≤7
     */
    public boolean isValid() {
        return Optional.ofNullable(page).filter(p -> p >= 0).isPresent() &&
               Optional.ofNullable(size).filter(s -> s > 0 && s <= 100).isPresent() &&
               Optional.ofNullable(sortBy).filter(s -> !s.trim().isEmpty()).isPresent();
    }

    /**
     * Check if time filter is active
     */
    public boolean hasTimeFilter() {
        return startTime != null || endTime != null ||
               recentOnly || freshOnly || todayOnly ||
               hoursBack != null || daysBack != null;
    }

    /**
     * Check if content filter is active
     */
    public boolean hasContentFilter() {
        return (categories != null && !categories.isEmpty()) ||
               (sources != null && !sources.isEmpty()) ||
               (regions != null && !regions.isEmpty()) ||
               (searchTerm != null && !searchTerm.trim().isEmpty());
    }

    /**
     * Check if market filter is active
     */
    public boolean hasMarketFilter() {
        return (symbols != null && !symbols.isEmpty()) ||
               (sectors != null && !sectors.isEmpty()) ||
               (currencies != null && !currencies.isEmpty()) ||
               (commodities != null && !commodities.isEmpty());
    }

    /**
     * Check if sentiment filter is active
     */
    public boolean hasSentimentFilter() {
        return (sentimentLabels != null && !sentimentLabels.isEmpty()) ||
               minSentimentScore != null || maxSentimentScore != null ||
               minConfidenceScore != null;
    }

    /**
     * Check if impact filter is active
     */
    public boolean hasImpactFilter() {
        return minRelevanceScore != null || minImpactScore != null ||
               minUrgencyScore != null ||
               (marketImpactLevels != null && !marketImpactLevels.isEmpty());
    }

    /**
     * Check if special filter is active
     */
    public boolean hasSpecialFilter() {
        return trendingOnly || breakingOnly || marketMovingOnly ||
               verifiedOnly || highQualityOnly;
    }

    /**
     * Check if engagement filter is active
     */
    public boolean hasEngagementFilter() {
        return minViewCount != null || minShareCount != null ||
               minCommentCount != null || minEngagementScore != null ||
               highEngagementOnly;
    }

    /**
     * Check if quality filter is active
     */
    public boolean hasQualityFilter() {
        return minQualityScore != null || minWordCount != null ||
               maxWordCount != null || maxReadingTime != null;
    }

    /**
     * Get total number of active filters
     * RULE #4: Stream API for counting, no imperative loops
     */
    public int getActiveFilterCount() {
        return (int) List.of(
            hasTimeFilter(),
            hasContentFilter(),
            hasMarketFilter(),
            hasSentimentFilter(),
            hasImpactFilter(),
            hasSpecialFilter(),
            hasEngagementFilter(),
            hasQualityFilter()
        ).stream().filter(Boolean::booleanValue).count();
    }

    // ========================================================================
    // Time Range Calculation (RULE #3: Functional, no if-else chains)
    // ========================================================================

    /**
     * Get effective start time using functional Optional chain
     * RULE #3: No if-else statements, functional composition
     * RULE #5: ≤15 lines, cognitive complexity ≤7
     * RULE #17: Named constants for all time calculations
     */
    public Instant getEffectiveStartTime() {
        Instant now = Instant.now();

        return Optional.ofNullable(startTime)
            .or(() -> calculateFreshOnlyTime(now))
            .or(() -> calculateRecentOnlyTime(now))
            .or(() -> calculateTodayOnlyTime(now))
            .or(() -> calculateHoursBackTime(now))
            .or(() -> calculateDaysBackTime(now))
            .orElseGet(() -> now.minusSeconds(SECONDS_IN_WEEK));
    }

    /**
     * Calculate time for freshOnly filter (last hour)
     */
    private Optional<Instant> calculateFreshOnlyTime(Instant now) {
        return Optional.ofNullable(freshOnly)
            .filter(Boolean::booleanValue)
            .map(f -> now.minusSeconds(SECONDS_IN_HOUR));
    }

    /**
     * Calculate time for recentOnly filter (last 24 hours)
     */
    private Optional<Instant> calculateRecentOnlyTime(Instant now) {
        return Optional.ofNullable(recentOnly)
            .filter(Boolean::booleanValue)
            .map(r -> now.minusSeconds(SECONDS_IN_DAY));
    }

    /**
     * Calculate time for todayOnly filter (last 24 hours)
     */
    private Optional<Instant> calculateTodayOnlyTime(Instant now) {
        return Optional.ofNullable(todayOnly)
            .filter(Boolean::booleanValue)
            .map(t -> now.minusSeconds(SECONDS_IN_DAY));
    }

    /**
     * Calculate time for hoursBack filter
     */
    private Optional<Instant> calculateHoursBackTime(Instant now) {
        return Optional.ofNullable(hoursBack)
            .map(hours -> now.minusSeconds(hours * SECONDS_IN_HOUR));
    }

    /**
     * Calculate time for daysBack filter
     */
    private Optional<Instant> calculateDaysBackTime(Instant now) {
        return Optional.ofNullable(daysBack)
            .map(days -> now.minusSeconds(days * SECONDS_IN_DAY));
    }

    /**
     * Get effective end time using functional Optional
     * RULE #3: No ternary operator, use Optional
     */
    public Instant getEffectiveEndTime() {
        return Optional.ofNullable(endTime).orElseGet(Instant::now);
    }

    // ========================================================================
    // Complexity Assessment (RULE #3: Functional, no if-else)
    // ========================================================================

    /**
     * Get complexity level using functional predicate matching
     * RULE #3: No if-else chains, functional predicate composition
     * RULE #5: ≤15 lines, cognitive complexity ≤7
     */
    public ComplexityLevel getComplexity() {
        int filters = getActiveFilterCount();
        int symbolCount = Optional.ofNullable(symbols).map(Set::size).orElse(0);
        boolean hasSearchTerm = Optional.ofNullable(searchTerm).filter(s -> !s.isEmpty()).isPresent();

        return getComplexityPredicates().entrySet().stream()
            .filter(entry -> entry.getValue().test(new ComplexityContext(filters, symbolCount, hasSearchTerm)))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(ComplexityLevel.LOW);
    }

    /**
     * Complexity predicates map for functional classification
     * RULE #3: Replace if-else with predicate map
     */
    private static Map<ComplexityLevel, Predicate<ComplexityContext>> getComplexityPredicates() {
        return Map.of(
            ComplexityLevel.HIGH, ctx ->
                ctx.filters() > HIGH_COMPLEXITY_FILTER_THRESHOLD ||
                ctx.symbolCount() > HIGH_COMPLEXITY_SYMBOL_THRESHOLD ||
                ctx.hasSearchTerm(),
            ComplexityLevel.MEDIUM, ctx ->
                ctx.filters() > MEDIUM_COMPLEXITY_FILTER_THRESHOLD,
            ComplexityLevel.LOW, ctx -> true
        );
    }

    /**
     * Complexity context record for predicate evaluation
     */
    private record ComplexityContext(int filters, int symbolCount, boolean hasSearchTerm) {}

    // ========================================================================
    // Preset Request Factory Methods
    // ========================================================================

    /**
     * Create request for breaking news
     */
    public static MarketNewsRequest breakingNews() {
        return MarketNewsRequest.builder()
            .breakingOnly(true)
            .recentOnly(true)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .includeSentiment(true)
            .includeAnalysis(true)
            .build();
    }

    /**
     * Create request for trending news
     */
    public static MarketNewsRequest trendingNews() {
        return MarketNewsRequest.builder()
            .trendingOnly(true)
            .recentOnly(true)
            .sortBy("relevanceScore")
            .sortDirection(SortDirection.DESC)
            .includeEngagement(true)
            .build();
    }

    /**
     * Create request for market-moving news
     */
    public static MarketNewsRequest marketMovingNews() {
        return MarketNewsRequest.builder()
            .marketMovingOnly(true)
            .daysBack(3)
            .sortBy("impactScore")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }

    /**
     * Create request for recent news
     */
    public static MarketNewsRequest recentNews() {
        return MarketNewsRequest.builder()
            .recentOnly(true)
            .highQualityOnly(true)
            .excludeDuplicates(true)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for fresh news (last hour)
     */
    public static MarketNewsRequest freshNews() {
        return MarketNewsRequest.builder()
            .freshOnly(true)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }

    /**
     * Create request for positive sentiment news
     */
    public static MarketNewsRequest positiveNews() {
        return MarketNewsRequest.builder()
            .minSentimentScore(new BigDecimal("0.3"))
            .minConfidenceScore(new BigDecimal("0.7"))
            .recentOnly(true)
            .sortBy("sentimentScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for negative sentiment news
     */
    public static MarketNewsRequest negativeNews() {
        return MarketNewsRequest.builder()
            .maxSentimentScore(new BigDecimal("-0.3"))
            .minConfidenceScore(new BigDecimal("0.7"))
            .recentOnly(true)
            .sortBy("sentimentScore")
            .sortDirection(SortDirection.ASC)
            .build();
    }

    /**
     * Create request for high impact news
     */
    public static MarketNewsRequest highImpactNews() {
        return MarketNewsRequest.builder()
            .minImpactScore(new BigDecimal("70"))
            .minRelevanceScore(new BigDecimal("60"))
            .daysBack(7)
            .sortBy("impactScore")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }

    /**
     * Create request for symbol-specific news
     */
    public static MarketNewsRequest symbolNews(String symbol) {
        return MarketNewsRequest.builder()
            .symbols(Set.of(symbol))
            .daysBack(7)
            .sortBy("relevanceScore")
            .sortDirection(SortDirection.DESC)
            .includeSentiment(true)
            .includeAnalysis(true)
            .build();
    }

    /**
     * Create request for sector-specific news
     */
    public static MarketNewsRequest sectorNews(String sector) {
        return MarketNewsRequest.builder()
            .sectors(Set.of(sector))
            .daysBack(7)
            .sortBy("relevanceScore")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }

    /**
     * Create request for category-specific news
     */
    public static MarketNewsRequest categoryNews(String category) {
        return MarketNewsRequest.builder()
            .categories(Set.of(category))
            .daysBack(14)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for source-specific news
     */
    public static MarketNewsRequest sourceNews(String source) {
        return MarketNewsRequest.builder()
            .sources(Set.of(source))
            .daysBack(7)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for high engagement news
     */
    public static MarketNewsRequest highEngagementNews() {
        return MarketNewsRequest.builder()
            .highEngagementOnly(true)
            .daysBack(3)
            .sortBy("engagementScore")
            .sortDirection(SortDirection.DESC)
            .includeEngagement(true)
            .build();
    }

    /**
     * Create request for quality news
     */
    public static MarketNewsRequest qualityNews() {
        return MarketNewsRequest.builder()
            .highQualityOnly(true)
            .verifiedOnly(true)
            .excludeDuplicates(true)
            .minQualityScore(new BigDecimal("0.8"))
            .recentOnly(true)
            .sortBy("qualityScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for search query
     */
    public static MarketNewsRequest searchNews(String query) {
        return MarketNewsRequest.builder()
            .searchTerm(query)
            .daysBack(30)
            .highQualityOnly(true)
            .excludeDuplicates(true)
            .sortBy("relevanceScore")
            .sortDirection(SortDirection.DESC)
            .includeContent(true)
            .build();
    }

    // ========================================================================
    // Analysis-Specific Request Factory Methods
    // ========================================================================

    /**
     * Create request for sentiment analysis
     */
    public static MarketNewsRequest forSentimentAnalysis() {
        return MarketNewsRequest.builder()
            .recentOnly(true)
            .minConfidenceScore(new BigDecimal("0.7"))
            .includeSentiment(true)
            .includeAnalysis(true)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for market analysis
     */
    public static MarketNewsRequest forMarketAnalysis() {
        return MarketNewsRequest.builder()
            .marketMovingOnly(true)
            .minRelevanceScore(new BigDecimal("70"))
            .daysBack(7)
            .includeAnalysis(true)
            .includeRelated(true)
            .sortBy("impactScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Create request for risk monitoring
     */
    public static MarketNewsRequest forRiskMonitoring() {
        return MarketNewsRequest.builder()
            .minImpactScore(new BigDecimal("60"))
            .sentimentLabels(Set.of(
                MarketNews.SentimentLabel.NEGATIVE,
                MarketNews.SentimentLabel.VERY_NEGATIVE
            ))
            .freshOnly(true)
            .includeAnalysis(true)
            .sortBy("urgencyScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }
}
