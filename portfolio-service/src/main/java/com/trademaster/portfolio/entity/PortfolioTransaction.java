package com.trademaster.portfolio.entity;

import com.trademaster.portfolio.model.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Transaction Entity
 * 
 * Represents all transactions that affect a portfolio including:
 * - Trade executions (buy/sell/short/cover)
 * - Cash movements (deposits/withdrawals) 
 * - Corporate actions (splits/dividends)
 * - Fees and interest
 * 
 * Provides complete audit trail for portfolio changes and P&L calculations.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "portfolio_transactions", indexes = {
    @Index(name = "idx_transaction_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_transaction_order_id", columnList = "order_id"),
    @Index(name = "idx_transaction_symbol", columnList = "symbol"),
    @Index(name = "idx_transaction_type", columnList = "transaction_type"),
    @Index(name = "idx_transaction_executed_at", columnList = "executed_at"),
    @Index(name = "idx_transaction_portfolio_date", columnList = "portfolio_id, executed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "transactionId")
@ToString(exclude = "portfolio")
public class PortfolioTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;
    
    @Column(name = "portfolio_id", nullable = false, insertable = false, updatable = false)
    private Long portfolioId;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "trade_id")
    private String tradeId;
    
    @Column(name = "symbol", length = 20)
    private String symbol;
    
    @Column(name = "exchange", length = 10)
    private String exchange;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "price", precision = 12, scale = 4)
    private BigDecimal price;
    
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "commission", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal commission = BigDecimal.ZERO;
    
    @Column(name = "tax", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;
    
    @Column(name = "other_fees", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal otherFees = BigDecimal.ZERO;
    
    @Column(name = "net_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal netAmount;
    
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";
    
    @Column(name = "settlement_date")
    private Instant settlementDate;
    
    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
    
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "realized_pnl", precision = 19, scale = 4)
    private BigDecimal realizedPnl;
    
    @Column(name = "cost_basis", precision = 12, scale = 4)
    private BigDecimal costBasis;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;
    
    // Business methods
    
    /**
     * Calculate net cash impact of transaction
     */
    public BigDecimal getCashImpact() {
        if (!transactionType.affectsCash()) {
            return BigDecimal.ZERO;
        }
        
        return switch (transactionType) {
            case BUY, BUY_TO_COVER, FEE, WITHDRAWAL -> netAmount.negate();
            case SELL, SHORT_SELL, DIVIDEND, INTEREST, DEPOSIT -> netAmount;
            default -> BigDecimal.ZERO;
        };
    }
    
    /**
     * Calculate quantity impact of transaction
     */
    public Integer getQuantityImpact() {
        if (!transactionType.affectsQuantity()) {
            return 0;
        }
        
        return switch (transactionType) {
            case BUY, STOCK_DIVIDEND -> quantity;
            case SELL -> -quantity;
            case SHORT_SELL -> -quantity; // Negative for short positions
            case BUY_TO_COVER -> quantity; // Positive to reduce short position
            case SPLIT -> calculateSplitQuantityChange();
            default -> 0;
        };
    }
    
    /**
     * Check if transaction generates realized P&L
     */
    public boolean generatesRealizedPnl() {
        return transactionType.realizesGains() && realizedPnl != null;
    }
    
    /**
     * Get total transaction cost including fees
     */
    public BigDecimal getTotalCost() {
        return amount.add(commission).add(tax).add(otherFees);
    }
    
    /**
     * Check if this is a trade execution transaction
     */
    public boolean isTradeExecution() {
        return transactionType.isTradeExecution();
    }
    
    /**
     * Check if this is a corporate action
     */
    public boolean isCorporateAction() {
        return transactionType.isCorporateAction();
    }
    
    /**
     * Get position direction multiplier
     */
    public int getDirectionMultiplier() {
        return switch (transactionType) {
            case BUY, BUY_TO_COVER, STOCK_DIVIDEND -> 1;
            case SELL, SHORT_SELL -> -1;
            default -> 0;
        };
    }
    
    /**
     * Calculate transaction P&L if selling
     */
    public BigDecimal calculatePnl(BigDecimal averageCostBasis) {
        if (!transactionType.realizesGains() || costBasis == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal saleValue = price.multiply(new BigDecimal(quantity));
        BigDecimal costValue = averageCostBasis.multiply(new BigDecimal(quantity));
        
        return saleValue.subtract(costValue).subtract(commission).subtract(tax).subtract(otherFees);
    }
    
    /**
     * Set net amount based on gross amount and fees
     */
    public void calculateNetAmount() {
        this.netAmount = this.amount.subtract(commission).subtract(tax).subtract(otherFees);
    }
    
    /**
     * Create transaction from trade execution
     */
    public static PortfolioTransaction fromTradeExecution(
            Portfolio portfolio, 
            Long orderId, 
            String tradeId,
            String symbol, 
            String exchange,
            TransactionType type,
            Integer quantity, 
            BigDecimal price,
            BigDecimal commission,
            Instant executedAt) {
        
        BigDecimal amount = price.multiply(new BigDecimal(quantity));
        BigDecimal netAmount = amount.subtract(commission);
        
        return PortfolioTransaction.builder()
            .portfolio(portfolio)
            .portfolioId(portfolio.getPortfolioId())
            .orderId(orderId)
            .tradeId(tradeId)
            .symbol(symbol)
            .exchange(exchange)
            .transactionType(type)
            .quantity(quantity)
            .price(price)
            .amount(amount)
            .commission(commission)
            .netAmount(netAmount)
            .currency(portfolio.getCurrency())
            .executedAt(executedAt)
            .settlementDate(executedAt.plusSeconds(2 * 24 * 60 * 60)) // T+2 settlement
            .description(String.format("%s %d %s @ %s", type.getValue(), quantity, symbol, price))
            .build();
    }
    
    /**
     * Create cash transaction (deposit/withdrawal)
     */
    public static PortfolioTransaction createCashTransaction(
            Portfolio portfolio,
            TransactionType type,
            BigDecimal amount,
            String description) {
        
        return PortfolioTransaction.builder()
            .portfolio(portfolio)
            .portfolioId(portfolio.getPortfolioId())
            .transactionType(type)
            .amount(amount)
            .netAmount(amount)
            .currency(portfolio.getCurrency())
            .executedAt(Instant.now())
            .settlementDate(Instant.now())
            .description(description)
            .build();
    }
    
    /**
     * Create dividend transaction
     */
    public static PortfolioTransaction createDividendTransaction(
            Portfolio portfolio,
            String symbol,
            BigDecimal dividendPerShare,
            Integer shares,
            Instant paymentDate) {
        
        BigDecimal totalDividend = dividendPerShare.multiply(new BigDecimal(shares));
        
        return PortfolioTransaction.builder()
            .portfolio(portfolio)
            .portfolioId(portfolio.getPortfolioId())
            .symbol(symbol)
            .transactionType(TransactionType.DIVIDEND)
            .quantity(shares)
            .price(dividendPerShare)
            .amount(totalDividend)
            .netAmount(totalDividend)
            .currency(portfolio.getCurrency())
            .executedAt(paymentDate)
            .settlementDate(paymentDate)
            .description(String.format("Dividend payment for %d shares of %s @ %s per share", 
                shares, symbol, dividendPerShare))
            .build();
    }
    
    private Integer calculateSplitQuantityChange() {
        // For stock splits, would need additional split ratio data
        // This is a simplified implementation
        return quantity != null ? quantity : 0;
    }
}