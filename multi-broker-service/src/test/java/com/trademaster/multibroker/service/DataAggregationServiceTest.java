package com.trademaster.multibroker.service;

import com.trademaster.multibroker.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Data Aggregation Service Test
 * 
 * MANDATORY: Comprehensive Testing + TradeMaster Standards + Zero Placeholders
 * 
 * Unit tests for DataAggregationService covering portfolio consolidation,
 * position aggregation, and financial calculations using JUnit 5 and Mockito.
 * 
 * Test Coverage:
 * - Portfolio aggregation across multiple brokers
 * - Position consolidation for same symbols
 * - Weighted average price calculations
 * - P&L and percentage calculations
 * - Asset allocation and broker breakdown
 * - Edge cases and error handling
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Comprehensive Service Testing)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Data Aggregation Service Tests")
class DataAggregationServiceTest {
    
    @Mock
    private PositionNormalizationService normalizationService;
    
    @Mock
    private PriceService priceService;
    
    @Mock
    private CurrencyConversionService currencyService;
    
    @Mock
    private AssetClassificationService assetClassificationService;
    
    @Mock
    private Executor virtualThreadExecutor;
    
    private DataAggregationService dataAggregationService;
    
    @BeforeEach
    void setUp() {
        dataAggregationService = new DataAggregationService(
            normalizationService,
            priceService,
            currencyService,
            assetClassificationService,
            virtualThreadExecutor
        );
    }
    
    @Test
    @DisplayName("Should aggregate portfolio from multiple brokers successfully")
    void shouldAggregatePortfolioFromMultipleBrokers() {
        // Given
        String userId = "user123";
        List<BrokerPortfolio> brokerPortfolios = createTestBrokerPortfolios();
        
        // Mock dependencies
        when(normalizationService.normalize(any(BrokerPosition.class), anyString(), anyString()))
            .thenReturn(createTestNormalizedPosition("RELIANCE", "ZERODHA", "Zerodha"));
        
        when(priceService.getCurrentPrice(anyString()))
            .thenReturn(Optional.of(new BigDecimal("2500.00")));
            
        when(assetClassificationService.getCompanyName(anyString()))
            .thenReturn(Optional.of("Reliance Industries Limited"));
            
        when(assetClassificationService.getSector(anyString()))
            .thenReturn(Optional.of("Oil & Gas"));
            
        when(assetClassificationService.getAssetClass(anyString()))
            .thenReturn(Optional.of("EQUITY"));
        
        // When
        CompletableFuture<ConsolidatedPortfolio> future = 
            dataAggregationService.aggregatePortfolios(userId, brokerPortfolios);
        ConsolidatedPortfolio result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.totalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.positions()).isNotEmpty();
        assertThat(result.brokerBreakdown()).hasSize(2); // Two brokers
        assertThat(result.assetAllocation()).isNotEmpty();
        assertThat(result.lastUpdated()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle empty broker portfolios gracefully")
    void shouldHandleEmptyBrokerPortfolios() {
        // Given
        String userId = "user123";
        List<BrokerPortfolio> emptyPortfolios = List.of();
        
        // When
        CompletableFuture<ConsolidatedPortfolio> future = 
            dataAggregationService.aggregatePortfolios(userId, emptyPortfolios);
        ConsolidatedPortfolio result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.totalValue()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.totalCost()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.positions()).isEmpty();
        assertThat(result.brokerBreakdown()).isEmpty();
        assertThat(result.assetAllocation()).isEmpty();
    }
    
    @Test
    @DisplayName("Should consolidate same symbol positions across brokers")
    void shouldConsolidateSameSymbolPositionsAcrossBrokers() {
        // Given
        String userId = "user123";
        List<BrokerPortfolio> brokerPortfolios = createTestBrokerPortfoliosWithSameSymbol();
        
        // Mock normalization to return same symbol from different brokers
        when(normalizationService.normalize(any(BrokerPosition.class), anyString(), anyString()))
            .thenReturn(createTestNormalizedPosition("RELIANCE", "ZERODHA", "Zerodha"))
            .thenReturn(createTestNormalizedPosition("RELIANCE", "UPSTOX", "Upstox"));
        
        when(priceService.getCurrentPrice("RELIANCE"))
            .thenReturn(Optional.of(new BigDecimal("2500.00")));
            
        when(assetClassificationService.getCompanyName("RELIANCE"))
            .thenReturn(Optional.of("Reliance Industries Limited"));
            
        when(assetClassificationService.getSector("RELIANCE"))
            .thenReturn(Optional.of("Oil & Gas"));
            
        when(assetClassificationService.getAssetClass("RELIANCE"))
            .thenReturn(Optional.of("EQUITY"));
        
        // When
        CompletableFuture<ConsolidatedPortfolio> future = 
            dataAggregationService.aggregatePortfolios(userId, brokerPortfolios);
        ConsolidatedPortfolio result = future.join();
        
        // Then
        assertThat(result.positions()).hasSize(1); // Consolidated into single position
        ConsolidatedPosition position = result.positions().get(0);
        assertThat(position.symbol()).isEqualTo("RELIANCE");
        assertThat(position.getBrokerCount()).isEqualTo(2); // Two brokers
        assertThat(position.totalQuantity()).isEqualTo(150L); // 100 + 50
    }
    
    @Test
    @DisplayName("Should calculate weighted average price correctly")
    void shouldCalculateWeightedAveragePriceCorrectly() {
        // Given - Two positions with different prices and quantities
        String userId = "user123";
        List<BrokerPortfolio> brokerPortfolios = List.of(
            createBrokerPortfolio("ZERODHA", "Zerodha", 
                List.of(createBrokerPosition("RELIANCE", 100, new BigDecimal("2000.00")))),
            createBrokerPortfolio("UPSTOX", "Upstox",
                List.of(createBrokerPosition("RELIANCE", 200, new BigDecimal("2500.00"))))
        );
        
        // Expected weighted average: (100*2000 + 200*2500) / (100+200) = 700000/300 = 2333.33
        
        when(normalizationService.normalize(any(BrokerPosition.class), anyString(), anyString()))
            .thenReturn(createNormalizedPosition("RELIANCE", "ZERODHA", 100L, new BigDecimal("2000.00")))
            .thenReturn(createNormalizedPosition("RELIANCE", "UPSTOX", 200L, new BigDecimal("2500.00")));
        
        when(priceService.getCurrentPrice("RELIANCE"))
            .thenReturn(Optional.of(new BigDecimal("2600.00")));
            
        when(assetClassificationService.getCompanyName("RELIANCE"))
            .thenReturn(Optional.of("Reliance Industries Limited"));
            
        when(assetClassificationService.getSector("RELIANCE"))
            .thenReturn(Optional.of("Oil & Gas"));
            
        when(assetClassificationService.getAssetClass("RELIANCE"))
            .thenReturn(Optional.of("EQUITY"));
        
        // When
        CompletableFuture<ConsolidatedPortfolio> future = 
            dataAggregationService.aggregatePortfolios(userId, brokerPortfolios);
        ConsolidatedPosition position = future.join().positions().get(0);
        
        // Then
        BigDecimal expectedAvgPrice = new BigDecimal("2333.3333"); // Weighted average
        assertThat(position.avgPrice()).isEqualByComparingTo(expectedAvgPrice);
        assertThat(position.totalQuantity()).isEqualTo(300L);
    }
    
    @Test
    @DisplayName("Should calculate P&L correctly")
    void shouldCalculatePnLCorrectly() {
        // Given
        String userId = "user123";
        BigDecimal avgPrice = new BigDecimal("2000.00");
        BigDecimal currentPrice = new BigDecimal("2500.00");
        long quantity = 100L;
        
        List<BrokerPortfolio> brokerPortfolios = List.of(
            createBrokerPortfolio("ZERODHA", "Zerodha", 
                List.of(createBrokerPosition("RELIANCE", (int) quantity, avgPrice)))
        );
        
        when(normalizationService.normalize(any(BrokerPosition.class), anyString(), anyString()))
            .thenReturn(createNormalizedPosition("RELIANCE", "ZERODHA", quantity, avgPrice));
        
        when(priceService.getCurrentPrice("RELIANCE"))
            .thenReturn(Optional.of(currentPrice));
            
        when(assetClassificationService.getCompanyName("RELIANCE"))
            .thenReturn(Optional.of("Reliance Industries Limited"));
            
        when(assetClassificationService.getSector("RELIANCE"))
            .thenReturn(Optional.of("Oil & Gas"));
            
        when(assetClassificationService.getAssetClass("RELIANCE"))
            .thenReturn(Optional.of("EQUITY"));
        
        // When
        CompletableFuture<ConsolidatedPortfolio> future = 
            dataAggregationService.aggregatePortfolios(userId, brokerPortfolios);
        ConsolidatedPosition position = future.join().positions().get(0);
        
        // Then
        BigDecimal expectedCost = avgPrice.multiply(BigDecimal.valueOf(quantity)); // 2000 * 100 = 200000
        BigDecimal expectedValue = currentPrice.multiply(BigDecimal.valueOf(quantity)); // 2500 * 100 = 250000
        BigDecimal expectedPnL = expectedValue.subtract(expectedCost); // 250000 - 200000 = 50000
        BigDecimal expectedPnLPercent = new BigDecimal("25.0000"); // 50000/200000 * 100 = 25%
        
        assertThat(position.totalCost()).isEqualByComparingTo(expectedCost);
        assertThat(position.currentValue()).isEqualByComparingTo(expectedValue);
        assertThat(position.unrealizedPnL()).isEqualByComparingTo(expectedPnL);
        assertThat(position.unrealizedPnLPercent()).isEqualByComparingTo(expectedPnLPercent);
    }
    
    @Test
    @DisplayName("Should handle price service failure gracefully")
    void shouldHandlePriceServiceFailureGracefully() {
        // Given
        String userId = "user123";
        List<BrokerPortfolio> brokerPortfolios = createTestBrokerPortfolios();
        
        when(normalizationService.normalize(any(BrokerPosition.class), anyString(), anyString()))
            .thenReturn(createTestNormalizedPosition("RELIANCE", "ZERODHA", "Zerodha"));
        
        // Price service returns empty (price unavailable)
        when(priceService.getCurrentPrice(anyString()))
            .thenReturn(Optional.empty());
            
        when(assetClassificationService.getCompanyName(anyString()))
            .thenReturn(Optional.of("Reliance Industries Limited"));
            
        when(assetClassificationService.getSector(anyString()))
            .thenReturn(Optional.of("Oil & Gas"));
            
        when(assetClassificationService.getAssetClass(anyString()))
            .thenReturn(Optional.of("EQUITY"));
        
        // When
        CompletableFuture<ConsolidatedPortfolio> future = 
            dataAggregationService.aggregatePortfolios(userId, brokerPortfolios);
        ConsolidatedPortfolio result = future.join();
        
        // Then - Should fallback to average price when current price unavailable
        assertThat(result).isNotNull();
        assertThat(result.positions()).isNotEmpty();
        ConsolidatedPosition position = result.positions().get(0);
        assertThat(position.currentPrice()).isEqualTo(position.avgPrice()); // Fallback
    }
    
    // Helper methods to create test data
    
    private List<BrokerPortfolio> createTestBrokerPortfolios() {
        return List.of(
            createBrokerPortfolio("ZERODHA", "Zerodha", 
                List.of(createBrokerPosition("RELIANCE", 100, new BigDecimal("2000.00")))),
            createBrokerPortfolio("UPSTOX", "Upstox",
                List.of(createBrokerPosition("TCS", 50, new BigDecimal("3500.00"))))
        );
    }
    
    private List<BrokerPortfolio> createTestBrokerPortfoliosWithSameSymbol() {
        return List.of(
            createBrokerPortfolio("ZERODHA", "Zerodha", 
                List.of(createBrokerPosition("RELIANCE", 100, new BigDecimal("2000.00")))),
            createBrokerPortfolio("UPSTOX", "Upstox",
                List.of(createBrokerPosition("RELIANCE", 50, new BigDecimal("2200.00"))))
        );
    }
    
    private BrokerPortfolio createBrokerPortfolio(String brokerId, String brokerName, 
                                                List<BrokerPosition> positions) {
        BigDecimal totalValue = positions.stream()
            .map(pos -> pos.ltp().multiply(BigDecimal.valueOf(pos.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        return BrokerPortfolio.builder()
            .brokerId(brokerId)
            .brokerName(brokerName)
            .userId(123L)
            .totalValue(totalValue)
            .totalInvestment(totalValue.multiply(new BigDecimal("0.9"))) // 10% profit assumption
            .dayPnl(new BigDecimal("1000.00"))
            .totalPnl(totalValue.multiply(new BigDecimal("0.1")))
            .positions(positions)
            .lastSynced(Instant.now())
            .build();
    }
    
    private BrokerPosition createBrokerPosition(String symbol, Integer quantity, BigDecimal price) {
        return BrokerPosition.builder()
            .symbol(symbol)
            .exchange("NSE")
            .quantity(quantity)
            .avgPrice(price)
            .ltp(price.multiply(new BigDecimal("1.05"))) // 5% gain
            .pnl(price.multiply(BigDecimal.valueOf(quantity)).multiply(new BigDecimal("0.05")))
            .positionType("LONG")
            .build();
    }
    
    private NormalizedBrokerPosition createTestNormalizedPosition(String symbol, String brokerId, String brokerName) {
        return NormalizedBrokerPosition.builder()
            .originalSymbol(symbol)
            .normalizedSymbol(symbol)
            .originalExchange("NSE")
            .normalizedExchange("NSE")
            .quantity(100L)
            .avgPrice(new BigDecimal("2000.00"))
            .ltp(new BigDecimal("2500.00"))
            .pnl(new BigDecimal("5000.00"))
            .dayChange(new BigDecimal("100.00"))
            .positionType("LONG")
            .brokerId(brokerId)
            .brokerName(brokerName)
            .build();
    }
    
    private NormalizedBrokerPosition createNormalizedPosition(String symbol, String brokerId, 
                                                            Long quantity, BigDecimal avgPrice) {
        return NormalizedBrokerPosition.builder()
            .originalSymbol(symbol)
            .normalizedSymbol(symbol)
            .originalExchange("NSE")
            .normalizedExchange("NSE")
            .quantity(quantity)
            .avgPrice(avgPrice)
            .ltp(avgPrice.multiply(new BigDecimal("1.1"))) // 10% gain
            .pnl(avgPrice.multiply(BigDecimal.valueOf(quantity)).multiply(new BigDecimal("0.1")))
            .dayChange(new BigDecimal("50.00"))
            .positionType("LONG")
            .brokerId(brokerId)
            .brokerName(brokerId.toLowerCase())
            .build();
    }
}