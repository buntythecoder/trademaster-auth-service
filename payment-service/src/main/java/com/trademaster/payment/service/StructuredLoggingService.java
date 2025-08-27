package com.trademaster.payment.service;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MANDATORY Structured Logging Service
 * 
 * Provides structured JSON logging for ELK stack integration as per TradeMaster standards.
 * Implements context preservation for Virtual Thread operations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class StructuredLoggingService {

    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger BUSINESS_AUDIT = LoggerFactory.getLogger("BUSINESS_AUDIT");
    private static final Logger PERFORMANCE = LoggerFactory.getLogger("PERFORMANCE");
    
    // Context Keys
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String USER_AGENT = "userAgent";
    private static final String TRANSACTION_ID = "transactionId";
    private static final String SUBSCRIPTION_ID = "subscriptionId";
    
    /**
     * Set correlation ID for request tracking
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId != null ? correlationId : UUID.randomUUID().toString());
    }
    
    /**
     * Set user context for logging
     */
    public void setUserContext(String userId, String sessionId, String ipAddress, String userAgent) {
        if (userId != null) MDC.put(USER_ID, userId);
        if (sessionId != null) MDC.put(SESSION_ID, sessionId);
        if (ipAddress != null) MDC.put(IP_ADDRESS, ipAddress);
        if (userAgent != null) MDC.put(USER_AGENT, userAgent);
    }
    
    /**
     * Set business context for logging
     */
    public void setBusinessContext(String transactionId, String subscriptionId) {
        if (transactionId != null) MDC.put(TRANSACTION_ID, transactionId);
        if (subscriptionId != null) MDC.put(SUBSCRIPTION_ID, subscriptionId);
    }
    
    /**
     * Clear all MDC context
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific context key
     */
    public void clearContext(String key) {
        MDC.remove(key);
    }
    
    // Security Audit Logs - MANDATORY per standards
    
    public void logSecurityIncident(String incidentType, String severity, String userId, 
                                  String ipAddress, String userAgent, Map<String, Object> details) {
        SECURITY_AUDIT.warn("Security incident detected",
            StructuredArguments.kv("incident_type", incidentType),
            StructuredArguments.kv("severity", severity),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("operation", "security_incident"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }
    
    public void logAuthenticationSuccess(String userId, String sessionId, String ipAddress, 
                                       String userAgent, long durationMs) {
        SECURITY_AUDIT.info("Authentication successful",
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("session_id", sessionId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("duration_ms", durationMs),
            StructuredArguments.kv("operation", "authentication"),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logAuthenticationFailure(String userIdOrEmail, String ipAddress, String userAgent, 
                                       String failureReason) {
        SECURITY_AUDIT.warn("Authentication failed",
            StructuredArguments.kv("user_identifier", userIdOrEmail),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("failure_reason", failureReason),
            StructuredArguments.kv("operation", "authentication"),
            StructuredArguments.kv("status", "failure"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logRateLimitViolation(String endpoint, String userId, String ipAddress, 
                                    String userAgent, String violationType) {
        SECURITY_AUDIT.warn("Rate limit violation",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("violation_type", violationType),
            StructuredArguments.kv("operation", "rate_limit_violation"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // Business Audit Logs - MANDATORY for Compliance
    
    public void logPaymentTransaction(String operation, String transactionId, String userId, 
                                    String gateway, String amount, String currency, 
                                    String status, Map<String, Object> metadata) {
        BUSINESS_AUDIT.info("Payment transaction event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("transaction_id", transactionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("gateway", gateway),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("currency", currency),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("metadata", metadata)
        );
    }
    
    public void logRefundTransaction(String operation, String refundId, String transactionId, 
                                   String userId, String gateway, String amount, String currency, 
                                   String reason, String status) {
        BUSINESS_AUDIT.info("Refund transaction event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("refund_id", refundId),
            StructuredArguments.kv("transaction_id", transactionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("gateway", gateway),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("currency", currency),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logSubscriptionEvent(String operation, String subscriptionId, String userId, 
                                   String planId, String planName, String amount, 
                                   String billingCycle, String status) {
        BUSINESS_AUDIT.info("Subscription event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("plan_id", planId),
            StructuredArguments.kv("plan_name", planName),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("billing_cycle", billingCycle),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logPaymentMethodEvent(String operation, String paymentMethodId, String userId, 
                                    String paymentMethodType, String gateway, String status) {
        BUSINESS_AUDIT.info("Payment method event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("payment_method_id", paymentMethodId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("payment_method_type", paymentMethodType),
            StructuredArguments.kv("gateway", gateway),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // Performance Logs - MANDATORY per standards
    
    public void logPerformanceMetric(String operation, String component, long durationMs, 
                                   String status, Map<String, Object> additionalMetrics) {
        PERFORMANCE.info("Performance metric",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("component", component),
            StructuredArguments.kv("duration_ms", durationMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("metrics", additionalMetrics)
        );
    }
    
    public void logDatabasePerformance(String query, long executionTimeMs, String status, 
                                     int recordsAffected) {
        PERFORMANCE.info("Database performance",
            StructuredArguments.kv("operation", "database_query"),
            StructuredArguments.kv("query_type", query),
            StructuredArguments.kv("execution_time_ms", executionTimeMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("records_affected", recordsAffected),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logGatewayPerformance(String gateway, String operation, long responseTimeMs, 
                                    String status, String responseCode) {
        PERFORMANCE.info("Gateway performance",
            StructuredArguments.kv("operation", "gateway_call"),
            StructuredArguments.kv("gateway", gateway),
            StructuredArguments.kv("gateway_operation", operation),
            StructuredArguments.kv("response_time_ms", responseTimeMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("response_code", responseCode),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // Application Logs - Standard structured logging
    
    public void logInfo(String operation, String message, Map<String, Object> context) {
        log.info("{}",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("message", message),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context)
        );
    }
    
    public void logError(String operation, String errorMessage, String errorCode, 
                        Throwable throwable, Map<String, Object> context) {
        log.error("Error in operation",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("error_message", errorMessage),
            StructuredArguments.kv("error_code", errorCode),
            StructuredArguments.kv("exception_class", throwable != null ? throwable.getClass().getSimpleName() : null),
            StructuredArguments.kv("stack_trace", throwable != null ? throwable.getMessage() : null),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context),
            throwable
        );
    }
}