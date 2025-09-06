package com.trademaster.portfolio.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Service MCP (Multi-Agent Communication Protocol) Controller
 * 
 * Provides standardized endpoints for agent-to-agent communication within
 * the TradeMaster AgentOS ecosystem. Handles position tracking, performance
 * analytics, risk assessment, asset allocation, and portfolio reporting
 * requests from other agents in the system.
 * 
 * MCP Protocol Features:
 * - Standardized request/response formats for portfolio operations
 * - Authentication and authorization for agent communications
 * - Real-time coordination with Trading and Market Data agents
 * - Structured concurrency for high-performance portfolio processing
 * - Comprehensive error handling and circuit breaker patterns
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mcp/portfolio")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "https://trademaster.io"})
public class PortfolioMCPController {
    
    private final PortfolioAgent portfolioAgent;
    private final PortfolioCapabilityRegistry capabilityRegistry;
    
    /**
     * MCP Endpoint: Track Positions
     * Handles position tracking requests from other agents
     */
    @PostMapping("/trackPositions")
    @MCPMethod("trackPositions")
    @PreAuthorize("hasRole('AGENT') or hasRole('TRADING_SYSTEM')")
    public ResponseEntity<MCPResponse> trackPositions(
            @MCPParam("accountId") @RequestParam String accountId,
            @MCPParam("symbols") @RequestParam List<String> symbols,
            @MCPParam("requestId") @RequestParam(required = false) Long requestId) {
        
        log.info("MCP: Received position tracking request for account: {} symbols: {}", 
                accountId, symbols.size());
        
        try {
            CompletableFuture<String> result = portfolioAgent.trackPositions(accountId, symbols);
            String trackingResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/trackPositions")
                .data(Map.of(
                    "success", true,
                    "data", trackingResult,
                    "message", "Position tracking initiated successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "requestId", requestId,
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to track positions", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/trackPositions")
                .error("Position tracking failed: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId() + "-" + requestId)
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Calculate Performance Metrics
     * Handles performance analytics requests from other agents
     */
    @PostMapping("/calculatePerformanceMetrics")
    @MCPMethod("calculatePerformanceMetrics")
    @PreAuthorize("hasRole('AGENT') or hasRole('ANALYTICS_SYSTEM')")
    public ResponseEntity<MCPResponse> calculatePerformanceMetrics(
            @MCPParam("performanceRequest") @RequestBody @Validated PerformanceAnalyticsRequest request) {
        
        log.info("MCP: Received performance metrics calculation request for account: {}", 
                request.getAccountId());
        
        try {
            CompletableFuture<String> result = portfolioAgent.calculatePerformanceMetrics(
                request.getAccountId(), 
                request.getStartDate(), 
                request.getEndDate()
            );
            String analyticsResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/calculatePerformanceMetrics")
                .data(Map.of(
                    "success", true,
                    "data", analyticsResult,
                    "message", "Performance metrics calculated successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "requestId", request.getRequestId(),
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to calculate performance metrics", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/calculatePerformanceMetrics")
                .error("Performance metrics calculation failed: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId() + "-" + request.getRequestId())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Assess Portfolio Risk
     * Handles risk assessment requests from other agents
     */
    @PostMapping("/assessPortfolioRisk")
    @MCPMethod("assessPortfolioRisk")
    @PreAuthorize("hasRole('AGENT') or hasRole('RISK_SYSTEM')")
    public ResponseEntity<MCPResponse> assessPortfolioRisk(
            @MCPParam("riskRequest") @RequestBody @Validated RiskAssessmentRequest request) {
        
        log.info("MCP: Received risk assessment request for account: {} type: {}", 
                request.getAccountId(), request.getAssessmentType());
        
        try {
            CompletableFuture<String> result = portfolioAgent.assessPortfolioRisk(
                request.getAccountId(), 
                request.getAssessmentType()
            );
            String riskResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/assessPortfolioRisk")
                .data(Map.of(
                    "success", true,
                    "data", riskResult,
                    "message", "Risk assessment completed successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "requestId", request.getRequestId(),
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to assess portfolio risk", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/assessPortfolioRisk")
                .error("Risk assessment failed: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId() + "-" + request.getRequestId())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Optimize Asset Allocation
     * Handles asset allocation optimization requests from other agents
     */
    @PostMapping("/optimizeAssetAllocation")
    @MCPMethod("optimizeAssetAllocation")
    @PreAuthorize("hasRole('AGENT') or hasRole('PORTFOLIO_SYSTEM')")
    public ResponseEntity<MCPResponse> optimizeAssetAllocation(
            @MCPParam("allocationRequest") @RequestBody @Validated AssetAllocationRequest request) {
        
        log.info("MCP: Received asset allocation optimization request for account: {} strategy: {}", 
                request.getAccountId(), request.getStrategy());
        
        try {
            CompletableFuture<String> result = portfolioAgent.optimizeAssetAllocation(
                request.getAccountId(),
                request.getStrategy(),
                request.getTargetAllocations()
            );
            String allocationResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/optimizeAssetAllocation")
                .data(Map.of(
                    "success", true,
                    "data", allocationResult,
                    "message", "Asset allocation optimization completed successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "requestId", request.getRequestId(),
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to optimize asset allocation", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/optimizeAssetAllocation")
                .error("Asset allocation optimization failed: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId() + "-" + request.getRequestId())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Generate Portfolio Report
     * Handles portfolio reporting requests from other agents
     */
    @PostMapping("/generatePortfolioReport")
    @MCPMethod("generatePortfolioReport")
    @PreAuthorize("hasRole('AGENT') or hasRole('REPORTING_SYSTEM')")
    public ResponseEntity<MCPResponse> generatePortfolioReport(
            @MCPParam("reportRequest") @RequestBody @Validated PortfolioReportRequest request) {
        
        log.info("MCP: Received portfolio report generation request for account: {} type: {}", 
                request.getAccountId(), request.getReportType());
        
        try {
            CompletableFuture<String> result = portfolioAgent.generatePortfolioReport(
                request.getAccountId(),
                request.getReportType(),
                request.getStartDate(),
                request.getEndDate()
            );
            String reportResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/generatePortfolioReport")
                .data(Map.of(
                    "success", true,
                    "data", reportResult,
                    "message", "Portfolio report generated successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "requestId", request.getRequestId(),
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to generate portfolio report", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/generatePortfolioReport")
                .error("Portfolio report generation failed: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId() + "-" + request.getRequestId())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Get Agent Capabilities
     * Returns current agent capabilities and health metrics
     */
    @GetMapping("/capabilities")
    @MCPMethod("getCapabilities")
    @PreAuthorize("hasRole('AGENT') or hasRole('ORCHESTRATION_SERVICE')")
    public ResponseEntity<MCPResponse> getCapabilities() {
        
        log.info("MCP: Received capabilities request");
        
        try {
            AgentCapabilitiesResponse capabilities = AgentCapabilitiesResponse.builder()
                .agentId(portfolioAgent.getAgentId())
                .agentType(portfolioAgent.getAgentType())
                .capabilities(portfolioAgent.getCapabilities())
                .healthScore(portfolioAgent.getHealthScore())
                .performanceSummary(capabilityRegistry.getPerformanceSummary())
                .build();
                
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/getCapabilities")
                .data(Map.of(
                    "success", true,
                    "data", capabilities,
                    "message", "Agent capabilities retrieved successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to get capabilities", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/getCapabilities")
                .error("Failed to get capabilities: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Agent Health Check
     * Returns current agent health and performance metrics
     */
    @GetMapping("/health")
    @MCPMethod("getHealth")
    @PreAuthorize("hasRole('AGENT') or hasRole('ORCHESTRATION_SERVICE')")
    public ResponseEntity<MCPResponse> getHealth() {
        
        log.debug("MCP: Received health check request");
        
        try {
            AgentHealthResponse health = AgentHealthResponse.builder()
                .agentId(portfolioAgent.getAgentId())
                .healthScore(portfolioAgent.getHealthScore())
                .status(portfolioAgent.getHealthScore() > 0.8 ? "HEALTHY" : "DEGRADED")
                .capabilities(portfolioAgent.getCapabilities())
                .lastUpdate(System.currentTimeMillis())
                .build();
                
            return ResponseEntity.ok(MCPResponse.builder("mcp/portfolio/getHealth")
                .data(Map.of(
                    "success", true,
                    "data", health,
                    "message", "Agent health retrieved successfully",
                    "agentId", portfolioAgent.getAgentId(),
                    "processingTimeMs", System.currentTimeMillis()
                ))
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to get health", e);
            return ResponseEntity.badRequest().body(MCPResponse.builder("mcp/portfolio/getHealth")
                .error("Failed to get health: " + e.getMessage())
                .correlationId(portfolioAgent.getAgentId())
                .build());
        }
    }
}

// MCP Request/Response DTOs

@lombok.Builder
@lombok.Data
class PerformanceAnalyticsRequest {
    private Long requestId;
    private String accountId;
    private LocalDate startDate;
    private LocalDate endDate;
}

@lombok.Builder
@lombok.Data
class RiskAssessmentRequest {
    private Long requestId;
    private String accountId;
    private RiskAssessmentType assessmentType;
}

@lombok.Builder  
@lombok.Data
class AssetAllocationRequest {
    private Long requestId;
    private String accountId;
    private AllocationStrategy strategy;
    private Map<String, BigDecimal> targetAllocations;
}

@lombok.Builder
@lombok.Data
class PortfolioReportRequest {
    private Long requestId;
    private String accountId;
    private ReportType reportType;
    private LocalDate startDate;
    private LocalDate endDate;
}


@lombok.Builder
@lombok.Data
class AgentCapabilitiesResponse {
    private String agentId;
    private String agentType;
    private List<String> capabilities;
    private Double healthScore;
    private Map<String, String> performanceSummary;
}

@lombok.Builder
@lombok.Data
class AgentHealthResponse {
    private String agentId;
    private Double healthScore;
    private String status;
    private List<String> capabilities;
    private Long lastUpdate;
}

// MCP Annotations for protocol compliance

@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface MCPMethod {
    String value();
}

@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface MCPParam {
    String value();
}