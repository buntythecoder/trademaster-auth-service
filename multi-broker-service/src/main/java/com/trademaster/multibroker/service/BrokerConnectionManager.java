package com.trademaster.multibroker.service;

import com.trademaster.multibroker.entity.BrokerConnection;
import com.trademaster.multibroker.entity.BrokerType;
import com.trademaster.multibroker.entity.ConnectionStatus;
import com.trademaster.multibroker.repository.BrokerConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broker Connection Manager
 * 
 * MANDATORY: Virtual Threads + Functional Composition + Zero Trust Security
 * 
 * Manages broker connection lifecycle including creation, validation, monitoring,
 * and cleanup. Implements secure connection state management with proper
 * authorization and audit logging.
 * 
 * Connection Lifecycle:
 * - Connection establishment with OAuth validation
 * - Health monitoring and status updates
 * - Token refresh and re-authentication
 * - Connection suspension and cleanup
 * - Audit trail for all connection events
 * 
 * Security Features:
 * - User-specific connection isolation
 * - Token encryption and secure storage
 * - Connection rate limiting
 * - Audit logging for security events
 * - Automatic cleanup of stale connections
 * 
 * Performance Features:
 * - Virtual thread-based operations
 * - Connection pooling and reuse
 * - Batch operations for efficiency
 * - Optimized database queries
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Connection Lifecycle Management)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerConnectionManager {
    
    private final BrokerConnectionRepository connectionRepository;
    private final EncryptionService encryptionService;
    private final BrokerOAuthService oauthService;
    
    // Virtual thread executor for connection operations
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * Create new broker connection
     * 
     * MANDATORY: Secure connection creation with token encryption
     * 
     * @param userId User identifier
     * @param brokerType Broker type
     * @param accessToken Broker access token
     * @param refreshToken Broker refresh token (optional)
     * @return CompletableFuture with created connection
     */
    @Transactional
    public CompletableFuture<BrokerConnection> createConnection(String userId,
                                                              BrokerType brokerType,
                                                              String accessToken,
                                                              String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Creating broker connection: userId={}, brokerType={}", userId, brokerType);
            
            // Check if connection already exists
            Optional<BrokerConnection> existingConnection = 
                connectionRepository.findByUserIdAndBrokerType(userId, brokerType);
            
            if (existingConnection.isPresent()) {
                log.info("Updating existing connection: userId={}, brokerType={}", userId, brokerType);
                return updateExistingConnection(existingConnection.get(), accessToken, refreshToken);
            }
            
            // Encrypt tokens for secure storage
            String encryptedAccessToken = encryptionService.encryptToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Failed to encrypt access token"));
            
            String encryptedRefreshToken = refreshToken != null ?
                encryptionService.encryptToken(refreshToken)
                    .orElse(null) : null;
            
            // Create new connection
            BrokerConnection connection = BrokerConnection.builder()
                .userId(userId)
                .brokerType(brokerType)
                .accountId(generateBrokerId(brokerType, userId))
                .encryptedAccessToken(encryptedAccessToken)
                .encryptedRefreshToken(encryptedRefreshToken)
                .status(ConnectionStatus.CONNECTED)
                .connectedAt(Instant.now())
                .lastSynced(Instant.now())
                .lastHealthCheck(Instant.now())
                .healthy(true)
                .syncCount(0L)
                .errorCount(0L)
                .build();
            
            // Save connection
            BrokerConnection savedConnection = connectionRepository.save(connection);
            
            log.info("Broker connection created: connectionId={}, userId={}, brokerType={}", 
                    savedConnection.getId(), userId, brokerType);
            
            return savedConnection;
            
        }, virtualThreadExecutor);
    }
    
    /**
     * Get user's broker connections
     * 
     * @param userId User identifier
     * @return CompletableFuture with list of connections
     */
    public CompletableFuture<List<BrokerConnection>> getUserConnections(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching user connections: userId={}", userId);
            return connectionRepository.findByUserIdOrderByConnectedAtDesc(userId);
        }, virtualThreadExecutor);
    }
    
    /**
     * Get active broker connections for user
     * 
     * @param userId User identifier
     * @return CompletableFuture with list of active connections
     */
    public CompletableFuture<List<BrokerConnection>> getActiveConnections(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching active connections: userId={}", userId);
            return connectionRepository.findByUserIdAndStatusIn(userId, 
                List.of(ConnectionStatus.CONNECTED, ConnectionStatus.DEGRADED));
        }, virtualThreadExecutor);
    }
    
    /**
     * Get connection by ID with user validation
     * 
     * @param connectionId Connection identifier
     * @param userId User identifier for authorization
     * @return Optional connection if authorized
     */
    public Optional<BrokerConnection> getConnection(String connectionId, String userId) {
        return connectionRepository.findByIdAndUserId(java.util.UUID.fromString(connectionId), userId);
    }
    
    /**
     * Update connection status
     * 
     * @param connectionId Connection identifier
     * @param status New connection status
     * @return CompletableFuture with updated connection
     */
    @Transactional
    public CompletableFuture<Optional<BrokerConnection>> updateConnectionStatus(String connectionId,
                                                                              ConnectionStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Updating connection status: connectionId={}, status={}", connectionId, status);
            
            Optional<BrokerConnection> connectionOpt = 
                connectionRepository.findById(java.util.UUID.fromString(connectionId));
            
            return connectionOpt.map(connection -> {
                BrokerConnection updatedConnection = connection.toBuilder()
                    .status(status)
                    .lastHealthCheck(Instant.now())
                    .healthy(status == ConnectionStatus.CONNECTED)
                    .build();
                
                return connectionRepository.save(updatedConnection);
            });
            
        }, virtualThreadExecutor);
    }
    
    /**
     * Refresh connection token
     * 
     * @param connectionId Connection identifier
     * @return CompletableFuture with token refresh result
     */
    @Transactional
    public CompletableFuture<Boolean> refreshConnectionToken(String connectionId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Refreshing connection token: connectionId={}", connectionId);
            
            Optional<BrokerConnection> connectionOpt = 
                connectionRepository.findById(java.util.UUID.fromString(connectionId));
            
            if (connectionOpt.isEmpty()) {
                log.warn("Connection not found for token refresh: connectionId={}", connectionId);
                return false;
            }
            
            BrokerConnection connection = connectionOpt.get();
            
            // Decrypt refresh token
            if (connection.getEncryptedRefreshToken() == null) {
                log.warn("No refresh token available: connectionId={}", connectionId);
                return false;
            }
            
            Optional<String> refreshTokenOpt = 
                encryptionService.decryptToken(connection.getEncryptedRefreshToken());
            
            if (refreshTokenOpt.isEmpty()) {
                log.error("Failed to decrypt refresh token: connectionId={}", connectionId);
                return false;
            }
            
            // Refresh tokens with broker
            return oauthService.refreshTokens(connection.getBrokerType(), refreshTokenOpt.get())
                .thenApply(newTokensOpt -> {
                    if (newTokensOpt.isEmpty()) {
                        log.warn("Token refresh failed: connectionId={}", connectionId);
                        updateConnectionStatus(connectionId, ConnectionStatus.TOKEN_EXPIRED);
                        return false;
                    }
                    
                    // Update connection with new tokens
                    BrokerOAuthService.BrokerTokens newTokens = newTokensOpt.get();
                    updateConnectionTokens(connection, newTokens);
                    
                    log.info("Token refresh successful: connectionId={}", connectionId);
                    return true;
                })
                .join();
                
        }, virtualThreadExecutor);
    }
    
    /**
     * Disconnect and remove broker connection
     * 
     * @param connectionId Connection identifier
     * @param userId User identifier for authorization
     * @return CompletableFuture with operation result
     */
    @Transactional
    public CompletableFuture<Boolean> disconnectBroker(String connectionId, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Disconnecting broker: connectionId={}, userId={}", connectionId, userId);
            
            Optional<BrokerConnection> connectionOpt = getConnection(connectionId, userId);
            
            if (connectionOpt.isEmpty()) {
                log.warn("Connection not found or unauthorized: connectionId={}, userId={}", 
                        connectionId, userId);
                return false;
            }
            
            BrokerConnection connection = connectionOpt.get();
            
            // Update status to disconnected
            BrokerConnection disconnectedConnection = connection.toBuilder()
                .status(ConnectionStatus.DISCONNECTED)
                .disconnectedAt(Instant.now())
                .healthy(false)
                .build();
            
            connectionRepository.save(disconnectedConnection);
            
            log.info("Broker disconnected: connectionId={}, userId={}, brokerType={}", 
                    connectionId, userId, connection.getBrokerType());
            
            return true;
            
        }, virtualThreadExecutor);
    }
    
    /**
     * Get connection health summary for user
     * 
     * @param userId User identifier
     * @return CompletableFuture with health summary
     */
    public CompletableFuture<ConnectionHealthSummary> getConnectionHealthSummary(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating connection health summary: userId={}", userId);
            
            List<BrokerConnection> connections = connectionRepository.findByUserId(userId);
            
            long totalConnections = connections.size();
            long healthyConnections = connections.stream()
                .mapToLong(conn -> conn.getIsHealthy() ? 1 : 0)
                .sum();
            long connectedCount = connections.stream()
                .mapToLong(conn -> conn.getStatus() == ConnectionStatus.CONNECTED ? 1 : 0)
                .sum();
            long degradedCount = connections.stream()
                .mapToLong(conn -> conn.getStatus() == ConnectionStatus.DEGRADED ? 1 : 0)
                .sum();
            long disconnectedCount = connections.stream()
                .mapToLong(conn -> conn.getStatus() == ConnectionStatus.DISCONNECTED ? 1 : 0)
                .sum();
            
            return ConnectionHealthSummary.builder()
                .userId(userId)
                .totalConnections(totalConnections)
                .healthyConnections(healthyConnections)
                .connectedCount(connectedCount)
                .degradedCount(degradedCount)
                .disconnectedCount(disconnectedCount)
                .overallHealthPercentage(totalConnections > 0 ? 
                    (healthyConnections * 100.0 / totalConnections) : 100.0)
                .lastChecked(Instant.now())
                .build();
                
        }, virtualThreadExecutor);
    }
    
    /**
     * Cleanup stale connections
     * 
     * @param maxAgeHours Maximum age for connections in hours
     * @return CompletableFuture with cleanup count
     */
    @Transactional
    public CompletableFuture<Long> cleanupStaleConnections(long maxAgeHours) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Cleaning up stale connections: maxAgeHours={}", maxAgeHours);
            
            Instant cutoffTime = Instant.now().minusSeconds(maxAgeHours * 3600);
            List<BrokerConnection> staleConnections = 
                connectionRepository.findStaleConnections(cutoffTime);
            
            staleConnections.forEach(connection -> {
                BrokerConnection updatedConnection = connection.toBuilder()
                    .status(ConnectionStatus.TOKEN_EXPIRED)
                    .healthy(false)
                    .build();
                    
                connectionRepository.save(updatedConnection);
            });
            
            log.info("Cleaned up {} stale connections", staleConnections.size());
            return (long) staleConnections.size();
            
        }, virtualThreadExecutor);
    }
    
    /**
     * Update existing connection with new tokens
     * 
     * @param existingConnection Existing connection
     * @param accessToken New access token
     * @param refreshToken New refresh token
     * @return Updated connection
     */
    private BrokerConnection updateExistingConnection(BrokerConnection existingConnection,
                                                    String accessToken,
                                                    String refreshToken) {
        // Encrypt new tokens
        String encryptedAccessToken = encryptionService.encryptToken(accessToken)
            .orElseThrow(() -> new RuntimeException("Failed to encrypt access token"));
        
        String encryptedRefreshToken = refreshToken != null ?
            encryptionService.encryptToken(refreshToken).orElse(null) : null;
        
        // Update existing connection
        BrokerConnection updatedConnection = existingConnection.toBuilder()
            .encryptedAccessToken(encryptedAccessToken)
            .encryptedRefreshToken(encryptedRefreshToken)
            .status(ConnectionStatus.CONNECTED)
            .lastSynced(Instant.now())
            .lastHealthCheck(Instant.now())
            .healthy(true)
            .build();
        
        return connectionRepository.save(updatedConnection);
    }
    
    /**
     * Update connection with refreshed tokens
     * 
     * @param connection Existing connection
     * @param newTokens New broker tokens
     */
    private void updateConnectionTokens(BrokerConnection connection, 
                                      BrokerOAuthService.BrokerTokens newTokens) {
        String encryptedAccessToken = encryptionService.encryptToken(newTokens.accessToken())
            .orElseThrow(() -> new RuntimeException("Failed to encrypt new access token"));
        
        String encryptedRefreshToken = newTokens.refreshToken() != null ?
            encryptionService.encryptToken(newTokens.refreshToken()).orElse(null) : null;
        
        BrokerConnection updatedConnection = connection.toBuilder()
            .encryptedAccessToken(encryptedAccessToken)
            .encryptedRefreshToken(encryptedRefreshToken)
            .lastSynced(Instant.now())
            .lastHealthCheck(Instant.now())
            .status(ConnectionStatus.CONNECTED)
            .healthy(true)
            .build();
        
        connectionRepository.save(updatedConnection);
    }
    
    /**
     * Generate unique broker ID
     * 
     * @param brokerType Broker type
     * @param userId User ID
     * @return Unique broker ID
     */
    private String generateBrokerId(BrokerType brokerType, String userId) {
        return String.format("%s_%s_%d", 
            brokerType.name().toLowerCase(),
            userId.replaceAll("[^a-zA-Z0-9]", ""),
            System.currentTimeMillis());
    }
    
    /**
     * Connection Health Summary
     */
    @lombok.Builder
    public record ConnectionHealthSummary(
        String userId,
        Long totalConnections,
        Long healthyConnections,
        Long connectedCount,
        Long degradedCount,
        Long disconnectedCount,
        Double overallHealthPercentage,
        Instant lastChecked
    ) {
        
        /**
         * Check if overall health is good
         * 
         * @return true if health percentage >= 80%
         */
        public boolean isHealthy() {
            return overallHealthPercentage != null && overallHealthPercentage >= 80.0;
        }
        
        /**
         * Get health status description
         * 
         * @return Health status description
         */
        public String getHealthStatus() {
            if (overallHealthPercentage == null) {
                return "Unknown";
            }
            
            return switch ((int) (overallHealthPercentage / 20)) {
                case 5, 4 -> "Excellent";
                case 3 -> "Good"; 
                case 2 -> "Fair";
                case 1 -> "Poor";
                default -> "Critical";
            };
        }
        
        /**
         * Create safe summary for logging
         * 
         * @return Safe summary string
         */
        public String toSafeSummary() {
            return String.format("HealthSummary[total=%d, healthy=%d, health=%.1f%%]",
                               totalConnections, healthyConnections, overallHealthPercentage);
        }
    }
    
    /**
     * Check if broker is connected for specific user
     * 
     * @param broker Broker type
     * @param userId User identifier
     * @return true if broker is connected for user
     */
    public boolean isBrokerConnectedForUser(BrokerType broker, String userId) {
        return connectionRepository.findAllByUserIdAndBrokerType(userId, broker)
            .stream()
            .anyMatch(conn -> conn.getStatus() == ConnectionStatus.CONNECTED && conn.getIsHealthy());
    }
    
    /**
     * Get all active users with broker connections
     * 
     * @return Set of user IDs with active connections
     */
    public java.util.Set<String> getAllActiveUsers() {
        return connectionRepository.findByStatus(ConnectionStatus.CONNECTED)
            .stream()
            .filter(BrokerConnection::getIsHealthy)
            .map(BrokerConnection::getUserId)
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Get active brokers (simplified version for compatibility)
     * 
     * @return List of active broker types
     */
    public List<BrokerType> getActiveBrokers() {
        return connectionRepository.findByStatus(ConnectionStatus.CONNECTED)
            .stream()
            .filter(BrokerConnection::getIsHealthy)
            .map(BrokerConnection::getBrokerType)
            .distinct()
            .toList();
    }
    
    /**
     * Check if broker is connected (general check)
     * 
     * @param broker Broker type
     * @return true if broker has any active connections
     */
    public boolean isBrokerConnected(BrokerType broker) {
        return connectionRepository.findByBrokerTypeAndStatus(broker, ConnectionStatus.CONNECTED)
            .stream()
            .anyMatch(BrokerConnection::getIsHealthy);
    }
}