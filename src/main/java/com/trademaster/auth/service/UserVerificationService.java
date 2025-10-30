package com.trademaster.auth.service;

import com.trademaster.auth.dto.EmailVerificationResponse;
import com.trademaster.auth.entity.VerificationToken;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User Verification Service
 *
 * Handles email verification and user account activation using functional programming patterns.
 *
 * Features:
 * - Token validation with expiry checks
 * - Email verification with user service integration
 * - Functional error handling with Result types
 * - Transaction management for consistency
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserVerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserService userService;

    /**
     * Verify user email with verification token
     *
     * Functional workflow:
     * 1. Parse userId to Long
     * 2. Find valid email verification token
     * 3. Mark token as used
     * 4. Mark user email as verified
     * 5. Return verification response
     *
     * @param userId User ID as string
     * @param token Verification token string
     * @return Result with verification response or error message
     */
    @Transactional
    public Result<EmailVerificationResponse, String> verifyEmail(String userId, String token) {
        log.info("Verifying email for user: {}", userId);

        return SafeOperations.safelyToResult(() -> parseUserId(userId))
            .flatMap(uid -> findValidVerificationToken(token, uid))
            .flatMap(this::markTokenAsUsed)
            .flatMap(this::markUserEmailAsVerified)
            .flatMap(this::createVerificationResponse)
            .mapError(error -> {
                log.error("Email verification failed for user {}: {}", userId, error);
                return error;
            });
    }

    /**
     * Parse userId string to Long using functional approach
     */
    private Long parseUserId(String userId) {
        return Optional.ofNullable(userId)
            .filter(id -> !id.isBlank())
            .map(String::trim)
            .map(id -> {
                try {
                    return Long.parseLong(id);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid user ID format: " + userId);
                }
            })
            .orElseThrow(() -> new IllegalArgumentException("User ID cannot be null or empty"));
    }

    /**
     * Find valid verification token using functional composition
     */
    private Result<VerificationToken, String> findValidVerificationToken(String token, Long userId) {
        return Optional.ofNullable(token)
            .filter(t -> !t.isBlank())
            .flatMap(t -> verificationTokenRepository.findValidTokenByTokenAndType(
                t,
                VerificationToken.TokenType.EMAIL_VERIFICATION,
                LocalDateTime.now()
            ))
            .filter(vToken -> vToken.getUser().getId().equals(userId))
            .map(Result::<VerificationToken, String>success)
            .orElseGet(() -> {
                log.warn("Invalid or expired verification token for user: {}", userId);
                return Result.failure("Invalid or expired verification token");
            });
    }

    /**
     * Mark verification token as used
     */
    private Result<VerificationToken, String> markTokenAsUsed(VerificationToken verificationToken) {
        return SafeOperations.safelyToResult(() -> {
            verificationToken.setUsedAt(LocalDateTime.now());
            verificationTokenRepository.save(verificationToken);
            log.debug("Verification token marked as used: tokenId={}", verificationToken.getId());
            return verificationToken;
        });
    }

    /**
     * Mark user email as verified using UserService
     */
    private Result<VerificationToken, String> markUserEmailAsVerified(VerificationToken verificationToken) {
        return SafeOperations.safelyToResult(() -> {
            userService.verifyEmail(verificationToken.getUser().getId());
            log.info("Email verified for user: userId={}", verificationToken.getUser().getId());
            return verificationToken;
        });
    }

    /**
     * Create verification response with user email
     */
    private Result<EmailVerificationResponse, String> createVerificationResponse(VerificationToken verificationToken) {
        return SafeOperations.safelyToResult(() ->
            new EmailVerificationResponse(
                "Email verified successfully",
                verificationToken.getUser().getEmail()
            )
        );
    }
}
