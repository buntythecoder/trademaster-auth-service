package com.trademaster.brokerauth.exception;

import com.trademaster.brokerauth.enums.BrokerType;
import lombok.Getter;

/**
 * Credential Management Exception
 * 
 * Custom exception for credential management related errors.
 * Includes credential context and security considerations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public class CredentialManagementException extends RuntimeException {
    
    private final BrokerType brokerType;
    private final Long userId;
    private final String credentialType;
    private final String operation;
    
    public CredentialManagementException(String message, String operation) {
        super(message);
        this.brokerType = null;
        this.userId = null;
        this.credentialType = null;
        this.operation = operation;
    }
    
    public CredentialManagementException(String message, BrokerType brokerType, Long userId, 
                                       String credentialType, String operation) {
        super(message);
        this.brokerType = brokerType;
        this.userId = userId;
        this.credentialType = credentialType;
        this.operation = operation;
    }
    
    public CredentialManagementException(String message, BrokerType brokerType, Long userId, 
                                       String credentialType, String operation, Throwable cause) {
        super(message, cause);
        this.brokerType = brokerType;
        this.userId = userId;
        this.credentialType = credentialType;
        this.operation = operation;
    }
    
    // Static factory methods for common scenarios
    
    public static CredentialManagementException credentialNotFound(BrokerType brokerType, Long userId) {
        return new CredentialManagementException(
            String.format("Credentials not found for %s", brokerType),
            brokerType,
            userId,
            "BROKER_CREDENTIALS",
            "READ"
        );
    }
    
    public static CredentialManagementException encryptionFailed(String operation) {
        return new CredentialManagementException(
            "Failed to encrypt credential data",
            null,
            null,
            "ENCRYPTED_DATA",
            operation
        );
    }
    
    public static CredentialManagementException decryptionFailed(String operation) {
        return new CredentialManagementException(
            "Failed to decrypt credential data",
            null,
            null,
            "ENCRYPTED_DATA",
            operation
        );
    }
    
    public static CredentialManagementException invalidCredentialFormat(BrokerType brokerType, String credentialType) {
        return new CredentialManagementException(
            String.format("Invalid %s format for %s", credentialType, brokerType),
            brokerType,
            null,
            credentialType,
            "VALIDATION"
        );
    }
    
    public static CredentialManagementException credentialExpired(BrokerType brokerType, Long userId, String credentialType) {
        return new CredentialManagementException(
            String.format("%s has expired for %s", credentialType, brokerType),
            brokerType,
            userId,
            credentialType,
            "VALIDATION"
        );
    }
    
    public static CredentialManagementException credentialStorageFailed(BrokerType brokerType, Long userId, String operation) {
        return new CredentialManagementException(
            String.format("Failed to store credentials for %s", brokerType),
            brokerType,
            userId,
            "BROKER_CREDENTIALS",
            operation
        );
    }
    
    public static CredentialManagementException keyManagementError(String operation, Throwable cause) {
        return new CredentialManagementException(
            "Encryption key management error",
            null,
            null,
            "ENCRYPTION_KEY",
            operation,
            cause
        );
    }
    
    public static CredentialManagementException insecureCredentialDetected(BrokerType brokerType, String credentialType) {
        return new CredentialManagementException(
            String.format("Insecure %s detected for %s", credentialType, brokerType),
            brokerType,
            null,
            credentialType,
            "SECURITY_VALIDATION"
        );
    }
}