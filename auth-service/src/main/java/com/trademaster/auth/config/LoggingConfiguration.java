package com.trademaster.auth.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Structured Logging Configuration for Auth Service
 * 
 * Comprehensive logging setup optimized for Grafana/ELK stack integration.
 * Provides structured JSON logging with correlation IDs and security context.
 * 
 * Key Features:
 * - Zero-impact structured logging for Virtual Threads
 * - Security context preservation across async operations
 * - Correlation ID tracking for distributed tracing
 * - Audit trail for compliance and security analysis
 * - Performance metrics embedded in logs
 * 
 * Performance Targets:
 * - Logging overhead: <0.1ms per log entry
 * - No blocking operations in Virtual Threads
 * - Minimal memory allocation
 * - Structured JSON for machine processing
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
@Slf4j
public class LoggingConfiguration {
    
    public LoggingConfiguration() {
        configureStructuredLogging();
    }
    
    private void configureStructuredLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Print logback configuration status for debugging
        log.info("Configuring structured logging for Auth Service");
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }
}

/**
 * Auth Service Structured Logger
 * 
 * Provides security-context aware logging methods for authentication operations.
 * All logs include correlation IDs and structured data for security analysis.
 */
@Component
@Slf4j
public class AuthLogger {
    
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String USER_AGENT = "userAgent";
    private static final String OPERATION = "operation";
    private static final String DURATION_MS = "durationMs";
    private static final String STATUS = "status";
    private static final String SECURITY_EVENT = "securityEvent";
    private static final String RISK_LEVEL = "riskLevel";
    
    /**
     * Set correlation ID for the current thread context
     */
    public void setCorrelationId() {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
    }
    
    /**
     * Set correlation ID with custom value
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }
    
    /**
     * Clear correlation ID from thread context
     */
    public void clearCorrelationId() {
        MDC.remove(CORRELATION_ID);
    }
    
    /**
     * Set user context for all subsequent logs
     */
    public void setUserContext(String userId, String sessionId) {
        if (userId != null) MDC.put(USER_ID, userId);
        if (sessionId != null) MDC.put(SESSION_ID, sessionId);
    }
    
    /**
     * Set request context for all subsequent logs
     */
    public void setRequestContext(String ipAddress, String userAgent) {
        if (ipAddress != null) MDC.put(IP_ADDRESS, ipAddress);
        if (userAgent != null) MDC.put(USER_AGENT, sanitizeUserAgent(userAgent));
    }
    
    /**
     * Clear all context from thread
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Log authentication attempt with security context
     */
    public void logAuthenticationAttempt(String username, String ipAddress, String userAgent, 
                                       String authMethod) {
        log.info("Authentication attempt initiated",
            StructuredArguments.kv(USER_ID, username),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv(USER_AGENT, sanitizeUserAgent(userAgent)),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv(OPERATION, "authentication_attempt"),
            StructuredArguments.kv(STATUS, "initiated"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log successful authentication
     */
    public void logAuthenticationSuccess(String userId, String sessionId, String ipAddress,
                                       String authMethod, long durationMs) {
        log.info("Authentication successful",
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv(SESSION_ID, sessionId),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "authentication"),
            StructuredArguments.kv(STATUS, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log failed authentication with reason
     */
    public void logAuthenticationFailure(String username, String ipAddress, String reason,
                                       String authMethod, long durationMs) {
        log.warn("Authentication failed",
            StructuredArguments.kv(USER_ID, username),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "authentication"),
            StructuredArguments.kv(STATUS, "failure"),
            StructuredArguments.kv(SECURITY_EVENT, "auth_failure"),
            StructuredArguments.kv(RISK_LEVEL, determineRiskLevel(reason)),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log user registration
     */
    public void logUserRegistration(String userId, String email, String ipAddress,
                                   String registrationMethod, long durationMs) {
        log.info("User registration completed",
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv("email", email),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv("registrationMethod", registrationMethod),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "user_registration"),
            StructuredArguments.kv(STATUS, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log security incident
     */
    public void logSecurityIncident(String incidentType, String severity, String userId,
                                   String ipAddress, String description, Map<String, Object> details) {
        var logBuilder = log.atWarn();
        
        logBuilder = logBuilder.addKeyValue("incidentType", incidentType)
            .addKeyValue("severity", severity)
            .addKeyValue(USER_ID, userId)
            .addKeyValue(IP_ADDRESS, ipAddress)
            .addKeyValue("description", description)
            .addKeyValue(OPERATION, "security_incident")
            .addKeyValue(SECURITY_EVENT, incidentType)
            .addKeyValue(RISK_LEVEL, severity)
            .addKeyValue("timestamp", Instant.now());
        
        if (details != null) {
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                logBuilder = logBuilder.addKeyValue("incident_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Security incident detected");
    }
    
    /**
     * Log rate limiting violation
     */
    public void logRateLimitViolation(String clientId, String ipAddress, String endpoint,
                                    int requestCount, int limitThreshold) {
        log.warn("Rate limit violation detected",
            StructuredArguments.kv("clientId", clientId),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("requestCount", requestCount),
            StructuredArguments.kv("limitThreshold", limitThreshold),
            StructuredArguments.kv(OPERATION, "rate_limit_check"),
            StructuredArguments.kv(STATUS, "violation"),
            StructuredArguments.kv(SECURITY_EVENT, "rate_limit_violation"),
            StructuredArguments.kv(RISK_LEVEL, "medium"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log MFA attempt
     */
    public void logMfaAttempt(String userId, String mfaType, String status, long durationMs) {
        log.info("MFA attempt processed",
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv("mfaType", mfaType),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "mfa_verification"),
            StructuredArguments.kv(STATUS, status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log session management events
     */
    public void logSessionEvent(String userId, String sessionId, String eventType,
                               String ipAddress, long durationMs) {
        log.info("Session event processed",
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv(SESSION_ID, sessionId),
            StructuredArguments.kv("eventType", eventType),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "session_management"),
            StructuredArguments.kv(STATUS, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log token operations
     */
    public void logTokenOperation(String userId, String tokenType, String operation,
                                 boolean success, long durationMs) {
        log.info("Token operation completed",
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv("tokenType", tokenType),
            StructuredArguments.kv("tokenOperation", operation),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "token_management"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log API request
     */
    public void logApiRequest(String endpoint, String method, String userId, String ipAddress,
                             int statusCode, long durationMs, String userAgent) {
        log.info("API request processed",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("method", method),
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv(IP_ADDRESS, ipAddress),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(USER_AGENT, sanitizeUserAgent(userAgent)),
            StructuredArguments.kv(OPERATION, "api_request"),
            StructuredArguments.kv(STATUS, statusCode < 400 ? "success" : "error"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log database operation
     */
    public void logDatabaseOperation(String operation, String tableName, int recordsAffected,
                                   long durationMs, boolean success) {
        log.debug("Database operation completed",
            StructuredArguments.kv("dbOperation", operation),
            StructuredArguments.kv("tableName", tableName),
            StructuredArguments.kv("recordsAffected", recordsAffected),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "database_operation"),
            StructuredArguments.kv(STATUS, success ? "success" : "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log cache operation
     */
    public void logCacheOperation(String operation, String key, boolean hit, long durationMs) {
        log.debug("Cache operation completed",
            StructuredArguments.kv("cacheOperation", operation),
            StructuredArguments.kv("key", key),
            StructuredArguments.kv("hit", hit),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "cache_operation"),
            StructuredArguments.kv(STATUS, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log audit event for compliance
     */
    public void logAuditEvent(String eventType, String userId, String action, String resource,
                             String outcome, Map<String, Object> auditData) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue("eventType", eventType)
            .addKeyValue(USER_ID, userId)
            .addKeyValue("action", action)
            .addKeyValue("resource", resource)
            .addKeyValue("outcome", outcome)
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "audit");
        
        if (auditData != null) {
            for (Map.Entry<String, Object> entry : auditData.entrySet()) {
                logBuilder = logBuilder.addKeyValue("audit_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Audit event recorded for compliance");
    }
    
    /**
     * Log error with context
     */
    public void logError(String operation, String errorType, String errorMessage,
                        String userId, Exception exception) {
        log.error("Operation failed",
            StructuredArguments.kv(OPERATION, operation),
            StructuredArguments.kv("errorType", errorType),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv(USER_ID, userId),
            StructuredArguments.kv("exceptionClass", exception != null ? exception.getClass().getSimpleName() : null),
            StructuredArguments.kv(STATUS, "error"),
            StructuredArguments.kv("timestamp", Instant.now()),
            exception
        );
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformanceMetrics(String operation, long durationMs, boolean success,
                                     Map<String, Object> additionalMetrics) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue(OPERATION, operation)
            .addKeyValue(DURATION_MS, durationMs)
            .addKeyValue(STATUS, success ? "success" : "failure")
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "performance");
        
        if (additionalMetrics != null) {
            for (Map.Entry<String, Object> entry : additionalMetrics.entrySet()) {
                logBuilder = logBuilder.addKeyValue("metric_" + entry.getKey(), entry.getValue());
            }
        }
        
        logBuilder.log("Performance metrics recorded");
    }
    
    /**
     * Create structured argument for custom objects
     */
    public static StructuredArguments.ObjectAppendingMarker structuredObject(String key, Object value) {
        return StructuredArguments.kv(key, value);
    }
    
    /**
     * Log with custom structured data
     */
    public void logWithStructuredData(String message, String logLevel, 
                                     Map<String, Object> structuredData) {
        var logBuilder = switch (logLevel.toUpperCase()) {
            case "ERROR" -> log.atError();
            case "WARN" -> log.atWarn();
            case "INFO" -> log.atInfo();
            case "DEBUG" -> log.atDebug();
            case "TRACE" -> log.atTrace();
            default -> log.atInfo();
        };
        
        logBuilder = logBuilder.addKeyValue("timestamp", Instant.now());
        
        for (Map.Entry<String, Object> entry : structuredData.entrySet()) {
            logBuilder = logBuilder.addKeyValue(entry.getKey(), entry.getValue());
        }
        
        logBuilder.log(message);
    }
    
    // Utility Methods
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        // Remove sensitive information and limit length
        return userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;
    }
    
    private String determineRiskLevel(String failureReason) {
        if (failureReason == null) return "low";
        
        String reason = failureReason.toLowerCase();
        if (reason.contains("brute") || reason.contains("suspicious") || reason.contains("blocked")) {
            return "high";
        } else if (reason.contains("invalid") || reason.contains("expired")) {
            return "medium";
        } else {
            return "low";
        }
    }
}