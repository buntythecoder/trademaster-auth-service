package com.trademaster.marketdata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Market Impact Analysis Service
 * 
 * Analyzes the market impact of economic events and news.
 * Provides impact scoring and correlation analysis.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketImpactAnalysisService {

    /**
     * Analyzes the potential market impact of an economic event
     */
    public double analyzeEconomicEventImpact(String eventType, String region, double previousValue, double forecastValue) {
        // Basic impact analysis based on deviation from forecast
        if (forecastValue == 0) {
            return 0.5; // Moderate impact when no forecast available
        }
        
        double deviationPercent = Math.abs((previousValue - forecastValue) / forecastValue) * 100;
        
        // Higher deviation means higher potential market impact
        if (deviationPercent > 10) return 1.0;      // High impact
        if (deviationPercent > 5) return 0.8;       // Medium-high impact
        if (deviationPercent > 2) return 0.6;       // Medium impact
        if (deviationPercent > 1) return 0.4;       // Low-medium impact
        return 0.2; // Low impact
    }

    /**
     * Calculates market correlation for economic events
     */
    public double calculateMarketCorrelation(String eventType, String market) {
        // Basic correlation mapping - in real implementation would use historical data
        return switch (eventType.toLowerCase()) {
            case "gdp", "employment", "inflation" -> 0.8; // High correlation
            case "retail_sales", "industrial_production" -> 0.6; // Medium correlation
            case "housing_data" -> 0.4; // Low-medium correlation
            default -> 0.3; // Low correlation
        };
    }

    /**
     * Provides market impact assessment
     */
    public String assessMarketImpact(double impactScore) {
        if (impactScore >= 0.8) return "HIGH";
        if (impactScore >= 0.6) return "MEDIUM_HIGH";
        if (impactScore >= 0.4) return "MEDIUM";
        if (impactScore >= 0.2) return "LOW_MEDIUM";
        return "LOW";
    }
}