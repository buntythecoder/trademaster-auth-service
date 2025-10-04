package com.trademaster.auth.controller;

import com.trademaster.auth.dto.DeviceResponse;
import com.trademaster.auth.entity.DeviceSettings;
import com.trademaster.auth.entity.UserDevice;
import com.trademaster.auth.service.DeviceTrustService;
import com.trademaster.auth.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/devices")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Device Trust Management", description = "Device trust and security management endpoints")
public class DeviceController {

    private final DeviceTrustService deviceTrustService;
    private final SecurityAuditService securityAuditService;

    @GetMapping
    @Operation(summary = "Get User Devices", description = "Get all devices associated with the authenticated user")
    public ResponseEntity<List<DeviceResponse>> getUserDevices(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        List<UserDevice> devices = deviceTrustService.getUserDevices(Long.valueOf(userId));
        
        List<DeviceResponse> deviceResponses = devices.stream()
                .map(DeviceResponse::fromEntity)
                .toList();
        
        return ResponseEntity.ok(deviceResponses);
    }

    @GetMapping("/trusted")
    @Operation(summary = "Get Trusted Devices", description = "Get all trusted devices for the authenticated user")
    public ResponseEntity<List<DeviceResponse>> getTrustedDevices(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        List<UserDevice> trustedDevices = deviceTrustService.getTrustedDevices(Long.valueOf(userId));
        
        List<DeviceResponse> deviceResponses = trustedDevices.stream()
                .map(DeviceResponse::fromEntity)
                .toList();
        
        return ResponseEntity.ok(deviceResponses);
    }

    @PostMapping("/trust")
    @Operation(summary = "Trust Current Device", description = "Mark the current device as trusted")
    public ResponseEntity<Map<String, Object>> trustCurrentDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        // Register current device if not exists, then trust it
        UserDevice device = deviceTrustService.registerDevice(Long.valueOf(userId), request, sessionId);
        deviceTrustService.trustDevice(Long.valueOf(userId), device.getDeviceFingerprint(), sessionId);
        
        securityAuditService.logDeviceEvent(userId, sessionId, "DEVICE_TRUSTED", device.getDeviceFingerprint(), request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Device trusted successfully",
                "deviceId", device.getId(),
                "deviceName", device.getDeviceName()
        ));
    }

    @PostMapping("/{deviceFingerprint}/trust")
    @Operation(summary = "Trust Specific Device", description = "Mark a specific device as trusted")
    public ResponseEntity<Map<String, Object>> trustDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String deviceFingerprint,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        try {
            deviceTrustService.trustDevice(Long.valueOf(userId), deviceFingerprint, sessionId);
            
            securityAuditService.logDeviceEvent(userId, sessionId, "DEVICE_TRUSTED", deviceFingerprint, request);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Device trusted successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Device not found"
            ));
        }
    }

    @DeleteMapping("/{deviceFingerprint}/trust")
    @Operation(summary = "Revoke Device Trust", description = "Revoke trust for a specific device")
    public ResponseEntity<Map<String, Object>> revokeTrust(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String deviceFingerprint,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        try {
            deviceTrustService.revokeTrust(Long.valueOf(userId), deviceFingerprint, sessionId);
            
            securityAuditService.logDeviceEvent(userId, sessionId, "DEVICE_TRUST_REVOKED", deviceFingerprint, request);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Device trust revoked successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Device not found"
            ));
        }
    }

    @PostMapping("/{deviceFingerprint}/block")
    @Operation(summary = "Block Device", description = "Block a specific device from accessing the account")
    public ResponseEntity<Map<String, Object>> blockDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String deviceFingerprint,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        deviceTrustService.blockDevice(Long.valueOf(userId), deviceFingerprint, sessionId);
        
        securityAuditService.logDeviceEvent(userId, sessionId, "DEVICE_BLOCKED", deviceFingerprint, request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Device blocked successfully"
        ));
    }

    @DeleteMapping("/{deviceFingerprint}/block")
    @Operation(summary = "Unblock Device", description = "Unblock a previously blocked device")
    public ResponseEntity<Map<String, Object>> unblockDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String deviceFingerprint,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        deviceTrustService.unblockDevice(Long.valueOf(userId), deviceFingerprint, sessionId);
        
        securityAuditService.logDeviceEvent(userId, sessionId, "DEVICE_UNBLOCKED", deviceFingerprint, request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Device unblocked successfully"
        ));
    }

    @GetMapping("/current")
    @Operation(summary = "Get Current Device Info", description = "Get information about the current device")
    public ResponseEntity<Map<String, Object>> getCurrentDeviceInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        // Register/update current device
        UserDevice device = deviceTrustService.registerDevice(Long.valueOf(userId), request, sessionId);

        boolean trusted = deviceTrustService.isDeviceTrusted(Long.valueOf(userId), device.getDeviceFingerprint());
        boolean requiresMfa = deviceTrustService.requiresMfaForDevice(Long.valueOf(userId), device.getDeviceFingerprint());
        
        return ResponseEntity.ok(Map.of(
                "device", DeviceResponse.fromEntity(device),
                "trusted", trusted,
                "requiresMfa", requiresMfa
        ));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get Device Settings", description = "Get device trust settings for the authenticated user")
    public ResponseEntity<DeviceSettings> getDeviceSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        DeviceSettings settings = deviceTrustService.getDeviceSettings(Long.valueOf(userId));
        
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    @Operation(summary = "Update Device Settings", description = "Update device trust settings")
    public ResponseEntity<DeviceSettings> updateDeviceSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DeviceSettings settings,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        DeviceSettings updatedSettings = deviceTrustService.updateDeviceSettings(Long.valueOf(userId), settings, sessionId);
        
        securityAuditService.logSecurityEvent(Long.parseLong(userId), "DEVICE_SETTINGS_UPDATED", 
                "INFO", request.getRemoteAddr(), request.getHeader("User-Agent"), 
                Map.of("action", "Device trust settings updated"));
        
        return ResponseEntity.ok(updatedSettings);
    }
}