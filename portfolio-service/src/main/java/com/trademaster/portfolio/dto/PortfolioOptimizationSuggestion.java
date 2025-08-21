package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio Optimization Suggestion DTO
 * 
 * Data transfer object containing portfolio optimization recommendations.
 * Provides actionable suggestions for improving portfolio performance and risk profile.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record PortfolioOptimizationSuggestion(
    String suggestionId,
    Long portfolioId,
    String suggestionType,
    String category,
    String title,
    String description,
    String rationale,
    BigDecimal currentValue,
    BigDecimal targetValue,
    BigDecimal expectedImprovement,
    BigDecimal confidenceScore,
    String priority,
    List<OptimizationAction> actions,
    List<String> risks,
    List<String> benefits,
    BigDecimal implementationCost,
    String timeframe,
    Instant validUntil,
    Instant generatedAt
) {}

/**
 * Optimization Action DTO
 */
record OptimizationAction(
    String actionType,
    String symbol,
    String action,
    Integer quantity,
    BigDecimal targetAllocation,
    BigDecimal expectedImpact,
    String reasoning
) {}