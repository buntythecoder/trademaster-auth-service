package com.trademaster.auth.service;

import com.trademaster.auth.entity.DeviceSettings;
import com.trademaster.auth.entity.SecurityAuditLog;
import com.trademaster.auth.entity.UserDevice;
import com.trademaster.auth.pattern.Functions;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import com.trademaster.auth.repository.DeviceSettingsRepository;
import com.trademaster.auth.repository.SecurityAuditLogRepository;
import com.trademaster.auth.repository.UserDeviceRepository;
import com.trademaster.auth.security.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    @Transactional(readOnly = false)
    public UserDevice registerDevice(Long userId, HttpServletRequest request, String sessionId) {
        Result<UserDevice, String> result = SafeOperations.safelyToResult(() -> {
            String deviceFingerprint = deviceFingerprintService.generateFingerprint(request);
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(request);
            
            log.info("Registering device for user: {} with fingerprint: {}", userId, deviceFingerprint);
            
            return userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .map(existingDevice -> updateExistingDevice(existingDevice, userAgent, ipAddress, request, userId))
                .orElseGet(() -> createNewDevice(userId, deviceFingerprint, userAgent, ipAddress, request, sessionId));
        });
        
        return result.fold(
            error -> {
                throw new RuntimeException("Failed to register device: " + error);
            },
            success -> success
        );
    }

    /**
     * Check if device is trusted
     */
    public boolean isDeviceTrusted(Long userId, String deviceFingerprint) {
        return userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
            .map(userDevice -> evaluateDeviceTrust(userId, deviceFingerprint, userDevice))
            .orElseGet(() -> {
                log.debug("Unknown device for user: {} - {}", userId, deviceFingerprint);
                return false;
            });
    }

    /**
     * Trust a device
     */
    @Transactional(readOnly = false)
    public void trustDevice(Long userId, String deviceFingerprint, String sessionId) {
        log.info("Trusting device for user: {} - {}", userId, deviceFingerprint);

        userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
            .map(device -> processDeviceTrusting(device, userId, deviceFingerprint, sessionId))
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));
    }

    /**
     * Revoke trust for a device
     */
    @Transactional(readOnly = false)
    public void revokeTrust(Long userId, String deviceFingerprint, String sessionId) {
        log.info("Revoking trust for device - user: {} device: {}", userId, deviceFingerprint);

        userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
            .map(device -> processDeviceTrustRevocation(device, userId, deviceFingerprint, sessionId))
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));
    }

    /**
     * Block a device
     */
    @Transactional(readOnly = false)
    public void blockDevice(Long userId, String deviceFingerprint, String sessionId) {
        log.info("Blocking device for user: {} - {}", userId, deviceFingerprint);
        
        Functions.compose(
            settings -> blockDeviceInSettings((DeviceSettings) settings, deviceFingerprint),
            settings -> revokeDeviceTrustIfPresent(userId, deviceFingerprint),
            settings -> logDeviceBlocking(userId, deviceFingerprint, sessionId)
        ).apply(getOrCreateDeviceSettings(userId));
        
        log.info("Device blocked for user: {} - {}", userId, deviceFingerprint);
    }

    /**
     * Unblock a device
     */
    @Transactional(readOnly = false)
    public void unblockDevice(Long userId, String deviceFingerprint, String sessionId) {
        log.info("Unblocking device for user: {} - {}", userId, deviceFingerprint);
        
        Functions.compose(
            settings -> unblockDeviceInSettings((DeviceSettings) settings, deviceFingerprint),
            settings -> logDeviceUnblocking(userId, deviceFingerprint, sessionId)
        ).apply(getOrCreateDeviceSettings(userId));
        
        log.info("Device unblocked for user: {} - {}", userId, deviceFingerprint);
    }

    /**
     * Get all devices for user
     */
    public List<UserDevice> getUserDevices(Long userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    /**
     * Get trusted devices for user
     */
    public List<UserDevice> getTrustedDevices(Long userId) {
        return userDeviceRepository.findActiveTrustedDevicesForUser(userId, LocalDateTime.now());
    }

    /**
     * Get device settings for user
     */
    public DeviceSettings getDeviceSettings(Long userId) {
        return getOrCreateDeviceSettings(userId);
    }

    /**
     * Update device settings
     */
    @Transactional(readOnly = false)
    public DeviceSettings updateDeviceSettings(Long userId, DeviceSettings settings, String sessionId) {
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
     * Validate device trust and return comprehensive trust information
     */
    public DeviceTrustResult validateDeviceTrust(String deviceId, Long userId) {
        Result<DeviceTrustResult, String> result = SafeOperations.safelyToResult(() -> {
            log.info("Validating device trust for device: {} user: {}", deviceId, userId);

            return userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceId)
                .map(device -> evaluateDeviceTrustLevel(deviceId, userId, device))
                .orElseGet(() -> createUnknownDeviceResult(deviceId, userId));
        });

        return result.fold(
            error -> {
                log.error("Error validating device trust for device: {} user: {}, error: {}", deviceId, userId, error);
                return createErrorDeviceResult(deviceId, userId, error);
            },
            success -> success
        );
    }

    /**
     * Check if MFA is required for device
     */
    public boolean requiresMfaForDevice(Long userId, String deviceFingerprint) {
        return Optional.of(getOrCreateDeviceSettings(userId))
            .filter(DeviceSettings::shouldRequireMfaForUntrusted)
            .map(settings -> !isDeviceTrusted(userId, deviceFingerprint))
            .orElse(false);
    }

    /**
     * Remove old devices (cleanup task)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional(readOnly = false)
    public void cleanupOldDevices() {
        VirtualThreadFactory.INSTANCE.runAsync(() -> {
            SafeOperations.safelyToResult(() -> {
                log.info("Starting cleanup of old devices");
                
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(180);
                List<UserDevice> staleDevices = userDeviceRepository.findStaleDevices(cutoffDate);
                
                staleDevices
                    .forEach(device -> log.debug("Removing stale device: {} for user: {}", 
                        device.getDeviceFingerprint(), device.getUserId()));
                
                userDeviceRepository.deleteAll(staleDevices);
                log.info("Cleaned up {} stale devices", staleDevices.size());
                
                return staleDevices.size();
            })
            .mapError(error -> {
                log.error("Failed to cleanup old devices: {}", error);
                return error;
            });
        });
    }

    /**
     * Revoke expired trusted devices (cleanup task)
     */
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    @Transactional(readOnly = false)
    public void revokeExpiredTrustedDevices() {
        log.debug("Checking for expired trusted devices");

        userDeviceRepository.revokeExpiredTrustedDevices(LocalDateTime.now());
    }

    // Private helper methods
    
    private UserDevice updateExistingDevice(UserDevice device, String userAgent, String ipAddress,
                                          HttpServletRequest request, Long userId) {
        device.updateLastSeen();
        device.setUserAgent(userAgent);
        device.setIpAddress(ipAddress);
        device.setLocation(extractLocationFromRequest(request));

        log.debug("Updated existing device for user: {}", userId);
        return userDeviceRepository.save(device);
    }
    
    private UserDevice createNewDevice(Long userId, String deviceFingerprint, String userAgent,
                                     String ipAddress, HttpServletRequest request, String sessionId) {
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
    
    private boolean evaluateDeviceTrust(Long userId, String deviceFingerprint, UserDevice userDevice) {
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        
        return Optional.of(settings)
            .filter(s -> !s.isDeviceBlocked(deviceFingerprint))
            .map(s -> {
                boolean trusted = userDevice.isTrusted();
                log.debug("Device trust check for user: {} device: {} - trusted: {}", 
                    userId, deviceFingerprint, trusted);
                return trusted;
            })
            .orElseGet(() -> {
                log.warn("Blocked device attempted access for user: {} - {}", userId, deviceFingerprint);
                return false;
            });
    }
    
    private UserDevice processDeviceTrusting(UserDevice device, Long userId, String deviceFingerprint, String sessionId) {
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        
        device.trust(settings.getTrustDurationDays());
        userDeviceRepository.save(device);
        
        // Log device trust event
        SecurityAuditLog auditLog = SecurityAuditLog.deviceTrusted(String.valueOf(userId), sessionId, deviceFingerprint);
        securityAuditLogRepository.save(auditLog);
        
        log.info("Device trusted for user: {} - {} for {} days", userId, deviceFingerprint, settings.getTrustDurationDays());
        return device;
    }
    
    private UserDevice processDeviceTrustRevocation(UserDevice device, Long userId, String deviceFingerprint, String sessionId) {
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
        return device;
    }
    
    private DeviceSettings blockDeviceInSettings(DeviceSettings settings, String deviceFingerprint) {
        settings.blockDevice(deviceFingerprint);
        return deviceSettingsRepository.save(settings);
    }
    
    private DeviceSettings revokeDeviceTrustIfPresent(Long userId, String deviceFingerprint) {
        userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
            .ifPresent(device -> {
                device.revokeTrust();
                userDeviceRepository.save(device);
            });
        return getOrCreateDeviceSettings(userId);
    }
    
    private DeviceSettings logDeviceBlocking(Long userId, String deviceFingerprint, String sessionId) {
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_BLOCKED")
                .description("Device blocked: " + deviceFingerprint)
                .riskLevel(SecurityAuditLog.RiskLevel.HIGH)
                .build();
        securityAuditLogRepository.save(auditLog);
        return getOrCreateDeviceSettings(userId);
    }
    
    private DeviceSettings unblockDeviceInSettings(DeviceSettings settings, String deviceFingerprint) {
        settings.unblockDevice(deviceFingerprint);
        return deviceSettingsRepository.save(settings);
    }
    
    private DeviceSettings logDeviceUnblocking(Long userId, String deviceFingerprint, String sessionId) {
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_UNBLOCKED")
                .description("Device unblocked: " + deviceFingerprint)
                .riskLevel(SecurityAuditLog.RiskLevel.LOW)
                .build();
        securityAuditLogRepository.save(auditLog);
        return getOrCreateDeviceSettings(userId);
    }
    
    private DeviceTrustResult createUnknownDeviceResult(String deviceId, Long userId) {
        return DeviceTrustResult.builder()
            .deviceId(deviceId)
            .userId(String.valueOf(userId))
            .trustLevel(TrustLevel.UNKNOWN)
            .trusted(false)
            .reason("Device not found")
            .build();
    }
    
    private DeviceTrustResult createErrorDeviceResult(String deviceId, Long userId, String errorMessage) {
        return DeviceTrustResult.builder()
            .deviceId(deviceId)
            .userId(String.valueOf(userId))
            .trustLevel(TrustLevel.ERROR)
            .trusted(false)
            .reason("Validation failed: " + errorMessage)
            .build();
    }
    
    private DeviceTrustResult evaluateDeviceTrustLevel(String deviceId, Long userId, UserDevice device) {
        DeviceSettings settings = getOrCreateDeviceSettings(userId);
        
        return Optional.of(settings)
            .filter(s -> s.isDeviceBlocked(deviceId))
            .map(s -> createBlockedDeviceResult(deviceId, userId))
            .orElseGet(() -> evaluateDeviceTrustStatus(deviceId, userId, device));
    }
    
    private DeviceTrustResult createBlockedDeviceResult(String deviceId, Long userId) {
        return DeviceTrustResult.builder()
            .deviceId(deviceId)
            .userId(String.valueOf(userId))
            .trustLevel(TrustLevel.BLOCKED)
            .trusted(false)
            .reason("Device is blocked")
            .build();
    }
    
    private final Map<Predicate<UserDevice>, Function<UserDevice, TrustAssessment>> trustAssessmentStrategies = Map.of(
        device -> device.isTrusted() && device.getTrustExpiry() != null && device.getTrustExpiry().isBefore(LocalDateTime.now()),
        device -> new TrustAssessment(TrustLevel.EXPIRED, false, "Trust has expired"),

            UserDevice::isTrusted,
        device -> new TrustAssessment(TrustLevel.TRUSTED, true, "Device is trusted"),
        
        device -> device.getFirstSeen().isAfter(LocalDateTime.now().minusHours(1)),
        device -> new TrustAssessment(TrustLevel.NEW, false, "New device detected")
    );
    
    private DeviceTrustResult evaluateDeviceTrustStatus(String deviceId, Long userId, UserDevice device) {
        TrustAssessment assessment = trustAssessmentStrategies.entrySet().stream()
            .filter(entry -> entry.getKey().test(device))
            .findFirst()
            .map(entry -> entry.getValue().apply(device))
            .orElse(new TrustAssessment(TrustLevel.UNTRUSTED, false, "Device is not trusted"));

        return DeviceTrustResult.builder()
            .deviceId(deviceId)
            .userId(String.valueOf(userId))
            .trustLevel(assessment.trustLevel())
            .trusted(assessment.trusted())
            .reason(assessment.reason())
            .lastSeen(device.getLastSeen())
            .firstSeen(device.getFirstSeen())
            .build();
    }
    
    private record TrustAssessment(TrustLevel trustLevel, boolean trusted, String reason) {}
    
    private DeviceSettings getOrCreateDeviceSettings(Long userId) {
        return deviceSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DeviceSettings settings = DeviceSettings.createDefault(userId);
                    return deviceSettingsRepository.save(settings);
                });
    }

    private String getClientIpAddress(HttpServletRequest request) {
        // ✅ FUNCTIONAL PROGRAMMING: Using Optional chain and Result types
        String ipAddress = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(header -> !header.isEmpty())
            .map(header -> header.split(",")[0].trim())
            .or(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                .filter(header -> !header.isEmpty()))
            .orElse(request.getRemoteAddr());

        return SafeOperations.safelyToResult(() -> {
            if (ipAddress == null || ipAddress.trim().isEmpty()) {
                return "127.0.0.1";
            }
            return ipAddress.trim();
        })
                .fold(
                    error -> {
                        log.warn("Failed to parse IP address {}: {}", ipAddress, error);
                        return "127.0.0.1";
                    },
                    success -> success
                );
    }

    private String extractLocationFromRequest(HttpServletRequest request) {
        String clientIp = getClientIpAddressString(request);
        
        Result<String, String> result = SafeOperations.safelyToResult(() -> Optional.of(clientIp)
            .filter(ip -> !isPrivateIpAddress.test(ip))
            .map(this::detectLocationByIpPattern)
            .orElse(buildLocationString("Private Network", "N/A", "N/A")));
        return result
                .map(location -> {
                    log.debug("Extracted location '{}' for IP: {}", location, clientIp);
                    return location;
                })
                .getOrElse(buildLocationString("Unknown", "N/A", "N/A"));
    }
    
    private String getClientIpAddressString(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(header -> !header.isEmpty())
            .map(header -> header.split(",")[0].trim())
            .or(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                .filter(header -> !header.isEmpty())
                .map(String::trim))
            .orElse(request.getRemoteAddr());
    }
    
    private final Predicate<String> isPrivateIpAddress = ip ->
        Stream.of("192.168.", "10.", "172.", "127.0.0.1", "0:0:0:0:0:0:0:1")
            .anyMatch(pattern -> ip.equals(pattern) || ip.startsWith(pattern));
    
    private final Map<Predicate<String>, Function<String, String>> locationStrategies = Map.of(
        ip -> ip.startsWith("203.") || ip.startsWith("49."), 
        ip -> buildLocationString("India", "IN", "Asia"),
        
        ip -> ip.startsWith("8.8.") || ip.startsWith("74.125."), 
        ip -> buildLocationString("United States", "US", "North America"),
        
        ip -> ip.startsWith("216.") || ip.startsWith("184."), 
        ip -> buildLocationString("United States", "US", "North America")
    );
    
    private String detectLocationByIpPattern(String ip) {
        return locationStrategies.entrySet().stream()
            .filter(entry -> entry.getKey().test(ip))
            .findFirst()
            .map(entry -> entry.getValue().apply(ip))
            .orElse(buildLocationString("Unknown", "XX", "Unknown"));
    }
    
    private String buildLocationString(String country, String countryCode, String continent) {
        return String.format("%s (%s) - %s", country, countryCode, continent);
    }

    private final Map<Predicate<String>, String> deviceTypeStrategies = Map.of(
        ua -> ua.contains("mobile") || ua.contains("iphone"), "Mobile Device",
        ua -> ua.contains("ipad") || ua.contains("tablet"), "Tablet",
        ua -> ua.contains("windows"), "Windows Computer",
        ua -> ua.contains("mac"), "Mac Computer",
        ua -> ua.contains("linux"), "Linux Computer"
    );
    
    private String extractDeviceName(String userAgent) {
        return Optional.ofNullable(userAgent)
            .map(String::toLowerCase)
            .flatMap(ua -> deviceTypeStrategies.entrySet().stream()
                .filter(entry -> entry.getKey().test(ua))
                .findFirst()
                .map(Map.Entry::getValue))
            .orElse("Unknown Device");
    }

    /**
     * Device trust result data class
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DeviceTrustResult {
        private String deviceId;
        private String userId;
        private TrustLevel trustLevel;
        private boolean trusted;
        private String reason;
        private LocalDateTime lastSeen;
        private LocalDateTime firstSeen;
    }

    /**
     * Trust level enumeration
     */
    public enum TrustLevel {
        UNKNOWN, TRUSTED, UNTRUSTED, NEW, EXPIRED, BLOCKED, ERROR
    }
}