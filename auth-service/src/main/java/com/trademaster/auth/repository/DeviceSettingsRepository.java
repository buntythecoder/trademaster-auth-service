package com.trademaster.auth.repository;

import com.trademaster.auth.entity.DeviceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSettingsRepository extends JpaRepository<DeviceSettings, String> {

    Optional<DeviceSettings> findByUserId(String userId);

    @Query("SELECT d FROM DeviceSettings d WHERE d.trustDurationDays > :threshold")
    List<DeviceSettings> findByTrustDurationDaysGreaterThan(@Param("threshold") int threshold);

    @Query("SELECT d FROM DeviceSettings d WHERE d.requireMfaForUntrusted = true")
    List<DeviceSettings> findByRequireMfaForUntrustedTrue();

    @Query("SELECT d FROM DeviceSettings d WHERE d.notifyNewDevices = true")
    List<DeviceSettings> findByNotifyNewDevicesTrue();

    @Query("SELECT d FROM DeviceSettings d WHERE array_length(d.blockedDevices, 1) > 0")
    List<DeviceSettings> findUsersWithBlockedDevices();

    @Query("SELECT AVG(d.trustDurationDays) FROM DeviceSettings d")
    Double getAverageTrustDuration();

    @Query("SELECT COUNT(d) FROM DeviceSettings d WHERE d.requireMfaForUntrusted = true")
    long countUsersRequiringMfaForUntrusted();

    @Query("SELECT COUNT(d) FROM DeviceSettings d WHERE d.notifyNewDevices = true")
    long countUsersWithNewDeviceNotifications();

    @Query("SELECT COUNT(d) FROM DeviceSettings d WHERE array_length(d.blockedDevices, 1) > 0")
    long countUsersWithBlockedDevices();

    @Query("SELECT SUM(array_length(d.blockedDevices, 1)) FROM DeviceSettings d")
    Long getTotalBlockedDevicesCount();
}