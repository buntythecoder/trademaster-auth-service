package com.trademaster.agentos.security.service;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Authentication Service - Handles JWT token validation and management.
 * Implements Zero Trust authentication with token-based security.
 */
@Slf4j
@Service
public class AuthenticationService {
    
    private final SecretKey signingKey;
    private final long tokenExpiration;
    private final long refreshTokenExpiration;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, SecurityContext> tokenContextCache = new ConcurrentHashMap<>();
    
    public AuthenticationService(
            @Value("${security.jwt.secret:DefaultSecretKeyForDevelopmentOnly12345678901234567890}") String secret,
            @Value("${security.jwt.expiration:3600000}") long tokenExpiration,
            @Value("${security.jwt.refresh-expiration:86400000}") long refreshTokenExpiration) {
        
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenExpiration = tokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
    
    /**
     * Authenticate security context by validating token.
     */
    public Result<SecurityContext, SecurityError> authenticate(SecurityContext context) {
        log.debug("Authenticating context: correlationId={}", context.correlationId());
        
        // Check if token is provided
        if (context.token() == null || context.token().isBlank()) {
            log.warn("No token provided: correlationId={}", context.correlationId());
            return Result.failure(SecurityError.authenticationFailed(
                "No authentication token provided", context.correlationId()));
        }
        
        // Check if token is revoked
        if (revokedTokens.contains(context.token())) {
            log.warn("Token is revoked: correlationId={}", context.correlationId());
            return Result.failure(SecurityError.authenticationFailed(
                "Token has been revoked", context.correlationId()));
        }
        
        // Validate token
        return validateTokenInternal(context.token())
            .map(claims -> enrichContext(context, claims));
    }
    
    /**
     * Validate JWT token and extract claims.
     */
    public Result<SecurityContext, SecurityError> validateToken(String token) {
        log.debug("Validating token");
        
        // Check cache first
        SecurityContext cached = tokenContextCache.get(token);
        if (cached != null) {
            log.debug("Token found in cache: userId={}", cached.userId());
            return Result.success(cached);
        }
        
        return validateTokenInternal(token)
            .flatMap(this::buildContextFromClaims)
            .onSuccess(context -> tokenContextCache.put(token, context));
    }
    
    /**
     * Refresh authentication token.
     */
    public Result<String, SecurityError> refreshToken(SecurityContext context, String refreshToken) {
        log.debug("Refreshing token: correlationId={}", context.correlationId());
        
        // Validate refresh token
        return validateTokenInternal(refreshToken)
            .flatMap(claims -> {
                String tokenType = claims.get("type", String.class);
                if (!"refresh".equals(tokenType)) {
                    return Result.failure(SecurityError.authenticationFailed(
                        "Invalid refresh token", context.correlationId()));
                }
                
                // Generate new access token
                String newToken = generateToken(context);
                
                // Revoke old token
                revokeToken(context.token());
                
                return Result.success(newToken);
            });
    }
    
    /**
     * Extract security context from validated token.
     */
    public Result<SecurityContext, SecurityError> extractContextFromToken(String token) {
        log.debug("Extracting context from token");
        
        return validateTokenInternal(token)
            .flatMap(this::buildContextFromClaims);
    }
    
    /**
     * Revoke a token.
     */
    public void revokeToken(String token) {
        if (token != null && !token.isBlank()) {
            log.debug("Revoking token");
            revokedTokens.add(token);
            tokenContextCache.remove(token);
        }
    }
    
    /**
     * Generate new JWT token for security context.
     */
    public String generateToken(SecurityContext context) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenExpiration);
        
        return Jwts.builder()
            .setSubject(context.userId())
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("sessionId", context.sessionId())
            .claim("roles", context.roles())
            .claim("permissions", context.permissions())
            .claim("securityLevel", context.securityLevel().name())
            .claim("type", "access")
            .signWith(signingKey)
            .compact();
    }
    
    /**
     * Generate refresh token.
     */
    public String generateRefreshToken(SecurityContext context) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);
        
        return Jwts.builder()
            .setSubject(context.userId())
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("sessionId", context.sessionId())
            .claim("type", "refresh")
            .signWith(signingKey)
            .compact();
    }
    
    // Private helper methods
    
    private Result<Claims, SecurityError> validateTokenInternal(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("Token expired: expiry={}", expiration);
                return Result.failure(SecurityError.tokenExpired(UUID.randomUUID().toString()));
            }
            
            return Result.success(claims);
            
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return Result.failure(SecurityError.authenticationFailed(
                "Invalid token: " + e.getMessage(), UUID.randomUUID().toString()));
        }
    }
    
    private SecurityContext enrichContext(SecurityContext context, Claims claims) {
        // Enrich context with token claims
        return SecurityContext.builder()
            .correlationId(context.correlationId())
            .userId(claims.getSubject())
            .sessionId(claims.get("sessionId", String.class))
            .token(context.token())
            .roles(extractRoles(claims))
            .permissions(extractPermissions(claims))
            .attributes(context.attributes())
            .ipAddress(context.ipAddress())
            .userAgent(context.userAgent())
            .timestamp(context.timestamp())
            .securityLevel(extractSecurityLevel(claims))
            .riskScore(context.riskScore())
            .build();
    }
    
    private Result<SecurityContext, SecurityError> buildContextFromClaims(Claims claims) {
        try {
            SecurityContext context = SecurityContext.builder()
                .correlationId(UUID.randomUUID().toString())
                .userId(claims.getSubject())
                .sessionId(claims.get("sessionId", String.class))
                .roles(extractRoles(claims))
                .permissions(extractPermissions(claims))
                .securityLevel(extractSecurityLevel(claims))
                .build();
            
            return Result.success(context);
        } catch (Exception e) {
            log.error("Failed to build context from claims", e);
            return Result.failure(SecurityError.authenticationFailed(
                "Invalid token claims", UUID.randomUUID().toString()));
        }
    }
    
    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get("roles");
        return switch (rolesObj) {
            case null -> Set.of();
            case Collection<?> collection -> collection.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
            case String s -> Set.of(s.split(","));
            default -> Set.of(rolesObj.toString());
        };
    }
    
    @SuppressWarnings("unchecked")
    private Set<String> extractPermissions(Claims claims) {
        Object permsObj = claims.get("permissions");
        return switch (permsObj) {
            case null -> Set.of();
            case Collection<?> collection -> collection.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
            case String s -> Set.of(s.split(","));
            default -> Set.of(permsObj.toString());
        };
    }
    
    private SecurityContext.SecurityLevel extractSecurityLevel(Claims claims) {
        String level = claims.get("securityLevel", String.class);
        return level != null 
            ? SecurityContext.SecurityLevel.valueOf(level)
            : SecurityContext.SecurityLevel.STANDARD;
    }
}