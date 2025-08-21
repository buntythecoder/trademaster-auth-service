package com.trademaster.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Portfolio Management Service
 * 
 * Comprehensive portfolio management platform providing:
 * - Real-time portfolio valuation using Java 24 Virtual Threads
 * - Position tracking with sub-10ms updates
 * - P&L calculations with comprehensive analytics
 * - Risk management and portfolio optimization
 * - Integration with Trading and Market Data services
 * 
 * Key Features:
 * - Java 24 Virtual Threads for unlimited scalability
 * - Real-time position updates from order executions
 * - Comprehensive P&L tracking (realized/unrealized)
 * - Advanced portfolio analytics and risk metrics
 * - Multi-currency portfolio support
 * 
 * Performance Targets:
 * - Portfolio valuation: <50ms response time
 * - Position updates: <10ms processing time
 * - P&L calculations: <25ms for full portfolio
 * - Support 10,000+ concurrent users
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}