package com.trademaster.trading.config;

import com.trademaster.trading.security.JwtAuthenticationFilter;
import com.trademaster.trading.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration for Trading Service
 * 
 * Configures JWT-based authentication and authorization for trading endpoints using Java 24 Virtual Threads.
 * Integrates with Epic 1 authentication service for user validation.
 * 
 * Security Features:
 * - JWT token validation with Virtual Thread support
 * - User-specific data isolation
 * - Role-based access control
 * - Rate limiting protection
 * - CORS configuration
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Trading endpoints require authentication
                .requestMatchers("/api/v1/orders/**").authenticated()
                .requestMatchers("/api/v1/portfolio/**").authenticated()
                .requestMatchers("/api/v1/trades/**").authenticated()
                
                // WebSocket endpoints
                .requestMatchers("/ws/**").authenticated()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (configure based on environment)
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",   // React development server
            "http://localhost:8080",   // Frontend production
            "https://*.trademaster.com" // Production domains
        ));
        
        // Allow specific methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Allow specific headers
        configuration.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept", "Origin", 
            "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}