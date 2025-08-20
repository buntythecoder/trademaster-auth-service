package com.trademaster.auth.security;

import com.trademaster.auth.entity.User;
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
        
        String jwt = getJwtFromRequest(request);
        
        if (StringUtils.hasText(jwt)) {
            try {
                // Validate token
                if (jwtTokenProvider.validateToken(jwt)) {
                    // Check if it's an access token (not refresh token)
                    if (!jwtTokenProvider.isRefreshToken(jwt)) {
                        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                        
                        // Load user details
                        UserDetails userDetails = userService.loadUserById(userId);
                        
                        if (userDetails != null && userDetails.isEnabled()) {
                            User user = (User) userDetails;
                            
                            // Validate device fingerprint for additional security
                            String currentDeviceFingerprint = deviceFingerprintService.generateFingerprint(request);
                            String tokenDeviceFingerprint = jwtTokenProvider.getDeviceFingerprintFromToken(jwt);
                            
                            if (isDeviceFingerprintValid(currentDeviceFingerprint, tokenDeviceFingerprint)) {
                                // Create authentication token
                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                
                                // Set authentication context
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                
                                // Update user's last activity
                                userService.updateLastActivity(userId, request.getRemoteAddr(), currentDeviceFingerprint);
                                
                                log.debug("User {} authenticated successfully with JWT token", user.getEmail());
                            } else {
                                log.warn("Device fingerprint mismatch for user {}", userId);
                                // Don't set authentication - will be treated as unauthenticated
                            }
                        } else {
                            log.warn("User not found or disabled for user ID: {}", userId);
                        }
                    } else {
                        log.debug("Refresh token used in authentication header - ignoring");
                    }
                } else {
                    log.debug("JWT token validation failed");
                }
            } catch (Exception e) {
                log.error("Error processing JWT token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from request Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Validate device fingerprint with some tolerance for browser updates
     */
    private boolean isDeviceFingerprintValid(String currentFingerprint, String tokenFingerprint) {
        if (currentFingerprint == null || tokenFingerprint == null) {
            return false;
        }
        
        // Exact match
        if (currentFingerprint.equals(tokenFingerprint)) {
            return true;
        }
        
        // Calculate similarity for browser updates tolerance
        double similarity = calculateFingerPrintSimilarity(currentFingerprint, tokenFingerprint);
        
        // Allow 85% similarity to account for minor browser updates
        return similarity >= 0.85;
    }

    /**
     * Calculate fingerprint similarity using Jaccard similarity
     */
    private double calculateFingerPrintSimilarity(String fp1, String fp2) {
        if (fp1 == null || fp2 == null) {
            return 0.0;
        }
        
        // Split fingerprints into components
        String[] components1 = fp1.split(":");
        String[] components2 = fp2.split(":");
        
        if (components1.length != components2.length) {
            return 0.0;
        }
        
        int matches = 0;
        for (int i = 0; i < components1.length; i++) {
            if (components1[i].equals(components2[i])) {
                matches++;
            }
        }
        
        return (double) matches / components1.length;
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