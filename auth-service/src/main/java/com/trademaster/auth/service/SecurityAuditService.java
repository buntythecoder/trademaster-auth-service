package com.trademaster.auth.service;

import com.trademaster.auth.entity.SecurityAuditLog;
import com.trademaster.auth.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final SecurityAuditLogRepository securityAuditLogRepository;

    /**
     * Log a security event asynchronously
     */
    @Async
    public void logSecurityEvent(String userId, String sessionId, String eventType, 
                                String description, SecurityAuditLog.RiskLevel riskLevel,
                                HttpServletRequest request, Map<String, Object> metadata) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType(eventType)
                    .description(description)
                    .riskLevel(riskLevel)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .location(extractLocationFromRequest(request))
                    .metadata(metadata)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            securityAuditLogRepository.save(auditLog);
            
            // Log high-risk events immediately
            if (riskLevel == SecurityAuditLog.RiskLevel.HIGH || riskLevel == SecurityAuditLog.RiskLevel.CRITICAL) {
                log.warn("HIGH/CRITICAL security event: {} for user: {} - {}", eventType, userId, description);
            } else {
                log.info("Security event: {} for user: {} - {}", eventType, userId, description);
            }
            
        } catch (Exception e) {
            log.error("Failed to log security event", e);
        }
    }

    /**
     * Log authentication event
     */
    @Async
    public void logAuthenticationEvent(String userId, String eventType, boolean success,
                                     HttpServletRequest request, String details) {
        SecurityAuditLog.RiskLevel riskLevel = success ? 
                SecurityAuditLog.RiskLevel.LOW : SecurityAuditLog.RiskLevel.MEDIUM;
        
        String description = success ? 
                "Authentication successful" : "Authentication failed: " + details;
        
        logSecurityEvent(userId, null, eventType, description, riskLevel, request, null);
    }

    /**
     * Log MFA event
     */
    @Async
    public void logMfaEvent(String userId, String sessionId, String eventType, 
                           boolean success, String mfaType, HttpServletRequest request) {
        SecurityAuditLog.RiskLevel riskLevel = success ? 
                SecurityAuditLog.RiskLevel.LOW : SecurityAuditLog.RiskLevel.MEDIUM;
        
        String description = String.format("MFA %s %s for type: %s", 
                eventType, success ? "successful" : "failed", mfaType);
        
        Map<String, Object> metadata = Map.of(
                "mfa_type", mfaType,
                "success", success
        );
        
        logSecurityEvent(userId, sessionId, eventType, description, riskLevel, request, metadata);
    }

    /**
     * Log device event
     */
    @Async
    public void logDeviceEvent(String userId, String sessionId, String eventType,
                              String deviceFingerprint, HttpServletRequest request) {
        SecurityAuditLog.RiskLevel riskLevel = SecurityAuditLog.RiskLevel.LOW;
        
        // Increase risk level for certain events
        if (eventType.contains("BLOCKED") || eventType.contains("SUSPICIOUS")) {
            riskLevel = SecurityAuditLog.RiskLevel.HIGH;
        } else if (eventType.contains("NEW")) {
            riskLevel = SecurityAuditLog.RiskLevel.MEDIUM;
        }
        
        String description = String.format("Device event: %s for device: %s", eventType, deviceFingerprint);
        
        Map<String, Object> metadata = Map.of(
                "device_fingerprint", deviceFingerprint,
                "event_type", eventType
        );
        
        logSecurityEvent(userId, sessionId, eventType, description, riskLevel, request, metadata);
    }

    /**
     * Log session event
     */
    @Async
    public void logSessionEvent(String userId, String sessionId, String eventType,
                               HttpServletRequest request, Map<String, Object> additionalData) {
        SecurityAuditLog.RiskLevel riskLevel = SecurityAuditLog.RiskLevel.LOW;
        
        String description = String.format("Session event: %s", eventType);
        
        Map<String, Object> metadata = additionalData != null ? 
                new java.util.HashMap<>(additionalData) : new java.util.HashMap<>();
        metadata.put("session_id", sessionId);
        
        logSecurityEvent(userId, sessionId, eventType, description, riskLevel, request, metadata);
    }

    /**
     * Detect suspicious activity patterns
     */
    @Async
    @Transactional
    public void analyzeSuspiciousActivity(String userId, String ipAddress) {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            
            // Check for multiple failed login attempts
            long failedLogins = securityAuditLogRepository.countFailedLoginsForUser(userId, oneHourAgo);
            if (failedLogins >= 5) {
                SecurityAuditLog suspiciousEvent = SecurityAuditLog.builder()
                        .userId(userId)
                        .eventType("SECURITY_SUSPICIOUS_MULTIPLE_FAILED_LOGINS")
                        .description(String.format("Multiple failed login attempts detected: %d in last hour", failedLogins))
                        .riskLevel(SecurityAuditLog.RiskLevel.HIGH)
                        .timestamp(LocalDateTime.now())
                        .build();
                
                securityAuditLogRepository.save(suspiciousEvent);
                log.warn("SUSPICIOUS ACTIVITY: Multiple failed logins for user: {} ({})", userId, failedLogins);
            }
            
            // Check for failed logins from same IP
            if (ipAddress != null) {
                try {
                    InetAddress inet = InetAddress.getByName(ipAddress);
                    long ipFailedLogins = securityAuditLogRepository.countFailedLoginsFromIp(inet, oneHourAgo);
                    if (ipFailedLogins >= 10) {
                        SecurityAuditLog suspiciousEvent = SecurityAuditLog.builder()
                                .eventType("SECURITY_SUSPICIOUS_IP_ACTIVITY")
                                .description(String.format("Multiple failed logins from IP: %s (%d attempts)", ipAddress, ipFailedLogins))
                                .ipAddress(inet)
                                .riskLevel(SecurityAuditLog.RiskLevel.CRITICAL)
                                .timestamp(LocalDateTime.now())
                                .build();
                        
                        securityAuditLogRepository.save(suspiciousEvent);
                        log.error("CRITICAL SUSPICIOUS ACTIVITY: Multiple failed logins from IP: {} ({})", ipAddress, ipFailedLogins);
                    }
                } catch (Exception e) {
                    log.warn("Error analyzing IP-based suspicious activity", e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error analyzing suspicious activity for user: {}", userId, e);
        }
    }

    /**
     * Get security audit logs for user
     */
    public Page<SecurityAuditLog> getUserAuditLogs(String userId, Pageable pageable) {
        return securityAuditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get recent high-risk events
     */
    public List<SecurityAuditLog> getRecentHighRiskEvents(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return securityAuditLogRepository.findHighRiskEvents().stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .toList();
    }

    /**
     * Get security metrics summary
     */
    public Map<String, Object> getSecurityMetrics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<Object[]> eventTypeSummary = securityAuditLogRepository.getEventTypeSummary(since);
        List<Object[]> riskLevelSummary = securityAuditLogRepository.getRiskLevelSummary(since);
        
        Map<String, Long> eventTypes = new java.util.HashMap<>();
        for (Object[] row : eventTypeSummary) {
            eventTypes.put((String) row[0], (Long) row[1]);
        }
        
        Map<String, Long> riskLevels = new java.util.HashMap<>();
        for (Object[] row : riskLevelSummary) {
            riskLevels.put(row[0].toString(), (Long) row[1]);
        }
        
        return Map.of(
                "period_days", days,
                "event_types", eventTypes,
                "risk_levels", riskLevels,
                "total_events", eventTypes.values().stream().mapToLong(Long::longValue).sum()
        );
    }

    /**
     * Cleanup old audit logs (scheduled task)
     */
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void cleanupOldAuditLogs() {
        log.info("Starting cleanup of old audit logs");
        
        // Keep audit logs for 1 year
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
        
        long deletedCount = securityAuditLogRepository.findAll().stream()
                .filter(log -> log.getTimestamp().isBefore(cutoffDate))
                .peek(log -> securityAuditLogRepository.delete(log))
                .count();
        
        log.info("Cleaned up {} old audit log entries", deletedCount);
    }

    // Helper methods
    
    private InetAddress extractIpAddress(HttpServletRequest request) {
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
            log.warn("Error extracting IP address", e);
            try {
                return InetAddress.getByName(request.getRemoteAddr());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String extractLocationFromRequest(HttpServletRequest request) {
        // In a real implementation, you would use a GeoIP service
        return "Unknown Location";
    }
}