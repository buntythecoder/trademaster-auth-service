package com.trademaster.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeviceSettings {

    @Id
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private Long userId;

    @Column(name = "trust_duration_days")
    @Builder.Default
    private Integer trustDurationDays = 30;

    @Column(name = "require_mfa_for_untrusted")
    @Builder.Default
    private Boolean requireMfaForUntrusted = true;

    @Column(name = "notify_new_devices")
    @Builder.Default
    private Boolean notifyNewDevices = true;

    @Column(name = "blocked_devices", columnDefinition = "text")
    @Builder.Default
    private String blockedDevices = "";

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic methods
    public boolean shouldRequireMfaForUntrusted() {
        return Boolean.TRUE.equals(this.requireMfaForUntrusted);
    }

    public boolean shouldNotifyOnNewDevices() {
        return Boolean.TRUE.equals(this.notifyNewDevices);
    }

    public boolean isDeviceBlocked(String deviceFingerprint) {
        return blockedDevices != null && blockedDevices.contains(deviceFingerprint);
    }

    public void blockDevice(String deviceFingerprint) {
        blockedDevices = Optional.ofNullable(blockedDevices)
            .orElse("");
        blockedDevices = Optional.of(blockedDevices)
            .filter(devices -> !devices.contains(deviceFingerprint))
            .map(devices -> devices.isEmpty() ? deviceFingerprint : devices + "," + deviceFingerprint)
            .orElse(blockedDevices);
    }

    public void unblockDevice(String deviceFingerprint) {
        blockedDevices = Optional.ofNullable(blockedDevices)
            .filter(devices -> devices.contains(deviceFingerprint))
            .map(devices -> devices.replace(deviceFingerprint + ",", "")
                                   .replace("," + deviceFingerprint, "")
                                   .replace(deviceFingerprint, ""))
            .orElse(blockedDevices);
    }

    public LocalDateTime calculateTrustExpiry() {
        return LocalDateTime.now().plusDays(this.trustDurationDays);
    }

    public int getBlockedDevicesCount() {
        return Optional.ofNullable(blockedDevices)
            .filter(devices -> !devices.isEmpty())
            .map(devices -> devices.split(",").length)
            .orElse(0);
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("DeviceSettings{userId=%s, trustDays=%d, requireMfa=%s, notifyNew=%s, blockedCount=%d}", 
                           userId, trustDurationDays, requireMfaForUntrusted, notifyNewDevices, getBlockedDevicesCount());
    }

    // Static factory method for default settings
    public static DeviceSettings createDefault(Long userId) {
        return DeviceSettings.builder()
                .userId(userId)
                .trustDurationDays(30)
                .requireMfaForUntrusted(true)
                .notifyNewDevices(true)
                .blockedDevices("")
                .build();
    }
}