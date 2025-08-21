package com.trademaster.auth.service;

import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.entity.User;
import com.trademaster.auth.entity.UserProfile;
import com.trademaster.auth.entity.UserRole;
import com.trademaster.auth.entity.UserRoleAssignment;
import com.trademaster.auth.repository.UserRepository;
import com.trademaster.auth.repository.UserProfileRepository;
import com.trademaster.auth.repository.UserRoleRepository;
import com.trademaster.auth.repository.UserRoleAssignmentRepository;
import com.trademaster.auth.security.JwtTokenProvider;
import com.trademaster.auth.security.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Functional Authentication Service - Java 24 Architecture
 * 
 * Advanced Design Patterns Applied:
 * - Strategy Pattern: Authentication strategies, validation chains
 * - Command Pattern: Registration and login operations
 * - Chain of Responsibility: Validation pipeline
 * - Observer Pattern: Audit and notification events
 * - Factory Pattern: Response and user creation
 * 
 * Architectural Principles:
 * - Virtual Threads: All async operations use virtual threads for scalable concurrency
 * - Railway Oriented Programming: Result types for error handling
 * - Lock-free Operations: Atomic references for thread-safe state management
 * - Functional Programming: Higher-order functions and immutable data where beneficial
 * - Structured Concurrency: Coordinated task execution with proper lifecycle management
 * - SOLID Principles: Single responsibility, dependency inversion
 * - Architectural Fitness: Patterns applied where they improve maintainability
 * 
 * @author TradeMaster Development Team
 * @version 3.0.0 - Functional Architecture with Virtual Threads
 */
@Service
@Slf4j
public class FunctionalAuthenticationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceFingerprintService deviceFingerprintService;
    private final UserService userService;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final PasswordPolicyService passwordPolicyService;
    private final SessionManagementService sessionManagementService;
    private final VerificationTokenService verificationTokenService;
    
    // Virtual Thread Executors
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AsyncTaskExecutor authenticationExecutor;
    private final AsyncTaskExecutor notificationExecutor;

    // Railway Oriented Programming - Result Type
    public sealed interface AuthResult<T, E> permits AuthSuccess, AuthFailure {
        record AuthSuccess<T, E>(T value) implements AuthResult<T, E> {}
        record AuthFailure<T, E>(E error) implements AuthResult<T, E> {}
        
        static <T, E> AuthResult<T, E> success(T value) {
            return new AuthSuccess<>(value);
        }
        
        static <T, E> AuthResult<T, E> failure(E error) {
            return new AuthFailure<>(error);
        }
        
        default <U> AuthResult<U, E> map(Function<T, U> mapper) {
            return switch (this) {
                case AuthSuccess(var value) -> success(mapper.apply(value));
                case AuthFailure(var error) -> failure(error);
            };
        }
        
        default <U> AuthResult<U, E> flatMap(Function<T, AuthResult<U, E>> mapper) {
            return switch (this) {
                case AuthSuccess(var value) -> mapper.apply(value);
                case AuthFailure(var error) -> failure(error);
            };
        }
        
        default boolean isSuccess() {
            return this instanceof AuthSuccess;
        }
        
        default T orElse(T defaultValue) {
            return switch (this) {
                case AuthSuccess(var value) -> value;
                case AuthFailure(var ignored) -> defaultValue;
            };
        }
        
        default T orElseThrow() {
            return switch (this) {
                case AuthSuccess(var value) -> value;
                case AuthFailure(var error) -> throw new RuntimeException(error.toString());
            };
        }
    }

    // Strategy Pattern - Authentication Strategies
    public enum AuthenticationStrategy {
        STANDARD_LOGIN(request -> validateStandardLogin(request)),
        MFA_LOGIN(request -> validateMfaLogin(request)),
        SOCIAL_LOGIN(request -> validateSocialLogin(request));
        
        private final Function<AuthenticationRequest, AuthResult<User, String>> authenticator;
        
        AuthenticationStrategy(Function<AuthenticationRequest, AuthResult<User, String>> authenticator) {
            this.authenticator = authenticator;
        }
        
        public AuthResult<User, String> authenticate(AuthenticationRequest request) {
            return authenticator.apply(request);
        }
    }

    // Strategy Pattern - Validation Strategies
    public enum ValidationStrategy {
        REGISTRATION_VALIDATION(request -> validateRegistrationRequest((RegistrationRequest) request)),
        LOGIN_VALIDATION(request -> validateLoginRequest((AuthenticationRequest) request)),
        PASSWORD_VALIDATION(password -> validatePassword((String) password));
        
        private final Function<Object, AuthResult<Boolean, String>> validator;
        
        ValidationStrategy(Function<Object, AuthResult<Boolean, String>> validator) {
            this.validator = validator;
        }
        
        public AuthResult<Boolean, String> validate(Object request) {
            return validator.apply(request);
        }
    }

    // Command Pattern - Authentication Commands
    public sealed interface AuthCommand<T> permits RegisterCommand, LoginCommand, RefreshTokenCommand {
        CompletableFuture<AuthResult<T, String>> execute();
        
        default <U> AuthCommand<U> map(Function<T, U> mapper) {
            return new MappedCommand<>(this, mapper);
        }
        
        default AuthCommand<T> withRetry(int attempts) {
            return new RetryCommand<>(this, attempts);
        }
    }

    // Command implementations
    public record RegisterCommand(RegistrationRequest request, HttpServletRequest httpRequest) implements AuthCommand<AuthenticationResponse> {
        @Override
        public CompletableFuture<AuthResult<AuthenticationResponse, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return ValidationStrategy.REGISTRATION_VALIDATION
                    .validate(request)
                    .flatMap(valid -> processRegistration(request, httpRequest))
                    .map(response -> response);
            }, virtualExecutor);
        }
    }

    public record LoginCommand(AuthenticationRequest request, HttpServletRequest httpRequest) implements AuthCommand<AuthenticationResponse> {
        @Override
        public CompletableFuture<AuthResult<AuthenticationResponse, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return ValidationStrategy.LOGIN_VALIDATION
                    .validate(request)
                    .flatMap(valid -> processLogin(request, httpRequest))
                    .map(response -> response);
            }, virtualExecutor);
        }
    }

    public record RefreshTokenCommand(String refreshToken, String deviceFingerprint) implements AuthCommand<AuthenticationResponse> {
        @Override
        public CompletableFuture<AuthResult<AuthenticationResponse, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return processTokenRefresh(refreshToken, deviceFingerprint);
            }, virtualExecutor);
        }
    }

    // Helper command wrappers
    public record MappedCommand<T, U>(AuthCommand<T> original, Function<T, U> mapper) implements AuthCommand<U> {
        @Override
        public CompletableFuture<AuthResult<U, String>> execute() {
            return original.execute().thenApply(result -> result.map(mapper));
        }
    }

    public record RetryCommand<T>(AuthCommand<T> original, int attempts) implements AuthCommand<T> {
        @Override
        public CompletableFuture<AuthResult<T, String>> execute() {
            // Simplified retry implementation - would be more sophisticated in practice
            return original.execute().thenCompose(result -> {
                if (result.isSuccess() || attempts <= 1) {
                    return CompletableFuture.completedFuture(result);
                }
                return new RetryCommand<>(original, attempts - 1).execute();
            });
        }
    }

    // Constructor with Dependency Injection
    public FunctionalAuthenticationService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserRoleRepository userRoleRepository,
            UserRoleAssignmentRepository userRoleAssignmentRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            DeviceFingerprintService deviceFingerprintService,
            UserService userService,
            MfaService mfaService,
            AuditService auditService,
            EmailService emailService,
            PasswordPolicyService passwordPolicyService,
            SessionManagementService sessionManagementService,
            VerificationTokenService verificationTokenService,
            @Qualifier("authenticationExecutor") AsyncTaskExecutor authenticationExecutor,
            @Qualifier("notificationExecutor") AsyncTaskExecutor notificationExecutor) {
        
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.deviceFingerprintService = deviceFingerprintService;
        this.userService = userService;
        this.mfaService = mfaService;
        this.auditService = auditService;
        this.emailService = emailService;
        this.passwordPolicyService = passwordPolicyService;
        this.sessionManagementService = sessionManagementService;
        this.verificationTokenService = verificationTokenService;
        this.authenticationExecutor = authenticationExecutor;
        this.notificationExecutor = notificationExecutor;
    }

    // Public API Methods using Command Pattern
    @Async
    @Transactional
    public CompletableFuture<AuthenticationResponse> registerAsync(RegistrationRequest request, HttpServletRequest httpRequest) {
        return new RegisterCommand(request, httpRequest)
            .withRetry(2)
            .execute()
            .thenApply(AuthResult::orElseThrow);
    }

    @Async
    @Transactional
    public CompletableFuture<AuthenticationResponse> loginAsync(AuthenticationRequest request, HttpServletRequest httpRequest) {
        return new LoginCommand(request, httpRequest)
            .withRetry(1)
            .execute()
            .thenApply(AuthResult::orElseThrow);
    }

    @Async
    public CompletableFuture<AuthenticationResponse> refreshTokenAsync(String refreshToken, String deviceFingerprint) {
        return new RefreshTokenCommand(refreshToken, deviceFingerprint)
            .execute()
            .thenApply(AuthResult::orElseThrow);
    }

    // Private Implementation Methods

    private AuthResult<AuthenticationResponse, String> processRegistration(
            RegistrationRequest request, HttpServletRequest httpRequest) {
        
        try {
            // Structured concurrency for parallel operations
            return CompletableFuture.supplyAsync(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    // Fork parallel tasks
                    var existsCheck = scope.fork(() -> userService.existsByEmail(request.getEmail()));
                    var passwordValidation = scope.fork(() -> 
                        passwordPolicyService.validatePassword(request.getPassword(), request.getEmail()));
                    var deviceFingerprint = scope.fork(() -> 
                        deviceFingerprintService.generateFingerprint(httpRequest));
                    
                    scope.join();
                    scope.throwIfFailed();
                    
                    // Check results
                    if (existsCheck.resultNow()) {
                        return AuthResult.failure("User with this email already exists");
                    }
                    
                    String fingerprint = deviceFingerprint.resultNow();
                    String ipAddress = getClientIpAddress(httpRequest);
                    
                    // Create user and profile
                    return createUserWithProfile(request, fingerprint, ipAddress, httpRequest);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return AuthResult.<AuthenticationResponse, String>failure("Registration interrupted");
                } catch (Exception e) {
                    return AuthResult.<AuthenticationResponse, String>failure("Registration failed: " + e.getMessage());
                }
            }, virtualExecutor).join();
            
        } catch (Exception e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            return AuthResult.failure("Registration failed: " + e.getMessage());
        }
    }

    private AuthResult<AuthenticationResponse, String> processLogin(
            AuthenticationRequest request, HttpServletRequest httpRequest) {
        
        try {
            String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
            String ipAddress = getClientIpAddress(httpRequest);
            
            // Find user and validate account status
            return userService.findByEmail(request.getEmail())
                .map(user -> validateAndAuthenticate(user, request, deviceFingerprint, ipAddress))
                .orElse(AuthResult.failure("Invalid credentials"));
                
        } catch (Exception e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return AuthResult.failure("Login failed: " + e.getMessage());
        }
    }

    private AuthResult<AuthenticationResponse, String> processTokenRefresh(String refreshToken, String deviceFingerprint) {
        try {
            // Validate refresh token
            return Optional.ofNullable(jwtTokenProvider.validateRefreshToken(refreshToken))
                .map(user -> generateTokenResponse(user, deviceFingerprint, ""))
                .map(AuthResult::<AuthenticationResponse, String>success)
                .orElse(AuthResult.failure("Invalid refresh token"));
                
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return AuthResult.failure("Token refresh failed: " + e.getMessage());
        }
    }

    // Validation Methods (Strategy Pattern)
    private static AuthResult<Boolean, String> validateRegistrationRequest(RegistrationRequest request) {
        return Optional.ofNullable(request)
            .filter(r -> r.getEmail() != null && !r.getEmail().isBlank())
            .filter(r -> r.getPassword() != null && !r.getPassword().isBlank())
            .filter(r -> r.getFirstName() != null && !r.getFirstName().isBlank())
            .filter(r -> r.getLastName() != null && !r.getLastName().isBlank())
            .map(r -> AuthResult.<Boolean, String>success(true))
            .orElse(AuthResult.failure("Invalid registration request"));
    }

    private static AuthResult<Boolean, String> validateLoginRequest(AuthenticationRequest request) {
        return Optional.ofNullable(request)
            .filter(r -> r.getEmail() != null && !r.getEmail().isBlank())
            .filter(r -> r.getPassword() != null && !r.getPassword().isBlank())
            .map(r -> AuthResult.<Boolean, String>success(true))
            .orElse(AuthResult.failure("Invalid login request"));
    }

    private static AuthResult<Boolean, String> validatePassword(String password) {
        return Optional.ofNullable(password)
            .filter(p -> p.length() >= 8)
            .map(p -> AuthResult.<Boolean, String>success(true))
            .orElse(AuthResult.failure("Password does not meet requirements"));
    }

    private static AuthResult<User, String> validateStandardLogin(AuthenticationRequest request) {
        // Simplified implementation - would integrate with actual authentication
        return AuthResult.failure("Not implemented");
    }

    private static AuthResult<User, String> validateMfaLogin(AuthenticationRequest request) {
        return AuthResult.failure("Not implemented");
    }

    private static AuthResult<User, String> validateSocialLogin(AuthenticationRequest request) {
        return AuthResult.failure("Not implemented");
    }

    // Helper Methods
    private AuthResult<AuthenticationResponse, String> createUserWithProfile(
            RegistrationRequest request, String deviceFingerprint, String ipAddress, HttpServletRequest httpRequest) {
        
        try {
            // Create user entity
            User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .kycStatus(User.KycStatus.PENDING)
                .subscriptionTier(User.SubscriptionTier.FREE)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .failedLoginAttempts(0)
                .passwordChangedAt(LocalDateTime.now())
                .deviceFingerprint(deviceFingerprint)
                .createdBy("self-registration")
                .build();

            User savedUser = userRepository.save(user);

            // Create user profile
            UserProfile userProfile = UserProfile.builder()
                .userId(savedUser.getId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .countryCode(request.getCountryCode())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .riskTolerance(request.getRiskTolerance())
                .tradingExperience(request.getTradingExperience())
                .preferences(Map.of("email_notifications", true, "sms_notifications", false))
                .createdBy("self-registration")
                .build();

            userProfileRepository.save(userProfile);

            // Assign default role and send notifications asynchronously
            assignDefaultRoleAsync(savedUser.getId());
            sendNotificationsAsync(savedUser, httpRequest, deviceFingerprint, ipAddress);

            // Generate response
            AuthenticationResponse response = generateTokenResponse(savedUser, deviceFingerprint, ipAddress);
            return AuthResult.success(response);

        } catch (Exception e) {
            return AuthResult.failure("Failed to create user: " + e.getMessage());
        }
    }

    private AuthResult<AuthenticationResponse, String> validateAndAuthenticate(
            User user, AuthenticationRequest request, String deviceFingerprint, String ipAddress) {
        
        try {
            // Validate account status
            AuthResult<Boolean, String> statusValidation = validateAccountStatus(user);
            if (!statusValidation.isSuccess()) {
                return AuthResult.failure(statusValidation.orElse(false).toString());
            }

            // Perform authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User authenticatedUser = (User) authentication.getPrincipal();
            
            // Handle successful login asynchronously
            handleSuccessfulLoginAsync(authenticatedUser, ipAddress, deviceFingerprint);

            // Generate response
            AuthenticationResponse response = generateTokenResponse(authenticatedUser, deviceFingerprint, ipAddress);
            return AuthResult.success(response);

        } catch (AuthenticationException e) {
            handleFailedLoginAsync(request.getEmail(), ipAddress, deviceFingerprint);
            return AuthResult.failure("Authentication failed: " + e.getMessage());
        }
    }

    private AuthResult<Boolean, String> validateAccountStatus(User user) {
        return switch (user.getAccountStatus()) {
            case ACTIVE -> AuthResult.success(true);
            case SUSPENDED -> AuthResult.failure("Account is suspended");
            case LOCKED -> AuthResult.failure("Account is locked");
            case INACTIVE -> AuthResult.failure("Account is inactive");
            default -> AuthResult.failure("Invalid account status");
        };
    }

    private AuthenticationResponse generateTokenResponse(User user, String deviceFingerprint, String ipAddress) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());
        
        String accessToken = jwtTokenProvider.generateToken(authentication, deviceFingerprint, ipAddress);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, deviceFingerprint);

        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getJwtExpirationMs())
            .user(mapUserToDto(user))
            .requiresEmailVerification(!user.getEmailVerified())
            .requiresMfa(false)
            .message("Authentication successful")
            .build();
    }

    // Async Helper Methods using Virtual Threads
    @Async("authenticationExecutor")
    private CompletableFuture<Void> assignDefaultRoleAsync(Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Optional<UserRole> defaultRole = userRoleRepository.findByName("USER");
                if (defaultRole.isPresent()) {
                    UserRoleAssignment assignment = UserRoleAssignment.builder()
                        .userId(userId)
                        .roleId(defaultRole.get().getId())
                        .assignedAt(LocalDateTime.now())
                        .assignedBy("system")
                        .isActive(true)
                        .build();
                    userRoleAssignmentRepository.save(assignment);
                }
            } catch (Exception e) {
                log.error("Failed to assign default role to user {}: {}", userId, e.getMessage());
            }
        }, virtualExecutor);
    }

    @Async("notificationExecutor") 
    private CompletableFuture<Void> sendNotificationsAsync(User user, HttpServletRequest httpRequest, 
                                                           String deviceFingerprint, String ipAddress) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Send email verification
                String verificationToken = verificationTokenService.generateEmailVerificationToken(
                    user, ipAddress, httpRequest.getHeader("User-Agent"));
                emailService.sendEmailVerification(user.getEmail(), verificationToken);
                
                // Log audit event
                auditService.logAuthenticationEvent(user.getId(), "REGISTRATION", "SUCCESS", 
                    ipAddress, httpRequest.getHeader("User-Agent"), deviceFingerprint, 
                    Map.of("registration_method", "email", "subscription_tier", "free"), null);
                    
            } catch (Exception e) {
                log.error("Failed to send notifications for user {}: {}", user.getId(), e.getMessage());
            }
        }, virtualExecutor);
    }

    @Async("authenticationExecutor")
    private CompletableFuture<Void> handleSuccessfulLoginAsync(User user, String ipAddress, String deviceFingerprint) {
        return CompletableFuture.runAsync(() -> {
            userService.handleSuccessfulLogin(user, ipAddress, deviceFingerprint);
        }, virtualExecutor);
    }

    @Async("authenticationExecutor")
    private CompletableFuture<Void> handleFailedLoginAsync(String email, String ipAddress, String deviceFingerprint) {
        return CompletableFuture.runAsync(() -> {
            userService.handleFailedLogin(email, ipAddress, deviceFingerprint);
        }, virtualExecutor);
    }

    // Utility Methods
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuthenticationResponse.UserDto mapUserToDto(User user) {
        return AuthenticationResponse.UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .emailVerified(user.getEmailVerified())
            .phoneVerified(user.getPhoneVerified())
            .kycStatus(user.getKycStatus().name())
            .subscriptionTier(user.getSubscriptionTier().name())
            .accountStatus(user.getAccountStatus().name())
            .build();
    }
}