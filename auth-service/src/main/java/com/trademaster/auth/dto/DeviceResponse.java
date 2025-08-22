package com.trademaster.auth.dto;

import com.trademaster.auth.entity.UserDevice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceResponse {
    private String id;
    private String deviceName;
    private String deviceFingerprint;
    private String userAgent;
    private String location;
    private boolean trusted;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private LocalDateTime trustExpiry;
    private boolean isNewDevice;
    private boolean isExpired;
    
    public static DeviceResponse fromEntity(UserDevice device) {
        return DeviceResponse.builder()
                .id(device.getId().toString())
                .deviceName(device.getDeviceName())
                .deviceFingerprint(device.getDeviceFingerprint())
                .userAgent(device.getUserAgent())
                .location(device.getLocation())
                .trusted(device.isTrusted())
                .firstSeen(device.getFirstSeen())
                .lastSeen(device.getLastSeen())
                .trustExpiry(device.getTrustExpiry())
                .isNewDevice(device.isNewDevice())
                .isExpired(device.isExpired())
                .build();
    }
}