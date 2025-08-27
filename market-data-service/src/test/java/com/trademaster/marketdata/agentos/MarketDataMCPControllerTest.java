package com.trademaster.marketdata.agentos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.service.TechnicalAnalysisService;
import com.trademaster.marketdata.service.MarketScannerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Market Data MCP Controller
 * Tests MCP endpoints and agent communication
 */
@WebMvcTest(MarketDataMCPController.class)
class MarketDataMCPControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MarketDataService marketDataService;

    @MockBean
    private TechnicalAnalysisService technicalAnalysisService;

    @MockBean
    private MarketScannerService marketScannerService;

    @MockBean
    private MarketDataAgent marketDataAgent;

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldGetMarketData() throws Exception {
        // Given
        var symbols = List.of("AAPL", "GOOGL");
        when(marketDataAgent.handleDataRequest(any())).thenReturn(any());

        // When & Then
        mockMvc.perform(post("/mcp/market-data/getMarketData")
                .with(csrf())
                .param("symbols", "AAPL", "GOOGL")
                .param("timeframe", "1D")
                .param("includeIndicators", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldSubscribeToUpdates() throws Exception {
        // Given
        var subscriptionResult = Map.of(
            "subscriptionId", "sub-12345",
            "status", "ACTIVE"
        );
        when(marketDataService.subscribeToRealTimeUpdates(anyList(), any(Integer.class), any()))
            .thenReturn(subscriptionResult);

        // When & Then
        mockMvc.perform(post("/mcp/market-data/subscribeToUpdates")
                .with(csrf())
                .param("symbols", "AAPL", "GOOGL")
                .param("updateFrequency", "1000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBSCRIBED"))
                .andExpect(jsonPath("$.symbols").isArray());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldPerformTechnicalAnalysis() throws Exception {
        // Given
        when(technicalAnalysisService.calculateIndicators(anyList(), anyList()))
            .thenReturn(Map.of("RSI", 65.2, "MACD", 0.15));

        // When & Then
        mockMvc.perform(post("/mcp/market-data/performTechnicalAnalysis")
                .with(csrf())
                .param("symbols", "AAPL")
                .param("indicators", "RSI", "MACD")
                .param("period", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.indicators").isArray());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldScanMarket() throws Exception {
        // Given
        var scanCriteria = Map.of(
            "minVolume", 1000000,
            "priceChange", Map.of("min", 5, "max", 20)
        );
        when(marketScannerService.performScan(any()))
            .thenReturn(List.of("AAPL", "GOOGL", "MSFT"));

        // When & Then
        mockMvc.perform(post("/mcp/market-data/scanMarket")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanCriteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldManagePriceAlerts() throws Exception {
        // Given
        var alertConfig = Map.of(
            "symbol", "AAPL",
            "targetPrice", 150.0,
            "condition", "above"
        );
        when(marketDataService.createPriceAlert(any()))
            .thenReturn(Map.of("alertId", "alert-12345", "status", "ACTIVE"));

        // When & Then
        mockMvc.perform(post("/mcp/market-data/managePriceAlerts")
                .with(csrf())
                .param("action", "CREATE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alertConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.action").value("CREATE"));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldGetAgentHealth() throws Exception {
        // Given
        when(marketDataAgent.getHealthScore()).thenReturn(0.95);
        when(marketDataAgent.getAgentId()).thenReturn("market-data-agent");
        when(marketDataAgent.getAgentType()).thenReturn("MARKET_DATA");
        when(marketDataAgent.getCapabilities()).thenReturn(List.of(
            "REAL_TIME_DATA", "HISTORICAL_DATA", "TECHNICAL_ANALYSIS"
        ));

        // When & Then
        mockMvc.perform(get("/mcp/market-data/getAgentHealth")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("market-data-agent"))
                .andExpect(jsonPath("$.agentType").value("MARKET_DATA"))
                .andExpect(jsonPath("$.healthScore").value(0.95))
                .andExpect(jsonPath("$.status").value("HEALTHY"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/mcp/market-data/getAgentHealth"))
                .andExpect(status().isUnauthorized());
    }
}