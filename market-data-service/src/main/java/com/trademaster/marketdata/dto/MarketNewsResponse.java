package com.trademaster.marketdata.dto;

import com.trademaster.marketdata.entity.MarketNews;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Market News Response DTO
 * 
 * Comprehensive response containing market news with sentiment analysis,
 * engagement metrics, and market impact assessment.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record MarketNewsResponse(
    
    // Request metadata
    MarketNewsRequest originalRequest,
    Instant responseTime,
    String timezone,
    
    // News data
    List<MarketNewsDto> news,
    
    // Analytics and insights
    NewsAnalytics analytics,
    SentimentAnalysis sentimentAnalysis,
    MarketImpactSummary marketImpact,
    
    // Pagination
    PaginationInfo pagination,
    
    // Additional insights
    List<String> marketAlerts,
    List<TrendingTopic> trendingTopics,
    Map<String, Object> insights
    
) {
    
    /**
     * Market News DTO for API responses
     */
    @Builder
    public record MarketNewsDto(
        
        // Basic information
        Long id,
        String newsId,
        String title,
        String summary,
        String content,        // Optional based on request
        String source,
        String author,
        
        // Timing
        Instant publishedAt,
        Instant updatedAt,
        String displayAge,     // "2 hours ago", "1 day ago", etc.
        Long ageInMinutes,
        Long ageInHours,
        
        // URLs and media
        String url,
        String imageUrl,
        
        // Categorization
        String category,
        String subCategory,
        String region,
        List<String> tags,
        
        // Market relevance
        List<String> relatedSymbols,
        List<String> relatedSectors,
        List<String> relatedCurrencies,
        List<String> relatedCommodities,
        
        // Sentiment analysis
        BigDecimal sentimentScore,     // -1.0 to 1.0
        MarketNews.SentimentLabel sentimentLabel,
        String sentimentDisplay,       // "Positive", "Negative", etc.
        String sentimentEmoji,         // 😊, 😞, etc.
        BigDecimal confidenceScore,    // 0.0 to 1.0
        
        // Scoring
        BigDecimal relevanceScore,     // 0-100
        BigDecimal impactScore,        // 0-100
        BigDecimal urgencyScore,       // 0-100
        BigDecimal compositeScore,     // Combined relevance score
        MarketNews.MarketImpact marketImpact,
        String impactDisplay,
        String impactEmoji,
        
        // Engagement metrics (optional)
        Long viewCount,
        Long shareCount,
        Long commentCount,
        Long engagementScore,
        
        // Content metrics
        Integer wordCount,
        Integer readingTimeMinutes,
        List<String> keyPhrases,
        List<String> namedEntities,
        
        // Quality and verification
        BigDecimal qualityScore,
        Boolean isVerified,
        Boolean isDuplicate,
        String duplicateGroup,
        
        // Special flags
        Boolean isTrending,
        Boolean isBreakingNews,
        Boolean isMarketMoving,
        Boolean isFresh,
        Boolean isRecent,
        Boolean isPositiveSentiment,
        Boolean isNegativeSentiment,
        Boolean isHighRelevance,
        Boolean isHighImpact,
        Boolean isUrgent,
        Boolean shouldBeFeatured,
        
        // Display helpers
        Integer priorityScore,
        String categoryDisplay,
        String sourceDisplay,
        String timeDisplay,
        Map<String, Object> metadata
        
    ) {
        
        /**
         * Factory method to create DTO from entity
         */
        public static MarketNewsDto fromEntity(MarketNews news, boolean includeContent, boolean includeEngagement) {
            return MarketNewsDto.builder()
                .id(news.getId())
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .content(includeContent ? news.getContent() : null)
                .source(news.getSource())
                .author(news.getAuthor())
                .publishedAt(news.getPublishedAt())
                .updatedAt(news.getUpdatedAt())
                .displayAge(news.getDisplayAge())
                .ageInMinutes(news.getAgeInMinutes())
                .ageInHours(news.getAgeInHours())
                .url(news.getUrl())
                .imageUrl(news.getImageUrl())
                .category(news.getCategory())
                .subCategory(news.getSubCategory())
                .region(news.getRegion())
                .tags(parseJsonArray(news.getTags()))
                .relatedSymbols(parseJsonArray(news.getRelatedSymbols()))
                .relatedSectors(parseJsonArray(news.getRelatedSectors()))
                .relatedCurrencies(parseJsonArray(news.getRelatedCurrencies()))
                .relatedCommodities(parseJsonArray(news.getRelatedCommodities()))
                .sentimentScore(news.getSentimentScore())
                .sentimentLabel(news.getSentimentLabel())
                .sentimentDisplay(news.getSentimentLabel() != null ? 
                    news.getSentimentLabel().getDescription() : "Neutral")
                .sentimentEmoji(news.getSentimentEmoji())
                .confidenceScore(news.getConfidenceScore())
                .relevanceScore(news.getRelevanceScore())
                .impactScore(news.getImpactScore())
                .urgencyScore(news.getUrgencyScore())
                .compositeScore(news.getCompositeScore())
                .marketImpact(news.getMarketImpact())
                .impactDisplay(news.getMarketImpact() != null ? 
                    news.getMarketImpact().getDescription() : "Unknown impact")
                .impactEmoji(news.getImpactEmoji())
                .viewCount(includeEngagement ? news.getViewCount() : null)
                .shareCount(includeEngagement ? news.getShareCount() : null)
                .commentCount(includeEngagement ? news.getCommentCount() : null)
                .engagementScore(includeEngagement ? news.getEngagementScore() : null)
                .wordCount(news.getWordCount())
                .readingTimeMinutes(news.getReadingTimeMinutes())
                .keyPhrases(parseJsonArray(news.getKeyPhrases()))
                .namedEntities(parseJsonArray(news.getNamedEntities()))
                .qualityScore(news.getQualityScore())
                .isVerified(news.getIsVerified())
                .isDuplicate(news.getIsDuplicate())
                .duplicateGroup(news.getDuplicateGroup())
                .isTrending(news.getIsTrending())
                .isBreakingNews(news.getIsBreakingNews())
                .isMarketMoving(news.getIsMarketMoving())
                .isFresh(news.isFresh())
                .isRecent(news.isRecent())
                .isPositiveSentiment(news.isPositiveSentiment())
                .isNegativeSentiment(news.isNegativeSentiment())
                .isHighRelevance(news.isHighRelevance())
                .isHighImpact(news.isHighImpact())
                .isUrgent(news.isUrgent())
                .shouldBeFeatured(news.shouldBeFeatured())
                .priorityScore(news.getPriorityScore())
                .categoryDisplay(formatCategory(news.getCategory()))
                .sourceDisplay(formatSource(news.getSource()))
                .timeDisplay(formatTimeDisplay(news.getPublishedAt()))
                .metadata(buildMetadata(news))
                .build();
        }
        
        // Helper methods
        private static List<String> parseJsonArray(String jsonArray) {
            // Simplified JSON parsing - in production use proper JSON library
            if (jsonArray == null || jsonArray.trim().isEmpty()) {
                return List.of();
            }
            // Stub implementation
            return List.of();
        }
        
        private static String formatCategory(String category) {
            if (category == null) return "General";
            return switch (category.toUpperCase()) {
                case "EARNINGS" -> "Earnings & Results";
                case "ECONOMY" -> "Economic Data";
                case "POLITICS" -> "Political News";
                case "TECHNOLOGY" -> "Technology & Innovation";
                case "CENTRAL_BANK" -> "Central Bank Policy";
                case "MARKET_UPDATE" -> "Market Updates";
                default -> category;
            };
        }
        
        private static String formatSource(String source) {
            if (source == null) return "Unknown";
            return switch (source.toLowerCase()) {
                case "reuters" -> "Reuters";
                case "bloomberg" -> "Bloomberg";
                case "economic_times" -> "Economic Times";
                case "financial_express" -> "Financial Express";
                case "mint" -> "Mint";
                case "cnbc" -> "CNBC";
                case "marketwatch" -> "MarketWatch";
                default -> source;
            };
        }
        
        private static String formatTimeDisplay(Instant publishedAt) {
            if (publishedAt == null) return "";
            // Format time for display - simplified
            return publishedAt.toString();
        }
        
        private static Map<String, Object> buildMetadata(MarketNews news) {
            return Map.of(
                "dataProvider", news.getDataProvider() != null ? news.getDataProvider() : "internal",
                "processingVersion", news.getProcessingVersion() != null ? news.getProcessingVersion() : "1.0",
                "qualityGrade", getQualityGrade(news.getQualityScore()),
                "engagementLevel", getEngagementLevel(news.getEngagementScore()),
                "freshnessLevel", getFreshnessLevel(news.getAgeInHours())
            );
        }
        
        private static String getQualityGrade(BigDecimal qualityScore) {
            if (qualityScore == null) return "Unknown";
            double score = qualityScore.doubleValue();
            if (score >= 0.9) return "A+";
            if (score >= 0.8) return "A";
            if (score >= 0.7) return "B";
            if (score >= 0.6) return "C";
            return "D";
        }
        
        private static String getEngagementLevel(Long engagementScore) {
            if (engagementScore == null || engagementScore == 0) return "Low";
            if (engagementScore > 100000) return "Viral";
            if (engagementScore > 10000) return "High";
            if (engagementScore > 1000) return "Medium";
            return "Low";
        }
        
        private static String getFreshnessLevel(Long ageInHours) {
            if (ageInHours == null) return "Unknown";
            if (ageInHours < 1) return "Fresh";
            if (ageInHours < 6) return "Recent";
            if (ageInHours < 24) return "Today";
            if (ageInHours < 168) return "This Week";
            return "Older";
        }
    }
    
    /**
     * News analytics and statistics
     */
    @Builder
    public record NewsAnalytics(
        Integer totalNews,
        Integer breakingNews,
        Integer trendingNews,
        Integer marketMovingNews,
        Integer verifiedNews,
        Integer duplicateNews,
        Map<String, Integer> newsByCategory,
        Map<String, Integer> newsBySource,
        Map<String, Integer> newsByRegion,
        Map<String, Integer> newsByHour,
        BigDecimal averageRelevanceScore,
        BigDecimal averageImpactScore,
        BigDecimal averageSentimentScore,
        BigDecimal averageQualityScore,
        String mostActiveSource,
        String mostActiveCategory,
        Integer totalEngagement,
        String peakHour
    ) {}
    
    /**
     * Sentiment analysis summary
     */
    @Builder
    public record SentimentAnalysis(
        BigDecimal overallSentiment,        // -1.0 to 1.0
        String overallSentimentLabel,       // "Positive", "Negative", etc.
        String sentimentTrend,              // "Improving", "Declining", "Stable"
        Map<String, Integer> sentimentDistribution,
        Map<String, BigDecimal> sentimentByCategory,
        Map<String, BigDecimal> sentimentBySource,
        Map<String, BigDecimal> sentimentBySymbol,
        Map<String, BigDecimal> sentimentBySector,
        List<MarketNewsDto> mostPositiveNews,
        List<MarketNewsDto> mostNegativeNews,
        BigDecimal confidenceLevel,
        String marketMood               // "Optimistic", "Pessimistic", "Neutral", "Mixed"
    ) {}
    
    /**
     * Market impact summary
     */
    @Builder
    public record MarketImpactSummary(
        List<MarketNewsDto> highImpactNews,
        List<MarketNewsDto> breakingNews,
        List<MarketNewsDto> marketMovingNews,
        Map<String, BigDecimal> symbolImpactScores,
        Map<String, BigDecimal> sectorImpactScores,
        Map<String, BigDecimal> currencyImpactScores,
        List<String> riskFactors,
        List<String> opportunities,
        String overallMarketImpact,     // "High", "Medium", "Low"
        BigDecimal volatilityForecast,  // Expected volatility increase
        List<String> tradingRecommendations
    ) {}
    
    /**
     * Trending topic
     */
    @Builder
    public record TrendingTopic(
        String topic,
        Integer mentionCount,
        BigDecimal averageSentiment,
        BigDecimal trendScore,
        List<String> relatedSymbols,
        String category
    ) {}
    
    /**
     * Pagination information
     */
    @Builder
    public record PaginationInfo(
        Integer currentPage,
        Integer pageSize,
        Integer totalPages,
        Long totalNews,
        Boolean hasNext,
        Boolean hasPrevious
    ) {}
    
    /**
     * Get news by sentiment
     */
    public List<MarketNewsDto> getNewsBySentiment(MarketNews.SentimentLabel sentiment) {
        return news.stream()
            .filter(n -> sentiment.equals(n.sentimentLabel()))
            .toList();
    }
    
    /**
     * Get positive news
     */
    public List<MarketNewsDto> getPositiveNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isPositiveSentiment()))
            .toList();
    }
    
    /**
     * Get negative news
     */
    public List<MarketNewsDto> getNegativeNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isNegativeSentiment()))
            .toList();
    }
    
    /**
     * Get breaking news
     */
    public List<MarketNewsDto> getBreakingNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isBreakingNews()))
            .toList();
    }
    
    /**
     * Get trending news
     */
    public List<MarketNewsDto> getTrendingNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isTrending()))
            .toList();
    }
    
    /**
     * Get market moving news
     */
    public List<MarketNewsDto> getMarketMovingNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isMarketMoving()))
            .toList();
    }
    
    /**
     * Get high impact news
     */
    public List<MarketNewsDto> getHighImpactNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isHighImpact()))
            .toList();
    }
    
    /**
     * Get fresh news (last hour)
     */
    public List<MarketNewsDto> getFreshNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.isFresh()))
            .toList();
    }
    
    /**
     * Get featured news
     */
    public List<MarketNewsDto> getFeaturedNews() {
        return news.stream()
            .filter(n -> Boolean.TRUE.equals(n.shouldBeFeatured()))
            .toList();
    }
    
    /**
     * Get news by category
     */
    public List<MarketNewsDto> getNewsByCategory(String category) {
        return news.stream()
            .filter(n -> category.equals(n.category()))
            .toList();
    }
    
    /**
     * Get news by source
     */
    public List<MarketNewsDto> getNewsBySource(String source) {
        return news.stream()
            .filter(n -> source.equals(n.source()))
            .toList();
    }
    
    /**
     * Get news affecting symbol
     */
    public List<MarketNewsDto> getNewsForSymbol(String symbol) {
        return news.stream()
            .filter(n -> n.relatedSymbols() != null && n.relatedSymbols().contains(symbol))
            .toList();
    }
    
    /**
     * Get news affecting sector
     */
    public List<MarketNewsDto> getNewsForSector(String sector) {
        return news.stream()
            .filter(n -> n.relatedSectors() != null && n.relatedSectors().contains(sector))
            .toList();
    }
    
    /**
     * Get response summary
     */
    public String getSummary() {
        if (analytics == null) {
            return String.format("Found %d news articles", news.size());
        }
        
        return String.format(
            "Found %d articles (%d breaking, %d trending, %d market-moving)",
            analytics.totalNews(),
            analytics.breakingNews(),
            analytics.trendingNews(),
            analytics.marketMovingNews()
        );
    }
    
    /**
     * Get overall market sentiment
     */
    public String getOverallSentiment() {
        if (sentimentAnalysis != null) {
            return sentimentAnalysis.overallSentimentLabel();
        }
        return "Unknown";
    }
    
    /**
     * Check if response contains significant news
     */
    public boolean hasSignificantNews() {
        return analytics != null && 
               (analytics.breakingNews() > 0 || analytics.marketMovingNews() > 0);
    }
    
    /**
     * Get market risk level
     */
    public String getRiskLevel() {
        if (marketImpact == null) {
            return "UNKNOWN";
        }
        
        return marketImpact.overallMarketImpact() != null ? 
            marketImpact.overallMarketImpact() : "UNKNOWN";
    }
}