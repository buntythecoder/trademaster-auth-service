package com.trademaster.marketdata.agentos;

import com.trademaster.marketdata.dto.MarketDataRequest;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.service.TechnicalAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Market Data Agent
 * Tests agent capabilities and AgentOS integration
 */
@ExtendWith(MockitoExtension.class)
class MarketDataAgentTest {

    @Mock
    private MarketDataService marketDataService;
    
    @Mock
    private TechnicalAnalysisService technicalAnalysisService;
    
    @Mock
    private MarketDataCapabilityRegistry capabilityRegistry;
    
    private MarketDataAgent marketDataAgent;

    @BeforeEach
    void setUp() {
        marketDataAgent = new MarketDataAgent(
            marketDataService, 
            technicalAnalysisService, 
            capabilityRegistry
        );
    }

    @Test
    void shouldReturnCorrectAgentInformation() {
        assertThat(marketDataAgent.getAgentId()).isEqualTo("market-data-agent");
        assertThat(marketDataAgent.getAgentType()).isEqualTo("MARKET_DATA");
        assertThat(marketDataAgent.getCapabilities()).containsExactlyInAnyOrder(
            "REAL_TIME_DATA",
            "HISTORICAL_DATA",
            "TECHNICAL_ANALYSIS",
            "MARKET_SCANNING",
            "PRICE_ALERTS"
        );
    }

    @Test
    void shouldProvideRealTimeDataCapability() {
        // Given
        var symbols = List.of("AAPL", "GOOGL");
        when(marketDataService.getRealTimeData(symbols)).thenReturn(any());
        
        // When
        var future = marketDataAgent.provideRealTimeData(symbols);
        
        // Then
        assertThat(future).isNotNull();
        assertThat(future.join()).contains("Real-time data streaming active");
    }

    @Test
    void shouldProvideHistoricalDataCapability() {
        // Given
        var symbols = List.of("AAPL", "GOOGL");
        var timeframe = "1D";
        when(marketDataService.getHistoricalData(symbols, timeframe)).thenReturn(any());
        
        // When
        var future = marketDataAgent.provideHistoricalData(symbols, timeframe);
        
        // Then
        assertThat(future).isNotNull();
        assertThat(future.join()).contains("Historical data retrieved");
    }

    @Test
    void shouldProvideTechnicalAnalysisCapability() {
        // Given
        var symbols = List.of("AAPL", "GOOGL");
        var indicators = List.of("RSI", "MACD");
        when(technicalAnalysisService.calculateIndicators(symbols, indicators)).thenReturn(any());
        
        // When
        var future = marketDataAgent.provideTechnicalAnalysis(symbols, indicators);
        
        // Then
        assertThat(future).isNotNull();
        assertThat(future.join()).contains("Technical analysis completed");
    }

    @Test
    void shouldHandleMarketDataRequest() {
        // Given
        var request = MarketDataRequest.builder()
            .requestId(12345L)
            .symbols(List.of("AAPL"))
            .timeframe("1D")
            .build();
        
        when(marketDataService.getRealTimeData(anyList())).thenReturn(any());
        when(marketDataService.getHistoricalData(anyList(), any())).thenReturn(any());
        when(technicalAnalysisService.calculateIndicators(anyList(), anyList())).thenReturn(any());
        when(capabilityRegistry.calculateOverallHealthScore()).thenReturn(0.95);
        
        // When
        var future = marketDataAgent.handleDataRequest(request);
        
        // Then
        assertThat(future).isNotNull();
        var response = future.join();
        assertThat(response.getRequestId()).isEqualTo(12345L);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldCalculateHealthScore() {
        // Given
        when(capabilityRegistry.calculateOverallHealthScore()).thenReturn(0.85);
        
        // When
        var healthScore = marketDataAgent.getHealthScore();
        
        // Then
        assertThat(healthScore).isEqualTo(0.85);
    }
}