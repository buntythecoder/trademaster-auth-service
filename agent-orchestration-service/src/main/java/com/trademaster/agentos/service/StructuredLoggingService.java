package com.trademaster.agentos.service;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MANDATORY: Structured Logging Service for ELK Stack
 * 
 * Implements comprehensive structured JSON logging as required by 
 * trademaster-coding-standards.md v2.0:
 * - Structured JSON format with correlation IDs
 * - Context preservation across Virtual Thread operations
 * - Separate audit trails for compliance
 * - Security incident logging
 * - Performance metrics logging
 */
@Service
public class StructuredLoggingService {
    
    private static final Logger applicationLog = LoggerFactory.getLogger(StructuredLoggingService.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    // MDC Context Keys
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String USER_AGENT = "userAgent";
    private static final String OPERATION = "operation";
    
    /**
     * Initialize logging context for a request
     * MANDATORY: Call this at the start of each request to preserve context
     */
    public void initializeRequestContext(String userId, String sessionId, String ipAddress, String userAgent) {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
        if (userId != null) MDC.put(USER_ID, userId);
        if (sessionId != null) MDC.put(SESSION_ID, sessionId);
        if (ipAddress != null) MDC.put(IP_ADDRESS, ipAddress);
        if (userAgent != null) MDC.put(USER_AGENT, userAgent);
    }
    
    /**
     * Clear logging context - MANDATORY to call at request completion
     * Prevents memory leaks in Virtual Thread environment
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Set operation context for business logic
     */
    public void setOperationContext(String operation) {
        MDC.put(OPERATION, operation);
    }
    
    // APPLICATION LOGS
    
    public void logAgentCreated(String agentId, String agentType, String createdBy) {
        applicationLog.info("Agent created successfully",
            StructuredArguments.kv("agentId", agentId),
            StructuredArguments.kv("agentType", agentType),
            StructuredArguments.kv("createdBy", createdBy),
            StructuredArguments.kv("operation", "agent_creation"),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logAgentDestroyed(String agentId, String agentType, String destroyedBy, String reason) {
        applicationLog.info("Agent destroyed",
            StructuredArguments.kv("agentId", agentId),
            StructuredArguments.kv("agentType", agentType),
            StructuredArguments.kv("destroyedBy", destroyedBy),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("operation", "agent_destruction"),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logTaskExecution(String taskId, String taskType, String agentId, String priority, long durationMs) {
        applicationLog.info("Task executed",
            StructuredArguments.kv("taskId", taskId),
            StructuredArguments.kv("taskType", taskType),
            StructuredArguments.kv("agentId", agentId),
            StructuredArguments.kv("priority", priority),
            StructuredArguments.kv("durationMs", durationMs),
            StructuredArguments.kv("operation", "task_execution"),
            StructuredArguments.kv("status", "completed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logTaskFailure(String taskId, String taskType, String agentId, String errorMessage, long durationMs) {
        applicationLog.error("Task execution failed",
            StructuredArguments.kv("taskId", taskId),
            StructuredArguments.kv("taskType", taskType),
            StructuredArguments.kv("agentId", agentId),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv("durationMs", durationMs),
            StructuredArguments.kv("operation", "task_execution"),
            StructuredArguments.kv("status", "failed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logWorkflowCompletion(String workflowId, String workflowType, int tasksCompleted, long totalDurationMs) {
        applicationLog.info("Workflow completed",
            StructuredArguments.kv("workflowId", workflowId),
            StructuredArguments.kv("workflowType", workflowType),
            StructuredArguments.kv("tasksCompleted", tasksCompleted),
            StructuredArguments.kv("totalDurationMs", totalDurationMs),
            StructuredArguments.kv("operation", "workflow_completion"),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // SECURITY LOGS - MANDATORY
    
    public void logAuthenticationSuccess(String userId, String sessionId, String ipAddress, 
                                       String authMethod, long durationMs) {
        securityLog.info("Authentication successful",
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("sessionId", sessionId),
            StructuredArguments.kv("ipAddress", ipAddress),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv("durationMs", durationMs),
            StructuredArguments.kv("operation", "authentication"),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logAuthenticationFailure(String attemptedUserId, String ipAddress, String authMethod,
                                       String failureReason, long durationMs) {
        securityLog.warn("Authentication failed",
            StructuredArguments.kv("attemptedUserId", attemptedUserId),
            StructuredArguments.kv("ipAddress", ipAddress),
            StructuredArguments.kv("authMethod", authMethod),
            StructuredArguments.kv("failureReason", failureReason),
            StructuredArguments.kv("durationMs", durationMs),
            StructuredArguments.kv("operation", "authentication"),
            StructuredArguments.kv("status", "failed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logSecurityIncident(String incidentType, String severity, String userId, 
                                  String ipAddress, Map<String, Object> details) {
        securityLog.error("Security incident detected",
            StructuredArguments.kv("incidentType", incidentType),
            StructuredArguments.kv("severity", severity),
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("ipAddress", ipAddress),
            StructuredArguments.kv("operation", "security_incident"),
            StructuredArguments.kv("status", "detected"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }
    
    public void logRateLimitViolation(String endpoint, String ipAddress, String userId, int requestCount) {
        securityLog.warn("Rate limit violation",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("ipAddress", ipAddress),
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("requestCount", requestCount),
            StructuredArguments.kv("operation", "rate_limit_violation"),
            StructuredArguments.kv("status", "blocked"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logTokenValidation(String tokenType, String userId, boolean valid, String reason) {
        securityLog.info("Token validation",
            StructuredArguments.kv("tokenType", tokenType),
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("valid", valid),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("operation", "token_validation"),
            StructuredArguments.kv("status", valid ? "success" : "failed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // AUDIT LOGS - MANDATORY for compliance
    
    public void logBusinessTransaction(String transactionType, String entityId, String action,
                                     String performedBy, Map<String, Object> changes) {
        auditLog.info("Business transaction",
            StructuredArguments.kv("transactionType", transactionType),
            StructuredArguments.kv("entityId", entityId),
            StructuredArguments.kv("action", action),
            StructuredArguments.kv("performedBy", performedBy),
            StructuredArguments.kv("operation", "business_transaction"),
            StructuredArguments.kv("status", "completed"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("changes", changes)
        );
    }
    
    public void logDataAccess(String dataType, String entityId, String accessType, String userId) {
        auditLog.info("Data access",
            StructuredArguments.kv("dataType", dataType),
            StructuredArguments.kv("entityId", entityId),
            StructuredArguments.kv("accessType", accessType),
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("operation", "data_access"),
            StructuredArguments.kv("status", "accessed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logConfigurationChange(String configKey, String oldValue, String newValue, String changedBy) {
        auditLog.warn("Configuration changed",
            StructuredArguments.kv("configKey", configKey),
            StructuredArguments.kv("oldValue", oldValue != null ? "***masked***" : null),
            StructuredArguments.kv("newValue", newValue != null ? "***masked***" : null),
            StructuredArguments.kv("changedBy", changedBy),
            StructuredArguments.kv("operation", "configuration_change"),
            StructuredArguments.kv("status", "changed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // PERFORMANCE LOGS
    
    public void logSlowOperation(String operation, String details, long durationMs, long thresholdMs) {
        applicationLog.warn("Slow operation detected",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("details", details),
            StructuredArguments.kv("durationMs", durationMs),
            StructuredArguments.kv("thresholdMs", thresholdMs),
            StructuredArguments.kv("operationType", "performance_warning"),
            StructuredArguments.kv("status", "slow"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logVirtualThreadMetrics(long activeThreads, long totalCreated, String context) {
        applicationLog.debug("Virtual thread metrics",
            StructuredArguments.kv("activeVirtualThreads", activeThreads),
            StructuredArguments.kv("totalVirtualThreadsCreated", totalCreated),
            StructuredArguments.kv("context", context),
            StructuredArguments.kv("operation", "virtual_thread_metrics"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // ERROR LOGGING
    
    public void logError(String operation, String errorMessage, Exception exception, Map<String, Object> context) {
        applicationLog.error("Operation failed",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv("exceptionClass", exception != null ? exception.getClass().getSimpleName() : null),
            StructuredArguments.kv("operation", "error"),
            StructuredArguments.kv("status", "failed"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context),
            exception
        );
    }
    
    // ✅ GENERIC LOGGING METHODS
    
    public void logInfo(String operation, Map<String, Object> context) {
        applicationLog.info("Operation completed",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context)
        );
    }
    
    public void logDebug(String operation, Map<String, Object> context) {
        applicationLog.debug("Operation debug",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context)
        );
    }
    
    public void logWarning(String operation, Map<String, Object> context) {
        applicationLog.warn("Operation warning",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("status", "warning"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context)
        );
    }
    
    // UTILITY METHODS
    
    /**
     * Get current correlation ID for use in service calls
     */
    public String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }
    
    /**
     * Propagate context to child Virtual Threads
     * MANDATORY: Use when spawning async operations to preserve context
     */
    public Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }
    
    /**
     * Set context from parent thread
     * MANDATORY: Use in async operations to restore context
     */
    public void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }
}