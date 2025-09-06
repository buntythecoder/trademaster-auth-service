package com.trademaster.pnlengine.service.calculation;

import com.trademaster.pnlengine.common.functional.Result;
import com.trademaster.pnlengine.common.functional.Validation;
import com.trademaster.pnlengine.dto.PnLResultDTOs.SectorBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sector Analysis and Breakdown Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + <200 lines + <10 methods
 * 
 * Focused service for analyzing portfolio composition by sectors
 * using functional composition and immutable data patterns.
 * 
 * Single Responsibility: Sector allocation analysis and breakdown calculations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class SectorAnalysisService {
    
    @Value("${pnl.sector.min-allocation:0.01}")
    private BigDecimal minAllocation;
    
    @Value("${pnl.sector.max-concentration:0.40}")
    private BigDecimal maxConcentration;
    
    private static final MathContext PRECISION = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    // Sector mapping for Indian equities
    private static final Map<String, String> SYMBOL_SECTOR_MAP = Map.of(
        "RELIANCE", "Energy",
        "TCS", "Information Technology", 
        "INFY", "Information Technology",
        "HDFC", "Financial Services",
        "ICICIBANK", "Financial Services",
        "HDFCBANK", "Financial Services",
        "BHARTIARTL", "Telecommunications",
        "ITC", "Consumer Goods",
        "KOTAKBANK", "Financial Services",
        "HINDUNILVR", "Consumer Goods"
    );
    
    // ============================================================================
    // CORE SECTOR ANALYSIS METHODS (MAX 10 METHODS RULE)
    // ============================================================================
    
    /**
     * Analyze sector breakdown for user portfolio
     * Max 15 lines per method rule
     */
    public Result<SectorBreakdown, Exception> analyzeSectorBreakdown(String userId) {
        return Validation.USER_ID.apply(userId)
            .flatMap(validUserId -> getUserPortfolioPositions(validUserId))
            .map(this::calculateSectorAllocations)
            .map(this::createSectorBreakdown)
            .flatMap(this::validateSectorConcentration);
    }
    
    /**
     * Calculate sector-wise P&L performance
     */
    public Result<Map<String, SectorMetrics>, Exception> analyzeSectorPerformance(
            Set<BrokerCalculationService.Position> positions) {
        
        return Result.of(() -> positions.stream()
            .collect(Collectors.groupingBy(
                pos -> SYMBOL_SECTOR_MAP.getOrDefault(pos.symbol(), "Others"),
                Collectors.mapping(Function.identity(), Collectors.toSet())
            )))
            .map(sectorGroups -> sectorGroups.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> calculateSectorMetrics(entry.getValue())
                )));
    }
    
    /**
     * Get sector diversification score
     */
    public Result<DiversificationScore, Exception> calculateDiversificationScore(
            Map<String, SectorMetrics> sectorMetrics) {
        
        return Result.of(() -> {
            var sectorCount = sectorMetrics.size();
            var concentrationRisk = calculateConcentrationRisk(sectorMetrics);
            var diversificationIndex = calculateHerfindahlIndex(sectorMetrics);
            
            return new DiversificationScore(sectorCount, concentrationRisk, diversificationIndex,
                assessDiversificationLevel(sectorCount, concentrationRisk));
        });
    }
    
    // ============================================================================
    // INTERNAL CALCULATION METHODS
    // ============================================================================
    
    private Result<Set<BrokerCalculationService.Position>, Exception> getUserPortfolioPositions(String userId) {
        return Result.of(() -> {
            // Simulate fetching positions from multiple brokers
            log.debug("Fetching portfolio positions for user: {}", userId);
            return Set.of(
                new BrokerCalculationService.Position("RELIANCE", BigDecimal.valueOf(100), 
                    BigDecimal.valueOf(2500), BigDecimal.valueOf(2550), BigDecimal.valueOf(250000)),
                new BrokerCalculationService.Position("TCS", BigDecimal.valueOf(50), 
                    BigDecimal.valueOf(3200), BigDecimal.valueOf(3250), BigDecimal.valueOf(160000)),
                new BrokerCalculationService.Position("HDFCBANK", BigDecimal.valueOf(75), 
                    BigDecimal.valueOf(1400), BigDecimal.valueOf(1450), BigDecimal.valueOf(105000))
            );
        });
    }
    
    private Map<String, BigDecimal> calculateSectorAllocations(Set<BrokerCalculationService.Position> positions) {
        var totalValue = positions.stream()
            .map(pos -> pos.quantity().multiply(pos.currentPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return positions.stream()
            .collect(Collectors.groupingBy(
                pos -> SYMBOL_SECTOR_MAP.getOrDefault(pos.symbol(), "Others"),
                Collectors.reducing(BigDecimal.ZERO, 
                    pos -> pos.quantity().multiply(pos.currentPrice()),
                    BigDecimal::add)
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().divide(totalValue, PRECISION).multiply(HUNDRED)
            ));
    }
    
    private SectorBreakdown createSectorBreakdown(Map<String, BigDecimal> allocations) {
        var sectorAllocations = allocations.entrySet().stream()
            .filter(entry -> entry.getValue().compareTo(minAllocation) >= 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        var topSector = sectorAllocations.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
        
        return new SectorBreakdown(sectorAllocations, topSector, sectorAllocations.size());
    }
    
    private SectorMetrics calculateSectorMetrics(Set<BrokerCalculationService.Position> sectorPositions) {
        var totalValue = sectorPositions.stream()
            .map(pos -> pos.quantity().multiply(pos.currentPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        var totalPnL = sectorPositions.stream()
            .map(pos -> pos.quantity().multiply(pos.currentPrice().subtract(pos.averagePrice())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        var positionCount = sectorPositions.size();
        var avgReturn = totalValue.compareTo(BigDecimal.ZERO) > 0 ?
            totalPnL.divide(totalValue, PRECISION).multiply(HUNDRED) : BigDecimal.ZERO;
        
        return new SectorMetrics(totalValue, totalPnL, avgReturn, positionCount);
    }
    
    // ============================================================================
    // RISK ANALYSIS METHODS
    // ============================================================================
    
    private Result<SectorBreakdown, Exception> validateSectorConcentration(SectorBreakdown breakdown) {
        var maxAllocation = breakdown.sectorAllocations().values().stream()
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        return maxAllocation.compareTo(maxConcentration.multiply(HUNDRED)) <= 0 ?
            Result.success(breakdown) :
            Result.failure(new RuntimeException(
                String.format("Sector concentration risk: %.2f%% exceeds maximum %.2f%%",
                    maxAllocation, maxConcentration.multiply(HUNDRED))));
    }
    
    private BigDecimal calculateConcentrationRisk(Map<String, SectorMetrics> sectorMetrics) {
        var totalValue = sectorMetrics.values().stream()
            .map(SectorMetrics::totalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sectorMetrics.values().stream()
            .map(metrics -> metrics.totalValue().divide(totalValue, PRECISION))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO)
            .multiply(HUNDRED);
    }
    
    private BigDecimal calculateHerfindahlIndex(Map<String, SectorMetrics> sectorMetrics) {
        var totalValue = sectorMetrics.values().stream()
            .map(SectorMetrics::totalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sectorMetrics.values().stream()
            .map(metrics -> {
                var weight = metrics.totalValue().divide(totalValue, PRECISION);
                return weight.multiply(weight);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private DiversificationLevel assessDiversificationLevel(int sectorCount, BigDecimal concentrationRisk) {
        return switch (sectorCount) {
            case int count when count >= 8 && concentrationRisk.compareTo(BigDecimal.valueOf(25)) < 0 -> 
                DiversificationLevel.EXCELLENT;
            case int count when count >= 5 && concentrationRisk.compareTo(BigDecimal.valueOf(40)) < 0 -> 
                DiversificationLevel.GOOD;
            case int count when count >= 3 -> DiversificationLevel.MODERATE;
            default -> DiversificationLevel.POOR;
        };
    }
    
    // ============================================================================
    // IMMUTABLE RESULT RECORDS
    // ============================================================================
    
    public record SectorMetrics(
        BigDecimal totalValue,
        BigDecimal totalPnL,
        BigDecimal avgReturn,
        Integer positionCount
    ) {}
    
    public record DiversificationScore(
        Integer sectorCount,
        BigDecimal concentrationRisk,
        BigDecimal herfindahlIndex,
        DiversificationLevel level
    ) {}
    
    public enum DiversificationLevel {
        POOR, MODERATE, GOOD, EXCELLENT
    }
}