package com.trademaster.portfolio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AgentOS Configuration Properties
 * 
 * Implements Rule #26 (Configuration Synchronization Audit) - MANDATORY config validation.
 * 
 * All AgentOS properties externalized with validation and reasonable defaults.
 * Sync with application.yml agentos.* configuration entries.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Spring Boot 3.5)
 */
@ConfigurationProperties(prefix = "agentos")
@Data
@Validated
public class AgentOSProperties {
    
    @Valid
    @NotNull
    private Agent agent = new Agent();
    
    @Data
    public static class Agent {
        
        /**
         * Agent identifier
         * Default: portfolio-agent
         */
        @NotNull
        private String id = "portfolio-agent";
        
        /**
         * Agent type for orchestration service
         * Default: PORTFOLIO
         */
        @NotNull
        private String type = "PORTFOLIO";
        
        /**
         * Agent capabilities configuration
         */
        @Valid
        @NotNull
        private Map<String, Capability> capabilities = Map.of();
        
        /**
         * Agent health monitoring configuration
         */
        @Valid
        @NotNull
        private Health health = new Health();
    }
    
    @Data
    public static class Capability {
        
        /**
         * Capability proficiency level
         * Values: BASIC, INTERMEDIATE, ADVANCED, EXPERT
         */
        @NotNull
        private String proficiency = "INTERMEDIATE";
        
        /**
         * Maximum concurrent requests for this capability
         */
        @Min(value = 1, message = "Max concurrent requests must be at least 1")
        @Max(value = 10000, message = "Max concurrent requests cannot exceed 10000")
        private Integer maxConcurrentRequests = 100;
        
        /**
         * Timeout in milliseconds for capability operations
         */
        @Min(value = 100, message = "Timeout must be at least 100ms")
        @Max(value = 300000, message = "Timeout cannot exceed 5 minutes")
        private Integer timeoutMs = 5000;
        
        /**
         * Resource requirements for this capability
         */
        @NotNull
        private List<String> resourceRequirements = List.of();
    }
    
    @Data
    public static class Health {
        
        /**
         * Health check interval in seconds
         * Default: 30 seconds
         */
        @Min(value = 5, message = "Health check interval must be at least 5 seconds")
        @Max(value = 300, message = "Health check interval cannot exceed 300 seconds")
        private Integer checkIntervalSeconds = 30;
        
        /**
         * Performance reporting interval in seconds
         * Default: 60 seconds
         */
        @Min(value = 10, message = "Performance window must be at least 10 seconds")
        @Max(value = 600, message = "Performance window cannot exceed 600 seconds")
        private Integer performanceReportingIntervalSeconds = 60;
        
        /**
         * Capability monitoring interval in seconds
         * Default: 120 seconds
         */
        @Min(value = 30, message = "Capability monitoring interval must be at least 30 seconds")
        @Max(value = 600, message = "Capability monitoring interval cannot exceed 600 seconds")
        private Integer capabilityMonitoringIntervalSeconds = 120;
        
        /**
         * Performance window in minutes for health calculations
         * Default: 5 minutes
         */
        @Min(value = 1, message = "Performance window must be at least 1 minute")
        @Max(value = 60, message = "Performance window cannot exceed 60 minutes")
        private Integer performanceWindowMinutes = 5;
        
        /**
         * Minimum health score threshold (0.0-1.0)
         * Default: 0.7 (70%)
         */
        @Min(value = 0, message = "Min health score must be at least 0.0")
        @Max(value = 1, message = "Min health score cannot exceed 1.0")
        private Double minHealthScore = 0.7;
    }
}