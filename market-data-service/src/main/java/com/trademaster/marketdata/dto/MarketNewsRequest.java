package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.MarketNews;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Market News Request DTO
 * 
 * Request parameters for querying market news with comprehensive
 * filtering, sentiment analysis, and relevance scoring.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
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
    
    public MarketNewsRequest {
        // Set defaults
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "publishedAt";
        if (sortDirection == null) sortDirection = SortDirection.DESC;
        if (recentOnly == null) recentOnly = false;
        if (freshOnly == null) freshOnly = false;
        if (todayOnly == null) todayOnly = false;
        if (trendingOnly == null) trendingOnly = false;
        if (breakingOnly == null) breakingOnly = false;
        if (marketMovingOnly == null) marketMovingOnly = false;
        if (verifiedOnly == null) verifiedOnly = false;
        if (highQualityOnly == null) highQualityOnly = false;
        if (excludeDuplicates == null) excludeDuplicates = true;
        if (highEngagementOnly == null) highEngagementOnly = false;
        if (includeContent == null) includeContent = false;
        if (includeSentiment == null) includeSentiment = true;
        if (includeEngagement == null) includeEngagement = true;
        if (includeRelated == null) includeRelated = true;
        if (includeAnalysis == null) includeAnalysis = false;
        if (summarizeOnly == null) summarizeOnly = false;
        if (timeZone == null) timeZone = "UTC";
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    /**
     * Validation methods
     */
    public boolean isValid() {
        return page != null && page >= 0 &&
               size != null && size > 0 && size <= 100 &&
               sortBy != null && !sortBy.trim().isEmpty();
    }
    
    public boolean hasTimeFilter() {
        return startTime != null || endTime != null ||
               recentOnly || freshOnly || todayOnly ||
               hoursBack != null || daysBack != null;
    }
    
    public boolean hasContentFilter() {
        return (categories != null && !categories.isEmpty()) ||
               (sources != null && !sources.isEmpty()) ||
               (regions != null && !regions.isEmpty()) ||
               (searchTerm != null && !searchTerm.trim().isEmpty());
    }
    
    public boolean hasMarketFilter() {
        return (symbols != null && !symbols.isEmpty()) ||
               (sectors != null && !sectors.isEmpty()) ||
               (currencies != null && !currencies.isEmpty()) ||
               (commodities != null && !commodities.isEmpty());
    }
    
    public boolean hasSentimentFilter() {
        return (sentimentLabels != null && !sentimentLabels.isEmpty()) ||
               minSentimentScore != null || maxSentimentScore != null ||
               minConfidenceScore != null;
    }
    
    public boolean hasImpactFilter() {
        return minRelevanceScore != null || minImpactScore != null ||
               minUrgencyScore != null ||
               (marketImpactLevels != null && !marketImpactLevels.isEmpty());
    }
    
    public boolean hasSpecialFilter() {
        return trendingOnly || breakingOnly || marketMovingOnly ||
               verifiedOnly || highQualityOnly;
    }
    
    public boolean hasEngagementFilter() {
        return minViewCount != null || minShareCount != null ||
               minCommentCount != null || minEngagementScore != null ||
               highEngagementOnly;
    }
    
    public boolean hasQualityFilter() {
        return minQualityScore != null || minWordCount != null ||
               maxWordCount != null || maxReadingTime != null;
    }
    
    /**
     * Get total number of active filters
     */
    public int getActiveFilterCount() {
        int count = 0;
        
        if (hasTimeFilter()) count++;
        if (hasContentFilter()) count++;
        if (hasMarketFilter()) count++;
        if (hasSentimentFilter()) count++;
        if (hasImpactFilter()) count++;
        if (hasSpecialFilter()) count++;
        if (hasEngagementFilter()) count++;
        if (hasQualityFilter()) count++;
        
        return count;
    }
    
    /**
     * Get effective time range
     */
    public Instant getEffectiveStartTime() {
        if (startTime != null) {
            return startTime;
        }
        
        Instant now = Instant.now();
        
        if (freshOnly) {
            return now.minusSeconds(3600); // 1 hour
        }
        
        if (recentOnly) {
            return now.minusSeconds(86400); // 24 hours
        }
        
        if (todayOnly) {
            return now.minusSeconds(86400); // 24 hours (simplified)
        }
        
        if (hoursBack != null) {
            return now.minusSeconds(hoursBack * 3600L);
        }
        
        if (daysBack != null) {
            return now.minusSeconds(daysBack * 86400L);
        }
        
        // Default: last 7 days
        return now.minusSeconds(7 * 86400L);
    }
    
    public Instant getEffectiveEndTime() {
        return endTime != null ? endTime : Instant.now();
    }
    
    /**
     * Get complexity level
     */
    public String getComplexity() {
        int filters = getActiveFilterCount();
        
        if (filters > 5 || (symbols != null && symbols.size() > 10) ||
            (searchTerm != null && !searchTerm.isEmpty())) {
            return "HIGH";
        } else if (filters > 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Create preset requests
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
    
    public static MarketNewsRequest trendingNews() {
        return MarketNewsRequest.builder()
            .trendingOnly(true)
            .recentOnly(true)
            .sortBy("relevanceScore")
            .sortDirection(SortDirection.DESC)
            .includeEngagement(true)
            .build();
    }
    
    public static MarketNewsRequest marketMovingNews() {
        return MarketNewsRequest.builder()
            .marketMovingOnly(true)
            .daysBack(3)
            .sortBy("impactScore")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }
    
    public static MarketNewsRequest recentNews() {
        return MarketNewsRequest.builder()
            .recentOnly(true)
            .highQualityOnly(true)
            .excludeDuplicates(true)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketNewsRequest freshNews() {
        return MarketNewsRequest.builder()
            .freshOnly(true)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }
    
    public static MarketNewsRequest positiveNews() {
        return MarketNewsRequest.builder()
            .minSentimentScore(new BigDecimal("0.3"))
            .minConfidenceScore(new BigDecimal("0.7"))
            .recentOnly(true)
            .sortBy("sentimentScore")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketNewsRequest negativeNews() {
        return MarketNewsRequest.builder()
            .maxSentimentScore(new BigDecimal("-0.3"))
            .minConfidenceScore(new BigDecimal("0.7"))
            .recentOnly(true)
            .sortBy("sentimentScore")
            .sortDirection(SortDirection.ASC)
            .build();
    }
    
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
    
    public static MarketNewsRequest sectorNews(String sector) {
        return MarketNewsRequest.builder()
            .sectors(Set.of(sector))
            .daysBack(7)
            .sortBy("relevanceScore")
            .sortDirection(SortDirection.DESC)
            .includeAnalysis(true)
            .build();
    }
    
    public static MarketNewsRequest categoryNews(String category) {
        return MarketNewsRequest.builder()
            .categories(Set.of(category))
            .daysBack(14)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketNewsRequest sourceNews(String source) {
        return MarketNewsRequest.builder()
            .sources(Set.of(source))
            .daysBack(7)
            .sortBy("publishedAt")
            .sortDirection(SortDirection.DESC)
            .build();
    }
    
    public static MarketNewsRequest highEngagementNews() {
        return MarketNewsRequest.builder()
            .highEngagementOnly(true)
            .daysBack(3)
            .sortBy("engagementScore")
            .sortDirection(SortDirection.DESC)
            .includeEngagement(true)
            .build();
    }
    
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
    
    /**
     * Create request for specific analysis needs
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