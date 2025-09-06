package com.trademaster.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for TradeMaster Config Server.
 * 
 * Provides secure access to configuration endpoints while allowing
 * health checks and metrics to be accessible for monitoring.
 * 
 * Security Features:
 * - HTTP Basic authentication for config access
 * - Role-based access control
 * - Encrypted password storage
 * - Public access for health checks
 * - CORS configuration for frontend access
 */
@Configuration
@EnableWebSecurity
public class ConfigServerSecurityConfig {

    @Value("${spring.security.user.name:config-admin}")
    private String configUsername;
    
    @Value("${spring.security.user.password:config-secret-2024}")
    private String configPassword;

    /**
     * Password encoder for secure password storage.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * User details service with in-memory user store.
     * In production, this should be replaced with a proper user service.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails configUser = User.builder()
                .username(configUsername)
                .password(passwordEncoder().encode(configPassword))
                .roles("ADMIN", "CONFIG_READER")
                .build();

        UserDetails healthUser = User.builder()
                .username("health-checker")
                .password(passwordEncoder().encode("health-check-2024"))
                .roles("HEALTH")
                .build();

        return new InMemoryUserDetailsManager(configUser, healthUser);
    }

    /**
     * Security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints for monitoring and health checks
                .requestMatchers(
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/prometheus"
                ).permitAll()
                
                // Configuration endpoints require authentication
                .requestMatchers("/config/**").hasRole("ADMIN")
                .requestMatchers("/{application}/**").hasAnyRole("ADMIN", "CONFIG_READER")
                
                // Management endpoints require health role or admin
                .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "HEALTH")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic
                .realmName("TradeMaster Config Server")
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/actuator/**", "/config/**")
            )
            .cors(cors -> cors
                .configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOriginPatterns(java.util.List.of("*"));
                    corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                })
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
            )
            .build();
    }
}