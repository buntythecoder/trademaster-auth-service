package com.trademaster.multibroker.repository;

import com.trademaster.multibroker.entity.BrokerConnection;
import com.trademaster.multibroker.entity.BrokerType;
import com.trademaster.multibroker.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Broker Connection Repository
 * 
 * MANDATORY: Spring Data JPA + Custom Queries + Performance Optimization
 * 
 * Data access layer for broker connection management with optimized queries
 * for high-performance operations. Includes custom queries for health monitoring,
 * connection analytics, and administrative operations.
 * 
 * Query Optimization:
 * - Indexed queries for user and broker lookups
 * - Batch operations for bulk updates
 * - Custom queries for complex filtering
 * - Native queries for performance-critical operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Connection Data Access)
 */
@Repository
public interface BrokerConnectionRepository extends JpaRepository<BrokerConnection, UUID> {
    
    /**
     * Find connections by user ID
     * 
     * @param userId User identifier
     * @return List of user's connections
     */
    List<BrokerConnection> findByUserId(String userId);
    
    /**
     * Find connections by user ID ordered by connection date
     * 
     * @param userId User identifier
     * @return List of connections ordered by connected date descending
     */
    List<BrokerConnection> findByUserIdOrderByConnectedAtDesc(String userId);
    
    /**
     * Find connection by user ID and broker type
     * 
     * @param userId User identifier
     * @param brokerType Broker type
     * @return Optional connection
     */
    Optional<BrokerConnection> findByUserIdAndBrokerType(String userId, BrokerType brokerType);
    
    /**
     * Find connection by ID and user ID (for authorization)
     * 
     * @param id Connection ID
     * @param userId User ID
     * @return Optional connection if user is authorized
     */
    Optional<BrokerConnection> findByIdAndUserId(UUID id, String userId);
    
    /**
     * Find connections by user ID and status
     * 
     * @param userId User identifier
     * @param statuses List of connection statuses
     * @return List of connections with specified statuses
     */
    List<BrokerConnection> findByUserIdAndStatusIn(String userId, List<ConnectionStatus> statuses);
    
    /**
     * Find connections by status
     * 
     * @param statuses List of connection statuses
     * @return List of connections with specified statuses
     */
    List<BrokerConnection> findByStatusIn(List<ConnectionStatus> statuses);
    
    /**
     * Find healthy connections
     * 
     * @return List of healthy connections
     */
    List<BrokerConnection> findByIsHealthyTrue();
    
    /**
     * Find unhealthy connections
     * 
     * @return List of unhealthy connections
     */
    List<BrokerConnection> findByIsHealthyFalse();
    
    /**
     * Find stale connections (not synced recently)
     * 
     * @param cutoffTime Cutoff time for staleness
     * @return List of stale connections
     */
    @Query("SELECT bc FROM BrokerConnection bc WHERE bc.lastSynced < :cutoffTime")
    List<BrokerConnection> findStaleConnections(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Find connections that need health check
     * 
     * @param cutoffTime Cutoff time for last health check
     * @return List of connections needing health check
     */
    @Query("SELECT bc FROM BrokerConnection bc WHERE bc.lastHealthCheck < :cutoffTime OR bc.lastHealthCheck IS NULL")
    List<BrokerConnection> findConnectionsNeedingHealthCheck(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Count connections by user ID
     * 
     * @param userId User identifier
     * @return Number of connections for user
     */
    long countByUserId(String userId);
    
    /**
     * Count healthy connections by user ID
     * 
     * @param userId User identifier
     * @return Number of healthy connections for user
     */
    long countByUserIdAndIsHealthyTrue(String userId);
    
    /**
     * Count connections by status
     * 
     * @param status Connection status
     * @return Number of connections with status
     */
    long countByStatus(ConnectionStatus status);
    
    /**
     * Get connection statistics for admin dashboard
     * 
     * @return Connection statistics
     */
    @Query("""
        SELECT new map(
            COUNT(*) as totalConnections,
            SUM(CASE WHEN bc.isHealthy = true THEN 1 ELSE 0 END) as healthyConnections,
            SUM(CASE WHEN bc.status = 'CONNECTED' THEN 1 ELSE 0 END) as connectedCount,
            SUM(CASE WHEN bc.status = 'DEGRADED' THEN 1 ELSE 0 END) as degradedCount,
            SUM(CASE WHEN bc.status = 'DISCONNECTED' THEN 1 ELSE 0 END) as disconnectedCount
        )
        FROM BrokerConnection bc
        """)
    java.util.Map<String, Object> getConnectionStatistics();
    
    /**
     * Get user connection summary
     * 
     * @param userId User identifier
     * @return User connection summary
     */
    @Query("""
        SELECT new map(
            COUNT(*) as totalConnections,
            SUM(CASE WHEN bc.isHealthy = true THEN 1 ELSE 0 END) as healthyConnections,
            SUM(CASE WHEN bc.status = 'CONNECTED' THEN 1 ELSE 0 END) as connectedCount,
            MAX(bc.lastSynced) as lastSyncTime
        )
        FROM BrokerConnection bc
        WHERE bc.userId = :userId
        """)
    java.util.Map<String, Object> getUserConnectionSummary(@Param("userId") String userId);
    
    /**
     * Find connections by status
     * 
     * @param status Connection status
     * @return List of connections with specified status
     */
    List<BrokerConnection> findByStatus(ConnectionStatus status);
    
    /**
     * Find connections by broker type and status
     * 
     * @param brokerType Broker type
     * @param status Connection status
     * @return List of connections for broker type with specified status
     */
    List<BrokerConnection> findByBrokerTypeAndStatus(BrokerType brokerType, ConnectionStatus status);
    
    /**
     * Find all connections by user ID and broker type (multiple results)
     * 
     * @param userId User identifier
     * @param brokerType Broker type
     * @return List of connections for user and broker type
     */
    List<BrokerConnection> findAllByUserIdAndBrokerType(String userId, BrokerType brokerType);
}