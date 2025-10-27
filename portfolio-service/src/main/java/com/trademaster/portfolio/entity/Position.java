package com.trademaster.portfolio.entity;

import com.trademaster.portfolio.model.PositionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Position Entity
 * 
 * Represents a position (holding) in a specific security within a portfolio.
 * Tracks quantity, cost basis, current value, and P&L for the position.
 * 
 * Optimized for high-frequency updates with Java 24 Virtual Threads.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "positions", indexes = {
    @Index(name = "idx_position_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_position_symbol", columnList = "symbol"),
    @Index(name = "idx_position_portfolio_symbol", columnList = "portfolio_id, symbol", unique = true),
    @Index(name = "idx_position_updated_at", columnList = "updated_at"),
    @Index(name = "idx_position_expiry_date", columnList = "expiry_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "positionId")
@ToString(exclude = "portfolio")
public class Position {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;
    
    @Column(name = "portfolio_id", nullable = false, insertable = false, updatable = false)
    private Long portfolioId;
    
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;
    
    @Column(name = "exchange", nullable = false, length = 10)
    private String exchange;
    
    @Column(name = "instrument_type", length = 20)
    @Builder.Default
    private String instrumentType = "EQUITY";

    @Column(name = "sector", length = 50)
    @Builder.Default
    private String sector = "UNKNOWN";

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "average_cost", precision = 12, scale = 4, nullable = false)
    private BigDecimal averageCost;
    
    @Column(name = "total_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalCost;
    
    @Column(name = "current_price", precision = 12, scale = 4)
    private BigDecimal currentPrice;
    
    @Column(name = "market_value", precision = 19, scale = 4)
    private BigDecimal marketValue;
    
    @Column(name = "unrealized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;
    
    @Column(name = "realized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;
    
    @Column(name = "day_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal dayPnl = BigDecimal.ZERO;
    
    @Column(name = "previous_close_price", precision = 12, scale = 4)
    private BigDecimal previousClosePrice;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", nullable = false, length = 10)
    @Builder.Default
    private PositionType positionType = PositionType.LONG;
    
    @Column(name = "last_trade_price", precision = 12, scale = 4)
    private BigDecimal lastTradePrice;
    
    @Column(name = "last_trade_quantity")
    private Integer lastTradeQuantity;
    
    @Column(name = "last_trade_at")
    private Instant lastTradeAt;
    
    @Column(name = "last_price_update_at")
    private Instant lastPriceUpdateAt;
    
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "expiry_date")
    private Instant expiryDate;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;
    
    // Business methods
    
    /**
     * Update position with new trade
     */
    public void updateFromTrade(Integer tradeQuantity, BigDecimal tradePrice, Instant tradeTime) {
        // Store last trade info
        this.lastTradePrice = tradePrice;
        this.lastTradeQuantity = tradeQuantity;
        this.lastTradeAt = tradeTime;
        
        // Update position quantity and average cost
        if (this.quantity == 0) {
            // Opening new position
            this.quantity = tradeQuantity;
            this.averageCost = tradePrice;
            this.totalCost = tradePrice.multiply(new BigDecimal(tradeQuantity));
            this.openedAt = tradeTime;
        } else {
            // Adding to existing position
            BigDecimal newTotalCost = this.totalCost.add(
                tradePrice.multiply(new BigDecimal(tradeQuantity))
            );
            int newQuantity = this.quantity + tradeQuantity;
            
            if (newQuantity > 0) {
                this.averageCost = newTotalCost.divide(
                    new BigDecimal(newQuantity), 4, BigDecimal.ROUND_HALF_UP
                );
                this.totalCost = newTotalCost;
                this.quantity = newQuantity;
            } else if (newQuantity == 0) {
                // Position closed - realize P&L
                BigDecimal tradePnl = calculateRealizedPnl(tradeQuantity, tradePrice);
                this.realizedPnl = this.realizedPnl.add(tradePnl);
                this.quantity = 0;
                this.totalCost = BigDecimal.ZERO;
                this.unrealizedPnl = BigDecimal.ZERO;
                this.marketValue = BigDecimal.ZERO;
            } else {
                // Partial close - realize proportional P&L
                BigDecimal tradePnl = calculateRealizedPnl(tradeQuantity, tradePrice);
                this.realizedPnl = this.realizedPnl.add(tradePnl);
                
                // Update remaining position
                this.quantity = newQuantity;
                this.totalCost = this.averageCost.multiply(new BigDecimal(newQuantity));
            }
        }
    }
    
    /**
     * Update current market price and recalculate P&L
     */
    public void updatePrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        this.lastPriceUpdateAt = Instant.now();
        
        if (quantity != 0) {
            // Calculate market value
            this.marketValue = newPrice.multiply(new BigDecimal(Math.abs(quantity)));
            
            // Calculate unrealized P&L
            this.unrealizedPnl = calculateUnrealizedPnl();
            
            // Calculate day P&L if previous close is available
            if (previousClosePrice != null) {
                BigDecimal priceDiff = newPrice.subtract(previousClosePrice);
                this.dayPnl = priceDiff.multiply(new BigDecimal(quantity))
                    .multiply(new BigDecimal(positionType.getPnLMultiplier()));
            }
        } else {
            this.marketValue = BigDecimal.ZERO;
            this.unrealizedPnl = BigDecimal.ZERO;
            this.dayPnl = BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculate unrealized P&L
     */
    public BigDecimal calculateUnrealizedPnl() {
        if (quantity == 0 || currentPrice == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal currentValue = currentPrice.multiply(new BigDecimal(Math.abs(quantity)));
        BigDecimal costValue = averageCost.multiply(new BigDecimal(Math.abs(quantity)));
        
        BigDecimal pnl = currentValue.subtract(costValue);
        
        // Apply position type multiplier
        return pnl.multiply(new BigDecimal(positionType.getPnLMultiplier()));
    }
    
    /**
     * Calculate realized P&L for a trade
     */
    private BigDecimal calculateRealizedPnl(Integer tradeQuantity, BigDecimal tradePrice) {
        if (quantity == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal costBasis = averageCost.multiply(new BigDecimal(Math.abs(tradeQuantity)));
        BigDecimal saleValue = tradePrice.multiply(new BigDecimal(Math.abs(tradeQuantity)));
        
        BigDecimal pnl = saleValue.subtract(costBasis);
        
        // Adjust for position type and trade direction
        if (positionType == PositionType.SHORT) {
            pnl = pnl.negate();
        }
        
        return pnl;
    }
    
    /**
     * Get total P&L (realized + unrealized)
     */
    public BigDecimal getTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }
    
    /**
     * Get position return percentage
     */
    public BigDecimal getReturnPercent() {
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return getTotalPnl()
            .divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Check if position is closed
     */
    public boolean isClosed() {
        return quantity == 0;
    }
    
    /**
     * Check if position is profitable
     */
    public boolean isProfitable() {
        return getTotalPnl().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Set end-of-day price for next day P&L calculation
     */
    public void setEndOfDayPrice(BigDecimal closePrice) {
        this.previousClosePrice = closePrice;
    }
    
    /**
     * Get position weight as percentage of portfolio
     */
    public BigDecimal getPositionWeight(BigDecimal portfolioValue) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0 || marketValue == null) {
            return BigDecimal.ZERO;
        }
        
        return marketValue
            .divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}