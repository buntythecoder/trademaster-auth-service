package com.trademaster.brokerauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Broker Authentication Service Application
 * 
 * Main application class for the TradeMaster Broker Authentication Service.
 * Handles secure authentication flows for multiple broker integrations.
 * 
 * Features:
 * - Multi-broker authentication (Zerodha, Upstox, Angel One, ICICI Direct)
 * - OAuth 2.0 and API key authentication flows
 * - Secure credential storage with AES-256-GCM encryption
 * - Session management with automatic token refresh
 * - Rate limiting per broker requirements
 * - Comprehensive monitoring and metrics
 * - Virtual Threads for high-performance I/O operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
@Slf4j
public class BrokerAuthServiceApplication {
    
    public static void main(String[] args) {
        // Log startup information
        log.info("Starting TradeMaster Broker Authentication Service...");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Virtual Threads Available: {}", Runtime.version().feature() >= 19);
        
        // Configure system properties for optimal Virtual Thread performance
        System.setProperty("jdk.virtualThreadScheduler.parallelism", 
                          String.valueOf(Runtime.getRuntime().availableProcessors()));
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "256");
        
        SpringApplication app = new SpringApplication(BrokerAuthServiceApplication.class);
        
        // Set default properties
        app.setDefaultProperties(java.util.Map.of(
            "spring.application.name", "broker-auth-service",
            "spring.profiles.active", "local",
            "management.endpoints.web.exposure.include", "health,info,metrics,prometheus",
            "logging.level.com.trademaster.brokerauth", "INFO"
        ));
        
        try {
            app.run(args);
            log.info("TradeMaster Broker Authentication Service started successfully");
            log.info("Swagger UI available at: http://localhost:8087/api/v1/swagger-ui.html");
            log.info("Health check available at: http://localhost:8087/api/v1/actuator/health");
            log.info("Metrics available at: http://localhost:8087/api/v1/actuator/prometheus");
        } catch (Exception e) {
            log.error("Failed to start TradeMaster Broker Authentication Service", e);
            System.exit(1);
        }
    }
}