package com.trademaster.userprofile.agentos;

import com.trademaster.userprofile.service.UserProfileService;
import com.trademaster.userprofile.service.DocumentService;
import com.trademaster.userprofile.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * User Profile MCP Controller
 * 
 * Provides MCP (Multi-Agent Communication Protocol) endpoints for the
 * TradeMaster User Profile Agent. Enables standardized agent-to-agent
 * communication for user management, KYC compliance, document handling,
 * broker configurations, and preferences management within the AgentOS ecosystem.
 * 
 * MCP Capabilities:
 * - Agent registration and discovery
 * - Health status reporting
 * - User profile service coordination
 * - KYC compliance integration
 * - Inter-agent user management
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/mcp/userprofile")
@RequiredArgsConstructor
public class UserProfileMCPController {
    
    private final UserProfileAgent userProfileAgent;
    private final UserProfileCapabilityRegistry capabilityRegistry;
    private final UserProfileService userProfileService;
    private final DocumentService documentService;
    private final UserPreferencesService userPreferencesService;
    
    /**
     * Agent registration endpoint for AgentOS discovery
     */
    @PostMapping("/register")
    public ResponseEntity<AgentRegistrationResponse> registerAgent(
            @RequestBody AgentRegistrationRequest request) {
        
        log.info("Registering user profile agent with AgentOS orchestrator");
        
        try {
            var capabilities = userProfileAgent.getCapabilities();
            var healthScore = userProfileAgent.getHealthScore();
            
            var response = AgentRegistrationResponse.builder()
                .agentId(userProfileAgent.getAgentId())
                .agentType(userProfileAgent.getAgentType())
                .capabilities(capabilities)
                .healthScore(healthScore)
                .status("REGISTERED")
                .registrationTime(System.currentTimeMillis())
                .build();
                
            log.info("User profile agent registered successfully with health score: {}", healthScore);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to register user profile agent", e);
            return ResponseEntity.status(500)
                .body(AgentRegistrationResponse.builder()
                    .status("REGISTRATION_FAILED")
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
            var overallHealth = userProfileAgent.getHealthScore();
            var capabilitySummary = capabilityRegistry.getPerformanceSummary();
            
            var response = AgentHealthResponse.builder()
                .agentId(userProfileAgent.getAgentId())
                .healthScore(overallHealth)
                .status(overallHealth > 0.8 ? "HEALTHY" : overallHealth > 0.5 ? "DEGRADED" : "UNHEALTHY")
                .capabilityHealth(capabilitySummary)
                .lastUpdated(System.currentTimeMillis())
                .build();
                
            log.debug("Agent health check completed - Score: {}", overallHealth);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.status(500)
                .body(AgentHealthResponse.builder()
                    .agentId(userProfileAgent.getAgentId())
                    .status("HEALTH_CHECK_FAILED")
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
            var capabilities = userProfileAgent.getCapabilities();
            var capabilityDetails = Map.of(
                "USER_MANAGEMENT", Map.of(
                    "proficiency", "EXPERT",
                    "description", "Complete user lifecycle management with authentication integration",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate("USER_MANAGEMENT"),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime("USER_MANAGEMENT")
                ),
                "KYC_COMPLIANCE", Map.of(
                    "proficiency", "EXPERT", 
                    "description", "Automated KYC document processing and regulatory compliance",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate("KYC_COMPLIANCE"),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime("KYC_COMPLIANCE")
                ),
                "DOCUMENT_MANAGEMENT", Map.of(
                    "proficiency", "ADVANCED",
                    "description", "Secure document storage with OCR and classification",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate("DOCUMENT_MANAGEMENT"),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime("DOCUMENT_MANAGEMENT")
                ),
                "BROKER_CONFIGURATION", Map.of(
                    "proficiency", "ADVANCED",
                    "description", "Multi-broker account linking and API key management",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate("BROKER_CONFIGURATION"),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime("BROKER_CONFIGURATION")
                ),
                "PREFERENCE_MANAGEMENT", Map.of(
                    "proficiency", "INTERMEDIATE",
                    "description", "User customization and personalization settings",
                    "successRate", capabilityRegistry.getCapabilitySuccessRate("PREFERENCE_MANAGEMENT"),
                    "avgExecutionTime", capabilityRegistry.getCapabilityAverageExecutionTime("PREFERENCE_MANAGEMENT")
                )
            );
            
            var response = AgentCapabilitiesResponse.builder()
                .agentId(userProfileAgent.getAgentId())
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
     * Inter-agent user profile management endpoint
     */
    @PostMapping("/manage-profile")
    public CompletableFuture<ResponseEntity<UserProfileMCPResponse>> manageProfile(
            @RequestBody UserProfileManagementMCPRequest request) {
        
        log.info("Received MCP user profile management request for user: {} from agent: {}", 
                request.getUserId(), request.getRequestingAgentId());
        
        return userProfileAgent.manageUserProfile(
                request.getUserId(), 
                request.getOperation(), 
                request.getProfileData())
            .thenApply(result -> {
                var response = UserProfileMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("SUCCESS")
                    .result(result)
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.info("MCP user profile management completed successfully for user: {}", request.getUserId());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                var response = UserProfileMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("FAILED")
                    .errorMessage(ex.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.error("MCP user profile management failed for user: {}", request.getUserId(), ex);
                return ResponseEntity.status(500).body(response);
            });
    }
    
    /**
     * Inter-agent KYC compliance endpoint
     */
    @PostMapping("/kyc-compliance")
    public CompletableFuture<ResponseEntity<KYCComplianceMCPResponse>> processKYCCompliance(
            @RequestBody KYCComplianceMCPRequest request) {
        
        log.info("Received MCP KYC compliance request for user: {} from agent: {}", 
                request.getUserId(), request.getRequestingAgentId());
        
        return userProfileAgent.processKYCCompliance(
                request.getUserId(),
                request.getDocumentType(),
                request.getDocumentData())
            .thenApply(result -> {
                var response = KYCComplianceMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("SUCCESS")
                    .result(result)
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.info("MCP KYC compliance completed successfully for user: {}", request.getUserId());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                var response = KYCComplianceMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("FAILED")
                    .errorMessage(ex.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.error("MCP KYC compliance failed for user: {}", request.getUserId(), ex);
                return ResponseEntity.status(500).body(response);
            });
    }
    
    /**
     * Inter-agent document management endpoint
     */
    @PostMapping("/manage-documents")
    public CompletableFuture<ResponseEntity<DocumentManagementMCPResponse>> manageDocuments(
            @RequestBody DocumentManagementMCPRequest request) {
        
        log.info("Received MCP document management request for user: {} from agent: {}", 
                request.getUserId(), request.getRequestingAgentId());
        
        return userProfileAgent.manageDocuments(
                request.getUserId(),
                request.getOperation(),
                request.getDocumentId(),
                request.getDocumentData())
            .thenApply(result -> {
                var response = DocumentManagementMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("SUCCESS")
                    .result(result)
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.info("MCP document management completed successfully for user: {}", request.getUserId());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                var response = DocumentManagementMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("FAILED")
                    .errorMessage(ex.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.error("MCP document management failed for user: {}", request.getUserId(), ex);
                return ResponseEntity.status(500).body(response);
            });
    }
    
    /**
     * Inter-agent broker configuration endpoint
     */
    @PostMapping("/configure-broker")
    public CompletableFuture<ResponseEntity<BrokerConfigurationMCPResponse>> configureBroker(
            @RequestBody BrokerConfigurationMCPRequest request) {
        
        log.info("Received MCP broker configuration request for user: {} broker: {} from agent: {}", 
                request.getUserId(), request.getBrokerName(), request.getRequestingAgentId());
        
        return userProfileAgent.configureBrokers(
                request.getUserId(),
                request.getOperation(),
                request.getBrokerName(),
                request.getConfigurationData())
            .thenApply(result -> {
                var response = BrokerConfigurationMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("SUCCESS")
                    .result(result)
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.info("MCP broker configuration completed successfully for user: {} broker: {}", 
                        request.getUserId(), request.getBrokerName());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                var response = BrokerConfigurationMCPResponse.builder()
                    .requestId(request.getRequestId())
                    .status("FAILED")
                    .errorMessage(ex.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                    .build();
                    
                log.error("MCP broker configuration failed for user: {} broker: {}", 
                        request.getUserId(), request.getBrokerName(), ex);
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
            var overallHealth = userProfileAgent.getHealthScore();
            
            var response = AgentMetricsResponse.builder()
                .agentId(userProfileAgent.getAgentId())
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
    
    // ========== REST API ENDPOINTS AS SPECIFIED IN STORY 5 ==========
    
    /**
     * Get complete user profile
     */
    @GetMapping("/api/v1/mcp/userprofile/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String userId) {
        
        try {
            log.info("Retrieving complete user profile for user: {}", userId);
            
            var profile = userProfileService.getProfile(userId);
            var preferences = userPreferencesService.getUserPreferences(userId);
            var brokerConfig = userProfileService.listBrokerConnections(userId);
            
            var response = Map.of(
                "userId", userId,
                "profile", profile,
                "preferences", preferences,
                "brokerConfigurations", brokerConfig,
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("USER_MANAGEMENT");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve user profile for user: {}", userId, e);
            capabilityRegistry.recordFailedExecution("USER_MANAGEMENT", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage(), "userId", userId));
        }
    }
    
    /**
     * Update user profile data
     */
    @PostMapping("/api/v1/mcp/userprofile/updateProfile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(@RequestBody Map<String, Object> profileData) {
        
        try {
            var userId = (String) profileData.get("userId");
            log.info("Updating user profile for user: {}", userId);
            
            var updatedProfile = userProfileService.updateProfile(userId, profileData);
            
            var response = Map.of(
                "status", "SUCCESS",
                "userId", userId,
                "updatedProfile", updatedProfile,
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("USER_MANAGEMENT");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to update user profile", e);
            capabilityRegistry.recordFailedExecution("USER_MANAGEMENT", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }
    
    /**
     * Get KYC compliance status
     */
    @GetMapping("/api/v1/mcp/userprofile/kycStatus/{userId}")
    public ResponseEntity<Map<String, Object>> getKYCStatus(@PathVariable String userId) {
        
        try {
            log.info("Retrieving KYC compliance status for user: {}", userId);
            
            var kycStatus = profileValidationService.validateKYCCompliance(userId);
            var documents = documentService.listUserDocuments(userId);
            
            var response = Map.of(
                "userId", userId,
                "kycStatus", kycStatus,
                "documents", documents,
                "complianceScore", kycStatus.getComplianceScore(),
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("KYC_COMPLIANCE");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve KYC status for user: {}", userId, e);
            capabilityRegistry.recordFailedExecution("KYC_COMPLIANCE", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage(), "userId", userId));
        }
    }
    
    /**
     * Process KYC documents
     */
    @PostMapping("/api/v1/mcp/userprofile/processDocument")
    public ResponseEntity<Map<String, Object>> processKYCDocument(@RequestBody Map<String, Object> documentRequest) {
        
        try {
            var userId = (String) documentRequest.get("userId");
            var documentType = (String) documentRequest.get("documentType");
            log.info("Processing KYC document for user: {} type: {}", userId, documentType);
            
            var documentResult = documentService.processKYCDocument(userId, documentType, documentRequest.get("documentData"));
            var complianceResult = profileValidationService.validateKYCCompliance(userId);
            
            var response = Map.of(
                "status", "SUCCESS",
                "userId", userId,
                "documentType", documentType,
                "documentResult", documentResult,
                "complianceStatus", complianceResult.getStatus(),
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("KYC_COMPLIANCE");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to process KYC document", e);
            capabilityRegistry.recordFailedExecution("KYC_COMPLIANCE", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }
    
    /**
     * Get broker configurations
     */
    @GetMapping("/api/v1/mcp/userprofile/brokerConfig/{userId}")
    public ResponseEntity<Map<String, Object>> getBrokerConfigurations(@PathVariable String userId) {
        
        try {
            log.info("Retrieving broker configurations for user: {}", userId);
            
            var brokerConnections = userProfileService.listBrokerConnections(userId);
            
            var response = Map.of(
                "userId", userId,
                "brokerConfigurations", brokerConnections,
                "connectionCount", brokerConnections.size(),
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("BROKER_CONFIGURATION");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve broker configurations for user: {}", userId, e);
            capabilityRegistry.recordFailedExecution("BROKER_CONFIGURATION", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage(), "userId", userId));
        }
    }
    
    /**
     * Configure broker connection
     */
    @PostMapping("/api/v1/mcp/userprofile/configureBroker")
    public ResponseEntity<Map<String, Object>> configureBrokerConnection(@RequestBody Map<String, Object> configRequest) {
        
        try {
            var userId = (String) configRequest.get("userId");
            var brokerName = (String) configRequest.get("brokerName");
            log.info("Configuring broker connection for user: {} broker: {}", userId, brokerName);
            
            var configResult = userProfileService.configureBroker(userId, brokerName, configRequest.get("configurationData"));
            var connectionTest = userProfileService.testBrokerConnection(userId, brokerName);
            
            var response = Map.of(
                "status", "SUCCESS",
                "userId", userId,
                "brokerName", brokerName,
                "configurationResult", configResult,
                "connectionStatus", connectionTest,
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("BROKER_CONFIGURATION");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to configure broker connection", e);
            capabilityRegistry.recordFailedExecution("BROKER_CONFIGURATION", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }
    
    /**
     * Get user preferences
     */
    @GetMapping("/api/v1/mcp/userprofile/preferences/{userId}")
    public ResponseEntity<Map<String, Object>> getUserPreferences(@PathVariable String userId) {
        
        try {
            log.info("Retrieving user preferences for user: {}", userId);
            
            var preferences = userPreferencesService.getUserPreferences(userId);
            
            var response = Map.of(
                "userId", userId,
                "preferences", preferences,
                "lastUpdated", preferences.getUpdatedAt(),
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("PREFERENCE_MANAGEMENT");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve user preferences for user: {}", userId, e);
            capabilityRegistry.recordFailedExecution("PREFERENCE_MANAGEMENT", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage(), "userId", userId));
        }
    }
    
    /**
     * Update user preferences
     */
    @PostMapping("/api/v1/mcp/userprofile/updatePreferences")
    public ResponseEntity<Map<String, Object>> updateUserPreferences(@RequestBody Map<String, Object> preferencesRequest) {
        
        try {
            var userId = (String) preferencesRequest.get("userId");
            var category = (String) preferencesRequest.get("category");
            log.info("Updating user preferences for user: {} category: {}", userId, category);
            
            var updatedPreferences = userPreferencesService.updateUserPreferences(userId, category, preferencesRequest.get("preferencesData"));
            
            var response = Map.of(
                "status", "SUCCESS",
                "userId", userId,
                "category", category,
                "updatedPreferences", updatedPreferences,
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("PREFERENCE_MANAGEMENT");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to update user preferences", e);
            capabilityRegistry.recordFailedExecution("PREFERENCE_MANAGEMENT", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }
    
    /**
     * Validate regulatory compliance
     */
    @PostMapping("/api/v1/mcp/userprofile/validateCompliance")
    public ResponseEntity<Map<String, Object>> validateCompliance(@RequestBody Map<String, Object> complianceRequest) {
        
        try {
            var userId = (String) complianceRequest.get("userId");
            var complianceType = (String) complianceRequest.get("complianceType");
            log.info("Validating regulatory compliance for user: {} type: {}", userId, complianceType);
            
            var complianceResult = profileValidationService.validateRegulatoryCompliance(userId, complianceType);
            var auditTrail = profileValidationService.getComplianceAuditTrail(userId);
            
            var response = Map.of(
                "status", "SUCCESS",
                "userId", userId,
                "complianceType", complianceType,
                "complianceResult", complianceResult,
                "auditTrail", auditTrail,
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("KYC_COMPLIANCE");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to validate compliance for user: {}", (String) complianceRequest.get("userId"), e);
            capabilityRegistry.recordFailedExecution("KYC_COMPLIANCE", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }
    
    /**
     * Get user activity audit trail
     */
    @GetMapping("/api/v1/mcp/userprofile/auditTrail/{userId}")
    public ResponseEntity<Map<String, Object>> getAuditTrail(@PathVariable String userId,
                                                            @RequestParam(defaultValue = "30") int days) {
        
        try {
            log.info("Retrieving audit trail for user: {} days: {}", userId, days);
            
            var auditTrail = userProfileService.getAuditTrail(userId, days);
            var complianceEvents = profileValidationService.getComplianceAuditTrail(userId);
            
            var response = Map.of(
                "userId", userId,
                "auditTrail", auditTrail,
                "complianceEvents", complianceEvents,
                "periodDays", days,
                "totalEvents", auditTrail.size(),
                "timestamp", System.currentTimeMillis()
            );
            
            capabilityRegistry.recordSuccessfulExecution("USER_MANAGEMENT");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve audit trail for user: {}", userId, e);
            capabilityRegistry.recordFailedExecution("USER_MANAGEMENT", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage(), "userId", userId));
        }
    }
}

// MCP Request/Response Data Classes

@lombok.Builder
@lombok.Data
class AgentRegistrationRequest {
    private String orchestratorId;
    private String requestId;
    private Long timestamp;
    private Map<String, Object> orchestratorConfig;
}

@lombok.Builder
@lombok.Data
class AgentRegistrationResponse {
    private String agentId;
    private String agentType;
    private java.util.List<String> capabilities;
    private Double healthScore;
    private String status;
    private String errorMessage;
    private Long registrationTime;
}

@lombok.Builder
@lombok.Data
class AgentHealthResponse {
    private String agentId;
    private Double healthScore;
    private String status;
    private Map<String, String> capabilityHealth;
    private String errorMessage;
    private Long lastUpdated;
}

@lombok.Builder
@lombok.Data
class AgentCapabilitiesResponse {
    private String agentId;
    private java.util.List<String> capabilities;
    private Map<String, Map<String, Object>> capabilityDetails;
    private Integer totalCapabilities;
    private String errorMessage;
}

@lombok.Builder
@lombok.Data
class UserProfileManagementMCPRequest {
    private String requestId;
    private String requestingAgentId;
    private String userId;
    private String operation;
    private Object profileData;
    private Long timestamp;
}

@lombok.Builder
@lombok.Data
class UserProfileMCPResponse {
    private String requestId;
    private String status;
    private String result;
    private String errorMessage;
    private Long processingTimeMs;
}

@lombok.Builder
@lombok.Data
class KYCComplianceMCPRequest {
    private String requestId;
    private String requestingAgentId;
    private String userId;
    private String documentType;
    private String documentData;
    private Long timestamp;
}

@lombok.Builder
@lombok.Data
class KYCComplianceMCPResponse {
    private String requestId;
    private String status;
    private String result;
    private String errorMessage;
    private Long processingTimeMs;
}

@lombok.Builder
@lombok.Data
class DocumentManagementMCPRequest {
    private String requestId;
    private String requestingAgentId;
    private String userId;
    private String operation;
    private String documentId;
    private Object documentData;
    private Long timestamp;
}

@lombok.Builder
@lombok.Data
class DocumentManagementMCPResponse {
    private String requestId;
    private String status;
    private String result;
    private String errorMessage;
    private Long processingTimeMs;
}

@lombok.Builder
@lombok.Data
class BrokerConfigurationMCPRequest {
    private String requestId;
    private String requestingAgentId;
    private String userId;
    private String operation;
    private String brokerName;
    private Object configurationData;
    private Long timestamp;
}

@lombok.Builder
@lombok.Data
class BrokerConfigurationMCPResponse {
    private String requestId;
    private String status;
    private String result;
    private String errorMessage;
    private Long processingTimeMs;
}

@lombok.Builder
@lombok.Data
class AgentMetricsResponse {
    private String agentId;
    private Double healthScore;
    private Map<String, String> capabilityMetrics;
    private String errorMessage;
    private Long timestamp;
}