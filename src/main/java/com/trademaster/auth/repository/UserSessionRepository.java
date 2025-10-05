package com.trademaster.auth.repository;

import com.trademaster.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId")
    List<UserSession> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.active = true")
    List<UserSession> findByUserIdAndActiveTrue(@Param("userId") Long userId);

    @Query("SELECT s FROM UserSession s WHERE s.sessionId = :sessionId AND s.active = true")
    Optional<UserSession> findBySessionIdAndActiveTrue(@Param("sessionId") String sessionId);

    @Query("SELECT s FROM UserSession s WHERE s.deviceFingerprint = :deviceFingerprint")
    List<UserSession> findByDeviceFingerprint(@Param("deviceFingerprint") String deviceFingerprint);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.active = true AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.active = true")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.lastActivity < :cutoffDate AND s.active = true")
    List<UserSession> findStaleSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivity = :timestamp WHERE s.sessionId = :sessionId")
    void updateLastActivity(@Param("sessionId") String sessionId, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE UserSession s SET s.expiresAt = :expiresAt WHERE s.sessionId = :sessionId")
    void extendSession(@Param("sessionId") String sessionId, @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.sessionId = :sessionId")
    void terminateSession(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.userId = :userId")
    void terminateAllSessionsForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.active = true AND s.expiresAt > :now")
    long countActiveSessionsForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId ORDER BY s.lastActivity DESC")
    List<UserSession> findSessionsByUserIdOrderByLastActivity(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :cutoffDate")
    void deleteByExpiresAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}