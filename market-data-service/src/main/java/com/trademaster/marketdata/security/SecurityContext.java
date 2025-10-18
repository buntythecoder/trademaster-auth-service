package com.trademaster.marketdata.security;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable Security Context for Zero Trust operations
 *
 * Contains all security-relevant information for access control decisions.
 *
 * @param userId User identifier
 * @param roles User roles for authorization
 * @param ipAddress Request IP address for risk assessment
 * @param requestTime Request timestamp
 * @param metadata Additional security metadata
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record SecurityContext(
    String userId,
    java.util.Set<String> roles,
    String ipAddress,
    Instant requestTime,
    Map<String, String> metadata
) {
    public SecurityContext {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(roles, "Roles cannot be null");
        Objects.requireNonNull(requestTime, "Request time cannot be null");

        // Defensive copies for immutability
        roles = java.util.Set.copyOf(roles);
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Create SecurityContext from Spring Security principal
     */
    public static SecurityContext fromUserDetails(
            org.springframework.security.core.userdetails.UserDetails userDetails,
            String ipAddress) {

        var roles = userDetails.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(java.util.stream.Collectors.toSet());

        return new SecurityContext(
            userDetails.getUsername(),
            roles,
            ipAddress,
            Instant.now(),
            Map.of("authenticated", "true")
        );
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... requiredRoles) {
        return java.util.Arrays.stream(requiredRoles)
            .anyMatch(this::hasRole);
    }
}
