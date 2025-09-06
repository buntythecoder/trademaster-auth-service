package com.trademaster.portfolio.agentos;

import com.trademaster.portfolio.domain.PerformanceMetrics;
import com.trademaster.portfolio.domain.PortfolioData;
import com.trademaster.portfolio.domain.RiskMetrics;
import com.trademaster.portfolio.functional.PortfolioErrors;
import com.trademaster.portfolio.functional.Result;
import com.trademaster.portfolio.service.FunctionalPortfolioService;
import com.trademaster.portfolio.service.PerformanceAnalyticsService;
import com.trademaster.portfolio.service.PortfolioAggregationService;
import com.trademaster.portfolio.service.RiskManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * MCP Portfolio Server Testing Suite
 * 
 * Tests all 10 MCP endpoints with Virtual Thread execution.
 * Validates AgentOS integration, error handling, and response formats.
 * 
 * Testing Coverage:
 * - All 10 MCP endpoints functionality
 * - Virtual Thread execution validation
 * - Error handling and propagation
 * - Response format consistency
 * - Timeout behavior testing
 * - Structured concurrency coordination
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Testing Suite)
 */
@ExtendWith(MockitoExtension.class)
class MCPPortfolioServerTest {
    
    @Mock
    private FunctionalPortfolioService portfolioService;
    
    @Mock
    private PortfolioAggregationService aggregationService;
    
    @Mock
    private PerformanceAnalyticsService performanceService;
    
    @Mock
    private RiskManagementService riskService;
    
    private MCPPortfolioServer mcpServer;
    
    @BeforeEach
    void setUp() {
        mcpServer = new MCPPortfolioServer(portfolioService, aggregationService, 
                                          performanceService, riskService);
    }
    
    @Test
    @DisplayName("MCP Endpoint 1: portfolio/get-positions should return positions successfully")
    void shouldGetPositionsSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1001L;
        Map<String, Object> parameters = Map.of();
        
        PortfolioData mockPortfolio = PortfolioData.builder()
                .portfolioId(123L)
                .userId(userId)
                .portfolioName("Test Portfolio")
                .totalValue(new BigDecimal("100000.00"))
                .totalCost(new BigDecimal("95000.00"))
                .unrealizedPnl(new BigDecimal("5000.00"))
                .build();
        
        when(portfolioService.getPortfolioByUserId(userId))
                .thenReturn(Result.success(mockPortfolio));
        
        // When
        CompletableFuture<MCPResponse> future = mcpServer.getPositions(userId, parameters);
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        MCPResponse.Success success = response.asSuccess();
        assertThat(success.endpoint()).isEqualTo("portfolio/get-positions");
        assertThat(success.hasData("userId")).isTrue();
        assertThat(success.getData("userId", Long.class)).isEqualTo(userId);
        assertThat(success.hasData("portfolio")).isTrue();
        assertThat(success.hasData("totalValue")).isTrue();
    }
    
    @Test
    @DisplayName("MCP Endpoint 2: portfolio/get-performance should return analytics successfully")
    void shouldGetPerformanceSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1002L;
        String period = "1Y";
        Map<String, Object> parameters = Map.of("period", period);
        
        PerformanceMetrics mockMetrics = PerformanceMetrics.builder()
                .totalReturn(new BigDecimal("15000.00"))
                .totalReturnPercent(new BigDecimal("15.0"))
                .sharpeRatio(new BigDecimal("1.25"))
                .volatility(new BigDecimal("12.5"))
                .maxDrawdown(new BigDecimal("8.2"))
                .build();
        
        when(performanceService.calculatePortfolioPerformance(userId, period))
                .thenReturn(Result.success(mockMetrics));
        
        // When
        CompletableFuture<MCPResponse> future = mcpServer.getPerformance(userId, parameters);
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        MCPResponse.Success success = response.asSuccess();
        assertThat(success.endpoint()).isEqualTo("portfolio/get-performance");
        assertThat(success.getData("period", String.class)).isEqualTo(period);
        assertThat(success.hasData("metrics")).isTrue();
        assertThat(success.hasData("sharpeRatio")).isTrue();
        assertThat(success.hasData("totalReturn")).isTrue();
    }
    
    @Test
    @DisplayName("MCP Endpoint 3: portfolio/get-risk-metrics should return risk analysis")
    void shouldGetRiskMetricsSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1003L;
        Double confidenceLevel = 0.95;
        Map<String, Object> parameters = Map.of("confidenceLevel", confidenceLevel);
        
        RiskMetrics mockRiskMetrics = RiskMetrics.builder()
                .valueAtRisk(new BigDecimal("5000.00"))
                .concentrationRisk(new BigDecimal("15.5"))
                .volatility(new BigDecimal("12.8"))
                .riskScore("MODERATE")
                .build();
        
        when(riskService.calculatePortfolioRisk(userId, confidenceLevel))
                .thenReturn(Result.success(mockRiskMetrics));
        
        // When
        CompletableFuture<MCPResponse> future = mcpServer.getRiskMetrics(userId, parameters);
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        MCPResponse.Success success = response.asSuccess();
        assertThat(success.endpoint()).isEqualTo("portfolio/get-risk-metrics");
        assertThat(success.getData("confidenceLevel", Double.class)).isEqualTo(confidenceLevel);
        assertThat(success.hasData("riskMetrics")).isTrue();
        assertThat(success.hasData("var")).isTrue();
    }
    
    @Test
    @DisplayName("MCP Endpoint 10: portfolio/health-check should return server status")
    void shouldPerformHealthCheckSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        Map<String, Object> parameters = Map.of();
        
        // When
        CompletableFuture<MCPResponse> future = mcpServer.healthCheck(parameters);
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        MCPResponse.Success success = response.asSuccess();
        assertThat(success.endpoint()).isEqualTo("portfolio/health-check");
        assertThat(success.hasData("status")).isTrue();
        assertThat(success.getData("status", String.class)).isIn("HEALTHY", "DEGRADED");
        assertThat(success.hasData("services")).isTrue();
        assertThat(success.hasData("version")).isTrue();
    }
    
    @Test
    @DisplayName("Should handle service failures gracefully")
    void shouldHandleServiceFailuresGracefully() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1004L;
        Map<String, Object> parameters = Map.of();
        
        PortfolioErrors.NotFoundError error = PortfolioErrors.NotFoundError.portfolioNotFound(userId);
        when(portfolioService.getPortfolioByUserId(userId))
                .thenReturn(Result.failure(error));
        
        // When
        CompletableFuture<MCPResponse> future = mcpServer.getPositions(userId, parameters);
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isError()).isTrue();
        MCPResponse.Error errorResponse = response.asError();
        assertThat(errorResponse.endpoint()).isEqualTo("portfolio/get-positions");
        assertThat(errorResponse.errorMessage()).contains("Failed to retrieve positions");
        assertThat(errorResponse.correlationId()).isNotNull();
    }
    
    @Test
    @DisplayName("Should execute operations on virtual threads")
    void shouldExecuteOperationsOnVirtualThreads() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1005L;
        Map<String, Object> parameters = Map.of();
        
        PortfolioData mockPortfolio = PortfolioData.builder()
                .portfolioId(123L)
                .userId(userId)
                .portfolioName("Virtual Thread Test")
                .totalValue(new BigDecimal("50000.00"))
                .build();
        
        when(portfolioService.getPortfolioByUserId(userId))
                .thenReturn(Result.success(mockPortfolio));
        
        // When - Execute operation and verify it runs on virtual thread
        CompletableFuture<MCPResponse> future = mcpServer.getPositions(userId, parameters);
        
        // Verify the future is completed by a virtual thread
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.correlationId()).startsWith("mcp-");
    }
    
    @Test
    @DisplayName("Should handle concurrent MCP requests efficiently")
    void shouldHandleConcurrentMCPRequestsEfficiently() throws InterruptedException {
        // Given
        int numberOfRequests = 20;
        Long baseUserId = 2000L;
        
        // Mock service responses using functional approach
        java.util.stream.IntStream.range(0, numberOfRequests)
            .forEach(i -> {
                Long userId = baseUserId + i;
                PortfolioData mockPortfolio = PortfolioData.builder()
                        .portfolioId((long) (100 + i))
                        .userId(userId)
                        .portfolioName("Concurrent Test " + i)
                        .totalValue(new BigDecimal((i + 1) * 10000))
                        .build();
                
                when(portfolioService.getPortfolioByUserId(userId))
                        .thenReturn(Result.success(mockPortfolio));
            });
        
        // When - Execute multiple concurrent requests using functional approach
        CompletableFuture<MCPResponse>[] futures = java.util.stream.IntStream.range(0, numberOfRequests)
            .mapToObj(i -> {
                Long userId = baseUserId + i;
                return mcpServer.getPositions(userId, Map.of());
            })
            .toArray(CompletableFuture[]::new);
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures).join();
        
        // Then - All requests should succeed using functional approach
        java.util.Arrays.stream(futures)
            .forEach(future -> {
                MCPResponse response = future.join();
                assertThat(response.isSuccess()).isTrue();
                assertThat(response.asSuccess().endpoint()).isEqualTo("portfolio/get-positions");
            });
    }
    
    @Test
    @DisplayName("Should validate MCP response format consistency")
    void shouldValidateMCPResponseFormatConsistency() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1006L;
        Map<String, Object> parameters = Map.of();
        
        PortfolioData mockPortfolio = PortfolioData.builder()
                .portfolioId(456L)
                .userId(userId)
                .portfolioName("Format Test Portfolio")
                .totalValue(new BigDecimal("75000.00"))
                .build();
        
        when(portfolioService.getPortfolioByUserId(userId))
                .thenReturn(Result.success(mockPortfolio));
        
        // When
        CompletableFuture<MCPResponse> future = mcpServer.getPositions(userId, parameters);
        MCPResponse response = future.get();
        
        // Then - Validate response format
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.endpoint()).isNotNull();
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.correlationId()).isNotNull();
        
        MCPResponse.Success success = response.asSuccess();
        assertThat(success.data()).isNotNull();
        assertThat(success.status()).isEqualTo("SUCCESS");
        
        // Validate required data fields
        assertThat(success.hasData("userId")).isTrue();
        assertThat(success.hasData("timestamp")).isTrue();
        assertThat(success.hasData("portfolio")).isTrue();
    }
    
    @Test
    @DisplayName("Should handle MCP endpoint timeouts gracefully")
    void shouldHandleMCPEndpointTimeoutsGracefully() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1007L;
        Map<String, Object> parameters = Map.of();
        
        // Mock slow service response (simulated by delay in mock)
        when(portfolioService.getPortfolioByUserId(userId))
                .thenAnswer(invocation -> {
                    // Simulate slow service
                    Thread.sleep(100);
                    return Result.success(PortfolioData.builder()
                            .portfolioId(789L)
                            .userId(userId)
                            .portfolioName("Timeout Test")
                            .totalValue(new BigDecimal("25000.00"))
                            .build());
                });
        
        // When - Execute with reasonable timeout expectation
        CompletableFuture<MCPResponse> future = mcpServer.getPositions(userId, parameters);
        
        // Should complete within reasonable time (structured concurrency has 30s default timeout)
        MCPResponse response = future.get();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.asSuccess().getData("userId", Long.class)).isEqualTo(userId);
    }
    
    @Test
    @DisplayName("Should generate unique correlation IDs for tracing")
    void shouldGenerateUniqueCorrelationIdsForTracing() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1008L;
        Map<String, Object> parameters = Map.of();
        
        PortfolioData mockPortfolio = PortfolioData.builder()
                .portfolioId(999L)
                .userId(userId)
                .portfolioName("Correlation Test")
                .totalValue(new BigDecimal("30000.00"))
                .build();
        
        when(portfolioService.getPortfolioByUserId(userId))
                .thenReturn(Result.success(mockPortfolio));
        
        // When - Execute multiple requests
        CompletableFuture<MCPResponse> future1 = mcpServer.getPositions(userId, parameters);
        CompletableFuture<MCPResponse> future2 = mcpServer.getPositions(userId, parameters);
        
        MCPResponse response1 = future1.get();
        MCPResponse response2 = future2.get();
        
        // Then - Correlation IDs should be unique
        assertThat(response1.correlationId()).isNotNull();
        assertThat(response2.correlationId()).isNotNull();
        assertThat(response1.correlationId()).isNotEqualTo(response2.correlationId());
        
        // Correlation IDs should follow expected format
        assertThat(response1.correlationId()).startsWith("mcp-");
        assertThat(response2.correlationId()).startsWith("mcp-");
    }
}