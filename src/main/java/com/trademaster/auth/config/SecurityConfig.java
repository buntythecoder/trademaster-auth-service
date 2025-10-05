package com.trademaster.auth.config;

import com.trademaster.auth.constants.AuthConstants;
import com.trademaster.auth.security.JwtAuthenticationFilter;
import com.trademaster.auth.security.JwtAuthenticationEntryPoint;
import com.trademaster.auth.security.ServiceApiKeyFilter;
import com.trademaster.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration for TradeMaster Authentication Service
 * 
 * Features:
 * - JWT-based authentication
 * - Role-based access control (RBAC)
 * - Rate limiting integration
 * - CORS configuration for web clients
 * - Security headers and CSRF protection
 * - Method-level security annotations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final ServiceApiKeyFilter serviceApiKeyFilter;

    /**
     * Configure HTTP security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for JWT-based API
            .csrf(AbstractHttpConfigurer::disable)

            // Disable CORS - Kong Gateway handles CORS at API Gateway level
            .cors(AbstractHttpConfigurer::disable)
            
            // Configure session management (stateless for JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authentication entry point
            .exceptionHandling(exceptions -> 
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_REGISTER, 
                               AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_LOGIN,
                               AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_FORGOT_PASSWORD,
                               AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_RESET_PASSWORD,
                               AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_VERIFY_EMAIL).permitAll()
                
                // Actuator endpoints (health check, metrics)
                .requestMatchers("/actuator/health", 
                               "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole(AuthConstants.ROLE_ADMIN)
                
                // API documentation
                .requestMatchers("/swagger-ui/**",
                               "/v3/api-docs/**",
                               "/swagger-resources/**",
                               "/webjars/**").permitAll()

                // Internal API endpoints - require SERVICE role (handled by ServiceApiKeyFilter)
                .requestMatchers("/internal/**").hasRole("SERVICE")
                
                // MFA endpoints - require authentication
                .requestMatchers(AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_MFA).authenticated()
                
                // Token refresh - require authentication
                .requestMatchers(AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_REFRESH).authenticated()
                
                // Logout - require authentication
                .requestMatchers(AuthConstants.API_V1_AUTH + AuthConstants.ENDPOINT_LOGOUT).authenticated()
                
                // Profile management - require authentication
                .requestMatchers(AuthConstants.ENDPOINT_PROFILE).authenticated()
                
                // Admin endpoints
                .requestMatchers(AuthConstants.ENDPOINT_ADMIN).hasRole(AuthConstants.ROLE_ADMIN)
                
                // All other requests require authentication
                .anyRequest().authenticated())
            
            // Add custom authentication filters in order
            .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)  // Order 1: Kong API keys
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)  // Order 2: JWT
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(AuthConstants.SECURITY_HEADER_MAX_AGE_SECONDS)
                    .includeSubDomains(true)))
            
            // Configure authentication provider
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    /**
     * Configure CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins in production
        configuration.setAllowedOriginPatterns(Arrays.asList(AuthConstants.ALLOWED_ORIGINS));
        
        configuration.setAllowedMethods(Arrays.asList(AuthConstants.ALLOWED_METHODS));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(AuthConstants.CORS_MAX_AGE_SECONDS);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    /**
     * Password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(AuthConstants.BCRYPT_STRENGTH);
    }

    /**
     * Authentication manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configure DAO authentication provider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(AuthConstants.HIDE_USER_NOT_FOUND_EXCEPTIONS);
        return authProvider;
    }
}