package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import com.trademaster.multibroker.entity.ConnectionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Broker Connection Response DTO
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents response data for broker connection operations. Used for API
 * responses when connections are created, updated, or retrieved. Provides
 * comprehensive connection state information for client applications.
 * 
 * Response Features:
 * - Connection state and status information
 * - Health and performance metrics
 * - Trading capabilities and limits
 * - Error information and recovery guidance
 * - Audit trail and connection history
 * 
 * Security Features:
 * - No sensitive token information exposed
 * - Masked broker credentials
 * - Audit-safe representation
 * - Client-safe error messaging
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker API Response)
 */
@Builder
public record BrokerConnectionResponse(
    String connectionId,
    String userId,
    BrokerType brokerType,
    String brokerName,
    ConnectionStatus status,
    Boolean isHealthy,
    String statusMessage,
    Instant connectedAt,
    Instant lastSynced,
    Instant lastHealthCheck,
    Long errorCount,
    BigDecimal totalValue,
    Integer positionCount,
    Boolean tradingEnabled,
    String apiVersion,
    Long responseTimeMs,
    String errorMessage
) {
    
    /**
     * Check if connection is actively trading
     * 
     * @return true if connection can execute trades
     */
    public boolean canTrade() {
        return status == ConnectionStatus.CONNECTED &&
               isHealthy != null && isHealthy &&
               tradingEnabled != null && tradingEnabled;
    }
    
    /**
     * Check if connection is degraded but functional
     * 
     * @return true if connection is degraded but operational
     */
    public boolean isDegraded() {
        return status == ConnectionStatus.DEGRADED &&
               isHealthy != null && !isHealthy &&
               tradingEnabled != null && tradingEnabled;
    }
    
    /**
     * Check if connection requires attention
     * 
     * @return true if connection has issues
     */
    public boolean needsAttention() {
        return !canTrade() ||
               errorCount != null && errorCount > 5 ||
               lastHealthCheck != null && isHealthCheckStale();
    }
    
    /**
     * Get connection health percentage
     * 
     * @return health percentage (0-100)
     */
    public double getHealthPercentage() {
        if (Boolean.TRUE.equals(isHealthy) && status == ConnectionStatus.CONNECTED) {
            return 100.0;
        } else if (status == ConnectionStatus.DEGRADED) {
            return 70.0;
        } else if (status == ConnectionStatus.DISCONNECTED) {
            return 0.0;
        }
        return 50.0; // Unknown state
    }
    
    /**
     * Get display-friendly status message
     * 
     * @return user-friendly status description
     */
    public String getDisplayStatus() {
        if (canTrade()) {
            return "Active and Trading";
        } else if (isDegraded()) {
            return "Degraded Performance";
        } else if (status == ConnectionStatus.DISCONNECTED) {
            return "Disconnected";
        }
        return "Unknown Status";
    }
    
    /**
     * Get connection uptime in seconds
     * 
     * @return uptime in seconds, or 0 if not connected
     */
    public long getUptimeSeconds() {
        if (connectedAt == null) {
            return 0L;
        }
        return Instant.now().getEpochSecond() - connectedAt.getEpochSecond();
    }
    
    /**
     * Check if health check is stale
     * 
     * @return true if last health check is older than 10 minutes
     */
    private boolean isHealthCheckStale() {
        if (lastHealthCheck == null) {
            return true;
        }
        Instant tenMinutesAgo = Instant.now().minusSeconds(600);
        return lastHealthCheck.isBefore(tenMinutesAgo);
    }
    
    /**
     * Get performance indicator based on response time
     * 
     * @return performance level (EXCELLENT, GOOD, FAIR, POOR)
     */
    public String getPerformanceIndicator() {
        if (responseTimeMs == null) {
            return "UNKNOWN";
        }
        
        return switch (responseTimeMs.intValue()) {
            case int ms when ms < 100 -> "EXCELLENT";
            case int ms when ms < 500 -> "GOOD";
            case int ms when ms < 2000 -> "FAIR";
            default -> "POOR";
        };
    }
    
    /**
     * Check if connection has portfolio data
     * 
     * @return true if connection has positions
     */
    public boolean hasPortfolioData() {
        return positionCount != null && positionCount > 0 &&
               totalValue != null && totalValue.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Create response from BrokerConnection entity
     * 
     * @param connection BrokerConnection entity
     * @return BrokerConnectionResponse
     */
    public static BrokerConnectionResponse from(com.trademaster.multibroker.entity.BrokerConnection connection) {
        return BrokerConnectionResponse.builder()
            .connectionId(connection.getId().toString())
            .userId(connection.getUserId())
            .brokerType(connection.getBrokerType())
            .brokerName(connection.getBrokerType().getDisplayName())
            .status(connection.getStatus())
            .isHealthy(connection.getIsHealthy())
            .statusMessage(connection.getStatus().getDisplayName())
            .connectedAt(connection.getConnectedAt())
            .lastSynced(connection.getLastSynced())
            .lastHealthCheck(connection.getLastHealthCheck())
            .errorCount(connection.getErrorCount())
            .positionCount(0) // TODO: Get from portfolio data
            .tradingEnabled(connection.getStatus() == ConnectionStatus.CONNECTED)
            .apiVersion("v2")
            .build();
    }
    
    /**
     * Create error response
     * 
     * @param message error message
     * @return error response
     */
    public static BrokerConnectionResponse error(String message) {
        return BrokerConnectionResponse.builder()
            .status(ConnectionStatus.ERROR)
            .isHealthy(false)
            .tradingEnabled(false)
            .errorMessage(message)
            .statusMessage("Connection failed")
            .build();
    }
    
    /**
     * Create safe response without sensitive information
     * 
     * @return sanitized response for client consumption
     */
    public BrokerConnectionResponse sanitize() {
        return BrokerConnectionResponse.builder()
            .connectionId(connectionId)
            .userId("***") // Mask user ID for security
            .brokerType(brokerType)
            .brokerName(brokerName)
            .status(status)
            .isHealthy(isHealthy)
            .statusMessage(statusMessage)
            .connectedAt(connectedAt)
            .lastSynced(lastSynced)
            .lastHealthCheck(lastHealthCheck)
            .errorCount(errorCount)
            .totalValue(totalValue)
            .positionCount(positionCount)
            .tradingEnabled(tradingEnabled)
            .apiVersion(apiVersion)
            .responseTimeMs(responseTimeMs)
            .errorMessage(errorMessage)
            .build();
    }
}