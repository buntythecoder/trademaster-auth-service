package com.trademaster.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * TradeMaster Spring Cloud Config Server Application
 * 
 * Centralized configuration management server for all TradeMaster microservices.
 * Provides secure, versioned, and environment-specific configuration delivery.
 * 
 * Features:
 * - Git-based configuration repository
 * - Environment-specific profiles (dev, test, prod)
 * - Encrypted configuration support
 * - Health monitoring and metrics
 * - Service discovery integration
 * - Configuration change notifications
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since Java 24
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {

    /**
     * Main method to bootstrap the Spring Cloud Config Server.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}