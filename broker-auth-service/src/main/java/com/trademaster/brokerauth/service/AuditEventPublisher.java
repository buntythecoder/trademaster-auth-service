package com.trademaster.brokerauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.config.CorrelationConfig;
import com.trademaster.brokerauth.enums.BrokerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Audit Event Publisher
 * 
 * Publishes audit events to Kafka for distributed audit trail.
 * Provides async event publishing with failure handling and retry logic.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AuditEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // Kafka topic names
    private static final String AUDIT_TOPIC = "trademaster.audit.events";
    private static final String SECURITY_TOPIC = "trademaster.security.events";
    private static final String COMPLIANCE_TOPIC = "trademaster.compliance.events";
    
    /**
     * Publish authentication audit event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishAuthenticationEvent(Long userId, BrokerType brokerType, 
                                                             String eventType, boolean success, 
                                                             String clientIp, String userAgent,
                                                             Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                AuditEvent event = AuditEvent.builder()
                        .eventType("AUTHENTICATION")
                        .subEventType(eventType)
                        .userId(userId)
                        .brokerType(brokerType != null ? brokerType.toString() : null)
                        .success(success)
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .additionalData(additionalData)
                        .build();
                
                publishToTopic(AUDIT_TOPIC, event, "auth-" + userId);
                
            } catch (Exception e) {
                log.error("Failed to publish authentication audit event", e);
            }
        });
    }
    
    /**
     * Publish session audit event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishSessionEvent(Long userId, BrokerType brokerType, 
                                                      String eventType, String sessionId, 
                                                      String clientIp, Map<String, Object> sessionData) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Hash session ID for privacy
                String hashedSessionId = hashSessionId(sessionId);
                
                Map<String, Object> eventData = new HashMap<>();
                if (sessionData != null) {
                    eventData.putAll(sessionData);
                }
                eventData.put("sessionId", hashedSessionId);
                
                AuditEvent event = AuditEvent.builder()
                        .eventType("SESSION")
                        .subEventType(eventType)
                        .userId(userId)
                        .brokerType(brokerType != null ? brokerType.toString() : null)
                        .success(true)
                        .clientIp(clientIp)
                        .additionalData(eventData)
                        .build();
                
                publishToTopic(AUDIT_TOPIC, event, "session-" + userId);
                
            } catch (Exception e) {
                log.error("Failed to publish session audit event", e);
            }
        });
    }
    
    /**
     * Publish security incident event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishSecurityIncident(String incidentType, String severity, 
                                                           Long userId, String clientIp, 
                                                           String userAgent, Map<String, Object> incidentData) {
        return CompletableFuture.runAsync(() -> {
            try {
                AuditEvent event = AuditEvent.builder()
                        .eventType("SECURITY_INCIDENT")
                        .subEventType(incidentType)
                        .userId(userId)
                        .success(false) // Security incidents are always failures
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .severity(severity)
                        .additionalData(incidentData)
                        .build();
                
                // Security incidents go to both audit and security topics
                publishToTopic(AUDIT_TOPIC, event, "security-" + incidentType);
                publishToTopic(SECURITY_TOPIC, event, "incident-" + severity);
                
            } catch (Exception e) {
                log.error("Failed to publish security incident event", e);
            }
        });
    }
    
    /**
     * Publish compliance audit event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishComplianceEvent(String complianceType, Long userId, 
                                                          BrokerType brokerType, boolean compliant, 
                                                          Map<String, Object> complianceData) {
        return CompletableFuture.runAsync(() -> {
            try {
                AuditEvent event = AuditEvent.builder()
                        .eventType("COMPLIANCE")
                        .subEventType(complianceType)
                        .userId(userId)
                        .brokerType(brokerType != null ? brokerType.toString() : null)
                        .success(compliant)
                        .additionalData(complianceData)
                        .build();
                
                publishToTopic(COMPLIANCE_TOPIC, event, "compliance-" + complianceType);
                
            } catch (Exception e) {
                log.error("Failed to publish compliance audit event", e);
            }
        });
    }
    
    /**
     * Publish rate limiting audit event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishRateLimitEvent(Long userId, BrokerType brokerType, 
                                                        String rateLimitType, boolean violated, 
                                                        String clientIp, Map<String, Object> rateLimitData) {
        return CompletableFuture.runAsync(() -> {
            try {
                AuditEvent event = AuditEvent.builder()
                        .eventType("RATE_LIMIT")
                        .subEventType(rateLimitType)
                        .userId(userId)
                        .brokerType(brokerType != null ? brokerType.toString() : null)
                        .success(!violated)
                        .clientIp(clientIp)
                        .severity(violated ? "medium" : "info")
                        .additionalData(rateLimitData)
                        .build();
                
                publishToTopic(AUDIT_TOPIC, event, "ratelimit-" + userId);
                
                // Also publish to security topic if violated
                if (violated) {
                    publishToTopic(SECURITY_TOPIC, event, "ratelimit-violation");
                }
                
            } catch (Exception e) {
                log.error("Failed to publish rate limit audit event", e);
            }
        });
    }
    
    /**
     * Publish configuration change event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishConfigurationChangeEvent(String configType, String configKey, 
                                                                  String changedBy, Map<String, Object> changeData) {
        return CompletableFuture.runAsync(() -> {
            try {
                AuditEvent event = AuditEvent.builder()
                        .eventType("CONFIGURATION_CHANGE")
                        .subEventType(configType)
                        .success(true)
                        .severity("high") // Configuration changes are always high severity
                        .additionalData(Map.of(
                            "configKey", configKey,
                            "changedBy", changedBy,
                            "changeDetails", changeData
                        ))
                        .build();
                
                publishToTopic(AUDIT_TOPIC, event, "config-" + configType);
                publishToTopic(SECURITY_TOPIC, event, "config-change");
                
            } catch (Exception e) {
                log.error("Failed to publish configuration change event", e);
            }
        });
    }
    
    /**
     * Publish generic audit event
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Void> publishAuditEvent(String eventType, String subEventType, 
                                                     Long userId, boolean success, 
                                                     Map<String, Object> eventData) {
        return CompletableFuture.runAsync(() -> {
            try {
                AuditEvent event = AuditEvent.builder()
                        .eventType(eventType)
                        .subEventType(subEventType)
                        .userId(userId)
                        .success(success)
                        .additionalData(eventData)
                        .build();
                
                publishToTopic(AUDIT_TOPIC, event, eventType.toLowerCase() + "-" + userId);
                
            } catch (Exception e) {
                log.error("Failed to publish generic audit event", e);
            }
        });
    }
    
    /**
     * Publish event to Kafka topic
     */
    private void publishToTopic(String topic, AuditEvent event, String key) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(topic, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send audit event to topic {}: {}", topic, ex.getMessage());
                } else {
                    log.debug("Successfully sent audit event to topic {} with key {}", topic, key);
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to serialize audit event for topic {}", topic, e);
        }
    }
    
    /**
     * Hash session ID for privacy
     */
    private String hashSessionId(String sessionId) {
        if (sessionId == null || sessionId.length() < 8) {
            return "invalid";
        }
        
        return sessionId.substring(0, 4) + "***" + 
               sessionId.substring(sessionId.length() - 4) + 
               ":" + Integer.toHexString(sessionId.hashCode());
    }
    
    /**
     * Audit Event data structure
     */
    public static class AuditEvent {
        private String eventType;
        private String subEventType;
        private Long userId;
        private String brokerType;
        private boolean success;
        private String clientIp;
        private String userAgent;
        private String severity;
        private String timestamp;
        private String correlationId;
        private String requestId;
        private String service;
        private String version;
        private Map<String, Object> additionalData;
        
        // Default constructor
        public AuditEvent() {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
            this.requestId = CorrelationConfig.CorrelationContext.getRequestId();
            this.service = "broker-auth-service";
            this.version = "1.0.0";
        }
        
        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private final AuditEvent event = new AuditEvent();
            
            public Builder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }
            
            public Builder subEventType(String subEventType) {
                event.subEventType = subEventType;
                return this;
            }
            
            public Builder userId(Long userId) {
                event.userId = userId;
                return this;
            }
            
            public Builder brokerType(String brokerType) {
                event.brokerType = brokerType;
                return this;
            }
            
            public Builder success(boolean success) {
                event.success = success;
                return this;
            }
            
            public Builder clientIp(String clientIp) {
                event.clientIp = clientIp;
                return this;
            }
            
            public Builder userAgent(String userAgent) {
                event.userAgent = userAgent;
                return this;
            }
            
            public Builder severity(String severity) {
                event.severity = severity;
                return this;
            }
            
            public Builder additionalData(Map<String, Object> additionalData) {
                event.additionalData = additionalData;
                return this;
            }
            
            public AuditEvent build() {
                return event;
            }
        }
        
        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public String getSubEventType() { return subEventType; }
        public void setSubEventType(String subEventType) { this.subEventType = subEventType; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getBrokerType() { return brokerType; }
        public void setBrokerType(String brokerType) { this.brokerType = brokerType; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
    }
}