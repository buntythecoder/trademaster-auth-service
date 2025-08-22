package com.trademaster.auth.service;

import com.trademaster.auth.entity.DeviceSettings;
import com.trademaster.auth.entity.SecurityAuditLog;
import com.trademaster.auth.entity.UserDevice;
import com.trademaster.auth.repository.DeviceSettingsRepository;
import com.trademaster.auth.repository.SecurityAuditLogRepository;
import com.trademaster.auth.repository.UserDeviceRepository;
import com.trademaster.auth.security.DeviceFingerprintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTrustService {

    private final UserDeviceRepository userDeviceRepository;
    private final DeviceSettingsRepository deviceSettingsRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;
    private final DeviceFingerprintService deviceFingerprintService;

    /**
     * Register or update device information
     */
    @Transactional
    public UserDevice registerDevice(String userId, HttpServletRequest request, String sessionId) {
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(request);
        String userAgent = request.getHeader("User-Agent");
        InetAddress ipAddress = getClientIpAddress(request);
        
        log.info("Registering device for user: {} with fingerprint: {}", userId, deviceFingerprint);
        
        Optional<UserDevice> existingDevice = userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
        
        if (existingDevice.isPresent()) {
            // Update existing device
            UserDevice device = existingDevice.get();
            device.updateLastSeen();
            device.setUserAgent(userAgent);
            device.setIpAddress(ipAddress);
            device.setLocation(extractLocationFromRequest(request));
            
            log.debug("Updated existing device for user: {}", userId);
            return userDeviceRepository.save(device);
        } else {
            // Create new device
            UserDevice newDevice = UserDevice.builder()
                    .userId(userId)
                    .deviceFingerprint(deviceFingerprint)
                    .deviceName(extractDeviceName(userAgent))
                    .userAgent(userAgent)
                    .ipAddress(ipAddress)
                    .location(extractLocationFromRequest(request))
                    .trusted(false)
                    .firstSeen(LocalDateTime.now())
                    .lastSeen(LocalDateTime.now())
                    .build();
            
            UserDevice savedDevice = userDeviceRepository.save(newDevice);
            
            // Log new device detection
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("DEVICE_NEW_DETECTED")
                    .description("New device detected: " + extractDeviceName(userAgent))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .location(extractLocationFromRequest(request))
                    .riskLevel(SecurityAuditLog.RiskLevel.MEDIUM)
                    .build();
            securityAuditLogRepository.save(auditLog);
            
            log.info("New device registered for user: {} - {}", userId, extractDeviceName(userAgent));
            return savedDevice;
        }
    }

    /**
     * Check if device is trusted
     */
    public boolean isDeviceTrusted(String userId, String deviceFingerprint) {
        Optional<UserDevice> device = userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
        
        if (device.isPresent()) {
            UserDevice userDevice = device.get();
            boolean trusted = userDevice.isTrusted();
            
            // Check if device is blocked
            DeviceSettings settings = getOrCreateDeviceSettings(userId);
            if (settings.isDeviceBlocked(deviceFingerprint)) {
                log.warn("Blocked device attempted access for user: {} - {}", userId, deviceFingerprint);
                return false;
            }
            
            log.debug("Device trust check for user: {} device: {} - trusted: {}", userId, deviceFingerprint, trusted);
            return trusted;
        }
        
        log.debug("Unknown device for user: {} - {}", userId, deviceFingerprint);
        return false;
    }

    /**
     * Trust a device
     */
    @Transactional
    public void trustDevice(String userId, String deviceFingerprint, String sessionId) {
        log.info("Trusting device for user: {} - {}", userId, deviceFingerprint);
        
        Optional<UserDevice> deviceOpt = userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found");
        }
        
        UserDevice device = deviceOpt.get();
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        
        device.trust(settings.getTrustDurationDays());
        userDeviceRepository.save(device);
        
        // Log device trust event
        SecurityAuditLog auditLog = SecurityAuditLog.deviceTrusted(userId, sessionId, deviceFingerprint);
        securityAuditLogRepository.save(auditLog);
        
        log.info("Device trusted for user: {} - {} for {} days", userId, deviceFingerprint, settings.getTrustDurationDays());
    }

    /**
     * Revoke trust for a device
     */
    @Transactional
    public void revokeTrust(String userId, String deviceFingerprint, String sessionId) {
        log.info("Revoking trust for device - user: {} device: {}", userId, deviceFingerprint);
        
        Optional<UserDevice> deviceOpt = userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found");
        }
        
        UserDevice device = deviceOpt.get();
        device.revokeTrust();
        userDeviceRepository.save(device);
        
        // Log trust revocation
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_TRUST_REVOKED")
                .description("Device trust revoked: " + deviceFingerprint)
                .riskLevel(SecurityAuditLog.RiskLevel.MEDIUM)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        log.info("Device trust revoked for user: {} - {}", userId, deviceFingerprint);
    }

    /**
     * Block a device
     */
    @Transactional
    public void blockDevice(String userId, String deviceFingerprint, String sessionId) {
        log.info("Blocking device for user: {} - {}", userId, deviceFingerprint);
        
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        settings.blockDevice(deviceFingerprint);
        deviceSettingsRepository.save(settings);
        
        // Also revoke trust if device is trusted
        Optional<UserDevice> deviceOpt = userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
        if (deviceOpt.isPresent()) {
            UserDevice device = deviceOpt.get();
            device.revokeTrust();
            userDeviceRepository.save(device);
        }
        
        // Log device blocking
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_BLOCKED")
                .description("Device blocked: " + deviceFingerprint)
                .riskLevel(SecurityAuditLog.RiskLevel.HIGH)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        log.info("Device blocked for user: {} - {}", userId, deviceFingerprint);
    }

    /**
     * Unblock a device
     */
    @Transactional
    public void unblockDevice(String userId, String deviceFingerprint, String sessionId) {
        log.info("Unblocking device for user: {} - {}", userId, deviceFingerprint);
        
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        settings.unblockDevice(deviceFingerprint);
        deviceSettingsRepository.save(settings);
        
        // Log device unblocking
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_UNBLOCKED")
                .description("Device unblocked: " + deviceFingerprint)
                .riskLevel(SecurityAuditLog.RiskLevel.LOW)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        log.info("Device unblocked for user: {} - {}", userId, deviceFingerprint);
    }

    /**
     * Get all devices for user
     */
    public List<UserDevice> getUserDevices(String userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    /**
     * Get trusted devices for user
     */
    public List<UserDevice> getTrustedDevices(String userId) {
        return userDeviceRepository.findActiveTrustedDevicesForUser(userId, LocalDateTime.now());
    }

    /**
     * Get device settings for user
     */
    public DeviceSettings getDeviceSettings(String userId) {
        return getOrCreateDeviceSettings(userId);
    }

    /**
     * Update device settings
     */
    @Transactional
    public DeviceSettings updateDeviceSettings(String userId, DeviceSettings settings, String sessionId) {
        log.info("Updating device settings for user: {}", userId);
        
        settings.setUserId(userId);
        DeviceSettings savedSettings = deviceSettingsRepository.save(settings);
        
        // Log settings update
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_SETTINGS_UPDATED")
                .description("Device settings updated")
                .riskLevel(SecurityAuditLog.RiskLevel.LOW)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        return savedSettings;
    }

    /**
     * Check if MFA is required for device
     */
    public boolean requiresMfaForDevice(String userId, String deviceFingerprint) {
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        
        if (!settings.shouldRequireMfaForUntrusted()) {
            return false;
        }
        
        return !isDeviceTrusted(userId, deviceFingerprint);
    }

    /**
     * Remove old devices (cleanup task)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldDevices() {
        log.info("Starting cleanup of old devices");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(180); // 6 months old
        List<UserDevice> staleDevices = userDeviceRepository.findStaleDevices(cutoffDate);
        
        for (UserDevice device : staleDevices) {
            log.debug("Removing stale device: {} for user: {}", device.getDeviceFingerprint(), device.getUserId());
        }
        
        userDeviceRepository.deleteAll(staleDevices);
        log.info("Cleaned up {} stale devices", staleDevices.size());
    }

    /**
     * Revoke expired trusted devices (cleanup task)
     */
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    @Transactional
    public void revokeExpiredTrustedDevices() {
        log.debug("Checking for expired trusted devices");
        
        userDeviceRepository.revokeExpiredTrustedDevices(LocalDateTime.now());
    }

    // Private helper methods
    
    private DeviceSettings getOrCreateDeviceSettings(String userId) {
        return deviceSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DeviceSettings settings = DeviceSettings.createDefault(userId);
                    return deviceSettingsRepository.save(settings);
                });
    }

    private InetAddress getClientIpAddress(HttpServletRequest request) {
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return InetAddress.getByName(xForwardedFor.split(",")[0].trim());
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return InetAddress.getByName(xRealIp);
            }
            
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (Exception e) {
            log.warn("Error extracting client IP address", e);
            try {
                return InetAddress.getByName(request.getRemoteAddr());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String extractLocationFromRequest(HttpServletRequest request) {
        // In a real implementation, you would use a GeoIP service
        // For now, return a placeholder
        return "Unknown Location";
    }

    private String extractDeviceName(String userAgent) {
        if (userAgent == null) {
            return "Unknown Device";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("mobile") || ua.contains("iphone")) {
            return "Mobile Device";
        } else if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        } else if (ua.contains("windows")) {
            return "Windows Computer";
        } else if (ua.contains("mac")) {
            return "Mac Computer";
        } else if (ua.contains("linux")) {
            return "Linux Computer";
        } else {
            return "Unknown Device";
        }
    }
}