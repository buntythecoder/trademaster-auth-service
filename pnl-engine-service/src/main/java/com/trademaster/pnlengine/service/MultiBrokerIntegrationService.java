package com.trademaster.pnlengine.service;

import com.trademaster.pnlengine.dto.PnLResultDTOs.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-Broker Integration Service Interface
 * 
 * MANDATORY: Java 24 + Virtual Threads + Functional Programming + Zero Placeholders
 * 
 * High-performance integration interface for coordinating P&L calculations
 * across multiple broker platforms with real-time data synchronization.
 * 
 * Core Features:
 * - Real-time broker data synchronization
 * - Cross-broker position aggregation
 * - Broker connection health monitoring
 * - Multi-broker transaction processing
 * - Account balance reconciliation
 * 
 * Performance Targets:
 * - Broker data retrieval: <100ms per broker
 * - Position synchronization: <50ms per position
 * - Connection health check: <30ms per broker
 * - Transaction processing: <25ms per transaction
 * 
 * Integration Points:
 * - Broker Authentication Service: Session management and credentials
 * - Market Data Service: Real-time price feeds
 * - Notification Service: Connection status alerts
 * - Audit Service: Transaction and access logging
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker P&L Engine)
 */
public interface MultiBrokerIntegrationService {
    
    /**
     * Get comprehensive broker account data for P&L calculations
     * 
     * @param userId User identifier
     * @param brokerType Target broker platform
     * @return Broker account data with balances and metadata
     */
    CompletableFuture<BrokerAccountData> getBrokerData(String userId, BrokerType brokerType);
    
    /**
     * Retrieve all positions from specific broker
     * 
     * @param userId User identifier
     * @param brokerType Target broker platform
     * @return List of positions for P&L calculation
     */
    CompletableFuture<List<BrokerPosition>> getBrokerPositions(String userId, BrokerType brokerType);
    
    /**
     * Get broker connection and synchronization status
     * 
     * @param userId User identifier
     * @param brokerType Target broker platform
     * @return Current connection status and health metrics
     */
    CompletableFuture<BrokerConnectionInfo> getBrokerConnectionStatus(String userId, BrokerType brokerType);
    
    /**
     * Retrieve historical transactions for P&L attribution
     * 
     * @param userId User identifier
     * @param brokerType Target broker platform
     * @param fromDate Start date for transaction history
     * @param toDate End date for transaction history
     * @return List of transactions for analysis
     */
    CompletableFuture<List<BrokerTransaction>> getBrokerTransactions(String userId, BrokerType brokerType, 
                                                                   Instant fromDate, Instant toDate);
    
    /**
     * Synchronize positions across all connected brokers
     * 
     * @param userId User identifier
     * @return Aggregated cross-broker position summary
     */
    CompletableFuture<CrossBrokerPositionSummary> synchronizeAllBrokerPositions(String userId);
    
    /**
     * Get consolidated account balances across all brokers
     * 
     * @param userId User identifier
     * @return Consolidated balance information
     */
    CompletableFuture<ConsolidatedBalance> getConsolidatedBalances(String userId);
    
    // Supporting data records
    
    record BrokerAccountData(
        String accountId,
        BrokerType brokerType,
        BigDecimal cashBalance,
        BigDecimal investedAmount,
        BigDecimal marginAvailable,
        BigDecimal marginUsed,
        String accountStatus,
        Instant lastSyncTime,
        String correlationId
    ) {}
    
    record BrokerPosition(
        String symbol,
        String companyName,
        String sector,
        String assetClass,
        Integer quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnL,
        BigDecimal dayPnL,
        Integer holdingDays,
        Instant lastUpdated,
        String brokerPositionId
    ) {}
    
    record BrokerConnectionInfo(
        BrokerType brokerType,
        boolean isConnected,
        String connectionStatus,
        Instant lastSuccessfulSync,
        Integer consecutiveFailures,
        String lastError,
        Long avgResponseTimeMs,
        Instant statusTimestamp
    ) {}
    
    record BrokerTransaction(
        String transactionId,
        String symbol,
        String transactionType,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        BigDecimal fees,
        Instant executionTime,
        String orderId,
        String brokerTransactionId
    ) {}
    
    record CrossBrokerPositionSummary(
        String userId,
        Integer totalPositions,
        Integer activeBrokers,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnL,
        List<ConsolidatedPosition> consolidatedPositions,
        Instant synchronizedAt
    ) {}
    
    record ConsolidatedPosition(
        String symbol,
        Integer totalQuantity,
        BigDecimal weightedAverageCost,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnL,
        List<BrokerPositionBreakdown> brokerBreakdown
    ) {}
    
    record ConsolidatedBalance(
        String userId,
        BigDecimal totalCashBalance,
        BigDecimal totalInvestedAmount,
        BigDecimal totalMarginAvailable,
        BigDecimal totalMarginUsed,
        List<BrokerBalanceBreakdown> brokerBreakdown,
        Instant consolidatedAt
    ) {}
    
    record BrokerBalanceBreakdown(
        BrokerType brokerType,
        String accountId,
        BigDecimal cashBalance,
        BigDecimal investedAmount,
        BigDecimal marginAvailable,
        BigDecimal marginUsed,
        String accountStatus
    ) {}
}