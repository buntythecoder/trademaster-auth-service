package com.trademaster.portfolio.agentos;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP Response Structure (AgentOS Integration)
 * 
 * Standardized response format for Multi-Agent Communication Protocol.
 * Implements immutable Record pattern for type safety and performance.
 * 
 * Features:
 * - Immutable Record structure for thread safety
 * - Success/Error state management
 * - Standardized metadata fields
 * - Builder pattern for complex responses
 * - Correlation ID support for tracing
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - AgentOS Compliance)
 */
public sealed interface MCPResponse 
    permits MCPResponse.Success, MCPResponse.Error {
    
    String endpoint();
    LocalDateTime timestamp();
    String correlationId();
    
    /**
     * Success response with data payload
     */
    record Success(
        String endpoint,
        Map<String, Object> data,
        LocalDateTime timestamp,
        String correlationId,
        String status
    ) implements MCPResponse {
        
        public Success {
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalArgumentException("Endpoint cannot be null or blank");
            }
            if (data == null) {
                throw new IllegalArgumentException("Data cannot be null");
            }
            
            // Set defaults
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
            correlationId = correlationId != null ? correlationId : generateCorrelationId();
            status = status != null ? status : "SUCCESS";
        }
        
        /**
         * Check if response contains specific data key
         */
        public boolean hasData(String key) {
            return data.containsKey(key);
        }
        
        /**
         * Get typed data value
         */
        @SuppressWarnings("unchecked")
        public <T> T getData(String key, Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }
        
        /**
         * Get data value with default
         */
        @SuppressWarnings("unchecked")
        public <T> T getData(String key, T defaultValue) {
            Object value = data.get(key);
            if (value != null && defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }
    
    /**
     * Error response with error details
     */
    record Error(
        String endpoint,
        String errorMessage,
        String errorCode,
        LocalDateTime timestamp,
        String correlationId,
        String status,
        Map<String, Object> errorDetails
    ) implements MCPResponse {
        
        public Error {
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalArgumentException("Endpoint cannot be null or blank");
            }
            if (errorMessage == null || errorMessage.isBlank()) {
                throw new IllegalArgumentException("Error message cannot be null or blank");
            }
            
            // Set defaults
            errorCode = errorCode != null ? errorCode : "UNKNOWN_ERROR";
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
            correlationId = correlationId != null ? correlationId : generateCorrelationId();
            status = status != null ? status : "ERROR";
            errorDetails = errorDetails != null ? errorDetails : Map.of();
        }
        
        /**
         * Check if error is of specific type
         */
        public boolean isErrorType(String type) {
            return errorCode.equals(type);
        }
        
        /**
         * Get error detail value
         */
        @SuppressWarnings("unchecked")
        public <T> T getErrorDetail(String key, Class<T> type) {
            Object value = errorDetails.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }
    }
    
    /**
     * Create success response
     */
    static Success success(String endpoint, Map<String, Object> data) {
        return new Success(endpoint, data, null, null, null);
    }
    
    /**
     * Create success response with correlation ID
     */
    static Success success(String endpoint, Map<String, Object> data, String correlationId) {
        return new Success(endpoint, data, null, correlationId, null);
    }
    
    /**
     * Create error response
     */
    static Error error(String endpoint, String errorMessage) {
        return new Error(endpoint, errorMessage, null, null, null, null, null);
    }
    
    /**
     * Create error response with error code
     */
    static Error error(String endpoint, String errorMessage, String errorCode) {
        return new Error(endpoint, errorMessage, errorCode, null, null, null, null);
    }
    
    /**
     * Create error response with details
     */
    static Error error(String endpoint, String errorMessage, String errorCode, 
                      Map<String, Object> errorDetails) {
        return new Error(endpoint, errorMessage, errorCode, null, null, null, errorDetails);
    }
    
    /**
     * Create error response with correlation ID
     */
    static Error errorWithCorrelationId(String endpoint, String errorMessage, String correlationId) {
        return new Error(endpoint, errorMessage, null, null, correlationId, null, null);
    }
    
    /**
     * Check if response is successful
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    /**
     * Check if response is error
     */
    default boolean isError() {
        return this instanceof Error;
    }
    
    /**
     * Get response as Success (throws if Error)
     */
    default Success asSuccess() {
        if (this instanceof Success success) {
            return success;
        }
        throw new IllegalStateException("Response is not a success: " + this);
    }
    
    /**
     * Get response as Error (throws if Success)
     */
    default Error asError() {
        if (this instanceof Error error) {
            return error;
        }
        throw new IllegalStateException("Response is not an error: " + this);
    }
    
    /**
     * Create builder for complex responses
     */
    static Builder builder(String endpoint) {
        return new Builder(endpoint);
    }
    
    /**
     * Builder for complex MCP responses
     */
    class Builder {
        private final String endpoint;
        private Map<String, Object> data;
        private String errorMessage;
        private String errorCode;
        private String correlationId;
        private String status;
        private Map<String, Object> errorDetails;
        
        private Builder(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }
        
        public Builder error(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder errorDetails(Map<String, Object> errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }
        
        public MCPResponse build() {
            if (errorMessage != null) {
                return new Error(endpoint, errorMessage, errorCode, null, correlationId, status, errorDetails);
            } else if (data != null) {
                return new Success(endpoint, data, null, correlationId, status);
            } else {
                throw new IllegalStateException("Either data or errorMessage must be provided");
            }
        }
    }
    
    /**
     * Generate unique correlation ID for tracing
     */
    private static String generateCorrelationId() {
        return "mcp-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
}