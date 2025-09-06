package com.trademaster.pnlengine.service.impl;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import com.trademaster.pnlengine.entity.PnLCalculationResult;
import com.trademaster.pnlengine.repository.PnLCalculationResultRepository;
import com.trademaster.pnlengine.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for P&L Calculation Engine Implementation
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * Comprehensive test coverage for P&L calculation engine including:
 * - Multi-broker P&L aggregation
 * - Performance attribution analysis  
 * - Tax optimization calculations
 * - Real-time streaming functionality
 * - Batch processing operations
 * - Historical trend analysis
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
@ExtendWith(MockitoExtension.class)
class PnLCalculationEngineImplTest {
    
    @Mock private MultiBrokerIntegrationService multiBrokerService;
    @Mock private MarketDataService marketDataService;
    @Mock private PortfolioIntegrationService portfolioService;
    @Mock private NotificationIntegrationService notificationService;
    @Mock private PnLCacheService cacheService;
    @Mock private PnLAuditService auditService;
    @Mock private PnLValidationService validationService;
    @Mock private TaxCalculationService taxService;
    @Mock private PerformanceAnalyticsService performanceService;
    @Mock private PnLCalculationResultRepository pnlRepository;
    
    private PnLCalculationEngineImpl pnlEngine;
    
    private static final String TEST_USER_ID = "test-user-123";
    private static final Long TEST_PORTFOLIO_ID = 1L;
    private static final String TEST_SYMBOL = "AAPL";
    private static final String TEST_CORRELATION_ID = "correlation-123";
    
    @BeforeEach
    void setUp() {
        pnlEngine = new PnLCalculationEngineImpl(
            multiBrokerService, marketDataService, portfolioService, notificationService,
            cacheService, auditService, validationService, taxService, performanceService,
            pnlRepository
        );
    }
    
    // ============================================================================
    // CORE P&L CALCULATION TESTS
    // ============================================================================
    
    @Test
    void calculateMultiBrokerPnL_ShouldAggregateAllBrokers() throws Exception {
        // Given: Mock broker P&L results for multiple brokers
        var zerodhaResult = createMockBrokerPnLResult(BrokerType.ZERODHA, BigDecimal.valueOf(50000.00), BigDecimal.valueOf(2500.00));
        var upstoxResult = createMockBrokerPnLResult(BrokerType.UPSTOX, BigDecimal.valueOf(30000.00), BigDecimal.valueOf(1500.00));
        
        when(multiBrokerService.getBrokerData(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerAccountData()));
        when(multiBrokerService.getBrokerPositions(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerPositions()));
        when(multiBrokerService.getBrokerConnectionStatus(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockConnectionStatus()));
        
        when(marketDataService.getCurrentPrice(anyString()))
            .thenReturn(Optional.of(BigDecimal.valueOf(150.00)));
        
        when(pnlRepository.save(any(PnLCalculationResult.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: Calculating multi-broker P&L
        var futureResult = pnlEngine.calculateMultiBrokerPnL(TEST_USER_ID, TEST_PORTFOLIO_ID);
        var result = futureResult.get();
        
        // Then: Should aggregate all broker results
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.portfolioId()).isEqualTo(TEST_PORTFOLIO_ID);
        assertThat(result.totalPortfolioValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.activeBrokers()).isGreaterThan(0);
        assertThat(result.calculationTimeMs()).isGreaterThan(0);
        assertThat(result.correlationId()).isNotNull();
        
        // Verify audit logging
        verify(auditService, never()).logOperation(anyString(), anyString(), anyString(), anyString(), any());
        
        // Verify caching not called for calculation (only retrieval)
        verify(cacheService, never()).clearUserCache(anyString());
    }
    
    @Test
    void calculateBrokerPnL_ShouldCalculateForSpecificBroker() throws Exception {
        // Given: Mock data for specific broker
        when(multiBrokerService.getBrokerData(TEST_USER_ID, BrokerType.ZERODHA))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerAccountData()));
        when(multiBrokerService.getBrokerPositions(TEST_USER_ID, BrokerType.ZERODHA))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerPositions()));
        when(multiBrokerService.getBrokerConnectionStatus(TEST_USER_ID, BrokerType.ZERODHA))
            .thenReturn(CompletableFuture.completedFuture(createMockConnectionStatus()));
        
        when(marketDataService.getCurrentPrice(anyString()))
            .thenReturn(Optional.of(BigDecimal.valueOf(150.00)));
        when(pnlRepository.findLatestByUserIdAndBrokerType(TEST_USER_ID, BrokerType.ZERODHA))
            .thenReturn(Optional.of(createMockPnLCalculationResult()));
        
        // When: Calculating broker-specific P&L
        var futureResult = pnlEngine.calculateBrokerPnL(TEST_USER_ID, BrokerType.ZERODHA);
        var result = futureResult.get();
        
        // Then: Should return broker-specific result
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.brokerType()).isEqualTo(BrokerType.ZERODHA);
        assertThat(result.portfolioValue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.calculationTimeMs()).isGreaterThan(0);
        
        // Verify repository interaction for realized P&L
        verify(pnlRepository).findLatestByUserIdAndBrokerType(TEST_USER_ID, BrokerType.ZERODHA);
    }
    
    @Test
    void calculatePositionPnL_ShouldCalculateAcrossAllBrokers() throws Exception {
        // Given: Mock positions across multiple brokers for same symbol
        var positions = List.of(
            createMockBrokerPosition(BrokerType.ZERODHA, TEST_SYMBOL, 100, BigDecimal.valueOf(140.00)),
            createMockBrokerPosition(BrokerType.UPSTOX, TEST_SYMBOL, 50, BigDecimal.valueOf(145.00))
        );
        
        when(multiBrokerService.getBrokerPositions(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(positions));
        when(marketDataService.getCurrentPrice(TEST_SYMBOL))
            .thenReturn(Optional.of(BigDecimal.valueOf(150.00)));
        
        // When: Calculating position P&L
        var futureResult = pnlEngine.calculatePositionPnL(TEST_USER_ID, TEST_SYMBOL);
        var results = futureResult.get();
        
        // Then: Should return position results from all brokers
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(pos -> TEST_SYMBOL.equals(pos.symbol()));
        assertThat(results).allMatch(pos -> TEST_USER_ID.equals(pos.userId()));
        assertThat(results).allMatch(pos -> pos.unrealizedPnL() != null);
    }
    
    @Test
    void calculateUnrealizedPnL_ShouldSumPositionUnrealizedPnL() throws Exception {
        // Given: Mock positions with unrealized gains
        var positions = List.of(
            createMockBrokerPosition(BrokerType.ZERODHA, "AAPL", 100, BigDecimal.valueOf(140.00)),
            createMockBrokerPosition(BrokerType.ZERODHA, "GOOGL", 10, BigDecimal.valueOf(2800.00))
        );
        
        when(multiBrokerService.getBrokerPositions(TEST_USER_ID, BrokerType.ZERODHA))
            .thenReturn(CompletableFuture.completedFuture(positions));
        when(marketDataService.getCurrentPrice("AAPL"))
            .thenReturn(Optional.of(BigDecimal.valueOf(150.00))); // $10 gain per share
        when(marketDataService.getCurrentPrice("GOOGL"))
            .thenReturn(Optional.of(BigDecimal.valueOf(2900.00))); // $100 gain per share
        
        // When: Calculating unrealized P&L
        var futureResult = pnlEngine.calculateUnrealizedPnL(TEST_USER_ID, BrokerType.ZERODHA);
        var result = futureResult.get();
        
        // Then: Should return sum of all unrealized gains/losses
        // Expected: (150-140)*100 + (2900-2800)*10 = 1000 + 1000 = 2000
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
    }
    
    // ============================================================================
    // TAX OPTIMIZATION TESTS
    // ============================================================================
    
    @Test
    void calculateTaxOptimizedPnL_ShouldOptimizeTaxLotSelection() throws Exception {
        // Given: Mock tax optimization service
        var taxOptimization = createMockTaxOptimization();
        when(taxService.optimizeTaxLotSelection(TEST_USER_ID, TEST_SYMBOL, 100, CostBasisMethod.FIFO))
            .thenReturn(taxOptimization);
        
        // When: Calculating tax-optimized P&L
        var futureResult = pnlEngine.calculateTaxOptimizedPnL(
            TEST_USER_ID, TEST_SYMBOL, 100, BigDecimal.valueOf(150.00), CostBasisMethod.FIFO);
        var result = futureResult.get();
        
        // Then: Should return tax-optimized result
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.symbol()).isEqualTo(TEST_SYMBOL);
        assertThat(result.sellQuantity()).isEqualTo(100);
        assertThat(result.currentPrice()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        
        // Verify tax service interaction
        verify(taxService).optimizeTaxLotSelection(TEST_USER_ID, TEST_SYMBOL, 100, CostBasisMethod.FIFO);
        verify(auditService).logOperation(eq(TEST_USER_ID), eq("TAX_OPTIMIZED_PNL"), anyString(), eq("SUCCESS"), isNull());
    }
    
    @Test
    void getConsolidatedTaxLots_ShouldReturnTaxLotInformation() throws Exception {
        // Given: Mock tax lots
        var taxLots = List.of(
            createMockTaxLotInfo("lot-1", 50, BigDecimal.valueOf(140.00)),
            createMockTaxLotInfo("lot-2", 30, BigDecimal.valueOf(145.00))
        );
        when(taxService.getConsolidatedTaxLots(TEST_USER_ID, TEST_SYMBOL, CostBasisMethod.WEIGHTED_AVERAGE))
            .thenReturn(taxLots);
        
        // When: Getting consolidated tax lots
        var futureResult = pnlEngine.getConsolidatedTaxLots(TEST_USER_ID, TEST_SYMBOL, CostBasisMethod.WEIGHTED_AVERAGE);
        var result = futureResult.get();
        
        // Then: Should return tax lot information
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(lot -> lot.symbol().equals(TEST_SYMBOL));
        assertThat(result).allMatch(lot -> lot.quantity() > 0);
        
        // Verify tax service interaction
        verify(taxService).getConsolidatedTaxLots(TEST_USER_ID, TEST_SYMBOL, CostBasisMethod.WEIGHTED_AVERAGE);
    }
    
    // ============================================================================
    // REAL-TIME STREAMING TESTS
    // ============================================================================
    
    @Test
    void subscribeToRealtimePnL_ShouldCreateSubscription() throws Exception {
        // Given: Subscription types
        var subscriptionTypes = List.of(PnLUpdateType.UNREALIZED_PNL, PnLUpdateType.POSITION_CHANGE);
        
        // When: Subscribing to real-time P&L updates
        var futureResult = pnlEngine.subscribeToRealtimePnL(TEST_USER_ID, subscriptionTypes);
        var result = futureResult.get();
        
        // Then: Should create subscription successfully
        assertThat(result).isNotNull();
        assertThat(result.subscriptionId()).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.subscriptionTypes()).containsAll(subscriptionTypes);
        assertThat(result.isActive()).isTrue();
        
        // Verify notification service interaction
        verify(notificationService).subscribeToUpdates(eq(TEST_USER_ID), anyString(), eq(subscriptionTypes));
        verify(auditService).logOperation(eq(TEST_USER_ID), eq("PNL_SUBSCRIPTION"), anyString(), eq("SUCCESS"), isNull());
    }
    
    @Test
    void unsubscribeFromRealtimePnL_ShouldRemoveSubscription() throws Exception {
        // Given: Subscription ID
        var subscriptionId = "subscription-123";
        
        // When: Unsubscribing from real-time P&L updates
        var futureResult = pnlEngine.unsubscribeFromRealtimePnL(TEST_USER_ID, subscriptionId);
        futureResult.get(); // Wait for completion
        
        // Then: Should remove subscription
        verify(notificationService).unsubscribeFromUpdates(TEST_USER_ID, subscriptionId);
        verify(auditService).logOperation(eq(TEST_USER_ID), eq("PNL_UNSUBSCRIPTION"), anyString(), eq("SUCCESS"), isNull());
    }
    
    @Test
    void getPnLStreamingStatus_ShouldReturnSubscriptionStatus() throws Exception {
        // Given: Mock active subscriptions
        var subscriptions = List.of("sub-1", "sub-2");
        when(notificationService.getActiveSubscriptions(TEST_USER_ID)).thenReturn(subscriptions);
        
        // When: Getting streaming status
        var futureResult = pnlEngine.getPnLStreamingStatus(TEST_USER_ID);
        var result = futureResult.get();
        
        // Then: Should return subscription status
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.activeSubscriptions()).isEqualTo(2);
        assertThat(result.subscriptionIds()).containsAll(subscriptions);
        assertThat(result.isStreaming()).isTrue();
        
        // Verify notification service interaction
        verify(notificationService).getActiveSubscriptions(TEST_USER_ID);
    }
    
    // ============================================================================
    // BATCH PROCESSING TESTS
    // ============================================================================
    
    @Test
    void calculateBatchPnL_ShouldProcessMultipleUsers() throws Exception {
        // Given: Multiple user IDs for batch processing
        var userIds = List.of("user-1", "user-2", "user-3");
        
        // Mock multi-broker service for all users
        when(multiBrokerService.getBrokerData(anyString(), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerAccountData()));
        when(multiBrokerService.getBrokerPositions(anyString(), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerPositions()));
        when(multiBrokerService.getBrokerConnectionStatus(anyString(), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockConnectionStatus()));
        when(marketDataService.getCurrentPrice(anyString()))
            .thenReturn(Optional.of(BigDecimal.valueOf(150.00)));
        when(pnlRepository.save(any(PnLCalculationResult.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: Processing batch P&L calculation
        var futureResult = pnlEngine.calculateBatchPnL(userIds);
        var results = futureResult.get();
        
        // Then: Should process all users
        assertThat(results).hasSize(userIds.size());
        assertThat(results).allMatch(result -> result != null);
        assertThat(results).allMatch(result -> userIds.contains(result.userId()));
    }
    
    @Test
    void recalculateAllPnL_ShouldRefreshAllBrokerCalculations() throws Exception {
        // Given: Force refresh flag
        var forceRefresh = true;
        
        // Mock broker calculations
        when(multiBrokerService.getBrokerData(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerAccountData()));
        when(multiBrokerService.getBrokerPositions(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockBrokerPositions()));
        when(multiBrokerService.getBrokerConnectionStatus(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(CompletableFuture.completedFuture(createMockConnectionStatus()));
        when(marketDataService.getCurrentPrice(anyString()))
            .thenReturn(Optional.of(BigDecimal.valueOf(150.00)));
        when(pnlRepository.findLatestByUserIdAndBrokerType(eq(TEST_USER_ID), any(BrokerType.class)))
            .thenReturn(Optional.of(createMockPnLCalculationResult()));
        
        // When: Recalculating all P&L
        var futureResult = pnlEngine.recalculateAllPnL(TEST_USER_ID, forceRefresh);
        var result = futureResult.get();
        
        // Then: Should recalculate all brokers and clear cache
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.brokersRecalculated()).isEqualTo(BrokerType.values().length);
        assertThat(result.calculationTimeMs()).isGreaterThan(0);
        assertThat(result.success()).isTrue();
        
        // Verify cache was cleared due to force refresh
        verify(cacheService).clearUserCache(TEST_USER_ID);
        verify(auditService).logOperation(eq(TEST_USER_ID), eq("FULL_PNL_RECALCULATION"), anyString(), eq("SUCCESS"), anyLong());
    }
    
    // ============================================================================
    // VALIDATION TESTS
    // ============================================================================
    
    @Test
    void validatePnLAccuracy_ShouldValidateCalculationAccuracy() throws Exception {
        // Given: Mock validation results
        var validationResults = List.of(
            createMockValidationResult(BrokerType.ZERODHA, BigDecimal.valueOf(0.005)), // 0.5% tolerance - valid
            createMockValidationResult(BrokerType.UPSTOX, BigDecimal.valueOf(0.008))   // 0.8% tolerance - valid
        );
        when(validationService.validatePnLAccuracy(TEST_USER_ID)).thenReturn(validationResults);
        
        // When: Validating P&L accuracy
        var futureResult = pnlEngine.validatePnLAccuracy(TEST_USER_ID);
        var result = futureResult.get();
        
        // Then: Should return validation results
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.isValid()).isTrue(); // All tolerances under 1% threshold
        assertThat(result.validationResults()).hasSize(2);
        
        // Verify validation service interaction
        verify(validationService).validatePnLAccuracy(TEST_USER_ID);
        verify(auditService).logOperation(eq(TEST_USER_ID), eq("PNL_VALIDATION"), anyString(), eq("SUCCESS"), isNull());
    }
    
    // ============================================================================
    // HISTORICAL ANALYSIS TESTS
    // ============================================================================
    
    @Test
    void getHistoricalPnLTrend_ShouldReturnTrendData() throws Exception {
        // Given: Mock historical trend data from repository
        var trendData = List.of(
            new Object[]{Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS), BigDecimal.valueOf(95000.00)},
            new Object[]{Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS), BigDecimal.valueOf(100000.00)}
        );
        when(pnlRepository.getPortfolioValueTrend(eq(TEST_USER_ID), any(Instant.class), any(Instant.class)))
            .thenReturn(trendData);
        
        // When: Getting historical P&L trend
        var futureResult = pnlEngine.getHistoricalPnLTrend(TEST_USER_ID, 7, DataGranularity.DAILY);
        var result = futureResult.get();
        
        // Then: Should return trend analysis
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.periodDays()).isEqualTo(7);
        assertThat(result.granularity()).isEqualTo(DataGranularity.DAILY);
        assertThat(result.trendPoints()).hasSize(2);
        
        // Verify repository interaction
        verify(pnlRepository).getPortfolioValueTrend(eq(TEST_USER_ID), any(Instant.class), any(Instant.class));
    }
    
    // ============================================================================
    // HELPER METHODS FOR MOCK DATA CREATION
    // ============================================================================
    
    private BrokerPnLResult createMockBrokerPnLResult(BrokerType brokerType, BigDecimal portfolioValue, BigDecimal unrealizedPnL) {
        return new BrokerPnLResult(
            TEST_USER_ID, brokerType, "account-123", portfolioValue, BigDecimal.valueOf(5000.00), 
            portfolioValue.subtract(unrealizedPnL), unrealizedPnL, BigDecimal.valueOf(1000.00), 
            BigDecimal.valueOf(500.00), BigDecimal.valueOf(5.56), unrealizedPnL, 5,
            List.of(), ConnectionStatus.CONNECTED, Instant.now(), Instant.now(), 25L
        );
    }
    
    private com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerAccountData createMockBrokerAccountData() {
        return new com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerAccountData(
            "account-123", TEST_USER_ID, BrokerType.ZERODHA, BigDecimal.valueOf(5000.00),
            BigDecimal.valueOf(45000.00), BigDecimal.valueOf(50000.00), Instant.now(), true
        );
    }
    
    private List<com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerPosition> createMockBrokerPositions() {
        return List.of(
            createMockBrokerPosition(BrokerType.ZERODHA, "AAPL", 100, BigDecimal.valueOf(140.00)),
            createMockBrokerPosition(BrokerType.ZERODHA, "GOOGL", 10, BigDecimal.valueOf(2800.00))
        );
    }
    
    private com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerPosition createMockBrokerPosition(
            BrokerType brokerType, String symbol, int quantity, BigDecimal averageCost) {
        return new com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerPosition(
            TEST_USER_ID, brokerType, symbol, "Apple Inc.", "Technology", "Equity",
            quantity, averageCost, averageCost.add(BigDecimal.valueOf(5.00)), // Current price slightly higher
            averageCost.multiply(BigDecimal.valueOf(quantity)), null, null, Instant.now()
        );
    }
    
    private com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerConnectionStatus createMockConnectionStatus() {
        return com.trademaster.pnlengine.domain.PnLDomainTypes.BrokerConnectionStatus.CONNECTED;
    }
    
    private PnLCalculationResult createMockPnLCalculationResult() {
        var result = new PnLCalculationResult();
        result.setUserId(TEST_USER_ID);
        result.setBrokerType(BrokerType.ZERODHA);
        result.setTotalRealizedPnl(BigDecimal.valueOf(1000.00));
        result.setCalculatedAt(Instant.now());
        return result;
    }
    
    private com.trademaster.pnlengine.domain.PnLDomainTypes.TaxOptimization createMockTaxOptimization() {
        var selectedLots = List.of(
            new com.trademaster.pnlengine.domain.PnLDomainTypes.TaxLot(
                "lot-1", TEST_USER_ID, TEST_SYMBOL, BrokerType.ZERODHA, 50, BigDecimal.valueOf(140.00),
                BigDecimal.valueOf(7000.00), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS),
                400, true, CostBasisMethod.FIFO, "trade-123",
                BigDecimal.valueOf(7500.00), BigDecimal.valueOf(500.00), Instant.now()
            )
        );
        return new com.trademaster.pnlengine.domain.PnLDomainTypes.TaxOptimization(
            selectedLots, BigDecimal.valueOf(500.00), BigDecimal.valueOf(400.00), 
            BigDecimal.valueOf(100.00), BigDecimal.valueOf(75.00), "FIFO selection minimizes short-term gains"
        );
    }
    
    private TaxLotInfo createMockTaxLotInfo(String lotId, int quantity, BigDecimal averageCost) {
        return new TaxLotInfo(
            lotId, TEST_USER_ID, TEST_SYMBOL, BrokerType.ZERODHA, quantity, averageCost,
            averageCost.multiply(BigDecimal.valueOf(quantity)), Instant.now().minus(200, java.time.temporal.ChronoUnit.DAYS),
            200, false, CostBasisMethod.FIFO, "trade-456", BigDecimal.valueOf(15000.00),
            BigDecimal.valueOf(1000.00), Instant.now()
        );
    }
    
    private com.trademaster.pnlengine.domain.PnLDomainTypes.ValidationResult createMockValidationResult(BrokerType brokerType, BigDecimal tolerance) {
        return new com.trademaster.pnlengine.domain.PnLDomainTypes.ValidationResult(
            brokerType, tolerance, BigDecimal.valueOf(50000.00), BigDecimal.valueOf(50200.00),
            "Portfolio value validation", Instant.now()
        );
    }
}