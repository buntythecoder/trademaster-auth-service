package com.trademaster.behavioralai.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Audit Service
 * 
 * Handles security audit logging for compliance and monitoring.
 */
@Service
@RequiredArgsConstructor

public final class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    /**
     * Log security audit event
     * 
     * @param event Security audit event to log
     */
    public void logSecurityEvent(SecurityAuditEvent event) {
        // Use structured logging for security events
        log.info("SECURITY_AUDIT: correlationId={}, userId={}, endpoint={}, status={}, " +
                "clientInfo={}, duration={}ms, errorCode={}, errorMessage={}", 
                event.correlationId(), 
                event.userId(),
                event.endpoint(),
                event.success() ? "SUCCESS" : "FAILURE",
                formatClientInfo(event.clientInfo()),
                event.getDurationMs(),
                event.errorCode(),
                event.errorMessage());
                
        // In production, also send to security monitoring system
        // securityMonitoring.send(event);
    }

    private String formatClientInfo(SecurityContext.ClientInfo clientInfo) {
        if (clientInfo == null) {
            return "unknown";
        }
        
        return String.format("ip=%s,device=%s,location=%s", 
            maskIpAddress(clientInfo.ipAddress()),
            clientInfo.deviceFingerprint() != null ? clientInfo.deviceFingerprint().substring(0, 8) + "..." : "unknown",
            clientInfo.location() != null ? clientInfo.location() : "unknown");
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "unknown";
        }
        
        // Mask last octet for privacy
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
        }
        
        return "masked";
    }
}

/**
 * Security Audit Event Record
 */
record SecurityAuditEvent(
    String correlationId,
    String userId,
    String endpoint,
    boolean success,
    String errorCode,
    String errorMessage,
    SecurityContext.ClientInfo clientInfo,
    Instant startTime,
    Instant endTime
) {
    public long getDurationMs() {
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
    
    public static SecurityAuditEvent success(String correlationId, String userId, 
                                           String endpoint, SecurityContext.ClientInfo clientInfo,
                                           Instant startTime) {
        return new SecurityAuditEvent(
            correlationId, userId, endpoint, true, null, null, 
            clientInfo, startTime, Instant.now()
        );
    }
    
    public static SecurityAuditEvent failure(String correlationId, String userId, 
                                           String endpoint, String errorCode, String errorMessage,
                                           SecurityContext.ClientInfo clientInfo, Instant startTime) {
        return new SecurityAuditEvent(
            correlationId, userId, endpoint, false, errorCode, errorMessage,
            clientInfo, startTime, Instant.now()
        );
    }
}