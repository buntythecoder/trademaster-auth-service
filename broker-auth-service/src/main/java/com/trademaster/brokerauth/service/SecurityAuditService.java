package com.trademaster.brokerauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.config.CorrelationConfig;
import com.trademaster.brokerauth.enums.BrokerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Security Audit Service
 * 
 * Provides comprehensive security auditing and monitoring.
 * Tracks authentication events, security incidents, and suspicious activities.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {
    
    private final ObjectMapper objectMapper;
    private final StructuredLoggingService loggingService;
    
    /**
     * Log authentication attempt
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logAuthenticationAttempt(Long userId, BrokerType brokerType, 
                                                           String clientIp, String userAgent, 
                                                           boolean success, String failureReason) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("AUTH_ATTEMPT");
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("clientIp", clientIp);
                auditData.put("userAgent", userAgent);
                auditData.put("success", success);
                auditData.put("failureReason", failureReason);
                
                // Determine severity
                String severity = success ? "info" : "warning";
                if (failureReason != null && (failureReason.toLowerCase().contains("blocked") ||
                    failureReason.toLowerCase().contains("suspicious"))) {
                    severity = "high";
                }
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.info("SECURITY_AUDIT: {}", jsonAudit);
                
                // Also log to structured logging service
                if (!success) {
                    loggingService.logSecurityIncident(
                        "auth_failure", 
                        severity, 
                        userId != null ? userId.toString() : null, 
                        clientIp, 
                        userAgent, 
                        auditData
                    );
                }
                
            } catch (Exception e) {
                log.error("Failed to log authentication attempt audit", e);
            }
        });
    }
    
    /**
     * Log session creation
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logSessionCreated(Long userId, BrokerType brokerType, 
                                                    String sessionId, String clientIp, 
                                                    LocalDateTime expiresAt) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("SESSION_CREATED");
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("sessionId", hashSessionId(sessionId)); // Hash for privacy
                auditData.put("clientIp", clientIp);
                auditData.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.info("SECURITY_AUDIT: {}", jsonAudit);
                
            } catch (Exception e) {
                log.error("Failed to log session creation audit", e);
            }
        });
    }
    
    /**
     * Log session validation
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logSessionValidation(Long userId, BrokerType brokerType, 
                                                       String sessionId, String clientIp, 
                                                       boolean valid, String validationMethod) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("SESSION_VALIDATION");
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("sessionId", hashSessionId(sessionId));
                auditData.put("clientIp", clientIp);
                auditData.put("valid", valid);
                auditData.put("validationMethod", validationMethod);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.info("SECURITY_AUDIT: {}", jsonAudit);
                
                // Log security incident if validation failed
                if (!valid) {
                    loggingService.logSecurityIncident(
                        "session_validation_failed", 
                        "medium", 
                        userId != null ? userId.toString() : null, 
                        clientIp, 
                        null, 
                        auditData
                    );
                }
                
            } catch (Exception e) {
                log.error("Failed to log session validation audit", e);
            }
        });
    }
    
    /**
     * Log token refresh
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logTokenRefresh(Long userId, BrokerType brokerType, 
                                                  String sessionId, boolean success, 
                                                  String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("TOKEN_REFRESH");
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("sessionId", hashSessionId(sessionId));
                auditData.put("success", success);
                auditData.put("reason", reason);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.info("SECURITY_AUDIT: {}", jsonAudit);
                
            } catch (Exception e) {
                log.error("Failed to log token refresh audit", e);
            }
        });
    }
    
    /**
     * Log session termination
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logSessionTerminated(Long userId, BrokerType brokerType, 
                                                       String sessionId, String reason, 
                                                       String clientIp) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("SESSION_TERMINATED");
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("sessionId", hashSessionId(sessionId));
                auditData.put("reason", reason);
                auditData.put("clientIp", clientIp);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.info("SECURITY_AUDIT: {}", jsonAudit);
                
            } catch (Exception e) {
                log.error("Failed to log session termination audit", e);
            }
        });
    }
    
    /**
     * Log suspicious activity
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logSuspiciousActivity(String activityType, Long userId, 
                                                        String clientIp, String userAgent, 
                                                        Map<String, Object> details) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("SUSPICIOUS_ACTIVITY");
                auditData.put("activityType", activityType);
                auditData.put("userId", userId);
                auditData.put("clientIp", clientIp);
                auditData.put("userAgent", userAgent);
                
                if (details != null) {
                    auditData.put("details", details);
                }
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.warn("SECURITY_AUDIT: {}", jsonAudit);
                
                // Always log suspicious activities as security incidents
                loggingService.logSecurityIncident(
                    activityType, 
                    "high", 
                    userId != null ? userId.toString() : null, 
                    clientIp, 
                    userAgent, 
                    auditData
                );
                
            } catch (Exception e) {
                log.error("Failed to log suspicious activity audit", e);
            }
        });
    }
    
    /**
     * Log rate limit violation
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logRateLimitViolation(Long userId, BrokerType brokerType, 
                                                        String rateLimitType, String clientIp, 
                                                        int currentRequests, int maxRequests) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("RATE_LIMIT_VIOLATION");
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("rateLimitType", rateLimitType);
                auditData.put("clientIp", clientIp);
                auditData.put("currentRequests", currentRequests);
                auditData.put("maxRequests", maxRequests);
                auditData.put("violationRatio", (double) currentRequests / maxRequests);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.warn("SECURITY_AUDIT: {}", jsonAudit);
                
                // Log as security incident
                String severity = currentRequests > maxRequests * 2 ? "high" : "medium";
                loggingService.logSecurityIncident(
                    "rate_limit_violation", 
                    severity, 
                    userId != null ? userId.toString() : null, 
                    clientIp, 
                    null, 
                    auditData
                );
                
            } catch (Exception e) {
                log.error("Failed to log rate limit violation audit", e);
            }
        });
    }
    
    /**
     * Log configuration changes
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logConfigurationChange(String configType, String configKey, 
                                                         String oldValue, String newValue, 
                                                         String changedBy) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("CONFIG_CHANGE");
                auditData.put("configType", configType);
                auditData.put("configKey", configKey);
                auditData.put("oldValue", sanitizeConfigValue(oldValue));
                auditData.put("newValue", sanitizeConfigValue(newValue));
                auditData.put("changedBy", changedBy);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.warn("SECURITY_AUDIT: {}", jsonAudit);
                
            } catch (Exception e) {
                log.error("Failed to log configuration change audit", e);
            }
        });
    }
    
    /**
     * Log credential management events
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logCredentialEvent(String eventType, Long userId, 
                                                     BrokerType brokerType, String operation, 
                                                     boolean success, String clientIp) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = createBaseAuditData("CREDENTIAL_EVENT");
                auditData.put("eventType", eventType);
                auditData.put("userId", userId);
                auditData.put("brokerType", brokerType != null ? brokerType.toString() : null);
                auditData.put("operation", operation);
                auditData.put("success", success);
                auditData.put("clientIp", clientIp);
                
                String jsonAudit = objectMapper.writeValueAsString(auditData);
                log.info("SECURITY_AUDIT: {}", jsonAudit);
                
                // Log security incident for sensitive operations
                if (!success && ("DECRYPT".equals(operation) || "ENCRYPT".equals(operation))) {
                    loggingService.logSecurityIncident(
                        "credential_operation_failed", 
                        "high", 
                        userId != null ? userId.toString() : null, 
                        clientIp, 
                        null, 
                        auditData
                    );
                }
                
            } catch (Exception e) {
                log.error("Failed to log credential event audit", e);
            }
        });
    }
    
    /**
     * Create base audit data with common fields
     */
    private Map<String, Object> createBaseAuditData(String eventType) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", eventType);
        auditData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        auditData.put("correlationId", CorrelationConfig.CorrelationContext.getCorrelationId());
        auditData.put("requestId", CorrelationConfig.CorrelationContext.getRequestId());
        auditData.put("service", "broker-auth-service");
        auditData.put("version", "1.0.0");
        return auditData;
    }
    
    /**
     * Hash session ID for privacy in logs
     */
    private String hashSessionId(String sessionId) {
        if (sessionId == null || sessionId.length() < 8) {
            return "invalid";
        }
        
        // Return first 4 and last 4 characters with hash in between
        return sessionId.substring(0, 4) + "***" + 
               sessionId.substring(sessionId.length() - 4) + 
               ":" + Integer.toHexString(sessionId.hashCode());
    }
    
    /**
     * Sanitize configuration values to avoid logging sensitive data
     */
    private String sanitizeConfigValue(String value) {
        if (value == null) {
            return null;
        }
        
        String lowerValue = value.toLowerCase();
        
        // If it looks like a password, key, or secret, hide it
        if (lowerValue.contains("password") || lowerValue.contains("secret") || 
            lowerValue.contains("key") || lowerValue.contains("token")) {
            return "***REDACTED***";
        }
        
        // If the value is long and looks like encoded data, truncate it
        if (value.length() > 50 && (value.matches("^[A-Za-z0-9+/]*={0,2}$") || 
                                   value.matches("^[A-Fa-f0-9]+$"))) {
            return value.substring(0, 8) + "...***TRUNCATED***";
        }
        
        return value;
    }
}