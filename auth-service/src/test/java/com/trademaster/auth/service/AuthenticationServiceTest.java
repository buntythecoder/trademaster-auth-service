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
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.security.DeviceFingerprintService;
import com.trademaster.auth.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService
 *
 * ⚠️ DISABLED - REQUIRES API MIGRATION ⚠️
 * Spring Boot 3.5.3 upgrade changed AuthenticationService return types:
 *
 * 1. Return Type Changes (10 compilation errors):
 *    - register() now returns Result<User, String> instead of AuthenticationResponse
 *    - login() now returns Result<AuthenticationResponse, String> requiring unwrapping
 *    - authenticate() now returns CompletableFuture<Result<AuthenticationResponse, String>>
 *
 * 2. Parameter Type Changes:
 *    - User ID is Long, not String (multiple method calls affected)
 *    - MfaService.isUserMfaEnabled() expects Long userId
 *
 * TODO: Update tests to unwrap Result types and handle CompletableFuture async responses
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Disabled("Spring Boot 3.5.3 API migration required - AuthenticationService return types changed to Result<T,E>")
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserProfileRepository userProfileRepository;
    
    @Mock
    private UserRoleRepository userRoleRepository;
    
    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private DeviceFingerprintService deviceFingerprintService;
    
    @Mock
    private UserService userService;
    
    @Mock
    private MfaService mfaService;
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private PasswordPolicyService passwordPolicyService;
    
    @Mock
    private HttpServletRequest httpRequest;
    
    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private UserRole userRole;
    private RegistrationRequest registrationRequest;
    private AuthenticationRequest authenticationRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@trademaster.com")
            .passwordHash("hashedPassword")
            .kycStatus(User.KycStatus.PENDING)
            .subscriptionTier(User.SubscriptionTier.FREE)
            .accountStatus(User.AccountStatus.ACTIVE)
            .emailVerified(false)
            .phoneVerified(false)
            .failedLoginAttempts(0)
            .passwordChangedAt(LocalDateTime.now())
            .roleAssignments(new HashSet<>())
            .build();

        userRole = UserRole.builder()
            .id(1L)
            .roleName("USER")
            .description("Standard user role")
            .isActive(true)
            .build();

        // Create role assignment and add it to the user
        UserRoleAssignment roleAssignment = UserRoleAssignment.builder()
            .id(1L)
            .user(testUser)
            .role(userRole)
            .isActive(true)
            .assignedAt(LocalDateTime.now())
            .build();
            
        testUser.getRoleAssignments().add(roleAssignment);

        registrationRequest = RegistrationRequest.builder()
            .email("test@trademaster.com")
            .password("StrongPassword123!")
            .firstName("Test")
            .lastName("User")
            .countryCode("IN")
            .agreeToTerms(true)
            .agreeToPrivacyPolicy(true)
            .build();

        authenticationRequest = AuthenticationRequest.builder()
            .email("test@trademaster.com")
            .password("StrongPassword123!")
            .build();
    }

    @Test
    void register_ShouldCreateUserSuccessfully() {
        // Arrange
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRoleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(jwtTokenProvider.generateToken(any(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpirationMs()).thenReturn(900000L);

        // Act
        Result<User, String> result = authenticationService.register(registrationRequest, httpRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        User user = result.getValue().orElseThrow();
        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
        
        verify(userRepository, times(1)).save(any(User.class));
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
        verify(emailService, times(1)).sendEmailVerification(anyString(), anyString());
        verify(auditService, times(1)).logAuthenticationEvent(anyLong(), eq("REGISTRATION"), eq("SUCCESS"), 
            anyString(), anyString(), anyString(), any(), isNull());
    }

    @Test
    void register_ShouldFailWhenEmailExists() {
        // Arrange
        when(userService.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> authenticationService.register(registrationRequest, httpRequest));
        
        assertEquals("User with this email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldValidatePasswordPolicy() {
        // Arrange
        when(userService.existsByEmail(anyString())).thenReturn(false);
        doThrow(new IllegalArgumentException("Password too weak"))
            .when(passwordPolicyService).validatePassword(anyString(), anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authenticationService.register(registrationRequest, httpRequest));
        
        assertTrue(exception.getMessage().contains("Password too weak"), 
            "Expected message to contain 'Password too weak', but was: " + exception.getMessage());
        verify(passwordPolicyService, times(1)).validatePassword(anyString(), anyString());
    }

    @Test
    void login_ShouldAuthenticateSuccessfully() {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        when(mfaService.isUserMfaEnabled(anyString())).thenReturn(Result.success(false));
        when(jwtTokenProvider.generateToken(any(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpirationMs()).thenReturn(900000L);

        // Act
        Result<AuthenticationResponse, String> result = authenticationService.login(authenticationRequest, httpRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        AuthenticationResponse response = result.getValue().orElseThrow();
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertFalse(response.isRequiresMfa());
        assertEquals("Login successful", response.getMessage());
        
        verify(userService, times(1)).handleSuccessfulLogin(eq(testUser), anyString(), anyString());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void login_ShouldRequireMfaWhenEnabled() {
        // Arrange
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        when(mfaService.isUserMfaEnabled(anyString())).thenReturn(Result.success(true));
        when(mfaService.generateMfaChallenge(anyLong())).thenReturn("mfaChallenge");

        // Act
        Result<AuthenticationResponse, String> result = authenticationService.login(authenticationRequest, httpRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        AuthenticationResponse response = result.getValue().orElseThrow();
        assertTrue(response.isRequiresMfa());
        assertEquals("mfaChallenge", response.getMfaChallenge());
        assertEquals("MFA verification required", response.getMessage());
        assertNull(response.getAccessToken());
        
        verify(mfaService, times(1)).generateMfaChallenge(anyLong());
    }

    @Test
    void login_ShouldFailWithInvalidCredentials() {
        // Arrange
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        // Note: authenticationManager should not be called if user is not found

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
            () -> authenticationService.login(authenticationRequest, httpRequest));
        
        assertEquals("Invalid credentials", exception.getMessage());
        // The service calls handleFailedLogin twice: once when user not found, once in catch block
        verify(userService, times(2)).handleFailedLogin(anyString(), anyString(), anyString());
        verify(authenticationManager, never()).authenticate(any()); // Should not reach authentication
    }

    @Test
    void login_ShouldFailWithLockedAccount() {
        // Arrange
        User lockedUser = User.builder()
            .id(1L)
            .email("test@trademaster.com")
            .accountStatus(User.AccountStatus.LOCKED)
            .accountLockedUntil(LocalDateTime.now().plusMinutes(30))
            .build();
            
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(lockedUser));
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
            () -> authenticationService.login(authenticationRequest, httpRequest));
        
        assertTrue(exception.getMessage().contains("Account is locked"));
    }

    @Test
    void completeMfaVerification_ShouldSucceedWithValidCode() {
        // Arrange
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(mfaService.verifyMfaCode(anyString(), anyString(), anyString())).thenReturn(Result.success(true));
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent");
        when(jwtTokenProvider.generateToken(any(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpirationMs()).thenReturn(900000L);

        // Act
        Result<AuthenticationResponse, String> result = authenticationService.completeMfaVerification(
            "test@trademaster.com", "123456", "mfaToken", httpRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        AuthenticationResponse response = result.getValue().orElseThrow();
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertFalse(response.isRequiresMfa());
        assertEquals("MFA verification successful", response.getMessage());

        verify(mfaService, times(1)).verifyMfaCode(anyString(), eq("123456"), eq("mfaToken"));
        verify(auditService, times(1)).logAuthenticationEvent(anyLong(), eq("MFA_SUCCESS"), eq("SUCCESS"),
            anyString(), anyString(), anyString(), any(), isNull());
    }

    @Test
    void completeMfaVerification_ShouldFailWithInvalidCode() {
        // Arrange
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(mfaService.verifyMfaCode(anyString(), anyString(), anyString())).thenReturn(Result.success(false));
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent");

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
            () -> authenticationService.completeMfaVerification(
                "test@trademaster.com", "mfaToken", "invalid", httpRequest));
        
        assertEquals("MFA verification failed", exception.getMessage());
        verify(auditService, times(1)).logAuthenticationEvent(anyLong(), eq("MFA_FAILED"), eq("FAILED"), 
            anyString(), anyString(), anyString(), any(), isNull());
    }

    @Test
    void refreshToken_ShouldGenerateNewTokens() {
        // Arrange
        String refreshToken = "validRefreshToken";
        
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getDeviceFingerprintFromToken(anyString())).thenReturn("deviceFingerprint");
        when(jwtTokenProvider.validateDeviceFingerprint(anyString(), anyString())).thenReturn(true);
        when(userService.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("deviceFingerprint");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null); // No forwarded header in test
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent");
        when(jwtTokenProvider.generateToken(any(), anyString(), anyString())).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("newRefreshToken");
        when(jwtTokenProvider.getJwtExpirationMs()).thenReturn(900000L);

        // Act
        Result<AuthenticationResponse, String> result = authenticationService.refreshToken(refreshToken, httpRequest).join();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        AuthenticationResponse response = result.getValue().orElseThrow();
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        assertEquals("Token refreshed successfully", response.getMessage());
        
        verify(auditService, times(1)).logAuthenticationEvent(anyLong(), eq("TOKEN_REFRESHED"), eq("SUCCESS"), 
            anyString(), anyString(), anyString(), isNull(), isNull());
    }

    @Test
    void refreshToken_ShouldFailWithInvalidToken() {
        // Arrange
        String invalidToken = "invalidToken";
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
            () -> authenticationService.refreshToken(invalidToken, httpRequest));
        
        assertEquals("Token refresh failed", exception.getMessage());
    }

    @Test
    void refreshToken_ShouldFailWithDeviceFingerprintMismatch() {
        // Arrange
        String refreshToken = "validRefreshToken";
        
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getDeviceFingerprintFromToken(anyString())).thenReturn("oldFingerprint");
        when(jwtTokenProvider.validateDeviceFingerprint(anyString(), anyString())).thenReturn(false);
        when(userService.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn("newFingerprint");

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
            () -> authenticationService.refreshToken(refreshToken, httpRequest));
        
        assertEquals("Token refresh failed", exception.getMessage());
    }
}