package com.trademaster.multibroker.exception;

/**
 * Encryption Exception
 * 
 * MANDATORY: Functional Error Handling + Zero Placeholders + Security Compliance
 * 
 * Specialized exception for encryption and decryption operations. Provides
 * secure error handling for cryptographic operations while preventing
 * information disclosure that could compromise security.
 * 
 * Security Error Types:
 * - Encryption/decryption failures
 * - Key management errors
 * - Algorithm configuration issues
 * - Data integrity violations
 * - Cryptographic parameter errors
 * 
 * Security Features:
 * - No sensitive data exposure in error messages
 * - Safe error codes for troubleshooting
 * - Audit trail correlation
 * - Compliance with security standards
 * - Prevention of timing attacks through error handling
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Cryptographic Error Handling)
 */
public class EncryptionException extends RuntimeException {
    
    private final EncryptionError encryptionError;
    private final String operationId;
    private final String algorithmUsed;
    private final boolean isRecoverable;
    
    /**
     * Encryption Error Types
     */
    public enum EncryptionError {
        KEY_GENERATION_FAILED("Failed to generate encryption key"),
        ENCRYPTION_FAILED("Data encryption operation failed"),
        DECRYPTION_FAILED("Data decryption operation failed"),
        INVALID_KEY_FORMAT("Encryption key format is invalid"),
        KEY_NOT_FOUND("Encryption key not found or inaccessible"),
        ALGORITHM_NOT_SUPPORTED("Cryptographic algorithm not supported"),
        INVALID_INITIALIZATION_VECTOR("Initialization vector is invalid"),
        DATA_INTEGRITY_VIOLATION("Encrypted data integrity check failed"),
        PADDING_ERROR("Invalid padding in encrypted data"),
        KEY_SIZE_INVALID("Encryption key size is invalid for algorithm"),
        CIPHER_CONFIGURATION_ERROR("Cipher configuration error"),
        SECURE_RANDOM_FAILURE("Secure random number generation failed");
        
        private final String description;
        
        EncryptionError(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Create encryption exception
     * 
     * @param encryptionError encryption error type
     * @param operationId operation identifier for tracking
     */
    public EncryptionException(EncryptionError encryptionError, String operationId) {
        super(generateSecureMessage(encryptionError));
        this.encryptionError = encryptionError;
        this.operationId = operationId;
        this.algorithmUsed = null;
        this.isRecoverable = determineRecoverability(encryptionError);
    }
    
    /**
     * Create encryption exception with cause
     * 
     * @param encryptionError encryption error type
     * @param cause underlying cause
     * @param operationId operation identifier for tracking
     */
    public EncryptionException(EncryptionError encryptionError, 
                             Throwable cause, 
                             String operationId) {
        super(generateSecureMessage(encryptionError), cause);
        this.encryptionError = encryptionError;
        this.operationId = operationId;
        this.algorithmUsed = extractAlgorithmFromCause(cause);
        this.isRecoverable = determineRecoverability(encryptionError);
    }
    
    /**
     * Create comprehensive encryption exception
     * 
     * @param encryptionError encryption error type
     * @param cause underlying cause
     * @param operationId operation identifier
     * @param algorithmUsed cryptographic algorithm
     */
    public EncryptionException(EncryptionError encryptionError,
                             Throwable cause,
                             String operationId,
                             String algorithmUsed) {
        super(generateSecureMessage(encryptionError), cause);
        this.encryptionError = encryptionError;
        this.operationId = operationId;
        this.algorithmUsed = algorithmUsed;
        this.isRecoverable = determineRecoverability(encryptionError);
    }
    
    /**
     * Get encryption error type
     * 
     * @return encryption error
     */
    public EncryptionError getEncryptionError() {
        return encryptionError;
    }
    
    /**
     * Get operation identifier
     * 
     * @return operation ID for tracking
     */
    public String getOperationId() {
        return operationId;
    }
    
    /**
     * Get cryptographic algorithm used
     * 
     * @return algorithm name
     */
    public String getAlgorithmUsed() {
        return algorithmUsed;
    }
    
    /**
     * Check if error is recoverable
     * 
     * @return true if operation can be retried
     */
    public boolean isRecoverable() {
        return isRecoverable;
    }
    
    /**
     * Get error severity level
     * 
     * @return severity level
     */
    public String getSeverity() {
        return switch (encryptionError) {
            case DATA_INTEGRITY_VIOLATION, KEY_NOT_FOUND -> "CRITICAL";
            case ENCRYPTION_FAILED, DECRYPTION_FAILED -> "HIGH";
            case KEY_GENERATION_FAILED, ALGORITHM_NOT_SUPPORTED -> "MEDIUM";
            default -> "LOW";
        };
    }
    
    /**
     * Check if error indicates potential security breach
     * 
     * @return true if security may be compromised
     */
    public boolean indicatesSecurityBreach() {
        return encryptionError == EncryptionError.DATA_INTEGRITY_VIOLATION ||
               encryptionError == EncryptionError.KEY_NOT_FOUND ||
               encryptionError == EncryptionError.INVALID_KEY_FORMAT;
    }
    
    /**
     * Get recovery action guidance
     * 
     * @return suggested recovery action
     */
    public String getRecoveryAction() {
        return switch (encryptionError) {
            case KEY_GENERATION_FAILED -> 
                "Check system entropy and cryptographic provider availability";
            case ENCRYPTION_FAILED, DECRYPTION_FAILED -> 
                "Verify data format and retry with fresh encryption context";
            case INVALID_KEY_FORMAT -> 
                "Regenerate encryption keys using proper format";
            case KEY_NOT_FOUND -> 
                "Initialize encryption service and ensure key availability";
            case ALGORITHM_NOT_SUPPORTED -> 
                "Update cryptographic provider or use supported algorithm";
            case INVALID_INITIALIZATION_VECTOR -> 
                "Generate new initialization vector and retry";
            case DATA_INTEGRITY_VIOLATION -> 
                "Data may be corrupted - investigate and restore from backup";
            case PADDING_ERROR -> 
                "Check data format and encryption parameters";
            case KEY_SIZE_INVALID -> 
                "Use proper key size for the selected algorithm";
            case CIPHER_CONFIGURATION_ERROR -> 
                "Review and correct cipher configuration parameters";
            case SECURE_RANDOM_FAILURE -> 
                "Check system random number generator and retry";
        };
    }
    
    /**
     * Generate secure error message that doesn't expose sensitive information
     * 
     * @param encryptionError encryption error type
     * @return secure error message
     */
    private static String generateSecureMessage(EncryptionError encryptionError) {
        // Return generic security-safe messages
        return switch (encryptionError) {
            case KEY_GENERATION_FAILED, KEY_NOT_FOUND, INVALID_KEY_FORMAT -> 
                "Cryptographic key operation failed";
            case ENCRYPTION_FAILED, DECRYPTION_FAILED -> 
                "Cryptographic operation failed";
            case DATA_INTEGRITY_VIOLATION, PADDING_ERROR -> 
                "Data integrity check failed";
            case ALGORITHM_NOT_SUPPORTED, CIPHER_CONFIGURATION_ERROR -> 
                "Cryptographic configuration error";
            case INVALID_INITIALIZATION_VECTOR, KEY_SIZE_INVALID -> 
                "Invalid cryptographic parameters";
            case SECURE_RANDOM_FAILURE -> 
                "Random number generation failed";
        };
    }
    
    /**
     * Extract algorithm information from exception cause
     * 
     * @param cause exception cause
     * @return algorithm name if available
     */
    private static String extractAlgorithmFromCause(Throwable cause) {
        if (cause == null || cause.getMessage() == null) {
            return null;
        }
        
        String message = cause.getMessage();
        
        // Extract common algorithm names from error messages
        if (message.contains("AES")) return "AES";
        if (message.contains("RSA")) return "RSA";
        if (message.contains("GCM")) return "AES-GCM";
        if (message.contains("CBC")) return "AES-CBC";
        if (message.contains("PKCS")) return "PKCS";
        
        return null;
    }
    
    /**
     * Determine if encryption error is recoverable
     * 
     * @param encryptionError encryption error type
     * @return true if recoverable
     */
    private static boolean determineRecoverability(EncryptionError encryptionError) {
        return switch (encryptionError) {
            case KEY_GENERATION_FAILED, SECURE_RANDOM_FAILURE, 
                 CIPHER_CONFIGURATION_ERROR -> true;
            case DATA_INTEGRITY_VIOLATION, INVALID_KEY_FORMAT, 
                 KEY_NOT_FOUND, PADDING_ERROR -> false;
            case ENCRYPTION_FAILED, DECRYPTION_FAILED, 
                 INVALID_INITIALIZATION_VECTOR, KEY_SIZE_INVALID -> true;
            case ALGORITHM_NOT_SUPPORTED -> false;
        };
    }
    
    /**
     * Get error code for programmatic handling
     * 
     * @return error code
     */
    public String getErrorCode() {
        return "ENCRYPTION_" + encryptionError.name();
    }
    
    /**
     * Create audit log entry for this encryption error
     * 
     * @return audit log entry
     */
    public String createAuditLogEntry() {
        StringBuilder audit = new StringBuilder();
        audit.append("Encryption Error: ").append(encryptionError.name());
        audit.append(", Severity: ").append(getSeverity());
        audit.append(", Recoverable: ").append(isRecoverable);
        audit.append(", Security Breach: ").append(indicatesSecurityBreach());
        
        if (operationId != null) {
            audit.append(", Operation ID: ").append(operationId);
        }
        
        if (algorithmUsed != null) {
            audit.append(", Algorithm: ").append(algorithmUsed);
        }
        
        return audit.toString();
    }
    
    /**
     * Check if error requires immediate security response
     * 
     * @return true if immediate security response needed
     */
    public boolean requiresSecurityResponse() {
        return indicatesSecurityBreach() && "CRITICAL".equals(getSeverity());
    }
    
    /**
     * Get recommended escalation level
     * 
     * @return escalation level (NONE, SUPPORT, SECURITY, EMERGENCY)
     */
    public String getEscalationLevel() {
        if (requiresSecurityResponse()) {
            return "EMERGENCY";
        } else if (indicatesSecurityBreach()) {
            return "SECURITY";
        } else if ("HIGH".equals(getSeverity())) {
            return "SUPPORT";
        } else {
            return "NONE";
        }
    }
}