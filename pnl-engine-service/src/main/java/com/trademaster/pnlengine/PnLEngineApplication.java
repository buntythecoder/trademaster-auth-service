package com.trademaster.pnlengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * TradeMaster Multi-Broker P&L Engine Service
 * 
 * MANDATORY: Java 24 + Virtual Threads + Spring Boot 3.5+ + Zero Placeholders
 * 
 * Enterprise-grade profit and loss calculation engine providing real-time
 * P&L computation across multiple brokers with comprehensive tax optimization
 * and performance attribution analysis.
 * 
 * Core Capabilities:
 * - Real-time multi-broker P&L aggregation (<50ms response times)
 * - Multiple cost basis methods (FIFO, LIFO, Weighted Average, Specific ID)
 * - Advanced performance attribution (security selection, asset allocation, timing)
 * - Tax-optimized realized P&L calculations with audit trails
 * - Risk-adjusted returns (Sharpe ratio, maximum drawdown, volatility)
 * - WebSocket streaming for real-time P&L updates
 * - Comprehensive regulatory compliance reporting
 * 
 * Architecture Features:
 * - Java 24 Virtual Threads for unlimited scalability
 * - Functional programming patterns throughout (zero if-else, zero loops)
 * - Structured concurrency for coordinated parallel operations
 * - Event-driven P&L updates with broker integration
 * - Redis caching for sub-50ms calculation performance
 * - Circuit breakers for external service resilience
 * 
 * Integration Points:
 * - Multi-Broker Service: Position updates and broker data
 * - Market Data Service: Real-time pricing feeds
 * - Portfolio Service: Portfolio composition and holdings
 * - Notification Service: P&L alerts and threshold notifications
 * - AgentOS Framework: Multi-agent coordination and MCP protocol
 * 
 * Performance Targets:
 * - Portfolio P&L calculation: <50ms (cached) / <200ms (live)
 * - Position P&L calculation: <5ms per position
 * - Tax lot processing: <25ms per transaction
 * - Bulk calculations: <100ms for 1000+ positions
 * - Concurrent users: 10,000+ with Virtual Threads
 * - P&L update latency: <100ms broker-to-client
 * 
 * Security & Compliance:
 * - JWT-based authentication and authorization
 * - Comprehensive audit logging for all P&L calculations
 * - Tax compliance for multiple jurisdictions (India, US, EU)
 * - Regulatory reporting capabilities (MiFID II, Dodd-Frank)
 * - Zero Trust security architecture with input validation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Multi-Broker P&L Engine)
 * @since 2024-12-01
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@Slf4j
public class PnLEngineApplication {
    
    /**
     * Application entry point with Java 24 Virtual Threads
     * 
     * MANDATORY: Virtual Threads enabled for unlimited I/O scalability
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Enable Virtual Threads globally
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        // Performance optimizations for P&L calculations
        System.setProperty("spring.jpa.show-sql", "false");
        System.setProperty("spring.jpa.properties.hibernate.generate_statistics", "false");
        
        // Memory optimization for high-volume calculations
        System.setProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "50");
        System.setProperty("spring.cache.redis.time-to-live", "300000"); // 5 minutes
        
        log.info("🚀 Starting TradeMaster P&L Engine Service");
        log.info("📊 Java 24 + Virtual Threads enabled for scalable P&L calculations");
        log.info("💰 Multi-broker P&L aggregation with <50ms response targets");
        log.info("🔄 Real-time streaming updates via WebSocket");
        log.info("🛡️ Zero Trust security with comprehensive audit trails");
        log.info("⚡ Performance: 10,000+ concurrent users, sub-100ms latency");
        
        var context = SpringApplication.run(PnLEngineApplication.class, args);
        
        log.info("✅ TradeMaster P&L Engine Service started successfully");
        log.info("🌐 Service running on: http://localhost:{}/pnl-engine", 
                context.getEnvironment().getProperty("server.port", "8086"));
        log.info("📊 Swagger UI: http://localhost:{}/pnl-engine/swagger-ui.html",
                context.getEnvironment().getProperty("server.port", "8086"));
        log.info("🔍 Health Check: http://localhost:{}/pnl-engine/actuator/health",
                context.getEnvironment().getProperty("server.port", "8086"));
        log.info("📈 Metrics: http://localhost:{}/pnl-engine/actuator/prometheus",
                context.getEnvironment().getProperty("server.port", "8086"));
    }
}