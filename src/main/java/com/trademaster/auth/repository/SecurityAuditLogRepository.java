package com.trademaster.auth.repository;

import com.trademaster.auth.entity.SecurityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {

    List<SecurityAuditLog> findByUserId(Long userId);

    Page<SecurityAuditLog> findByUserId(Long userId, Pageable pageable);

    List<SecurityAuditLog> findByUserIdAndTimestampBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<SecurityAuditLog> findBySessionId(String sessionId);

    List<SecurityAuditLog> findByEventType(String eventType);

    List<SecurityAuditLog> findByRiskLevel(SecurityAuditLog.RiskLevel riskLevel);

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.riskLevel IN ('HIGH', 'CRITICAL')")
    List<SecurityAuditLog> findHighRiskEvents();

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.timestamp >= :since")
    List<SecurityAuditLog> findRecentEvents(@Param("since") LocalDateTime since);

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.userId = :userId AND s.timestamp >= :since")
    List<SecurityAuditLog> findRecentEventsForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.eventType LIKE 'LOGIN%' AND s.userId = :userId ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findLoginEventsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.eventType LIKE 'MFA_%' AND s.userId = :userId ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findMfaEventsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.eventType LIKE 'DEVICE_%' AND s.userId = :userId ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findDeviceEventsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s.eventType, COUNT(s) FROM SecurityAuditLog s WHERE s.timestamp >= :since GROUP BY s.eventType")
    List<Object[]> getEventTypeSummary(@Param("since") LocalDateTime since);

    @Query("SELECT s.riskLevel, COUNT(s) FROM SecurityAuditLog s WHERE s.timestamp >= :since GROUP BY s.riskLevel")
    List<Object[]> getRiskLevelSummary(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.userId = :userId AND s.eventType = 'LOGIN_FAILED' AND s.timestamp >= :since")
    long countFailedLoginsForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.ipAddress = :ipAddress AND s.eventType = 'LOGIN_FAILED' AND s.timestamp >= :since")
    long countFailedLoginsFromIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    void deleteByTimestampBefore(LocalDateTime cutoffDate);

    List<SecurityAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<SecurityAuditLog> findByEventTypeAndTimestampBetween(String eventType, LocalDateTime start, LocalDateTime end);
    
    long countByUserIdAndEventType(Long userId, String eventType);
    
    Page<SecurityAuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}