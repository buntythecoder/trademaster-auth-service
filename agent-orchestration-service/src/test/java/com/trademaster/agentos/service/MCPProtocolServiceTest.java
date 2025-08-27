package com.trademaster.agentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.config.AgentOSMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ✅ MANDATORY: Unit Tests for MCP Protocol Service
 * 
 * Validates Model Context Protocol implementation
 * Tests Virtual Threads async operations for agent communication
 */
@ExtendWith(MockitoExtension.class)
class MCPProtocolServiceTest {
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private AgentOSMetrics metrics;
    
    @Mock
    private StructuredLoggingService structuredLogger;
    
    @InjectMocks
    private MCPProtocolService mcpProtocolService;
    
    @BeforeEach
    void setUp() {
        when(metrics.startApiTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
    }
    
    /**
     * ✅ TEST: MCP resource registration and listing
     */
    @Test
    void listResources_ShouldReturnRegisteredResources() throws Exception {
        // Given
        MCPProtocolService.MCPResourceContent content = MCPProtocolService.MCPResourceContent.builder()
            .mimeType("application/json")
            .text("{\"data\":\"test\"}")
            .build();
        
        Supplier<MCPProtocolService.MCPResourceContent> contentProvider = () -> content;
        
        mcpProtocolService.registerResource(
            "test://resource/1", 
            "Test Resource", 
            "Test resource for unit testing",
            contentProvider
        );
        
        // When
        CompletableFuture<List<MCPProtocolService.MCPResource>> result = mcpProtocolService.listResources();
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get()).hasSize(1),
            () -> assertThat(result.get().get(0).getUri()).isEqualTo("test://resource/1"),
            () -> assertThat(result.get().get(0).getName()).isEqualTo("Test Resource"),
            () -> verify(structuredLogger).logBusinessTransaction(anyString(), anyString(), anyString(), anyString(), anyMap())
        );
    }
    
    /**
     * ✅ TEST: MCP resource reading
     */
    @Test
    void readResource_ShouldReturnResourceContent() throws Exception {
        // Given
        MCPProtocolService.MCPResourceContent expectedContent = MCPProtocolService.MCPResourceContent.builder()
            .mimeType("application/json")
            .text("{\"market\":\"analysis\"}")
            .build();
        
        Supplier<MCPProtocolService.MCPResourceContent> contentProvider = () -> expectedContent;
        
        mcpProtocolService.registerResource(
            "test://market/data",
            "Market Data",
            "Real-time market analysis data",
            contentProvider
        );
        
        // When
        CompletableFuture<MCPProtocolService.MCPResourceContent> result = mcpProtocolService.readResource("test://market/data");
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get().getMimeType()).isEqualTo("application/json"),
            () -> assertThat(result.get().getText()).isEqualTo("{\"market\":\"analysis\"}"),
            () -> verify(structuredLogger).logDataAccess(eq("mcp_resource"), eq("test://market/data"), eq("read"), eq("system"))
        );
    }
    
    /**
     * ✅ TEST: MCP tool execution
     */
    @Test
    void callTool_ShouldExecuteRegisteredTool() throws Exception {
        // Given
        MCPProtocolService.MCPToolResult expectedResult = MCPProtocolService.MCPToolResult.builder()
            .success(true)
            .message("Tool executed successfully")
            .data("{\"result\":\"analysis_complete\"}")
            .durationMs(150L)
            .build();
        
        MCPProtocolService.MCPToolExecutor executor = params -> expectedResult;
        
        mcpProtocolService.registerTool(
            "market_analyzer",
            "Market Analysis Tool",
            executor
        );
        
        Object parameters = "{\"symbol\":\"BTCUSD\",\"timeframe\":\"1H\"}";
        
        // When
        CompletableFuture<MCPProtocolService.MCPToolResult> result = mcpProtocolService.callTool("market_analyzer", parameters);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get().isSuccess()).isTrue(),
            () -> assertThat(result.get().getMessage()).isEqualTo("Tool executed successfully"),
            () -> assertThat(result.get().getDurationMs()).isEqualTo(150L),
            () -> verify(structuredLogger).logBusinessTransaction(anyString(), eq("market_analyzer"), anyString(), anyString(), anyMap())
        );
    }
    
    /**
     * ✅ TEST: TradeMaster trade order execution
     */
    @Test
    void executeTradeOrder_ShouldExecuteOrderViaMCP() throws Exception {
        // Given
        MCPProtocolService.MCPToolResult toolResult = MCPProtocolService.MCPToolResult.builder()
            .success(true)
            .message("Order executed")
            .data("{\"orderId\":\"12345\",\"status\":\"FILLED\"}")
            .durationMs(200L)
            .build();
        
        MCPProtocolService.MCPToolExecutor executor = params -> toolResult;
        mcpProtocolService.registerTool("trade_execution", "Trade Execution Tool", executor);
        
        MCPProtocolService.MCPTradeOrder order = MCPProtocolService.MCPTradeOrder.builder()
            .orderId("ORDER-123")
            .symbol("BTCUSD")
            .quantity(0.1)
            .side("BUY")
            .orderType("MARKET")
            .build();
        
        // When
        CompletableFuture<MCPProtocolService.MCPExecutionResult> result = mcpProtocolService.executeTradeOrder(order);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get().getOrderId()).isEqualTo("ORDER-123"),
            () -> assertThat(result.get().getStatus()).isEqualTo("EXECUTED"),
            () -> assertThat(result.get().getExecutionTime()).isEqualTo(200L),
            () -> verify(structuredLogger).logBusinessTransaction(anyString(), eq("ORDER-123"), anyString(), anyString(), anyMap())
        );
    }
    
    /**
     * ✅ TEST: Market data analysis via MCP
     */
    @Test
    void analyzeMarketData_ShouldReturnAnalysisInsights() throws Exception {
        // Given
        MCPProtocolService.MCPToolResult toolResult = MCPProtocolService.MCPToolResult.builder()
            .success(true)
            .data(java.util.Map.of(
                "symbol", "BTCUSD",
                "recommendation", "BUY",
                "confidence", 0.85,
                "signals", java.util.List.of("RSI_OVERSOLD", "MACD_BULLISH")
            ))
            .durationMs(300L)
            .build();
        
        MCPProtocolService.MCPMarketInsights expectedInsights = MCPProtocolService.MCPMarketInsights.builder()
            .symbol("BTCUSD")
            .recommendation("BUY")
            .confidence(0.85)
            .signals(java.util.List.of("RSI_OVERSOLD", "MACD_BULLISH"))
            .build();
        
        MCPProtocolService.MCPToolExecutor executor = params -> toolResult;
        mcpProtocolService.registerTool("market_analysis", "Market Analysis Tool", executor);
        
        when(objectMapper.convertValue(any(), eq(MCPProtocolService.MCPMarketInsights.class)))
            .thenReturn(expectedInsights);
        
        MCPProtocolService.MCPAnalysisRequest request = MCPProtocolService.MCPAnalysisRequest.builder()
            .symbol("BTCUSD")
            .timeframe("1H")
            .indicators(java.util.List.of("RSI", "MACD", "BB"))
            .build();
        
        // When
        CompletableFuture<MCPProtocolService.MCPMarketInsights> result = mcpProtocolService.analyzeMarketData(request);
        
        // Then
        assertAll(
            () -> assertThat(result).succeedsWithin(java.time.Duration.ofSeconds(1)),
            () -> assertThat(result.get().getSymbol()).isEqualTo("BTCUSD"),
            () -> assertThat(result.get().getRecommendation()).isEqualTo("BUY"),
            () -> assertThat(result.get().getConfidence()).isEqualTo(0.85),
            () -> verify(structuredLogger).logBusinessTransaction(anyString(), eq("BTCUSD"), anyString(), anyString(), anyMap())
        );
    }
    
    /**
     * ✅ TEST: Resource not found error handling
     */
    @Test
    void readResource_ShouldThrowExceptionWhenNotFound() throws Exception {
        // When & Then
        CompletableFuture<MCPProtocolService.MCPResourceContent> result = mcpProtocolService.readResource("nonexistent://resource");
        
        assertAll(
            () -> assertThat(result).failsWithin(java.time.Duration.ofSeconds(1)),
            () -> verify(metrics).recordError(eq("mcp_read_resource"), anyString()),
            () -> verify(structuredLogger).logError(anyString(), anyString(), any(Exception.class), anyMap())
        );
    }
    
    /**
     * ✅ TEST: Tool not found error handling
     */
    @Test
    void callTool_ShouldThrowExceptionWhenNotFound() throws Exception {
        // When & Then
        CompletableFuture<MCPProtocolService.MCPToolResult> result = mcpProtocolService.callTool("nonexistent_tool", "{}");
        
        assertAll(
            () -> assertThat(result).failsWithin(java.time.Duration.ofSeconds(1)),
            () -> verify(metrics).recordError(eq("mcp_call_tool"), anyString()),
            () -> verify(structuredLogger).logError(anyString(), anyString(), any(Exception.class), anyMap())
        );
    }
}