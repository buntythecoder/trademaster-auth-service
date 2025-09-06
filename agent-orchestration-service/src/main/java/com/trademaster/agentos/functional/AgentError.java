package com.trademaster.agentos.functional;

/**
 * ✅ MANDATORY: Domain-Specific Error Types for Agent OS
 * 
 * Sealed hierarchy of errors specific to agent operations.
 * Enables functional error handling with type safety and comprehensive error reporting.
 * 
 * Features:
 * - Sealed interface hierarchy for exhaustive pattern matching
 * - Rich error context with relevant data
 * - Functional composition with Result and ValidationResult
 * - Zero exception-based control flow
 */
public sealed interface AgentError permits 
    AgentError.NotFound,
    AgentError.NotFoundByName,
    AgentError.InvalidState,
    AgentError.ValidationFailed,
    AgentError.ValidationError,
    AgentError.MultipleValidationsFailed,
    AgentError.CapabilityMismatch,
    AgentError.NoAvailableAgent,
    AgentError.ResourceExhausted,
    AgentError.ResourceError,
    AgentError.AgentOverloaded,
    AgentError.CommunicationFailed,
    AgentError.HeartbeatTimeout,
    AgentError.SecurityViolation,
    AgentError.UnauthorizedOperation,
    AgentError.ConfigurationError,
    AgentError.PersistenceError,
    AgentError.CreationError,
    AgentError.BatchCreationError,
    AgentError.InvalidStateTransition,
    AgentError.StateValidationFailed,
    AgentError.StrategyNotFound,
    AgentError.SelectionError,
    AgentError.CommandExecutionError,
    AgentError.EventPublishingError,
    AgentError.SystemError,
    AgentError.TimeoutError {
    
    /**
     * Get error message
     */
    String getMessage();
    
    /**
     * Get error code for API responses
     */
    String getErrorCode();
    
    /**
     * Get severity level
     */
    Severity getSeverity();
    
    /**
     * Check if error is retryable
     */
    default boolean isRetryable() {
        return getSeverity() != Severity.CRITICAL;
    }
    
    enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    // ✅ AGENT NOT FOUND ERRORS
    
    record NotFound(Long agentId, String context) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent not found: ID=" + agentId + 
                   (context != null ? " (Context: " + context + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_NOT_FOUND";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    record NotFoundByName(String agentName, String context) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent not found: Name=" + agentName + 
                   (context != null ? " (Context: " + context + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_NOT_FOUND_BY_NAME";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    // ✅ INVALID STATE ERRORS
    
    record InvalidState(Long agentId, String currentState, String expectedState, String operation) implements AgentError {
        @Override
        public String getMessage() {
            return "Invalid agent state for operation '" + operation + "': " +
                   "Agent ID=" + agentId + ", Current=" + currentState + ", Expected=" + expectedState;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_INVALID_STATE";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    // ✅ VALIDATION FAILED ERRORS
    
    record ValidationFailed(String field, String value, String reason) implements AgentError {
        @Override
        public String getMessage() {
            return "Validation failed for field '" + field + "': " + reason + 
                   (value != null ? " (Value: " + value + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_VALIDATION_FAILED";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    record ValidationError(String field, String message) implements AgentError {
        @Override
        public String getMessage() {
            return field != null ? field + ": " + message : message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_VALIDATION_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    record MultipleValidationsFailed(java.util.List<String> validationErrors) implements AgentError {
        @Override
        public String getMessage() {
            return "Multiple validation failures: " + String.join("; ", validationErrors);
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_MULTIPLE_VALIDATIONS_FAILED";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    // ✅ CAPABILITY MISMATCH ERRORS
    
    record CapabilityMismatch(Long agentId, java.util.List<String> requiredCapabilities, 
                             java.util.List<String> agentCapabilities) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent capabilities mismatch: Agent ID=" + agentId + 
                   ", Required=" + requiredCapabilities + 
                   ", Available=" + agentCapabilities;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_CAPABILITY_MISMATCH";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record NoAvailableAgent(String agentType, java.util.List<String> requiredCapabilities) implements AgentError {
        @Override
        public String getMessage() {
            return "No available agent found: Type=" + agentType + 
                   ", Required capabilities=" + requiredCapabilities;
        }
        
        @Override
        public String getErrorCode() {
            return "NO_AVAILABLE_AGENT";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    // ✅ RESOURCE EXHAUSTED ERRORS
    
    record ResourceExhausted(String resourceType, Long currentUsage, Long maxCapacity) implements AgentError {
        @Override
        public String getMessage() {
            return "Resource exhausted: " + resourceType + 
                   " (Current: " + currentUsage + "/" + maxCapacity + ")";
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_RESOURCE_EXHAUSTED";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.CRITICAL;
        }
    }
    
    record ResourceError(String message) implements AgentError {
        @Override
        public String getMessage() {
            return "Resource error: " + message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_RESOURCE_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record AgentOverloaded(Long agentId, Integer currentLoad, Integer maxCapacity) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent overloaded: Agent ID=" + agentId + 
                   " (Load: " + currentLoad + "/" + maxCapacity + ")";
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_OVERLOADED";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    // ✅ COMMUNICATION FAILED ERRORS
    
    record CommunicationFailed(Long agentId, String operation, String reason) implements AgentError {
        @Override
        public String getMessage() {
            return "Communication failed with agent: Agent ID=" + agentId + 
                   ", Operation=" + operation + ", Reason=" + reason;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_COMMUNICATION_FAILED";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record HeartbeatTimeout(Long agentId, java.time.Duration timeout) implements AgentError {
        @Override
        public String getMessage() {
            return "Heartbeat timeout for agent: Agent ID=" + agentId + 
                   ", Timeout=" + timeout.toSeconds() + "s";
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_HEARTBEAT_TIMEOUT";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    // ✅ SECURITY VIOLATION ERRORS
    
    record SecurityViolation(Long agentId, String violation, String context) implements AgentError {
        @Override
        public String getMessage() {
            return "Security violation: Agent ID=" + agentId + 
                   ", Violation=" + violation + 
                   (context != null ? " (Context: " + context + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_SECURITY_VIOLATION";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.CRITICAL;
        }
    }
    
    record UnauthorizedOperation(Long agentId, String operation, Long userId) implements AgentError {
        @Override
        public String getMessage() {
            return "Unauthorized operation: Agent ID=" + agentId + 
                   ", Operation=" + operation + ", User ID=" + userId;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_UNAUTHORIZED_OPERATION";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.CRITICAL;
        }
    }
    
    // ✅ CONFIGURATION ERROR
    
    record ConfigurationError(String component, String parameter, String reason) implements AgentError {
        @Override
        public String getMessage() {
            return "Configuration error in " + component + 
                   ": Parameter=" + parameter + ", Reason=" + reason;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_CONFIGURATION_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    // ✅ UTILITY METHODS
    
    /**
     * Convert validation errors to AgentError
     */
    static AgentError fromValidation(java.util.List<String> validationErrors) {
        return validationErrors.size() == 1 
            ? new ValidationFailed("", "", validationErrors.get(0))
            : new MultipleValidationsFailed(validationErrors);
    }
    
    /**
     * Convert ValidationResult to Result<T, AgentError>
     */
    static <T> Result<T, AgentError> fromValidationResult(ValidationResult<T> validation) {
        return switch (validation) {
            case ValidationResult.Valid(var value) -> Result.success(value);
            case ValidationResult.Invalid(var errors) -> Result.failure(fromValidation(errors));
        };
    }
    
    // ✅ PERSISTENCE ERROR
    
    record PersistenceError(String message, Exception cause) implements AgentError {
        @Override
        public String getMessage() {
            return "Persistence error: " + message + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_PERSISTENCE_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    /**
     * Create a simple error with message
     */
    static AgentError simple(String message) {
        return new ValidationFailed("", "", message);
    }
    
    /**
     * Create unsupported operation error
     */
    static AgentError unsupportedOperation(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create validation error
     */
    static AgentError validationError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create resource error
     */
    static AgentError resourceError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create quality error
     */
    static AgentError qualityError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create system error
     */
    static AgentError systemError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create business error
     */
    static AgentError businessError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create compliance error
     */
    static AgentError complianceError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create timeout error
     */
    static AgentError timeoutError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    /**
     * Create calculation error
     */
    static AgentError calculationError(String code, String message) {
        return new ValidationError(code, message);
    }
    
    // ✅ DESIGN PATTERN SPECIFIC ERRORS
    
    record CreationError(String message, Throwable cause) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent creation error: " + message + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_CREATION_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record BatchCreationError(String message) implements AgentError {
        @Override
        public String getMessage() {
            return "Batch agent creation error: " + message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_BATCH_CREATION_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record InvalidStateTransition(Long agentId, com.trademaster.agentos.domain.entity.AgentStatus fromState, com.trademaster.agentos.domain.entity.AgentStatus toState) implements AgentError {
        @Override
        public String getMessage() {
            return "Invalid state transition for agent: " + agentId + 
                   " from " + fromState + " to " + toState;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_INVALID_STATE_TRANSITION";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record StateValidationFailed(Long agentId, com.trademaster.agentos.domain.entity.AgentStatus targetState, String reason) implements AgentError {
        @Override
        public String getMessage() {
            return "State validation failed for agent: " + agentId + 
                   ", target state: " + targetState + ", reason: " + reason;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_STATE_VALIDATION_FAILED";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    record StrategyNotFound(String message) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent selection strategy not found: " + message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_STRATEGY_NOT_FOUND";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record SelectionError(String message) implements AgentError {
        @Override
        public String getMessage() {
            return "Agent selection error: " + message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_SELECTION_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record CommandExecutionError(String message, Throwable cause) implements AgentError {
        @Override
        public String getMessage() {
            return "Command execution error: " + message + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_COMMAND_EXECUTION_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record EventPublishingError(String message, Throwable cause) implements AgentError {
        @Override
        public String getMessage() {
            return "Event publishing error: " + message + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "");
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_EVENT_PUBLISHING_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.MEDIUM;
        }
    }
    
    record SystemError(String message) implements AgentError {
        @Override
        public String getMessage() {
            return "System error: " + message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_SYSTEM_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
    
    record TimeoutError(String message) implements AgentError {
        @Override
        public String getMessage() {
            return "Timeout error: " + message;
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_TIMEOUT_ERROR";
        }
        
        @Override
        public Severity getSeverity() {
            return Severity.HIGH;
        }
    }
}