package com.trademaster.marketdata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentiment Analysis Service
 * 
 * Analyzes market sentiment from various sources including:
 * - News articles
 * - Social media
 * - Market reports
 * - Trading activity
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SentimentAnalysisService {

    /**
     * Analyzes sentiment from text content
     */
    public double analyzeSentiment(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0.0; // Neutral sentiment
        }

        // Basic sentiment analysis using keyword matching
        // In production, would use ML models or external APIs
        String lowerContent = content.toLowerCase();
        
        double positiveScore = countPositiveKeywords(lowerContent);
        double negativeScore = countNegativeKeywords(lowerContent);
        double totalWords = content.split("\\s+").length;
        
        if (totalWords == 0) return 0.0;
        
        // Calculate sentiment score from -1.0 (very negative) to 1.0 (very positive)
        return (positiveScore - negativeScore) / totalWords;
    }

    /**
     * Calculates market sentiment score for a symbol
     */
    public Map<String, Object> calculateMarketSentiment(String symbol) {
        // Mock implementation - would aggregate from multiple sources
        double overallSentiment = Math.random() * 2 - 1; // Random between -1 and 1
        
        return Map.of(
            "symbol", symbol,
            "sentimentScore", overallSentiment,
            "sentimentCategory", categorizeSentiment(overallSentiment),
            "confidence", 0.75,
            "sourcesCount", 150,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Analyzes news sentiment impact on market
     */
    public double analyzeNewsImpact(String newsContent, String symbol) {
        double baseSentiment = analyzeSentiment(newsContent);
        
        // Adjust impact based on content relevance and urgency
        double urgencyMultiplier = detectUrgency(newsContent);
        double relevanceMultiplier = calculateRelevance(newsContent, symbol);
        
        return baseSentiment * urgencyMultiplier * relevanceMultiplier;
    }

    /**
     * Categorizes sentiment score into human-readable categories
     */
    public String categorizeSentiment(double sentimentScore) {
        if (sentimentScore >= 0.6) return "VERY_POSITIVE";
        if (sentimentScore >= 0.2) return "POSITIVE";
        if (sentimentScore >= -0.2) return "NEUTRAL";
        if (sentimentScore >= -0.6) return "NEGATIVE";
        return "VERY_NEGATIVE";
    }

    private double countPositiveKeywords(String content) {
        String[] positiveKeywords = {
            "bullish", "positive", "growth", "profit", "gain", "increase", 
            "strong", "excellent", "good", "buy", "opportunity", "upgrade"
        };
        
        double count = 0;
        for (String keyword : positiveKeywords) {
            count += countOccurrences(content, keyword);
        }
        return count;
    }

    private double countNegativeKeywords(String content) {
        String[] negativeKeywords = {
            "bearish", "negative", "decline", "loss", "decrease", "fall",
            "weak", "poor", "bad", "sell", "risk", "downgrade", "crash"
        };
        
        double count = 0;
        for (String keyword : negativeKeywords) {
            count += countOccurrences(content, keyword);
        }
        return count;
    }

    private int countOccurrences(String content, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private double detectUrgency(String content) {
        String[] urgentKeywords = {"breaking", "urgent", "alert", "immediate", "crisis"};
        for (String keyword : urgentKeywords) {
            if (content.toLowerCase().contains(keyword)) {
                return 1.5; // Increase impact for urgent news
            }
        }
        return 1.0;
    }

    private double calculateRelevance(String content, String symbol) {
        // Check if symbol or company name is mentioned
        if (content.toLowerCase().contains(symbol.toLowerCase())) {
            return 1.2; // Higher relevance when symbol is mentioned
        }
        return 0.8; // Lower relevance for general news
    }
}