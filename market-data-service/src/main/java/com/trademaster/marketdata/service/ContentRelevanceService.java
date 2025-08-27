package com.trademaster.marketdata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Content Relevance Service
 * 
 * Determines the relevance of news content to specific symbols or markets.
 * Uses various algorithms to score content relevance and filter out noise.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentRelevanceService {

    private static final Set<String> MARKET_KEYWORDS = Set.of(
        "market", "trading", "stock", "share", "equity", "bond", "commodity",
        "forex", "currency", "futures", "options", "derivative", "index",
        "portfolio", "investment", "investor", "trader", "analyst", "earnings",
        "revenue", "profit", "loss", "dividend", "merger", "acquisition"
    );

    /**
     * Calculates relevance score for content against a specific symbol
     */
    public double calculateRelevanceScore(String content, String symbol) {
        if (content == null || content.trim().isEmpty() || symbol == null) {
            return 0.0;
        }

        String lowerContent = content.toLowerCase();
        String lowerSymbol = symbol.toLowerCase();
        double relevanceScore = 0.0;

        // Direct symbol mention (highest relevance)
        if (lowerContent.contains(lowerSymbol)) {
            relevanceScore += 0.4;
        }

        // Company name extraction and matching (would need company name mapping)
        relevanceScore += calculateCompanyNameRelevance(lowerContent, lowerSymbol);

        // Industry keyword matching
        relevanceScore += calculateIndustryRelevance(lowerContent, symbol);

        // Market keyword density
        relevanceScore += calculateMarketKeywordDensity(lowerContent);

        // Sector relevance
        relevanceScore += calculateSectorRelevance(lowerContent, symbol);

        return Math.min(1.0, relevanceScore); // Cap at 1.0
    }

    /**
     * Filters content based on minimum relevance threshold
     */
    public List<Map<String, Object>> filterRelevantContent(List<Map<String, Object>> contentList, 
                                                           String symbol, double minRelevanceThreshold) {
        return contentList.stream()
            .filter(content -> {
                String text = (String) content.get("content");
                double relevance = calculateRelevanceScore(text, symbol);
                content.put("relevanceScore", relevance);
                return relevance >= minRelevanceThreshold;
            })
            .sorted((a, b) -> Double.compare(
                (Double) b.get("relevanceScore"), 
                (Double) a.get("relevanceScore")
            ))
            .toList();
    }

    /**
     * Categorizes content relevance level
     */
    public String categorizeRelevance(double relevanceScore) {
        if (relevanceScore >= 0.8) return "HIGHLY_RELEVANT";
        if (relevanceScore >= 0.6) return "RELEVANT";
        if (relevanceScore >= 0.4) return "MODERATELY_RELEVANT";
        if (relevanceScore >= 0.2) return "SLIGHTLY_RELEVANT";
        return "NOT_RELEVANT";
    }

    /**
     * Calculates content quality score
     */
    public double calculateContentQuality(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0.0;
        }

        double qualityScore = 0.0;

        // Length factor (not too short, not too long)
        int length = content.length();
        if (length >= 100 && length <= 2000) {
            qualityScore += 0.3;
        } else if (length > 50) {
            qualityScore += 0.15;
        }

        // Sentence structure (basic check for complete sentences)
        int sentenceCount = content.split("[.!?]+").length;
        if (sentenceCount >= 3 && sentenceCount <= 20) {
            qualityScore += 0.2;
        }

        // Market relevance
        qualityScore += calculateMarketKeywordDensity(content.toLowerCase());

        // Avoid spam indicators
        if (!containsSpamIndicators(content)) {
            qualityScore += 0.2;
        }

        return Math.min(1.0, qualityScore);
    }

    private double calculateCompanyNameRelevance(String content, String symbol) {
        // Mock company name mapping - in real implementation would use company database
        Map<String, String> symbolToCompany = Map.of(
            "aapl", "apple",
            "msft", "microsoft",
            "googl", "google",
            "amzn", "amazon",
            "tsla", "tesla"
        );

        String companyName = symbolToCompany.get(symbol);
        if (companyName != null && content.contains(companyName)) {
            return 0.3;
        }
        return 0.0;
    }

    private double calculateIndustryRelevance(String content, String symbol) {
        // Mock industry mapping
        Map<String, List<String>> symbolToIndustryKeywords = Map.of(
            "aapl", List.of("technology", "smartphone", "consumer electronics"),
            "tsla", List.of("automotive", "electric vehicle", "clean energy"),
            "amzn", List.of("e-commerce", "cloud computing", "retail")
        );

        List<String> keywords = symbolToIndustryKeywords.get(symbol.toLowerCase());
        if (keywords == null) return 0.0;

        long matchCount = keywords.stream()
            .mapToLong(keyword -> countOccurrences(content, keyword))
            .sum();

        return Math.min(0.2, matchCount * 0.05);
    }

    private double calculateMarketKeywordDensity(String content) {
        String[] words = content.split("\\s+");
        long marketKeywordCount = 0;

        for (String word : words) {
            if (MARKET_KEYWORDS.contains(word.toLowerCase())) {
                marketKeywordCount++;
            }
        }

        return Math.min(0.3, (double) marketKeywordCount / words.length * 10);
    }

    private double calculateSectorRelevance(String content, String symbol) {
        // Mock sector keywords
        Set<String> sectorKeywords = Set.of(
            "financial", "technology", "healthcare", "energy", "utilities",
            "consumer", "industrial", "materials", "real estate", "telecom"
        );

        long sectorMatches = sectorKeywords.stream()
            .mapToLong(keyword -> countOccurrences(content, keyword))
            .sum();

        return Math.min(0.1, sectorMatches * 0.02);
    }

    private long countOccurrences(String content, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private boolean containsSpamIndicators(String content) {
        String lowerContent = content.toLowerCase();
        String[] spamKeywords = {
            "click here", "buy now", "limited time", "act fast", "guaranteed",
            "make money", "get rich", "free money", "instant", "miracle"
        };

        for (String spam : spamKeywords) {
            if (lowerContent.contains(spam)) {
                return true;
            }
        }

        // Check for excessive capitalization
        long upperCaseCount = content.chars().filter(Character::isUpperCase).count();
        double upperCaseRatio = (double) upperCaseCount / content.length();
        
        return upperCaseRatio > 0.5; // More than 50% uppercase is likely spam
    }
}