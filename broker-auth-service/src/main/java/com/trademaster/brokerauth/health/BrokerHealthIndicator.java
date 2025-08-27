package com.trademaster.brokerauth.health;

import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.service.broker.BrokerAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Broker Health Indicator
 * 
 * Monitors the health and availability of all configured broker services.
 * Provides detailed health information for each broker.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrokerHealthIndicator implements HealthIndicator {
    
    private final Map<BrokerType, BrokerAuthService> brokerServices;
    
    @Override
    public Health health() {
        try {
            Map<String, Object> brokerHealthMap = new HashMap<>();
            int healthyBrokers = 0;
            int totalBrokers = brokerServices.size();
            
            for (Map.Entry<BrokerType, BrokerAuthService> entry : brokerServices.entrySet()) {
                BrokerType brokerType = entry.getKey();
                BrokerAuthService service = entry.getValue();
                
                Map<String, Object> brokerHealth = checkBrokerHealth(brokerType, service);
                brokerHealthMap.put(brokerType.name().toLowerCase(), brokerHealth);
                
                if (Boolean.TRUE.equals(brokerHealth.get("healthy"))) {
                    healthyBrokers++;
                }
            }
            
            // Calculate overall health
            double healthPercentage = totalBrokers > 0 ? (double) healthyBrokers / totalBrokers : 0.0;
            boolean isHealthy = healthPercentage >= 0.5; // At least 50% of brokers should be healthy
            
            Health.Builder healthBuilder = isHealthy ? Health.up() : Health.down();
            
            return healthBuilder
                    .withDetail("brokers", brokerHealthMap)
                    .withDetail("healthy_brokers", healthyBrokers)
                    .withDetail("total_brokers", totalBrokers)
                    .withDetail("health_percentage", Math.round(healthPercentage * 100))
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error checking broker health", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
        }
    }
    
    private Map<String, Object> checkBrokerHealth(BrokerType brokerType, BrokerAuthService service) {
        Map<String, Object> health = new HashMap<>();
        LocalDateTime start = LocalDateTime.now();
        
        try {
            // Perform basic service checks
            boolean isConfigured = service != null;
            boolean supportsRefresh = service.supportsTokenRefresh();
            long sessionValiditySeconds = service.getSessionValiditySeconds();
            int maxSessionsPerUser = service.getMaxSessionsPerUser();
            
            Duration checkDuration = Duration.between(start, LocalDateTime.now());
            
            health.put("healthy", isConfigured);
            health.put("supports_refresh", supportsRefresh);
            health.put("session_validity_seconds", sessionValiditySeconds);
            health.put("max_sessions_per_user", maxSessionsPerUser);
            health.put("response_time_ms", checkDuration.toMillis());
            health.put("status", isConfigured ? "UP" : "DOWN");
            
            // Add broker-specific configuration
            switch (brokerType) {
                case ZERODHA -> health.put("auth_type", "OAuth 2.0 + API Key");
                case UPSTOX -> health.put("auth_type", "OAuth 2.0");
                case ANGEL_ONE -> health.put("auth_type", "API Key + TOTP");
                case ICICI_DIRECT -> health.put("auth_type", "Session Token");
                default -> health.put("auth_type", "Unknown");
            }
            
        } catch (Exception e) {
            log.warn("Health check failed for broker {}: {}", brokerType, e.getMessage());
            
            Duration checkDuration = Duration.between(start, LocalDateTime.now());
            
            health.put("healthy", false);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("response_time_ms", checkDuration.toMillis());
        }
        
        return health;
    }
}