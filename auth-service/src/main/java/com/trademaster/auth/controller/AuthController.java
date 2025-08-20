package com.trademaster.auth.controller;

import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.exception.RateLimitExceededException;
import com.trademaster.auth.service.AuthenticationService;
import com.trademaster.auth.service.RateLimitingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication REST Controller
 * 
 * Provides authentication endpoints for:
 * - User registration
 * - User login
 * - Token refresh
 * - MFA verification
 * - Logout
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://trademaster.com"})
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RateLimitingService rateLimitingService;

    /**
     * Register new user
     */
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with email verification. Requires strong password and valid email address.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User registration details",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RegistrationRequest.class),
                examples = @ExampleObject(
                    name = "Registration Example",
                    value = """
                        {
                          "email": "user@example.com",
                          "password": "StrongPassword123!",
                          "firstName": "John",
                          "lastName": "Doe",
                          "countryCode": "US",
                          "riskTolerance": "MODERATE",
                          "tradingExperience": "INTERMEDIATE",
                          "agreeToTerms": true,
                          "agreeToPrivacyPolicy": true
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid registration data or email already exists",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "User with this email already exists"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many registration attempts",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Too many registration attempts. Please try again later."
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Registration failed due to server error"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Check rate limiting
            String clientIp = httpRequest.getRemoteAddr();
            if (!rateLimitingService.isRegistrationAllowed(clientIp)) {
                log.warn("Registration rate limit exceeded for IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthenticationResponse.builder()
                        .message("Too many registration attempts. Please try again later.")
                        .build());
            }
            
            log.info("Registration attempt for email: {}", request.getEmail());
            
            AuthenticationResponse response = authenticationService.register(request, httpRequest);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(AuthenticationResponse.builder()
                    .message("Registration failed: " + e.getMessage())
                    .build());
                    
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthenticationResponse.builder()
                    .message("Registration failed due to server error")
                    .build());
        }
    }

    /**
     * User login
     */
    @Operation(
        summary = "Authenticate user login",
        description = "Authenticates user credentials and returns JWT tokens. May require MFA verification.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User login credentials",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationRequest.class),
                examples = @ExampleObject(
                    name = "Login Example",
                    value = """
                        {
                          "email": "user@example.com",
                          "password": "StrongPassword123!"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "202",
            description = "MFA verification required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Login failed: Invalid credentials"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many login attempts",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Too many login attempts. Please try again in 60 seconds."
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Check rate limiting based on both IP and email
            String clientIp = httpRequest.getRemoteAddr();
            String email = request.getEmail();
            
            if (!rateLimitingService.isLoginAllowed(clientIp) || !rateLimitingService.isLoginAllowed(email)) {
                long remainingSeconds = Math.max(
                    rateLimitingService.getSecondsUntilNextLoginAttempt(clientIp),
                    rateLimitingService.getSecondsUntilNextLoginAttempt(email)
                );
                
                log.warn("Login rate limit exceeded for email: {} from IP: {}", email, clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(remainingSeconds))
                    .body(AuthenticationResponse.builder()
                        .message(String.format("Too many login attempts. Please try again in %d seconds.", remainingSeconds))
                        .build());
            }
            
            log.info("Login attempt for email: {}", request.getEmail());
            
            AuthenticationResponse response = authenticationService.login(request, httpRequest);
            
            if (response.isRequiresMfa()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.warn("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthenticationResponse.builder()
                    .message("Login failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Complete MFA verification
     */
    @Operation(
        summary = "Complete MFA verification",
        description = "Completes the multi-factor authentication process using the MFA token and verification code.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "MFA verification details",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "MFA Verification Example",
                    value = """
                        {
                          "email": "user@example.com",
                          "mfaToken": "mfa_temp_token_12345",
                          "mfaCode": "123456"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "MFA verification successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Missing required MFA parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Email, MFA token, and MFA code are required"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "MFA verification failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "MFA verification failed: Invalid code or expired token"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthenticationResponse> verifyMfa(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String email = request.get("email");
            String mfaToken = request.get("mfaToken");
            String mfaCode = request.get("mfaCode");
            
            if (email == null || mfaToken == null || mfaCode == null) {
                return ResponseEntity.badRequest()
                    .body(AuthenticationResponse.builder()
                        .message("Email, MFA token, and MFA code are required")
                        .build());
            }
            
            log.info("MFA verification attempt for email: {}", email);
            
            AuthenticationResponse response = authenticationService.completeMfaVerification(
                email, mfaToken, mfaCode, httpRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.warn("MFA verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthenticationResponse.builder()
                    .message("MFA verification failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Refresh JWT token
     */
    @Operation(
        summary = "Refresh JWT access token",
        description = "Refreshes an expired JWT access token using a valid refresh token.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Token refresh details",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Token Refresh Example",
                    value = """
                        {
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Missing or empty refresh token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Refresh token is required"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Token refresh failed: Invalid or expired refresh token"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(AuthenticationResponse.builder()
                        .message("Refresh token is required")
                        .build());
            }
            
            log.info("Token refresh attempt");
            
            AuthenticationResponse response = authenticationService.refreshToken(refreshToken, httpRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthenticationResponse.builder()
                    .message("Token refresh failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * User logout
     */
    @Operation(
        summary = "User logout",
        description = "Logs out the authenticated user by invalidating their session and JWT token.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Logged out successfully"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "No valid authentication token provided",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "No valid token provided"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Logout failed due to server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Logout failed: Internal server error"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Logout request from IP: {}", httpRequest.getRemoteAddr());
            
            // Extract JWT token from Authorization header
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Get user ID from token for session management
                String sessionId = httpRequest.getSession().getId();
                
                // Invalidate the session
                authenticationService.logout(token, sessionId, httpRequest.getRemoteAddr());
                
                log.info("User logged out successfully");
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "No valid token provided"));
            }
            
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Logout failed: " + e.getMessage()));
        }
    }

    /**
     * Verify email address
     */
    @Operation(
        summary = "Verify email address",
        description = "Verifies a user's email address using the token sent during registration.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email verification token",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Email Verification Example",
                    value = """
                        {
                          "token": "email_verification_token_12345"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email verified successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Email verified successfully"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid or expired verification token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Invalid or expired verification token"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many email verification attempts",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Too many email verification attempts. Please try again later."
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String token = request.get("token");
            
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Verification token is required"));
            }
            
            // Check rate limiting for email verification
            String clientIp = httpRequest.getRemoteAddr();
            if (!rateLimitingService.isEmailVerificationAllowed(clientIp)) {
                log.warn("Email verification rate limit exceeded for IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many email verification attempts. Please try again later."));
            }
            
            log.info("Email verification attempt with token: {}", token.substring(0, 8) + "...");
            
            boolean verified = authenticationService.verifyEmail(token);
            
            if (verified) {
                return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired verification token"));
            }
            
        } catch (Exception e) {
            log.error("Email verification error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Email verification failed: " + e.getMessage()));
        }
    }

    /**
     * Request password reset
     */
    @Operation(
        summary = "Request password reset",
        description = "Initiates a password reset process by sending a reset link to the user's email address.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User email address",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Password Reset Request Example",
                    value = """
                        {
                          "email": "user@example.com"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset email sent (if email exists)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "If your email is registered, you will receive a password reset link"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Email address is required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Email is required"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many password reset requests",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Too many password reset requests. Please try again later."
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Password reset request failed due to server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Password reset request failed"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String email = request.get("email");
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required"));
            }
            
            // Check rate limiting for password reset requests
            String clientIp = httpRequest.getRemoteAddr();
            if (!rateLimitingService.isPasswordResetAllowed(clientIp) || !rateLimitingService.isPasswordResetAllowed(email)) {
                log.warn("Password reset rate limit exceeded for email: {} from IP: {}", email, clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many password reset requests. Please try again later."));
            }
            
            log.info("Password reset request for email: {}", email);
            
            boolean sent = authenticationService.requestPasswordReset(email, 
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            
            // Always return success message for security (don't reveal if email exists)
            return ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a password reset link"));
            
        } catch (Exception e) {
            log.error("Password reset request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Password reset request failed"));
        }
    }

    /**
     * Reset password with token
     */
    @Operation(
        summary = "Reset password with token",
        description = "Resets the user's password using a valid password reset token and new password.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Password reset details",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Password Reset Example",
                    value = """
                        {
                          "token": "password_reset_token_12345",
                          "newPassword": "NewStrongPassword123!"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "Password reset successfully"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or expired reset token",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Missing Parameters",
                        value = """
                            {
                              "message": "Token and new password are required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid Token",
                        value = """
                            {
                              "message": "Invalid or expired reset token"
                            }
                            """
                    )
                }
            )
        )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Token and new password are required"));
            }
            
            log.info("Password reset attempt with token: {}", token.substring(0, 8) + "...");
            
            boolean reset = authenticationService.resetPassword(token, newPassword, 
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            
            if (reset) {
                return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired reset token"));
            }
            
        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Password reset failed: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @Operation(
        summary = "Service health check",
        description = "Returns the current health status of the TradeMaster Authentication Service.",
        tags = {"Health"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy and operational",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "UP",
                          "service": "TradeMaster Auth Service",
                          "version": "1.0.0"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "TradeMaster Auth Service",
            "version", "1.0.0"
        ));
    }
}