package com.trademaster.pnlengine.entity;

import com.trademaster.pnlengine.service.BrokerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * P&L Calculation Result Entity
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Stores calculated P&L results for caching and audit trail purposes.
 * Enables fast retrieval of previously calculated P&L data while maintaining
 * comprehensive audit trails for regulatory compliance.
 * 
 * Key Features:
 * - Immutable calculation results with timestamp tracking
 * - Multi-broker P&L aggregation support
 * - Performance metrics for calculation timing
 * - Correlation IDs for distributed tracing
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
@Entity
@Table(name = "pnl_calculation_results", indexes = {
    @Index(name = "idx_pnl_user_portfolio", columnList = "user_id, portfolio_id"),
    @Index(name = "idx_pnl_broker_type", columnList = "broker_type"),
    @Index(name = "idx_pnl_calculated_at", columnList = "calculated_at"),
    @Index(name = "idx_pnl_correlation_id", columnList = "correlation_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "resultId")
public class PnLCalculationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "portfolio_id")
    private Long portfolioId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", length = 20)
    private BrokerType brokerType;
    
    @Column(name = "calculation_type", nullable = false, length = 50)
    private String calculationType; // MULTI_BROKER, BROKER_SPECIFIC, POSITION, etc.
    
    @Column(name = "total_portfolio_value", precision = 19, scale = 4)
    private BigDecimal totalPortfolioValue;
    
    @Column(name = "total_cash_balance", precision = 19, scale = 4)
    private BigDecimal totalCashBalance;
    
    @Column(name = "total_invested_amount", precision = 19, scale = 4)
    private BigDecimal totalInvestedAmount;
    
    @Column(name = "total_unrealized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalUnrealizedPnL = BigDecimal.ZERO;
    
    @Column(name = "total_realized_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalRealizedPnL = BigDecimal.ZERO;
    
    @Column(name = "total_day_pnl", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDayPnL = BigDecimal.ZERO;
    
    @Column(name = "total_return_percent", precision = 8, scale = 4)
    private BigDecimal totalReturnPercent;
    
    @Column(name = "total_return_amount", precision = 19, scale = 4)
    private BigDecimal totalReturnAmount;
    
    @Column(name = "total_positions", nullable = false)
    @Builder.Default
    private Integer totalPositions = 0;
    
    @Column(name = "active_brokers", nullable = false)
    @Builder.Default
    private Integer activeBrokers = 0;
    
    @Column(name = "calculation_time_ms", nullable = false)
    private Long calculationTimeMs;
    
    @Column(name = "correlation_id", length = 36, nullable = false)
    private String correlationId;
    
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData; // JSON serialized complete result
    
    @Column(name = "is_cached", nullable = false)
    @Builder.Default
    private Boolean isCached = false;
    
    @Column(name = "cache_expires_at")
    private Instant cacheExpiresAt;
    
    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Business methods
    
    /**
     * Get total P&L (realized + unrealized)
     */
    public BigDecimal getTotalPnL() {
        return totalRealizedPnL.add(totalUnrealizedPnL);
    }
    
    /**
     * Check if result is still valid for caching
     */
    public boolean isCacheValid() {
        return isCached && cacheExpiresAt != null && 
               Instant.now().isBefore(cacheExpiresAt);
    }
    
    /**
     * Mark result as cached with expiration
     */
    public void setCacheExpiration(Long cacheTimeoutMs) {
        this.isCached = true;
        this.cacheExpiresAt = Instant.now().plusMillis(cacheTimeoutMs);
    }
    
    /**
     * Check if calculation was fast (under performance target)
     */
    public boolean meetsPerformanceTarget(Long targetMs) {
        return calculationTimeMs != null && calculationTimeMs <= targetMs;
    }
}