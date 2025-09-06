package com.trademaster.agentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.config.AgentOSMetrics;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.MCPError;
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
     * ✅ FUNCTIONAL ERROR HANDLING: List available resources using Railway Programming
     */
    @Async
    public CompletableFuture<List<MCPResource>> listResources() {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_list_resources");
            
            return listResourcesFunctional()
                .onSuccess(resources -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logBusinessTransaction(
                        "mcp_operation",
                        "resources",
                        "list",
                        "system",
                        Map.of("resourceCount", resources.size())
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("mcp_list_resources", error.getErrorCode());
                    structuredLogger.logError("mcp_list_resources", error.getMessage(), null, 
                        Map.of("errorCode", error.getErrorCode()));
                })
                .getOrThrow(error -> new RuntimeException("Failed to list MCP resources: " + error.getMessage()));
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional resource listing pipeline
     */
    private Result<List<MCPResource>, MCPError> listResourcesFunctional() {
        return Result.tryExecute(() -> List.copyOf(resourceRegistry.values()))
            .mapError(e -> MCPError.protocolError("list_resources", "Failed to copy resource registry", Map.of("resourceCount", resourceRegistry.size())));
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Read specific resource using Railway Programming
     */
    @Async
    public CompletableFuture<MCPResourceContent> readResource(String uri) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_read_resource");
            
            return readResourceFunctional(uri)
                .onSuccess(content -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logDataAccess("mcp_resource", uri, "read", "system");
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("mcp_read_resource", error.getErrorCode());
                    structuredLogger.logError("mcp_read_resource", error.getMessage(), null, 
                        Map.of("uri", uri, "errorCode", error.getErrorCode()));
                })
                .getOrThrow(error -> new RuntimeException("Failed to read MCP resource: " + uri + ". " + error.getMessage()));
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional resource reading pipeline
     */
    private Result<MCPResourceContent, MCPError> readResourceFunctional(String uri) {
        return Result.fromNullable(resourceRegistry.get(uri), MCPError.resourceNotFound(uri, "read_resource"))
            .flatMap(resource -> 
                Result.tryExecute(() -> resource.getContentProvider().get())
                    .mapError(e -> MCPError.executionError("resource_content_provider", "Failed to get resource content", Map.of("uri", uri), e))
            );
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Execute tool using Railway Programming
     */
    @Async
    public CompletableFuture<MCPToolResult> callTool(String toolName, Object parameters) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_call_tool");
            
            return callToolFunctional(toolName, parameters)
                .onSuccess(result -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logBusinessTransaction(
                        "mcp_tool_execution",
                        toolName,
                        "execute",
                        "system",
                        Map.of("success", result.isSuccess(), "duration", result.getDurationMs())
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("mcp_call_tool", error.getErrorCode());
                    structuredLogger.logError("mcp_call_tool", error.getMessage(), null,
                        Map.of("toolName", toolName, "parameters", parameters, "errorCode", error.getErrorCode()));
                })
                .getOrThrow(error -> new RuntimeException("Failed to execute MCP tool: " + toolName + ". " + error.getMessage()));
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional tool execution pipeline
     */
    private Result<MCPToolResult, MCPError> callToolFunctional(String toolName, Object parameters) {
        return Result.fromNullable(toolRegistry.get(toolName), MCPError.toolNotFound(toolName, "call_tool"))
            .flatMap(tool -> 
                Result.tryExecute(() -> tool.execute(parameters))
                    .mapError(e -> MCPError.executionError(toolName, "Tool execution failed", parameters, e))
            );
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Execute trade order via MCP using Railway Programming
     */
    @Async
    public CompletableFuture<MCPExecutionResult> executeTradeOrder(MCPTradeOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_execute_trade_order");
            
            return executeTradeOrderFunctional(order)
                .onSuccess(result -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logBusinessTransaction(
                        "mcp_trade_execution",
                        order.getOrderId(),
                        "execute",
                        "system",
                        Map.of("symbol", order.getSymbol(), "quantity", order.getQuantity(), "status", result.getStatus())
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("mcp_execute_trade_order", error.getErrorCode());
                    structuredLogger.logError("mcp_execute_trade_order", error.getMessage(), null,
                        Map.of("orderId", order.getOrderId(), "symbol", order.getSymbol(), "errorCode", error.getErrorCode()));
                })
                .getOrThrow(error -> new RuntimeException("Failed to execute trade order via MCP: " + order.getOrderId() + ". " + error.getMessage()));
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional trade order execution pipeline
     */
    private Result<MCPExecutionResult, MCPError> executeTradeOrderFunctional(MCPTradeOrder order) {
        return validateTradeOrderFunctional(order)
            .flatMap(ignored -> callToolFunctional("trade_execution", order))
            .map(toolResult -> MCPExecutionResult.builder()
                .orderId(order.getOrderId())
                .status(toolResult.isSuccess() ? "EXECUTED" : "FAILED")
                .executionTime(toolResult.getDurationMs())
                .details(toolResult.getData())
                .build());
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Analyze market data via MCP using Railway Programming
     */
    @Async
    public CompletableFuture<MCPMarketInsights> analyzeMarketData(MCPAnalysisRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("mcp_analyze_market_data");
            
            return analyzeMarketDataFunctional(request)
                .onSuccess(insights -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logBusinessTransaction(
                        "mcp_market_analysis",
                        request.getSymbol(),
                        "analyze",
                        "system",
                        Map.of("timeframe", request.getTimeframe(), "indicators", request.getIndicators().size())
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("mcp_analyze_market_data", error.getErrorCode());
                    structuredLogger.logError("mcp_analyze_market_data", error.getMessage(), null,
                        Map.of("symbol", request.getSymbol(), "timeframe", request.getTimeframe(), "errorCode", error.getErrorCode()));
                })
                .getOrThrow(error -> new RuntimeException("Failed to analyze market data via MCP: " + request.getSymbol() + ". " + error.getMessage()));
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional market data analysis pipeline
     */
    private Result<MCPMarketInsights, MCPError> analyzeMarketDataFunctional(MCPAnalysisRequest request) {
        return callToolFunctional("market_analysis", request)
            .flatMap(toolResult -> 
                Result.tryExecute(() -> objectMapper.convertValue(toolResult.getData(), MCPMarketInsights.class))
                    .mapError(e -> MCPError.deserializationError("MCPMarketInsights", e))
            );
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
    
    /**
     * ✅ FUNCTIONAL VALIDATION: Validate trade order using Result monad
     */
    private Result<Boolean, MCPError> validateTradeOrderFunctional(MCPTradeOrder order) {
        return validateOrderId(order.getOrderId())
            .flatMap(ignored -> validateSymbol(order.getSymbol()))
            .flatMap(ignored -> validateQuantity(order.getQuantity()));
    }
    
    /**
     * ✅ FUNCTIONAL VALIDATION: Validate order ID
     */
    private Result<Boolean, MCPError> validateOrderId(String orderId) {
        return orderId != null && !orderId.isBlank()
            ? Result.success(true)
            : Result.failure(MCPError.validationError("orderId", String.valueOf(orderId), "Order ID is required and cannot be blank"));
    }
    
    /**
     * ✅ FUNCTIONAL VALIDATION: Validate symbol
     */
    private Result<Boolean, MCPError> validateSymbol(String symbol) {
        return symbol != null && !symbol.isBlank()
            ? Result.success(true)
            : Result.failure(MCPError.validationError("symbol", String.valueOf(symbol), "Symbol is required and cannot be blank"));
    }
    
    /**
     * ✅ FUNCTIONAL VALIDATION: Validate quantity
     */
    private Result<Boolean, MCPError> validateQuantity(double quantity) {
        return quantity > 0
            ? Result.success(true)
            : Result.failure(MCPError.validationError("quantity", String.valueOf(quantity), "Quantity must be positive"));
    }
    
    /**
     * ✅ LEGACY COMPATIBILITY: Keep original method for backward compatibility
     */
    private void validateTradeOrder(MCPTradeOrder order) {
        validateTradeOrderFunctional(order)
            .getOrThrow(error -> new IllegalArgumentException(error.getMessage()));
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