package com.trademaster.agentos.config;

import com.trademaster.agentos.service.StructuredLoggingService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ CORRECT: JWT Security Configuration with Virtual Threads
 * 
 * MANDATORY: Security implementation following trademaster-coding-standards.md v2.0
 * - JWT token validation with Virtual Threads
 * - Structured security logging
 * - Prometheus security metrics
 * - CORS configuration for React frontend
 * - Method-level security with SpEL
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final AgentOSMetrics metrics;
    private final StructuredLoggingService structuredLogger;
    
    @Value("${agentos.security.allowed-origins}")
    private String allowedOrigins;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Protected endpoints - require authentication
                .requestMatchers("/api/v1/agents/**").authenticated()
                .requestMatchers("/api/v1/tasks/**").authenticated()
                .requestMatchers("/api/v1/orchestration/**").authenticated()
                
                // Admin endpoints - require ADMIN role
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    structuredLogger.logSecurityIncident(
                        "unauthorized_access_attempt",
                        "warning",
                        null,
                        request.getRemoteAddr(),
                        Map.of(
                            "endpoint", request.getRequestURI(),
                            "method", request.getMethod(),
                            "userAgent", request.getHeader("User-Agent")
                        )
                    );
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    structuredLogger.logSecurityIncident(
                        "access_denied",
                        "warning",
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        request.getRemoteAddr(),
                        Map.of(
                            "endpoint", request.getRequestURI(),
                            "method", request.getMethod(),
                            "requiredRole", "ADMIN"
                        )
                    );
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Access denied\"}");
                })
            )
            .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

/**
 * ✅ CORRECT: JWT Authentication Filter with Virtual Threads
 * 
 * MANDATORY: Blocking JWT validation - Virtual Thread makes it scalable
 */
@Component
@RequiredArgsConstructor
@Slf4j
class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtAuthenticationManager authManager;
    private final StructuredLoggingService structuredLogger;
    private final AgentOSMetrics metrics;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        var timer = metrics.startApiTimer();
        
        try {
            // ✅ VIRTUAL THREADS: Blocking JWT validation - no performance penalty
            CompletableFuture<Authentication> authFuture = authManager.authenticateAsync(token, request);
            Authentication authentication = authFuture.join(); // Blocking call on Virtual Thread
            
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // ✅ STRUCTURED LOGGING: Record successful authentication
                structuredLogger.logAuthenticationSuccess(
                    authentication.getName(),
                    request.getSession().getId(),
                    request.getRemoteAddr(),
                    "JWT",
                    System.currentTimeMillis()
                );
                
                // ✅ CONTEXT PRESERVATION: Initialize request context for structured logging
                structuredLogger.initializeRequestContext(
                    authentication.getName(),
                    request.getSession().getId(),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
                );
            }
            
        } catch (Exception e) {
            // ✅ SECURITY LOGGING: Record authentication failure
            timer.stop(metrics.getAuthenticationTime());
            metrics.recordAuthenticationAttempt("JWT", "failed", System.currentTimeMillis());
            
            structuredLogger.logAuthenticationFailure(
                "unknown",
                request.getRemoteAddr(),
                "JWT",
                e.getMessage(),
                System.currentTimeMillis()
            );
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // ✅ CONTEXT CLEANUP: Clear MDC to prevent memory leaks in Virtual Threads
        structuredLogger.clearContext();
        super.destroy();
    }
}

/**
 * ✅ CORRECT: JWT Authentication Manager with Virtual Threads
 * 
 * MANDATORY: Uses blocking JWT validation with Virtual Thread scalability
 */
@Component
class JwtAuthenticationManager {
    
    private final SecretKey secretKey;
    private final StructuredLoggingService structuredLogger;
    private final AgentOSMetrics metrics;
    
    public JwtAuthenticationManager(@Value("${agentos.security.jwt-secret}") String jwtSecret,
                                  StructuredLoggingService structuredLogger,
                                  AgentOSMetrics metrics) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.structuredLogger = structuredLogger;
        this.metrics = metrics;
    }
    
    /**
     * ✅ VIRTUAL THREADS: Async authentication with CompletableFuture
     */
    public CompletableFuture<Authentication> authenticateAsync(String token, HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // ✅ BLOCKING JWT VALIDATION: Virtual Thread handles concurrency
                Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                
                // ✅ VALIDATION: Check token expiration
                if (claims.getExpiration().before(java.util.Date.from(Instant.now()))) {
                    structuredLogger.logTokenValidation("JWT", claims.getSubject(), false, "token_expired");
                    return null;
                }
                
                // ✅ EXTRACT USER INFO: Get user details from claims
                String userId = claims.getSubject();
                String username = claims.get("username", String.class);
                List<String> roles = claims.get("roles", List.class);
                
                // ✅ CREATE AUTHORITIES: Convert roles to Spring Security authorities
                List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .toList();
                
                // ✅ SUCCESS METRICS
                metrics.recordAuthenticationAttempt("JWT", "success", System.currentTimeMillis());
                structuredLogger.logTokenValidation("JWT", userId, true, "valid");
                
                return new UsernamePasswordAuthenticationToken(username, null, authorities);
                
            } catch (JwtException e) {
                // ✅ SECURITY LOGGING: Log JWT validation failure
                structuredLogger.logTokenValidation("JWT", "unknown", false, e.getMessage());
                structuredLogger.logSecurityIncident(
                    "jwt_validation_failed",
                    "warning",
                    "unknown",
                    request.getRemoteAddr(),
                    Map.of("error", e.getMessage(), "tokenPrefix", token.substring(0, Math.min(token.length(), 10)))
                );
                
                return null;
            }
        });
    }
}