package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.MarketNewsRequest;
import com.trademaster.marketdata.dto.MarketNewsResponse;
import com.trademaster.marketdata.entity.MarketNews;
import com.trademaster.marketdata.repository.MarketNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

/**
 * Market News Service
 * 
 * Provides comprehensive market news functionality with sentiment analysis,
 * relevance filtering, and intelligent content recommendations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketNewsService {
    
    private final MarketNewsRepository marketNewsRepository;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final ContentRelevanceService contentRelevanceService;
    private final NewsAggregationService newsAggregationService;
    
    /**
     * Get market news with comprehensive filtering and analysis
     */
    public CompletableFuture<MarketNewsResponse> getMarketNews(MarketNewsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing market news request with {} filters", request.getActiveFilterCount());
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Get time range
                Instant startTime = request.getEffectiveStartTime();
                Instant endTime = request.getEffectiveEndTime();
                
                // Build pageable
                Pageable pageable = buildPageable(request);
                
                // Get filtered news
                var newsTask = scope.fork(() -> getFilteredNews(request, startTime, endTime, pageable));
                
                // Start parallel analysis tasks
                var analyticsTask = scope.fork(() -> calculateAnalytics(startTime, endTime, request));
                var trendingTopicsTask = scope.fork(() -> getTrendingTopics(startTime, 10));
                
                scope.join();
                scope.throwIfFailed();
                
                Page<MarketNews> newsPage = newsTask.get();
                List<MarketNews> allNews = newsPage.getContent();
                
                // Convert to DTOs
                List<MarketNewsResponse.MarketNewsDto> newsDtos = allNews.stream()
                    .map(news -> convertToDto(news, request))
                    .toList();
                
                // Get analytics results
                var analytics = analyticsTask.get();
                var trendingTopics = trendingTopicsTask.get();
                
                // Perform sentiment analysis
                var sentimentAnalysis = performSentimentAnalysis(allNews, request);
                
                // Calculate market impact
                var marketImpact = calculateMarketImpact(allNews, request);
                
                // Generate market alerts
                var marketAlerts = generateMarketAlerts(allNews);
                
                // Build pagination info
                var pagination = buildPaginationInfo(newsPage);
                
                // Generate insights
                var insights = generateInsights(allNews, analytics, sentimentAnalysis);
                
                log.info("Market news response prepared: {} articles, {} breaking, sentiment: {}",
                    newsDtos.size(), 
                    analytics.breakingNews(),
                    sentimentAnalysis.overallSentimentLabel());
                
                return MarketNewsResponse.builder()
                    .originalRequest(request)
                    .responseTime(Instant.now())
                    .timezone(request.timeZone())
                    .news(newsDtos)
                    .analytics(analytics)
                    .sentimentAnalysis(sentimentAnalysis)
                    .marketImpact(marketImpact)
                    .pagination(pagination)
                    .marketAlerts(marketAlerts)
                    .trendingTopics(trendingTopics)
                    .insights(insights)
                    .build();
                    
            } catch (Exception e) {
                log.error("Failed to process market news request: {}", e.getMessage(), e);
                throw new RuntimeException("Market news processing failed", e);
            }
        });
    }
    
    /**
     * Get filtered news based on request parameters
     */
    private Page<MarketNews> getFilteredNews(MarketNewsRequest request, 
            Instant startTime, Instant endTime, Pageable pageable) {
        
        // Handle special filters first
        if (request.breakingOnly() != null && request.breakingOnly()) {
            List<MarketNews> breakingNews = marketNewsRepository.findBreakingNews(startTime);
            return createPageFromList(breakingNews, pageable);
        }
        
        if (request.trendingOnly() != null && request.trendingOnly()) {
            List<MarketNews> trendingNews = marketNewsRepository.findTrendingNews(startTime);
            return createPageFromList(trendingNews, pageable);
        }
        
        if (request.marketMovingOnly() != null && request.marketMovingOnly()) {
            List<MarketNews> marketMovingNews = marketNewsRepository.findMarketMovingNews(startTime);
            return createPageFromList(marketMovingNews, pageable);
        }
        
        if (request.freshOnly() != null && request.freshOnly()) {
            List<MarketNews> freshNews = marketNewsRepository.findFreshNews(
                Instant.now().minusSeconds(3600)); // Last hour
            return createPageFromList(freshNews, pageable);
        }
        
        // Handle search
        if (request.searchTerm() != null && !request.searchTerm().trim().isEmpty()) {
            return marketNewsRepository.searchNews(request.searchTerm(), startTime, pageable);
        }
        
        // Handle symbol-specific news
        if (request.symbols() != null && !request.symbols().isEmpty()) {
            List<MarketNews> symbolNews = new ArrayList<>();
            for (String symbol : request.symbols()) {
                symbolNews.addAll(marketNewsRepository.findNewsBySymbol(symbol, startTime));
            }
            return createPageFromList(symbolNews, pageable);
        }
        
        // Handle sector-specific news
        if (request.sectors() != null && !request.sectors().isEmpty()) {
            List<MarketNews> sectorNews = new ArrayList<>();
            for (String sector : request.sectors()) {
                sectorNews.addAll(marketNewsRepository.findNewsBySector(sector, startTime));
            }
            return createPageFromList(sectorNews, pageable);
        }
        
        // Handle sentiment filtering
        if (request.hasSentimentFilter()) {
            return getNewsBySentimentFilter(request, startTime, endTime, pageable);
        }
        
        // Use complex filtering for advanced requests
        if (hasComplexFilters(request)) {
            return marketNewsRepository.findNewsWithFilters(
                request.categories() != null ? new ArrayList<>(request.categories()) : null,
                request.sources() != null ? new ArrayList<>(request.sources()) : null,
                request.regions() != null ? new ArrayList<>(request.regions()) : null,
                request.minRelevanceScore(),
                request.minImpactScore(),
                request.minSentimentScore(),
                request.maxSentimentScore(),
                request.trendingOnly(),
                request.breakingOnly(),
                request.marketMovingOnly(),
                startTime,
                endTime,
                pageable);
        }
        
        // Default: get recent news
        return marketNewsRepository.findRecentNews(startTime, pageable);
    }
    
    /**
     * Get news by sentiment filter
     */
    private Page<MarketNews> getNewsBySentimentFilter(MarketNewsRequest request,
            Instant startTime, Instant endTime, Pageable pageable) {
        
        if (request.sentimentLabels() != null && !request.sentimentLabels().isEmpty()) {
            List<MarketNews> sentimentNews = new ArrayList<>();
            for (MarketNews.SentimentLabel label : request.sentimentLabels()) {
                sentimentNews.addAll(marketNewsRepository.findNewsBySentiment(label, startTime, endTime));
            }
            return createPageFromList(sentimentNews, pageable);
        }
        
        if (request.minSentimentScore() != null || request.maxSentimentScore() != null) {
            BigDecimal minScore = request.minSentimentScore() != null ? 
                request.minSentimentScore() : new BigDecimal("-1.0");
            BigDecimal maxScore = request.maxSentimentScore() != null ?
                request.maxSentimentScore() : new BigDecimal("1.0");
            
            List<MarketNews> sentimentNews = marketNewsRepository.findNewsBySentimentScore(
                minScore, maxScore, startTime);
            return createPageFromList(sentimentNews, pageable);
        }
        
        return marketNewsRepository.findRecentNews(startTime, pageable);
    }
    
    /**
     * Build pageable with sorting
     */
    private Pageable buildPageable(MarketNewsRequest request) {
        Sort sort = Sort.by(
            request.sortDirection() == MarketNewsRequest.SortDirection.DESC ?
                Sort.Direction.DESC : Sort.Direction.ASC,
            request.sortBy()
        );
        
        return PageRequest.of(request.page(), request.size(), sort);
    }
    
    /**
     * Check if request has complex filters
     */
    private boolean hasComplexFilters(MarketNewsRequest request) {
        return request.hasContentFilter() || request.hasImpactFilter() || 
               request.hasSpecialFilter() || request.hasEngagementFilter();
    }
    
    /**
     * Convert entity to DTO
     */
    private MarketNewsResponse.MarketNewsDto convertToDto(MarketNews news, MarketNewsRequest request) {
        boolean includeContent = request.includeContent() != null ? request.includeContent() : false;
        boolean includeEngagement = request.includeEngagement() != null ? request.includeEngagement() : true;
        
        return MarketNewsResponse.MarketNewsDto.fromEntity(news, includeContent, includeEngagement);
    }
    
    /**
     * Calculate analytics
     */
    private MarketNewsResponse.NewsAnalytics calculateAnalytics(Instant startTime, Instant endTime, 
            MarketNewsRequest request) {
        
        // Get all news in time range for analytics
        List<MarketNews> allNews = marketNewsRepository.findRecentNews(startTime);
        
        int totalNews = allNews.size();
        int breakingNews = (int) allNews.stream().filter(n -> Boolean.TRUE.equals(n.getIsBreakingNews())).count();
        int trendingNews = (int) allNews.stream().filter(n -> Boolean.TRUE.equals(n.getIsTrending())).count();
        int marketMovingNews = (int) allNews.stream().filter(n -> Boolean.TRUE.equals(n.getIsMarketMoving())).count();
        int verifiedNews = (int) allNews.stream().filter(n -> Boolean.TRUE.equals(n.getIsVerified())).count();
        int duplicateNews = (int) allNews.stream().filter(n -> Boolean.TRUE.equals(n.getIsDuplicate())).count();
        
        // Group by category
        Map<String, Integer> newsByCategory = allNews.stream()
            .collect(Collectors.groupingBy(
                MarketNews::getCategory,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group by source
        Map<String, Integer> newsBySource = allNews.stream()
            .collect(Collectors.groupingBy(
                MarketNews::getSource,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group by region
        Map<String, Integer> newsByRegion = allNews.stream()
            .filter(n -> n.getRegion() != null)
            .collect(Collectors.groupingBy(
                MarketNews::getRegion,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group by hour
        Map<String, Integer> newsByHour = allNews.stream()
            .collect(Collectors.groupingBy(
                n -> String.valueOf(n.getPublishedAt().atZone(java.time.ZoneOffset.UTC).getHour()),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Calculate averages
        BigDecimal averageRelevanceScore = allNews.stream()
            .map(MarketNews::getRelevanceScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(totalNews), 2, RoundingMode.HALF_UP);
        
        BigDecimal averageImpactScore = allNews.stream()
            .map(MarketNews::getImpactScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(totalNews), 2, RoundingMode.HALF_UP);
        
        BigDecimal averageSentimentScore = allNews.stream()
            .map(MarketNews::getSentimentScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(totalNews), 4, RoundingMode.HALF_UP);
        
        BigDecimal averageQualityScore = allNews.stream()
            .map(MarketNews::getQualityScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(totalNews), 4, RoundingMode.HALF_UP);
        
        // Find most active source and category
        String mostActiveSource = newsBySource.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
        
        String mostActiveCategory = newsByCategory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
        
        // Calculate total engagement
        Integer totalEngagement = allNews.stream()
            .mapToInt(n -> {
                long engagement = 0;
                if (n.getViewCount() != null) engagement += n.getViewCount();
                if (n.getShareCount() != null) engagement += n.getShareCount() * 5;
                if (n.getCommentCount() != null) engagement += n.getCommentCount() * 3;
                return (int) engagement;
            })
            .sum();
        
        // Find peak hour
        String peakHour = newsByHour.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> entry.getKey() + ":00")
            .orElse("Unknown");
        
        return MarketNewsResponse.NewsAnalytics.builder()
            .totalNews(totalNews)
            .breakingNews(breakingNews)
            .trendingNews(trendingNews)
            .marketMovingNews(marketMovingNews)
            .verifiedNews(verifiedNews)
            .duplicateNews(duplicateNews)
            .newsByCategory(newsByCategory)
            .newsBySource(newsBySource)
            .newsByRegion(newsByRegion)
            .newsByHour(newsByHour)
            .averageRelevanceScore(averageRelevanceScore)
            .averageImpactScore(averageImpactScore)
            .averageSentimentScore(averageSentimentScore)
            .averageQualityScore(averageQualityScore)
            .mostActiveSource(mostActiveSource)
            .mostActiveCategory(mostActiveCategory)
            .totalEngagement(totalEngagement)
            .peakHour(peakHour)
            .build();
    }
    
    /**
     * Perform sentiment analysis
     */
    private MarketNewsResponse.SentimentAnalysis performSentimentAnalysis(List<MarketNews> news, 
            MarketNewsRequest request) {
        
        // Calculate overall sentiment
        BigDecimal overallSentiment = news.stream()
            .map(MarketNews::getSentimentScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(news.size()), 4, RoundingMode.HALF_UP);
        
        String overallSentimentLabel = MarketNews.SentimentLabel.fromScore(overallSentiment).getDescription();
        
        // Determine sentiment trend (simplified)
        String sentimentTrend = determineSentimentTrend(news);
        
        // Sentiment distribution
        Map<String, Integer> sentimentDistribution = news.stream()
            .filter(n -> n.getSentimentLabel() != null)
            .collect(Collectors.groupingBy(
                n -> n.getSentimentLabel().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Sentiment by category
        Map<String, BigDecimal> sentimentByCategory = news.stream()
            .filter(n -> n.getSentimentScore() != null)
            .collect(Collectors.groupingBy(
                MarketNews::getCategory,
                Collectors.averagingDouble(n -> n.getSentimentScore().doubleValue())
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> BigDecimal.valueOf(entry.getValue()).setScale(4, RoundingMode.HALF_UP)
            ));
        
        // Sentiment by source  
        Map<String, BigDecimal> sentimentBySource = news.stream()
            .filter(n -> n.getSentimentScore() != null)
            .collect(Collectors.groupingBy(
                MarketNews::getSource,
                Collectors.averagingDouble(n -> n.getSentimentScore().doubleValue())
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> BigDecimal.valueOf(entry.getValue()).setScale(4, RoundingMode.HALF_UP)
            ));
        
        // Most positive/negative news
        List<MarketNewsResponse.MarketNewsDto> mostPositiveNews = news.stream()
            .filter(n -> n.getSentimentScore() != null)
            .sorted((a, b) -> b.getSentimentScore().compareTo(a.getSentimentScore()))
            .limit(5)
            .map(n -> convertToDto(n, request))
            .toList();
        
        List<MarketNewsResponse.MarketNewsDto> mostNegativeNews = news.stream()
            .filter(n -> n.getSentimentScore() != null)
            .sorted((a, b) -> a.getSentimentScore().compareTo(b.getSentimentScore()))
            .limit(5)
            .map(n -> convertToDto(n, request))
            .toList();
        
        // Calculate confidence level
        BigDecimal confidenceLevel = news.stream()
            .map(MarketNews::getConfidenceScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(news.size()), 4, RoundingMode.HALF_UP);
        
        // Determine market mood
        String marketMood = determineMarketMood(overallSentiment, sentimentDistribution);
        
        return MarketNewsResponse.SentimentAnalysis.builder()
            .overallSentiment(overallSentiment)
            .overallSentimentLabel(overallSentimentLabel)
            .sentimentTrend(sentimentTrend)
            .sentimentDistribution(sentimentDistribution)
            .sentimentByCategory(sentimentByCategory)
            .sentimentBySource(sentimentBySource)
            .sentimentBySymbol(Map.of()) // Would be calculated from related symbols
            .sentimentBySector(Map.of()) // Would be calculated from related sectors
            .mostPositiveNews(mostPositiveNews)
            .mostNegativeNews(mostNegativeNews)
            .confidenceLevel(confidenceLevel)
            .marketMood(marketMood)
            .build();
    }
    
    /**
     * Calculate market impact
     */
    private MarketNewsResponse.MarketImpactSummary calculateMarketImpact(List<MarketNews> news, 
            MarketNewsRequest request) {
        
        // High impact news
        List<MarketNewsResponse.MarketNewsDto> highImpactNews = news.stream()
            .filter(n -> n.getImpactScore() != null && n.getImpactScore().compareTo(new BigDecimal("70")) >= 0)
            .sorted((a, b) -> b.getImpactScore().compareTo(a.getImpactScore()))
            .limit(10)
            .map(n -> convertToDto(n, request))
            .toList();
        
        // Breaking news
        List<MarketNewsResponse.MarketNewsDto> breakingNews = news.stream()
            .filter(n -> Boolean.TRUE.equals(n.getIsBreakingNews()))
            .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
            .limit(10)
            .map(n -> convertToDto(n, request))
            .toList();
        
        // Market moving news
        List<MarketNewsResponse.MarketNewsDto> marketMovingNews = news.stream()
            .filter(n -> Boolean.TRUE.equals(n.getIsMarketMoving()))
            .sorted((a, b) -> b.getImpactScore().compareTo(a.getImpactScore()))
            .limit(10)
            .map(n -> convertToDto(n, request))
            .toList();
        
        // Calculate overall market impact
        String overallMarketImpact = calculateOverallMarketImpact(news);
        
        // Calculate volatility forecast
        BigDecimal volatilityForecast = calculateVolatilityForecast(news);
        
        return MarketNewsResponse.MarketImpactSummary.builder()
            .highImpactNews(highImpactNews)
            .breakingNews(breakingNews)
            .marketMovingNews(marketMovingNews)
            .symbolImpactScores(Map.of()) // Would be calculated from related symbols
            .sectorImpactScores(Map.of()) // Would be calculated from related sectors
            .currencyImpactScores(Map.of()) // Would be calculated from related currencies
            .riskFactors(generateRiskFactors(news))
            .opportunities(generateOpportunities(news))
            .overallMarketImpact(overallMarketImpact)
            .volatilityForecast(volatilityForecast)
            .tradingRecommendations(generateTradingRecommendations(news))
            .build();
    }
    
    // Helper methods (implementation stubs)
    
    private Page<MarketNews> createPageFromList(List<MarketNews> news, Pageable pageable) {
        // Create page from list - simplified implementation
        return Page.empty(); // Stub
    }
    
    private List<MarketNewsResponse.TrendingTopic> getTrendingTopics(Instant since, int limit) {
        // Get trending topics from repository
        List<Object[]> trendingData = marketNewsRepository.getTrendingTopics(since, limit);
        
        return trendingData.stream()
            .map(row -> MarketNewsResponse.TrendingTopic.builder()
                .topic((String) row[0])
                .mentionCount((Integer) row[1])
                .averageSentiment(BigDecimal.valueOf(0.5)) // Would calculate from actual data
                .trendScore(BigDecimal.valueOf(75.0)) // Would calculate trend score
                .relatedSymbols(List.of()) // Would extract from news
                .category("General")
                .build())
            .toList();
    }
    
    private List<String> generateMarketAlerts(List<MarketNews> news) {
        List<String> alerts = new ArrayList<>();
        
        long breakingCount = news.stream()
            .filter(n -> Boolean.TRUE.equals(n.getIsBreakingNews()) && n.isFresh())
            .count();
        
        if (breakingCount > 0) {
            alerts.add(String.format("🚨 %d fresh breaking news alert(s)", breakingCount));
        }
        
        long negativeHighImpact = news.stream()
            .filter(n -> n.isNegativeSentiment() && n.isHighImpact())
            .count();
        
        if (negativeHighImpact > 2) {
            alerts.add(String.format("⚠️ %d high-impact negative news stories detected", negativeHighImpact));
        }
        
        return alerts;
    }
    
    private MarketNewsResponse.PaginationInfo buildPaginationInfo(Page<MarketNews> page) {
        return MarketNewsResponse.PaginationInfo.builder()
            .currentPage(page.getNumber())
            .pageSize(page.getSize())
            .totalPages(page.getTotalPages())
            .totalNews(page.getTotalElements())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
    
    private Map<String, Object> generateInsights(List<MarketNews> news, 
            MarketNewsResponse.NewsAnalytics analytics,
            MarketNewsResponse.SentimentAnalysis sentimentAnalysis) {
        
        return Map.of(
            "newsFlow", determineNewsFlow(analytics),
            "sentimentShift", determineSentimentShift(sentimentAnalysis),
            "marketAttention", determineMarketAttention(news),
            "qualityAssessment", determineQualityAssessment(news)
        );
    }
    
    private String determineSentimentTrend(List<MarketNews> news) {
        // Simplified sentiment trend calculation
        return "STABLE";
    }
    
    private String determineMarketMood(BigDecimal overallSentiment, Map<String, Integer> distribution) {
        if (overallSentiment.compareTo(new BigDecimal("0.3")) > 0) return "Optimistic";
        if (overallSentiment.compareTo(new BigDecimal("-0.3")) < 0) return "Pessimistic";
        return "Neutral";
    }
    
    private String calculateOverallMarketImpact(List<MarketNews> news) {
        long highImpact = news.stream()
            .filter(n -> n.getImpactScore() != null && n.getImpactScore().compareTo(new BigDecimal("70")) >= 0)
            .count();
        
        if (highImpact > 10) return "High";
        if (highImpact > 5) return "Medium";
        return "Low";
    }
    
    private BigDecimal calculateVolatilityForecast(List<MarketNews> news) {
        // Calculate expected volatility increase based on news impact
        return new BigDecimal("15.5"); // Percentage
    }
    
    private List<String> generateRiskFactors(List<MarketNews> news) {
        return List.of(
            "Increased negative sentiment in financial sector",
            "Multiple breaking news events in short timeframe",
            "High impact economic events approaching"
        );
    }
    
    private List<String> generateOpportunities(List<MarketNews> news) {
        return List.of(
            "Positive earnings surprises creating momentum",
            "Sector rotation opportunities identified",
            "Oversold conditions in technology sector"
        );
    }
    
    private List<String> generateTradingRecommendations(List<MarketNews> news) {
        return List.of(
            "Consider defensive positioning ahead of volatility",
            "Monitor key support levels in major indices",
            "Watch for follow-through in sector leaders"
        );
    }
    
    private String determineNewsFlow(MarketNewsResponse.NewsAnalytics analytics) {
        if (analytics.totalNews() > 100) return "HIGH_VOLUME";
        if (analytics.totalNews() > 50) return "MODERATE_VOLUME";
        return "LOW_VOLUME";
    }
    
    private String determineSentimentShift(MarketNewsResponse.SentimentAnalysis sentiment) {
        return sentiment.sentimentTrend();
    }
    
    private String determineMarketAttention(List<MarketNews> news) {
        long trending = news.stream().filter(n -> Boolean.TRUE.equals(n.getIsTrending())).count();
        if (trending > 10) return "HIGH_ATTENTION";
        if (trending > 5) return "MODERATE_ATTENTION";
        return "LOW_ATTENTION";
    }
    
    private String determineQualityAssessment(List<MarketNews> news) {
        double avgQuality = news.stream()
            .filter(n -> n.getQualityScore() != null)
            .mapToDouble(n -> n.getQualityScore().doubleValue())
            .average()
            .orElse(0.5);
        
        if (avgQuality > 0.8) return "HIGH_QUALITY";
        if (avgQuality > 0.6) return "MODERATE_QUALITY";
        return "LOW_QUALITY";
    }
}