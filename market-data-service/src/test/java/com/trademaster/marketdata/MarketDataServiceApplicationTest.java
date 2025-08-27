package com.trademaster.marketdata;

import com.trademaster.marketdata.agentos.MarketDataAgent;
import com.trademaster.marketdata.agentos.MarketDataCapabilityRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Market Data Service Application
 * Tests AgentOS integration and core service functionality
 */
@SpringBootTest
@ActiveProfiles("test")
class MarketDataServiceApplicationTest {

    @Autowired
    private MarketDataAgent marketDataAgent;
    
    @Autowired
    private MarketDataCapabilityRegistry capabilityRegistry;

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
        assertThat(marketDataAgent).isNotNull();
        assertThat(capabilityRegistry).isNotNull();
    }

    @Test
    void agentOSIntegrationTest() {
        // Verify AgentOS components are properly configured
        assertThat(marketDataAgent.getAgentId()).isEqualTo("market-data-agent");
        assertThat(marketDataAgent.getAgentType()).isEqualTo("MARKET_DATA");
        assertThat(marketDataAgent.getCapabilities()).hasSize(5);
        assertThat(marketDataAgent.getHealthScore()).isGreaterThan(0.0);
    }

    @Test
    void capabilityRegistryTest() {
        // Test capability registry functionality
        var initialHealthScore = capabilityRegistry.calculateOverallHealthScore();
        assertThat(initialHealthScore).isEqualTo(1.0); // No executions yet
        
        // Record some metrics
        capabilityRegistry.recordSuccessfulExecution("REAL_TIME_DATA");
        capabilityRegistry.recordExecutionTime("REAL_TIME_DATA", 50L);
        
        var healthScore = capabilityRegistry.getCapabilityHealthScore("REAL_TIME_DATA");
        assertThat(healthScore).isEqualTo(1.0); // 100% success rate
    }
}