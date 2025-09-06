package com.trademaster.multibroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Multi-Broker Service Application
 * 
 * MANDATORY: Java 24 + Virtual Threads + Spring Boot 3.5.3
 * MANDATORY: Zero Trust Security + Functional Programming + Zero Placeholders
 * 
 * Enterprise-grade multi-broker integration service for TradeMaster platform.
 * Provides secure OAuth-based broker connections, real-time portfolio aggregation,
 * and intelligent order routing across major Indian brokers.
 * 
 * Key Features:
 * - OAuth 2.0 broker authentication (Zerodha, Upstox, Angel One, ICICI Direct)
 * - Real-time portfolio synchronization with Virtual Threads
 * - Circuit breaker patterns for broker resilience
 * - Encrypted token storage with rotation
 * - WebSocket real-time updates
 * - Comprehensive health monitoring
 * 
 * Performance Targets:
 * - Portfolio aggregation: <200ms for 5 brokers
 * - OAuth flow completion: <500ms
 * - WebSocket update latency: <50ms
 * - 99.9% uptime with circuit breakers
 * 
 * Security:
 * - Zero Trust architecture with tiered access control
 * - AES-256 token encryption at rest
 * - JWT-based authentication
 * - Rate limiting and abuse prevention
 * - Comprehensive audit logging
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Multi-Broker Integration)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class MultiBrokerServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MultiBrokerServiceApplication.class);
        
        // Enable Virtual Threads for enterprise performance
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        // Configure JVM for preview features (Java 24 Pattern Matching, Virtual Threads)
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "200");
        
        application.run(args);
    }
}