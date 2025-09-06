package com.trademaster.agentos.observer.impl;

import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.observer.AgentEvent;
import com.trademaster.agentos.observer.AgentEventObserver;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ✅ FUNCTIONAL: Security Audit Observer
 * 
 * Observes agent events to detect security violations and maintain audit trails.
 * Implements comprehensive security monitoring and threat detection.
 * 
 * Features:
 * - Security event correlation and analysis
 * - Threat detection and alerting
 * - Comprehensive audit trail maintenance
 * - Automated incident response triggers
 */
@Component
@RequiredArgsConstructor
public class SecurityAuditObserver implements AgentEventObserver {
    
    private final StructuredLoggingService structuredLogger;
    private final Map<Long, SecurityProfile> agentSecurityProfiles = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> securityIncidentCounters = new ConcurrentHashMap<>();
    
    // Security thresholds
    private static final int MAX_FAILED_OPERATIONS_PER_HOUR = 10;
    private static final int MAX_SECURITY_VIOLATIONS_PER_DAY = 3;
    private static final long SUSPICIOUS_ACTIVITY_WINDOW_MINUTES = 15;
    
    @Override
    public Result<Void, AgentError> handleEvent(AgentEvent event) {
        try {
            // Process all events for comprehensive audit trail
            logSecurityAuditEvent(event);
            
            // Analyze security-specific events using functional approach
            Optional.of(event)
                .filter(this::isSecurityRelevant)
                .ifPresent(this::analyzeSecurityEvent);
            
            // Update security profile for agent
            updateAgentSecurityProfile(event);
            
            // Detect suspicious patterns
            detectSuspiciousActivity(event);
            
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new AgentError.CommunicationFailed(
                event.getAgentId(), "security_audit", 
                "Failed to process security event: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Log comprehensive security audit event
     */
    private void logSecurityAuditEvent(AgentEvent event) {
        Map<String, Object> auditData = new java.util.HashMap<>();
        auditData.put("auditEventId", generateAuditEventId());
        auditData.put("timestamp", event.getTimestamp());
        auditData.put("agentId", event.getAgentId());
        auditData.put("agentName", event.getAgentName());
        auditData.put("eventType", event.getEventType());
        auditData.put("severity", event.getSeverity());
        auditData.put("previousStatus", event.getPreviousStatus());
        auditData.put("currentStatus", event.getCurrentStatus());
        auditData.put("metadata", event.getMetadata());
        auditData.put("isProblematic", event.isProblematic());
        auditData.put("source", event.getSource().orElse("system"));
        
        structuredLogger.logInfo("security_audit_event", auditData);
    }
    
    /**
     * ✅ FUNCTIONAL: Check if event is security-relevant
     */
    private boolean isSecurityRelevant(AgentEvent event) {
        return switch (event.getEventType()) {
            case SECURITY_VIOLATION, UNAUTHORIZED_ACCESS,
                 CRITICAL_ERROR, AGENT_SHUTDOWN,
                 CONFIGURATION_UPDATED, ERROR_OCCURRED -> true;
            default -> event.isProblematic() || event.getSeverity() == AgentEvent.Severity.CRITICAL;
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Analyze security-specific events
     */
    private void analyzeSecurityEvent(AgentEvent event) {
        switch (event.getEventType()) {
            case SECURITY_VIOLATION -> handleSecurityViolation(event);
            case UNAUTHORIZED_ACCESS -> handleUnauthorizedAccess(event);
            case CRITICAL_ERROR -> handleCriticalError(event);
            case CONFIGURATION_UPDATED -> handleConfigurationChange(event);
        }
        
        // Check for escalation needed using functional approach
        Optional.of(event)
            .filter(this::requiresSecurityEscalation)
            .ifPresent(this::escalateSecurityIncident);
    }
    
    /**
     * ✅ FUNCTIONAL: Handle security violation
     */
    private void handleSecurityViolation(AgentEvent event) {
        String incidentKey = "security_violation_" + event.getAgentId();
        int dailyCount = securityIncidentCounters.computeIfAbsent(incidentKey, 
            k -> new AtomicInteger(0)).incrementAndGet();
        
        Map<String, Object> violationData = Map.of(
            "securityIncidentId", generateSecurityIncidentId(),
            "agentId", event.getAgentId(),
            "agentName", event.getAgentName(),
            "violationType", "SECURITY_VIOLATION",
            "dailyCount", dailyCount,
            "errorMessage", event.getErrorMessage().orElse("Unknown violation"),
            "metadata", event.getMetadata(),
            "timestamp", event.getTimestamp()
        );
        
        structuredLogger.logWarning("security_violation_detected", violationData);
        
        Optional.of(dailyCount)
            .filter(count -> count >= MAX_SECURITY_VIOLATIONS_PER_DAY)
            .ifPresent(count -> triggerSecurityLockdown(event.getAgentId(), "Multiple security violations"));
    }
    
    /**
     * ✅ FUNCTIONAL: Handle unauthorized access attempt
     */
    private void handleUnauthorizedAccess(AgentEvent event) {
        String incidentKey = "unauthorized_access_" + event.getAgentId();
        int count = securityIncidentCounters.computeIfAbsent(incidentKey, 
            k -> new AtomicInteger(0)).incrementAndGet();
        
        Map<String, Object> accessData = Map.of(
            "securityIncidentId", generateSecurityIncidentId(),
            "agentId", event.getAgentId(),
            "agentName", event.getAgentName(),
            "accessAttemptType", "UNAUTHORIZED_ACCESS",
            "attemptCount", count,
            "metadata", event.getMetadata(),
            "timestamp", event.getTimestamp()
        );
        
        structuredLogger.logError("unauthorized_access_detected", "Unauthorized access detected", null, accessData);
        
        // Immediate response for unauthorized access using functional approach
        Optional.of(count)
            .filter(c -> c >= 3)
            .ifPresent(c -> triggerSecurityLockdown(event.getAgentId(), "Repeated unauthorized access attempts"));
    }
    
    /**
     * ✅ FUNCTIONAL: Handle critical error with security implications
     */
    private void handleCriticalError(AgentEvent event) {
        // Analyze if critical error has security implications
        String errorMessage = event.getErrorMessage().orElse("");
        boolean hasSecurityImplications = containsSecurityKeywords(errorMessage) ||
                                         event.getMetadata().containsKey("securityContext");
        
        Optional.of(hasSecurityImplications)
            .filter(Boolean::booleanValue)
            .ifPresent(implications -> {
                Map<String, Object> criticalErrorData = Map.of(
                    "securityIncidentId", generateSecurityIncidentId(),
                    "agentId", event.getAgentId(),
                    "agentName", event.getAgentName(),
                    "errorType", "CRITICAL_ERROR_WITH_SECURITY_IMPLICATIONS",
                    "errorMessage", errorMessage,
                    "metadata", event.getMetadata(),
                    "timestamp", event.getTimestamp()
                );
                
                structuredLogger.logError("critical_error_security_implications", "Critical error with security implications", null, criticalErrorData);
            });
    }
    
    /**
     * ✅ FUNCTIONAL: Handle configuration changes
     */
    private void handleConfigurationChange(AgentEvent event) {
        Map<String, Object> configChangeData = Map.of(
            "auditEventId", generateAuditEventId(),
            "agentId", event.getAgentId(),
            "agentName", event.getAgentName(),
            "changeType", "CONFIGURATION_UPDATED",
            "metadata", event.getMetadata(),
            "timestamp", event.getTimestamp()
        );
        
        structuredLogger.logInfo("agent_configuration_changed", configChangeData);
        
        // Check for suspicious configuration changes using functional approach
        Optional.of(event)
            .filter(this::isSuspiciousConfigurationChange)
            .ifPresent(e -> structuredLogger.logWarning("suspicious_configuration_change", configChangeData));
    }
    
    /**
     * ✅ FUNCTIONAL: Update agent security profile
     */
    private void updateAgentSecurityProfile(AgentEvent event) {
        SecurityProfile profile = agentSecurityProfiles.computeIfAbsent(event.getAgentId(),
            k -> new SecurityProfile(event.getAgentId(), event.getAgentName()));
        
        profile.recordEvent(event);
        
        // Check security profile health using functional approach
        Optional.of(profile.getRiskScore())
            .filter(risk -> risk > 0.7)
            .ifPresent(risk -> structuredLogger.logWarning("high_risk_agent_detected", 
                Map.of("agentId", event.getAgentId(),
                       "riskScore", risk,
                       "securityEvents", profile.getSecurityEventCount(),
                       "lastSecurityEvent", profile.getLastSecurityEvent())));
    }
    
    /**
     * ✅ FUNCTIONAL: Detect suspicious activity patterns
     */
    private void detectSuspiciousActivity(AgentEvent event) {
        Optional.ofNullable(agentSecurityProfiles.get(event.getAgentId()))
            .filter(SecurityProfile::hasSuspiciousPattern)
            .ifPresent(profile -> triggerSuspiciousActivityAlert(event, profile));
    }
    
    /**
     * ✅ FUNCTIONAL: Check if security escalation is required
     */
    private boolean requiresSecurityEscalation(AgentEvent event) {
        return event.getEventType() == AgentEvent.AgentEventType.CRITICAL_ERROR ||
               event.getEventType() == AgentEvent.AgentEventType.SECURITY_VIOLATION ||
               event.getSeverity() == AgentEvent.Severity.CRITICAL;
    }
    
    /**
     * ✅ FUNCTIONAL: Escalate security incident
     */
    private void escalateSecurityIncident(AgentEvent event) {
        Map<String, Object> escalationData = Map.of(
            "securityIncidentId", generateSecurityIncidentId(),
            "escalationLevel", "HIGH",
            "agentId", event.getAgentId(),
            "agentName", event.getAgentName(),
            "eventType", event.getEventType(),
            "severity", event.getSeverity(),
            "requiresImmediateAttention", true,
            "escalatedAt", Instant.now()
        );
        
        structuredLogger.logError("security_incident_escalated", "Security incident escalated", null, escalationData);
        
        // In a real implementation, this would trigger:
        // - Immediate notification to security team
        // - Automated incident response procedures
        // - Integration with SIEM systems
    }
    
    /**
     * ✅ FUNCTIONAL: Trigger security lockdown
     */
    private void triggerSecurityLockdown(Long agentId, String reason) {
        Map<String, Object> lockdownData = Map.of(
            "securityLockdownId", generateSecurityIncidentId(),
            "agentId", agentId,
            "lockdownReason", reason,
            "lockdownLevel", "IMMEDIATE",
            "triggeredAt", Instant.now(),
            "requiresManualIntervention", true
        );
        
        structuredLogger.logError("security_lockdown_triggered", "Security lockdown triggered", null, lockdownData);
        
        // In a real implementation, this would:
        // - Immediately suspend agent operations
        // - Isolate agent from sensitive resources
        // - Notify security operations center
        // - Initiate incident response procedures
    }
    
    /**
     * ✅ FUNCTIONAL: Trigger suspicious activity alert
     */
    private void triggerSuspiciousActivityAlert(AgentEvent event, SecurityProfile profile) {
        Map<String, Object> alertData = Map.of(
            "suspiciousActivityId", generateSecurityIncidentId(),
            "agentId", event.getAgentId(),
            "agentName", event.getAgentName(),
            "riskScore", profile.getRiskScore(),
            "suspiciousPattern", profile.getSuspiciousPatternDescription(),
            "detectedAt", Instant.now()
        );
        
        structuredLogger.logWarning("suspicious_activity_detected", alertData);
    }
    
    /**
     * ✅ FUNCTIONAL: Helper methods
     */
    private boolean containsSecurityKeywords(String text) {
        String[] securityKeywords = {"unauthorized", "access", "violation", "breach", "exploit", "attack"};
        String lowerText = text.toLowerCase();
        return java.util.Arrays.stream(securityKeywords).anyMatch(lowerText::contains);
    }
    
    private boolean isSuspiciousConfigurationChange(AgentEvent event) {
        // Implementation would analyze configuration changes for suspicious patterns
        return event.getMetadata().containsKey("privilegeEscalation") ||
               event.getMetadata().containsKey("securityBypass");
    }
    
    private String generateAuditEventId() {
        return "AUDIT_" + Instant.now().toEpochMilli() + "_" + System.nanoTime();
    }
    
    private String generateSecurityIncidentId() {
        return "SEC_INC_" + Instant.now().toEpochMilli() + "_" + System.nanoTime();
    }
    
    /**
     * ✅ FUNCTIONAL: Security Profile Data Structure
     */
    private static class SecurityProfile {
        private final Long agentId;
        private final String agentName;
        private final Instant createdAt;
        
        private int securityEventCount = 0;
        private int unauthorizedAccessAttempts = 0;
        private int configurationChanges = 0;
        private int criticalErrors = 0;
        private Instant lastSecurityEvent;
        private Instant lastUnauthorizedAccess;
        
        public SecurityProfile(Long agentId, String agentName) {
            this.agentId = agentId;
            this.agentName = agentName;
            this.createdAt = Instant.now();
        }
        
        public void recordEvent(AgentEvent event) {
            // Record security violations and unauthorized access
            Optional.of(event.getEventType())
                .filter(type -> type == AgentEvent.AgentEventType.SECURITY_VIOLATION ||
                               type == AgentEvent.AgentEventType.UNAUTHORIZED_ACCESS)
                .ifPresent(type -> {
                    securityEventCount++;
                    lastSecurityEvent = event.getTimestamp();
                });
            
            // Record unauthorized access attempts specifically
            Optional.of(event.getEventType())
                .filter(type -> type == AgentEvent.AgentEventType.UNAUTHORIZED_ACCESS)
                .ifPresent(type -> {
                    unauthorizedAccessAttempts++;
                    lastUnauthorizedAccess = event.getTimestamp();
                });
            
            // Record configuration changes
            Optional.of(event.getEventType())
                .filter(type -> type == AgentEvent.AgentEventType.CONFIGURATION_UPDATED)
                .ifPresent(type -> configurationChanges++);
            
            // Record critical errors
            Optional.of(event.getEventType())
                .filter(type -> type == AgentEvent.AgentEventType.CRITICAL_ERROR)
                .ifPresent(type -> criticalErrors++);
        }
        
        public double getRiskScore() {
            // ✅ FUNCTIONAL: Calculate base risk score
            double baseRiskScore = Math.min(securityEventCount * 0.2, 0.5) +
                                 Math.min(unauthorizedAccessAttempts * 0.3, 0.4) +
                                 Math.min(criticalErrors * 0.1, 0.3);
            
            // ✅ FUNCTIONAL: Calculate recent activity risk bonus using functional approach
            double recentActivityRisk = Optional.ofNullable(lastSecurityEvent)
                .map(lastEvent -> ChronoUnit.MINUTES.between(lastEvent, Instant.now()))
                .filter(minutes -> minutes < 60)
                .map(minutes -> 0.2)
                .orElse(0.0);
            
            return Math.min(baseRiskScore + recentActivityRisk, 1.0);
        }
        
        public boolean hasSuspiciousPattern() {
            // Multiple unauthorized access attempts in short time using functional approach
            boolean recentUnauthorizedAccess = Optional.ofNullable(lastUnauthorizedAccess)
                .map(lastAccess -> ChronoUnit.MINUTES.between(lastAccess, Instant.now()))
                .filter(minutes -> unauthorizedAccessAttempts >= 3 && minutes < SUSPICIOUS_ACTIVITY_WINDOW_MINUTES)
                .isPresent();
            
            // High frequency of security events
            return recentUnauthorizedAccess || securityEventCount >= 5;
        }
        
        public String getSuspiciousPatternDescription() {
            return Optional.of(unauthorizedAccessAttempts)
                .filter(attempts -> attempts >= 3)
                .map(attempts -> "Multiple unauthorized access attempts")
                .orElse(Optional.of(securityEventCount)
                    .filter(events -> events >= 5)
                    .map(events -> "High frequency of security events")
                    .orElse("Unknown suspicious pattern"));
        }
        
        // Getters
        public int getSecurityEventCount() { return securityEventCount; }
        public Instant getLastSecurityEvent() { return lastSecurityEvent; }
    }
}