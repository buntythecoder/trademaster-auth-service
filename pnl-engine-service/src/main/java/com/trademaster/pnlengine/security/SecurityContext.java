package com.trademaster.pnlengine.security;

import java.time.Instant;
import java.util.Set;

/**
 * Security Context for Zero Trust Architecture
 * 
 * MANDATORY: Java 24 + Zero Trust Security + Immutable Records
 * 
 * Immutable security context containing all authentication and authorization
 * information required for zero trust security validation.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Security Refactoring)
 */
public record SecurityContext(
    String userId,
    String sessionId,
    String correlationId,
    Set<String> authorities,
    Set<String> scopes,
    String clientIp,
    String userAgent,
    Instant authenticatedAt,
    Instant expiresAt,
    SecurityLevel securityLevel,
    RequestType requestType
) {
    
    public enum SecurityLevel {
        PUBLIC,      // Public endpoints
        AUTHENTICATED, // Requires valid authentication
        PRIVILEGED,    // Requires elevated permissions
        SYSTEM        // Internal system operations
    }
    
    public enum RequestType {
        API_CALL,      // External API request
        INTERNAL,      // Internal service communication
        BACKGROUND,    // Background/scheduled task
        SYSTEM        // System-level operation
    }
    
    /**
     * Create security context for API requests
     */
    public static SecurityContext forApiRequest(String userId, String sessionId, String correlationId,
                                              Set<String> authorities, Set<String> scopes,
                                              String clientIp, String userAgent) {
        return new SecurityContext(
            userId, sessionId, correlationId, authorities, scopes,
            clientIp, userAgent, Instant.now(), Instant.now().plusHours(1),
            SecurityLevel.AUTHENTICATED, RequestType.API_CALL
        );
    }
    
    /**
     * Create security context for internal service calls
     */
    public static SecurityContext forInternalService(String userId, String correlationId) {
        return new SecurityContext(
            userId, "internal-session", correlationId, Set.of("SYSTEM"), Set.of("internal:all"),
            "internal", "service-to-service", Instant.now(), Instant.now().plusMinutes(5),
            SecurityLevel.SYSTEM, RequestType.INTERNAL
        );
    }
    
    /**
     * Create security context for background operations
     */
    public static SecurityContext forBackgroundTask(String correlationId) {
        return new SecurityContext(
            "system", "background-session", correlationId, Set.of("SYSTEM"), Set.of("background:all"),
            "internal", "background-processor", Instant.now(), Instant.now().plusHours(24),
            SecurityLevel.SYSTEM, RequestType.BACKGROUND
        );
    }
    
    /**
     * Check if context has required authority
     */
    public boolean hasAuthority(String authority) {
        return authorities.contains(authority) || authorities.contains("ADMIN");
    }
    
    /**
     * Check if context has required scope
     */
    public boolean hasScope(String scope) {
        return scopes.contains(scope) || scopes.contains("all");
    }
    
    /**
     * Check if context is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if context is valid for operation
     */
    public boolean isValidFor(SecurityLevel requiredLevel) {
        return !isExpired() && securityLevel.ordinal() >= requiredLevel.ordinal();
    }
}