package com.trademaster.agentos.security.service;

import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Audit Service - Comprehensive security logging and audit trail.
 * Implements asynchronous audit logging with correlation tracking.
 */
@Slf4j
@Service
public class AuditService {
    
    private final ConcurrentLinkedQueue<AuditEntry> auditQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public AuditService() {
        // Schedule periodic flush of audit logs
        scheduler.scheduleAtFixedRate(this::flushAuditLogs, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Log security event with context.
     */
    public void logSecurityEvent(SecurityContext context, String event, Object details) {
        AuditEntry entry = new AuditEntry(
            context.correlationId(),
            context.userId(),
            context.sessionId(),
            event,
            "SUCCESS",
            details != null ? details.toString() : null,
            context.ipAddress(),
            context.userAgent(),
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        // Log immediately for critical events
        if (isCriticalEvent(event)) {
            log.warn("SECURITY_AUDIT [CRITICAL]: correlationId={}, userId={}, event={}, details={}",
                context.correlationId(), context.userId(), event, details);
        }
    }
    
    /**
     * Log security failure.
     */
    public void logSecurityFailure(SecurityContext context, String event, SecurityError error) {
        AuditEntry entry = new AuditEntry(
            context.correlationId(),
            context.userId(),
            context.sessionId(),
            event,
            "FAILURE",
            error.toString(),
            context.ipAddress(),
            context.userAgent(),
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        log.warn("SECURITY_AUDIT [FAILURE]: correlationId={}, userId={}, event={}, error={}",
            context.correlationId(), context.userId(), event, error.type());
    }
    
    /**
     * Log authentication attempt.
     */
    public void logAuthentication(String userId, String ipAddress, boolean success, String details) {
        String event = success ? "AUTHENTICATION_SUCCESS" : "AUTHENTICATION_FAILURE";
        String status = success ? "SUCCESS" : "FAILURE";
        
        AuditEntry entry = new AuditEntry(
            java.util.UUID.randomUUID().toString(),
            userId,
            null,
            event,
            status,
            details,
            ipAddress,
            null,
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        if (!success) {
            log.warn("SECURITY_AUDIT [AUTH_FAILURE]: userId={}, ip={}, details={}",
                userId, ipAddress, details);
        }
    }
    
    /**
     * Log authorization decision.
     */
    public void logAuthorization(SecurityContext context, String resource, boolean granted) {
        String event = granted ? "AUTHORIZATION_GRANTED" : "AUTHORIZATION_DENIED";
        String status = granted ? "SUCCESS" : "FAILURE";
        
        AuditEntry entry = new AuditEntry(
            context.correlationId(),
            context.userId(),
            context.sessionId(),
            event,
            status,
            "Resource: " + resource,
            context.ipAddress(),
            context.userAgent(),
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        if (!granted) {
            log.info("SECURITY_AUDIT [AUTH_DENIED]: userId={}, resource={}, roles={}",
                context.userId(), resource, context.roles());
        }
    }
    
    /**
     * Log data access.
     */
    public void logDataAccess(SecurityContext context, String dataType, String operation, String details) {
        AuditEntry entry = new AuditEntry(
            context.correlationId(),
            context.userId(),
            context.sessionId(),
            "DATA_ACCESS",
            "SUCCESS",
            String.format("Type=%s, Operation=%s, Details=%s", dataType, operation, details),
            context.ipAddress(),
            context.userAgent(),
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        // Log sensitive data access immediately
        if (isSensitiveData(dataType)) {
            log.info("SECURITY_AUDIT [SENSITIVE_DATA]: userId={}, dataType={}, operation={}",
                context.userId(), dataType, operation);
        }
    }
    
    /**
     * Log configuration change.
     */
    public void logConfigurationChange(SecurityContext context, String setting, String oldValue, String newValue) {
        AuditEntry entry = new AuditEntry(
            context.correlationId(),
            context.userId(),
            context.sessionId(),
            "CONFIGURATION_CHANGE",
            "SUCCESS",
            String.format("Setting=%s, Old=%s, New=%s", setting, oldValue, newValue),
            context.ipAddress(),
            context.userAgent(),
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        log.info("SECURITY_AUDIT [CONFIG_CHANGE]: userId={}, setting={}, old={}, new={}",
            context.userId(), setting, oldValue, newValue);
    }
    
    /**
     * Log security violation.
     */
    public void logSecurityViolation(SecurityContext context, String violationType, String details) {
        AuditEntry entry = new AuditEntry(
            context.correlationId(),
            context.userId(),
            context.sessionId(),
            "SECURITY_VIOLATION",
            "FAILURE",
            String.format("Type=%s, Details=%s", violationType, details),
            context.ipAddress(),
            context.userAgent(),
            Instant.now()
        );
        
        auditQueue.offer(entry);
        
        log.error("SECURITY_AUDIT [VIOLATION]: correlationId={}, userId={}, type={}, details={}",
            context.correlationId(), context.userId(), violationType, details);
    }
    
    /**
     * Async audit log with callback.
     */
    public CompletableFuture<Void> auditAsync(SecurityContext context, String event, Map<String, Object> details) {
        return CompletableFuture.runAsync(() -> {
            logSecurityEvent(context, event, details);
        });
    }
    
    /**
     * Get audit statistics.
     */
    public AuditStatistics getStatistics() {
        int queueSize = auditQueue.size();
        return new AuditStatistics(queueSize, 0, 0, 0);
    }
    
    // Private helper methods
    
    private void flushAuditLogs() {
        int count = 0;
        AuditEntry entry;
        
        while ((entry = auditQueue.poll()) != null && count < 100) {
            // In production, this would write to a persistent audit log
            log.info("AUDIT_LOG: {}", formatAuditEntry(entry));
            count++;
        }
        
        if (count > 0) {
            log.debug("Flushed {} audit entries", count);
        }
    }
    
    private String formatAuditEntry(AuditEntry entry) {
        return String.format(
            "[%s] correlationId=%s, userId=%s, sessionId=%s, event=%s, status=%s, ip=%s, details=%s",
            entry.timestamp,
            entry.correlationId,
            entry.userId,
            entry.sessionId,
            entry.event,
            entry.status,
            entry.ipAddress,
            entry.details
        );
    }
    
    private boolean isCriticalEvent(String event) {
        return event.contains("VIOLATION") || 
               event.contains("BREACH") || 
               event.contains("CRITICAL") ||
               event.contains("ADMIN");
    }
    
    private boolean isSensitiveData(String dataType) {
        return dataType.contains("PERSONAL") || 
               dataType.contains("FINANCIAL") || 
               dataType.contains("CREDENTIAL") ||
               dataType.contains("SECRET");
    }
    
    // Inner classes
    
    private record AuditEntry(
        String correlationId,
        String userId,
        String sessionId,
        String event,
        String status,
        String details,
        String ipAddress,
        String userAgent,
        Instant timestamp
    ) {}
    
    public record AuditStatistics(
        int queueSize,
        long totalEvents,
        long successEvents,
        long failureEvents
    ) {}
}