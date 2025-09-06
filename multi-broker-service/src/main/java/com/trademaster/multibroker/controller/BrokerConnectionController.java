package com.trademaster.multibroker.controller;

import com.trademaster.multibroker.dto.*;
import com.trademaster.multibroker.entity.BrokerConnection;
import com.trademaster.multibroker.entity.BrokerType;
import com.trademaster.multibroker.service.BrokerIntegrationService;
import com.trademaster.multibroker.service.BrokerOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Broker Connection REST Controller
 * 
 * MANDATORY: Zero Trust Security + Virtual Threads + Functional Programming
 * MANDATORY: OpenAPI Documentation + Validation + Error Handling
 * 
 * RESTful API controller for managing broker connections and OAuth flows.
 * Implements Zero Trust security with comprehensive input validation and
 * audit logging for all broker management operations.
 * 
 * Security Features:
 * - JWT-based authentication required for all endpoints
 * - Role-based access control (TRADER role minimum)
 * - Request correlation IDs for audit trail
 * - Rate limiting protection (100 requests/minute per user)
 * - Input validation and sanitization
 * - Comprehensive error responses with correlation IDs
 * 
 * Performance Features:
 * - Virtual Thread-based async operations
 * - Connection pooling for database operations
 * - Circuit breaker protection for external calls
 * - Response caching for broker metadata
 * 
 * API Endpoints:
 * - POST /api/v1/brokers/connect/initiate - Start OAuth flow
 * - POST /api/v1/brokers/connect/complete - Complete OAuth flow
 * - GET /api/v1/brokers/connections - List user connections
 * - GET /api/v1/brokers/portfolio/consolidated - Get consolidated portfolio
 * - DELETE /api/v1/brokers/connections/{id} - Disconnect broker
 * - GET /api/v1/brokers/supported - List supported brokers
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Production Multi-Broker API)
 */
@RestController
@RequestMapping("/api/v1/brokers")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Broker Management", description = "Multi-broker connection and portfolio management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BrokerConnectionController {
    
    private final BrokerIntegrationService integrationService;
    private final BrokerOAuthService oauthService;
    
    /**
     * Initiate OAuth flow for broker connection
     * 
     * MANDATORY: Real OAuth 2.0 flow - no mocks or stubs
     * 
     * @param request OAuth initiation request
     * @param authentication User authentication context
     * @return OAuth authorization URL and state
     */
    @PostMapping("/connect/initiate")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(
        summary = "Initiate broker OAuth connection",
        description = "Start OAuth 2.0 flow to connect user account with supported broker. " +
                     "Returns authorization URL where user should be redirected for broker login."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OAuth flow initiated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OAuthInitiateResponse> initiateConnection(
            @Valid @RequestBody BrokerConnectRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String correlationId = generateCorrelationId();
        
        log.info("Initiating OAuth flow for user: {}, broker: {}, correlationId: {}", 
                userId, request.getBrokerType(), correlationId);
        
        try {
            // Validate broker type is supported and active
            if (!request.getBrokerType().isActive()) {
                log.warn("Attempted connection to inactive broker: {}", request.getBrokerType());
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body(OAuthInitiateResponse.error("Broker is currently not available"));
            }
            
            // Check if user already has connection to this broker
            boolean existingConnection = integrationService.hasActiveConnection(userId, request.getBrokerType());
            if (existingConnection) {
                log.warn("User {} already has active connection to {}", userId, request.getBrokerType());
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body(OAuthInitiateResponse.error("Already connected to this broker"));
            }
            
            // Initiate OAuth flow
            String authUrl = oauthService.initiateOAuthFlow(
                userId, 
                request.getBrokerType(), 
                request.getRedirectUri()
            );
            
            OAuthInitiateResponse response = OAuthInitiateResponse.builder()
                .authorizationUrl(authUrl)
                .brokerType(request.getBrokerType())
                .correlationId(correlationId)
                .expiresIn(600) // 10 minutes
                .build();
            
            log.info("OAuth flow initiated successfully for user: {}, broker: {}", 
                    userId, request.getBrokerType());
            
            return ResponseEntity.ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
            
        } catch (Exception e) {
            log.error("Failed to initiate OAuth flow for user: {}, broker: {}, error: {}", 
                     userId, request.getBrokerType(), e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Correlation-ID", correlationId)
                .body(OAuthInitiateResponse.error("Failed to initiate broker connection"));
        }
    }
    
    /**
     * Complete OAuth flow and establish broker connection
     * 
     * @param request OAuth completion request with authorization code
     * @param authentication User authentication context
     * @return Broker connection details
     */
    @PostMapping("/connect/complete")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(
        summary = "Complete broker OAuth connection",
        description = "Complete OAuth 2.0 flow by exchanging authorization code for tokens " +
                     "and establishing secure broker connection with encrypted token storage."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Broker connected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid authorization code or state"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Connection failed")
    })
    public CompletableFuture<ResponseEntity<BrokerConnectionResponse>> completeConnection(
            @Valid @RequestBody OAuthCompleteRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String correlationId = generateCorrelationId();
        
        log.info("Completing OAuth flow for user: {}, correlationId: {}", userId, correlationId);
        
        return oauthService.exchangeCodeForTokens(request.getBrokerType(), request.getCode(), request.getState(), request.redirectUri())
            .thenCompose(tokens -> {
                if (tokens.isEmpty()) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.badRequest()
                            .header("X-Correlation-ID", correlationId)
                            .body(BrokerConnectionResponse.error("Invalid authorization code or state"))
                    );
                }
                
                // Create broker connection with tokens
                return integrationService.connectBrokerWithTokens(userId, request.getBrokerType(), tokens.get())
                    .thenApply(connection -> ResponseEntity.ok()
                        .header("X-Correlation-ID", correlationId)
                        .body(BrokerConnectionResponse.from(connection)))
                    .exceptionally(throwable -> {
                        log.error("Failed to create broker connection: {}", throwable.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .header("X-Correlation-ID", correlationId)
                            .body(BrokerConnectionResponse.error("Failed to establish broker connection"));
                    });
            })
            .exceptionally(throwable -> {
                log.error("OAuth token exchange failed: {}", throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Correlation-ID", correlationId)
                    .body(BrokerConnectionResponse.error("OAuth flow completion failed"));
            });
    }
    
    /**
     * Get all broker connections for authenticated user
     * 
     * @param authentication User authentication context
     * @return List of user's broker connections
     */
    @GetMapping("/connections")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(
        summary = "Get user broker connections",
        description = "Retrieve all broker connections for authenticated user with current status " +
                     "and health information. Excludes sensitive token data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connections retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<BrokerConnectionResponse>> getConnections(Authentication authentication) {
        
        String userId = authentication.getName();
        String correlationId = generateCorrelationId();
        
        log.debug("Retrieving broker connections for user: {}", userId);
        
        try {
            List<BrokerConnection> connections = integrationService.getUserConnections(userId);
            
            List<BrokerConnectionResponse> responses = connections.stream()
                .map(BrokerConnectionResponse::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok()
                .header("X-Correlation-ID", correlationId)
                .header("X-Total-Connections", String.valueOf(responses.size()))
                .body(responses);
            
        } catch (Exception e) {
            log.error("Failed to retrieve connections for user: {}, error: {}", userId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Correlation-ID", correlationId)
                .body(List.of());
        }
    }
    
    /**
     * Get consolidated portfolio across all brokers
     * 
     * @param authentication User authentication context
     * @param includeBreakdown Include broker-wise breakdown in response
     * @return Consolidated portfolio with aggregated metrics
     */
    @GetMapping("/portfolio/consolidated")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(
        summary = "Get consolidated portfolio",
        description = "Retrieve consolidated portfolio across all connected brokers with real-time " +
                     "market prices, P&L calculations, and optional broker-wise breakdown."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Portfolio retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "No broker connections found"),
        @ApiResponse(responseCode = "500", description = "Portfolio aggregation failed")
    })
    public CompletableFuture<ResponseEntity<ConsolidatedPortfolio>> getConsolidatedPortfolio(
            Authentication authentication,
            @Parameter(description = "Include detailed broker breakdown")
            @RequestParam(defaultValue = "true") boolean includeBreakdown) {
        
        String userId = authentication.getName();
        String correlationId = generateCorrelationId();
        
        log.info("Retrieving consolidated portfolio for user: {}, includeBreakdown: {}", 
                userId, includeBreakdown);
        
        return integrationService.getConsolidatedPortfolio(userId)
            .thenApply(portfolio -> {
                // Remove broker breakdown if not requested (for performance)
                if (!includeBreakdown) {
                    portfolio = ConsolidatedPortfolio.builder()
                        .userId(portfolio.userId())
                        .totalValue(portfolio.totalValue())
                        .totalCost(portfolio.totalCost())
                        .unrealizedPnL(portfolio.unrealizedPnL())
                        .unrealizedPnLPercent(portfolio.unrealizedPnLPercent())
                        .dayChange(portfolio.dayChange())
                        .dayChangePercent(portfolio.dayChangePercent())
                        .positions(portfolio.positions())
                        .brokerBreakdown(List.of()) // Empty breakdown
                        .assetAllocation(portfolio.assetAllocation())
                        .lastUpdated(portfolio.lastUpdated())
                        .dataFreshness(portfolio.dataFreshness())
                        .build();
                }
                
                return ResponseEntity.ok()
                    .header("X-Correlation-ID", correlationId)
                    .header("X-Position-Count", String.valueOf(portfolio.getPositionCount()))
                    .header("X-Broker-Count", String.valueOf(portfolio.getBrokerCount()))
                    .header("X-Data-Freshness", portfolio.dataFreshness().name())
                    .body(portfolio);
            })
            .exceptionally(throwable -> {
                log.error("Failed to retrieve consolidated portfolio for user: {}, error: {}", 
                         userId, throwable.getMessage());
                
                if (throwable.getCause() instanceof com.trademaster.multibroker.exception.NoBrokerConnectionsException) {
                    return ResponseEntity.notFound()
                        .header("X-Correlation-ID", correlationId)
                        .build();
                }
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Correlation-ID", correlationId)
                    .build();
            });
    }
    
    /**
     * Disconnect broker and remove connection
     * 
     * @param connectionId Broker connection ID to disconnect
     * @param authentication User authentication context
     * @return Success/failure response
     */
    @DeleteMapping("/connections/{connectionId}")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(
        summary = "Disconnect broker",
        description = "Disconnect and remove broker connection. This will stop health monitoring " +
                     "and clear stored OAuth tokens. Portfolio data will be removed from aggregation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Broker disconnected successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or connection not owned"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Disconnection failed")
    })
    public ResponseEntity<Void> disconnectBroker(
            @Parameter(description = "Broker connection ID") @PathVariable UUID connectionId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String correlationId = generateCorrelationId();
        
        log.info("Disconnecting broker for user: {}, connectionId: {}", userId, connectionId);
        
        try {
            integrationService.disconnectBroker(userId, connectionId);
            
            return ResponseEntity.noContent()
                .header("X-Correlation-ID", correlationId)
                .build();
            
        } catch (com.trademaster.multibroker.exception.BrokerConnectionNotFoundException e) {
            return ResponseEntity.notFound()
                .header("X-Correlation-ID", correlationId)
                .build();
                
        } catch (com.trademaster.multibroker.exception.UnauthorizedBrokerAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("X-Correlation-ID", correlationId)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to disconnect broker for user: {}, connectionId: {}, error: {}", 
                     userId, connectionId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Correlation-ID", correlationId)
                .build();
        }
    }
    
    /**
     * Get list of supported brokers
     * 
     * @return List of supported brokers with capabilities
     */
    @GetMapping("/supported")
    @Operation(
        summary = "Get supported brokers",
        description = "Retrieve list of supported brokers with their capabilities, " +
                     "API versions, and current status. No authentication required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Supported brokers retrieved successfully")
    })
    public ResponseEntity<List<BrokerInfo>> getSupportedBrokers() {
        
        String correlationId = generateCorrelationId();
        
        try {
            List<BrokerInfo> supportedBrokers = integrationService.getSupportedBrokers();
            
            return ResponseEntity.ok()
                .header("X-Correlation-ID", correlationId)
                .header("X-Supported-Count", String.valueOf(supportedBrokers.size()))
                .body(supportedBrokers);
            
        } catch (Exception e) {
            log.error("Failed to retrieve supported brokers: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Correlation-ID", correlationId)
                .body(List.of());
        }
    }
    
    /**
     * Health check endpoint for broker connections
     * 
     * @param authentication User authentication context
     * @return Health status of all broker connections
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(
        summary = "Check broker connections health",
        description = "Get health status and performance metrics for all user broker connections"
    )
    public ResponseEntity<BrokerHealthSummary> getBrokerHealth(Authentication authentication) {
        
        String userId = authentication.getName();
        String correlationId = generateCorrelationId();
        
        try {
            BrokerHealthSummary healthSummary = integrationService.getBrokerHealthSummary(userId);
            
            return ResponseEntity.ok()
                .header("X-Correlation-ID", correlationId)
                .body(healthSummary);
            
        } catch (Exception e) {
            log.error("Failed to retrieve broker health for user: {}, error: {}", userId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Correlation-ID", correlationId)
                .build();
        }
    }
    
    /**
     * Generate correlation ID for request tracking
     * 
     * @return Unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}