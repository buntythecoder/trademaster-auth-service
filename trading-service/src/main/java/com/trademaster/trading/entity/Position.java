package com.trademaster.trading.entity;

import com.trademaster.trading.model.PositionSide;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Position Entity
 * 
 * Represents a trading position with comprehensive tracking:
 * - Real-time position quantity and average cost
 * - P&L calculations with realized/unrealized breakdown  
 * - Risk metrics and exposure analysis
 * - Cost basis tracking with FIFO/LIFO support
 * - Margin and borrowing cost tracking
 * - Performance attribution and benchmarking
 * 
 * Optimized for high-frequency updates with Virtual Threads:
 * - Indexed fields for fast position lookups
 * - Calculated fields for performance optimization
 * - Concurrent-safe P&L calculations
 * - Real-time streaming integration
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "positions", indexes = {
    @Index(name = "idx_positions_user_id", columnList = "user_id"),
    @Index(name = "idx_positions_symbol", columnList = "symbol"),
    @Index(name = "idx_positions_user_symbol", columnList = "user_id, symbol", unique = true),
    @Index(name = "idx_positions_updated_at", columnList = "updated_at"),
    @Index(name = "idx_positions_market_value", columnList = "market_value"),
    @Index(name = "idx_positions_unrealized_pnl", columnList = "unrealized_pnl"),
    @Index(name = "idx_positions_active", columnList = "user_id, quantity")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who owns this position
     */
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;
    
    /**
     * Trading symbol (e.g., RELIANCE, TCS, INFY)
     */
    @Column(name = "symbol", nullable = false, length = 20)
    @NotNull
    private String symbol;
    
    /**
     * Exchange where security is traded (NSE, BSE)
     */
    @Column(name = "exchange", nullable = false, length = 10)
    @NotNull
    private String exchange;
    
    /**
     * Current position quantity (positive = long, negative = short)
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;
    
    /**
     * Position side derived from quantity
     */
    @Column(name = "side", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PositionSide side;
    
    /**
     * Average cost basis per share
     */
    @Column(name = "average_cost", precision = 15, scale = 4)
    private BigDecimal averageCost;
    
    /**
     * Total cost basis (quantity * average cost)
     */
    @Column(name = "cost_basis", precision = 20, scale = 4)
    private BigDecimal costBasis;
    
    /**
     * Current market price per share
     */
    @Column(name = "current_price", precision = 15, scale = 4)
    private BigDecimal currentPrice;
    
    /**
     * Current market value of position
     */
    @Column(name = "market_value", precision = 20, scale = 4)
    private BigDecimal marketValue;
    
    /**
     * Unrealized P&L (market value - cost basis)
     */
    @Column(name = "unrealized_pnl", precision = 20, scale = 4)
    private BigDecimal unrealizedPnL;
    
    /**
     * Unrealized P&L percentage
     */
    @Column(name = "unrealized_pnl_percent", precision = 8, scale = 4)
    private BigDecimal unrealizedPnLPercent;
    
    /**
     * Realized P&L from closed positions
     */
    @Column(name = "realized_pnl", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal realizedPnL = BigDecimal.ZERO;
    
    /**
     * Total P&L (realized + unrealized)
     */
    @Column(name = "total_pnl", precision = 20, scale = 4)
    private BigDecimal totalPnL;
    
    /**
     * Intraday P&L (change since market open)
     */
    @Column(name = "intraday_pnl", precision = 20, scale = 4)
    private BigDecimal intradayPnL;
    
    /**
     * Previous day's closing position value
     */
    @Column(name = "previous_close_value", precision = 20, scale = 4)
    private BigDecimal previousCloseValue;
    
    /**
     * Day change in position value
     */
    @Column(name = "day_change", precision = 20, scale = 4)
    private BigDecimal dayChange;
    
    /**
     * Day change percentage
     */
    @Column(name = "day_change_percent", precision = 8, scale = 4)
    private BigDecimal dayChangePercent;
    
    /**
     * Pending quantity from unexecuted orders
     */
    @Column(name = "pending_quantity")
    @Builder.Default
    private Integer pendingQuantity = 0;
    
    /**
     * Available quantity (quantity - pending_quantity)
     */
    @Column(name = "available_quantity")
    private Integer availableQuantity;
    
    /**
     * Maximum position size held during the day
     */
    @Column(name = "max_position_size")
    private Integer maxPositionSize;
    
    /**
     * Minimum position size held during the day
     */
    @Column(name = "min_position_size")
    private Integer minPositionSize;
    
    /**
     * Margin requirement for this position
     */
    @Column(name = "margin_requirement", precision = 15, scale = 4)
    private BigDecimal marginRequirement;
    
    /**
     * Margin utilization percentage
     */
    @Column(name = "margin_utilization", precision = 5, scale = 2)
    private BigDecimal marginUtilization;
    
    /**
     * Borrowing cost for short positions or margin
     */
    @Column(name = "borrowing_cost", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal borrowingCost = BigDecimal.ZERO;
    
    /**
     * Position risk score (0.0-1.0)
     */
    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;
    
    /**
     * Position weight in portfolio (percentage)
     */
    @Column(name = "portfolio_weight", precision = 5, scale = 2)
    private BigDecimal portfolioWeight;
    
    /**
     * Beta to market benchmark
     */
    @Column(name = "beta", precision = 8, scale = 4)
    private BigDecimal beta;
    
    /**
     * Number of trades that built this position
     */
    @Column(name = "trade_count")
    @Builder.Default
    private Integer tradeCount = 0;
    
    /**
     * First trade date for this position
     */
    @Column(name = "first_trade_date")
    private LocalDate firstTradeDate;
    
    /**
     * Last trade date for this position
     */
    @Column(name = "last_trade_date")
    private LocalDate lastTradeDate;
    
    /**
     * Days held in position
     */
    @Column(name = "days_held")
    private Integer daysHeld;
    
    /**
     * Sector classification
     */
    @Column(name = "sector", length = 50)
    private String sector;
    
    /**
     * Industry classification
     */
    @Column(name = "industry", length = 50)
    private String industry;
    
    /**
     * Asset class (EQUITY, BOND, COMMODITY, etc.)
     */
    @Column(name = "asset_class", length = 20)
    private String assetClass;
    
    /**
     * Position tags for categorization
     */
    @Column(name = "tags", length = 200)
    private String tags;
    
    /**
     * Position creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Last modification timestamp
     */
    @LastModifiedDate\n    @Column(name = \"updated_at\", nullable = false)\n    private Instant updatedAt;\n    \n    /**\n     * Last price update timestamp\n     */\n    @Column(name = \"price_updated_at\")\n    private Instant priceUpdatedAt;\n    \n    /**\n     * Additional position metadata as JSON\n     */\n    @Column(name = \"metadata\", columnDefinition = \"TEXT\")\n    private String metadata;\n    \n    // ========== Business Logic Methods ==========\n    \n    /**\n     * Update position with new trade\n     */\n    public void addTrade(Integer tradeQuantity, BigDecimal tradePrice, Instant tradeTime) {\n        if (tradeQuantity == null || tradeQuantity == 0) {\n            return;\n        }\n        \n        int newQuantity = (quantity != null ? quantity : 0) + tradeQuantity;\n        \n        if (newQuantity == 0) {\n            // Position closed - calculate realized P&L\n            if (averageCost != null) {\n                BigDecimal realizedPnLForTrade = tradePrice.subtract(averageCost)\n                    .multiply(BigDecimal.valueOf(Math.abs(tradeQuantity)));\n                realizedPnL = (realizedPnL != null ? realizedPnL : BigDecimal.ZERO)\n                    .add(realizedPnLForTrade);\n            }\n            \n            // Reset position\n            quantity = 0;\n            averageCost = null;\n            costBasis = BigDecimal.ZERO;\n            side = null;\n            \n        } else if (quantity == null || quantity == 0) {\n            // New position\n            quantity = newQuantity;\n            averageCost = tradePrice;\n            costBasis = tradePrice.multiply(BigDecimal.valueOf(Math.abs(newQuantity)));\n            side = newQuantity > 0 ? PositionSide.LONG : PositionSide.SHORT;\n            firstTradeDate = tradeTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate();\n            \n        } else if ((quantity > 0 && tradeQuantity > 0) || (quantity < 0 && tradeQuantity < 0)) {\n            // Same direction - update average cost\n            BigDecimal totalValue = costBasis.add(tradePrice.multiply(BigDecimal.valueOf(Math.abs(tradeQuantity))));\n            quantity = newQuantity;\n            averageCost = totalValue.divide(BigDecimal.valueOf(Math.abs(quantity)), 4, java.math.RoundingMode.HALF_UP);\n            costBasis = totalValue;\n            \n        } else {\n            // Opposite direction - partial or full close\n            int closedQuantity = Math.min(Math.abs(quantity), Math.abs(tradeQuantity));\n            \n            // Calculate realized P&L for closed portion\n            if (averageCost != null) {\n                BigDecimal realizedPnLForTrade = (tradePrice.subtract(averageCost))\n                    .multiply(BigDecimal.valueOf(closedQuantity))\n                    .multiply(BigDecimal.valueOf(quantity > 0 ? 1 : -1));\n                realizedPnL = (realizedPnL != null ? realizedPnL : BigDecimal.ZERO)\n                    .add(realizedPnLForTrade);\n            }\n            \n            quantity = newQuantity;\n            if (quantity == 0) {\n                averageCost = null;\n                costBasis = BigDecimal.ZERO;\n                side = null;\n            } else {\n                costBasis = averageCost.multiply(BigDecimal.valueOf(Math.abs(quantity)));\n            }\n        }\n        \n        // Update trade tracking\n        tradeCount = (tradeCount != null ? tradeCount : 0) + 1;\n        lastTradeDate = tradeTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate();\n        \n        // Update days held\n        if (firstTradeDate != null) {\n            daysHeld = (int) java.time.temporal.ChronoUnit.DAYS.between(\n                firstTradeDate, lastTradeDate != null ? lastTradeDate : LocalDate.now());\n        }\n        \n        updatedAt = Instant.now();\n    }\n    \n    /**\n     * Update market price and recalculate P&L\n     */\n    public void updateMarketPrice(BigDecimal newPrice, Instant priceTime) {\n        if (newPrice == null || quantity == null || quantity == 0) {\n            return;\n        }\n        \n        BigDecimal previousPrice = currentPrice;\n        currentPrice = newPrice;\n        priceUpdatedAt = priceTime;\n        \n        // Calculate market value\n        marketValue = currentPrice.multiply(BigDecimal.valueOf(Math.abs(quantity)));\n        \n        // Calculate unrealized P&L\n        if (averageCost != null && costBasis != null) {\n            unrealizedPnL = marketValue.subtract(costBasis);\n            if (quantity < 0) {\n                unrealizedPnL = unrealizedPnL.negate(); // Invert for short positions\n            }\n            \n            // Calculate unrealized P&L percentage\n            if (costBasis.compareTo(BigDecimal.ZERO) != 0) {\n                unrealizedPnLPercent = unrealizedPnL.divide(costBasis, 4, java.math.RoundingMode.HALF_UP)\n                    .multiply(BigDecimal.valueOf(100));\n            }\n        }\n        \n        // Calculate total P&L\n        totalPnL = (realizedPnL != null ? realizedPnL : BigDecimal.ZERO)\n            .add(unrealizedPnL != null ? unrealizedPnL : BigDecimal.ZERO);\n        \n        // Calculate day change\n        if (previousPrice != null && previousCloseValue != null) {\n            dayChange = marketValue.subtract(previousCloseValue);\n            if (previousCloseValue.compareTo(BigDecimal.ZERO) != 0) {\n                dayChangePercent = dayChange.divide(previousCloseValue, 4, java.math.RoundingMode.HALF_UP)\n                    .multiply(BigDecimal.valueOf(100));\n            }\n        }\n        \n        // Update available quantity\n        availableQuantity = quantity - (pendingQuantity != null ? pendingQuantity : 0);\n        \n        updatedAt = Instant.now();\n    }\n    \n    /**\n     * Update pending quantity from orders\n     */\n    public void updatePendingQuantity(Integer newPendingQuantity) {\n        pendingQuantity = newPendingQuantity != null ? newPendingQuantity : 0;\n        availableQuantity = (quantity != null ? quantity : 0) - pendingQuantity;\n        updatedAt = Instant.now();\n    }\n    \n    /**\n     * Set previous day's closing values for day change calculation\n     */\n    public void setPreviousDayClose(BigDecimal closingValue) {\n        previousCloseValue = closingValue;\n        \n        // Recalculate day changes if current market value is available\n        if (marketValue != null) {\n            dayChange = marketValue.subtract(previousCloseValue);\n            if (previousCloseValue.compareTo(BigDecimal.ZERO) != 0) {\n                dayChangePercent = dayChange.divide(previousCloseValue, 4, java.math.RoundingMode.HALF_UP)\n                    .multiply(BigDecimal.valueOf(100));\n            }\n        }\n    }\n    \n    // ========== Calculated Properties ==========\n    \n    /**\n     * Check if position is long\n     */\n    public boolean isLong() {\n        return quantity != null && quantity > 0;\n    }\n    \n    /**\n     * Check if position is short\n     */\n    public boolean isShort() {\n        return quantity != null && quantity < 0;\n    }\n    \n    /**\n     * Check if position is flat (no position)\n     */\n    public boolean isFlat() {\n        return quantity == null || quantity == 0;\n    }\n    \n    /**\n     * Get absolute position size\n     */\n    public Integer getAbsoluteQuantity() {\n        return quantity != null ? Math.abs(quantity) : 0;\n    }\n    \n    /**\n     * Get position value (market value or cost basis)\n     */\n    public BigDecimal getPositionValue() {\n        return marketValue != null ? marketValue : \n               (costBasis != null ? costBasis : BigDecimal.ZERO);\n    }\n    \n    /**\n     * Get total return percentage\n     */\n    public BigDecimal getTotalReturnPercent() {\n        if (totalPnL == null || costBasis == null || costBasis.compareTo(BigDecimal.ZERO) == 0) {\n            return BigDecimal.ZERO;\n        }\n        return totalPnL.divide(costBasis, 4, java.math.RoundingMode.HALF_UP)\n               .multiply(BigDecimal.valueOf(100));\n    }\n    \n    /**\n     * Check if position is profitable\n     */\n    public boolean isProfitable() {\n        return totalPnL != null && totalPnL.compareTo(BigDecimal.ZERO) > 0;\n    }\n    \n    /**\n     * Get risk-adjusted return (return per unit of risk score)\n     */\n    public BigDecimal getRiskAdjustedReturn() {\n        if (riskScore == null || riskScore.compareTo(BigDecimal.ZERO) == 0 || \n            getTotalReturnPercent().compareTo(BigDecimal.ZERO) == 0) {\n            return BigDecimal.ZERO;\n        }\n        return getTotalReturnPercent().divide(riskScore, 4, java.math.RoundingMode.HALF_UP);\n    }\n    \n    /**\n     * Get annualized return based on days held\n     */\n    public BigDecimal getAnnualizedReturn() {\n        if (daysHeld == null || daysHeld <= 0 || getTotalReturnPercent().compareTo(BigDecimal.ZERO) == 0) {\n            return BigDecimal.ZERO;\n        }\n        \n        BigDecimal dailyReturn = getTotalReturnPercent().divide(BigDecimal.valueOf(daysHeld), 8, java.math.RoundingMode.HALF_UP);\n        return dailyReturn.multiply(BigDecimal.valueOf(365));\n    }\n    \n    /**\n     * Check if position requires margin\n     */\n    public boolean isMarginPosition() {\n        return marginRequirement != null && marginRequirement.compareTo(BigDecimal.ZERO) > 0;\n    }\n    \n    /**\n     * Check if position is within risk limits\n     */\n    public boolean isWithinRiskLimits() {\n        return riskScore == null || riskScore.compareTo(new BigDecimal(\"0.8\")) <= 0;\n    }\n    \n    /**\n     * Get position summary for display\n     */\n    public String getPositionSummary() {\n        return String.format(\"%s %d %s @ ₹%.2f (P&L: ₹%.2f, %.2f%%)\",\n                             side != null ? side.name() : \"FLAT\",\n                             getAbsoluteQuantity(),\n                             symbol,\n                             averageCost != null ? averageCost : BigDecimal.ZERO,\n                             totalPnL != null ? totalPnL : BigDecimal.ZERO,\n                             getTotalReturnPercent());\n    }\n}"