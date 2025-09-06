package com.trademaster.portfolio.domain;

import com.trademaster.portfolio.model.PortfolioStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio Entity
 * 
 * Represents a user's portfolio with positions and metadata.
 */
@Entity
@Table(name = "portfolios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal totalValue;
    
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal cashBalance;
    
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal realizedPnl;
    
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal unrealizedPnl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PortfolioStatus status;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    /**
     * Calculate total P&L
     */
    public BigDecimal getTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }
}