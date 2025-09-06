package com.trademaster.pnlengine.service.calculation;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.functional.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Position-Level P&L Calculation Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for calculating P&L metrics at individual position level
 * using functional composition and stream processing.
 * 
 * Single Responsibility: Position-level P&L calculations and portfolio aggregation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class PositionCalculationService {
    
    @Value("${pnl.position.min-quantity:0.001}")
    private BigDecimal minQuantity;
    
    @Value("${pnl.position.max-price-variance:0.15}")
    private double maxPriceVariance;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // ============================================================================
    // CORE POSITION CALCULATION METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Calculate P&L metrics for individual position
     * Max 15 lines per method rule
     */
    public Result<PositionPnL, Exception> calculatePositionPnL(
            BrokerCalculationService.Position position) {
        
        return validatePosition(position)
            .map(this::computePositionMetrics)
            .flatMap(this::validateCalculatedMetrics);
    }
    
    /**
     * Calculate aggregated portfolio metrics from multiple positions
     */
    public Result<BrokerCalculationService.PortfolioMetrics, Exception> calculatePortfolioMetrics(
            Set<BrokerCalculationService.Position> positions) {
        
        return Result.of(() -> positions.stream()
            .map(this::calculatePositionPnL)
            .filter(Result::isSuccess)
            .map(result -> result.getOrThrow())
            .reduce(PositionPnL.empty(), this::aggregatePositions))
            .map(this::convertToPortfolioMetrics);
    }
    
    /**
     * Calculate daily P&L changes for positions
     */
    public Result<DailyPnLSummary, Exception> calculateDailyPnL(
            Set<BrokerCalculationService.Position> positions, Set<BrokerCalculationService.Position> previousDay) {
        
        return Result.of(() -> Stream.of(positions, previousDay)
            .map(this::calculateTotalValue)
            .reduce(BigDecimal::subtract)
            .orElse(BigDecimal.ZERO))
            .map(dailyChange -> new DailyPnLSummary(
                dailyChange, calculateDailyReturnPercent(positions, dailyChange), positions.size()));
    }
    
    /**
     * Validate position data integrity
     */
    public Result<BrokerCalculationService.Position, Exception> validatePosition(
            BrokerCalculationService.Position position) {
        
        return Validation.validate(position.quantity(), Validation.NON_NEGATIVE_AMOUNT)
            .flatMap(ignored -> Validation.validate(position.averagePrice(), Validation.NON_NEGATIVE_AMOUNT))
            .flatMap(ignored -> Validation.validate(position.currentPrice(), Validation.NON_NEGATIVE_AMOUNT))
            .flatMap(ignored -> validatePriceVariance(position))
            .map(ignored -> position)
            .mapError(error -> new IllegalArgumentException(Validation.formatValidationError(error)));
    }
    
    // ============================================================================
    // INTERNAL CALCULATION METHODS
    // ============================================================================
    
    private PositionPnL computePositionMetrics(BrokerCalculationService.Position position) {
        var currentValue = position.quantity().multiply(position.currentPrice(), PRECISION);
        var investedAmount = position.quantity().multiply(position.averagePrice(), PRECISION);
        var unrealizedPnL = currentValue.subtract(investedAmount);
        var returnPercent = calculatePositionReturn(investedAmount, unrealizedPnL);
        
        return new PositionPnL(
            position.symbol(), currentValue, unrealizedPnL, BigDecimal.ZERO, 
            unrealizedPnL, returnPercent, position.quantity().intValue()
        );
    }
    
    private BigDecimal calculatePositionReturn(BigDecimal invested, BigDecimal unrealized) {
        return invested.compareTo(BigDecimal.ZERO) > 0 ?
            unrealized.divide(invested, PRECISION).multiply(HUNDRED) :
            BigDecimal.ZERO;
    }
    
    private PositionPnL aggregatePositions(PositionPnL acc, PositionPnL position) {
        return new PositionPnL(
            "PORTFOLIO", 
            acc.currentValue().add(position.currentValue()),
            acc.unrealizedPnL().add(position.unrealizedPnL()),
            acc.realizedPnL().add(position.realizedPnL()),
            acc.totalPnL().add(position.totalPnL()),
            calculateWeightedReturn(acc, position),
            acc.positionsCount() + position.positionsCount()
        );
    }
    
    private BigDecimal calculateWeightedReturn(PositionPnL acc, PositionPnL position) {
        var totalValue = acc.currentValue().add(position.currentValue());
        return totalValue.compareTo(BigDecimal.ZERO) > 0 ?
            acc.totalPnL().add(position.totalPnL()).divide(totalValue, PRECISION).multiply(HUNDRED) :
            BigDecimal.ZERO;
    }
    
    // ============================================================================
    // VALIDATION AND CONVERSION METHODS
    // ============================================================================
    
    private Result<BrokerCalculationService.Position, Validation.ValidationError> validatePriceVariance(
            BrokerCalculationService.Position position) {
        
        var variance = position.currentPrice().subtract(position.averagePrice())
            .abs().divide(position.averagePrice(), PRECISION).doubleValue();
        
        return variance <= maxPriceVariance ?
            Result.success(position) :
            Result.failure(new Validation.ValidationError.BusinessRule("price_variance_exceeded",
                String.format("Price variance %.2f exceeds maximum %.2f", variance, maxPriceVariance)));
    }
    
    private Result<PositionPnL, Exception> validateCalculatedMetrics(PositionPnL position) {
        return position.currentValue().compareTo(BigDecimal.ZERO) >= 0 ?
            Result.success(position) :
            Result.failure(new IllegalStateException("Invalid calculated position metrics"));
    }
    
    private BrokerCalculationService.PortfolioMetrics convertToPortfolioMetrics(PositionPnL aggregated) {
        return new BrokerCalculationService.PortfolioMetrics(
            aggregated.currentValue(), aggregated.unrealizedPnL(), 
            aggregated.realizedPnL(), aggregated.totalPnL(), aggregated.positionsCount()
        );
    }
    
    private BigDecimal calculateTotalValue(Set<BrokerCalculationService.Position> positions) {
        return positions.stream()
            .map(pos -> pos.quantity().multiply(pos.currentPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateDailyReturnPercent(Set<BrokerCalculationService.Position> positions, 
                                                  BigDecimal dailyChange) {
        var totalValue = calculateTotalValue(positions);
        var previousValue = totalValue.subtract(dailyChange);
        
        return previousValue.compareTo(BigDecimal.ZERO) > 0 ?
            dailyChange.divide(previousValue, PRECISION).multiply(HUNDRED) :
            BigDecimal.ZERO;
    }
    
    // ============================================================================
    // IMMUTABLE RESULT RECORDS
    // ============================================================================
    
    public record PositionPnL(
        String symbol,
        BigDecimal currentValue,
        BigDecimal unrealizedPnL,
        BigDecimal realizedPnL,
        BigDecimal totalPnL,
        BigDecimal returnPercent,
        Integer positionsCount
    ) {
        public static PositionPnL empty() {
            return new PositionPnL("EMPTY", BigDecimal.ZERO, BigDecimal.ZERO, 
                                 BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
    }
    
    public record DailyPnLSummary(
        BigDecimal dailyChange,
        BigDecimal dailyReturnPercent,
        Integer positionsCount
    ) {}
}