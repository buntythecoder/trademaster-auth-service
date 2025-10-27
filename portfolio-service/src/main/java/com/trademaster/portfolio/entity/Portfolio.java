package com.trademaster.portfolio.entity;

import com.trademaster.portfolio.model.CostBasisMethod;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.model.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio Entity
 * 
 * Represents a user's investment portfolio with comprehensive tracking of:
 * - Total portfolio value and cash balance
 * - Realized and unrealized P&L
 * - Portfolio status and configuration
 * - Relationship to positions and transactions
 * 
 * Optimized for Java 24 Virtual Threads with minimal entity overhead.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "portfolios", indexes = {
    @Index(name = "idx_portfolio_user_id", columnList = "user_id"),
    @Index(name = "idx_portfolio_status", columnList = "status"),
    @Index(name = "idx_portfolio_updated_at", columnList = "updated_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "portfolioId")
@ToString(exclude = {"positions", "transactions"})
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long portfolioId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "portfolio_name", nullable = false, length = 100)
    private String portfolioName;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "total_value", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;
    
    @Column(name = "cash_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal cashBalance = BigDecimal.ZERO;
    
    @Column(name = "total_cost", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(name = "realized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;
    
    @Column(name = "unrealized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;
    
    @Column(name = "day_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal dayPnl = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PortfolioStatus status = PortfolioStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_basis_method", nullable = false, length = 20)
    @Builder.Default
    private CostBasisMethod costBasisMethod = CostBasisMethod.FIFO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.MODERATE;
    
    @Column(name = "margin_balance", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal marginBalance = BigDecimal.ZERO;
    
    @Column(name = "buying_power", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal buyingPower = BigDecimal.ZERO;
    
    @Column(name = "day_trades_count", nullable = false)
    @Builder.Default
    private Integer dayTradesCount = 0;
    
    @Column(name = "last_valuation_at")
    private Instant lastValuationAt;
    
    @Column(name = "last_pnl_calculation_at")
    private Instant lastPnlCalculationAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Relationships
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Position> positions = new ArrayList<>();
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortfolioTransaction> transactions = new ArrayList<>();
    
    // Business methods
    
    /**
     * Calculate total portfolio value (cash + positions)
     */
    public BigDecimal calculateTotalValue() {
        BigDecimal positionsValue = positions.stream()
            .map(Position::getMarketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return cashBalance.add(positionsValue);
    }
    
    /**
     * Calculate total P&L (realized + unrealized)
     */
    public BigDecimal getTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }
    
    /**
     * Calculate total return percentage
     */
    public BigDecimal getTotalReturnPercent() {
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return getTotalPnl()
            .divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Update portfolio value and P&L
     */
    public void updateValuation(BigDecimal newTotalValue, BigDecimal newUnrealizedPnl) {
        this.totalValue = newTotalValue;
        this.unrealizedPnl = newUnrealizedPnl;
        this.lastValuationAt = Instant.now();
    }
    
    /**
     * Add realized P&L from position closure
     */
    public void addRealizedPnl(BigDecimal pnl) {
        this.realizedPnl = this.realizedPnl.add(pnl);
        this.lastPnlCalculationAt = Instant.now();
    }
    
    /**
     * Update cash balance
     */
    public void updateCashBalance(BigDecimal amount) {
        this.cashBalance = this.cashBalance.add(amount);
    }
    
    /**
     * Check if portfolio allows trading
     */
    public boolean canTrade() {
        return status.allowsTrading();
    }
    
    /**
     * Check if portfolio is margin eligible
     */
    public boolean isMarginEligible() {
        return marginBalance != null && marginBalance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if portfolio is subject to PDT rule (Pattern Day Trader)
     */
    public boolean isPatternDayTrader() {
        return dayTradesCount >= 4;
    }
    
    /**
     * Reset day trades count (called daily)
     */
    public void resetDayTradesCount() {
        this.dayTradesCount = 0;
    }

    /**
     * Convenience alias for getPortfolioId() for backward compatibility
     *
     * Rule #18: Provide getId() alias for common usage patterns
     */
    public Long getId() {
        return portfolioId;
    }

    /**
     * Get total return (absolute P&L amount)
     *
     * Rule #18: Meaningful method name for total return amount
     */
    public BigDecimal getTotalReturn() {
        return getTotalPnl();
    }

    /**
     * Convenience alias for getMarginBalance() for backward compatibility
     *
     * Rule #18: Provide getMarginUsed() alias for common usage patterns
     */
    public BigDecimal getMarginUsed() {
        return marginBalance;
    }
}