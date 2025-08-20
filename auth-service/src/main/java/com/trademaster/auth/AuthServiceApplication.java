package com.trademaster.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for TradeMaster Authentication Service
 * 
 * This service provides:
 * - User authentication and authorization
 * - JWT token management
 * - Multi-factor authentication (MFA)
 * - Session management with Redis
 * - Rate limiting and security controls
 * - Audit logging for compliance
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableWebSecurity
@EnableJpaAuditing
@EnableCaching
@EnableTransactionManagement
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}