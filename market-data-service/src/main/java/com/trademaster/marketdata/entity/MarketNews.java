package com.trademaster.marketdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Market News Entity
 * 
 * Represents market news articles with sentiment analysis,
 * relevance scoring, and impact assessment for trading decisions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "market_news", indexes = {
    @Index(name = "idx_market_news_published_at", columnList = "publishedAt"),
    @Index(name = "idx_market_news_sentiment", columnList = "sentimentScore"),
    @Index(name = "idx_market_news_relevance", columnList = "relevanceScore"),
    @Index(name = "idx_market_news_category", columnList = "category"),
    @Index(name = "idx_market_news_source", columnList = "source"),
    @Index(name = "idx_market_news_symbols", columnList = "relatedSymbols"),
    @Index(name = "idx_market_news_trending", columnList = "isTrending")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketNews {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String newsId; // External provider news ID
    
    @Column(nullable = false, length = 1000)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private String source; // Reuters, Bloomberg, Economic Times, etc.
    
    @Column
    private String author;
    
    @Column(nullable = false)
    private Instant publishedAt;
    
    @Column
    private Instant updatedAt;
    
    @Column(nullable = false)
    private String url;
    
    @Column
    private String imageUrl;
    
    // Categorization
    @Column(nullable = false)
    private String category; // EARNINGS, ECONOMY, POLITICS, TECHNOLOGY, etc.
    
    @Column
    private String subCategory; // Quarterly Results, GDP, Elections, AI, etc.
    
    @Column
    private String region; // GLOBAL, INDIA, US, ASIA, EUROPE, etc.
    
    @Column(columnDefinition = "JSON")
    private String tags; // JSON array of tags
    
    // Market relevance
    @Column(columnDefinition = "JSON")
    private String relatedSymbols; // JSON array of stock symbols
    
    @Column(columnDefinition = "JSON")
    private String relatedSectors; // JSON array of sectors
    
    @Column(columnDefinition = "JSON")
    private String relatedCurrencies; // JSON array of currencies
    
    @Column(columnDefinition = "JSON")
    private String relatedCommodities; // JSON array of commodities
    
    // Sentiment Analysis
    @Column(precision = 5, scale = 4)
    private BigDecimal sentimentScore; // -1.0 (very negative) to 1.0 (very positive)
    
    @Column
    @Enumerated(EnumType.STRING)
    private SentimentLabel sentimentLabel;
    
    @Column(precision = 5, scale = 4)
    private BigDecimal confidenceScore; // 0.0 to 1.0 confidence in sentiment
    
    // Impact and relevance scoring
    @Column(precision = 5, scale = 2)
    private BigDecimal relevanceScore; // 0-100 relevance to markets
    
    @Column(precision = 5, scale = 2)
    private BigDecimal impactScore; // 0-100 potential market impact
    
    @Column
    @Enumerated(EnumType.STRING)
    private MarketImpact marketImpact;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal urgencyScore; // 0-100 time sensitivity
    
    // Engagement metrics
    @Column
    private Long viewCount;
    
    @Column
    private Long shareCount;
    
    @Column
    private Long commentCount;
    
    @Column
    private Boolean isTrending;
    
    @Column
    private Boolean isBreakingNews;
    
    @Column
    private Boolean isMarketMoving;
    
    // Content analysis
    @Column
    private Integer wordCount;
    
    @Column
    private Integer readingTimeMinutes;
    
    @Column(columnDefinition = "JSON")
    private String keyPhrases; // JSON array of extracted key phrases
    
    @Column(columnDefinition = "JSON")
    private String namedEntities; // JSON array of companies, people, places
    
    // Data quality
    @Column(precision = 3, scale = 2)
    private BigDecimal qualityScore; // 0.0 to 1.0 content quality score
    
    @Column
    private Boolean isVerified; // Source verification status
    
    @Column
    private Boolean isDuplicate; // Duplicate detection flag
    
    @Column
    private String duplicateGroup; // Group ID for duplicate articles
    
    // Processing metadata
    @Column
    private String dataProvider; // NewsAPI, Alpha Vantage, etc.
    
    @Column
    private Instant processedAt;
    
    @Column
    private String processingVersion;
    
    @Column(columnDefinition = "JSON")
    private String metadata; // Additional processing metadata
    
    // Audit fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant lastModified;
    
    @Version
    private Long version;
    
    /**
     * Sentiment labels
     */
    public enum SentimentLabel {
        VERY_NEGATIVE("Very Negative"),
        NEGATIVE("Negative"),
        SLIGHTLY_NEGATIVE("Slightly Negative"),
        NEUTRAL("Neutral"),
        SLIGHTLY_POSITIVE("Slightly Positive"),
        POSITIVE("Positive"),
        VERY_POSITIVE("Very Positive");
        
        private final String description;
        
        SentimentLabel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static SentimentLabel fromScore(BigDecimal score) {
            if (score == null) return NEUTRAL;
            
            double s = score.doubleValue();
            if (s <= -0.6) return VERY_NEGATIVE;
            if (s <= -0.3) return NEGATIVE;
            if (s <= -0.1) return SLIGHTLY_NEGATIVE;
            if (s >= 0.6) return VERY_POSITIVE;
            if (s >= 0.3) return POSITIVE;
            if (s >= 0.1) return SLIGHTLY_POSITIVE;
            return NEUTRAL;
        }
    }
    
    /**
     * Market impact levels
     */
    public enum MarketImpact {
        MINIMAL("Minimal market impact"),
        LOW("Low market impact"),
        MODERATE("Moderate market impact"),
        HIGH("High market impact"),
        CRITICAL("Critical market impact");
        
        private final String description;
        
        MarketImpact(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static MarketImpact fromScore(BigDecimal score) {
            if (score == null) return MINIMAL;
            
            double s = score.doubleValue();
            if (s >= 80) return CRITICAL;
            if (s >= 60) return HIGH;
            if (s >= 40) return MODERATE;
            if (s >= 20) return LOW;
            return MINIMAL;
        }
    }
    
    /**
     * Business logic methods
     */
    
    /**
     * Check if news is recent (within last 24 hours)
     */
    public boolean isRecent() {
        return publishedAt.isAfter(Instant.now().minusSeconds(86400));
    }
    
    /**
     * Check if news is fresh (within last hour)
     */
    public boolean isFresh() {
        return publishedAt.isAfter(Instant.now().minusSeconds(3600));
    }
    
    /**
     * Check if news is positive sentiment
     */
    public boolean isPositiveSentiment() {
        return sentimentScore != null && sentimentScore.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if news is negative sentiment
     */
    public boolean isNegativeSentiment() {
        return sentimentScore != null && sentimentScore.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Check if news has high relevance
     */
    public boolean isHighRelevance() {
        return relevanceScore != null && relevanceScore.compareTo(new BigDecimal("70")) >= 0;
    }
    
    /**
     * Check if news has high market impact
     */
    public boolean isHighImpact() {
        return impactScore != null && impactScore.compareTo(new BigDecimal("70")) >= 0;
    }
    
    /**
     * Check if news is time-sensitive
     */
    public boolean isUrgent() {
        return urgencyScore != null && urgencyScore.compareTo(new BigDecimal("70")) >= 0;
    }
    
    /**
     * Get age in hours
     */
    public long getAgeInHours() {
        return java.time.Duration.between(publishedAt, Instant.now()).toHours();
    }
    
    /**
     * Get age in minutes
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(publishedAt, Instant.now()).toMinutes();
    }
    
    /**
     * Check if news affects specific symbol
     */
    public boolean affectsSymbol(String symbol) {
        if (relatedSymbols == null) return false;
        // Simplified check - in production use proper JSON parsing
        return relatedSymbols.contains(symbol);
    }
    
    /**
     * Check if news affects specific sector
     */
    public boolean affectsSector(String sector) {
        if (relatedSectors == null) return false;
        return relatedSectors.contains(sector);
    }
    
    /**
     * Get engagement score
     */
    public Long getEngagementScore() {
        long engagement = 0;
        if (viewCount != null) engagement += viewCount;
        if (shareCount != null) engagement += shareCount * 5; // Shares worth more
        if (commentCount != null) engagement += commentCount * 3; // Comments worth more
        return engagement;
    }
    
    /**
     * Check if news has high engagement
     */
    public boolean isHighEngagement() {
        return getEngagementScore() > 10000; // Threshold for high engagement
    }
    
    /**
     * Get composite relevance score
     */
    public BigDecimal getCompositeScore() {
        BigDecimal composite = BigDecimal.ZERO;
        
        // Relevance weight: 40%
        if (relevanceScore != null) {
            composite = composite.add(relevanceScore.multiply(new BigDecimal("0.4")));
        }
        
        // Impact weight: 30%
        if (impactScore != null) {
            composite = composite.add(impactScore.multiply(new BigDecimal("0.3")));
        }
        
        // Recency weight: 20% (decay over time)
        long ageHours = getAgeInHours();
        BigDecimal recencyScore = BigDecimal.valueOf(Math.max(0, 100 - ageHours * 2));
        composite = composite.add(recencyScore.multiply(new BigDecimal("0.2")));
        
        // Engagement weight: 10%
        Long engagement = getEngagementScore();
        BigDecimal engagementScore = engagement > 0 ? 
            BigDecimal.valueOf(Math.min(100, Math.log10(engagement) * 20)) : BigDecimal.ZERO;
        composite = composite.add(engagementScore.multiply(new BigDecimal("0.1")));
        
        return composite.setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get display-friendly age
     */
    public String getDisplayAge() {
        long minutes = getAgeInMinutes();
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return String.format("%d minutes ago", minutes);
        
        long hours = minutes / 60;
        if (hours < 24) return String.format("%d hours ago", hours);
        
        long days = hours / 24;
        if (days < 7) return String.format("%d days ago", days);
        
        return String.format("%d weeks ago", days / 7);
    }
    
    /**
     * Get sentiment emoji
     */
    public String getSentimentEmoji() {
        if (sentimentLabel == null) return "😐";
        
        return switch (sentimentLabel) {
            case VERY_NEGATIVE -> "😡";
            case NEGATIVE -> "😞";
            case SLIGHTLY_NEGATIVE -> "😕";
            case NEUTRAL -> "😐";
            case SLIGHTLY_POSITIVE -> "🙂";
            case POSITIVE -> "😊";
            case VERY_POSITIVE -> "🚀";
        };
    }
    
    /**
     * Get impact emoji
     */
    public String getImpactEmoji() {
        if (marketImpact == null) return "➖";
        
        return switch (marketImpact) {
            case MINIMAL -> "➖";
            case LOW -> "🔸";
            case MODERATE -> "🔶";
            case HIGH -> "🔴";
            case CRITICAL -> "🚨";
        };
    }
    
    /**
     * Check if news should be featured
     */
    public boolean shouldBeFeatured() {
        return isBreakingNews || isMarketMoving || isTrending ||
               (isHighRelevance() && isHighImpact() && isRecent());
    }
    
    /**
     * Get priority score for ranking
     */
    public int getPriorityScore() {
        int score = 0;
        
        // Breaking news gets highest priority
        if (isBreakingNews != null && isBreakingNews) score += 100;
        
        // Market moving news gets high priority
        if (isMarketMoving != null && isMarketMoving) score += 80;
        
        // Trending news gets good priority
        if (isTrending != null && isTrending) score += 60;
        
        // Add impact and relevance scores
        if (impactScore != null) score += impactScore.intValue();
        if (relevanceScore != null) score += relevanceScore.intValue() / 2;
        
        // Boost for recent news
        if (isFresh()) score += 50;
        else if (isRecent()) score += 25;
        
        // Boost for high engagement
        if (isHighEngagement()) score += 30;
        
        return score;
    }
}