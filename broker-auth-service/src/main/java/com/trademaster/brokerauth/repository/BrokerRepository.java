package com.trademaster.brokerauth.repository;

import com.trademaster.brokerauth.entity.Broker;
import com.trademaster.brokerauth.enums.BrokerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Broker Repository
 * 
 * Data access layer for Broker entities.
 * Provides queries for broker configuration and availability.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface BrokerRepository extends JpaRepository<Broker, Long> {
    
    /**
     * Find broker by type
     */
    Optional<Broker> findByBrokerType(BrokerType brokerType);
    
    /**
     * Find all enabled brokers
     */
    @Query("SELECT b FROM Broker b WHERE b.isEnabled = true ORDER BY b.displayName")
    List<Broker> findAllEnabled();
    
    /**
     * Find all available brokers (enabled and not in maintenance)
     */
    @Query("SELECT b FROM Broker b WHERE b.isEnabled = true AND b.isMaintenance = false ORDER BY b.displayName")
    List<Broker> findAllAvailable();
    
    /**
     * Find brokers in maintenance mode
     */
    @Query("SELECT b FROM Broker b WHERE b.isMaintenance = true ORDER BY b.displayName")
    List<Broker> findAllInMaintenance();
    
    /**
     * Check if broker is available
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Broker b " +
           "WHERE b.brokerType = :brokerType AND b.isEnabled = true AND b.isMaintenance = false")
    boolean isBrokerAvailable(@Param("brokerType") BrokerType brokerType);
    
    /**
     * Find brokers supporting real-time data
     */
    @Query("SELECT b FROM Broker b WHERE b.isEnabled = true AND b.isMaintenance = false " +
           "AND b.brokerType IN ('ZERODHA', 'UPSTOX') ORDER BY b.displayName")
    List<Broker> findRealTimeDataBrokers();
    
    /**
     * Find brokers by session validity range
     */
    @Query("SELECT b FROM Broker b WHERE b.isEnabled = true " +
           "AND b.sessionValiditySeconds BETWEEN :minSeconds AND :maxSeconds " +
           "ORDER BY b.sessionValiditySeconds DESC")
    List<Broker> findBySessionValidityRange(@Param("minSeconds") Long minSeconds, 
                                           @Param("maxSeconds") Long maxSeconds);
    
    /**
     * Update maintenance mode for broker
     */
    @Query("UPDATE Broker b SET b.isMaintenance = :maintenance, b.maintenanceMessage = :message " +
           "WHERE b.brokerType = :brokerType")
    int updateMaintenanceMode(@Param("brokerType") BrokerType brokerType, 
                             @Param("maintenance") boolean maintenance, 
                             @Param("message") String message);
    
    /**
     * Update rate limits for broker
     */
    @Query("UPDATE Broker b SET b.rateLimitPerSecond = :perSecond, " +
           "b.rateLimitPerMinute = :perMinute, b.rateLimitPerDay = :perDay " +
           "WHERE b.brokerType = :brokerType")
    int updateRateLimits(@Param("brokerType") BrokerType brokerType, 
                        @Param("perSecond") Integer perSecond,
                        @Param("perMinute") Integer perMinute,
                        @Param("perDay") Integer perDay);
}