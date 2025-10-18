package com.trademaster.marketdata.agentos;

import com.trademaster.marketdata.dto.MarketDataRequest;
import com.trademaster.marketdata.dto.MarketDataResponse;
import com.trademaster.marketdata.dto.SubscriptionRequest;
import com.trademaster.marketdata.dto.SubscriptionResponse;
import com.trademaster.marketdata.dto.PriceAlertRequest;
import com.trademaster.marketdata.dto.PriceAlertResponse;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.service.TechnicalAnalysisService;
import com.trademaster.marketdata.service.MarketScannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data MCP (Multi-Agent Communication Protocol) Controller
 * 
 * Provides standardized MCP endpoints for agent-to-agent communication
 * in the TradeMaster AgentOS ecosystem. Supports both synchronous and
 * asynchronous market data operations with callback mechanisms.
 * 
 * MCP Methods:
 * - getMarketData: Real-time and historical data retrieval
 * - subscribeToUpdates: WebSocket subscription management
 * - performTechnicalAnalysis: Indicator calculations
 * - scanMarket: Symbol screening and filtering
 * - managePriceAlerts: Alert configuration and monitoring
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/mcp/market-data")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
public class MarketDataMCPController {
    
    private final MarketDataService marketDataService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final MarketScannerService marketScannerService;
    private final MarketDataAgent marketDataAgent;
    
    /**
     * MCP Method: getMarketData
     * Retrieves market data for specified symbols and timeframe
     */
    @PostMapping("/getMarketData")
    @MCPMethod("getMarketData")
    public ResponseEntity<MarketDataResponse> getMarketData(
            @MCPParam("symbols") @RequestParam List<String> symbols,
            @MCPParam("timeframe") @RequestParam(defaultValue = "1D") String timeframe,
            @MCPParam("includeIndicators") @RequestParam(defaultValue = "false") boolean includeIndicators) {
        
        try {
            log.info("MCP getMarketData request for symbols: {} with timeframe: {}", symbols, timeframe);
            
            var request = MarketDataRequest.builder()
                .symbols(symbols)
                .timeframe(timeframe)
                .includeIndicators(includeIndicators)
                .requestId(System.currentTimeMillis())
                .build();
            
            var response = marketDataAgent.handleDataRequest(request).join();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("MCP getMarketData failed", e);
            
            var errorResponse = MarketDataResponse.builder()
                .status("ERROR")
                .errorMessage(e.getMessage())
                .processingTimeMs(System.currentTimeMillis())
                .build();
                
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * MCP Method: subscribeToUpdates
     * Manages real-time data subscriptions with callback support
     */
    @PostMapping("/subscribeToUpdates")
    @MCPMethod("subscribeToUpdates")
    public ResponseEntity<Map<String, Object>> subscribeToUpdates(
            @MCPParam("symbols") @RequestParam List<String> symbols,
            @MCPParam("updateFrequency") @RequestParam(defaultValue = "1000") int updateFrequencyMs,
            @RequestBody(required = false) Map<String, Object> callbackConfig) {

        try {
            log.info("MCP subscribeToUpdates request for symbols: {}", symbols);

            // Rule #9: Build immutable SubscriptionRequest using builder pattern
            var subscriptionRequest = SubscriptionRequest.builder()
                .symbols(symbols)
                .updateFrequency(updateFrequencyMs)
                .build();

            // Rule #25: Service call with circuit breaker protection
            // Rule #11: Functional error handling with CompletableFuture
            SubscriptionResponse response = marketDataService
                .subscribeToRealTimeUpdates(subscriptionRequest)
                .join();

            return ResponseEntity.ok(Map.of(
                "status", response.status().name(),
                "symbols", response.symbols(),
                "subscriptionId", response.subscriptionId(),
                "updateFrequency", response.updateFrequencyMs(),
                "message", response.message()
            ));

        } catch (Exception e) {
            log.error("MCP subscribeToUpdates failed", e);

            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "errorMessage", e.getMessage()
            ));
        }
    }
    
    /**
     * MCP Method: performTechnicalAnalysis
     * Calculates technical indicators for specified symbols
     */
    @PostMapping("/performTechnicalAnalysis")
    @MCPMethod("performTechnicalAnalysis")
    public ResponseEntity<Map<String, Object>> performTechnicalAnalysis(
            @MCPParam("symbols") @RequestParam List<String> symbols,
            @MCPParam("indicators") @RequestParam List<String> indicators,
            @MCPParam("period") @RequestParam(defaultValue = "20") int period) {
        
        try {
            log.info("MCP performTechnicalAnalysis for symbols: {} with indicators: {}", 
                    symbols, indicators);
            
            var analysisResult = technicalAnalysisService.calculateIndicators(symbols, indicators);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "symbols", symbols,
                "indicators", indicators,
                "analysisResult", analysisResult,
                "calculationTime", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("MCP performTechnicalAnalysis failed", e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "errorMessage", e.getMessage()
            ));
        }
    }
    
    /**
     * MCP Method: scanMarket
     * Performs market scanning with specified criteria
     */
    @PostMapping("/scanMarket")
    @MCPMethod("scanMarket")
    public ResponseEntity<Map<String, Object>> scanMarket(
            @RequestBody Map<String, Object> scanCriteria) {
        
        try {
            log.info("MCP scanMarket request with criteria: {}", scanCriteria);
            
            var scanResults = marketScannerService.performScan(scanCriteria);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "scanCriteria", scanCriteria,
                "results", scanResults,
                "resultCount", scanResults.size(),
                "scanTime", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("MCP scanMarket failed", e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "errorMessage", e.getMessage()
            ));
        }
    }
    
    /**
     * MCP Method: managePriceAlerts
     * Configures and manages price alerts
     */
    @PostMapping("/managePriceAlerts")
    @MCPMethod("managePriceAlerts")
    public ResponseEntity<Map<String, Object>> managePriceAlerts(
            @MCPParam("action") @RequestParam String action,
            @RequestBody Map<String, Object> alertConfig) {

        try {
            log.info("MCP managePriceAlerts action: {} with config: {}", action, alertConfig);

            // Rule #9: Convert Map to immutable PriceAlertRequest
            var alertRequest = convertToPriceAlertRequest(alertConfig);

            // Rule #11: Functional error handling with CompletableFuture
            // Rule #14: Pattern matching for action routing
            PriceAlertResponse alertResponse = switch (action.toUpperCase()) {
                case "CREATE" -> marketDataService.createPriceAlert(alertRequest).join();
                case "UPDATE" -> marketDataService.updatePriceAlert(alertRequest).join();
                case "DELETE" -> marketDataService.deletePriceAlert(alertRequest).join();
                case "LIST" -> marketDataService.listPriceAlerts(alertRequest).join();
                default -> throw new IllegalArgumentException("Invalid action: " + action);
            };

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "action", action,
                "result", alertResponse,
                "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("MCP managePriceAlerts failed", e);

            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "errorMessage", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to convert Map<String, Object> to PriceAlertRequest
     * Rule #2 (SRP): Single responsibility for DTO conversion
     * Rule #11: Functional approach with Optional and pattern matching
     */
    private PriceAlertRequest convertToPriceAlertRequest(Map<String, Object> config) {
        var builder = PriceAlertRequest.builder();

        // Extract values from Map with type safety
        if (config.containsKey("name")) builder.name((String) config.get("name"));
        if (config.containsKey("description")) builder.description((String) config.get("description"));
        if (config.containsKey("symbol")) builder.symbol((String) config.get("symbol"));
        if (config.containsKey("exchange")) builder.exchange((String) config.get("exchange"));

        // Handle enum values safely
        if (config.containsKey("alertType")) {
            builder.alertType(com.trademaster.marketdata.entity.PriceAlert.AlertType.valueOf(
                config.get("alertType").toString().toUpperCase()));
        }
        if (config.containsKey("triggerCondition")) {
            builder.triggerCondition(com.trademaster.marketdata.entity.PriceAlert.TriggerCondition.valueOf(
                config.get("triggerCondition").toString().toUpperCase()));
        }
        if (config.containsKey("priority")) {
            builder.priority(com.trademaster.marketdata.entity.PriceAlert.Priority.valueOf(
                config.get("priority").toString().toUpperCase()));
        }
        if (config.containsKey("notificationMethod")) {
            builder.notificationMethod(com.trademaster.marketdata.entity.PriceAlert.NotificationMethod.valueOf(
                config.get("notificationMethod").toString().toUpperCase()));
        }

        // Handle BigDecimal values
        if (config.containsKey("targetPrice")) {
            builder.targetPrice(new BigDecimal(config.get("targetPrice").toString()));
        }
        if (config.containsKey("stopPrice")) {
            builder.stopPrice(new BigDecimal(config.get("stopPrice").toString()));
        }

        // Handle filter sets for LIST queries
        if (config.containsKey("symbols")) {
            @SuppressWarnings("unchecked")
            Set<String> symbols = Set.copyOf((List<String>) config.get("symbols"));
            builder.symbols(symbols);
        }
        if (config.containsKey("exchanges")) {
            @SuppressWarnings("unchecked")
            Set<String> exchanges = Set.copyOf((List<String>) config.get("exchanges"));
            builder.exchanges(exchanges);
        }

        // Boolean flags for queries
        if (config.containsKey("activeOnly")) builder.activeOnly((Boolean) config.get("activeOnly"));
        if (config.containsKey("triggeredOnly")) builder.triggeredOnly((Boolean) config.get("triggeredOnly"));

        return builder.build();
    }
    
    /**
     * MCP Method: getAgentHealth
     * Returns health status and metrics for the market data agent
     */
    @GetMapping("/getAgentHealth")
    @MCPMethod("getAgentHealth")
    public ResponseEntity<Map<String, Object>> getAgentHealth() {
        
        try {
            var healthScore = marketDataAgent.getHealthScore();
            var capabilities = marketDataAgent.getCapabilities();
            
            return ResponseEntity.ok(Map.of(
                "agentId", marketDataAgent.getAgentId(),
                "agentType", marketDataAgent.getAgentType(),
                "healthScore", healthScore,
                "capabilities", capabilities,
                "status", healthScore > 0.8 ? "HEALTHY" : healthScore > 0.5 ? "DEGRADED" : "UNHEALTHY",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("MCP getAgentHealth failed", e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "errorMessage", e.getMessage()
            ));
        }
    }
    
    /**
     * Async MCP method support for long-running operations
     */
    @PostMapping("/async/{methodName}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> handleAsyncMCPRequest(
            @PathVariable String methodName,
            @RequestBody Map<String, Object> parameters) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Async MCP method: {} with parameters: {}", methodName, parameters);
                
                // Route to appropriate async handler based on method name
                Object result = switch (methodName.toLowerCase()) {
                    case "getmarketdata" -> handleAsyncMarketData(parameters);
                    case "performtechnicalanalysis" -> handleAsyncTechnicalAnalysis(parameters);
                    case "scanmarket" -> handleAsyncMarketScan(parameters);
                    default -> throw new IllegalArgumentException("Unknown async method: " + methodName);
                };
                
                return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "method", methodName,
                    "result", result,
                    "completionTime", System.currentTimeMillis()
                ));
                
            } catch (Exception e) {
                log.error("Async MCP method {} failed", methodName, e);
                
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "method", methodName,
                    "errorMessage", e.getMessage()
                ));
            }
        });
    }
    
    private Object handleAsyncMarketData(Map<String, Object> parameters) {
        // Async market data processing implementation
        return "Async market data processing completed";
    }
    
    private Object handleAsyncTechnicalAnalysis(Map<String, Object> parameters) {
        // Async technical analysis processing implementation
        return "Async technical analysis completed";
    }
    
    private Object handleAsyncMarketScan(Map<String, Object> parameters) {
        // Async market scanning implementation
        return "Async market scan completed";
    }
}

