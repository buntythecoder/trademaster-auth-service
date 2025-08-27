package com.trademaster.auth.agentos;

import com.trademaster.auth.agentos.model.*;
import com.trademaster.auth.service.AuthenticationService;
import com.trademaster.auth.service.MfaService;
import com.trademaster.auth.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.trademaster.auth.constants.AuthConstants;

/**
 * Authentication MCP Controller
 * 
 * Provides MCP (Multi-Agent Communication Protocol) endpoints for the
 * TradeMaster Authentication Agent. Enables standardized agent-to-agent
 * communication for authentication and security services within the AgentOS ecosystem.
 * 
 * MCP Capabilities:
 * - Agent registration and discovery
 * - Health status reporting  
 * - Authentication service coordination
 * - Security audit integration
 * - Inter-agent session management
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/mcp/auth")
@RequiredArgsConstructor
public class AuthMCPController {
    
    private final AuthenticationAgent authenticationAgent;
    private final AuthCapabilityRegistry capabilityRegistry;
    private final AuthenticationService authenticationService;
    private final MfaService mfaService;
    private final SecurityAuditService securityAuditService;
    
    /**
     * Agent registration endpoint for AgentOS discovery
     */
    @PostMapping("/register")
    public ResponseEntity<AgentRegistrationResponse> registerAgent(
            @RequestBody AgentRegistrationRequest request) {
        
        log.info("Registering authentication agent with AgentOS orchestrator");
        
        try {
            var capabilities = authenticationAgent.getCapabilities();
            var healthScore = authenticationAgent.getHealthScore();
            
            var response = AgentRegistrationResponse.builder()
                .agentId(authenticationAgent.getAgentId())
                .agentType(authenticationAgent.getAgentType())
                .capabilities(capabilities)
                .healthScore(healthScore)
                .status(AuthConstants.STATUS_REGISTERED)
                .registrationTime(System.currentTimeMillis())
                .build();
                
            log.info("Authentication agent registered successfully with health score: {}", healthScore);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to register authentication agent", e);
            return ResponseEntity.status(500)
                .body(AgentRegistrationResponse.builder()
                    .status(AuthConstants.STATUS_REGISTRATION_FAILED)
                    .errorMessage(e.getMessage())
                    .build());
        }
    }
    
    /**
     * Health check endpoint for agent monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<AgentHealthResponse> getAgentHealth() {
        
        try {
            var overallHealth = authenticationAgent.getHealthScore();
            var capabilitySummary = capabilityRegistry.getPerformanceSummary();
            
            var response = AgentHealthResponse.builder()
                .agentId(authenticationAgent.getAgentId())
                .healthScore(overallHealth)
                .status(overallHealth > AuthConstants.HEALTH_SCORE_HEALTHY ? AuthConstants.STATUS_HEALTHY : 
                        overallHealth > AuthConstants.HEALTH_SCORE_DEGRADED ? AuthConstants.STATUS_DEGRADED : 
                        AuthConstants.STATUS_UNHEALTHY)
                .capabilityHealth(capabilitySummary)
                .lastUpdated(System.currentTimeMillis())
                .build();
                
            log.debug("Agent health check completed - Score: {}", overallHealth);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.status(500)
                .body(AgentHealthResponse.builder()
                    .agentId(authenticationAgent.getAgentId())
                    .status(AuthConstants.STATUS_HEALTH_CHECK_FAILED)
                    .errorMessage(e.getMessage())
                    .build());
        }
    }
    
    /**
     * Agent capability discovery endpoint
     */
    @GetMapping("/capabilities")
    public ResponseEntity<AgentCapabilitiesResponse> getAgentCapabilities() {
        
        try {
            var capabilities = authenticationAgent.getCapabilities();
            Map<String, Map<String, Object>> capabilityDetails = Map.of(
                AuthConstants.CAPABILITY_USER_AUTHENTICATION, Map.of(
                    "proficiency", AuthConstants.PROFICIENCY_EXPERT,
                    "description", "JWT-based authentication and session management",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate(AuthConstants.CAPABILITY_USER_AUTHENTICATION),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime(AuthConstants.CAPABILITY_USER_AUTHENTICATION)
                ),
                AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH, Map.of(
                    "proficiency", AuthConstants.PROFICIENCY_EXPERT, 
                    "description", "MFA setup, verification, and recovery",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate(AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime(AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH)
                ),
                AuthConstants.CAPABILITY_SECURITY_AUDIT, Map.of(
                    "proficiency", AuthConstants.PROFICIENCY_ADVANCED,
                    "description", "Security event logging and compliance monitoring",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate(AuthConstants.CAPABILITY_SECURITY_AUDIT),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime(AuthConstants.CAPABILITY_SECURITY_AUDIT)
                ),
                AuthConstants.CAPABILITY_SESSION_MANAGEMENT, Map.of(
                    "proficiency", AuthConstants.PROFICIENCY_ADVANCED,
                    "description", "User session lifecycle and security controls",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate(AuthConstants.CAPABILITY_SESSION_MANAGEMENT),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime(AuthConstants.CAPABILITY_SESSION_MANAGEMENT)
                ),
                AuthConstants.CAPABILITY_DEVICE_TRUST, Map.of(
                    "proficiency", AuthConstants.PROFICIENCY_INTERMEDIATE,
                    "description", "Device fingerprinting and trusted device management",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate("DEVICE_TRUST"),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime("DEVICE_TRUST")
                )
            );
            
            var response = AgentCapabilitiesResponse.builder()
                .agentId(authenticationAgent.getAgentId())
                .capabilities(capabilities)
                .capabilityDetails(capabilityDetails)
                .totalCapabilities(capabilities.size())
                .build();
                
            log.debug("Agent capabilities requested - {} capabilities available", capabilities.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve agent capabilities", e);
            return ResponseEntity.status(500)
                .body(AgentCapabilitiesResponse.builder()
                    .errorMessage(e.getMessage())
                    .build());
        }
    }
    
    /**
     * Inter-agent authentication request endpoint
     */
    @PostMapping("/authenticate-user")
    public CompletableFuture<ResponseEntity<AuthenticationMCPResponse>> authenticateUser(
            @RequestBody UserAuthenticationMCPRequest request) {
        
        log.info("Received MCP authentication request for user: {} from agent: {}", 
                request.getUsername(), request.getRequestingAgentId());
        
        return authenticationAgent.authenticateUser(
                request.getUsername(), 
                request.getPassword(), 
                request.getDeviceId())
            .thenApply(result -> {
                var response = AuthenticationMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status(AuthConstants.STATUS_SUCCESS)
                    .result(result)
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.info("MCP authentication completed successfully for user: {}", request.getUsername());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                var response = AuthenticationMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status(AuthConstants.STATUS_FAILED)
                    .errorMessage(ex.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.error("MCP authentication failed for user: {}", request.getUsername(), ex);
                return ResponseEntity.status(500).body(response);
            });
    }
    
    /**
     * Inter-agent MFA setup request endpoint
     */
    @PostMapping("/setup-mfa")
    public CompletableFuture<ResponseEntity<MFASetupMCPResponse>> setupMFA(
            @RequestBody MFASetupMCPRequest request) {
        
        log.info("Received MCP MFA setup request for user: {} from agent: {}", 
                request.getUserId(), request.getRequestingAgentId());
        
        return authenticationAgent.setupMFA(request.getUserId(), request.getMfaType())
            .thenApply(result -> {
                var response = MFASetupMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status(AuthConstants.STATUS_SUCCESS)
                    .result(result)
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.info("MCP MFA setup completed successfully for user: {}", request.getUserId());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                var response = MFASetupMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status(AuthConstants.STATUS_FAILED)
                    .errorMessage(ex.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.error("MCP MFA setup failed for user: {}", request.getUserId(), ex);
                return ResponseEntity.status(500).body(response);
            });
    }
    
    /**
     * Agent performance metrics endpoint
     */
    @GetMapping("/metrics")
    public ResponseEntity<AgentMetricsResponse> getAgentMetrics() {
        
        try {
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            var overallHealth = authenticationAgent.getHealthScore();
            
            var response = AgentMetricsResponse.builder()
                .agentId(authenticationAgent.getAgentId())
                .healthScore(overallHealth)
                .capabilityMetrics(performanceSummary)
                .timestamp(System.currentTimeMillis())
                .build();
                
            log.debug("Agent metrics requested - Health: {}", overallHealth);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve agent metrics", e);
            return ResponseEntity.status(500)
                .body(AgentMetricsResponse.builder()
                    .errorMessage(e.getMessage())
                    .build());
        }
    }
}
