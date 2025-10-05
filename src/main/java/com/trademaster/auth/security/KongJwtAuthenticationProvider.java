package com.trademaster.auth.security;

import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Kong JWT Authentication Provider
 *
 * MANDATORY: Golden Specification - Kong API Gateway Integration
 * MANDATORY: Rule #3 - Functional Programming (no if-else)
 * MANDATORY: Rule #11 - Railway Programming (Result types)
 *
 * Features:
 * - Kong-generated JWT token validation
 * - Claims extraction and authority mapping
 * - Functional error handling
 * - Security context integration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KongJwtAuthenticationProvider implements AuthenticationProvider {

    @Value("${kong.jwt.secret:your-256-bit-secret-key-here-change-in-production}")
    private String jwtSecret;

    @Value("${kong.jwt.issuer:trademaster-auth-service}")
    private String expectedIssuer;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return Optional.ofNullable(authentication.getCredentials())
                .map(Object::toString)
                .flatMap(this::validateAndParseToken)
                .map(this::createAuthenticationFromClaims)
                .orElseThrow(() -> new BadCredentialsException("Invalid Kong JWT token"));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Validate and parse Kong JWT token using functional approach
     */
    private Optional<Claims> validateAndParseToken(String token) {
        return parseJwtToken(token)
                .flatMap(this::validateTokenClaims)
                .map(result -> {
                    log.debug("Kong JWT token validated successfully for user: {}",
                            result.getSubject());
                    return result;
                })
                .or(() -> {
                    log.warn("Kong JWT token validation failed");
                    return Optional.empty();
                });
    }

    /**
     * Parse JWT token using functional error handling
     */
    private Optional<Claims> parseJwtToken(String token) {
        return SafeOperations.safely(() -> {
                SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
                return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }).or(() -> {
            log.warn("Failed to parse Kong JWT token");
            return Optional.empty();
        });
    }

    /**
     * Validate token claims using functional composition
     */
    private Optional<Claims> validateTokenClaims(Claims claims) {
        return Optional.of(claims)
                .filter(this::isTokenNotExpired)
                .filter(this::hasValidIssuer)
                .filter(this::hasValidSubject);
    }

    /**
     * Check if token is not expired using functional approach
     */
    private boolean isTokenNotExpired(Claims claims) {
        return Optional.ofNullable(claims.getExpiration())
                .map(expiration -> expiration.after(new Date()))
                .orElse(false);
    }

    /**
     * Validate issuer using functional approach
     */
    private boolean hasValidIssuer(Claims claims) {
        return Optional.ofNullable(claims.getIssuer())
                .filter(issuer -> issuer.equals(expectedIssuer))
                .isPresent();
    }

    /**
     * Check if subject exists using functional approach
     */
    private boolean hasValidSubject(Claims claims) {
        return Optional.ofNullable(claims.getSubject())
                .filter(subject -> !subject.trim().isEmpty())
                .isPresent();
    }

    /**
     * Create Spring Security Authentication from JWT claims
     */
    private Authentication createAuthenticationFromClaims(Claims claims) {
        String username = claims.getSubject();
        List<GrantedAuthority> authorities = extractAuthorities(claims);

        PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(username, null, authorities);

        authentication.setDetails(createAuthenticationDetails(claims));

        return authentication;
    }

    /**
     * Extract authorities from JWT claims using functional streams
     */
    private List<GrantedAuthority> extractAuthorities(Claims claims) {
        return Optional.ofNullable(claims.get("roles"))
                .filter(roles -> roles instanceof List<?>)
                .map(roles -> (List<?>) roles)
                .map(List::stream)
                .orElse(Stream.empty())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .toList();
    }

    /**
     * Create authentication details from claims
     */
    private KongJwtAuthenticationDetails createAuthenticationDetails(Claims claims) {
        return new KongJwtAuthenticationDetails(
                claims.getSubject(),
                claims.getIssuer(),
                claims.getIssuedAt(),
                claims.getExpiration(),
                extractSessionId(claims),
                extractDeviceFingerprint(claims)
        );
    }

    /**
     * Extract session ID from claims
     */
    private String extractSessionId(Claims claims) {
        return Optional.ofNullable(claims.get("sessionId"))
                .map(Object::toString)
                .orElse(null);
    }

    /**
     * Extract device fingerprint from claims
     */
    private String extractDeviceFingerprint(Claims claims) {
        return Optional.ofNullable(claims.get("deviceFingerprint"))
                .map(Object::toString)
                .orElse(null);
    }

    /**
     * Kong JWT Authentication Details record
     */
    public record KongJwtAuthenticationDetails(
            String subject,
            String issuer,
            Date issuedAt,
            Date expiration,
            String sessionId,
            String deviceFingerprint
    ) {
        public boolean isExpired() {
            return Optional.ofNullable(expiration)
                    .map(exp -> exp.before(new Date()))
                    .orElse(true);
        }

        public long getTimeToExpiry() {
            return Optional.ofNullable(expiration)
                    .map(exp -> exp.getTime() - System.currentTimeMillis())
                    .filter(ttl -> ttl > 0)
                    .orElse(0L);
        }
    }
}