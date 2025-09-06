package com.trademaster.portfolio.service;

import com.trademaster.portfolio.functional.Result;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Broker Integration Service Interface (Future Integration)
 * 
 * Defines contract for multi-broker integration and portfolio synchronization.
 * Supports major Indian brokers with standardized API abstraction.
 * 
 * Features:
 * - Multi-broker portfolio synchronization
 * - Real-time position updates
 * - Order placement and management
 * - Account balance monitoring
 * - Broker-specific authentication handling
 * 
 * Supported Brokers:
 * - Zerodha (Kite API)
 * - Upstox (Pro API)
 * - Angel One (SmartAPI)
 * - ICICI Direct
 * - HDFC Securities
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Service Interface)
 */
public interface BrokerIntegrationService {
    
    /**
     * Get all portfolios for a user across all connected brokers
     */
    CompletableFuture<Result<List<BrokerPortfolio>, BrokerIntegrationError>> 
        getAllUserPortfolios(Long userId);
    
    /**
     * Get portfolio from specific broker
     */
    CompletableFuture<Result<BrokerPortfolio, BrokerIntegrationError>> 
        getBrokerPortfolio(Long userId, String brokerId);
    
    /**
     * Synchronize portfolio data from broker
     */
    CompletableFuture<Result<SynchronizationResult, BrokerIntegrationError>> 
        synchronizePortfolio(Long userId, String brokerId);
    
    /**
     * Get account balance from broker
     */
    CompletableFuture<Result<AccountBalance, BrokerIntegrationError>> 
        getAccountBalance(Long userId, String brokerId);
    
    /**
     * Get trading limits and margins
     */
    CompletableFuture<Result<TradingLimits, BrokerIntegrationError>> 
        getTradingLimits(Long userId, String brokerId);
    
    /**
     * Validate broker connection and authentication
     */
    CompletableFuture<Result<BrokerStatus, BrokerIntegrationError>> 
        validateBrokerConnection(Long userId, String brokerId);
    
    /**
     * Get supported broker list
     */
    List<BrokerInfo> getSupportedBrokers();
    
    /**
     * Broker portfolio data
     */
    record BrokerPortfolio(
        String brokerId,
        String brokerName,
        Long userId,
        BigDecimal totalValue,
        BigDecimal totalInvestment,
        BigDecimal dayPnl,
        BigDecimal totalPnl,
        List<BrokerPosition> positions,
        java.time.Instant lastSynced
    ) {}
    
    /**
     * Broker position data
     */
    record BrokerPosition(
        String symbol,
        String exchange,
        Integer quantity,
        BigDecimal avgPrice,
        BigDecimal ltp, // Last Traded Price
        BigDecimal pnl,
        String positionType // LONG, SHORT
    ) {}
    
    /**
     * Account balance information
     */
    record AccountBalance(
        String brokerId,
        BigDecimal cashBalance,
        BigDecimal collateralValue,
        BigDecimal marginAvailable,
        BigDecimal marginUsed,
        String currency
    ) {}
    
    /**
     * Trading limits and margins
     */
    record TradingLimits(
        String brokerId,
        BigDecimal equityLimit,
        BigDecimal commodityLimit,
        BigDecimal currencyLimit,
        BigDecimal marginMultiplier,
        List<String> allowedSegments
    ) {}
    
    /**
     * Broker connection status
     */
    record BrokerStatus(
        String brokerId,
        String status, // CONNECTED, DISCONNECTED, ERROR
        String lastError,
        java.time.Instant lastValidated,
        boolean apiLimitReached
    ) {}
    
    /**
     * Broker information
     */
    record BrokerInfo(
        String brokerId,
        String brokerName,
        String apiVersion,
        List<String> supportedFeatures,
        boolean active
    ) {}
    
    /**
     * Synchronization result
     */
    record SynchronizationResult(
        String brokerId,
        int positionsUpdated,
        int positionsAdded,
        int positionsRemoved,
        boolean successful,
        String errorMessage,
        java.time.Instant syncTimestamp
    ) {}
    
    /**
     * Broker integration error types
     */
    sealed interface BrokerIntegrationError {
        
        record BrokerNotConnected(String brokerId) implements BrokerIntegrationError {}
        
        record AuthenticationFailed(String brokerId, String reason) implements BrokerIntegrationError {}
        
        record ApiLimitExceeded(String brokerId) implements BrokerIntegrationError {}
        
        record BrokerServiceDown(String brokerId, String status) implements BrokerIntegrationError {}
        
        record DataSyncError(String brokerId, String reason) implements BrokerIntegrationError {}
        
        record InvalidBrokerId(String brokerId) implements BrokerIntegrationError {}
        
        record NetworkError(String brokerId, String message) implements BrokerIntegrationError {}
    }
}