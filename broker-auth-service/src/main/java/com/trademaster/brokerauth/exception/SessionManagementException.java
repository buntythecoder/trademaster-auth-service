package com.trademaster.brokerauth.exception;

import com.trademaster.brokerauth.enums.BrokerType;
import lombok.Getter;

/**
 * Session Management Exception
 * 
 * Custom exception for session management related errors.
 * Includes session context and broker information.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public class SessionManagementException extends RuntimeException {
    
    private final String sessionId;
    private final BrokerType brokerType;
    private final Long userId;
    
    public SessionManagementException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
        this.brokerType = null;
        this.userId = null;
    }
    
    public SessionManagementException(String message, String sessionId, BrokerType brokerType, Long userId) {
        super(message);
        this.sessionId = sessionId;
        this.brokerType = brokerType;
        this.userId = userId;
    }
    
    public SessionManagementException(String message, String sessionId, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
        this.brokerType = null;
        this.userId = null;
    }
    
    public SessionManagementException(String message, String sessionId, BrokerType brokerType, 
                                    Long userId, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
        this.brokerType = brokerType;
        this.userId = userId;
    }
    
    // Static factory methods for common scenarios
    
    public static SessionManagementException sessionNotFound(String sessionId) {
        return new SessionManagementException(
            "Session not found: " + sessionId,
            sessionId
        );
    }
    
    public static SessionManagementException sessionExpired(String sessionId, BrokerType brokerType, Long userId) {
        return new SessionManagementException(
            "Session has expired",
            sessionId,
            brokerType,
            userId
        );
    }
    
    public static SessionManagementException invalidSessionState(String sessionId, String currentState, String expectedState) {
        return new SessionManagementException(
            String.format("Invalid session state: expected %s but found %s", expectedState, currentState),
            sessionId
        );
    }
    
    public static SessionManagementException sessionLimitExceeded(BrokerType brokerType, Long userId, int maxSessions) {
        return new SessionManagementException(
            String.format("Session limit exceeded for %s: maximum %d sessions allowed", brokerType, maxSessions),
            null,
            brokerType,
            userId
        );
    }
    
    public static SessionManagementException concurrentSessionConflict(String sessionId, BrokerType brokerType, Long userId) {
        return new SessionManagementException(
            "Concurrent session conflict detected",
            sessionId,
            brokerType,
            userId
        );
    }
}