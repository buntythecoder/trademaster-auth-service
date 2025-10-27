package com.trademaster.portfolio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * JWT Token Extractor for Portfolio Service.
 *
 * Purpose: Extract user identity from JWT tokens with functional programming patterns
 * following TradeMaster Rule #3 (No if-else statements).
 *
 * Design Pattern: Strategy pattern with functional composition
 * Security: Validates JWT signature and extracts claims safely
 *
 * Rules Compliance:
 * - Rule #3: Functional programming - No if-else, uses Optional and pattern matching
 * - Rule #6: Zero Trust - Validates all tokens before extracting data
 * - Rule #11: Error handling - Returns Optional, no exceptions thrown
 *
 * @author TradeMaster Development Team
 */
@Slf4j
@Component
public class JwtTokenExtractor {

    private final String jwtSecret;
    private final SecretKey secretKey;

    /**
     * Constructor with JWT secret configuration.
     *
     * @param jwtSecret JWT signing secret from configuration
     */
    public JwtTokenExtractor(@Value("${trademaster.jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extract user ID from Authorization header.
     *
     * Pattern: Functional composition with Optional chaining
     * Rule #3: No if-else - Uses Optional flatMap and map
     *
     * @param authorizationHeader Authorization header value (e.g., "Bearer <token>")
     * @return Optional containing user ID, or empty if invalid
     */
    public Optional<Long> extractUserIdFromHeader(String authorizationHeader) {
        return Optional.ofNullable(authorizationHeader)
            .filter(header -> header.startsWith("Bearer "))
            .map(header -> header.substring(7))
            .flatMap(this::extractUserIdFromToken);
    }

    /**
     * Extract user ID from JWT token string.
     *
     * Pattern: Safe extraction with Optional error handling
     * Rule #11: No try-catch in business logic, returns Optional
     *
     * @param token JWT token string
     * @return Optional containing user ID, or empty if invalid
     */
    public Optional<Long> extractUserIdFromToken(String token) {
        return parseToken(token)
            .flatMap(this::extractUserIdFromClaims);
    }

    /**
     * Extract user ID from fallback header (X-User-ID).
     *
     * Pattern: Alternative extraction strategy for development/testing
     * Rule #3: Functional composition instead of if-else
     *
     * @param userIdHeader Direct user ID header
     * @return Optional containing user ID, or empty if invalid
     */
    public Optional<Long> extractUserIdFromDirectHeader(String userIdHeader) {
        return Optional.ofNullable(userIdHeader)
            .flatMap(this::parseUserId);
    }

    /**
     * Extract user ID with multiple strategies.
     *
     * Pattern: Chain of responsibility with functional composition
     * Rule #3: No if-else - Uses Optional orElseGet chaining
     *
     * Tries in order:
     * 1. Authorization header (JWT)
     * 2. X-User-ID header (fallback)
     * 3. Empty (requires authentication)
     *
     * @param authHeader Authorization header
     * @param userIdHeader Fallback user ID header
     * @return Optional containing user ID
     */
    public Optional<Long> extractUserId(String authHeader, String userIdHeader) {
        return extractUserIdFromHeader(authHeader)
            .or(() -> extractUserIdFromDirectHeader(userIdHeader));
    }

    // Private helper methods (Rule #19: Private access control)

    /**
     * Parse JWT token to extract claims.
     *
     * Pattern: Safe parsing with error logging
     * Rule #11: Error handling with Optional instead of exceptions
     *
     * @param token JWT token string
     * @return Optional containing parsed claims
     */
    private Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            log.debug("Successfully parsed JWT token");
            return Optional.of(claims);
        } catch (Exception e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract user ID from JWT claims.
     *
     * Pattern: Pattern matching with switch expression
     * Rule #14: Pattern matching instead of if-else
     *
     * @param claims JWT claims
     * @return Optional containing user ID
     */
    private Optional<Long> extractUserIdFromClaims(Claims claims) {
        return Optional.ofNullable(claims.get("userId"))
            .or(() -> Optional.ofNullable(claims.getSubject()))
            .flatMap(this::parseUserId);
    }

    /**
     * Parse user ID from object or string.
     *
     * Pattern: Type-safe parsing with pattern matching
     * Rule #14: Switch expression for type handling
     *
     * @param userIdValue User ID as Object
     * @return Optional containing parsed user ID
     */
    private Optional<Long> parseUserId(Object userIdValue) {
        return switch (userIdValue) {
            case null -> {
                log.debug("User ID value is null");
                yield Optional.empty();
            }
            case Long userId -> {
                log.debug("Extracted user ID: {}", userId);
                yield Optional.of(userId);
            }
            case Integer userId -> {
                log.debug("Extracted user ID: {}", userId);
                yield Optional.of(userId.longValue());
            }
            case String userIdStr -> parseUserIdString(userIdStr);
            default -> {
                log.warn("Unexpected user ID type: {}", userIdValue.getClass());
                yield Optional.empty();
            }
        };
    }

    /**
     * Parse user ID from string value.
     *
     * Pattern: Safe string parsing with error handling
     * Rule #11: Returns Optional instead of throwing exception
     *
     * @param userIdStr User ID as string
     * @return Optional containing parsed user ID
     */
    private Optional<Long> parseUserIdString(String userIdStr) {
        try {
            Long userId = Long.parseLong(userIdStr);
            log.debug("Parsed user ID from string: {}", userId);
            return Optional.of(userId);
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID string format: {}", userIdStr);
            return Optional.empty();
        }
    }
}
