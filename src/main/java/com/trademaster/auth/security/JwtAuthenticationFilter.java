package com.trademaster.auth.security;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.*;
import com.trademaster.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * JWT Authentication Filter for processing JWT tokens in requests
 * 
 * This filter:
 * - Extracts JWT tokens from Authorization header
 * - Validates tokens using JwtTokenProvider
 * - Loads user details and sets authentication context
 * - Performs security checks (device fingerprint, token type)
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final DeviceFingerprintService deviceFingerprintService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        Optional.ofNullable(getJwtFromRequest(request))
            .filter(StringUtils::hasText)
            .ifPresent(jwt -> processJwtAuthentication(jwt, request));
        
        filterChain.doFilter(request, response);
    }
    
    private void processJwtAuthentication(String jwt, HttpServletRequest request) {
        SafeOperations.safelyToResult(() -> authenticateWithJwt(jwt, request))
            .mapError(e -> {
                log.error("Error processing JWT token: {}", e);
                SecurityContextHolder.clearContext();
                return "false";
            })
            .fold(
                result -> result,
                error -> false
            );
    }
    
    private Boolean authenticateWithJwt(String jwt, HttpServletRequest request) {
        return Optional.of(jwt)
            .filter(jwtTokenProvider::validateToken)
            .filter(token -> !jwtTokenProvider.isRefreshToken(token))
            .map(token -> processValidToken(token, request))
            .orElseGet(() -> {
                log.debug("JWT token validation failed or is refresh token");
                return false;
            });
    }
    
    private Boolean processValidToken(String jwt, HttpServletRequest request) {
        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
        
        return Optional.ofNullable(userService.loadUserById(userId))
            .filter(Objects::nonNull)
            .filter(UserDetails::isEnabled)
            .map(userDetails -> (User) userDetails)
            .filter(user -> validateDeviceFingerprint(jwt, request, user))
            .map(user -> {
                createAndSetAuthentication(user, request);
                updateUserActivity(userId, request);
                log.debug("User {} authenticated successfully with JWT token", user.getEmail());
                return true;
            })
            .orElseGet(() -> {
                log.warn("User not found, disabled, or device fingerprint mismatch for user ID: {}", userId);
                return false;
            });
    }
    
    private Boolean validateDeviceFingerprint(String jwt, HttpServletRequest request, User user) {
        String currentFingerprint = deviceFingerprintService.generateFingerprint(request);
        String tokenFingerprint = jwtTokenProvider.getDeviceFingerprintFromToken(jwt);
        
        return Optional.of(isDeviceFingerprintValid(currentFingerprint, tokenFingerprint))
            .filter(valid -> valid)
            .or(() -> {
                log.warn("Device fingerprint mismatch for user {}", user.getId());
                return Optional.of(false);
            })
            .orElse(false);
    }
    
    private void createAndSetAuthentication(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    private void updateUserActivity(Long userId, HttpServletRequest request) {
        String currentFingerprint = deviceFingerprintService.generateFingerprint(request);
        userService.updateLastActivity(userId, request.getRemoteAddr(), currentFingerprint);
    }

    /**
     * Extract JWT token from request Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
            .filter(StringUtils::hasText)
            .filter(token -> token.startsWith("Bearer "))
            .map(token -> token.substring(7))
            .orElse(null);
    }

    /**
     * Validate device fingerprint with some tolerance for browser updates
     */
    private boolean isDeviceFingerprintValid(String currentFingerprint, String tokenFingerprint) {
        return Optional.ofNullable(currentFingerprint)
            .flatMap(current -> Optional.ofNullable(tokenFingerprint)
                .map(token -> validateFingerprintMatch(current, token)))
            .orElse(false);
    }
    
    private Boolean validateFingerprintMatch(String currentFingerprint, String tokenFingerprint) {
        return Optional.of(currentFingerprint.equals(tokenFingerprint))
            .filter(exactMatch -> exactMatch)
            .map(match -> true)
            .orElseGet(() -> {
                double similarity = calculateFingerPrintSimilarity(currentFingerprint, tokenFingerprint);
                return similarity >= 0.85;
            });
    }

    /**
     * Calculate fingerprint similarity using Jaccard similarity
     */
    private double calculateFingerPrintSimilarity(String fp1, String fp2) {
        return Optional.ofNullable(fp1)
            .flatMap(f1 -> Optional.ofNullable(fp2).map(f2 -> new String[]{f1, f2}))
            .map(fps -> calculateComponentSimilarity(fps[0].split(":"), fps[1].split(":")))
            .orElse(0.0);
    }
    
    private Double calculateComponentSimilarity(String[] components1, String[] components2) {
        return Optional.of(components1.length == components2.length)
            .filter(sameLength -> sameLength)
            .map(sameLength -> {
                long matches = IntStream.range(0, components1.length)
                    .filter(i -> components1[i].equals(components2[i]))
                    .count();
                return (double) matches / components1.length;
            })
            .orElse(0.0);
    }

    /**
     * Skip JWT processing for certain paths
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT processing for these paths
        return path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/forgot-password") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/favicon.ico");
    }
}