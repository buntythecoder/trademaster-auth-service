package com.trademaster.auth.agentos;

import com.trademaster.auth.constants.AuthConstants;
import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * AgentOS Authentication Agent
 * 
 * Provides comprehensive authentication and security capabilities to the TradeMaster 
 * Agent ecosystem. Implements structured concurrency patterns for high-performance
 * authentication operations and integrates with the MCP (Multi-Agent Communication Protocol).
 * 
 * Agent Capabilities:
 * - USER_AUTHENTICATION: JWT-based authentication and session management
 * - MULTI_FACTOR_AUTH: MFA setup, verification, and recovery
 * - SECURITY_AUDIT: Security event logging and compliance monitoring
 * - SESSION_MANAGEMENT: User session lifecycle and security controls
 * - DEVICE_TRUST: Device fingerprinting and trusted device management
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationAgent implements AgentOSComponent {
    
    private final AuthenticationService authenticationService;
    private final MfaService mfaService;
    private final SecurityAuditService securityAuditService;
    private final SessionManagementService sessionManagementService;
    private final DeviceTrustService deviceTrustService;
    private final AuthCapabilityRegistry capabilityRegistry;
    
    /**
     * Handles user authentication requests using structured concurrency
     * for coordinated authentication processing and security validation.
     */
    @EventHandler(event = "AuthenticationRequest")
    public CompletableFuture<AuthenticationResponse> handleAuthentication(
            AuthenticationRequest request) {
        
        log.info("Processing authentication request for user: {}", request.getEmail());
        
        return executeCoordinatedAuthentication(
            request.getRequestId(),
            List.of(
                () -> validateCredentials(request),
                () -> checkAccountStatus(request),
                () -> validateDeviceTrust(request),
                () -> createUserSession(request),
                () -> auditAuthenticationAttempt(request)
            ),
            Duration.ofMillis(200)
        );
    }
    
    /**
     * User authentication capability with expert proficiency
     */
    @AgentCapability(name = AuthConstants.CAPABILITY_USER_AUTHENTICATION, proficiency = AuthConstants.PROFICIENCY_EXPERT)
    public CompletableFuture<String> authenticateUser(String username, String password, String deviceId) {
        return CompletableFuture.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                log.info("Authenticating user: {} from device: {}", username, deviceId);

                var authRequest = AuthenticationRequest.builder()
                    .email(username) // username parameter is actually email
                    .password(password)
                    .deviceInfo(deviceId) // deviceId maps to deviceInfo field
                    .build();
                var authResult = authenticationService.login(authRequest, null);
                String result = authResult.fold(
                    error -> {
                        throw new RuntimeException("Authentication failed: " + error);
                    },
                    response -> {
                        var sessionId = sessionManagementService.createSession(
                            response.getUser().getId(),
                            deviceId,
                            AuthConstants.AGENT_REQUEST_PREFIX + response.getUser().getId(),
                            AuthConstants.AGENT_USER_AGENT,
                            null // sessionData
                        );
                        return String.format("User %s authenticated successfully with session: %s",
                                           username, sessionId);
                    }
                );
                capabilityRegistry.recordSuccessfulExecution(AuthConstants.CAPABILITY_USER_AUTHENTICATION);
                return result;
            })
            .onFailure(error -> {
                log.error("Failed to authenticate user: {}", username);
                capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_USER_AUTHENTICATION, new RuntimeException(error));
            })
            .orElseThrow(error -> new RuntimeException("Authentication failed: " + error))
        );
    }
    
    /**
     * Multi-factor authentication capability with expert proficiency
     */
    @AgentCapability(name = AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH, proficiency = AuthConstants.PROFICIENCY_EXPERT)
    public CompletableFuture<String> setupMFA(String userId, String mfaType) {
        return CompletableFuture.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                log.info("Setting up MFA for user: {} type: {}", userId, mfaType);

                var mfaSetup = mfaService.setupTotpMfa(userId, "session_" + userId);
                securityAuditService.logSecurityEvent(Long.valueOf(userId), "MFA_SETUP", "LOW",
                    "127.0.0.1", "AuthenticationAgent", Map.of("action", "MFA setup completed"));
                capabilityRegistry.recordSuccessfulExecution(AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH);

                return String.format("MFA setup completed for user %s: %s", userId, mfaSetup.secretKey());
            })
            .onFailure(error -> {
                log.error("Failed to setup MFA for user: {}", userId);
                capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH, new RuntimeException(error));
            })
            .orElseThrow(error -> new RuntimeException("MFA setup failed: " + error))
        );
    }
    
    /**
     * Security audit capability with advanced proficiency
     */
    @AgentCapability(name = AuthConstants.CAPABILITY_SECURITY_AUDIT, proficiency = AuthConstants.PROFICIENCY_ADVANCED)
    public CompletableFuture<String> performSecurityAudit(String userId, String eventType, String details) {
        return CompletableFuture.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                log.info("Performing security audit for user: {} event: {}", userId, eventType);

                securityAuditService.logSecurityEvent(
                    Long.valueOf(userId), eventType, "LOW", "127.0.0.1", "AuthenticationAgent",
                    Map.of("details", details, "eventType", eventType)
                );
                var complianceCheck = securityAuditService.checkComplianceStatus(userId);
                capabilityRegistry.recordSuccessfulExecution(AuthConstants.CAPABILITY_SECURITY_AUDIT);

                return String.format("Security audit completed for user %s: compliance status %s",
                                   userId, complianceCheck.get("complianceLevel"));
            })
            .onFailure(error -> {
                log.error("Failed to perform security audit for user: {}", userId);
                capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_SECURITY_AUDIT, new RuntimeException(error));
            })
            .orElseThrow(error -> new RuntimeException("Security audit failed: " + error))
        );
    }
    
    /**
     * Session management capability with advanced proficiency
     */
    @AgentCapability(name = AuthConstants.CAPABILITY_SESSION_MANAGEMENT, proficiency = AuthConstants.PROFICIENCY_ADVANCED)
    public CompletableFuture<String> manageUserSession(String sessionId, String action) {
        return CompletableFuture.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                log.info("Managing session: {} action: {}", sessionId, action);

                var sessionResult = sessionManagementService.manageSession(sessionId, action);
                securityAuditService.logSecurityEvent(
                    Long.valueOf(sessionResult.getUserId()), "SESSION_" + action.toUpperCase(), "LOW",
                    "127.0.0.1", "AuthenticationAgent",
                    Map.of("action", action, "sessionId", sessionId)
                );
                capabilityRegistry.recordSuccessfulExecution(AuthConstants.CAPABILITY_SESSION_MANAGEMENT);

                return String.format("Session %s action %s completed successfully", sessionId, action);
            })
            .onFailure(error -> {
                log.error("Failed to manage session: {} action: {}", sessionId, action);
                capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_SESSION_MANAGEMENT, new RuntimeException(error));
            })
            .orElseThrow(error -> new RuntimeException("Session management failed: " + error))
        );
    }
    
    /**
     * Device trust capability with intermediate proficiency
     */
    @AgentCapability(name = AuthConstants.CAPABILITY_DEVICE_TRUST, proficiency = AuthConstants.PROFICIENCY_INTERMEDIATE)
    public CompletableFuture<String> validateDeviceTrust(String deviceId, String userId) {
        return CompletableFuture.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                log.info("Validating device trust for device: {} user: {}", deviceId, userId);

                var trustResult = deviceTrustService.validateDeviceTrust(deviceId, Long.valueOf(userId));
                securityAuditService.logSecurityEvent(
                    Long.valueOf(userId), "DEVICE_TRUST_CHECK", "LOW", "127.0.0.1", "AuthenticationAgent",
                    Map.of("device_id", deviceId, "trust_level", trustResult.getTrustLevel().toString())
                );
                capabilityRegistry.recordSuccessfulExecution(AuthConstants.CAPABILITY_DEVICE_TRUST);

                return String.format("Device trust validation completed: %s level for device %s",
                                   trustResult.getTrustLevel(), deviceId);
            })
            .onFailure(error -> {
                log.error("Failed to validate device trust for device: {}", deviceId);
                capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_DEVICE_TRUST, new RuntimeException(error));
            })
            .orElseThrow(error -> new RuntimeException("Device trust validation failed: " + error))
        );
    }
    
    /**
     * Executes coordinated authentication processing using Java 24 structured concurrency
     */
    private CompletableFuture<AuthenticationResponse> executeCoordinatedAuthentication(
            Long requestId,
            List<Supplier<String>> operations,
            Duration timeout) {

        return CompletableFuture.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                    // Fork all authentication operations
                    var subtasks = operations.stream()
                        .map(operation -> scope.fork(operation::get))
                        .toList();

                    // Join with timeout and handle failures
                    try {
                        scope.joinUntil(java.time.Instant.now().plus(timeout));
                        scope.throwIfFailed();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Authentication processing interrupted", e);
                    } catch (java.util.concurrent.TimeoutException e) {
                        throw new RuntimeException("Authentication processing timeout exceeded", e);
                    } catch (java.util.concurrent.ExecutionException e) {
                        throw new RuntimeException("Authentication processing execution failed", e);
                    }

                    // Collect results
                    var results = subtasks.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .toList();

                    log.info("Coordinated authentication processing completed for request: {}", requestId);

                    return AuthenticationResponse.builder()
                        .requestId(requestId)
                        .status("SUCCESS")
                        .processingResults(results)
                        .processingTimeMs(System.currentTimeMillis())
                        .build();
                }
            }).fold(
                error -> {
                    log.error("Coordinated authentication processing failed for request: {}", requestId);
                    return AuthenticationResponse.builder()
                        .requestId(requestId)
                        .status("FAILED")
                        .errorMessage(error)
                        .processingTimeMs(System.currentTimeMillis())
                        .build();
                },
                response -> response
            )
        );
    }
    
    // Helper methods for authentication operations
    
    private String validateCredentials(AuthenticationRequest request) {
        return SafeOperations.safelyToResult(() -> {
            var startTime = System.currentTimeMillis();

            // Validate email format
            Optional.ofNullable(request.getEmail())
                .filter(email -> email.matches("^[A-Za-z0-9+_.-]+@(.+)$"))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email format"));

            // Validate password strength
            Optional.ofNullable(request.getPassword())
                .filter(password -> password.length() >= 8)
                .orElseThrow(() -> new IllegalArgumentException("Password must be at least 8 characters"));

            // Use authentication service to validate credentials
            var authResult = authenticationService.login(request, null);

            var executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            capabilityRegistry.recordExecutionTime("USER_AUTHENTICATION", executionTime);

            return authResult.fold(
                error -> {
                    throw new RuntimeException("Credential validation failed: " + error);
                },
                response -> "Credentials validated successfully for user: " + response.getUser().getEmail()
            );
        }).fold(
            error -> {
                log.warn("Credential validation failed: {}", error);
                capabilityRegistry.recordFailedExecution("USER_AUTHENTICATION", new RuntimeException(error));
                return "Credential validation failed: " + error;
            },
            result -> result
        );
    }
    
    private String checkAccountStatus(AuthenticationRequest request) {
        return SafeOperations.safelyToResult(() -> {
            var startTime = System.currentTimeMillis();

            // For agent context, we'll perform validation by attempting authentication
            // This gives us both user existence and account status information
            var authResult = authenticationService.login(request, null);

            var executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            capabilityRegistry.recordExecutionTime("USER_AUTHENTICATION", executionTime);

            return authResult.fold(
                error -> {
                    // Analyze error to determine specific account status issues using functional pattern
                    String lowerError = error.toLowerCase();
                    return Stream.of(
                            Map.entry(List.of("not found", "does not exist"),
                                    "Account not found: " + request.getEmail()),
                            Map.entry(List.of("disabled", "inactive"),
                                    "Account is disabled: " + request.getEmail()),
                            Map.entry(List.of("locked"),
                                    "Account is locked: " + request.getEmail()),
                            Map.entry(List.of("expired"),
                                    "Account is expired: " + request.getEmail())
                        )
                        .filter(entry -> entry.getKey().stream()
                            .anyMatch(lowerError::contains))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(String.format("Account exists and active but authentication failed for: %s", request.getEmail()));
                },
                response -> String.format("Account status verified - User active: %s (ID: %d)",
                                        response.getUser().getEmail(), response.getUser().getId())
            );
        }).fold(
            error -> {
                log.warn("Account status check failed: {}", error);
                capabilityRegistry.recordFailedExecution("USER_AUTHENTICATION", new RuntimeException(error));
                return "Account status check failed: " + error;
            },
            result -> result
        );
    }
    
    private String validateDeviceTrust(AuthenticationRequest request) {
        return SafeOperations.safelyToResult(() -> {
            var startTime = System.currentTimeMillis();

            // First authenticate to get actual user information
            var authResult = authenticationService.login(request, null);

            return authResult.fold(
                error -> {
                    log.warn("Cannot validate device trust - authentication failed: {}", error);
                    capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_DEVICE_TRUST, new RuntimeException(error));
                    return "Device trust validation failed - authentication required: " + error;
                },
                response -> SafeOperations.safelyToResult(() -> {
                    // Use functional approach to generate device ID
                    final String deviceId = generateDeviceId(request.getDeviceInfo(), response.getUser().getId());

                    // Validate device trust with actual user ID
                    var trustValidation = deviceTrustService.validateDeviceTrust(deviceId, response.getUser().getId());

                    var executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
                    capabilityRegistry.recordExecutionTime("DEVICE_TRUST", executionTime);
                    capabilityRegistry.recordSuccessfulExecution(AuthConstants.CAPABILITY_DEVICE_TRUST);

                    return String.format("Device trust validated - Level: %s, Device: %s, Trusted: %s, User: %s (ID: %d)",
                                       trustValidation.getTrustLevel(), deviceId, trustValidation.isTrusted(),
                                       response.getUser().getEmail(), response.getUser().getId());
                }).fold(
                    deviceError -> {
                        log.error("Device trust validation failed for user {}: {}", response.getUser().getId(), deviceError);
                        capabilityRegistry.recordFailedExecution(AuthConstants.CAPABILITY_DEVICE_TRUST, new RuntimeException(deviceError));
                        return "Device trust validation failed: " + deviceError;
                    },
                    result -> result
                )
            );
        }).fold(
            error -> {
                log.error("Device trust validation process failed: {}", error);
                capabilityRegistry.recordFailedExecution("DEVICE_TRUST", new RuntimeException(error));
                return "Device trust validation process failed: " + error;
            },
            result -> result
        );
    }
    
    private String createUserSession(AuthenticationRequest request) {
        return SafeOperations.safelyToResult(() -> {
            var startTime = System.currentTimeMillis();

            // First authenticate to get user ID
            var authResult = authenticationService.login(request, null);

            return authResult.fold(
                error -> {
                    throw new RuntimeException("Cannot create session - authentication failed: " + error);
                },
                response -> SafeOperations.safelyToResult(() -> {
                    // Use functional approach to generate device info
                    final String deviceInfo = generateDeviceId(request.getDeviceInfo(), response.getUser().getId());

                    // Create session with validated user and proper context
                    String sessionId = sessionManagementService.createSession(
                        response.getUser().getId(),
                        deviceInfo,
                        "AgentOS-" + response.getUser().getId(),
                        "TradeMaster-AuthAgent/1.0 (User:" + response.getUser().getId() + ")",
                        Map.of(
                            "loginTime", System.currentTimeMillis(),
                            "authMethod", "password",
                            "agentRequested", true
                        )
                    );

                    var executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
                    capabilityRegistry.recordExecutionTime("SESSION_MANAGEMENT", executionTime);

                    return String.format("User session created successfully: %s for user %s",
                                       sessionId, response.getUser().getEmail());
                }).orElseThrow(sessionError -> {
                    capabilityRegistry.recordFailedExecution("SESSION_MANAGEMENT", new RuntimeException(sessionError));
                    return new RuntimeException("Session creation failed: " + sessionError);
                })
            );
        }).fold(
            error -> {
                log.warn("Session creation failed: {}", error);
                capabilityRegistry.recordFailedExecution("SESSION_MANAGEMENT", new RuntimeException(error));
                return "Session creation failed: " + error;
            },
            result -> result
        );
    }
    
    private String auditAuthenticationAttempt(AuthenticationRequest request) {
        return SafeOperations.safelyToResult(() -> {
            final var startTime = System.currentTimeMillis();

            // Use functional approach to determine audit result
            final var auditResult = determineAuditResult(request);
            final var userId = auditResult.userId();
            final var auditStatus = auditResult.status();

            // Log security event
            securityAuditService.logSecurityEvent(
                userId != null ? userId : -1L, // Use -1 for unknown users
                "AUTHENTICATION_" + auditStatus,
                "MEDIUM", // Authentication attempts are medium severity
                "AgentOS-" + (userId != null ? userId : "unknown"),
                "AuthenticationAgent",
                Map.of(
                    "email", request.getEmail(),
                    "deviceInfo", request.getDeviceInfo() != null ? request.getDeviceInfo() : "unknown",
                    "userAgent", "TradeMaster-AuthAgent/1.0 (" + request.getEmail() + ")",
                    "timestamp", System.currentTimeMillis(),
                    "requestId", request.getRequestId() != null ? request.getRequestId().toString() : "unknown"
                )
            );

            var executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            capabilityRegistry.recordExecutionTime("SECURITY_AUDIT", executionTime);
            capabilityRegistry.recordSuccessfulExecution("SECURITY_AUDIT");

            return String.format("Authentication attempt audited: status=%s, email=%s, ip=%s, userId=%s",
                               auditStatus, request.getEmail(),
                               "AgentOS-" + (userId != null ? userId : "unknown"),
                               userId != null ? userId : "unknown");
        }).fold(
            error -> {
                log.warn("Audit logging failed: {}", error);
                capabilityRegistry.recordFailedExecution("SECURITY_AUDIT", new RuntimeException(error));
                return "Audit logging failed: " + error;
            },
            result -> result
        );
    }
    
    @Override
    public String getAgentId() {
        return AuthConstants.AGENT_ID_AUTHENTICATION;
    }
    
    @Override
    public String getAgentType() {
        return AuthConstants.AGENT_TYPE_AUTHENTICATION;
    }
    
    @Override
    public List<String> getCapabilities() {
        return List.of(
            AuthConstants.CAPABILITY_USER_AUTHENTICATION,
            AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH,
            AuthConstants.CAPABILITY_SECURITY_AUDIT,
            AuthConstants.CAPABILITY_SESSION_MANAGEMENT,
            AuthConstants.CAPABILITY_DEVICE_TRUST
        );
    }
    
    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }
    
    @Override
    public void onRegistration() {
        log.info("Authentication Agent registered with Agent Orchestration Service");
        capabilityRegistry.initializeCapabilities();
        
        // Log agent startup metrics
        securityAuditService.logSecurityEvent(
            -1L, // System event
            "AGENT_REGISTRATION",
            "LOW",
            "127.0.0.1",
            "AuthenticationAgent",
            Map.of(
                "agentId", getAgentId(),
                "agentType", getAgentType(),
                "capabilities", String.join(",", getCapabilities()),
                "healthScore", getHealthScore().toString(),
                "timestamp", System.currentTimeMillis()
            )
        );
    }
    
    @Override
    public void onDeregistration() {
        log.info("Authentication Agent deregistered from Agent Orchestration Service");
        
        // Log final performance summary
        Map<String, String> performanceSummary = capabilityRegistry.getPerformanceSummary();
        log.info("Authentication Agent final performance summary: {}", performanceSummary);
        
        // Log agent shutdown metrics
        securityAuditService.logSecurityEvent(
            -1L, // System event
            "AGENT_DEREGISTRATION",
            "LOW",
            "127.0.0.1",
            "AuthenticationAgent",
            Map.of(
                "agentId", getAgentId(),
                "finalHealthScore", getHealthScore().toString(),
                "performanceSummary", performanceSummary.toString(),
                "timestamp", System.currentTimeMillis()
            )
        );
    }
    
    @Override
    public void performHealthCheck() {
        SafeOperations.safelyToResult(() -> {
            Double healthScore = capabilityRegistry.calculateOverallHealthScore();
            log.debug("Authentication Agent health check - Score: {}", healthScore);

            // Log health check if score is concerning using functional pattern
            Optional.of(healthScore)
                .filter(score -> score < 0.7)
                .ifPresent(score -> {
                    log.warn("Authentication Agent health score below threshold: {}", score);

                    securityAuditService.logSecurityEvent(
                        -1L, // System event
                        "AGENT_HEALTH_WARNING",
                        "MEDIUM",
                        "127.0.0.1",
                        "AuthenticationAgent",
                        Map.of(
                            "agentId", getAgentId(),
                            "healthScore", score.toString(),
                            "threshold", "0.7",
                            "timestamp", System.currentTimeMillis()
                        )
                    );
                });
            return null;
        }).onFailure(error -> {
            log.error("Authentication Agent health check failed", error);

            securityAuditService.logSecurityEvent(
                -1L, // System event
                "AGENT_HEALTH_CHECK_FAILED",
                "HIGH",
                "127.0.0.1",
                "AuthenticationAgent",
                Map.of(
                    "agentId", getAgentId(),
                    "error", error,
                    "timestamp", System.currentTimeMillis()
                )
            );
        });
    }
    
    /**
     * Immutable record for audit result following functional programming principles
     */
    private record AuditResult(Long userId, String status) {}
    
    /**
     * Functional method to generate device ID following functional programming principles
     */
    private String generateDeviceId(String deviceInfo, Long userId) {
        return Optional.ofNullable(deviceInfo)
            .filter(info -> !"unknown".equals(info))
            .orElse(String.format("agent-%s-%s", 
                AuthConstants.AGENT_ID_AUTHENTICATION, 
                userId));
    }
    
    /**
     * Functional method to determine error status using pattern matching approach
     */
    private AuditResult determineErrorStatus(String error) {
        final String lowerError = error.toLowerCase();
        
        // Functional pattern matching using Stream API
        return List.of(
                Map.entry(List.of("not found", "does not exist"), "FAILED_USER_NOT_FOUND"),
                Map.entry(List.of("disabled", "locked", "expired"), "FAILED_ACCOUNT_STATUS")
            )
            .stream()
            .filter(entry -> entry.getKey().stream().anyMatch(lowerError::contains))
            .map(entry -> new AuditResult(null, entry.getValue()))
            .findFirst()
            .orElse(new AuditResult(null, "FAILED_AUTHENTICATION"));
    }
    
    /**
     * Functional method to determine audit result without mutable variables
     */
    private AuditResult determineAuditResult(AuthenticationRequest request) {
        return SafeOperations.safelyToResult(() -> {
            var authResult = authenticationService.login(request, null);

            return authResult.fold(
                error -> determineErrorStatus(error),
                response -> new AuditResult(response.getUser().getId(), "SUCCESS")
            );
        }).fold(
            error -> {
                log.debug("Could not resolve user ID for audit: {}", error);
                return new AuditResult(null, "FAILED_USER_LOOKUP");
            },
            result -> result
        );
    }
}

