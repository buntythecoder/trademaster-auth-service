package com.trademaster.auth.security;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.SafeOperations;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@Getter
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
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        
        this.secretKey = Optional.of(keyBytes)
            .filter(bytes -> bytes.length >= 32)
            .map(Keys::hmacShaKeyFor)
            .orElseThrow(() -> new IllegalArgumentException("JWT secret must be at least 32 characters (256 bits) long"));
            
        log.info("JWT Token Provider initialized with issuer: {}", jwtIssuer);
    }

    // ============= ADVANCED DESIGN PATTERNS IMPLEMENTATION =============
    
    /**
     * Sealed interface for type-safe token strategy pattern
     */
    public sealed interface TokenStrategy permits AccessTokenStrategy, RefreshTokenStrategy, SystemTokenStrategy {
        String getTokenType();
        long getExpirationMs();
        Map<String, Object> buildClaims(TokenContext context);
        
        default TokenBuilder createBuilder(TokenContext context, JwtTokenProvider provider) {
            return new TokenBuilder(this, context, provider);
        }
    }
    
    /**
     * Immutable token context using Records (Java 24)
     */
    public record TokenContext(
        String subject,
        String email,
        String deviceFingerprint,
        String ipAddress,
        String authorities,
        String subscriptionTier,
        String kycStatus,
        String purpose,
        Map<String, Object> customClaims
    ) {
        // Compact constructor for validation
        public TokenContext {
            Objects.requireNonNull(subject, "Subject cannot be null");
            customClaims = customClaims != null ? Map.copyOf(customClaims) : Map.of();
        }
        
        // Factory methods for different contexts
        public static TokenContext forUser(User user, String deviceFingerprint, String ipAddress, String authorities) {
            return new TokenContext(
                user.getId().toString(),
                user.getEmail(),
                deviceFingerprint,
                ipAddress,
                authorities,
                user.getSubscriptionTier().getValue(),
                user.getKycStatus().getValue(),
                null,
                Map.of()
            );
        }
        
        public static TokenContext forRefresh(User user, String deviceFingerprint) {
            return new TokenContext(
                user.getId().toString(),
                user.getEmail(),
                deviceFingerprint,
                null,
                null,
                null,
                null,
                null,
                Map.of()
            );
        }
        
        public static TokenContext forSystem(String purpose) {
            return new TokenContext("system", null, null, null, null, null, null, purpose, Map.of());
        }
        
        public static TokenContext simple(String email, Long userId, String deviceFingerprint) {
            return new TokenContext(userId.toString(), email, deviceFingerprint, null, null, null, null, null, Map.of());
        }
    }
    
    /**
     * High-performance functional token builder with caching
     */
    public static final class TokenBuilder {
        private final TokenStrategy strategy;
        private final TokenContext context;
        private final JwtTokenProvider provider;
        private String cachedToken = null;
        
        public TokenBuilder(TokenStrategy strategy, TokenContext context, JwtTokenProvider provider) {
            this.strategy = strategy;
            this.context = context;
            this.provider = provider;
        }
        
        public String build() {
            return Optional.ofNullable(cachedToken)
                    .orElseGet(() -> {
                        cachedToken = buildTokenInternal();
                        return cachedToken;
                    });
        }
        
        private String buildTokenInternal() {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + strategy.getExpirationMs());
            
            return Optional.of(Jwts.builder())
                .map(builder -> builder.subject(context.subject())
                    .issuer(provider.jwtIssuer)
                    .issuedAt(now)
                    .expiration(expiryDate))
                .map(builder -> addClaims(builder, strategy.buildClaims(context)))
                .map(builder -> builder.signWith(provider.secretKey))
                .map(JwtBuilder::compact)
                .orElseThrow(() -> new IllegalStateException("Token generation failed"));
        }
        
        private JwtBuilder addClaims(JwtBuilder builder, Map<String, Object> claims) {
            claims.forEach(builder::claim);
            return builder;
        }
    }
    
    /**
     * Strategy implementations using Records for immutability
     */
    public record AccessTokenStrategy(long expirationMs) implements TokenStrategy {
        @Override
        public String getTokenType() { return "access"; }
        
        @Override
        public long getExpirationMs() { return expirationMs; }
        
        @Override
        public Map<String, Object> buildClaims(TokenContext context) {
            return Stream.of(
                entry("email", context.email()),
                entry("authorities", context.authorities()),
                entry("subscription_tier", context.subscriptionTier()),
                entry("kyc_status", context.kycStatus()),
                entry("device_fingerprint", context.deviceFingerprint()),
                entry("ip_address", context.ipAddress()),
                entry("token_type", getTokenType())
            )
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
    
    public record RefreshTokenStrategy(long expirationMs) implements TokenStrategy {
        @Override
        public String getTokenType() { return "refresh"; }
        
        @Override
        public long getExpirationMs() { return expirationMs; }
        
        @Override
        public Map<String, Object> buildClaims(TokenContext context) {
            return Stream.of(
                entry("email", context.email()),
                entry("device_fingerprint", context.deviceFingerprint()),
                entry("token_type", getTokenType())
            )
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
    
    public record SystemTokenStrategy(long customExpirationMs) implements TokenStrategy {
        @Override
        public String getTokenType() { return "system"; }
        
        @Override
        public long getExpirationMs() { return customExpirationMs; }
        
        @Override
        public Map<String, Object> buildClaims(TokenContext context) {
            return Map.of(
                "purpose", context.purpose(),
                "token_type", getTokenType()
            );
        }
    }
    
    // Helper method for Map.Entry creation (Java 24 optimization)
    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return Map.entry(key, value);
    }
    
    // ============= PUBLIC API METHODS (Simplified) =============
    
    /**
     * Generate JWT access token using advanced patterns
     */
    public String generateToken(Authentication authentication, String deviceFingerprint, String ipAddress) {
        User user = (User) authentication.getPrincipal();
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
                
        return new AccessTokenStrategy(jwtExpirationMs)
            .createBuilder(TokenContext.forUser(user, deviceFingerprint, ipAddress, authorities), this)
            .build();
    }
    
    /**
     * Generate token with user details using advanced patterns
     */
    public String generateToken(String email, Long userId, String deviceFingerprint, boolean isRefreshToken) {
        TokenContext context = TokenContext.simple(email, userId, deviceFingerprint);
        TokenStrategy strategy = isRefreshToken 
            ? new RefreshTokenStrategy(jwtRefreshExpirationMs) 
            : new AccessTokenStrategy(jwtExpirationMs);
        
        return strategy.createBuilder(context, this).build();
    }
    
    /**
     * Get token expiration time in seconds
     */
    public long getExpirationTime() {
        return new AccessTokenStrategy(jwtExpirationMs).getExpirationMs() / 1000;
    }

    /**
     * Generate JWT refresh token using advanced patterns
     */
    public String generateRefreshToken(User user, String deviceFingerprint) {
        return new RefreshTokenStrategy(jwtRefreshExpirationMs)
            .createBuilder(TokenContext.forRefresh(user, deviceFingerprint), this)
            .build();
    }

    /**
     * Generate MFA token with limited scope (short-lived, MFA-only)
     */
    public String generateMfaToken(String email, Long userId, String deviceFingerprint) {
        TokenContext context = TokenContext.simple(email, userId, deviceFingerprint);
        
        // MFA tokens are short-lived (5 minutes) and have limited scope
        long mfaExpirationMs = 5 * 60 * 1000; // 5 minutes
        
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + mfaExpirationMs))
            .claim("userId", userId)
            .claim("deviceFingerprint", deviceFingerprint)
            .claim("tokenType", "MFA_TOKEN")
            .claim("mfaRequired", true)
            .signWith(secretKey)
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
        return SafeOperations.safelyToResult(() -> {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        })
        .fold(
            error -> {
                log.error("JWT token validation failed: {}", error);
                return false;
            },
            isValid -> isValid
        );
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return SafeOperations.safelyToResult(() -> {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        })
        .fold(
            error -> {
                log.error("Error checking token expiration: {}", error);
                return true;
            },
            expired -> expired
        );
    }

    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        return SafeOperations.safelyToResult(() -> {
            String tokenType = getTokenTypeFromToken(token);
            return "refresh".equals(tokenType);
        })
        .mapError(e -> {
            log.error("Error checking token type: {}", e);
            return "false";
        })
        .fold(
            error -> false,
            result -> result
        );
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
        return SafeOperations.safelyToResult(() -> {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            return Math.max(0, expiration.getTime() - now.getTime());
        })
        .mapError(e -> {
            log.error("Error getting remaining expiration time: {}", e);
            return "0";
        })
        .fold(
            error -> 0L,
            result -> result
        );
    }

    /**
     * Validate device fingerprint in token
     */
    public boolean validateDeviceFingerprint(String token, String currentDeviceFingerprint) {
        return SafeOperations.safelyToResult(() -> {
            String tokenDeviceFingerprint = getDeviceFingerprintFromToken(token);
            return Optional.ofNullable(currentDeviceFingerprint)
                .map(current -> current.equals(tokenDeviceFingerprint))
                .orElse(false);
        })
        .mapError(e -> {
            log.error("Error validating device fingerprint: {}", e);
            return "false";
        })
        .fold(
            error -> false,
            result -> result
        );
    }

    /**
     * Extract token from Authorization header
     */
    public String getTokenFromHeader(String authHeader) {
        return Optional.ofNullable(authHeader)
            .filter(header -> header.startsWith("Bearer "))
            .map(header -> header.substring(7))
            .orElse(null);
    }

    /**
     * Generate token for system operations using advanced strategy pattern
     */
    public String generateSystemToken(String purpose, long expirationMs) {
        return new SystemTokenStrategy(expirationMs)
            .createBuilder(TokenContext.forSystem(purpose), this)
            .build();
    }

}