package com.trademaster.agentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.config.AgentOSMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ MANDATORY: MCP (Model Context Protocol) Implementation
 * 
 * Phase 1 Requirement from AGENT_OS_MVP_SPEC.md:
 * - Basic MCP protocol implementation for agent communication
 * - Resource management and tool execution
 * - TradeMaster-specific MCP extensions
 * 
 * Uses Virtual Threads for scalable protocol operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MCPProtocolService {
    
    private final ObjectMapper objectMapper;
    private final AgentOSMetrics metrics;
    private final StructuredLoggingService structuredLogger;
    
    // ✅ RESOURCE REGISTRY: MCP Resources
    private final Map<String, MCPResource> resourceRegistry = new ConcurrentHashMap<>();
    
    // ✅ TOOL REGISTRY: MCP Tools
    private final Map<String, MCPTool> toolRegistry = new ConcurrentHashMap<>();
    
    /**
     * ✅ MCP CORE: List available resources
     */
    @Async
    public CompletableFuture<List<MCPResource>> listResources() {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_list_resources");
            
            try {
                List<MCPResource> resources = List.copyOf(resourceRegistry.values());
                timer.stop(metrics.getApiResponseTime());
                
                structuredLogger.logBusinessTransaction(
                    "mcp_operation",
                    "resources",
                    "list",
                    "system",
                    Map.of("resourceCount", resources.size())
                );
                
                return resources;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("mcp_list_resources", e.getClass().getSimpleName());
                structuredLogger.logError("mcp_list_resources", e.getMessage(), e, Map.of());
                throw new RuntimeException("Failed to list MCP resources", e);
            }
        });
    }
    
    /**
     * ✅ MCP CORE: Read specific resource
     */
    @Async
    public CompletableFuture<MCPResourceContent> readResource(String uri) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_read_resource");
            
            try {
                MCPResource resource = resourceRegistry.get(uri);
                if (resource == null) {
                    throw new IllegalArgumentException("Resource not found: " + uri);
                }
                
                MCPResourceContent content = resource.getContentProvider().get();
                
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logDataAccess("mcp_resource", uri, "read", "system");
                
                return content;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("mcp_read_resource", e.getClass().getSimpleName());
                structuredLogger.logError("mcp_read_resource", e.getMessage(), e, Map.of("uri", uri));
                throw new RuntimeException("Failed to read MCP resource: " + uri, e);
            }
        });
    }
    
    /**
     * ✅ MCP CORE: Execute tool
     */
    @Async
    public CompletableFuture<MCPToolResult> callTool(String toolName, Object parameters) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_call_tool");
            
            try {
                MCPTool tool = toolRegistry.get(toolName);
                if (tool == null) {
                    throw new IllegalArgumentException("Tool not found: " + toolName);
                }
                
                MCPToolResult result = tool.execute(parameters);
                
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logBusinessTransaction(
                    "mcp_tool_execution",
                    toolName,
                    "execute",
                    "system",
                    Map.of("success", result.isSuccess(), "duration", result.getDurationMs())
                );
                
                return result;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("mcp_call_tool", e.getClass().getSimpleName());
                structuredLogger.logError("mcp_call_tool", e.getMessage(), e, 
                    Map.of("toolName", toolName, "parameters", parameters));
                throw new RuntimeException("Failed to execute MCP tool: " + toolName, e);
            }
        });
    }
    
    /**
     * ✅ TRADEMASTER EXTENSION: Execute trade order via MCP
     */
    @Async
    public CompletableFuture<MCPExecutionResult> executeTradeOrder(MCPTradeOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_execute_trade_order");
            
            try {
                // ✅ VALIDATION: Validate trade order
                validateTradeOrder(order);
                
                // ✅ MCP EXECUTION: Execute via MCP protocol
                MCPToolResult toolResult = callTool("trade_execution", order).join();
                
                MCPExecutionResult result = MCPExecutionResult.builder()
                    .orderId(order.getOrderId())
                    .status(toolResult.isSuccess() ? "EXECUTED" : "FAILED")
                    .executionTime(toolResult.getDurationMs())
                    .details(toolResult.getData())
                    .build();
                
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logBusinessTransaction(
                    "mcp_trade_execution",
                    order.getOrderId(),
                    "execute",
                    "system",
                    Map.of("symbol", order.getSymbol(), "quantity", order.getQuantity(), "status", result.getStatus())
                );
                
                return result;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("mcp_execute_trade_order", e.getClass().getSimpleName());
                structuredLogger.logError("mcp_execute_trade_order", e.getMessage(), e,
                    Map.of("orderId", order.getOrderId(), "symbol", order.getSymbol()));
                throw new RuntimeException("Failed to execute trade order via MCP: " + order.getOrderId(), e);
            }
        });
    }
    
    /**
     * ✅ TRADEMASTER EXTENSION: Analyze market data via MCP
     */
    @Async
    public CompletableFuture<MCPMarketInsights> analyzeMarketData(MCPAnalysisRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_analyze_market_data");
            
            try {
                MCPToolResult toolResult = callTool("market_analysis", request).join();
                
                MCPMarketInsights insights = objectMapper.convertValue(
                    toolResult.getData(), 
                    MCPMarketInsights.class
                );
                
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logBusinessTransaction(
                    "mcp_market_analysis",
                    request.getSymbol(),
                    "analyze",
                    "system",
                    Map.of("timeframe", request.getTimeframe(), "indicators", request.getIndicators().size())
                );
                
                return insights;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("mcp_analyze_market_data", e.getClass().getSimpleName());
                structuredLogger.logError("mcp_analyze_market_data", e.getMessage(), e,
                    Map.of("symbol", request.getSymbol(), "timeframe", request.getTimeframe()));
                throw new RuntimeException("Failed to analyze market data via MCP: " + request.getSymbol(), e);
            }
        });
    }
    
    /**
     * ✅ REGISTRY: Register MCP resource
     */
    public void registerResource(String uri, String name, String description, 
                                java.util.function.Supplier<MCPResourceContent> contentProvider) {
        MCPResource resource = MCPResource.builder()
            .uri(uri)
            .name(name)
            .description(description)
            .contentProvider(contentProvider)
            .build();
            
        resourceRegistry.put(uri, resource);
        
        structuredLogger.logConfigurationChange(
            "mcp_resource_registry",
            null,
            uri,
            "system"
        );
    }
    
    /**
     * ✅ REGISTRY: Register MCP tool
     */
    public void registerTool(String name, String description, MCPToolExecutor executor) {
        MCPTool tool = MCPTool.builder()
            .name(name)
            .description(description)
            .executor(executor)
            .build();
            
        toolRegistry.put(name, tool);
        
        structuredLogger.logConfigurationChange(
            "mcp_tool_registry",
            null,
            name,
            "system"
        );
    }
    
    private void validateTradeOrder(MCPTradeOrder order) {
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
    
    // ✅ MCP DATA MODELS
    
    @lombok.Data
    @lombok.Builder
    public static class MCPResource {
        private String uri;
        private String name;
        private String description;
        private java.util.function.Supplier<MCPResourceContent> contentProvider;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MCPResourceContent {
        private String mimeType;
        private String text;
        private byte[] data;
        private Map<String, Object> metadata;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MCPTool {
        private String name;
        private String description;
        private MCPToolExecutor executor;
        
        public MCPToolResult execute(Object parameters) {
            return executor.execute(parameters);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MCPToolResult {
        private boolean success;
        private String message;
        private Object data;
        private long durationMs;
    }
    
    @FunctionalInterface
    public interface MCPToolExecutor {
        MCPToolResult execute(Object parameters);
    }
    
    // ✅ TRADEMASTER EXTENSIONS
    
    @lombok.Data
    @lombok.Builder
    public static class MCPTradeOrder {
        private String orderId;
        private String symbol;
        private double quantity;
        private String side; // BUY/SELL
        private String orderType; // MARKET/LIMIT
        private Double price;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MCPExecutionResult {
        private String orderId;
        private String status;
        private long executionTime;
        private Object details;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MCPAnalysisRequest {
        private String symbol;
        private String timeframe;
        private List<String> indicators;
        private Map<String, Object> parameters;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MCPMarketInsights {
        private String symbol;
        private String recommendation;
        private double confidence;
        private Map<String, Object> technicalIndicators;
        private List<String> signals;
        private Map<String, Object> riskMetrics;
    }
}