package com.trademaster.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Portfolio Service Application with AgentOS Integration
 * 
 * Comprehensive portfolio management platform providing:
 * - Real-time portfolio valuation using Java 24 Virtual Threads
 * - Position tracking with sub-10ms updates via AgentOS
 * - P&L calculations with structured concurrency patterns
 * - Risk management and portfolio optimization
 * - MCP protocol integration for agent-to-agent communication
 * 
 * AgentOS Integration Features:
 * - Portfolio Agent with 5 expert capabilities
 * - Real-time coordination with Trading and Market Data agents
 * - Structured concurrency for portfolio calculations
 * - Event-driven position updates and risk assessment
 * - Comprehensive health monitoring and performance tracking
 * 
 * Performance Targets:
 * - Portfolio valuation: <50ms response time
 * - Position updates: <10ms processing time via structured concurrency
 * - P&L calculations: <25ms for full portfolio with parallel processing
 * - Risk analytics: <200ms for comprehensive risk assessment
 * - Support 10,000+ concurrent users with Virtual Threads
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + AgentOS)
 * @since 2024
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
@EnableJpaRepositories
@EnableJpaAuditing
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableWebSocket
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        // Enable virtual threads for unlimited scalability
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}