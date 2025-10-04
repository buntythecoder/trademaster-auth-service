package com.trademaster.auth.config;

import com.trademaster.auth.filter.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration for Auth Service
 *
 * MANDATORY: Rule #15 - Structured Logging Configuration
 * MANDATORY: Rule #6 - Zero Trust Security Configuration
 *
 * Features:
 * - Correlation ID filter registration
 * - CORS configuration for frontend integration
 * - Request/response logging setup
 * - Security headers configuration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {

    private final CorrelationIdFilter correlationIdFilter;

    /**
     * Register Correlation ID Filter - Rule #15 compliance
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterBean() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(correlationIdFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // Execute first
        registration.setName("correlationIdFilter");
        return registration;
    }

    /**
     * Configure CORS for frontend integration
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "https://*.trademaster.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("X-Correlation-ID", "X-Rate-Limit-Remaining", "X-Rate-Limit-Reset")
                .maxAge(3600);
    }
}