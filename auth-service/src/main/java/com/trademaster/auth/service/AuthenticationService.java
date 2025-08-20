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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Authentication Service for user registration, login, and token management
 * 
 * Features:
 * - User registration with validation
 * - Secure login with device fingerprinting
 * - JWT token generation and refresh
 * - Failed login attempt tracking
 * - Account security validation
 * - MFA integration hooks
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

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

    /**
     * Register a new user
     */
    @Transactional
    public AuthenticationResponse register(RegistrationRequest request, HttpServletRequest httpRequest) {
        log.info("Processing registration request for email: {}", request.getEmail());

        // Validate registration request
        validateRegistrationRequest(request);

        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Validate password policy
        passwordPolicyService.validatePassword(request.getPassword(), request.getEmail());

        // Generate device fingerprint
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);

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

            // Save user
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

            // Assign default USER role
            assignDefaultRole(savedUser.getId());

            // Log registration event
            auditService.logAuthenticationEvent(savedUser.getId(), "REGISTRATION", "SUCCESS", 
                ipAddress, httpRequest.getHeader("User-Agent"), deviceFingerprint, 
                Map.of("registration_method", "email", "subscription_tier", "free"), null);

            // Generate and send email verification token
            String verificationToken = verificationTokenService.generateEmailVerificationToken(
                savedUser, ipAddress, httpRequest.getHeader("User-Agent"));
            emailService.sendEmailVerification(savedUser.getEmail(), verificationToken);

            // Generate tokens (user needs to verify email before full access)
            String accessToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(savedUser, null, savedUser.getAuthorities()), 
                deviceFingerprint, ipAddress);
            
            String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser, deviceFingerprint);

            log.info("User registered successfully: {}", savedUser.getEmail());

            return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs())
                .user(mapUserToDto(savedUser))
                .requiresEmailVerification(true)
                .requiresMfa(false)
                .message("Registration successful. Please verify your email address.")
                .build();

        } catch (Exception e) {
            // Log failed registration
            auditService.logAuthenticationEvent(null, "REGISTRATION", "FAILED", 
                ipAddress, httpRequest.getHeader("User-Agent"), deviceFingerprint, 
                Map.of("email", request.getEmail(), "error", e.getMessage()), null);
            
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticate user login
     */
    @Transactional
    public AuthenticationResponse login(AuthenticationRequest request, HttpServletRequest httpRequest) {
        log.info("Processing login request for email: {}", request.getEmail());

        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);

        try {
            // Find user first to check account status
            Optional<User> userOpt = userService.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                userService.handleFailedLogin(request.getEmail(), ipAddress, deviceFingerprint);
                throw new BadCredentialsException("Invalid credentials");
            }

            User user = userOpt.get();

            // Check account status before authentication
            validateAccountStatus(user);

            // Perform authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User authenticatedUser = (User) authentication.getPrincipal();

            // Handle successful login
            userService.handleSuccessfulLogin(authenticatedUser, ipAddress, deviceFingerprint);

            // Check if MFA is required
            boolean requiresMfa = mfaService.isUserMfaEnabled(authenticatedUser.getId());
            
            if (requiresMfa && !request.isMfaBypass()) {
                // Generate MFA challenge
                String mfaChallenge = mfaService.generateMfaChallenge(authenticatedUser.getId());
                
                // Return partial response requiring MFA
                return AuthenticationResponse.builder()
                    .requiresMfa(true)
                    .mfaChallenge(mfaChallenge)
                    .message("MFA verification required")
                    .build();
            }

            // Generate tokens
            String accessToken = jwtTokenProvider.generateToken(authentication, deviceFingerprint, ipAddress);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authenticatedUser, deviceFingerprint);

            log.info("User logged in successfully: {}", authenticatedUser.getEmail());

            return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs())
                .user(mapUserToDto(authenticatedUser))
                .requiresEmailVerification(!authenticatedUser.getEmailVerified())
                .requiresMfa(false)
                .message("Login successful")
                .build();

        } catch (AuthenticationException e) {
            userService.handleFailedLogin(request.getEmail(), ipAddress, deviceFingerprint);
            
            String errorMessage = getAuthenticationErrorMessage(e);
            log.warn("Login failed for email {}: {}", request.getEmail(), errorMessage);
            throw new BadCredentialsException(errorMessage);
        }
    }

    /**
     * Complete MFA verification
     */
    @Transactional
    public AuthenticationResponse completeMfaVerification(String email, String mfaToken, String mfaCode, 
                                                         HttpServletRequest httpRequest) {
        log.info("Processing MFA verification for email: {}", email);

        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);

        try {
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Verify MFA code
            boolean mfaValid = mfaService.verifyMfaCode(user.getId(), mfaToken, mfaCode);
            
            if (!mfaValid) {
                auditService.logAuthenticationEvent(user.getId(), "MFA_FAILED", "FAILED", 
                    ipAddress, httpRequest.getHeader("User-Agent"), deviceFingerprint, 
                    Map.of("mfa_method", "code_verification"), null);
                
                throw new BadCredentialsException("Invalid MFA code");
            }

            // Log successful MFA
            auditService.logAuthenticationEvent(user.getId(), "MFA_SUCCESS", "SUCCESS", 
                ipAddress, httpRequest.getHeader("User-Agent"), deviceFingerprint, 
                Map.of("mfa_method", "code_verification"), null);

            // Generate tokens
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            String accessToken = jwtTokenProvider.generateToken(authentication, deviceFingerprint, ipAddress);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user, deviceFingerprint);

            log.info("MFA verification successful for user: {}", user.getEmail());

            return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs())
                .user(mapUserToDto(user))
                .requiresEmailVerification(!user.getEmailVerified())
                .requiresMfa(false)
                .message("MFA verification successful")
                .build();

        } catch (Exception e) {
            log.error("MFA verification failed for email {}: {}", email, e.getMessage());
            throw new BadCredentialsException("MFA verification failed");
        }
    }

    /**
     * Refresh JWT token
     */
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        try {
            if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            User user = userService.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Validate device fingerprint
            String currentFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
            String tokenFingerprint = jwtTokenProvider.getDeviceFingerprintFromToken(refreshToken);
            
            if (!jwtTokenProvider.validateDeviceFingerprint(refreshToken, currentFingerprint)) {
                throw new BadCredentialsException("Device fingerprint mismatch");
            }

            // Generate new tokens
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            String newAccessToken = jwtTokenProvider.generateToken(authentication, currentFingerprint, 
                getClientIpAddress(httpRequest));
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user, currentFingerprint);

            // Log token refresh
            auditService.logAuthenticationEvent(userId, "TOKEN_REFRESHED", "SUCCESS", 
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"), 
                currentFingerprint, null, null);

            return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs())
                .user(mapUserToDto(user))
                .message("Token refreshed successfully")
                .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Token refresh failed");
        }
    }

    // Private helper methods

    private void validateRegistrationRequest(RegistrationRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }

    private void validateAccountStatus(User user) {
        if (user.getAccountStatus() == User.AccountStatus.LOCKED) {
            if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                throw new LockedException("Account is locked until " + user.getAccountLockedUntil());
            }
        }
        
        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            throw new DisabledException("Account is suspended");
        }
        
        if (user.getAccountStatus() == User.AccountStatus.DEACTIVATED) {
            throw new DisabledException("Account is deactivated");
        }
    }

    private void assignDefaultRole(Long userId) {
        UserRole userRole = userRoleRepository.findByRoleName("USER")
            .orElseThrow(() -> new RuntimeException("Default USER role not found"));

        UserRoleAssignment assignment = UserRoleAssignment.builder()
            .userId(userId)
            .roleId(userRole.getId())
            .assignedBy("system")
            .build();

        userRoleAssignmentRepository.save(assignment);
    }

    private String getAuthenticationErrorMessage(AuthenticationException e) {
        if (e instanceof LockedException) {
            return "Account is locked. Please try again later or contact support.";
        } else if (e instanceof DisabledException) {
            return "Account is disabled. Please contact support.";
        } else {
            return "Invalid credentials";
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * User logout - invalidate tokens and sessions
     */
    public void logout(String accessToken, String sessionId, String ipAddress) {
        try {
            // Extract user ID from token for session invalidation
            Long userIdLong = jwtTokenProvider.getUserIdFromToken(accessToken);
            if (userIdLong != null) {
                String deviceFingerprint = jwtTokenProvider.getDeviceFingerprintFromToken(accessToken);
                
                // Invalidate the session
                sessionManagementService.invalidateSession(sessionId, "USER_LOGOUT");
                
                // Log logout event for audit trail
                auditService.logAuthenticationEvent(userIdLong, "USER_LOGOUT", "SUCCESS", 
                    ipAddress, "Unknown", deviceFingerprint, 
                    Map.of("session_id", sessionId, "logout_method", "manual"), 
                    sessionId);
                
                log.info("User {} logged out successfully from IP: {}", userIdLong, ipAddress);
            }
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed", e);
        }
    }

    /**
     * Verify email using verification token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        try {
            Optional<User> userOpt = verificationTokenService.verifyEmailToken(token);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Save the user with updated email verification status
                userRepository.save(user);
                
                // Log the email verification event
                auditService.logAuthenticationEvent(user.getId(), "EMAIL_VERIFIED", "SUCCESS", 
                    "Unknown", "Unknown", "Unknown", 
                    Map.of("verification_method", "email_token"), null);
                
                log.info("Email verification successful for user: {}", user.getId());
                return true;
            } else {
                log.warn("Email verification failed for token: {}", token.substring(0, 8) + "...");
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error during email verification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Request password reset
     */
    @Transactional
    public boolean requestPasswordReset(String email, String ipAddress, String userAgent) {
        try {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
            if (userOpt.isEmpty()) {
                // Don't reveal if email exists for security
                log.warn("Password reset requested for non-existent email: {}", email);
                return true; // Return true to not reveal email existence
            }
            
            User user = userOpt.get();
            
            // Check if account is locked or deactivated
            if (user.getAccountStatus() == User.AccountStatus.LOCKED || 
                user.getAccountStatus() == User.AccountStatus.DEACTIVATED) {
                log.warn("Password reset requested for locked/disabled account: {}", email);
                return true; // Return true to not reveal account status
            }
            
            // Generate password reset token
            String resetToken = verificationTokenService.generatePasswordResetToken(user, ipAddress, userAgent);
            
            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            
            // Log password reset request
            auditService.logAuthenticationEvent(user.getId(), "PASSWORD_RESET_REQUESTED", "SUCCESS", 
                ipAddress, userAgent, "Unknown", 
                Map.of("reset_method", "email"), null);
            
            log.info("Password reset requested for user: {}", user.getId());
            return true;
            
        } catch (Exception e) {
            log.error("Error during password reset request: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reset password using token
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword, String ipAddress, String userAgent) {
        try {
            // Verify reset token first
            Optional<User> userOpt = verificationTokenService.verifyPasswordResetToken(token);
            if (userOpt.isEmpty()) {
                log.warn("Invalid or expired password reset token: {}", token.substring(0, 8) + "...");
                return false;
            }
            
            User user = userOpt.get();
            
            // Validate new password with user's email
            passwordPolicyService.validatePassword(newPassword, user.getEmail());
            
            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setPasswordChangedAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0); // Reset failed attempts
            
            // If account was locked due to failed attempts, unlock it
            if (user.getAccountStatus() == User.AccountStatus.LOCKED) {
                user.setAccountStatus(User.AccountStatus.ACTIVE);
            }
            
            userRepository.save(user);
            
            // Mark token as used
            verificationTokenService.markPasswordResetTokenAsUsed(token);
            
            // Invalidate all user sessions for security
            sessionManagementService.invalidateAllUserSessions(user.getId(), "PASSWORD_RESET");
            
            // Log password reset success
            auditService.logAuthenticationEvent(user.getId(), "PASSWORD_RESET_COMPLETED", "SUCCESS", 
                ipAddress, userAgent, "Unknown", 
                Map.of("reset_method", "email_token"), null);
            
            log.info("Password reset successful for user: {}", user.getId());
            return true;
            
        } catch (Exception e) {
            log.error("Error during password reset: {}", e.getMessage());
            return false;
        }
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .kycStatus(user.getKycStatus())
            .subscriptionTier(user.getSubscriptionTier())
            .accountStatus(user.getAccountStatus())
            .emailVerified(user.getEmailVerified())
            .phoneVerified(user.getPhoneVerified())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }

    // DTO classes would be defined in separate files
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String email;
        private User.KycStatus kycStatus;
        private User.SubscriptionTier subscriptionTier;
        private User.AccountStatus accountStatus;
        private Boolean emailVerified;
        private Boolean phoneVerified;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
    }
}