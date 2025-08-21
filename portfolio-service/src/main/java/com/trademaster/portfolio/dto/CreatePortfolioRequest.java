package com.trademaster.portfolio.dto;

import com.trademaster.portfolio.model.AccountType;
import com.trademaster.portfolio.model.RiskLevel;

import java.math.BigDecimal;

/**
 * Create Portfolio Request DTO
 * 
 * Data transfer object for portfolio creation requests.
 * Contains all necessary information to create a new portfolio for a user.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record CreatePortfolioRequest(
    String portfolioName,
    BigDecimal initialCashBalance,
    RiskLevel riskLevel,
    String currency,
    AccountType accountType,
    Boolean marginEnabled
) {
    public CreatePortfolioRequest {
        if (portfolioName == null || portfolioName.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be null or empty");
        }
        if (initialCashBalance == null || initialCashBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial cash balance must be non-negative");
        }
        if (riskLevel == null) {
            throw new IllegalArgumentException("Risk level cannot be null");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }
        if (marginEnabled == null) {
            marginEnabled = false;
        }
    }
}