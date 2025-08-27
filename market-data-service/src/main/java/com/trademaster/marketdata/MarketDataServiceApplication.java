package com.trademaster.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * TradeMaster Market Data Service Application
 * 
 * Real-time market data integration service providing:
 * - Multi-exchange market data aggregation (NSE, BSE)
 * - WebSocket-based real-time streaming
 * - Apache Kafka event processing
 * - InfluxDB time-series data storage
 * - High-frequency trading data support
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024-08-20
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
@EnableJpaRepositories
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableWebSocket
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        // Set system properties for optimal performance
        System.setProperty("spring.application.name", "trademaster-market-data-service");
        System.setProperty("server.port", "8084");
        
        // Configure JVM for real-time processing
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "8");
        System.setProperty("spring.kafka.consumer.max-poll-records", "1000");
        
        SpringApplication app = new SpringApplication(MarketDataServiceApplication.class);
        
        // Set default profiles
        app.setDefaultProperties(java.util.Map.of(
            "spring.profiles.default", "development",
            "logging.level.com.trademaster.marketdata", "INFO",
            "logging.level.org.apache.kafka", "WARN",
            "logging.level.com.influxdb", "WARN"
        ));
        
        app.run(args);
    }
}