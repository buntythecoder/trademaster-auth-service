package com.trademaster.marketdata.repository;

import com.trademaster.marketdata.entity.MarketNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Market News Repository
 * 
 * Provides data access methods for market news with optimized
 * queries for sentiment analysis, relevance filtering, and trending content.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface MarketNewsRepository extends JpaRepository<MarketNews, Long> {
    
    /**
     * Find news by external news ID
     */
    Optional<MarketNews> findByNewsId(String newsId);
    
    /**
     * Get recent news (last 24 hours)
     */
    @Query("SELECT n FROM MarketNews n WHERE n.publishedAt >= :since " +
           "ORDER BY n.publishedAt DESC, n.relevanceScore DESC")
    List<MarketNews> findRecentNews(@Param("since") Instant since);
    
    /**
     * Get recent news with pagination
     */
    @Query("SELECT n FROM MarketNews n WHERE n.publishedAt >= :since " +
           "ORDER BY n.publishedAt DESC, n.relevanceScore DESC")
    Page<MarketNews> findRecentNews(@Param("since") Instant since, Pageable pageable);
    
    /**
     * Get trending news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.isTrending = true " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findTrendingNews(@Param("since") Instant since);
    
    /**
     * Get breaking news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.isBreakingNews = true " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.publishedAt DESC")
    List<MarketNews> findBreakingNews(@Param("since") Instant since);
    
    /**
     * Get market moving news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.isMarketMoving = true " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.impactScore DESC, n.publishedAt DESC")
    List<MarketNews> findMarketMovingNews(@Param("since") Instant since);
    
    /**
     * Get news by sentiment
     */
    @Query("SELECT n FROM MarketNews n WHERE n.sentimentLabel = :sentiment " +
           "AND n.publishedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY n.publishedAt DESC")
    List<MarketNews> findNewsBySentiment(
        @Param("sentiment") MarketNews.SentimentLabel sentiment,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get news by sentiment score range
     */
    @Query("SELECT n FROM MarketNews n WHERE n.sentimentScore BETWEEN :minScore AND :maxScore " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.sentimentScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsBySentimentScore(
        @Param("minScore") BigDecimal minScore,
        @Param("maxScore") BigDecimal maxScore,
        @Param("since") Instant since);
    
    /**
     * Get highly positive news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.sentimentScore >= :minScore " +
           "AND n.confidenceScore >= :minConfidence " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.sentimentScore DESC, n.publishedAt DESC")
    List<MarketNews> findPositiveNews(
        @Param("minScore") BigDecimal minScore,
        @Param("minConfidence") BigDecimal minConfidence,
        @Param("since") Instant since);
    
    /**
     * Get highly negative news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.sentimentScore <= :maxScore " +
           "AND n.confidenceScore >= :minConfidence " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.sentimentScore ASC, n.publishedAt DESC")
    List<MarketNews> findNegativeNews(
        @Param("maxScore") BigDecimal maxScore,
        @Param("minConfidence") BigDecimal minConfidence,
        @Param("since") Instant since);
    
    /**
     * Get news by category
     */
    @Query("SELECT n FROM MarketNews n WHERE n.category = :category " +
           "AND n.publishedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsByCategory(
        @Param("category") String category,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get news by multiple categories
     */
    @Query("SELECT n FROM MarketNews n WHERE n.category IN :categories " +
           "AND n.publishedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsByCategories(
        @Param("categories") List<String> categories,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get news by source
     */
    @Query("SELECT n FROM MarketNews n WHERE n.source = :source " +
           "AND n.publishedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY n.publishedAt DESC")
    List<MarketNews> findNewsBySource(
        @Param("source") String source,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get news by region
     */
    @Query("SELECT n FROM MarketNews n WHERE n.region = :region " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsByRegion(
        @Param("region") String region,
        @Param("since") Instant since);
    
    /**
     * Get high relevance news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.relevanceScore >= :minRelevance " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findHighRelevanceNews(
        @Param("minRelevance") BigDecimal minRelevance,
        @Param("since") Instant since);
    
    /**
     * Get high impact news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.impactScore >= :minImpact " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.impactScore DESC, n.publishedAt DESC")
    List<MarketNews> findHighImpactNews(
        @Param("minImpact") BigDecimal minImpact,
        @Param("since") Instant since);
    
    /**
     * Get news affecting specific symbol
     */
    @Query("SELECT n FROM MarketNews n WHERE n.relatedSymbols LIKE CONCAT('%', :symbol, '%') " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsBySymbol(
        @Param("symbol") String symbol,
        @Param("since") Instant since);
    
    /**
     * Get news affecting multiple symbols
     */
    @Query("SELECT DISTINCT n FROM MarketNews n WHERE " +
           "(:symbols IS NULL OR EXISTS (SELECT 1 FROM unnest(string_to_array(REPLACE(REPLACE(n.relatedSymbols, '[', ''), ']', ''), ',')) AS symbol WHERE TRIM(BOTH '\"' FROM symbol) IN :symbols)) " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsBySymbols(
        @Param("symbols") List<String> symbols,
        @Param("since") Instant since);
    
    /**
     * Get news affecting specific sector
     */
    @Query("SELECT n FROM MarketNews n WHERE n.relatedSectors LIKE CONCAT('%', :sector, '%') " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findNewsBySector(
        @Param("sector") String sector,
        @Param("since") Instant since);
    
    /**
     * Search news by title and content
     */
    @Query("SELECT n FROM MarketNews n WHERE " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> searchNews(
        @Param("searchTerm") String searchTerm,
        @Param("since") Instant since);
    
    /**
     * Search news with pagination
     */
    @Query("SELECT n FROM MarketNews n WHERE " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    Page<MarketNews> searchNews(
        @Param("searchTerm") String searchTerm,
        @Param("since") Instant since,
        Pageable pageable);
    
    /**
     * Get top news by engagement
     */
    @Query("SELECT n FROM MarketNews n WHERE n.publishedAt >= :since " +
           "ORDER BY (COALESCE(n.viewCount, 0) + COALESCE(n.shareCount, 0) * 5 + COALESCE(n.commentCount, 0) * 3) DESC")
    List<MarketNews> findTopEngagementNews(@Param("since") Instant since);
    
    /**
     * Get news with specific market impact
     */
    @Query("SELECT n FROM MarketNews n WHERE n.marketImpact = :impact " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.publishedAt DESC")
    List<MarketNews> findNewsByMarketImpact(
        @Param("impact") MarketNews.MarketImpact impact,
        @Param("since") Instant since);
    
    /**
     * Get fresh news (last hour)
     */
    @Query("SELECT n FROM MarketNews n WHERE n.publishedAt >= :since " +
           "ORDER BY n.publishedAt DESC")
    List<MarketNews> findFreshNews(@Param("since") Instant since);
    
    /**
     * Get verified news only
     */
    @Query("SELECT n FROM MarketNews n WHERE n.isVerified = true " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    List<MarketNews> findVerifiedNews(@Param("since") Instant since);
    
    /**
     * Get high quality news
     */
    @Query("SELECT n FROM MarketNews n WHERE n.qualityScore >= :minQuality " +
           "AND n.isDuplicate = false " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.qualityScore DESC, n.relevanceScore DESC")
    List<MarketNews> findHighQualityNews(
        @Param("minQuality") BigDecimal minQuality,
        @Param("since") Instant since);
    
    /**
     * Complex filtering query
     */
    @Query("SELECT n FROM MarketNews n WHERE " +
           "(:categories IS NULL OR n.category IN :categories) " +
           "AND (:sources IS NULL OR n.source IN :sources) " +
           "AND (:regions IS NULL OR n.region IN :regions) " +
           "AND (:minRelevance IS NULL OR n.relevanceScore >= :minRelevance) " +
           "AND (:minImpact IS NULL OR n.impactScore >= :minImpact) " +
           "AND (:minSentiment IS NULL OR n.sentimentScore >= :minSentiment) " +
           "AND (:maxSentiment IS NULL OR n.sentimentScore <= :maxSentiment) " +
           "AND (:trending IS NULL OR n.isTrending = :trending) " +
           "AND (:breaking IS NULL OR n.isBreakingNews = :breaking) " +
           "AND (:marketMoving IS NULL OR n.isMarketMoving = :marketMoving) " +
           "AND n.publishedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY n.relevanceScore DESC, n.publishedAt DESC")
    Page<MarketNews> findNewsWithFilters(
        @Param("categories") List<String> categories,
        @Param("sources") List<String> sources,
        @Param("regions") List<String> regions,
        @Param("minRelevance") BigDecimal minRelevance,
        @Param("minImpact") BigDecimal minImpact,
        @Param("minSentiment") BigDecimal minSentiment,
        @Param("maxSentiment") BigDecimal maxSentiment,
        @Param("trending") Boolean trending,
        @Param("breaking") Boolean breaking,
        @Param("marketMoving") Boolean marketMoving,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable);
    
    /**
     * Get sentiment distribution
     */
    @Query("SELECT n.sentimentLabel, COUNT(n) FROM MarketNews n " +
           "WHERE n.publishedAt >= :since " +
           "GROUP BY n.sentimentLabel")
    List<Object[]> getSentimentDistribution(@Param("since") Instant since);
    
    /**
     * Get category statistics
     */
    @Query("SELECT n.category, COUNT(n), AVG(n.relevanceScore), AVG(n.sentimentScore) " +
           "FROM MarketNews n WHERE n.publishedAt >= :since " +
           "GROUP BY n.category ORDER BY COUNT(n) DESC")
    List<Object[]> getCategoryStatistics(@Param("since") Instant since);
    
    /**
     * Get source statistics
     */
    @Query("SELECT n.source, COUNT(n), AVG(n.relevanceScore), AVG(n.qualityScore) " +
           "FROM MarketNews n WHERE n.publishedAt >= :since " +
           "GROUP BY n.source ORDER BY COUNT(n) DESC")
    List<Object[]> getSourceStatistics(@Param("since") Instant since);
    
    /**
     * Get hourly news volume
     */
    @Query("SELECT DATE_TRUNC('hour', n.publishedAt), COUNT(n) " +
           "FROM MarketNews n WHERE n.publishedAt >= :since " +
           "GROUP BY DATE_TRUNC('hour', n.publishedAt) " +
           "ORDER BY DATE_TRUNC('hour', n.publishedAt)")
    List<Object[]> getHourlyNewsVolume(@Param("since") Instant since);
    
    /**
     * Find duplicate candidates
     */
    @Query("SELECT n.title, COUNT(n) FROM MarketNews n " +
           "WHERE n.publishedAt >= :since " +
           "GROUP BY n.title HAVING COUNT(n) > 1")
    List<Object[]> findDuplicateCandidates(@Param("since") Instant since);
    
    /**
     * Get news requiring processing updates
     */
    @Query("SELECT n FROM MarketNews n WHERE " +
           "(n.sentimentScore IS NULL OR n.relevanceScore IS NULL OR n.impactScore IS NULL) " +
           "AND n.publishedAt >= :since " +
           "ORDER BY n.publishedAt DESC")
    List<MarketNews> findNewsRequiringProcessing(@Param("since") Instant since);
    
    /**
     * Get average sentiment by time period
     */
    @Query("SELECT DATE_TRUNC('day', n.publishedAt), AVG(n.sentimentScore), COUNT(n) " +
           "FROM MarketNews n WHERE n.publishedAt BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE_TRUNC('day', n.publishedAt) " +
           "ORDER BY DATE_TRUNC('day', n.publishedAt)")
    List<Object[]> getDailySentimentTrend(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get most mentioned symbols in news
     */
    @Query(value = "SELECT symbol, COUNT(*) as mention_count " +
           "FROM (SELECT unnest(string_to_array(REPLACE(REPLACE(related_symbols, '[', ''), ']', ''), ',')) as symbol " +
           "FROM market_news WHERE published_at >= :since) symbols " +
           "WHERE symbol IS NOT NULL AND TRIM(BOTH '\"' FROM symbol) != '' " +
           "GROUP BY symbol ORDER BY COUNT(*) DESC LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> getMostMentionedSymbols(@Param("since") Instant since, @Param("limit") Integer limit);
    
    /**
     * Get trending topics
     */
    @Query(value = "SELECT tag, COUNT(*) as tag_count " +
           "FROM (SELECT unnest(string_to_array(REPLACE(REPLACE(tags, '[', ''), ']', ''), ',')) as tag " +
           "FROM market_news WHERE published_at >= :since) tags " +
           "WHERE tag IS NOT NULL AND TRIM(BOTH '\"' FROM tag) != '' " +
           "GROUP BY tag ORDER BY COUNT(*) DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> getTrendingTopics(@Param("since") Instant since, @Param("limit") Integer limit);
    
    /**
     * Get news volume by hour of day
     */
    @Query("SELECT EXTRACT(HOUR FROM n.publishedAt), COUNT(n) " +
           "FROM MarketNews n WHERE n.publishedAt >= :since " +
           "GROUP BY EXTRACT(HOUR FROM n.publishedAt) " +
           "ORDER BY EXTRACT(HOUR FROM n.publishedAt)")
    List<Object[]> getNewsVolumeByHour(@Param("since") Instant since);
}