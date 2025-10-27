package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Rebalancing Plan DTO
 *
 * Comprehensive plan for portfolio rebalancing with buy/sell recommendations.
 * Includes current vs target allocation and trade recommendations.
 *
 * Rule #9: Immutable Records for Data Transfer Objects
 * Rule #3: Functional Programming - Immutable data structures
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record RebalancingPlan(
    Long portfolioId,
    String strategy,
    BigDecimal totalPortfolioValue,
    Map<String, AllocationComparison> allocations,
    List<TradeRecommendation> tradeRecommendations,
    BigDecimal estimatedTradingCosts,
    BigDecimal estimatedTaxImpact,
    BigDecimal netRebalancingCost,
    String riskAssessment,
    Instant generatedAt
) {

    /**
     * Allocation comparison for a symbol
     */
    public record AllocationComparison(
        String symbol,
        BigDecimal currentPercentage,
        BigDecimal targetPercentage,
        BigDecimal deviationPercentage,
        BigDecimal currentValue,
        BigDecimal targetValue,
        BigDecimal adjustmentNeeded,
        boolean needsRebalancing
    ) {
        /**
         * Check if deviation exceeds threshold
         * Rule #3: Functional predicate method
         */
        public boolean exceedsThreshold(BigDecimal threshold) {
            return deviationPercentage.abs().compareTo(threshold) > 0;
        }
    }

    /**
     * Trade recommendation for rebalancing
     */
    public record TradeRecommendation(
        String symbol,
        String action, // BUY or SELL
        Integer quantity,
        BigDecimal estimatedPrice,
        BigDecimal estimatedValue,
        BigDecimal estimatedCost,
        BigDecimal taxImpact,
        String reason,
        int priority // 1 = highest priority
    ) {
        /**
         * Check if this is a buy order
         * Rule #3: Functional predicate method
         */
        public boolean isBuy() {
            return "BUY".equalsIgnoreCase(action);
        }

        /**
         * Check if this is a sell order
         * Rule #3: Functional predicate method
         */
        public boolean isSell() {
            return "SELL".equalsIgnoreCase(action);
        }
    }

    /**
     * Factory method for empty plan
     * Rule #4: Factory Pattern with functional construction
     */
    public static RebalancingPlan empty(Long portfolioId, String strategy) {
        return new RebalancingPlan(
            portfolioId,
            strategy,
            BigDecimal.ZERO,
            Map.of(),
            List.of(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "No rebalancing needed - portfolio is within target allocations",
            Instant.now()
        );
    }

    /**
     * Check if rebalancing is needed
     * Rule #3: Functional predicate method
     */
    public boolean requiresRebalancing() {
        return !tradeRecommendations.isEmpty();
    }

    /**
     * Get total buy value
     * Rule #3: Functional stream operation
     */
    public BigDecimal getTotalBuyValue() {
        return tradeRecommendations.stream()
            .filter(TradeRecommendation::isBuy)
            .map(TradeRecommendation::estimatedValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total sell value
     * Rule #3: Functional stream operation
     */
    public BigDecimal getTotalSellValue() {
        return tradeRecommendations.stream()
            .filter(TradeRecommendation::isSell)
            .map(TradeRecommendation::estimatedValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
