package com.trademaster.agentos.service;

import java.util.Map;

/**
 * ✅ INTERFACE SEGREGATION: Security Logging Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only security logging operations
 * - Interface Segregation: Separated from application logging
 * - Dependency Inversion: Abstractions for security logging
 */
public interface ISecurityLogger {
    
    /**
     * ✅ SRP: Log authentication success - single responsibility
     */
    void logAuthenticationSuccess(String userId, String sessionId, String ipAddress, 
                                 String authMethod, long durationMs);
    
    /**
     * ✅ SRP: Log authentication failure - single responsibility
     */
    void logAuthenticationFailure(String attemptedUserId, String ipAddress, String authMethod,
                                 String failureReason, long durationMs);
    
    /**
     * ✅ SRP: Log security incident - single responsibility
     */
    void logSecurityIncident(String incidentType, String severity, String userId, 
                            String ipAddress, Map<String, Object> details);
    
    /**
     * ✅ SRP: Log rate limit violation - single responsibility
     */
    void logRateLimitViolation(String endpoint, String ipAddress, String userId, int requestCount);
    
    /**
     * ✅ SRP: Log token validation - single responsibility
     */
    void logTokenValidation(String tokenType, String userId, boolean valid, String reason);
}