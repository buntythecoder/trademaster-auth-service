package com.trademaster.auth.entity;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
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
import java.util.Arrays;
import java.util.List;

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
    @Column(name = "user_id", length = 50)
    @EqualsAndHashCode.Include
    private String userId;

    @Column(name = "trust_duration_days")
    @Builder.Default
    private Integer trustDurationDays = 30;

    @Column(name = "require_mfa_for_untrusted")
    @Builder.Default
    private Boolean requireMfaForUntrusted = true;

    @Column(name = "notify_new_devices")
    @Builder.Default
    private Boolean notifyNewDevices = true;

    @Type(StringArrayType.class)
    @Column(name = "blocked_devices", columnDefinition = "text[]")
    @Builder.Default
    private String[] blockedDevices = new String[0];

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
        return blockedDevices != null && 
               Arrays.asList(blockedDevices).contains(deviceFingerprint);
    }

    public void blockDevice(String deviceFingerprint) {
        if (blockedDevices == null) {
            blockedDevices = new String[]{deviceFingerprint};
        } else {
            List<String> deviceList = new java.util.ArrayList<>(Arrays.asList(blockedDevices));
            if (!deviceList.contains(deviceFingerprint)) {
                deviceList.add(deviceFingerprint);
                blockedDevices = deviceList.toArray(new String[0]);
            }
        }
    }

    public void unblockDevice(String deviceFingerprint) {
        if (blockedDevices != null) {
            List<String> deviceList = new java.util.ArrayList<>(Arrays.asList(blockedDevices));
            deviceList.remove(deviceFingerprint);
            blockedDevices = deviceList.toArray(new String[0]);
        }
    }

    public LocalDateTime calculateTrustExpiry() {
        return LocalDateTime.now().plusDays(this.trustDurationDays);
    }

    public int getBlockedDevicesCount() {
        return blockedDevices != null ? blockedDevices.length : 0;
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("DeviceSettings{userId=%s, trustDays=%d, requireMfa=%s, notifyNew=%s, blockedCount=%d}", 
                           userId, trustDurationDays, requireMfaForUntrusted, notifyNewDevices, getBlockedDevicesCount());
    }

    // Static factory method for default settings
    public static DeviceSettings createDefault(String userId) {
        return DeviceSettings.builder()
                .userId(userId)
                .trustDurationDays(30)
                .requireMfaForUntrusted(true)
                .notifyNewDevices(true)
                .blockedDevices(new String[0])
                .build();
    }
}