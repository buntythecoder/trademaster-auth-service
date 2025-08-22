package com.trademaster.auth.repository;

import com.trademaster.auth.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    List<UserDevice> findByUserId(String userId);

    Optional<UserDevice> findByUserIdAndDeviceFingerprint(String userId, String deviceFingerprint);

    List<UserDevice> findByUserIdAndTrusted(String userId, boolean trusted);

    List<UserDevice> findByTrustedTrue();

    @Query("SELECT d FROM UserDevice d WHERE d.userId = :userId AND d.trusted = true AND (d.trustExpiry IS NULL OR d.trustExpiry > :now)")
    List<UserDevice> findActiveTrustedDevicesForUser(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT d FROM UserDevice d WHERE d.trustExpiry IS NOT NULL AND d.trustExpiry < :now AND d.trusted = true")
    List<UserDevice> findExpiredTrustedDevices(@Param("now") LocalDateTime now);

    @Query("SELECT d FROM UserDevice d WHERE d.lastSeen < :cutoffDate")
    List<UserDevice> findStaleDevices(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE UserDevice d SET d.lastSeen = :timestamp WHERE d.id = :id")
    void updateLastSeen(@Param("id") UUID id, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE UserDevice d SET d.trusted = false WHERE d.trustExpiry < :now")
    void revokeExpiredTrustedDevices(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.userId = :userId AND d.trusted = true")
    long countTrustedDevicesForUser(@Param("userId") String userId);

    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.userId = :userId")
    long countDevicesForUser(@Param("userId") String userId);

    @Query("SELECT d FROM UserDevice d WHERE d.firstSeen > :since")
    List<UserDevice> findNewDevicesSince(@Param("since") LocalDateTime since);

    void deleteByUserId(String userId);

    void deleteByUserIdAndDeviceFingerprint(String userId, String deviceFingerprint);
}