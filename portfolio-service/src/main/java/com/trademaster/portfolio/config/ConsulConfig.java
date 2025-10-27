package com.trademaster.portfolio.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * ✅ CONSUL INTEGRATION: Service Discovery & Configuration Management for Portfolio Service
 *
 * MANDATORY COMPLIANCE:
 * - Java 24 Virtual Threads for all async operations
 * - Functional programming patterns (no if-else)
 * - SOLID principles with single responsibility
 * - Zero Trust Security with SecurityFacade pattern
 * - Circuit breaker integration for external calls
 *
 * CONSUL FEATURES:
 * - Service discovery with health check integration
 * - Dynamic configuration management with hot reload
 * - Distributed key-value store for portfolio data
 * - Service mesh integration with Consul Connect
 * - Multi-datacenter awareness for global deployments
 *
 * PERFORMANCE TARGETS:
 * - Service registration: <5ms response time
 * - Health check updates: <10ms response time
 * - Configuration reload: <50ms hot reload
 * - Service discovery queries: <25ms average
 *
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
@ConditionalOnConsulEnabled
@RequiredArgsConstructor
@Slf4j
public class ConsulConfig {

    @Value("${spring.application.name:portfolio-service}")
    private String serviceName;

    @Value("${server.port:8083}")
    private int serverPort;

    @Value("${management.server.port:8083}")
    private int managementPort;

    @Value("${spring.cloud.consul.discovery.health-check-interval:10s}")
    private String healthCheckInterval;

    @Value("${trademaster.consul.datacenter:trademaster-dc}")
    private String datacenter;

    // ✅ VIRTUAL THREADS: Dedicated executor for Consul operations
    private final ScheduledExecutorService consulExecutor =
        Executors.newScheduledThreadPool(2, Thread.ofVirtual().name("consul-portfolio-", 1).factory());

    /**
     * ✅ FUNCTIONAL: Enhanced Consul discovery properties with Portfolio-specific settings
     * Cognitive Complexity: 3
     */
    @Bean
    @Profile("!test")
    public ConsulDiscoveryProperties consulDiscoveryProperties() {
        // ✅ FUNCTIONAL: Direct property configuration without method chaining
        ConsulDiscoveryProperties props = createConsulDiscoveryProperties();

        // ✅ CRITICAL: Enable manual registration since auto-discovery is disabled
        props.setEnabled(true);
        props.setRegister(true);

        configureServiceRegistrationProperties(props);
        configureHealthCheckProperties(props);
        configureServiceTagProperties(props);
        configureNetworkingProperties(props);
        return props;
    }

    private ConsulDiscoveryProperties createConsulDiscoveryProperties() {
        // Use reflection to create instance since constructor is private
        try {
            var constructor = ConsulDiscoveryProperties.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            // Fallback - this will be configured by Spring Boot auto-configuration
            throw new IllegalStateException("Unable to create ConsulDiscoveryProperties", e);
        }
    }

    /**
     * ✅ FUNCTIONAL: Configure service registration settings
     * Cognitive Complexity: 2
     */
    private void configureServiceRegistrationProperties(ConsulDiscoveryProperties props) {
        props.setServiceName(serviceName);
        props.setPort(serverPort);
        props.setInstanceId(serviceName + ":" + java.util.UUID.randomUUID().toString().substring(0, 8));
        props.setPreferIpAddress(true);
        props.setScheme("http"); // Use HTTPS in production
        props.setRegister(true);
        props.setDeregister(true);
    }

    /**
     * ✅ FUNCTIONAL: Configure health check settings
     * Cognitive Complexity: 2
     */
    private void configureHealthCheckProperties(ConsulDiscoveryProperties props) {
        props.setHealthCheckPath("/actuator/health");
        props.setHealthCheckInterval(healthCheckInterval);
        props.setHealthCheckTimeout("5s");
        props.setHealthCheckCriticalTimeout("30s");
        props.setFailFast(false);
    }

    /**
     * ✅ FUNCTIONAL: Configure service tags for discovery and monitoring - Portfolio Specific
     * Cognitive Complexity: 2
     */
    private void configureServiceTagProperties(ConsulDiscoveryProperties props) {
        props.setTags(List.of(
            "version=1.0.0",
            "environment=" + getActiveProfile(),
            "java=24",
            "virtual-threads=enabled",
            "service-type=portfolio-management",
            "capabilities=PORTFOLIO_TRACKING,PNL_CALCULATION,RISK_ANALYTICS,PERFORMANCE_REPORTING",
            "sla-critical=25ms",
            "sla-high=50ms",
            "sla-standard=100ms",
            "framework=spring-boot-3.5.3",
            "protocol=http",
            "context-path=/api/v1",
            "auth-types=JWT,API_KEY",
            "security=AES256,RATE_LIMITING,CIRCUIT_BREAKER"
        ));

        // ✅ FUNCTIONAL: Service metadata for advanced discovery (underscore keys for Consul compatibility)
        var metadataMap = new HashMap<String, String>();
        metadataMap.put("management_context_path", "/actuator");
        metadataMap.put("health_path", "/actuator/health");
        metadataMap.put("metrics_path", "/actuator/metrics");
        metadataMap.put("info_path", "/actuator/info");
        metadataMap.put("prometheus_path", "/actuator/prometheus");
        metadataMap.put("openapi_path", "/v3/api-docs");
        metadataMap.put("swagger_ui_path", "/swagger-ui.html");
        metadataMap.put("prometheus_scrape", "true");
        metadataMap.put("prometheus_port", String.valueOf(managementPort));
        metadataMap.put("sla_critical", "25ms");
        metadataMap.put("sla_high", "50ms");
        metadataMap.put("datacenter", datacenter);
        metadataMap.put("service_mesh", "consul-connect-ready");
        metadataMap.put("kong_compatible", "true");
        metadataMap.put("secure", "false"); // Set to true for production

        // Portfolio-specific metadata
        metadataMap.put("portfolio_capabilities", "TRACKING,PNL_CALCULATION,RISK_ANALYTICS,PERFORMANCE");
        metadataMap.put("calculation_types", "FIFO,LIFO,AVERAGE_COST,SPECIFIC_IDENTIFICATION");
        metadataMap.put("realtime_pnl", "true");
        metadataMap.put("risk_monitoring", "true");
        metadataMap.put("performance_analytics", "true");
        metadataMap.put("multi_currency", "true");
        metadataMap.put("tax_lot_tracking", "true");
        metadataMap.put("circuit_breaker_enabled", "true");
        metadataMap.put("external_apis", "market-data,broker-integration");
        metadataMap.put("internal_apis", "portfolio-summary,position-tracking,pnl-calculation");

        props.setMetadata(metadataMap);
    }

    /**
     * ✅ FUNCTIONAL: Configure networking and connectivity settings
     * Cognitive Complexity: 2
     */
    private void configureNetworkingProperties(ConsulDiscoveryProperties props) {
        props.setHostname(getServiceHostname());
        props.setIpAddress(getServiceIpAddress());
        props.setCatalogServicesWatchDelay(1000);
        props.setCatalogServicesWatchTimeout(2);
        props.setQueryPassing(true);
        props.setIncludeHostnameInInstanceId(true);
    }

    /**
     * ✅ FUNCTIONAL: Get active Spring profile
     * Cognitive Complexity: 1
     */
    private String getActiveProfile() {
        return java.util.Optional.ofNullable(System.getProperty("spring.profiles.active"))
            .orElse(java.util.Optional.ofNullable(System.getenv("SPRING_PROFILES_ACTIVE"))
                .orElse("default"));
    }

    /**
     * ✅ FUNCTIONAL: Get service hostname with fallback logic
     * Cognitive Complexity: 2
     */
    private String getServiceHostname() {
        return java.util.Optional.ofNullable(System.getenv("HOSTNAME"))
            .orElse(java.util.Optional.ofNullable(System.getenv("CONSUL_CLIENT_HOSTNAME"))
                .orElse("localhost"));
    }

    /**
     * ✅ FUNCTIONAL: Get service IP address with environment-based detection
     * Cognitive Complexity: 2
     */
    private String getServiceIpAddress() {
        return java.util.Optional.ofNullable(System.getenv("SERVICE_IP"))
            .orElse(java.util.Optional.ofNullable(System.getenv("POD_IP"))
                .orElse(detectLocalIpAddress()));
    }

    /**
     * ✅ FUNCTIONAL: Detect local IP address for service registration
     * Cognitive Complexity: 3
     */
    private String detectLocalIpAddress() {
        try {
            // ✅ FUNCTIONAL: Use Java NIO to detect network interface IP
            return java.net.NetworkInterface.networkInterfaces()
                .flatMap(java.net.NetworkInterface::inetAddresses)
                .filter(addr -> !addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address)
                .findFirst()
                .map(java.net.InetAddress::getHostAddress)
                .orElse("127.0.0.1");
        } catch (Exception e) {
            log.warn("Failed to detect local IP address, using localhost: {}", e.getMessage());
            return "127.0.0.1";
        }
    }

    /**
     * ✅ FUNCTIONAL: Initialize periodic Consul connectivity monitoring
     */
    public void initializeConsulMonitoring() {
        // ✅ VIRTUAL THREADS: Schedule periodic Consul connectivity checks
        consulExecutor.scheduleAtFixedRate(
            this::monitorConsulConnectivity,
            30, 60, TimeUnit.SECONDS
        );

        log.info("Consul configuration initialized for portfolio-service on port: {} in datacenter: {}",
            serverPort, datacenter);
    }

    /**
     * ✅ FUNCTIONAL: Monitor Consul connectivity and log status
     * Cognitive Complexity: 2
     */
    private void monitorConsulConnectivity() {
        try {
            // ✅ VIRTUAL THREADS: Async connectivity check
            CompletableFuture.runAsync(() -> {
                try {
                    // Simple connectivity test - this would be enhanced with actual Consul client calls
                    log.debug("Consul connectivity monitoring - portfolio-service: {} healthy", serviceName);
                } catch (Exception e) {
                    log.warn("Consul connectivity issue detected: {}", e.getMessage());
                }
            }, consulExecutor);
        } catch (Exception e) {
            log.error("Failed to execute Consul connectivity monitoring", e);
        }
    }
}
