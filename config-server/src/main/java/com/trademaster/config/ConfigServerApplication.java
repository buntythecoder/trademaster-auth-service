package com.trademaster.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TradeMaster Configuration Server
 * 
 * Centralized configuration management server using Spring Cloud Config.
 * Integrates with Consul for service discovery and provides configuration
 * management for all TradeMaster microservices.
 * 
 * Features:
 * - Git-backed configuration repository
 * - Environment-specific configurations (dev, test, prod)
 * - Real-time configuration refresh via Consul
 * - Secure configuration encryption
 * - Consul service discovery integration
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigServerApplication.class);
    
    public static void main(String[] args) {
        log.info("Starting TradeMaster Configuration Server...");
        SpringApplication.run(ConfigServerApplication.class, args);
        log.info("TradeMaster Configuration Server started successfully");
    }
}