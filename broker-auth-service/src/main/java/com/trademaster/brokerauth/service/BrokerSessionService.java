package com.trademaster.brokerauth.service;

import com.trademaster.brokerauth.entity.BrokerAccount;
import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
import com.trademaster.brokerauth.repository.BrokerAccountRepository;
import com.trademaster.brokerauth.repository.BrokerSessionRepository;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broker Session Management Service
 * 
 * Manages broker authentication sessions with comprehensive lifecycle management.
 * Handles session creation, validation, refresh, and cleanup operations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerSessionService {
    
    private final BrokerSessionRepository sessionRepository;
    private final BrokerAccountRepository accountRepository;
    private final CredentialManagementService credentialService;
    private final CredentialEncryptionService encryptionService;
    private final Map<BrokerType, BrokerAuthService> brokerServices;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter sessionCreationCounter;
    private final Counter sessionValidationCounter;
    private final Counter sessionRefreshCounter;
    private final Timer sessionCreationTimer;
    private final Timer sessionValidationTimer;
    
    public BrokerSessionService(BrokerSessionRepository sessionRepository,
                               BrokerAccountRepository accountRepository,
                               CredentialManagementService credentialService,
                               CredentialEncryptionService encryptionService,
                               List<BrokerAuthService> brokerAuthServices,
                               MeterRegistry meterRegistry) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.credentialService = credentialService;
        this.encryptionService = encryptionService;
        this.meterRegistry = meterRegistry;
        
        // Initialize broker services map
        this.brokerServices = new ConcurrentHashMap<>();
        brokerAuthServices.forEach(service -> 
            this.brokerServices.put(service.getBrokerType(), service)
        );
        
        // Initialize metrics
        this.sessionCreationCounter = Counter.builder("broker.session.creation")
                .description("Number of broker sessions created")
                .register(meterRegistry);
                
        this.sessionValidationCounter = Counter.builder("broker.session.validation")
                .description("Number of broker session validations")
                .register(meterRegistry);
                
        this.sessionRefreshCounter = Counter.builder("broker.session.refresh")
                .description("Number of broker session refreshes")
                .register(meterRegistry);
                
        this.sessionCreationTimer = Timer.builder("broker.session.creation.duration")
                .description("Time taken to create broker sessions")
                .register(meterRegistry);
                
        this.sessionValidationTimer = Timer.builder("broker.session.validation.duration")
                .description("Time taken to validate broker sessions")
                .register(meterRegistry);
    }
    
    /**
     * Create new broker session through authentication
     */
    @Transactional
    @Async("brokerAuthExecutor")
    public CompletableFuture<SessionResult> createSession(Long userId, BrokerType brokerType, 
                                                         String authorizationCode, String clientIp, 
                                                         String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                log.info("Creating broker session for user {} and broker {}", userId, brokerType);
                
                // Get broker account and credentials
                Optional<BrokerAccount> accountOpt = accountRepository
                        .findByUserIdAndBrokerType(userId, brokerType);
                
                if (accountOpt.isEmpty()) {
                    return SessionResult.failure("Broker account not found for user");
                }
                
                BrokerAccount account = accountOpt.get();
                if (!account.canAuthenticate()) {
                    return SessionResult.failure("Broker account cannot authenticate");
                }
                
                // Check session limits
                if (account.hasReachedMaxSessions()) {
                    return SessionResult.failure("Maximum sessions limit reached for this broker");
                }
                
                // Get credentials
                Optional<BrokerCredentials> credentialsOpt = credentialService
                        .getCredentials(userId, brokerType);
                
                if (credentialsOpt.isEmpty()) {
                    return SessionResult.failure("Broker credentials not found");
                }
                
                BrokerCredentials credentials = credentialsOpt.get();
                BrokerAuthService authService = brokerServices.get(brokerType);
                
                if (authService == null) {
                    return SessionResult.failure("Authentication service not available for broker");
                }
                
                // Create pending session
                BrokerSession session = createPendingSession(account, clientIp, userAgent);
                sessionRepository.save(session);
                
                // Perform authentication
                BrokerAuthService.AuthResult authResult = authService
                        .exchangeCodeForTokens(credentials, authorizationCode, null)
                        .join();
                
                if (!authResult.isSuccess()) {
                    session.markError(authResult.getErrorMessage());
                    sessionRepository.save(session);
                    return SessionResult.failure(authResult.getErrorMessage());
                }
                
                // Activate session with tokens
                String encryptedAccessToken = encryptionService.encrypt(authResult.getAccessToken());
                String encryptedRefreshToken = authResult.getRefreshToken() != null ? 
                        encryptionService.encrypt(authResult.getRefreshToken()) : null;
                
                session.activate(encryptedAccessToken, encryptedRefreshToken, authResult.getExpiresAt());
                sessionRepository.save(session);
                
                sessionCreationCounter.increment();
                log.info("Broker session created successfully: {}", session.getSessionId());
                
                return SessionResult.success(session);
                
            } catch (Exception e) {
                log.error("Failed to create broker session for user {} and broker {}", 
                         userId, brokerType, e);
                return SessionResult.failure("Session creation failed: " + e.getMessage());
            } finally {
                sample.stop(sessionCreationTimer);
            }
        });
    }
    
    /**
     * Validate existing session
     */
    @Transactional(readOnly = true)
    @Async("brokerAuthExecutor")
    public CompletableFuture<Boolean> validateSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                sessionValidationCounter.increment();
                
                Optional<BrokerSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
                if (sessionOpt.isEmpty()) {
                    return false;
                }
                
                BrokerSession session = sessionOpt.get();
                if (!session.isActive()) {
                    return false;
                }
                
                // Get credentials for validation
                Optional<BrokerCredentials> credentialsOpt = credentialService
                        .getCredentials(session.getUserId(), session.getBrokerType());
                
                if (credentialsOpt.isEmpty()) {
                    return false;
                }
                
                BrokerAuthService authService = brokerServices.get(session.getBrokerType());
                if (authService == null) {
                    return false;
                }
                
                // Validate with broker
                boolean isValid = authService.validateSession(credentialsOpt.get(), session).join();
                
                if (!isValid) {
                    session.expire();
                    sessionRepository.save(session);
                }
                
                return isValid;
                
            } catch (Exception e) {
                log.error("Failed to validate session {}", sessionId, e);
                return false;
            } finally {
                sample.stop(sessionValidationTimer);
            }
        });
    }
    
    /**
     * Refresh session tokens
     */
    @Transactional
    @Async("brokerAuthExecutor")
    public CompletableFuture<SessionResult> refreshSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Refreshing session: {}", sessionId);
                
                Optional<BrokerSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
                if (sessionOpt.isEmpty()) {
                    return SessionResult.failure("Session not found");
                }
                
                BrokerSession session = sessionOpt.get();
                if (!session.canBeRefreshed()) {
                    return SessionResult.failure("Session cannot be refreshed");
                }
                
                // Get credentials
                Optional<BrokerCredentials> credentialsOpt = credentialService
                        .getCredentials(session.getUserId(), session.getBrokerType());
                
                if (credentialsOpt.isEmpty()) {
                    return SessionResult.failure("Credentials not found");
                }
                
                BrokerAuthService authService = brokerServices.get(session.getBrokerType());
                if (authService == null) {
                    return SessionResult.failure("Authentication service not available");
                }
                
                // Start refresh process
                session.startRefresh();
                sessionRepository.save(session);
                
                // Decrypt refresh token
                String refreshToken = encryptionService.decrypt(session.getEncryptedRefreshToken());
                
                // Refresh tokens
                BrokerAuthService.AuthResult authResult = authService
                        .refreshToken(credentialsOpt.get(), refreshToken)
                        .join();
                
                if (!authResult.isSuccess()) {
                    session.markError(authResult.getErrorMessage());
                    sessionRepository.save(session);
                    return SessionResult.failure(authResult.getErrorMessage());
                }
                
                // Update session with new tokens
                String encryptedAccessToken = encryptionService.encrypt(authResult.getAccessToken());
                String encryptedRefreshToken = authResult.getRefreshToken() != null ? 
                        encryptionService.encrypt(authResult.getRefreshToken()) : null;
                
                session.completeRefresh(encryptedAccessToken, encryptedRefreshToken, 
                                      authResult.getExpiresAt());
                sessionRepository.save(session);
                
                sessionRefreshCounter.increment();
                log.info("Session refreshed successfully: {}", sessionId);
                
                return SessionResult.success(session);
                
            } catch (Exception e) {
                log.error("Failed to refresh session {}", sessionId, e);
                return SessionResult.failure("Session refresh failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Revoke session
     */
    @Transactional
    @Async("brokerAuthExecutor")
    public CompletableFuture<Boolean> revokeSession(String sessionId, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Revoking session: {} for reason: {}", sessionId, reason);
                
                Optional<BrokerSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
                if (sessionOpt.isEmpty()) {
                    return false;
                }
                
                BrokerSession session = sessionOpt.get();
                
                // Get credentials for revocation
                Optional<BrokerCredentials> credentialsOpt = credentialService
                        .getCredentials(session.getUserId(), session.getBrokerType());
                
                if (credentialsOpt.isPresent()) {
                    BrokerAuthService authService = brokerServices.get(session.getBrokerType());
                    if (authService != null && session.isActive()) {
                        // Attempt to revoke with broker
                        authService.revokeSession(credentialsOpt.get(), session).join();
                    }
                }
                
                // Revoke session locally
                session.revoke(reason);
                sessionRepository.save(session);
                
                log.info("Session revoked successfully: {}", sessionId);
                return true;
                
            } catch (Exception e) {
                log.error("Failed to revoke session {}", sessionId, e);
                return false;
            }
        });
    }
    
    /**
     * Get active sessions for user and broker
     */
    @Transactional(readOnly = true)
    public List<BrokerSession> getActiveSessions(Long userId, BrokerType brokerType) {
        return sessionRepository.findActiveSessionsByUserIdAndBrokerType(userId, brokerType);
    }
    
    /**
     * Get session by ID
     */
    @Transactional(readOnly = true)
    public Optional<BrokerSession> getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }
    
    /**
     * Cleanup expired sessions
     */
    @Transactional
    @Async("brokerAuthExecutor")
    public CompletableFuture<Integer> cleanupExpiredSessions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting expired sessions cleanup");
                
                List<BrokerSession> expiredSessions = sessionRepository.findExpiredSessions();
                int count = 0;
                
                for (BrokerSession session : expiredSessions) {
                    if (session.getStatus() == SessionStatus.ACTIVE && session.isExpired()) {
                        session.expire();
                        sessionRepository.save(session);
                        count++;
                    }
                }
                
                log.info("Cleaned up {} expired sessions", count);
                return count;
                
            } catch (Exception e) {
                log.error("Failed to cleanup expired sessions", e);
                return 0;
            }
        });
    }
    
    /**
     * Create pending session
     */
    private BrokerSession createPendingSession(BrokerAccount account, String clientIp, String userAgent) {
        return BrokerSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .brokerAccount(account)
                .userId(account.getUserId())
                .brokerType(account.getBrokerType())
                .status(SessionStatus.PENDING)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .loginMethod("oauth")
                .build();
    }
    
    /**
     * Session operation result
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionResult {
        private boolean success;
        private BrokerSession session;
        private String errorMessage;
        private String errorCode;
        
        public static SessionResult success(BrokerSession session) {
            return SessionResult.builder()
                    .success(true)
                    .session(session)
                    .build();
        }
        
        public static SessionResult failure(String errorMessage) {
            return SessionResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
        
        public static SessionResult failure(String errorMessage, String errorCode) {
            return SessionResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .errorCode(errorCode)
                    .build();
        }
    }
}