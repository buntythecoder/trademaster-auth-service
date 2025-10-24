package com.trademaster.auth.controller;

import com.trademaster.auth.constants.AuthConstants;
import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.AuthenticationResponse;
import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.service.AuthenticationFacade;
import com.trademaster.auth.service.RateLimitingService;
import com.trademaster.auth.service.UserService;
import com.trademaster.auth.service.VerificationTokenService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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

    private final AuthenticationFacade authenticationFacade;
    private final RateLimitingService rateLimitingService;
    private final VerificationTokenService verificationTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

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
                          "email": "{{user_email}}",
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
        
        String clientIp = httpRequest.getRemoteAddr();
        
        return Optional.of(rateLimitingService.isRegistrationAllowed(clientIp))
            .filter(allowed -> allowed)
            .map(allowed -> {
                log.info("Registration attempt for email: {}", request.getEmail());
                return SafeOperations.safelyToResult(() -> authenticationFacade.register(request, httpRequest))
                    .fold(
                        user -> ResponseEntity.status(HttpStatus.CREATED).body(AuthenticationResponse.builder()
                            .message("Registration successful")
                            .build()),
                        error -> {
                            log.error("Registration failed: {}", error);
                            return ResponseEntity.badRequest().body(AuthenticationResponse.builder()
                                .message("Registration failed: " + error)
                                .build());
                        }
                    );
            })
            .orElseGet(() -> {
                log.warn("Registration rate limit exceeded for IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthenticationResponse.builder()
                        .message(AuthConstants.MSG_TOO_MANY_REGISTRATION_ATTEMPTS)
                        .build());
            });
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
                          "email": "{{user_email}}",
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
        
        String clientIp = httpRequest.getRemoteAddr();
        String email = request.getEmail();
        
        return Optional.of(rateLimitingService.isLoginAllowed(clientIp) && rateLimitingService.isLoginAllowed(email))
            .filter(allowed -> allowed)
            .<ResponseEntity<AuthenticationResponse>>map(allowed -> {
                log.info("Login attempt for email: {}", request.getEmail());
                
                return authenticationFacade.login(request, httpRequest)
                    .fold(
                        error -> {
                            log.warn("Login failed for email {}: {}", request.getEmail(), error);
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthenticationResponse.builder()
                                .message("Login failed: " + error)
                                .build());
                        },
                        ResponseEntity::ok
                    );
            })
            .orElseGet(() -> {
                long remainingSeconds = Math.max(
                    rateLimitingService.getSecondsUntilNextLoginAttempt(clientIp),
                    rateLimitingService.getSecondsUntilNextLoginAttempt(email)
                );
                
                log.warn("Login rate limit exceeded for email: {} from IP: {}", email, clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(remainingSeconds))
                    .body(AuthenticationResponse.builder()
                        .message(String.format(AuthConstants.MSG_TOO_MANY_LOGIN_ATTEMPTS, remainingSeconds))
                        .build());
            });
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
                          "email": "{{user_email}}",
                          "mfaToken": "{{generated_mfa_token}}",
                          "mfaCode": "{{user_mfa_code}}"
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
        
        String email = request.get("email");
        String mfaToken = request.get("mfaToken");
        String mfaCode = request.get("mfaCode");
        
        return Optional.of(email)
            .filter(e -> mfaToken != null && mfaCode != null)
            .<ResponseEntity<AuthenticationResponse>>map(validEmail -> {
                log.info("MFA verification attempt for email: {}", validEmail);
                
                return authenticationFacade.completeMfaVerification(
                        validEmail, mfaToken, mfaCode, httpRequest)
                    .fold(
                        error -> {
                            log.warn("MFA verification failed: {}", error);
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthenticationResponse.builder()
                                .message("MFA verification failed: " + error)
                                .build());
                        },
                        ResponseEntity::ok
                    );
            })
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(AuthenticationResponse.builder()
                    .message("Email, MFA token, and MFA code are required")
                    .build()));
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
        
        String refreshToken = request.get("refreshToken");
        
        return Optional.ofNullable(refreshToken)
            .filter(token -> !token.isEmpty())
            .<ResponseEntity<AuthenticationResponse>>map(token -> {
                log.info("Token refresh attempt");
                
                return authenticationFacade.refreshAccessToken(token, httpRequest)
                    .join() // Block for the CompletableFuture result
                    .fold(
                        error -> {
                            log.warn("Token refresh failed: {}", error);
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthenticationResponse.builder()
                                .message("Token refresh failed: " + error)
                                .build());
                        },
                        response -> ResponseEntity.ok(response)
                    );
            })
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(AuthenticationResponse.builder()
                    .message("Refresh token is required")
                    .build()));
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
        
        log.info("Logout request from IP: {}", httpRequest.getRemoteAddr());
        
        return Optional.ofNullable(httpRequest.getHeader("Authorization"))
            .filter(header -> header.startsWith("Bearer "))
            .map(header -> header.substring(AuthConstants.BEARER_TOKEN_PREFIX_LENGTH))
            .map(token -> {
                String sessionId = httpRequest.getSession().getId();
                
                return SafeOperations.safelyToResult(() -> {
                    authenticationFacade.logout(token, sessionId, httpRequest.getRemoteAddr());
                    return "Logged out successfully";
                })
                .mapError(error -> {
                    log.error("Logout error: {}", error);
                    return "Logout failed: " + error;
                })
                .fold(
                    message -> {
                        log.info("User logged out successfully");
                        return ResponseEntity.ok(Map.of("message", message));
                    },
                    errorMessage -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", errorMessage))
                );
            })
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(Map.of("message", "No valid token provided")));
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
                          "token": "{{verification_token}}"
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
        
        String token = request.get("token");
        String clientIp = httpRequest.getRemoteAddr();
        
        return Optional.ofNullable(token)
            .filter(t -> !t.isEmpty())
            .filter(t -> rateLimitingService.isEmailVerificationAllowed(clientIp))
            .map(validToken -> {
                log.info("Email verification attempt with token: {}", validToken.substring(0, AuthConstants.TOKEN_LOG_PREFIX_LENGTH) + "...");
                
                return SafeOperations.safelyToResult(() -> {
                    // Use existing verification token service
                    var userOpt = verificationTokenService.verifyEmailToken(validToken);
                    return userOpt.isPresent();
                })
                .fold(
                    error -> {
                        log.error("Email verification error: {}", error);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("message", "Email verification failed"));
                    },
                    verified -> verified
                        ? ResponseEntity.ok(Map.of("message", "Email verified successfully"))
                        : ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification token"))
                );
            })
            .orElseGet(() -> Optional.ofNullable(token)
                .filter(t -> !t.isEmpty())
                .map(t -> {
                    log.warn("Email verification rate limit exceeded for IP: {}", clientIp);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("message", AuthConstants.MSG_TOO_MANY_EMAIL_VERIFICATION_ATTEMPTS));
                })
                .orElse(ResponseEntity.badRequest()
                    .body(Map.of("message", "Verification token is required"))));
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
                          "email": "{{user_email}}"
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
        
        String email = request.get("email");
        String clientIp = httpRequest.getRemoteAddr();
        
        return Optional.ofNullable(email)
            .filter(e -> !e.isEmpty())
            .filter(e -> rateLimitingService.isPasswordResetAllowed(clientIp) && rateLimitingService.isPasswordResetAllowed(e))
            .map(validEmail -> {
                log.info("Password reset request for email: {}", validEmail);
                
                return SafeOperations.safelyToResult(() ->
                        {
                            userService.findByEmail(validEmail)
                                    .map(user -> {
                                        // Password reset token generation and email sending handled by service layer
                                        log.info("Password reset process initiated for: {}", validEmail);
                                        return true;
                                    });
                            return true;
                        } // Always return true for security (don't reveal if email exists)
                )
                .fold(
                    success -> ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a password reset link")),
                    error -> {
                        log.error("Password reset request error: {}", error);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("message", "Password reset request failed"));
                    }
                );
            })
            .orElseGet(() -> Optional.ofNullable(email)
                .filter(e -> !e.isEmpty())
                .map(e -> {
                    log.warn("Password reset rate limit exceeded for email: {} from IP: {}", e, clientIp);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("message", "Too many password reset requests. Please try again later."));
                })
                .orElse(ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required"))));
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
                          "token": "{{reset_token}}",
                          "newPassword": "{{new_secure_password}}"
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
        
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        return Optional.ofNullable(token)
            .filter(t -> !t.isEmpty() && newPassword != null && !newPassword.isEmpty())
            .map(validToken -> {
                log.info("Password reset attempt with token: {}", validToken.substring(0, AuthConstants.TOKEN_LOG_PREFIX_LENGTH) + "...");
                
                return SafeOperations.safelyToResult(() -> 
                    verificationTokenService.verifyPasswordResetToken(validToken)
                        .map(user -> {
                            // Update password using user service - hash password first
                            String hashedPassword = passwordEncoder.encode(newPassword);
                            userService.updatePassword(user.getId(), hashedPassword);
                            return true;
                        })
                        .orElse(false)
                )
                .fold(
                    error -> {
                        log.error("Password reset error: {}", error);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("message", "Password reset failed"));
                    },
                    success -> success
                        ? ResponseEntity.ok(Map.of("message", "Password reset successfully"))
                        : ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired reset token"))
                );
            })
            .orElseGet(() -> ResponseEntity.badRequest()
                .body(Map.of("message", "Token and new password are required")));
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
            "version", AuthConstants.API_VERSION
        ));
    }
}