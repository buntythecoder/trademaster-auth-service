package com.trademaster.portfolio.domain;

import com.trademaster.portfolio.model.PortfolioStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Domain Model (Legacy)
 *
 * Represents a user's portfolio with positions and metadata.
 *
 * IMPORTANT: This is a LEGACY domain model used only for data transfer within the service layer.
 * It is NOT a JPA entity and should NOT be persisted to the database.
 *
 * For database operations, use com.trademaster.portfolio.entity.Portfolio instead.
 * New code should use com.trademaster.portfolio.entity.Portfolio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private PortfolioStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Calculate total P&L
     */
    public BigDecimal getTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }
}