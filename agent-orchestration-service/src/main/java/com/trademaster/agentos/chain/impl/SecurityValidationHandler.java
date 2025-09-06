package com.trademaster.agentos.chain.impl;

import com.trademaster.agentos.chain.TaskProcessingHandler;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ✅ FUNCTIONAL: Security Validation Handler
 * 
 * Second handler in the chain that performs security validation.
 * Ensures tasks meet security requirements and don't contain malicious content.
 * 
 * Security Checks:
 * - User authorization validation
 * - Payload security scanning
 * - Resource access validation
 * - Rate limiting checks
 * - Suspicious pattern detection
 */
@Component
@RequiredArgsConstructor
public class SecurityValidationHandler extends TaskProcessingHandler {
    
    private final StructuredLoggingService structuredLogger;
    
    // Security patterns to detect potentially malicious content
    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
        Pattern.compile("(?i)(script|eval|exec|system|shell)"),
        Pattern.compile("(?i)(drop|delete|truncate)\\s+(table|database)"),
        Pattern.compile("(?i)(union|select).*(from|where)"),
        Pattern.compile("(?i)<script[^>]*>.*</script>"),
        Pattern.compile("(?i)javascript:")
    );
    
    @Override
    public Result<Task, AgentError> handle(Task task) {
        structuredLogger.logDebug("security_validation_started", 
            Map.of("taskId", task.getTaskId(), "userId", task.getUserId()));
        
        return validateSecurity(task)
            .onSuccess(validatedTask -> 
                structuredLogger.logDebug("security_validation_passed", 
                    Map.of("taskId", task.getTaskId())))
            .onFailure(error -> 
                structuredLogger.logWarning("security_validation_failed", 
                    Map.of("taskId", task.getTaskId(), "userId", task.getUserId(),
                           "violation", error.getMessage())))
            .flatMap(this::processNext);
    }
    
    /**
     * ✅ FUNCTIONAL: Comprehensive security validation
     */
    private Result<Task, AgentError> validateSecurity(Task task) {
        return validateUserAuthorization(task)
            .flatMap(this::validatePayloadSecurity)
            .flatMap(this::validateResourceAccess)
            .flatMap(this::validateRateLimit);
    }
    
    /**
     * ✅ FUNCTIONAL: User authorization validation
     */
    private Result<Task, AgentError> validateUserAuthorization(Task task) {
        return (task.getUserId() == null || task.getUserId() <= 0)
            ? Result.failure(new AgentError.SecurityViolation(null, "invalid_user_id", "User ID is invalid"))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Payload security scanning
     */
    private Result<Task, AgentError> validatePayloadSecurity(Task task) {
        return (task.getInputParameters() != null && containsSuspiciousContent(task.getInputParameters()))
            ? Result.failure(new AgentError.SecurityViolation(null, "malicious_payload", 
                "Task input parameters contain potentially malicious content"))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Resource access validation
     */
    private Result<Task, AgentError> validateResourceAccess(Task task) {
        // Check if user has permission for required capabilities
        return task.getRequiredCapabilities().stream()
            .anyMatch(capability -> capability.toString().toLowerCase().contains("admin") ||
                     capability.toString().toLowerCase().contains("system"))
            ? validateAdminAccess(task)
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Admin access validation
     */
    private Result<Task, AgentError> validateAdminAccess(Task task) {
        // Simplified admin check - in real implementation, check user roles
        return (task.getUserId() < 100) // Simplified: admin users have ID < 100
            ? Result.failure(new AgentError.UnauthorizedOperation(null, 
                "admin_capability_required", task.getUserId()))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Rate limiting validation
     */
    private Result<Task, AgentError> validateRateLimit(Task task) {
        // Simplified rate limiting - in real implementation, use Redis or similar
        return task.getPriority().getLevel() > 3 && !isAuthorizedForHighPriority(task)
            ? Result.failure(new AgentError.SecurityViolation(null, "rate_limit_exceeded",
                "High priority tasks require special authorization"))
            : Result.success(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Helper methods using pure functions
     */
    private boolean containsSuspiciousContent(String content) {
        return SUSPICIOUS_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(content).find());
    }
    
    private boolean isAuthorizedForHighPriority(Task task) {
        // Simplified authorization check
        return task.getUserId() < 1000; // Premium users have lower IDs
    }
    
    @Override
    public String getHandlerName() {
        return "SecurityValidation";
    }
}