package com.trademaster.auth.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import com.trademaster.auth.constants.AuthConstants;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
class AuthLogger {
    
    // All constants now centralized in AuthConstants class
    
    /**
     * Set correlation ID for the current thread context
     */
    public void setCorrelationId() {
        MDC.put(AuthConstants.CORRELATION_ID, UUID.randomUUID().toString());
    }
    
    /**
     * Set correlation ID with custom value
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(AuthConstants.CORRELATION_ID, correlationId);
    }
    
    /**
     * Clear correlation ID from thread context
     */
    public void clearCorrelationId() {
        MDC.remove(AuthConstants.CORRELATION_ID);
    }
    
    /**
     * Set user context for all subsequent logs
     */
    public void setUserContext(String userId, String sessionId) {
        if (userId != null) MDC.put(AuthConstants.USER_ID_FIELD, userId);
        if (sessionId != null) MDC.put(AuthConstants.SESSION_ID_FIELD, sessionId);
    }
    
    /**
     * Set request context for all subsequent logs
     */
    public void setRequestContext(String ipAddress, String userAgent) {
        if (ipAddress != null) MDC.put(AuthConstants.IP_ADDRESS_FIELD, ipAddress);
        if (userAgent != null) MDC.put(AuthConstants.USER_AGENT_FIELD, sanitizeUserAgent(userAgent));
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
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, username),
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv(AuthConstants.USER_AGENT_FIELD, sanitizeUserAgent(userAgent)),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "authentication_attempt"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "initiated"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log successful authentication
     */
    public void logAuthenticationSuccess(String userId, String sessionId, String ipAddress,
                                       String authMethod, long durationMs) {
        log.info("Authentication successful",
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv(AuthConstants.SESSION_ID_FIELD, sessionId),
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "authentication"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log failed authentication with reason
     */
    public void logAuthenticationFailure(String username, String ipAddress, String reason,
                                       String authMethod, long durationMs) {
        log.warn("Authentication failed",
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, username),
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "authentication"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "failure"),
            StructuredArguments.kv(AuthConstants.SECURITY_EVENT_FIELD, "auth_failure"),
            StructuredArguments.kv(AuthConstants.RISK_LEVEL_FIELD, determineRiskLevel(reason)),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log user registration
     */
    public void logUserRegistration(String userId, String email, String ipAddress,
                                   String registrationMethod, long durationMs) {
        log.info("User registration completed",
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv("email", email),
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv("registrationMethod", registrationMethod),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "user_registration"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "success"),
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
            .addKeyValue(AuthConstants.USER_ID_FIELD, userId)
            .addKeyValue(AuthConstants.IP_ADDRESS_FIELD, ipAddress)
            .addKeyValue("description", description)
            .addKeyValue(AuthConstants.OPERATION_FIELD, "security_incident")
            .addKeyValue(AuthConstants.SECURITY_EVENT_FIELD, incidentType)
            .addKeyValue(AuthConstants.RISK_LEVEL_FIELD, severity)
            .addKeyValue("timestamp", Instant.now());
        
        if (details != null) {
            logBuilder = details.entrySet().stream()
                .reduce(logBuilder, 
                    (builder, entry) -> builder.addKeyValue("incident_" + entry.getKey(), entry.getValue()),
                    (b1, b2) -> b1);
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
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("requestCount", requestCount),
            StructuredArguments.kv("limitThreshold", limitThreshold),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "rate_limit_check"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "violation"),
            StructuredArguments.kv(AuthConstants.SECURITY_EVENT_FIELD, "rate_limit_violation"),
            StructuredArguments.kv(AuthConstants.RISK_LEVEL_FIELD, "medium"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log MFA attempt
     */
    public void logMfaAttempt(String userId, String mfaType, String status, long durationMs) {
        log.info("MFA attempt processed",
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv("mfaType", mfaType),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "mfa_verification"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log session management events
     */
    public void logSessionEvent(String userId, String sessionId, String eventType,
                               String ipAddress, long durationMs) {
        log.info("Session event processed",
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv(AuthConstants.SESSION_ID_FIELD, sessionId),
            StructuredArguments.kv("eventType", eventType),
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "session_management"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log token operations
     */
    public void logTokenOperation(String userId, String tokenType, String operation,
                                 boolean success, long durationMs) {
        log.info("Token operation completed",
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv("tokenType", tokenType),
            StructuredArguments.kv("tokenOperation", operation),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "token_management"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, success ? "success" : "failure"),
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
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv(AuthConstants.IP_ADDRESS_FIELD, ipAddress),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.USER_AGENT_FIELD, sanitizeUserAgent(userAgent)),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "api_request"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, statusCode < AuthConstants.HTTP_CLIENT_ERROR_THRESHOLD ? "success" : "error"),
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
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "database_operation"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, success ? "success" : "failure"),
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
            StructuredArguments.kv(AuthConstants.DURATION_MS_FIELD, durationMs),
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, "cache_operation"),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "success"),
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
            .addKeyValue(AuthConstants.USER_ID_FIELD, userId)
            .addKeyValue("action", action)
            .addKeyValue("resource", resource)
            .addKeyValue("outcome", outcome)
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "audit");
        
        if (auditData != null) {
            logBuilder = auditData.entrySet().stream()
                .reduce(logBuilder,
                    (builder, entry) -> builder.addKeyValue("audit_" + entry.getKey(), entry.getValue()),
                    (b1, b2) -> b1);
        }
        
        logBuilder.log("Audit event recorded for compliance");
    }
    
    /**
     * Log error with context
     */
    public void logError(String operation, String errorType, String errorMessage,
                        String userId, Exception exception) {
        log.error("Operation failed",
            StructuredArguments.kv(AuthConstants.OPERATION_FIELD, operation),
            StructuredArguments.kv("errorType", errorType),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv(AuthConstants.USER_ID_FIELD, userId),
            StructuredArguments.kv("exceptionClass", exception != null ? exception.getClass().getSimpleName() : null),
            StructuredArguments.kv(AuthConstants.STATUS_FIELD, "error"),
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
        
        logBuilder = logBuilder.addKeyValue(AuthConstants.OPERATION_FIELD, operation)
            .addKeyValue(AuthConstants.DURATION_MS_FIELD, durationMs)
            .addKeyValue(AuthConstants.STATUS_FIELD, success ? "success" : "failure")
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "performance");
        
        if (additionalMetrics != null) {
            logBuilder = additionalMetrics.entrySet().stream()
                .reduce(logBuilder,
                    (builder, entry) -> builder.addKeyValue("metric_" + entry.getKey(), entry.getValue()),
                    (b1, b2) -> b1);
        }
        
        logBuilder.log("Performance metrics recorded");
    }
    
    /**
     * Create structured argument for custom objects
     */
    public static Object structuredObject(String key, Object value) {
        return StructuredArguments.kv(key, value);
    }
    
    private static final Map<String, Supplier<org.slf4j.spi.LoggingEventBuilder>> LOG_LEVEL_BUILDERS = Map.of(
        AuthConstants.LOG_LEVEL_ERROR, () -> log.atError(),
        AuthConstants.LOG_LEVEL_WARN, () -> log.atWarn(),
        AuthConstants.LOG_LEVEL_INFO, () -> log.atInfo(),
        AuthConstants.LOG_LEVEL_DEBUG, () -> log.atDebug(),
        AuthConstants.LOG_LEVEL_TRACE, () -> log.atTrace()
    );
    
    /**
     * Log with custom structured data
     */
    public void logWithStructuredData(String message, String logLevel, 
                                     Map<String, Object> structuredData) {
        var logBuilder = LOG_LEVEL_BUILDERS
            .getOrDefault(logLevel.toUpperCase(), () -> log.atInfo())
            .get();
        
        logBuilder = logBuilder.addKeyValue("timestamp", Instant.now());
        
        logBuilder = structuredData.entrySet().stream()
            .reduce(logBuilder,
                (builder, entry) -> builder.addKeyValue(entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);
        
        logBuilder.log(message);
    }
    
    // Utility Methods
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        // Remove sensitive information and limit length
        return userAgent.length() > AuthConstants.MAX_USER_AGENT_LENGTH 
            ? userAgent.substring(0, AuthConstants.MAX_USER_AGENT_LENGTH) 
            : userAgent;
    }
    
    private static final Map<String, String> RISK_LEVEL_PATTERNS = Map.of(
        AuthConstants.RISK_LEVEL_HIGH, "brute|suspicious|blocked",
        AuthConstants.RISK_LEVEL_MEDIUM, "invalid|expired"
    );
    
    private String determineRiskLevel(String failureReason) {
        return Optional.ofNullable(failureReason)
            .map(String::toLowerCase)
            .flatMap(reason -> RISK_LEVEL_PATTERNS.entrySet().stream()
                .filter(entry -> reason.matches(".*(" + entry.getValue() + ").*"))
                .map(Map.Entry::getKey)
                .findFirst())
            .orElse(AuthConstants.RISK_LEVEL_LOW);
    }
}