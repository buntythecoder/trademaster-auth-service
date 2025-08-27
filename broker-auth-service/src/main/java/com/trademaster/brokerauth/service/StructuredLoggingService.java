package com.trademaster.brokerauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.config.CorrelationConfig;
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
 * Structured Logging Service
 * 
 * Provides structured JSON logging for ELK stack integration.
 * Includes correlation tracking and standardized log formats.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructuredLoggingService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Log authentication event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logAuthEvent(String eventType, String brokerType, 
                                               Long userId, boolean success, 
                                               Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logData = createBaseLogData("AUTH_EVENT", eventType);
                logData.put("brokerType", brokerType);
                logData.put("userId", userId);
                logData.put("success", success);
                
                if (additionalData != null) {
                    logData.putAll(additionalData);
                }
                
                String jsonLog = objectMapper.writeValueAsString(logData);
                
                if (success) {
                    log.info("AUTH_EVENT: {}", jsonLog);
                } else {
                    log.warn("AUTH_EVENT: {}", jsonLog);
                }
                
            } catch (Exception e) {
                log.error("Failed to log authentication event", e);
            }
        });
    }
    
    /**
     * Log session event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logSessionEvent(String eventType, String sessionId, 
                                                  String brokerType, Long userId,
                                                  Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logData = createBaseLogData("SESSION_EVENT", eventType);
                logData.put("sessionId", sessionId);
                logData.put("brokerType", brokerType);
                logData.put("userId", userId);
                
                if (additionalData != null) {
                    logData.putAll(additionalData);
                }
                
                String jsonLog = objectMapper.writeValueAsString(logData);
                log.info("SESSION_EVENT: {}", jsonLog);
                
            } catch (Exception e) {
                log.error("Failed to log session event", e);
            }
        });
    }
    
    /**
     * Log rate limit event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logRateLimitEvent(String eventType, String brokerType, 
                                                    Long userId, boolean exceeded,
                                                    Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logData = createBaseLogData("RATE_LIMIT_EVENT", eventType);
                logData.put("brokerType", brokerType);
                logData.put("userId", userId);
                logData.put("exceeded", exceeded);
                
                if (additionalData != null) {
                    logData.putAll(additionalData);
                }
                
                String jsonLog = objectMapper.writeValueAsString(logData);
                
                if (exceeded) {
                    log.warn("RATE_LIMIT_EVENT: {}", jsonLog);
                } else {
                    log.debug("RATE_LIMIT_EVENT: {}", jsonLog);
                }
                
            } catch (Exception e) {
                log.error("Failed to log rate limit event", e);
            }
        });
    }
    
    /**
     * Log error with full context
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logError(String eventType, String errorMessage, 
                                           String errorCategory, Throwable throwable,
                                           Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logData = createBaseLogData("ERROR_EVENT", eventType);
                logData.put("errorMessage", errorMessage);
                logData.put("errorCategory", errorCategory);
                
                if (throwable != null) {
                    logData.put("exceptionType", throwable.getClass().getSimpleName());
                    logData.put("stackTrace", getStackTraceString(throwable));
                }
                
                if (additionalData != null) {
                    logData.putAll(additionalData);
                }
                
                String jsonLog = objectMapper.writeValueAsString(logData);
                log.error("ERROR_EVENT: {}", jsonLog);
                
            } catch (Exception e) {
                log.error("Failed to log error event", e);
            }
        });
    }
    
    /**
     * Log security incident
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logSecurityIncident(String incidentType, String severity,
                                                      String userId, String clientIp,
                                                      String userAgent, Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logData = createBaseLogData("SECURITY_INCIDENT", incidentType);
                logData.put("severity", severity);
                logData.put("affectedUserId", userId);
                logData.put("clientIp", clientIp);
                logData.put("userAgent", userAgent);
                
                if (additionalData != null) {
                    logData.putAll(additionalData);
                }
                
                String jsonLog = objectMapper.writeValueAsString(logData);
                log.warn("SECURITY_INCIDENT: {}", jsonLog);
                
            } catch (Exception e) {
                log.error("Failed to log security incident", e);
            }
        });
    }
    
    /**
     * Log performance metrics
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> logPerformanceMetrics(String operationType, long durationMs,
                                                         boolean success, Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logData = createBaseLogData("PERFORMANCE_METRICS", operationType);
                logData.put("durationMs", durationMs);
                logData.put("success", success);
                
                if (additionalData != null) {
                    logData.putAll(additionalData);
                }
                
                String jsonLog = objectMapper.writeValueAsString(logData);
                log.info("PERFORMANCE_METRICS: {}", jsonLog);
                
            } catch (Exception e) {
                log.error("Failed to log performance metrics", e);
            }
        });
    }
    
    /**
     * Create base log data with correlation information
     */
    private Map<String, Object> createBaseLogData(String logType, String eventType) {
        Map<String, Object> logData = new HashMap<>();
        
        // Add correlation information
        logData.put("logType", logType);
        logData.put("eventType", eventType);
        logData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logData.put("service", "broker-auth-service");
        logData.put("version", "1.0.0");
        
        // Add correlation context if available
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
        if (correlationId != null) {
            logData.put("correlationId", correlationId);
        }
        
        String requestId = CorrelationConfig.CorrelationContext.getRequestId();
        if (requestId != null) {
            logData.put("requestId", requestId);
        }
        
        String userId = CorrelationConfig.CorrelationContext.getUserId();
        if (userId != null) {
            logData.put("contextUserId", userId);
        }
        
        return logData;
    }
    
    /**
     * Extract stack trace as string
     */
    private String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        
        String stackTrace = sw.toString();
        
        // Limit stack trace length to prevent log explosion
        if (stackTrace.length() > 5000) {
            stackTrace = stackTrace.substring(0, 5000) + "... (truncated)";
        }
        
        return stackTrace;
    }
}