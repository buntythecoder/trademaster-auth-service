package com.trademaster.behavioralai.functional;

import java.time.Instant;
import java.util.Map;

/**
 * Behavioral AI Error Hierarchy
 * 
 * Sealed class hierarchy for type-safe error handling in behavioral AI operations.
 * Provides specific error types for different failure scenarios.
 */
public sealed class BehavioralAIError extends Error 
    permits BehavioralAIError.AnalysisError, 
            BehavioralAIError.ModelError,
            BehavioralAIError.ValidationError,
            BehavioralAIError.DataError,
            BehavioralAIError.InterventionError {
    
    private final String code;
    private final String message;
    private final Instant timestamp;
    private final Map<String, Object> context;
    
    protected BehavioralAIError(String code, String message, Map<String, Object> context) {
        super(message);
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
        this.context = context != null ? Map.copyOf(context) : Map.of();
    }
    
    public String getCode() { return code; }
    public String getErrorMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getContext() { return context; }
    
    /**
     * Analysis-related errors
     */
    public static final class AnalysisError extends BehavioralAIError {
        
        public static final String INSUFFICIENT_DATA = "ANALYSIS_001";
        public static final String PROCESSING_TIMEOUT = "ANALYSIS_002";
        public static final String FEATURE_EXTRACTION_FAILED = "ANALYSIS_003";
        public static final String PATTERN_DETECTION_FAILED = "ANALYSIS_004";
        
        private AnalysisError(String code, String message, Map<String, Object> context) {
            super(code, message, context);
        }
        
        public static AnalysisError insufficientData(String userId, String sessionId) {
            return new AnalysisError(
                INSUFFICIENT_DATA,
                "Insufficient trading data for behavioral analysis",
                Map.of("userId", userId, "sessionId", sessionId)
            );
        }
        
        public static AnalysisError processingTimeout(String operation, long timeoutMs) {
            return new AnalysisError(
                PROCESSING_TIMEOUT,
                "Analysis operation timed out after " + timeoutMs + "ms",
                Map.of("operation", operation, "timeoutMs", timeoutMs)
            );
        }
        
        public static AnalysisError featureExtractionFailed(String feature, String reason) {
            return new AnalysisError(
                FEATURE_EXTRACTION_FAILED,
                "Failed to extract feature: " + feature + " - " + reason,
                Map.of("feature", feature, "reason", reason)
            );
        }
        
        public static AnalysisError patternDetectionFailed(String patternType, String reason) {
            return new AnalysisError(
                PATTERN_DETECTION_FAILED,
                "Pattern detection failed for: " + patternType + " - " + reason,
                Map.of("patternType", patternType, "reason", reason)
            );
        }
    }
    
    /**
     * ML Model-related errors
     */
    public static final class ModelError extends BehavioralAIError {
        
        public static final String MODEL_NOT_LOADED = "MODEL_001";
        public static final String INFERENCE_FAILED = "MODEL_002";
        public static final String MODEL_OUTDATED = "MODEL_003";
        public static final String FEATURE_MISMATCH = "MODEL_004";
        
        private ModelError(String code, String message, Map<String, Object> context) {
            super(code, message, context);
        }
        
        public static ModelError modelNotLoaded(String modelId) {
            return new ModelError(
                MODEL_NOT_LOADED,
                "ML model not loaded: " + modelId,
                Map.of("modelId", modelId)
            );
        }
        
        public static ModelError inferenceFailed(String modelId, String error) {
            return new ModelError(
                INFERENCE_FAILED,
                "Model inference failed: " + error,
                Map.of("modelId", modelId, "error", error)
            );
        }
        
        public static ModelError modelOutdated(String modelId, Instant lastUpdate) {
            return new ModelError(
                MODEL_OUTDATED,
                "Model is outdated and needs retraining",
                Map.of("modelId", modelId, "lastUpdate", lastUpdate.toString())
            );
        }
        
        public static ModelError featureMismatch(String expected, String actual) {
            return new ModelError(
                FEATURE_MISMATCH,
                "Feature schema mismatch - expected: " + expected + ", actual: " + actual,
                Map.of("expected", expected, "actual", actual)
            );
        }
    }
    
    /**
     * Validation-related errors
     */
    public static final class ValidationError extends BehavioralAIError {
        
        public static final String INVALID_INPUT = "VALIDATION_001";
        public static final String MISSING_REQUIRED_FIELD = "VALIDATION_002";
        public static final String CONSTRAINT_VIOLATION = "VALIDATION_003";
        public static final String BUSINESS_RULE_VIOLATION = "VALIDATION_004";
        
        private ValidationError(String code, String message, Map<String, Object> context) {
            super(code, message, context);
        }
        
        public static ValidationError invalidInput(String field, String value, String reason) {
            return new ValidationError(
                INVALID_INPUT,
                "Invalid input for field '" + field + "': " + reason,
                Map.of("field", field, "value", value, "reason", reason)
            );
        }
        
        public static ValidationError missingRequiredField(String field) {
            return new ValidationError(
                MISSING_REQUIRED_FIELD,
                "Required field missing: " + field,
                Map.of("field", field)
            );
        }
        
        public static ValidationError constraintViolation(String constraint, String details) {
            return new ValidationError(
                CONSTRAINT_VIOLATION,
                "Constraint violation: " + constraint + " - " + details,
                Map.of("constraint", constraint, "details", details)
            );
        }
        
        public static ValidationError businessRuleViolation(String rule, String details) {
            return new ValidationError(
                BUSINESS_RULE_VIOLATION,
                "Business rule violation: " + rule + " - " + details,
                Map.of("rule", rule, "details", details)
            );
        }
    }
    
    /**
     * Data-related errors
     */
    public static final class DataError extends BehavioralAIError {
        
        public static final String DATA_NOT_FOUND = "DATA_001";
        public static final String DATA_CORRUPTION = "DATA_002";
        public static final String STORAGE_FAILURE = "DATA_003";
        public static final String RETRIEVAL_FAILURE = "DATA_004";
        
        private DataError(String code, String message, Map<String, Object> context) {
            super(code, message, context);
        }
        
        public static DataError dataNotFound(String dataType, String identifier) {
            return new DataError(
                DATA_NOT_FOUND,
                dataType + " not found: " + identifier,
                Map.of("dataType", dataType, "identifier", identifier)
            );
        }
        
        public static DataError dataCorruption(String dataSource, String details) {
            return new DataError(
                DATA_CORRUPTION,
                "Data corruption detected in " + dataSource + ": " + details,
                Map.of("dataSource", dataSource, "details", details)
            );
        }
        
        public static DataError storageFailed(String operation, String reason) {
            return new DataError(
                STORAGE_FAILURE,
                "Storage operation failed: " + operation + " - " + reason,
                Map.of("operation", operation, "reason", reason)
            );
        }
        
        public static DataError retrievalFailed(String query, String reason) {
            return new DataError(
                RETRIEVAL_FAILURE,
                "Data retrieval failed: " + reason,
                Map.of("query", query, "reason", reason)
            );
        }
    }
    
    /**
     * Intervention-related errors
     */
    public static final class InterventionError extends BehavioralAIError {
        
        public static final String INTERVENTION_FAILED = "INTERVENTION_001";
        public static final String RATE_LIMITED = "INTERVENTION_002";
        public static final String USER_OPTED_OUT = "INTERVENTION_003";
        public static final String DELIVERY_FAILED = "INTERVENTION_004";
        
        private InterventionError(String code, String message, Map<String, Object> context) {
            super(code, message, context);
        }
        
        public static InterventionError interventionFailed(String type, String reason) {
            return new InterventionError(
                INTERVENTION_FAILED,
                "Intervention failed: " + type + " - " + reason,
                Map.of("type", type, "reason", reason)
            );
        }
        
        public static InterventionError rateLimited(String userId, int maxInterventions) {
            return new InterventionError(
                RATE_LIMITED,
                "User has exceeded intervention rate limit: " + maxInterventions,
                Map.of("userId", userId, "maxInterventions", maxInterventions)
            );
        }
        
        public static InterventionError userOptedOut(String userId, String interventionType) {
            return new InterventionError(
                USER_OPTED_OUT,
                "User has opted out of interventions: " + interventionType,
                Map.of("userId", userId, "interventionType", interventionType)
            );
        }
        
        public static InterventionError deliveryFailed(String channel, String reason) {
            return new InterventionError(
                DELIVERY_FAILED,
                "Intervention delivery failed via " + channel + ": " + reason,
                Map.of("channel", channel, "reason", reason)
            );
        }
    }
}