package com.trademaster.mlinfra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * ML Infrastructure Platform Application
 * 
 * Enterprise-grade MLOps platform providing:
 * - Experiment tracking with MLflow
 * - Feature store with Redis/PostgreSQL
 * - Model serving with <50ms latency
 * - Real-time data pipelines with Kafka
 * - Kubernetes-native deployment
 * 
 * Built with Java 24 Virtual Threads for optimal performance.
 */
@Slf4j
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class MLInfrastructurePlatformApplication {

    private static final String APPLICATION_NAME = "ML Infrastructure Platform";
    private static final String VERSION = "1.0.0";
    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final String VIRTUAL_THREADS_ENABLED = System.getProperty("spring.threads.virtual.enabled", "false");

    public static void main(String[] args) {
        logStartupBanner();
        validateVirtualThreads();
        
        var application = new SpringApplication(MLInfrastructurePlatformApplication.class);
        application.setDefaultProperties(getDefaultProperties());
        
        var context = application.run(args);
        logStartupComplete(context);
    }

    private static void logStartupBanner() {
        log.info("");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  {} v{}", APPLICATION_NAME, VERSION);
        log.info("  Production-Grade MLOps Platform for TradeMaster");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  Java Version: {}", JAVA_VERSION);
        log.info("  Virtual Threads: {}", VIRTUAL_THREADS_ENABLED);
        log.info("  Spring Profile: {}", System.getProperty("spring.profiles.active", "default"));
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("");
    }

    private static void validateVirtualThreads() {
        if (!"true".equals(VIRTUAL_THREADS_ENABLED)) {
            log.warn("⚠️  Virtual Threads not enabled! Add -Dspring.threads.virtual.enabled=true");
            log.warn("⚠️  Performance will be significantly degraded without Virtual Threads");
        } else {
            log.info("✅ Virtual Threads enabled - optimal performance mode activated");
        }
    }

    private static void logStartupComplete(org.springframework.context.ConfigurableApplicationContext context) {
        var environment = context.getEnvironment();
        var port = environment.getProperty("server.port", "8080");
        var contextPath = environment.getProperty("server.servlet.context-path", "");
        
        log.info("");
        log.info("✅ {} started successfully!", APPLICATION_NAME);
        log.info("🌍 Application URL: http://localhost:{}{}", port, contextPath);
        log.info("📊 Health Check: http://localhost:{}{}/actuator/health", port, contextPath);
        log.info("📈 Metrics: http://localhost:{}{}/actuator/prometheus", port, contextPath);
        log.info("🤖 MLflow UI: http://localhost:5000 (when MLflow is running)");
        log.info("📋 Grafana: http://localhost:3000 (when monitoring stack is deployed)");
        log.info("");
        log.info("Ready to serve ML workloads with enterprise-grade reliability! 🚀");
        log.info("");
    }

    private static java.util.Properties getDefaultProperties() {
        var properties = new java.util.Properties();
        properties.setProperty("spring.threads.virtual.enabled", "true");
        properties.setProperty("server.compression.enabled", "true");
        properties.setProperty("server.http2.enabled", "true");
        properties.setProperty("spring.jpa.open-in-view", "false");
        properties.setProperty("spring.kafka.producer.acks", "all");
        properties.setProperty("spring.kafka.producer.retries", "3");
        properties.setProperty("spring.kafka.consumer.auto-offset-reset", "earliest");
        properties.setProperty("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        properties.setProperty("management.endpoint.health.show-details", "when-authorized");
        properties.setProperty("management.metrics.export.prometheus.enabled", "true");
        properties.setProperty("logging.level.com.trademaster", "INFO");
        properties.setProperty("logging.pattern.console", "%d{HH:mm:ss.SSS} [%thread] %-5level [%logger{36}] - %msg%n");
        return properties;
    }
}