package com.trademaster.userprofile.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Functional Security Configuration for User Profile Service
 * 
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: JWT Authentication with Role-Based Access Control
 * MANDATORY: Method-level security with @PreAuthorize annotations
 * MANDATORY: CORS configuration for frontend integration
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Value("${trademaster.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;
    
    @Value("${trademaster.cors.allowed-headers:*}")
    private String allowedHeaders;
    
    @Value("${trademaster.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;
    
    @Value("${trademaster.security.permit-all-paths:/actuator/health,/actuator/info}")
    private String[] permitAllPaths;
    
    /**
     * Main security filter chain with functional configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(permitAllPaths).permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // Profile endpoints - user can access own profile or admin can access any
                .requestMatchers("/api/v1/profiles/**").hasAnyRole("USER", "ADMIN")
                
                // Document endpoints - user can manage own documents or admin can access any
                .requestMatchers("/api/v1/documents/**").hasAnyRole("USER", "ADMIN")
                
                // Preferences endpoints - user can manage own preferences or admin can access any
                .requestMatchers("/api/v1/preferences/**").hasAnyRole("USER", "ADMIN")
                
                // Audit endpoints - admin only
                .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            .build();
    }
    
    
    /**
     * Custom authentication entry point for JWT failures
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Authentication failed for request: {} from IP: {} - {}", 
                request.getRequestURI(), 
                getClientIpAddress(request), 
                authException.getMessage());
            
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            String jsonResponse = """
                {
                    "error": "Unauthorized",
                    "message": "Authentication required",
                    "path": "%s",
                    "timestamp": "%s"
                }
                """.formatted(
                    request.getRequestURI(),
                    LocalDateTime.now().toString()
                );
            
            response.getWriter().write(jsonResponse);
        };
    }
    
    /**
     * Custom access denied handler for authorization failures
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access denied for request: {} from user: {} IP: {} - {}", 
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
                getClientIpAddress(request), 
                accessDeniedException.getMessage());
            
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            String jsonResponse = """
                {
                    "error": "Access Denied",
                    "message": "Insufficient privileges",
                    "path": "%s",
                    "timestamp": "%s"
                }
                """.formatted(
                    request.getRequestURI(),
                    LocalDateTime.now().toString()
                );
            
            response.getWriter().write(jsonResponse);
        };
    }
    
    /**
     * CORS configuration for frontend integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins
        List<String> origins = List.of(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);
        
        // Configure allowed methods
        List<String> methods = List.of(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);
        
        // Configure allowed headers
        if ("*".equals(allowedHeaders)) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = List.of(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        }
        
        // Allow credentials for JWT tokens
        configuration.setAllowCredentials(true);
        
        // Cache preflight responses
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configured with origins: {}, methods: {}, headers: {}", 
            origins, methods, allowedHeaders);
        
        return source;
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}