package com.trademaster.brokerauth.controller;

import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.service.BrokerAuthenticationService;
import com.trademaster.brokerauth.service.BrokerRateLimitService;
import com.trademaster.brokerauth.service.CredentialManagementService.BrokerCredentials;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Broker Authentication Controller
 * 
 * REST API endpoints for broker authentication and session management.
 * Provides secure endpoints for OAuth flows, credential management, and session operations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/broker-auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Broker Authentication", description = "Broker authentication and session management APIs")
public class BrokerAuthController {
    
    private final BrokerAuthenticationService authenticationService;
    private final BrokerRateLimitService rateLimitService;
    
    /**
     * Initiate broker authentication flow
     */
    @PostMapping("/initiate/{brokerType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Initiate broker authentication", 
               description = "Start the authentication process for a specific broker")
    public CompletableFuture<ResponseEntity<AuthFlowResponse>> initiateAuth(
            @Parameter(description = "Broker type to authenticate with")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        return authenticationService.initiateAuthFlow(userId, brokerType, clientIp, userAgent)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(AuthFlowResponse.fromResult(result));
                    } else {
                        HttpStatus status = mapErrorStatus(result.getStatus());
                        return ResponseEntity.status(status)
                                .body(AuthFlowResponse.fromResult(result));
                    }
                });
    }
    
    /**
     * Complete broker authentication flow
     */
    @PostMapping("/complete/{brokerType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Complete broker authentication", 
               description = "Complete the authentication process with authorization code or credentials")
    public CompletableFuture<ResponseEntity<AuthFlowResponse>> completeAuth(
            @Parameter(description = "Broker type")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Valid @RequestBody AuthCompleteRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        return authenticationService.completeAuthFlow(
                userId, brokerType, request.getAuthorizationCode(), 
                request.getState(), clientIp, userAgent)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(AuthFlowResponse.fromResult(result));
                    } else {
                        HttpStatus status = mapErrorStatus(result.getStatus());
                        return ResponseEntity.status(status)
                                .body(AuthFlowResponse.fromResult(result));
                    }
                });
    }
    
    /**
     * Store broker credentials
     */
    @PostMapping("/credentials/{brokerType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Store broker credentials", 
               description = "Securely store encrypted broker credentials")
    public CompletableFuture<ResponseEntity<AuthFlowResponse>> storeCredentials(
            @Parameter(description = "Broker type")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Valid @RequestBody BrokerCredentials credentials) {
        
        // Ensure broker type matches
        credentials.setBrokerType(brokerType);
        
        return authenticationService.storeCredentials(userId, brokerType, credentials)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(AuthFlowResponse.fromResult(result));
                    } else {
                        HttpStatus status = mapErrorStatus(result.getStatus());
                        return ResponseEntity.status(status)
                                .body(AuthFlowResponse.fromResult(result));
                    }
                });
    }
    
    /**
     * Get active sessions for user and broker
     */
    @GetMapping("/sessions/{brokerType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get active sessions", 
               description = "Retrieve all active sessions for user and broker")
    public CompletableFuture<ResponseEntity<SessionsResponse>> getActiveSessions(
            @Parameter(description = "Broker type")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {
        
        return authenticationService.getActiveSessions(userId, brokerType)
                .thenApply(sessions -> ResponseEntity.ok(SessionsResponse.builder()
                        .brokerType(brokerType)
                        .userId(userId)
                        .sessions(sessions)
                        .totalSessions(sessions.size())
                        .build()));
    }
    
    /**
     * Get broker account status
     */
    @GetMapping("/status/{brokerType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get broker account status", 
               description = "Get the current status of broker account configuration")
    public CompletableFuture<ResponseEntity<BrokerAuthenticationService.BrokerAccountStatus>> getAccountStatus(
            @Parameter(description = "Broker type")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {
        
        return authenticationService.getBrokerAccountStatus(userId, brokerType)
                .thenApply(ResponseEntity::ok);
    }
    
    /**
     * Revoke session
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Revoke session", 
               description = "Revoke an active broker session")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> revokeSession(
            @Parameter(description = "Session ID to revoke")
            @PathVariable String sessionId,
            @Parameter(description = "Reason for revocation")
            @RequestParam(required = false, defaultValue = "User requested") String reason) {
        
        return authenticationService.revokeSession(sessionId, reason)
                .thenApply(success -> {
                    if (success) {
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Session revoked successfully",
                                "sessionId", sessionId
                        ));
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of(
                                        "success", false,
                                        "message", "Failed to revoke session",
                                        "sessionId", sessionId
                                ));
                    }
                });
    }
    
    /**
     * Refresh session if needed
     */
    @PostMapping("/sessions/{sessionId}/refresh")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Refresh session", 
               description = "Refresh session tokens if needed")
    public CompletableFuture<ResponseEntity<AuthFlowResponse>> refreshSession(
            @Parameter(description = "Session ID to refresh")
            @PathVariable String sessionId) {
        
        return authenticationService.refreshSessionIfNeeded(sessionId)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(AuthFlowResponse.fromResult(result));
                    } else {
                        HttpStatus status = mapErrorStatus(result.getStatus());
                        return ResponseEntity.status(status)
                                .body(AuthFlowResponse.fromResult(result));
                    }
                });
    }
    
    /**
     * Get rate limit usage stats
     */
    @GetMapping("/rate-limits/{brokerType}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get rate limit usage", 
               description = "Get current rate limit usage statistics")
    public CompletableFuture<ResponseEntity<BrokerRateLimitService.UsageStats>> getRateLimitUsage(
            @Parameter(description = "Broker type")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {
        
        return rateLimitService.getUsageStats(brokerType, userId)
                .thenApply(ResponseEntity::ok);
    }
    
    /**
     * OAuth callback handler (for brokers that support it)
     */
    @GetMapping("/callback/{brokerType}")
    @Operation(summary = "OAuth callback", 
               description = "Handle OAuth callback from broker")
    public ResponseEntity<String> handleOAuthCallback(
            @Parameter(description = "Broker type")
            @PathVariable BrokerType brokerType,
            @Parameter(description = "Authorization code")
            @RequestParam String code,
            @Parameter(description = "State parameter")
            @RequestParam String state,
            @Parameter(description = "Error if any")
            @RequestParam(required = false) String error) {
        
        if (error != null) {
            log.warn("OAuth callback received error for broker {}: {}", brokerType, error);
            return ResponseEntity.badRequest()
                    .body("Authentication failed: " + error);
        }
        
        // Return a simple HTML page with the code and state for client-side handling
        String html = String.format("""
                <html>
                <head><title>Authentication Success</title></head>
                <body>
                    <h2>Authentication Successful</h2>
                    <p>Please return to the TradeMaster app to complete the process.</p>
                    <script>
                        // Post message to parent window if in popup
                        if (window.opener) {
                            window.opener.postMessage({
                                type: 'auth_callback',
                                broker: '%s',
                                code: '%s',
                                state: '%s'
                            }, '*');
                            window.close();
                        }
                    </script>
                </body>
                </html>
                """, brokerType, code, state);
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private HttpStatus mapErrorStatus(BrokerAuthenticationService.AuthFlowStatus status) {
        return switch (status) {
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case MISSING_CREDENTIALS, INVALID_CREDENTIALS -> HttpStatus.BAD_REQUEST;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    // Request/Response DTOs
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuthCompleteRequest {
        private String authorizationCode;
        private String state;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuthFlowResponse {
        private boolean success;
        private String status;
        private String message;
        private String authorizationUrl;
        private String state;
        private SessionInfo session;
        
        public static AuthFlowResponse fromResult(BrokerAuthenticationService.AuthFlowResult result) {
            AuthFlowResponseBuilder builder = AuthFlowResponse.builder()
                    .success(result.isSuccess())
                    .status(result.getStatus().toString())
                    .message(result.getMessage())
                    .authorizationUrl(result.getAuthorizationUrl())
                    .state(result.getState());
                    
            if (result.getSession() != null) {
                builder.session(SessionInfo.fromSession(result.getSession()));
            }
            
            return builder.build();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionInfo {
        private String sessionId;
        private String brokerType;
        private String status;
        private String expiresAt;
        private boolean needsRefresh;
        
        public static SessionInfo fromSession(BrokerSession session) {
            return SessionInfo.builder()
                    .sessionId(session.getSessionId())
                    .brokerType(session.getBrokerType().toString())
                    .status(session.getStatus().toString())
                    .expiresAt(session.getExpiresAt().toString())
                    .needsRefresh(session.needsRefresh())
                    .build();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionsResponse {
        private BrokerType brokerType;
        private Long userId;
        private List<BrokerSession> sessions;
        private int totalSessions;
    }
}