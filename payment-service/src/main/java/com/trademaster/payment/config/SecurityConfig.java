package com.trademaster.payment.config;

import com.trademaster.payment.security.ServiceApiKeyFilter;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration for Payment Service
 * Implements Zero Trust Security per Golden Specification
 *
 * Compliance:
 * - Rule 6: Zero Trust Security - JWT for external, API key for internal
 * - Rule 23: Security Implementation - Method-level security
 * - Rule 10: @Slf4j for structured logging
 * - Rule 16: Dynamic configuration with @Value
 *
 * Security Architecture:
 * - External APIs: JWT authentication (Kong gateway → service)
 * - Internal APIs: API key authentication (service → service)
 * - Health/Actuator: Open access for monitoring
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final ServiceApiKeyFilter serviceApiKeyFilter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain for Payment Service");

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Health checks - open access for Kong/Consul
                .requestMatchers("/actuator/health", "/api/v2/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // Internal APIs - API key authentication
                .requestMatchers("/api/internal/**").hasRole("SERVICE")

                // External APIs - JWT authentication
                .requestMatchers("/api/v1/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // JWT validation for external APIs
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            // API key validation for internal APIs
            .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security filter chain configured successfully");
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JWT decoder with issuer: {}", issuerUri);
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }
}
