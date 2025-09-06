package com.trademaster.agentos.functional;

import java.util.Map;

/**
 * ✅ FUNCTIONAL ERROR HANDLING: MCP Protocol error types
 * 
 * Sealed interface hierarchy for MCP operations following Rule #11
 * - No exceptions in business logic
 * - Railway Oriented Programming
 * - Type-safe error handling for MCP protocol
 */
public sealed interface MCPError permits
    MCPError.ResourceNotFoundError,
    MCPError.ToolNotFoundError,
    MCPError.SerializationError,
    MCPError.DeserializationError,
    MCPError.ProtocolError,
    MCPError.ValidationError,
    MCPError.ExecutionError,
    MCPError.NetworkError,
    MCPError.TimeoutError,
    MCPError.TradeOrderError {
    
    /**
     * Get error message for logging/debugging
     */
    String getMessage();
    
    /**
     * Get error code for classification
     */
    String getErrorCode();
    
    /**
     * Get severity level
     */
    default String getSeverity() {
        return "ERROR";
    }
    
    /**
     * MCP resource not found
     */
    record ResourceNotFoundError(String uri, String context) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP resource not found: %s (context: %s)", uri, context);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_RESOURCE_NOT_FOUND";
        }
    }
    
    /**
     * MCP tool not found
     */
    record ToolNotFoundError(String toolName, String context) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP tool not found: %s (context: %s)", toolName, context);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_TOOL_NOT_FOUND";
        }
    }
    
    /**
     * JSON serialization failed in MCP context
     */
    record SerializationError(String objectType, String details, Exception cause) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP serialization failed for %s: %s", objectType, details);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_SERIALIZATION_ERROR";
        }
    }
    
    /**
     * JSON deserialization failed in MCP context
     */
    record DeserializationError(String objectType, String details, Exception cause) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP deserialization failed for %s: %s", objectType, details);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_DESERIALIZATION_ERROR";
        }
    }
    
    /**
     * MCP protocol violation
     */
    record ProtocolError(String operation, String reason, Map<String, Object> context) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP protocol error in %s: %s", operation, reason);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_PROTOCOL_ERROR";
        }
    }
    
    /**
     * MCP request/response validation failed
     */
    record ValidationError(String field, String value, String requirement) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP validation failed for %s='%s': %s", field, value, requirement);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_VALIDATION_ERROR";
        }
    }
    
    /**
     * MCP tool execution failed
     */
    record ExecutionError(String toolName, String reason, Object parameters, Exception cause) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP tool %s execution failed: %s", toolName, reason);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_EXECUTION_ERROR";
        }
        
        @Override
        public String getSeverity() {
            return "CRITICAL";
        }
    }
    
    /**
     * Network communication error
     */
    record NetworkError(String endpoint, String operation, String details, Exception cause) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP network error for %s %s: %s", operation, endpoint, details);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_NETWORK_ERROR";
        }
    }
    
    /**
     * Operation timed out
     */
    record TimeoutError(String operation, long timeoutMs, long actualMs) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("MCP %s timed out: %dms (limit: %dms)", operation, actualMs, timeoutMs);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_TIMEOUT_ERROR";
        }
    }
    
    /**
     * Trade order specific error
     */
    record TradeOrderError(String orderId, String symbol, String reason, Map<String, Object> orderDetails) implements MCPError {
        @Override
        public String getMessage() {
            return String.format("Trade order %s for %s failed: %s", orderId, symbol, reason);
        }
        
        @Override
        public String getErrorCode() {
            return "MCP_TRADE_ORDER_ERROR";
        }
        
        @Override
        public String getSeverity() {
            return "CRITICAL";
        }
    }
    
    // Factory methods for common error creation patterns
    
    /**
     * Create resource not found error
     */
    static MCPError resourceNotFound(String uri, String context) {
        return new ResourceNotFoundError(uri, context);
    }
    
    /**
     * Create tool not found error
     */
    static MCPError toolNotFound(String toolName, String context) {
        return new ToolNotFoundError(toolName, context);
    }
    
    /**
     * Create serialization error from exception
     */
    static MCPError serializationError(String objectType, Exception cause) {
        return new SerializationError(objectType, cause.getMessage(), cause);
    }
    
    /**
     * Create deserialization error from exception
     */
    static MCPError deserializationError(String objectType, Exception cause) {
        return new DeserializationError(objectType, cause.getMessage(), cause);
    }
    
    /**
     * Create protocol error
     */
    static MCPError protocolError(String operation, String reason) {
        return new ProtocolError(operation, reason, Map.of());
    }
    
    /**
     * Create protocol error with context
     */
    static MCPError protocolError(String operation, String reason, Map<String, Object> context) {
        return new ProtocolError(operation, reason, context);
    }
    
    /**
     * Create validation error
     */
    static MCPError validationError(String field, String value, String requirement) {
        return new ValidationError(field, value, requirement);
    }
    
    /**
     * Create execution error from exception
     */
    static MCPError executionError(String toolName, String reason, Object parameters, Exception cause) {
        return new ExecutionError(toolName, reason, parameters, cause);
    }
    
    /**
     * Create network error from exception
     */
    static MCPError networkError(String endpoint, String operation, Exception cause) {
        return new NetworkError(endpoint, operation, cause.getMessage(), cause);
    }
    
    /**
     * Create trade order error
     */
    static MCPError tradeOrderError(String orderId, String symbol, String reason, Map<String, Object> orderDetails) {
        return new TradeOrderError(orderId, symbol, reason, orderDetails);
    }
}