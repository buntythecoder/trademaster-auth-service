package com.trademaster.auth.service;

import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.entity.User;
import com.trademaster.auth.entity.UserProfile;
import com.trademaster.auth.entity.UserRole;
import com.trademaster.auth.entity.UserRoleAssignment;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.ServiceOperations;
import com.trademaster.auth.repository.UserProfileRepository;
import com.trademaster.auth.repository.UserRepository;
import com.trademaster.auth.repository.UserRoleAssignmentRepository;
import com.trademaster.auth.repository.UserRoleRepository;
import com.trademaster.auth.validator.AuthenticationValidators;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * User Registration Service - SOLID Single Responsibility Principle
 *
 * Responsibilities:
 * - User registration and account creation
 * - Email verification initiation
 * - User profile creation
 * - Default role assignment
 * - Registration audit logging
 *
 * This service is 100% functional programming compliant:
 * - No if-else statements (uses Optional, pattern matching, Result types)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - No for/while loops (uses Stream API and functional composition)
 * - Uses Virtual Threads for async operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;

    /**
     * Async user registration using railway-oriented programming
     *
     * @param request Registration request with user details
     * @return CompletableFuture containing Result with created User or error message
     */
    @Transactional(readOnly = false)
    public CompletableFuture<Result<User, String>> registerUser(RegistrationRequest request) {
        return CompletableFuture.supplyAsync(() ->
            createRegistrationPipeline().apply(request),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Sync user registration wrapper
     *
     * @param request Registration request with user details
     * @param httpRequest HTTP request for context
     * @return Result containing created User or error message
     */
    @Transactional(readOnly = false)
    public Result<User, String> register(RegistrationRequest request, HttpServletRequest httpRequest) {
        return createRegistrationPipeline().apply(request);
    }

    // Private pipeline methods - Functional composition

    /**
     * Function composition pipeline for registration
     * Chain of responsibility pattern with functional composition
     */
    private Function<RegistrationRequest, Result<User, String>> createRegistrationPipeline() {
        return request -> {
            Result<RegistrationRequest, String> validated = validateRegistrationRequest().apply(request);
            Result<RegistrationRequest, String> emailChecked = checkEmailUniqueness().apply(validated);
            Result<UserCreationData, String> userCreated = createUserEntity().apply(emailChecked);
            Result<UserCreationData, String> userSaved = saveUser().apply(userCreated);
            Result<UserCreationData, String> profileCreated = createUserProfile().apply(userSaved);
            Result<UserCreationData, String> roleAssigned = assignDefaultRole().apply(profileCreated);
            Result<UserCreationData, String> emailSent = sendVerificationEmail().apply(roleAssigned);
            Result<User, String> audited = auditRegistration().apply(emailSent);
            return audited;
        };
    }

    /**
     * Registration validation using functional chains
     */
    private Function<RegistrationRequest, Result<RegistrationRequest, String>> validateRegistrationRequest() {
        return AuthenticationValidators.validateRegistrationRequest();
    }

    /**
     * Email uniqueness check using Optional - replaces if-else
     */
    private Function<Result<RegistrationRequest, String>, Result<RegistrationRequest, String>> checkEmailUniqueness() {
        return result -> result.flatMap(request -> {
            boolean exists = userService.existsByEmail(request.getEmail());
            return exists ? Result.failure("Email already exists") : Result.success(request);
        });
    }

    /**
     * User entity creation using functional mapping
     */
    private Function<Result<RegistrationRequest, String>, Result<UserCreationData, String>> createUserEntity() {
        return result -> result.map(request -> {
            User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(false) // Email verification required
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .build();
            return new UserCreationData(request, user);
        });
    }

    /**
     * User persistence using SafeOperations
     */
    private Function<Result<UserCreationData, String>, Result<UserCreationData, String>> saveUser() {
        return result -> result.flatMap(data ->
            ServiceOperations.execute("saveUser", () -> userRepository.save(data.user()))
                .map(savedUser -> new UserCreationData(data.request(), savedUser))
        );
    }

    /**
     * User profile creation using functional composition
     */
    private Function<Result<UserCreationData, String>, Result<UserCreationData, String>> createUserProfile() {
        return result -> result.flatMap(data ->
            ServiceOperations.execute("createUserProfile", () -> {
                UserProfile profile = UserProfile.builder()
                    .user(data.user())
                    .dateOfBirth(data.request().getDateOfBirth())
                    .phoneNumber(data.request().getPhoneNumber())
                    .address(data.request().getAddress())
                    .createdAt(LocalDateTime.now())
                    .build();
                return userProfileRepository.save(profile);
            }).map(profile -> data)
        );
    }

    /**
     * Role assignment using functional strategies
     */
    private Function<Result<UserCreationData, String>, Result<UserCreationData, String>> assignDefaultRole() {
        return result -> result.flatMap(data ->
            findOrCreateDefaultRole()
                .flatMap(role -> assignRoleToUser(data.user(), role))
                .map(assignment -> data)
        );
    }

    /**
     * Email verification sending using functional approach
     */
    private Function<Result<UserCreationData, String>, Result<UserCreationData, String>> sendVerificationEmail() {
        return result -> result.map(data -> {
            generateVerificationToken(data.user())
                .map(token -> {
                    emailService.sendEmailVerification(data.user().getEmail(), token);
                    return token;
                });
            return data;
        });
    }

    /**
     * Registration auditing using functional logging
     */
    private Function<Result<UserCreationData, String>, Result<User, String>> auditRegistration() {
        return result -> result.map(data -> {
            auditService.logAuthenticationEvent(
                data.user().getId(),
                "USER_REGISTRATION",
                "SUCCESS",
                "127.0.0.1", // Default for registration
                "Registration Service",
                null,
                Map.of("email", data.user().getEmail()),
                null
            );
            return data.user();
        });
    }

    // Private helper methods - Supporting operations

    /**
     * Find or create default USER role
     */
    private Result<UserRole, String> findOrCreateDefaultRole() {
        return SafeOperations.safelyToResult(() ->
            userRoleRepository.findByRoleName("USER")
                .orElseGet(() -> userRoleRepository.save(
                    UserRole.builder()
                        .roleName("USER")
                        .description("Default user role")
                        .build()
                ))
        );
    }

    /**
     * Assign role to user using functional operations
     */
    private Result<UserRoleAssignment, String> assignRoleToUser(User user, UserRole role) {
        return ServiceOperations.execute("assignRole", () ->
            userRoleAssignmentRepository.save(
                UserRoleAssignment.builder()
                    .user(user)
                    .role(role)
                    .assignedAt(LocalDateTime.now())
                    .build()
            )
        );
    }

    /**
     * Generate verification token using Optional pattern
     */
    private Optional<String> generateVerificationToken(User user) {
        return SafeOperations.safely(() ->
            verificationTokenService.generateEmailVerificationToken(user, "127.0.0.1", "AuthService")
        );
    }

    // Data classes for type safety and immutability

    /**
     * Immutable data holder for registration pipeline
     */
    private record UserCreationData(RegistrationRequest request, User user) {}
}
