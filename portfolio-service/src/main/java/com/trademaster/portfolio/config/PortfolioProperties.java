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

/**
 * Portfolio Service Configuration Properties
 * 
 * Implements Rule #26 (Configuration Synchronization Audit) - MANDATORY config validation.
 * 
 * All portfolio service properties externalized with validation and reasonable defaults.
 * Sync with application.yml trademaster.portfolio.* configuration entries.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Spring Boot 3.5)
 */
@ConfigurationProperties(prefix = "trademaster.portfolio")
@Data
@Validated
public class PortfolioProperties {
    
    @Valid
    @NotNull
    private PnL pnl = new PnL();
    
    @Valid
    @NotNull
    private Risk risk = new Risk();
    
    @Valid
    @NotNull
    private Performance performance = new Performance();
    
    @Valid
    @NotNull
    private Cache cache = new Cache();
    
    @Valid
    @NotNull
    private Integration integration = new Integration();
    
    @Data
    public static class PnL {
        
        /**
         * P&L calculation interval
         * Default: PT1M (1 minute)
         */
        @NotNull
        private Duration calculationInterval = Duration.ofMinutes(1);
        
        /**
         * Batch size for P&L calculations
         * Default: 100
         */
        @Min(value = 10, message = "PnL batch size must be at least 10")
        @Max(value = 1000, message = "PnL batch size cannot exceed 1000")
        private Integer batchSize = 100;
        
        /**
         * Enable real-time P&L updates
         * Default: true
         */
        @NotNull
        private Boolean enableRealTimeUpdates = true;
    }
    
    @Data
    public static class Risk {
        
        /**
         * VaR confidence level (0.0-1.0)
         * Default: 0.95 (95%)
         */
        @Min(value = 0, message = "VaR confidence level must be at least 0.0")
        @Max(value = 1, message = "VaR confidence level cannot exceed 1.0")
        private Double varConfidenceLevel = 0.95;
        
        /**
         * Stress test scenarios to run
         */
        @NotNull
        private List<String> stressTestScenarios = List.of("MARKET_CRASH", "INTEREST_RATE_SHOCK", "CURRENCY_DEVALUATION");
        
        /**
         * Maximum position concentration limit (0.0-1.0)
         * Default: 0.20 (20% max position)
         */
        @Min(value = 0, message = "Concentration limit must be at least 0.0")
        @Max(value = 1, message = "Concentration limit cannot exceed 1.0")
        private Double concentrationLimit = 0.20;
    }
    
    @Data
    public static class Performance {
        
        /**
         * Enable virtual threads for performance operations
         * Default: true
         */
        @NotNull
        private Boolean enableVirtualThreads = true;
        
        /**
         * Maximum concurrent calculations
         * Default: 1000
         */
        @Min(value = 10, message = "Max concurrent calculations must be at least 10")
        @Max(value = 10000, message = "Max concurrent calculations cannot exceed 10000")
        private Integer maxConcurrentCalculations = 1000;
        
        /**
         * Calculation timeout duration
         * Default: PT30S (30 seconds)
         */
        @NotNull
        private Duration calculationTimeout = Duration.ofSeconds(30);
    }
    
    @Data
    public static class Cache {
        
        /**
         * Portfolio cache TTL
         * Default: PT5M (5 minutes)
         */
        @NotNull
        private Duration portfolioTtl = Duration.ofMinutes(5);
        
        /**
         * Position cache TTL
         * Default: PT1M (1 minute)
         */
        @NotNull
        private Duration positionTtl = Duration.ofMinutes(1);
        
        /**
         * Analytics cache TTL
         * Default: PT15M (15 minutes)
         */
        @NotNull
        private Duration analyticsTtl = Duration.ofMinutes(15);
    }
    
    @Data
    public static class Integration {
        
        @Valid
        @NotNull
        private Service tradingService = new Service();
        
        @Valid
        @NotNull
        private Service marketDataService = new Service();
        
        @Valid
        @NotNull
        private Service authService = new Service();
        
        @Data
        public static class Service {
            
            /**
             * Service URL
             */
            @NotNull
            private String url = "http://localhost:8080";
            
            /**
             * Service timeout duration
             */
            @NotNull
            private Duration timeout = Duration.ofSeconds(10);
            
            /**
             * Number of retry attempts
             * Default: 3
             */
            @Min(value = 0, message = "Retry attempts must be at least 0")
            @Max(value = 10, message = "Retry attempts cannot exceed 10")
            private Integer retryAttempts = 3;
        }
    }
}