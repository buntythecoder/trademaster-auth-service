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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tax Optimization and Calculation Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for tax-related calculations and optimization strategies
 * using functional composition and Indian taxation rules.
 * 
 * Single Responsibility: Tax calculation and optimization recommendations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class TaxOptimizationService {
    
    @Value("${tax.stcg.rate:0.15}")
    private BigDecimal shortTermCapitalGainsRate;
    
    @Value("${tax.ltcg.rate:0.10}")
    private BigDecimal longTermCapitalGainsRate;
    
    @Value("${tax.ltcg.exemption-limit:100000}")
    private BigDecimal ltcgExemptionLimit;
    
    @Value("${tax.holding-period-days:365}")
    private int longTermHoldingPeriodDays;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // ============================================================================
    // CORE TAX CALCULATION METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Calculate comprehensive tax liability for realized gains
     * Max 15 lines per method rule
     */
    public Result<TaxLiability, Exception> calculateTaxLiability(
            String userId, List<RealizedTransaction> transactions) {
        
        return Validation.USER_ID.apply(userId)
            .flatMap(validUserId -> validateTransactions(transactions))
            .map(this::segregateByHoldingPeriod)
            .map(this::computeTaxComponents)
            .flatMap(this::validateTaxCalculation);
    }
    
    /**
     * Generate tax optimization recommendations
     */
    public Result<TaxOptimizationRecommendations, Exception> generateOptimizationRecommendations(
            List<UnrealizedPosition> positions, BigDecimal currentTaxLiability) {
        
        return Result.of(() -> positions.stream()
            .collect(Collectors.groupingBy(this::classifyHoldingPeriod)))
            .map(segregatedPositions -> createOptimizationStrategies(segregatedPositions, currentTaxLiability))
            .map(this::prioritizeRecommendations);
    }
    
    /**
     * Calculate tax-loss harvesting opportunities
     */
    public Result<TaxHarvestingOpportunities, Exception> identifyHarvestingOpportunities(
            List<UnrealizedPosition> positions) {
        
        return Result.of(() -> {
            var lossPositions = filterLossPositions(positions);
            var shortTermLosses = calculateTotalLosses(lossPositions, this::isShortTerm);
            var longTermLosses = calculateTotalLosses(lossPositions, this::isLongTerm);
            
            return new TaxHarvestingOpportunities(
                shortTermLosses, longTermLosses, lossPositions.size(),
                calculatePotentialSavings(shortTermLosses, longTermLosses)
            );
        });
    }
    
    /**
     * Calculate wash sale rule violations
     */
    public Result<WashSaleAnalysis, Exception> analyzeWashSaleRisk(
            List<RealizedTransaction> recentTransactions) {
        
        return Result.of(() -> recentTransactions.stream()
            .collect(Collectors.groupingBy(RealizedTransaction::symbol)))
            .map(symbolTransactions -> symbolTransactions.entrySet().stream()
                .filter(entry -> hasWashSaleViolation(entry.getValue()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> calculateWashSaleImpact(entry.getValue())
                )))
            .map(washSales -> new WashSaleAnalysis(washSales.size(), 
                washSales.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }
    
    // ============================================================================
    // INTERNAL CALCULATION METHODS
    // ============================================================================
    
    private HoldingPeriodSegregation segregateByHoldingPeriod(List<RealizedTransaction> transactions) {
        var shortTermTransactions = transactions.stream()
            .filter(this::isShortTermTransaction)
            .toList();
        
        var longTermTransactions = transactions.stream()
            .filter(txn -> !isShortTermTransaction(txn))
            .toList();
        
        return new HoldingPeriodSegregation(shortTermTransactions, longTermTransactions);
    }
    
    private TaxLiability computeTaxComponents(HoldingPeriodSegregation segregation) {
        var stcgGains = calculateTotalGains(segregation.shortTermTransactions());
        var ltcgGains = calculateTotalGains(segregation.longTermTransactions());
        
        var stcgTax = stcgGains.multiply(shortTermCapitalGainsRate);
        var taxableLtcgGains = ltcgGains.max(BigDecimal.ZERO).subtract(ltcgExemptionLimit).max(BigDecimal.ZERO);
        var ltcgTax = taxableLtcgGains.multiply(longTermCapitalGainsRate);
        
        return new TaxLiability(
            stcgGains, ltcgGains, stcgTax, ltcgTax,
            stcgTax.add(ltcgTax), ltcgExemptionLimit
        );
    }
    
    private TaxOptimizationRecommendations createOptimizationStrategies(
            Map<HoldingClassification, List<UnrealizedPosition>> positions, BigDecimal currentLiability) {
        
        var shortTermPositions = positions.getOrDefault(HoldingClassification.SHORT_TERM, List.of());
        var nearLongTermPositions = positions.getOrDefault(HoldingClassification.NEAR_LONG_TERM, List.of());
        
        var recommendations = List.of(
            createHoldingPeriodRecommendation(nearLongTermPositions),
            createLossHarvestingRecommendation(shortTermPositions),
            createGainRealizationRecommendation(positions.getOrDefault(HoldingClassification.LONG_TERM, List.of()))
        );
        
        var potentialSavings = recommendations.stream()
            .map(TaxRecommendation::potentialSavings)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new TaxOptimizationRecommendations(recommendations, potentialSavings, currentLiability);
    }
    
    // ============================================================================
    // HELPER AND VALIDATION METHODS
    // ============================================================================
    
    private boolean isShortTermTransaction(RealizedTransaction transaction) {
        return ChronoUnit.DAYS.between(transaction.purchaseDate(), transaction.saleDate()) < longTermHoldingPeriodDays;
    }
    
    private boolean isShortTerm(UnrealizedPosition position) {
        return ChronoUnit.DAYS.between(position.purchaseDate(), LocalDate.now()) < longTermHoldingPeriodDays;
    }
    
    private boolean isLongTerm(UnrealizedPosition position) {
        return !isShortTerm(position);
    }
    
    private List<UnrealizedPosition> filterLossPositions(List<UnrealizedPosition> positions) {
        return positions.stream()
            .filter(pos -> pos.currentValue().compareTo(pos.purchaseValue()) < 0)
            .toList();
    }
    
    private BigDecimal calculateTotalGains(List<RealizedTransaction> transactions) {
        return transactions.stream()
            .map(RealizedTransaction::realizedGain)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateTotalLosses(List<UnrealizedPosition> positions, Predicate<UnrealizedPosition> filter) {
        return positions.stream()
            .filter(filter)
            .map(pos -> pos.purchaseValue().subtract(pos.currentValue()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private Result<List<RealizedTransaction>, Exception> validateTransactions(List<RealizedTransaction> transactions) {
        return transactions.isEmpty() ?
            Result.failure(new IllegalArgumentException("No transactions provided for tax calculation")) :
            Result.success(transactions);
    }
    
    private Result<TaxLiability, Exception> validateTaxCalculation(TaxLiability liability) {
        return liability.totalTax().compareTo(BigDecimal.ZERO) >= 0 ?
            Result.success(liability) :
            Result.failure(new IllegalStateException("Invalid tax calculation result"));
    }
    
    // ============================================================================
    // IMMUTABLE RESULT RECORDS
    // ============================================================================
    
    public record RealizedTransaction(
        String symbol, LocalDate purchaseDate, LocalDate saleDate,
        BigDecimal purchaseValue, BigDecimal saleValue, BigDecimal realizedGain
    ) {}
    
    public record UnrealizedPosition(
        String symbol, LocalDate purchaseDate, BigDecimal purchaseValue, BigDecimal currentValue
    ) {}
    
    public record TaxLiability(
        BigDecimal shortTermGains, BigDecimal longTermGains,
        BigDecimal shortTermTax, BigDecimal longTermTax,
        BigDecimal totalTax, BigDecimal exemptionUsed
    ) {}
    
    public record TaxHarvestingOpportunities(
        BigDecimal shortTermLosses, BigDecimal longTermLosses, 
        Integer lossPositions, BigDecimal potentialSavings
    ) {}
    
    record HoldingPeriodSegregation(
        List<RealizedTransaction> shortTermTransactions,
        List<RealizedTransaction> longTermTransactions
    ) {}
    
    enum HoldingClassification {
        SHORT_TERM, NEAR_LONG_TERM, LONG_TERM
    }
}