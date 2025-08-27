package com.trademaster.brokerauth.service;

import com.trademaster.brokerauth.config.CorrelationConfig;
import com.trademaster.brokerauth.entity.BrokerAccount;
import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
import com.trademaster.brokerauth.repository.BrokerAccountRepository;
import com.trademaster.brokerauth.service.broker.BrokerAuthService;
import com.trademaster.brokerauth.service.CredentialManagementService.BrokerCredentials;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broker Authentication Orchestration Service
 * 
 * Main orchestration service for broker authentication flows.
 * Coordinates credential management, session handling, rate limiting, and error tracking.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerAuthenticationService {
    
    private final CredentialManagementService credentialService;
    private final BrokerSessionService sessionService;
    private final BrokerRateLimitService rateLimitService;
    private final BrokerAccountRepository accountRepository;
    private final Map<BrokerType, BrokerAuthService> brokerServices;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter authenticationAttemptCounter;
    private final Counter authenticationSuccessCounter;
    private final Counter authenticationFailureCounter;
    private final Timer authenticationTimer;
    
    public BrokerAuthenticationService(CredentialManagementService credentialService,
                                     BrokerSessionService sessionService,
                                     BrokerRateLimitService rateLimitService,
                                     BrokerAccountRepository accountRepository,
                                     List<BrokerAuthService> brokerAuthServices,
                                     MeterRegistry meterRegistry) {
        this.credentialService = credentialService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
        this.accountRepository = accountRepository;
        this.meterRegistry = meterRegistry;
        
        // Initialize broker services map
        this.brokerServices = new ConcurrentHashMap<>();
        brokerAuthServices.forEach(service -> 
            this.brokerServices.put(service.getBrokerType(), service)
        );
        
        // Initialize metrics
        this.authenticationAttemptCounter = Counter.builder("broker.auth.attempts")
                .description("Number of broker authentication attempts")
                .register(meterRegistry);
                
        this.authenticationSuccessCounter = Counter.builder("broker.auth.success")
                .description("Number of successful broker authentications")
                .register(meterRegistry);
                
        this.authenticationFailureCounter = Counter.builder("broker.auth.failures")
                .description("Number of failed broker authentications")
                .register(meterRegistry);
                
        this.authenticationTimer = Timer.builder("broker.auth.duration")
                .description("Time taken for broker authentication")
                .register(meterRegistry);
    }
    
    /**
     * Initiate authentication flow for a broker
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<AuthFlowResult> initiateAuthFlow(Long userId, BrokerType brokerType, String clientIp, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            authenticationAttemptCounter.increment();
            
            try {
                log.info("Initiating auth flow for user {} and broker {} (correlationId: {})", 
                        userId, brokerType, CorrelationConfig.CorrelationContext.getCorrelationId());
                
                // Check rate limits
                BrokerRateLimitService.RateLimitResult rateLimitResult = rateLimitService
                        .checkRateLimit(brokerType, userId, "auth")
                        .join();
                        
                if (!rateLimitResult.isAllowed()) {
                    return AuthFlowResult.rateLimitExceeded(rateLimitResult.getReason());
                }
                
                // Get credentials
                Optional<BrokerCredentials> credentialsOpt = credentialService
                        .getCredentials(userId, brokerType);
                        
                if (credentialsOpt.isEmpty()) {
                    return AuthFlowResult.missingCredentials("Broker credentials not configured");
                }
                
                BrokerCredentials credentials = credentialsOpt.get();
                BrokerAuthService authService = brokerServices.get(brokerType);
                
                if (authService == null) {
                    return AuthFlowResult.serviceUnavailable("Authentication service not available");
                }
                
                // Generate authorization URL for OAuth flows
                String state = java.util.UUID.randomUUID().toString();
                
                try {
                    String authUrl = authService.getAuthorizationUrl(credentials, state);
                    
                    return AuthFlowResult.authUrlGenerated(authUrl, state);
                    
                } catch (UnsupportedOperationException e) {
                    // Broker doesn't use OAuth flow (e.g., Angel One, ICICI)
                    return AuthFlowResult.directAuth("Direct API authentication required");
                }
                
            } catch (Exception e) {
                log.error("Failed to initiate auth flow for user {} and broker {}", userId, brokerType, e);
                authenticationFailureCounter.increment();
                return AuthFlowResult.error("Failed to initiate authentication: " + e.getMessage());
            } finally {
                sample.stop(authenticationTimer);
            }
        });
    }
    
    /**
     * Complete authentication flow with authorization code
     */
    @Transactional
    @Async("brokerAuthExecutor")
    public CompletableFuture<AuthFlowResult> completeAuthFlow(Long userId, BrokerType brokerType, 
                                                             String authorizationCode, String state,
                                                             String clientIp, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                log.info("Completing auth flow for user {} and broker {} (correlationId: {})", 
                        userId, brokerType, CorrelationConfig.CorrelationContext.getCorrelationId());
                
                // Check rate limits
                BrokerRateLimitService.RateLimitResult rateLimitResult = rateLimitService
                        .checkRateLimit(brokerType, userId, "auth")
                        .join();
                        
                if (!rateLimitResult.isAllowed()) {
                    authenticationFailureCounter.increment();
                    return AuthFlowResult.rateLimitExceeded(rateLimitResult.getReason());
                }
                
                // Create session
                BrokerSessionService.SessionResult sessionResult = sessionService
                        .createSession(userId, brokerType, authorizationCode, clientIp, userAgent)
                        .join();
                        
                if (!sessionResult.isSuccess()) {
                    authenticationFailureCounter.increment();
                    return AuthFlowResult.error(sessionResult.getErrorMessage());
                }
                
                // Record successful authentication
                rateLimitService.recordApiCall(brokerType, userId, "auth");
                authenticationSuccessCounter.increment();
                
                log.info("Authentication completed successfully for user {} and broker {}", userId, brokerType);
                
                return AuthFlowResult.success(sessionResult.getSession());
                
            } catch (Exception e) {
                log.error("Failed to complete auth flow for user {} and broker {}", userId, brokerType, e);
                authenticationFailureCounter.increment();
                return AuthFlowResult.error("Authentication failed: " + e.getMessage());
            } finally {
                sample.stop(authenticationTimer);
            }
        });
    }
    
    /**
     * Store broker credentials
     */
    @Transactional
    @Async("brokerAuthExecutor")
    public CompletableFuture<AuthFlowResult> storeCredentials(Long userId, BrokerType brokerType, 
                                                             BrokerCredentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Storing credentials for user {} and broker {}", userId, brokerType);
                
                if (!credentials.isValid()) {
                    return AuthFlowResult.invalidCredentials("Invalid credentials provided");
                }
                
                credentialService.storeCredentials(userId, brokerType, credentials);
                
                log.info("Credentials stored successfully for user {} and broker {}", userId, brokerType);
                return AuthFlowResult.credentialsStored("Credentials stored successfully");
                
            } catch (Exception e) {
                log.error("Failed to store credentials for user {} and broker {}", userId, brokerType, e);
                return AuthFlowResult.error("Failed to store credentials: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get user's active sessions
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<List<BrokerSession>> getActiveSessions(Long userId, BrokerType brokerType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sessionService.getActiveSessions(userId, brokerType);
            } catch (Exception e) {
                log.error("Failed to get active sessions for user {} and broker {}", userId, brokerType, e);
                return List.of();
            }
        });
    }
    
    /**
     * Revoke session
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<Boolean> revokeSession(String sessionId, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sessionService.revokeSession(sessionId, reason).join();
            } catch (Exception e) {
                log.error("Failed to revoke session {}", sessionId, e);
                return false;
            }
        });
    }
    
    /**
     * Refresh session if needed
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<AuthFlowResult> refreshSessionIfNeeded(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<BrokerSession> sessionOpt = sessionService.getSession(sessionId);
                
                if (sessionOpt.isEmpty()) {
                    return AuthFlowResult.error("Session not found");
                }
                
                BrokerSession session = sessionOpt.get();
                
                if (!session.needsRefresh()) {
                    return AuthFlowResult.noActionNeeded("Session does not need refresh");
                }
                
                if (!session.canBeRefreshed()) {
                    return AuthFlowResult.error("Session cannot be refreshed - re-authentication required");
                }
                
                BrokerSessionService.SessionResult refreshResult = sessionService
                        .refreshSession(sessionId)
                        .join();
                        
                if (!refreshResult.isSuccess()) {
                    return AuthFlowResult.error(refreshResult.getErrorMessage());
                }
                
                return AuthFlowResult.sessionRefreshed(refreshResult.getSession());
                
            } catch (Exception e) {
                log.error("Failed to refresh session {}", sessionId, e);
                return AuthFlowResult.error("Session refresh failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get broker account status for user
     */
    @Async("brokerAuthExecutor")
    public CompletableFuture<BrokerAccountStatus> getBrokerAccountStatus(Long userId, BrokerType brokerType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<BrokerAccount> accountOpt = accountRepository
                        .findByUserIdAndBrokerType(userId, brokerType);
                        
                if (accountOpt.isEmpty()) {
                    return BrokerAccountStatus.notConfigured();
                }
                
                BrokerAccount account = accountOpt.get();
                List<BrokerSession> activeSessions = sessionService.getActiveSessions(userId, brokerType);
                
                return BrokerAccountStatus.builder()
                        .configured(true)
                        .verified(account.getIsVerified())
                        .active(account.getIsActive())
                        .healthy(account.isHealthy())
                        .activeSessions(activeSessions.size())
                        .maxSessions(account.getBroker() != null ? account.getBroker().getMaxSessionsPerUser() : 1)
                        .successRate(account.getSuccessRate())
                        .lastConnection(account.getLastConnectionAt())
                        .build();
                        
            } catch (Exception e) {
                log.error("Failed to get broker account status for user {} and broker {}", userId, brokerType, e);
                return BrokerAccountStatus.error("Failed to get account status");
            }
        });
    }
    
    /**
     * Authentication flow result
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuthFlowResult {
        private boolean success;
        private AuthFlowStatus status;
        private String message;
        private String authorizationUrl;
        private String state;
        private BrokerSession session;
        
        public static AuthFlowResult authUrlGenerated(String authUrl, String state) {
            return AuthFlowResult.builder()
                    .success(true)
                    .status(AuthFlowStatus.AUTH_URL_GENERATED)
                    .authorizationUrl(authUrl)
                    .state(state)
                    .message("Authorization URL generated")
                    .build();
        }
        
        public static AuthFlowResult directAuth(String message) {
            return AuthFlowResult.builder()
                    .success(true)
                    .status(AuthFlowStatus.DIRECT_AUTH_REQUIRED)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult success(BrokerSession session) {
            return AuthFlowResult.builder()
                    .success(true)
                    .status(AuthFlowStatus.AUTHENTICATED)
                    .session(session)
                    .message("Authentication successful")
                    .build();
        }
        
        public static AuthFlowResult credentialsStored(String message) {
            return AuthFlowResult.builder()
                    .success(true)
                    .status(AuthFlowStatus.CREDENTIALS_STORED)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult sessionRefreshed(BrokerSession session) {
            return AuthFlowResult.builder()
                    .success(true)
                    .status(AuthFlowStatus.SESSION_REFRESHED)
                    .session(session)
                    .message("Session refreshed successfully")
                    .build();
        }
        
        public static AuthFlowResult noActionNeeded(String message) {
            return AuthFlowResult.builder()
                    .success(true)
                    .status(AuthFlowStatus.NO_ACTION_NEEDED)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult rateLimitExceeded(String message) {
            return AuthFlowResult.builder()
                    .success(false)
                    .status(AuthFlowStatus.RATE_LIMIT_EXCEEDED)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult missingCredentials(String message) {
            return AuthFlowResult.builder()
                    .success(false)
                    .status(AuthFlowStatus.MISSING_CREDENTIALS)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult invalidCredentials(String message) {
            return AuthFlowResult.builder()
                    .success(false)
                    .status(AuthFlowStatus.INVALID_CREDENTIALS)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult serviceUnavailable(String message) {
            return AuthFlowResult.builder()
                    .success(false)
                    .status(AuthFlowStatus.SERVICE_UNAVAILABLE)
                    .message(message)
                    .build();
        }
        
        public static AuthFlowResult error(String message) {
            return AuthFlowResult.builder()
                    .success(false)
                    .status(AuthFlowStatus.ERROR)
                    .message(message)
                    .build();
        }
    }
    
    public enum AuthFlowStatus {
        AUTH_URL_GENERATED,
        DIRECT_AUTH_REQUIRED,
        AUTHENTICATED,
        CREDENTIALS_STORED,
        SESSION_REFRESHED,
        NO_ACTION_NEEDED,
        RATE_LIMIT_EXCEEDED,
        MISSING_CREDENTIALS,
        INVALID_CREDENTIALS,
        SERVICE_UNAVAILABLE,
        ERROR
    }
    
    /**
     * Broker account status
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BrokerAccountStatus {
        private boolean configured;
        private boolean verified;
        private boolean active;
        private boolean healthy;
        private int activeSessions;
        private int maxSessions;
        private double successRate;
        private LocalDateTime lastConnection;
        private String errorMessage;
        
        public static BrokerAccountStatus notConfigured() {
            return BrokerAccountStatus.builder()
                    .configured(false)
                    .build();
        }
        
        public static BrokerAccountStatus error(String errorMessage) {
            return BrokerAccountStatus.builder()
                    .errorMessage(errorMessage)
                    .build();
        }
        
        public boolean canCreateNewSession() {
            return configured && verified && active && activeSessions < maxSessions;
        }
        
        public boolean requiresAttention() {
            return !healthy || !verified || successRate < 80.0;
        }
    }
}