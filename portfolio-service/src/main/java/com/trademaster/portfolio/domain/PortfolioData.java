package com.trademaster.portfolio.domain;

import com.trademaster.portfolio.model.CostBasisMethod;
import com.trademaster.portfolio.model.PortfolioStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Value Object (Immutable Record)
 * 
 * Follows Rules #3 (Functional Programming) and #9 (Immutability & Records Usage).
 * Replaces mutable Portfolio entity with functional immutable record.
 * 
 * Features:
 * - Immutable data structure with validation
 * - Builder pattern for complex construction
 * - Functional methods for calculations
 * - No setters or mutable operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Functional Programming)
 */
public record PortfolioData(
    Long portfolioId,
    Long userId,
    String portfolioName,
    String currency,
    BigDecimal totalValue,
    BigDecimal cashBalance,
    BigDecimal totalCost,
    BigDecimal realizedPnl,
    BigDecimal unrealizedPnl,
    BigDecimal dayPnl,
    PortfolioStatus status,
    CostBasisMethod costBasisMethod,
    BigDecimal marginBalance,
    BigDecimal buyingPower,
    Integer dayTradesCount,
    Instant lastValuationAt,
    Instant lastPnlCalculationAt,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
    
    /**
     * Compact constructor with validation
     */
    public PortfolioData {
        // Validate required fields using functional approach
        validateNonNull(portfolioName, "Portfolio name cannot be null");
        validateNonNull(currency, "Currency cannot be null");
        validateNonNull(status, "Status cannot be null");
        validateNonNull(costBasisMethod, "Cost basis method cannot be null");
        
        // Set defaults for null values using functional approach
        totalValue = defaultIfNull(totalValue, BigDecimal.ZERO);
        cashBalance = defaultIfNull(cashBalance, BigDecimal.ZERO);
        totalCost = defaultIfNull(totalCost, BigDecimal.ZERO);
        realizedPnl = defaultIfNull(realizedPnl, BigDecimal.ZERO);
        unrealizedPnl = defaultIfNull(unrealizedPnl, BigDecimal.ZERO);
        dayPnl = defaultIfNull(dayPnl, BigDecimal.ZERO);
        marginBalance = defaultIfNull(marginBalance, BigDecimal.ZERO);
        buyingPower = defaultIfNull(buyingPower, BigDecimal.ZERO);
        dayTradesCount = defaultIfNull(dayTradesCount, 0);
        
        // Validate business rules
        validateCurrency(currency);
        validatePositiveAmount(cashBalance, "Cash balance cannot be negative");
    }
    
    /**
     * Builder for complex construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Calculate total portfolio value (functional approach)
     */
    public BigDecimal calculateTotalValue() {
        return cashBalance.add(totalValue.subtract(cashBalance));
    }
    
    /**
     * Calculate total P&L (functional approach)
     */
    public BigDecimal calculateTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }
    
    /**
     * Calculate total return percentage (functional approach)
     */
    public BigDecimal calculateTotalReturnPercent() {
        return switch (totalCost.compareTo(BigDecimal.ZERO)) {
            case 0, -1 -> BigDecimal.ZERO;
            default -> calculateTotalPnl()
                .divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        };
    }
    
    /**
     * Check if portfolio allows trading (functional approach)
     */
    public boolean canTrade() {
        return status.allowsTrading();
    }
    
    /**
     * Check if portfolio is margin eligible (functional approach)
     */
    public boolean isMarginEligible() {
        return marginBalance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if portfolio is subject to PDT rule (functional approach)
     */
    public boolean isPatternDayTrader() {
        return dayTradesCount >= 4;
    }
    
    /**
     * Get positions count (functional approach)
     * Note: This is a simplified calculation - in real implementation would 
     * count actual positions from position holdings
     */
    public int getPositionsCount() {
        // Simplified: return a default value based on portfolio activity
        return status == PortfolioStatus.ACTIVE && totalValue.compareTo(BigDecimal.ZERO) > 0 ? 5 : 0;
    }
    
    /**
     * Update portfolio valuation (immutable approach)
     */
    public PortfolioData withValuation(BigDecimal newTotalValue, BigDecimal newUnrealizedPnl) {
        return new PortfolioData(
            portfolioId, userId, portfolioName, currency,
            newTotalValue, cashBalance, totalCost, realizedPnl, newUnrealizedPnl, dayPnl,
            status, costBasisMethod, marginBalance, buyingPower, dayTradesCount,
            Instant.now(), lastPnlCalculationAt, createdAt, Instant.now(), version
        );
    }
    
    /**
     * Add realized P&L (immutable approach)
     */
    public PortfolioData withAddedRealizedPnl(BigDecimal additionalPnl) {
        return new PortfolioData(
            portfolioId, userId, portfolioName, currency,
            totalValue, cashBalance.add(additionalPnl), totalCost, 
            realizedPnl.add(additionalPnl), unrealizedPnl, dayPnl,
            status, costBasisMethod, marginBalance, buyingPower, dayTradesCount,
            lastValuationAt, Instant.now(), createdAt, Instant.now(), version
        );
    }
    
    /**
     * Update cash balance (immutable approach)
     */
    public PortfolioData withCashBalanceUpdate(BigDecimal amount) {
        BigDecimal newCashBalance = cashBalance.add(amount);
        validatePositiveAmount(newCashBalance, "Insufficient cash balance for withdrawal");
        
        return new PortfolioData(
            portfolioId, userId, portfolioName, currency,
            totalValue.add(amount), newCashBalance, totalCost, realizedPnl, unrealizedPnl, dayPnl,
            status, costBasisMethod, marginBalance, buyingPower, dayTradesCount,
            lastValuationAt, lastPnlCalculationAt, createdAt, Instant.now(), version
        );
    }
    
    /**
     * Update status (immutable approach)
     */
    public PortfolioData withStatus(PortfolioStatus newStatus) {
        return new PortfolioData(
            portfolioId, userId, portfolioName, currency,
            totalValue, cashBalance, totalCost, realizedPnl, unrealizedPnl, dayPnl,
            newStatus, costBasisMethod, marginBalance, buyingPower, dayTradesCount,
            lastValuationAt, lastPnlCalculationAt, createdAt, Instant.now(), version
        );
    }
    
    /**
     * Reset day trades count (immutable approach)
     */
    public PortfolioData withResetDayTrades() {
        return new PortfolioData(
            portfolioId, userId, portfolioName, currency,
            totalValue, cashBalance, totalCost, realizedPnl, unrealizedPnl, dayPnl,
            status, costBasisMethod, marginBalance, buyingPower, 0,
            lastValuationAt, lastPnlCalculationAt, createdAt, Instant.now(), version
        );
    }
    
    // Functional validation helper methods
    private static void validateNonNull(Object value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    private static void validateCurrency(String currency) {
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be 3 characters: " + currency);
        }
    }
    
    private static void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message + ": " + amount);
        }
    }
    
    /**
     * Builder for PortfolioData construction
     */
    public static class Builder {
        private Long portfolioId;
        private Long userId;
        private String portfolioName;
        private String currency = "INR";
        private BigDecimal totalValue = BigDecimal.ZERO;
        private BigDecimal cashBalance = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;
        private BigDecimal unrealizedPnl = BigDecimal.ZERO;
        private BigDecimal dayPnl = BigDecimal.ZERO;
        private PortfolioStatus status = PortfolioStatus.ACTIVE;
        private CostBasisMethod costBasisMethod = CostBasisMethod.FIFO;
        private BigDecimal marginBalance = BigDecimal.ZERO;
        private BigDecimal buyingPower = BigDecimal.ZERO;
        private Integer dayTradesCount = 0;
        private Instant lastValuationAt;
        private Instant lastPnlCalculationAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Long version = 0L;
        
        public Builder portfolioId(Long portfolioId) {
            this.portfolioId = portfolioId;
            return this;
        }
        
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder portfolioName(String portfolioName) {
            this.portfolioName = portfolioName;
            return this;
        }
        
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder totalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }
        
        public Builder cashBalance(BigDecimal cashBalance) {
            this.cashBalance = cashBalance;
            return this;
        }
        
        public Builder totalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
            return this;
        }
        
        public Builder realizedPnl(BigDecimal realizedPnl) {
            this.realizedPnl = realizedPnl;
            return this;
        }
        
        public Builder unrealizedPnl(BigDecimal unrealizedPnl) {
            this.unrealizedPnl = unrealizedPnl;
            return this;
        }
        
        public Builder dayPnl(BigDecimal dayPnl) {
            this.dayPnl = dayPnl;
            return this;
        }
        
        public Builder status(PortfolioStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder costBasisMethod(CostBasisMethod costBasisMethod) {
            this.costBasisMethod = costBasisMethod;
            return this;
        }
        
        public Builder marginBalance(BigDecimal marginBalance) {
            this.marginBalance = marginBalance;
            return this;
        }
        
        public Builder buyingPower(BigDecimal buyingPower) {
            this.buyingPower = buyingPower;
            return this;
        }
        
        public Builder dayTradesCount(Integer dayTradesCount) {
            this.dayTradesCount = dayTradesCount;
            return this;
        }
        
        public Builder lastValuationAt(Instant lastValuationAt) {
            this.lastValuationAt = lastValuationAt;
            return this;
        }
        
        public Builder lastPnlCalculationAt(Instant lastPnlCalculationAt) {
            this.lastPnlCalculationAt = lastPnlCalculationAt;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Builder version(Long version) {
            this.version = version;
            return this;
        }
        
        public PortfolioData build() {
            Instant now = Instant.now();
            return new PortfolioData(
                portfolioId, userId, portfolioName, currency,
                totalValue, cashBalance, totalCost, realizedPnl, unrealizedPnl, dayPnl,
                status, costBasisMethod, marginBalance, buyingPower, dayTradesCount,
                defaultIfNull(lastValuationAt, now),
                lastPnlCalculationAt,
                defaultIfNull(createdAt, now),
                defaultIfNull(updatedAt, now),
                version
            );
        }
    }
}