package com.trademaster.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration for TradeMaster Authentication Service
 * 
 * Provides comprehensive API documentation with:
 * - Authentication examples
 * - Request/Response schemas
 * - Error handling documentation
 * - Security scheme definitions
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TradeMaster Authentication Service API")
                .version("1.0.0")
                .description("""
                    # TradeMaster Authentication Service
                    
                    A comprehensive authentication and authorization service for the TradeMaster trading platform.
                    
                    ## Features
                    - User registration and login
                    - JWT-based authentication with refresh tokens
                    - Multi-factor authentication (MFA)
                    - Email verification and password reset
                    - Device fingerprinting and security monitoring
                    - Session management with Redis
                    - Comprehensive audit logging
                    
                    ## Security
                    - Financial industry-grade security standards
                    - AES-256 encryption for sensitive data
                    - BCrypt password hashing with 12 rounds
                    - Rate limiting and account lockout protection
                    - Device fingerprinting and suspicious activity detection
                    
                    ## Authentication
                    Most endpoints require authentication via JWT Bearer tokens.
                    Include the token in the Authorization header: `Bearer <your-jwt-token>`
                    """)
                .contact(new Contact()
                    .name("TradeMaster Development Team")
                    .email("dev@trademaster.com")
                    .url("https://trademaster.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://trademaster.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080" + contextPath)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api-staging.trademaster.com" + contextPath)
                    .description("Staging Environment"),
                new Server()
                    .url("https://api.trademaster.com" + contextPath)
                    .description("Production Environment")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT authentication token obtained from login endpoint")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}