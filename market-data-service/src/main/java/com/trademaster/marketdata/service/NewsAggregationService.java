package com.trademaster.marketdata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * News Aggregation Service
 * 
 * Aggregates news from multiple sources and provides unified feed.
 * Handles deduplication, ranking, and filtering of news content.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsAggregationService {

    private final ContentRelevanceService contentRelevanceService;
    private final SentimentAnalysisService sentimentAnalysisService;

    /**
     * Aggregates news from multiple sources
     */
    public List<Map<String, Object>> aggregateNews(String symbol, int maxResults) {
        // Mock news sources - in real implementation would integrate with news APIs
        List<Map<String, Object>> allNews = fetchFromAllSources();
        
        return allNews.stream()
            .filter(news -> {
                String content = (String) news.get("content");
                double relevance = contentRelevanceService.calculateRelevanceScore(content, symbol);
                news.put("relevanceScore", relevance);
                return relevance >= 0.3; // Filter out low relevance news
            })
            .map(news -> enrichNewsWithAnalysis(news, symbol))
            .sorted((a, b) -> {
                // Sort by relevance and recency
                double relevanceA = (Double) a.get("relevanceScore");
                double relevanceB = (Double) b.get("relevanceScore");
                if (relevanceA != relevanceB) {
                    return Double.compare(relevanceB, relevanceA);
                }
                LocalDateTime timeA = (LocalDateTime) a.get("publishedAt");
                LocalDateTime timeB = (LocalDateTime) b.get("publishedAt");
                return timeB.compareTo(timeA);
            })
            .limit(maxResults)
            .toList();
    }

    /**
     * Gets breaking news with high priority
     */
    public List<Map<String, Object>> getBreakingNews(String symbol) {
        return aggregateNews(symbol, 10).stream()
            .filter(news -> {
                String content = (String) news.get("content");
                return isBreakingNews(content);
            })
            .toList();
    }

    /**
     * Aggregates news by time period
     */
    public Map<String, List<Map<String, Object>>> aggregateNewsByTimePeriod(String symbol, 
                                                                            LocalDateTime startTime, 
                                                                            LocalDateTime endTime) {
        List<Map<String, Object>> news = aggregateNews(symbol, 100);
        
        return news.stream()
            .filter(item -> {
                LocalDateTime publishedAt = (LocalDateTime) item.get("publishedAt");
                return publishedAt.isAfter(startTime) && publishedAt.isBefore(endTime);
            })
            .collect(Collectors.groupingBy(item -> {
                LocalDateTime publishedAt = (LocalDateTime) item.get("publishedAt");
                return publishedAt.toLocalDate().toString(); // Group by date
            }));
    }

    /**
     * Gets news summary for a symbol
     */
    public Map<String, Object> getNewsSummary(String symbol) {
        List<Map<String, Object>> recentNews = aggregateNews(symbol, 50);
        
        double avgSentiment = recentNews.stream()
            .mapToDouble(news -> (Double) news.get("sentimentScore"))
            .average()
            .orElse(0.0);
        
        long breakingNewsCount = recentNews.stream()
            .filter(news -> (Boolean) news.get("isBreaking"))
            .count();
        
        double avgRelevance = recentNews.stream()
            .mapToDouble(news -> (Double) news.get("relevanceScore"))
            .average()
            .orElse(0.0);
        
        return Map.of(
            "symbol", symbol,
            "totalNewsCount", recentNews.size(),
            "breakingNewsCount", breakingNewsCount,
            "averageSentiment", avgSentiment,
            "sentimentCategory", sentimentAnalysisService.categorizeSentiment(avgSentiment),
            "averageRelevance", avgRelevance,
            "lastUpdated", LocalDateTime.now(),
            "topNews", recentNews.stream().limit(5).toList()
        );
    }

    /**
     * Removes duplicate news articles
     */
    public List<Map<String, Object>> removeDuplicates(List<Map<String, Object>> newsList) {
        return newsList.stream()
            .collect(Collectors.toMap(
                news -> generateContentHash((String) news.get("title")), 
                news -> news,
                (existing, replacement) -> {
                    // Keep the one with higher relevance score
                    double existingRelevance = (Double) existing.getOrDefault("relevanceScore", 0.0);
                    double replacementRelevance = (Double) replacement.getOrDefault("relevanceScore", 0.0);
                    return existingRelevance >= replacementRelevance ? existing : replacement;
                }
            ))
            .values()
            .stream()
            .toList();
    }

    private List<Map<String, Object>> fetchFromAllSources() {
        // Mock news data - in real implementation would fetch from multiple news APIs
        return List.of(
            createMockNews("Market rallies on positive earnings", "Reuters", LocalDateTime.now().minusHours(1)),
            createMockNews("Technology stocks surge amid AI optimism", "Bloomberg", LocalDateTime.now().minusHours(2)),
            createMockNews("Federal Reserve hints at rate cuts", "Financial Times", LocalDateTime.now().minusHours(3)),
            createMockNews("BREAKING: Major merger announcement", "CNBC", LocalDateTime.now().minusMinutes(30)),
            createMockNews("Quarterly GDP growth exceeds expectations", "Wall Street Journal", LocalDateTime.now().minusHours(4))
        );
    }

    private Map<String, Object> createMockNews(String title, String source, LocalDateTime publishedAt) {
        return Map.of(
            "id", "news_" + System.currentTimeMillis() + "_" + Math.random(),
            "title", title,
            "content", title + ". Lorem ipsum dolor sit amet, consectetur adipiscing elit. Market analysis shows positive trends.",
            "source", source,
            "publishedAt", publishedAt,
            "url", "https://example.com/news/" + title.toLowerCase().replace(" ", "-")
        );
    }

    private Map<String, Object> enrichNewsWithAnalysis(Map<String, Object> news, String symbol) {
        String content = (String) news.get("content");
        
        // Add sentiment analysis
        double sentimentScore = sentimentAnalysisService.analyzeSentiment(content);
        news.put("sentimentScore", sentimentScore);
        news.put("sentimentCategory", sentimentAnalysisService.categorizeSentiment(sentimentScore));
        
        // Add quality score
        double qualityScore = contentRelevanceService.calculateContentQuality(content);
        news.put("qualityScore", qualityScore);
        
        // Add breaking news flag
        news.put("isBreaking", isBreakingNews(content));
        
        return news;
    }

    private boolean isBreakingNews(String content) {
        String lowerContent = content.toLowerCase();
        String[] breakingKeywords = {
            "breaking", "urgent", "alert", "just in", "developing", 
            "flash", "immediate", "emergency", "unprecedented"
        };
        
        for (String keyword : breakingKeywords) {
            if (lowerContent.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String generateContentHash(String content) {
        // Simple hash generation - in real implementation would use proper hashing
        return String.valueOf(content.toLowerCase().trim().hashCode());
    }
}