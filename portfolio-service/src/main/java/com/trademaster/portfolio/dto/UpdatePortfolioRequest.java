package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.RiskLevel;

/**
 * Update Portfolio Request DTO
 * 
 * Data transfer object for portfolio update requests.
 * Contains fields that can be modified after portfolio creation.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record UpdatePortfolioRequest(
    String portfolioName,
    RiskLevel riskLevel
) {
    public UpdatePortfolioRequest {
        // Allow null values for optional updates
        if (portfolioName != null && portfolioName.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty if provided");
        }
    }
}