package com.trademaster.auth.repository;

import com.trademaster.auth.entity.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AuthAuditLog entity operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    /**
     * Find audit logs by user ID
     */
    Page<AuthAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by event type
     */
    List<AuthAuditLog> findByEventTypeOrderByCreatedAtDesc(AuthAuditLog.EventType eventType);

    /**
     * Find audit logs by event status
     */
    List<AuthAuditLog> findByEventStatusOrderByCreatedAtDesc(AuthAuditLog.EventStatus eventStatus);

    /**
     * Find audit logs by correlation ID
     */
    List<AuthAuditLog> findByCorrelationIdOrderByCreatedAtDesc(UUID correlationId);

    /**
     * Find audit logs by session ID
     */
    List<AuthAuditLog> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find audit logs by IP address
     */
    List<AuthAuditLog> findByIpAddressOrderByCreatedAtDesc(java.net.InetAddress ipAddress);

    /**
     * Find high-risk audit logs
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE a.riskScore >= :riskThreshold ORDER BY a.createdAt DESC")
    List<AuthAuditLog> findHighRiskEvents(@Param("riskThreshold") int riskThreshold);

    /**
     * Find critical events
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE a.eventType IN ('SECURITY_VIOLATION', 'SUSPICIOUS_ACTIVITY', 'ACCOUNT_LOCKED') ORDER BY a.createdAt DESC")
    List<AuthAuditLog> findCriticalEvents();

    /**
     * Find failed login attempts for user
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE a.userId = :userId AND a.eventType = 'LOGIN_FAILED' AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuthAuditLog> findFailedLoginAttempts(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find audit logs by date range
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuthAuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      Pageable pageable);

    /**
     * Find suspicious activities by user
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE a.userId = :userId AND (a.eventType = 'SUSPICIOUS_ACTIVITY' OR a.riskScore >= 75) ORDER BY a.createdAt DESC")
    List<AuthAuditLog> findSuspiciousActivitiesByUser(@Param("userId") Long userId);

    /**
     * Count events by type for period
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuthAuditLog a WHERE a.createdAt >= :since GROUP BY a.eventType")
    List<Object[]> countEventsByType(@Param("since") LocalDateTime since);

    /**
     * Count events by status for period
     */
    @Query("SELECT a.eventStatus, COUNT(a) FROM AuthAuditLog a WHERE a.createdAt >= :since GROUP BY a.eventStatus")
    List<Object[]> countEventsByStatus(@Param("since") LocalDateTime since);

    /**
     * Find unprocessed audit logs
     */
    List<AuthAuditLog> findByProcessedAtIsNullOrderByCreatedAtAsc();

    /**
     * Find recent activities for compliance reporting
     */
    @Query("SELECT a FROM AuthAuditLog a WHERE a.createdAt >= :since AND a.eventType IN :eventTypes ORDER BY a.createdAt DESC")
    List<AuthAuditLog> findRecentActivitiesForCompliance(@Param("since") LocalDateTime since, 
                                                         @Param("eventTypes") List<AuthAuditLog.EventType> eventTypes);

    /**
     * Get the latest blockchain hash for integrity chain
     */
    @Query("SELECT a.blockchainHash FROM AuthAuditLog a WHERE a.blockchainHash IS NOT NULL ORDER BY a.id DESC LIMIT 1")
    String getLatestBlockchainHash();

    /**
     * Find audit logs for SEBI compliance export
     */
    @Query(value = """
        SELECT a.* FROM auth_audit_log a 
        WHERE a.created_at BETWEEN :startDate AND :endDate 
        AND (a.event_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILED', 'REGISTRATION', 'ACCOUNT_LOCKED', 'SUSPICIOUS_ACTIVITY')
        OR a.risk_score >= 50)
        ORDER BY a.created_at DESC
        """, nativeQuery = true)
    List<AuthAuditLog> findComplianceExportData(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get authentication statistics for dashboard
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_events,
            COUNT(CASE WHEN event_status = 'SUCCESS' THEN 1 END) as successful_events,
            COUNT(CASE WHEN event_status = 'FAILED' THEN 1 END) as failed_events,
            COUNT(CASE WHEN event_type = 'LOGIN_SUCCESS' THEN 1 END) as successful_logins,
            COUNT(CASE WHEN event_type = 'LOGIN_FAILED' THEN 1 END) as failed_logins,
            COUNT(CASE WHEN risk_score >= 75 THEN 1 END) as high_risk_events,
            COUNT(DISTINCT user_id) as unique_users,
            COUNT(DISTINCT ip_address) as unique_ips
        FROM auth_audit_log 
        WHERE created_at >= :since
        """, nativeQuery = true)
    Object[] getAuthenticationStatistics(@Param("since") LocalDateTime since);

    /**
     * Delete old audit logs (for cleanup - use carefully!)
     */
    @Query("DELETE FROM AuthAuditLog a WHERE a.createdAt < :cutoffDate AND a.eventType NOT IN ('SECURITY_VIOLATION', 'SUSPICIOUS_ACTIVITY')")
    void deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Archive old audit logs to compliance table
     */
    @Query(value = """
        INSERT INTO compliance_reports (report_type, report_period_start, report_period_end, total_records, report_data, report_hash, created_at)
        SELECT 
            'ARCHIVED_AUDIT_LOGS',
            :startDate,
            :endDate,
            COUNT(*),
            JSON_AGG(
                JSON_BUILD_OBJECT(
                    'id', id,
                    'user_id', user_id,
                    'event_type', event_type,
                    'event_status', event_status,
                    'created_at', created_at,
                    'ip_address', host(ip_address),
                    'risk_score', risk_score
                )
            ),
            encode(sha256(JSON_AGG(row_to_json(auth_audit_log))::text::bytea), 'hex'),
            NOW()
        FROM auth_audit_log
        WHERE created_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    void archiveAuditLogsForCompliance(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
}