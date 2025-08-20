package com.trademaster.auth.security;

import com.trademaster.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for generating and validating JWT tokens
 * 
 * This class handles:
 * - JWT token generation for authentication and refresh
 * - Token validation and parsing
 * - Device fingerprint validation
 * - Token type detection (access vs refresh)
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${trademaster.jwt.secret}")
    private String jwtSecret;

    @Value("${trademaster.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${trademaster.jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    @Value("${trademaster.jwt.issuer}")
    private String jwtIssuer;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // Ensure the secret key is at least 256 bits for HS256
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters (256 bits) long");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Token Provider initialized with issuer: {}", jwtIssuer);
    }

    /**
     * Generate JWT access token
     */
    public String generateToken(Authentication authentication, String deviceFingerprint, String ipAddress) {
        User user = (User) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("email", user.getEmail())
                .claim("authorities", authorities)
                .claim("subscription_tier", user.getSubscriptionTier().getValue())
                .claim("kyc_status", user.getKycStatus().getValue())
                .claim("device_fingerprint", deviceFingerprint)
                .claim("ip_address", ipAddress)
                .claim("token_type", "access")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate JWT refresh token
     */
    public String generateRefreshToken(User user, String deviceFingerprint) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("email", user.getEmail())
                .claim("device_fingerprint", deviceFingerprint)
                .claim("token_type", "refresh")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get user ID from JWT token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * Get all claims from JWT token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get email from JWT token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Get device fingerprint from JWT token
     */
    public String getDeviceFingerprintFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("device_fingerprint", String.class);
    }

    /**
     * Get token type from JWT token
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("token_type", String.class);
    }

    /**
     * Get authorities from JWT token
     */
    public String getAuthoritiesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("authorities", String.class);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            log.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get token expiration time
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Get remaining time to expiration in milliseconds
     */
    public long getRemainingExpirationTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            return Math.max(0, expiration.getTime() - now.getTime());
        } catch (Exception e) {
            log.error("Error getting remaining expiration time: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Validate device fingerprint in token
     */
    public boolean validateDeviceFingerprint(String token, String currentDeviceFingerprint) {
        try {
            String tokenDeviceFingerprint = getDeviceFingerprintFromToken(token);
            return currentDeviceFingerprint != null && 
                   currentDeviceFingerprint.equals(tokenDeviceFingerprint);
        } catch (Exception e) {
            log.error("Error validating device fingerprint: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract token from Authorization header
     */
    public String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Generate token for system operations
     */
    public String generateSystemToken(String purpose, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject("system")
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("purpose", purpose)
                .claim("token_type", "system")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Getters for configuration values
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public long getJwtRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }
}