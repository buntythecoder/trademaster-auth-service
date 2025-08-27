package com.trademaster.userprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * User Profile Service Application with AgentOS Integration
 * 
 * Comprehensive user management platform providing:
 * - Complete user lifecycle management with Java 24 Virtual Threads
 * - KYC compliance and document processing via AgentOS
 * - Multi-broker configuration and management
 * - User preferences and personalization
 * - MCP protocol integration for agent-to-agent communication
 * 
 * AgentOS Integration Features:
 * - User Profile Agent with 5 expert capabilities
 * - Real-time coordination with Authentication and Trading agents
 * - Structured concurrency for user operations
 * - Event-driven KYC processing and compliance monitoring
 * - Comprehensive health monitoring and performance tracking
 * 
 * Performance Targets:
 * - User profile queries: <25ms response time
 * - KYC processing: <5 minutes for automated validation
 * - Document upload: <2 seconds for 10MB files
 * - Broker configuration: <100ms for connection validation
 * - Support 50,000+ concurrent users with Virtual Threads
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + AgentOS)
 * @since 2024
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaAuditing
@EnableJpaRepositories
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableWebSocket
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class UserProfileServiceApplication {
    
    public static void main(String[] args) {
        // Enable virtual threads for unlimited scalability
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(UserProfileServiceApplication.class, args);
    }
}
