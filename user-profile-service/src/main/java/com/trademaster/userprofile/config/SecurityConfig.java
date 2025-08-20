package com.trademaster.userprofile.config;

import com.trademaster.userprofile.security.JwtAuthenticationFilter;
import com.trademaster.userprofile.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (health checks, documentation)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Profile creation endpoint (users need to be authenticated but may not have profile yet)
                .requestMatchers(HttpMethod.POST, "/api/v1/profiles").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me/exists").hasAnyRole("USER", "ADMIN")
                
                // User profile endpoints (users can access their own data)
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/profiles/me/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/profiles/me").hasAnyRole("USER", "ADMIN")
                
                // Document endpoints (users can manage their own documents)
                .requestMatchers(HttpMethod.GET, "/api/v1/documents/me/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/documents/me/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/me/**").hasAnyRole("USER", "ADMIN")
                
                // Admin-only endpoints
                .requestMatchers("/api/v1/profiles/search").hasRole("ADMIN")
                .requestMatchers("/api/v1/profiles/by-kyc-status/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/profiles/statistics").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
                .requestMatchers("/api/v1/profiles/kyc-renewal-needed").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/{profileId}").hasAnyRole("ADMIN", "SUPPORT_AGENT")
                
                // Document verification endpoints (admin/compliance only)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/documents/{documentId}/verify").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
                .requestMatchers("/api/v1/documents/pending-verification").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
                
                // KYC verification endpoints
                .requestMatchers("/api/v1/kyc/**").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins in production (from environment variables)
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",      // Frontend development
            "http://localhost:5173",      // Vite dev server
            "http://localhost:8000",      // Kong Gateway
            "https://*.trademaster.com"   // Production domains
        ));
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        
        return source;
    }
}