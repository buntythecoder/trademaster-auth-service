package com.trademaster.behavioralai.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Security Context Record
 * 
 * Immutable security context containing authentication and authorization information.
 */
public record SecurityContext(
    String userId,
    String endpoint,
    String httpMethod,
    ClientInfo clientInfo,
    AuthenticationInfo authInfo,
    List<String> requiredPermissions,
    Map<String, Object> requestMetadata,
    Instant requestTime
) {
    
    public SecurityContext {
        // Validate required fields
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint cannot be null or blank");
        }
        if (requestTime == null) {
            throw new IllegalArgumentException("requestTime cannot be null");
        }
        
        // Ensure immutability
        requiredPermissions = requiredPermissions != null ? List.copyOf(requiredPermissions) : List.of();
        requestMetadata = requestMetadata != null ? Map.copyOf(requestMetadata) : Map.of();
    }
    
    public static SecurityContext create(String userId, String endpoint, String httpMethod) {
        return new SecurityContext(
            userId,
            endpoint, 
            httpMethod,
            null, // client info set by security components
            null, // auth info set during authentication
            List.of(),
            Map.of(),
            Instant.now()
        );
    }
    
    public SecurityContext withAuthentication(AuthenticationInfo authInfo) {
        return new SecurityContext(
            this.userId, this.endpoint, this.httpMethod, this.clientInfo,
            authInfo, this.requiredPermissions, this.requestMetadata, this.requestTime
        );
    }
    
    public SecurityContext withClientInfo(ClientInfo clientInfo) {
        return new SecurityContext(
            this.userId, this.endpoint, this.httpMethod, clientInfo,
            this.authInfo, this.requiredPermissions, this.requestMetadata, this.requestTime
        );
    }
    
    public boolean isAuthenticated() {
        return authInfo != null && authInfo.isValid();
    }
    
    public boolean hasPermission(String permission) {
        return authInfo != null && authInfo.permissions().contains(permission);
    }
    
    public boolean hasAllRequiredPermissions() {
        return authInfo != null && authInfo.permissions().containsAll(requiredPermissions);
    }
    
    public record ClientInfo(
        String ipAddress,
        String userAgent,
        String deviceFingerprint,
        String location,
        Instant firstSeen
    ) {}
    
    public record AuthenticationInfo(
        String token,
        String tokenType,
        List<String> permissions,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt
    ) {
        public boolean isValid() {
            return token != null && !token.isBlank() && 
                   expiresAt != null && expiresAt.isAfter(Instant.now());
        }
        
        public boolean isExpired() {
            return expiresAt == null || expiresAt.isBefore(Instant.now());
        }
    }
}