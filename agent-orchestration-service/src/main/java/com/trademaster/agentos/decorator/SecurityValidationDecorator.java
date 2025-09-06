package com.trademaster.agentos.decorator;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * ✅ FUNCTIONAL: Security Validation Decorator
 * 
 * Decorates agent operations with comprehensive security validation.
 * Implements security policies, access controls, and threat detection.
 * 
 * Features:
 * - Pre-operation security validation
 * - Post-operation security auditing
 * - Threat detection and prevention
 * - Security policy enforcement
 */
@Component
@RequiredArgsConstructor
public class SecurityValidationDecorator implements AgentServiceDecorator {
    
    private final StructuredLoggingService structuredLogger;
    
    // Security policies
    private static final Set<String> RESTRICTED_OPERATIONS = Set.of("SHUTDOWN", "MAINTENANCE");
    private static final long MAX_HEARTBEAT_AGE_MINUTES = 5;
    private static final int MAX_CONCURRENT_OPERATIONS = 100;
    
    @Override
    public <T> java.util.function.Function<Agent, Result<T, AgentError>> decorate(
            java.util.function.Function<Agent, Result<T, AgentError>> operation) {
        
        return agent -> performSecurityValidation(agent)
            .flatMap(validatedAgent -> {
                // Execute operation with security context
                return operation.apply(validatedAgent)
                    .onSuccess(result -> auditSecuritySuccess(agent, result))
                    .onFailure(error -> auditSecurityFailure(agent, error));
            });
    }
    
    /**
     * ✅ FUNCTIONAL: Perform comprehensive security validation
     */
    private Result<Agent, AgentError> performSecurityValidation(Agent agent) {
        return validateAgentIdentity(agent)
            .flatMap(this::validateAgentStatus)
            .flatMap(this::validateHeartbeatFreshness)
            .flatMap(this::validateOperationLimits)
            .flatMap(this::validateSecurityPolicies);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent identity and integrity
     */
    private Result<Agent, AgentError> validateAgentIdentity(Agent agent) {
        if (agent == null) {
            return Result.failure(new AgentError.SecurityViolation(
                null, "identity_validation", "Agent identity is null"));
        }
        
        if (agent.getAgentId() == null || agent.getAgentId() <= 0) {
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "identity_validation", "Invalid agent ID"));
        }
        
        if (agent.getAgentName() == null || agent.getAgentName().trim().isEmpty()) {
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "identity_validation", "Agent name is required"));
        }
        
        // Check for suspicious agent names
        if (containsSuspiciousPatterns(agent.getAgentName())) {
            structuredLogger.logWarning("security_suspicious_agent_name", 
                Map.of("agentId", agent.getAgentId(),
                       "agentName", agent.getAgentName(),
                       "securityEvent", "SUSPICIOUS_NAME_DETECTED"));
            
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "identity_validation", "Suspicious agent name detected"));
        }
        
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent status for security compliance
     */
    private Result<Agent, AgentError> validateAgentStatus(Agent agent) {
        if (agent.getStatus() == null) {
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "status_validation", "Agent status is required"));
        }
        
        // Prevent operations on compromised or failed agents
        switch (agent.getStatus()) {
            case FAILED -> {
                return Result.failure(new AgentError.SecurityViolation(
                    agent.getAgentId(), "status_validation", "Operations not allowed on failed agents"));
            }
            case SHUTDOWN -> {
                return Result.failure(new AgentError.SecurityViolation(
                    agent.getAgentId(), "status_validation", "Operations not allowed on shutdown agents"));
            }
            default -> {
                return Result.success(agent);
            }
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Validate heartbeat freshness for security
     */
    private Result<Agent, AgentError> validateHeartbeatFreshness(Agent agent) {
        if (agent.getLastHeartbeat() == null) {
            structuredLogger.logWarning("security_missing_heartbeat", 
                Map.of("agentId", agent.getAgentId(),
                       "securityEvent", "MISSING_HEARTBEAT"));
            
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "heartbeat_validation", "Agent heartbeat is missing"));
        }
        
        long minutesSinceHeartbeat = ChronoUnit.MINUTES.between(
            agent.getLastHeartbeat(), Instant.now());
        
        if (minutesSinceHeartbeat > MAX_HEARTBEAT_AGE_MINUTES) {
            structuredLogger.logWarning("security_stale_heartbeat", 
                Map.of("agentId", agent.getAgentId(),
                       "minutesSinceHeartbeat", minutesSinceHeartbeat,
                       "maxAllowedMinutes", MAX_HEARTBEAT_AGE_MINUTES,
                       "securityEvent", "STALE_HEARTBEAT"));
            
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "heartbeat_validation", 
                "Agent heartbeat is too old: " + minutesSinceHeartbeat + " minutes"));
        }
        
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate operation limits for security
     */
    private Result<Agent, AgentError> validateOperationLimits(Agent agent) {
        if (agent.getCurrentLoad() != null && agent.getCurrentLoad() > MAX_CONCURRENT_OPERATIONS) {
            structuredLogger.logWarning("security_operation_limit_exceeded", 
                Map.of("agentId", agent.getAgentId(),
                       "currentLoad", agent.getCurrentLoad(),
                       "maxAllowed", MAX_CONCURRENT_OPERATIONS,
                       "securityEvent", "OPERATION_LIMIT_EXCEEDED"));
            
            return Result.failure(new AgentError.SecurityViolation(
                agent.getAgentId(), "operation_limits", 
                "Agent operation limit exceeded: " + agent.getCurrentLoad()));
        }
        
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate security policies
     */
    private Result<Agent, AgentError> validateSecurityPolicies(Agent agent) {
        // Additional security policy validations can be added here
        // For example: role-based access control, resource quotas, etc.
        
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Audit successful security operations
     */
    private <T> void auditSecuritySuccess(Agent agent, T result) {
        structuredLogger.logInfo("security_operation_success", 
            Map.of("agentId", agent.getAgentId(),
                   "agentName", agent.getAgentName(),
                   "resultType", result.getClass().getSimpleName(),
                   "securityEvent", "OPERATION_AUTHORIZED",
                   "timestamp", Instant.now()));
    }
    
    /**
     * ✅ FUNCTIONAL: Audit failed security operations
     */
    private void auditSecurityFailure(Agent agent, AgentError error) {
        structuredLogger.logWarning("security_operation_failure", 
            Map.of("agentId", agent.getAgentId(),
                   "agentName", agent.getAgentName(),
                   "errorType", error.getClass().getSimpleName(),
                   "errorMessage", error.getMessage(),
                   "securityEvent", "OPERATION_DENIED",
                   "timestamp", Instant.now()));
    }
    
    /**
     * ✅ FUNCTIONAL: Check for suspicious patterns in agent names
     */
    private boolean containsSuspiciousPatterns(String agentName) {
        String[] suspiciousPatterns = {
            "admin", "root", "system", "service", "daemon",
            "script", "bot", "crawler", "spider", "hack"
        };
        
        String lowerName = agentName.toLowerCase();
        return java.util.Arrays.stream(suspiciousPatterns)
            .anyMatch(lowerName::contains);
    }
    
    /**
     * ✅ FUNCTIONAL: Create security validation decorator instance
     */
    public static AgentServiceDecorator create(StructuredLoggingService logger) {
        return new SecurityValidationDecorator(logger);
    }
}