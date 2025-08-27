package com.trademaster.brokerauth.exception;

import com.trademaster.brokerauth.enums.BrokerType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Broker Authentication Exception
 * 
 * Custom exception for broker authentication related errors.
 * Includes broker context and structured error information.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public class BrokerAuthenticationException extends RuntimeException {
    
    private final String errorCode;
    private final BrokerType brokerType;
    private final Long userId;
    private final HttpStatus httpStatus;
    private final Map<String, Object> additionalData;
    
    public BrokerAuthenticationException(String message, String errorCode, 
                                       BrokerType brokerType, Long userId) {
        super(message);
        this.errorCode = errorCode;
        this.brokerType = brokerType;
        this.userId = userId;
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.additionalData = null;
    }
    
    public BrokerAuthenticationException(String message, String errorCode, 
                                       BrokerType brokerType, Long userId, 
                                       HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.brokerType = brokerType;
        this.userId = userId;
        this.httpStatus = httpStatus;
        this.additionalData = null;
    }
    
    public BrokerAuthenticationException(String message, String errorCode, 
                                       BrokerType brokerType, Long userId, 
                                       HttpStatus httpStatus, Map<String, Object> additionalData) {
        super(message);
        this.errorCode = errorCode;
        this.brokerType = brokerType;
        this.userId = userId;
        this.httpStatus = httpStatus;
        this.additionalData = additionalData;
    }
    
    public BrokerAuthenticationException(String message, String errorCode, 
                                       BrokerType brokerType, Long userId, 
                                       Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.brokerType = brokerType;
        this.userId = userId;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.additionalData = null;
    }
    
    // Static factory methods for common scenarios
    
    public static BrokerAuthenticationException invalidCredentials(BrokerType brokerType, Long userId) {
        return new BrokerAuthenticationException(
            "Invalid credentials provided for broker authentication",
            "INVALID_CREDENTIALS",
            brokerType,
            userId,
            HttpStatus.BAD_REQUEST
        );
    }
    
    public static BrokerAuthenticationException authenticationFailed(BrokerType brokerType, Long userId, String reason) {
        return new BrokerAuthenticationException(
            "Broker authentication failed: " + reason,
            "AUTHENTICATION_FAILED",
            brokerType,
            userId,
            HttpStatus.UNAUTHORIZED
        );
    }
    
    public static BrokerAuthenticationException brokerUnavailable(BrokerType brokerType) {
        return new BrokerAuthenticationException(
            "Broker service is currently unavailable",
            "BROKER_UNAVAILABLE",
            brokerType,
            null,
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }
    
    public static BrokerAuthenticationException sessionExpired(BrokerType brokerType, Long userId, String sessionId) {
        return new BrokerAuthenticationException(
            "Broker session has expired",
            "SESSION_EXPIRED",
            brokerType,
            userId,
            HttpStatus.UNAUTHORIZED,
            Map.of("sessionId", sessionId)
        );
    }
    
    public static BrokerAuthenticationException tokenRefreshFailed(BrokerType brokerType, Long userId) {
        return new BrokerAuthenticationException(
            "Failed to refresh broker tokens",
            "TOKEN_REFRESH_FAILED",
            brokerType,
            userId,
            HttpStatus.UNAUTHORIZED
        );
    }
}