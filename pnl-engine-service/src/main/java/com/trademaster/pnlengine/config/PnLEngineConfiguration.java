package com.trademaster.pnlengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Externalized P&L Engine Configuration
 * 
 * MANDATORY: Java 24 + Configuration Properties + Validation
 * 
 * Centralizes all configuration properties for the P&L Engine service
 * with validation and type safety.
 * 
 * Single Responsibility: Configuration management and validation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
@Configuration
@ConfigurationProperties(prefix = "pnl")
@Data
@Validated
public final class PnLEngineConfiguration {
    
    /**
     * Calculation-related configuration
     */
    @NotNull
    private Calculation calculation = new Calculation();
    
    /**
     * Broker integration configuration
     */
    @NotNull
    private Broker broker = new Broker();
    
    /**
     * Security configuration
     */
    @NotNull
    private Security security = new Security();
    
    /**
     * Tax optimization configuration
     */
    @NotNull
    private Tax tax = new Tax();
    
    /**
     * Analytics configuration
     */
    @NotNull
    private Analytics analytics = new Analytics();
    
    /**
     * Persistence configuration
     */
    @NotNull
    private Persistence persistence = new Persistence();
    
    @Data
    public static class Calculation {
        @Min(1000)
        @Max(60000)
        private long timeoutMs = 5000L;
        
        @Min(2)
        @Max(20)
        private int precision = 10;
        
        private boolean parallelEnabled = true;
        private boolean cacheEnabled = true;
        private boolean asyncEnabled = true;
    }
    
    @Data
    public static class Broker {
        @Min(1000)
        @Max(30000)
        private long connectionTimeoutMs = 3000L;
        
        @Min(100)
        @Max(10000)
        private int maxRetryAttempts = 3;
        
        @Min(500)
        @Max(30000)
        private long retryDelayMs = 1000L;
        
        private boolean circuitBreakerEnabled = true;
    }
    
    @Data
    public static class Security {
        @Min(0.1)
        @Max(1.0)
        private double riskThreshold = 0.8;
        
        @Min(10)
        @Max(1000)
        private int rateLimitMaxRequests = 100;
        
        @Min(1)
        @Max(24)
        private int sessionMaxDurationHours = 8;
        
        private boolean auditEnabled = true;
        private boolean encryptionEnabled = true;
    }
    
    @Data
    public static class Tax {
        @NotNull
        @Min(0.0)
        @Max(0.5)
        private BigDecimal stcgRate = BigDecimal.valueOf(0.15);
        
        @NotNull
        @Min(0.0)
        @Max(0.5)
        private BigDecimal ltcgRate = BigDecimal.valueOf(0.10);
        
        @NotNull
        @Min(0)
        @Max(1000000)
        private BigDecimal ltcgExemptionLimit = BigDecimal.valueOf(100000);
        
        @Min(1)
        @Max(1095)
        private int holdingPeriodDays = 365;
    }
    
    @Data
    public static class Analytics {
        @NotNull
        @Min(0.0)
        @Max(0.2)
        private BigDecimal riskFreeRate = BigDecimal.valueOf(0.06);
        
        @Min(200)
        @Max(300)
        private int tradingDaysPerYear = 252;
        
        @NotNull
        @Min(0.5)
        @Max(0.99)
        private BigDecimal confidenceLevel = BigDecimal.valueOf(0.95);
        
        private boolean performanceTrackingEnabled = true;
    }
    
    @Data
    public static class Persistence {
        @Min(10)
        @Max(1000)
        private int batchSize = 100;
        
        @Min(30)
        @Max(3650)
        private int retentionDays = 365;
        
        private boolean asyncEnabled = true;
        private boolean compressionEnabled = false;
        
        @Min(1000)
        @Max(30000)
        private long queryTimeoutMs = 5000L;
    }
    
    @Data
    public static class Sector {
        @NotNull
        @Min(0.001)
        @Max(0.1)
        private BigDecimal minAllocation = BigDecimal.valueOf(0.01);
        
        @NotNull
        @Min(0.1)
        @Max(1.0)
        private BigDecimal maxConcentration = BigDecimal.valueOf(0.40);
        
        private boolean diversificationAnalysisEnabled = true;
    }
    
    @Data
    public static class Position {
        @NotNull
        @Min(0.001)
        @Max(1.0)
        private BigDecimal minQuantity = BigDecimal.valueOf(0.001);
        
        @Min(0.01)
        @Max(1.0)
        private double maxPriceVariance = 0.15;
        
        private boolean realTimeValuationEnabled = true;
    }
}