package com.trademaster.multibroker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Security Service
 * 
 * MANDATORY: Zero Trust Security + JWT Token Management + Virtual Threads
 * 
 * Handles authentication and authorization for WebSocket connections and
 * API endpoints. Implements JWT token validation, user session management,
 * and security context management.
 * 
 * Security Features:
 * - JWT token generation and validation
 * - User session management
 * - Role-based access control
 * - Token expiration and refresh
 * - Security audit logging
 * 
 * Zero Trust Implementation:
 * - All requests require valid authentication
 * - Token-based stateless authentication
 * - User context validation for every operation
 * - Security logging for audit trails
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Zero Trust Security)
 */
@Slf4j
@Service
public class SecurityService {
    
    @Value("${jwt.secret:MySecretKeyForTradeMasterMultiBrokerServiceThatIsLongEnough}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400}") // 24 hours in seconds
    private long jwtExpirationInSeconds;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * Validate JWT token
     * 
     * MANDATORY: Comprehensive token validation with security logging
     * 
     * @param token JWT token to validate
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Token validation failed: empty or null token");
                return false;
            }
            
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.warn("Token validation failed: token expired");
                return false;
            }
            
            // Validate required claims
            String userId = claims.getSubject();
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Token validation failed: missing user ID");
                return false;
            }
            
            log.debug("Token validation successful for user: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract user ID from JWT token
     * 
     * @param token JWT token
     * @return User ID or null if extraction fails
     */
    public String extractUserIdFromToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.getSubject();
            
        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract user roles from JWT token
     * 
     * @param token JWT token
     * @return Optional list of user roles
     */
    public Optional<String[]> extractUserRolesFromToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return Optional.empty();
            }
            
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            String roles = claims.get("roles", String.class);
            if (roles != null && !roles.trim().isEmpty()) {
                return Optional.of(roles.split(","));
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("Failed to extract roles from token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Generate JWT token for user
     * 
     * @param userId User identifier
     * @param roles User roles
     * @return Generated JWT token
     */
    public String generateToken(String userId, String[] roles) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(jwtExpirationInSeconds);
            
            String rolesString = roles != null ? String.join(",", roles) : "";
            
            return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("roles", rolesString)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
                
        } catch (Exception e) {
            log.error("Failed to generate token for user: {}", userId, e);
            throw new SecurityException("Token generation failed", e);
        }
    }
    
    /**
     * Check if user has required role
     * 
     * @param token JWT token
     * @param requiredRole Required role
     * @return true if user has the required role
     */
    public boolean hasRole(String token, String requiredRole) {
        Optional<String[]> roles = extractUserRolesFromToken(token);
        
        if (roles.isEmpty()) {
            return false;
        }
        
        for (String role : roles.get()) {
            if (requiredRole.equalsIgnoreCase(role.trim())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get token expiration time
     * 
     * @param token JWT token
     * @return Optional expiration instant
     */
    public Optional<Instant> getTokenExpiration(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return Optional.empty();
            }
            
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            Date expiration = claims.getExpiration();
            return Optional.of(expiration.toInstant());
            
        } catch (Exception e) {
            log.warn("Failed to get token expiration: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Check if token is about to expire
     * 
     * @param token JWT token
     * @param minutesThreshold Minutes before expiration to consider "about to expire"
     * @return true if token expires within the threshold
     */
    public boolean isTokenAboutToExpire(String token, long minutesThreshold) {
        Optional<Instant> expiration = getTokenExpiration(token);
        
        if (expiration.isEmpty()) {
            return true; // Consider invalid tokens as expired
        }
        
        Instant threshold = Instant.now().plusSeconds(minutesThreshold * 60);
        return expiration.get().isBefore(threshold);
    }
    
    /**
     * Create security context from token
     * 
     * @param token JWT token
     * @return Optional security context
     */
    public Optional<SecurityContext> createSecurityContext(String token) {
        if (!validateToken(token)) {
            return Optional.empty();
        }
        
        String userId = extractUserIdFromToken(token);
        Optional<String[]> roles = extractUserRolesFromToken(token);
        
        if (userId == null) {
            return Optional.empty();
        }
        
        return Optional.of(SecurityContext.builder()
            .userId(userId)
            .token(token)
            .roles(roles.orElse(new String[0]))
            .authenticatedAt(Instant.now())
            .build());
    }
    
    /**
     * Security Context Record
     */
    @lombok.Builder
    public record SecurityContext(
        String userId,
        String token,
        String[] roles,
        Instant authenticatedAt
    ) {
        
        /**
         * Check if user has specific role
         * 
         * @param role Role to check
         * @return true if user has the role
         */
        public boolean hasRole(String role) {
            if (roles == null || role == null) {
                return false;
            }
            
            for (String userRole : roles) {
                if (role.equalsIgnoreCase(userRole.trim())) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * Get formatted roles string
         * 
         * @return Comma-separated roles
         */
        public String getRolesAsString() {
            return roles != null ? String.join(",", roles) : "";
        }
    }
}