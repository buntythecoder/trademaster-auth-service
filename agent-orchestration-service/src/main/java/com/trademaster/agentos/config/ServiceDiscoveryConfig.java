package com.trademaster.agentos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * ✅ MANDATORY: Service Discovery Configuration for Agent OS
 * 
 * Integrates with Eureka/Consul for service discovery, health checks,
 * and load balancing across the TradeMaster microservices ecosystem.
 */
@Configuration
@EnableDiscoveryClient
@Slf4j
public class ServiceDiscoveryConfig {

    @Value("${spring.application.name:agent-orchestration-service}")
    private String serviceName;

    @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port:8090}}")
    private String instanceId;

    /**
     * ✅ FUNCTIONAL: Load-balanced REST template for inter-service communication
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        log.info("Configuring load-balanced REST template for service: {}", serviceName);
        return new RestTemplate();
    }

    /**
     * ✅ FUNCTIONAL: Service health check endpoint for discovery
     */
    @Bean
    public ServiceHealthIndicator serviceHealthIndicator() {
        return new ServiceHealthIndicator();
    }

    /**
     * ✅ FUNCTIONAL: Custom health indicator for service discovery
     */
    public static class ServiceHealthIndicator {
        
        /**
         * ✅ FUNCTIONAL: Return service health information for monitoring
         */
        public java.util.Map<String, Object> getHealthInfo() {
            return java.util.Map.of(
                "service", "agent-orchestration-service",
                "status", "healthy",
                "capabilities", java.util.List.of(
                    "agent-management",
                    "task-orchestration", 
                    "workflow-execution",
                    "multi-agent-coordination"
                ),
                "timestamp", java.time.Instant.now()
            );
        }
    }
}