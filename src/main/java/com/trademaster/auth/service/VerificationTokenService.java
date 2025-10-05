package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.entity.VerificationToken;
import com.trademaster.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing verification tokens
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${trademaster.verification.email.expiration-hours:24}")
    private int emailVerificationExpirationHours;

    @Value("${trademaster.verification.password-reset.expiration-hours:1}")
    private int passwordResetExpirationHours;

    /**
     * Generate and save email verification token
     */
    @Transactional(readOnly = false)
    public String generateEmailVerificationToken(User user, String ipAddress, String userAgent) {
        // Remove any existing email verification tokens for this user
        verificationTokenRepository.deleteByUserIdAndTokenType(
            user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION);

        // Generate new token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(emailVerificationExpirationHours);

        VerificationToken verificationToken = VerificationToken.builder()
            .user(user)
            .token(token)
            .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
            .expiresAt(expiresAt)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        verificationTokenRepository.save(verificationToken);
        
        log.info("Generated email verification token for user: {} expires at: {}", 
                user.getId(), expiresAt);
        
        return token;
    }

    /**
     * Generate and save password reset token
     */
    @Transactional(readOnly = false)
    public String generatePasswordResetToken(User user, String ipAddress, String userAgent) {
        // Remove any existing password reset tokens for this user
        verificationTokenRepository.deleteByUserIdAndTokenType(
            user.getId(), VerificationToken.TokenType.PASSWORD_RESET);

        // Generate new token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(passwordResetExpirationHours);

        VerificationToken verificationToken = VerificationToken.builder()
            .user(user)
            .token(token)
            .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
            .expiresAt(expiresAt)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        verificationTokenRepository.save(verificationToken);
        
        log.info("Generated password reset token for user: {} expires at: {}", 
                user.getId(), expiresAt);
        
        return token;
    }

    /**
     * Verify email verification token
     */
    @Transactional(readOnly = false)
    public Optional<User> verifyEmailToken(String token) {
        Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository
            .findValidTokenByTokenAndType(token, VerificationToken.TokenType.EMAIL_VERIFICATION, LocalDateTime.now());

        return verificationTokenOpt
            .map(verificationToken -> {
                User user = verificationToken.getUser();
                
                verificationToken.markAsUsed();
                verificationTokenRepository.save(verificationToken);
                
                user = user.withEmailVerified(true);
                log.info("Email verified successfully for user: {}", user.getId());
                return user;
            })
            .or(() -> {
                log.warn("Invalid or expired email verification token: {}", token);
                return Optional.empty();
            });
    }

    /**
     * Verify password reset token
     */
    @Transactional(readOnly = true)
    public Optional<User> verifyPasswordResetToken(String token) {
        Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository
            .findValidTokenByTokenAndType(token, VerificationToken.TokenType.PASSWORD_RESET, LocalDateTime.now());

        return verificationTokenOpt
            .map(verificationToken -> {
                log.info("Valid password reset token found for user: {}", verificationToken.getUser().getId());
                return verificationToken.getUser();
            })
            .or(() -> {
                log.warn("Invalid or expired password reset token: {}", token);
                return Optional.empty();
            });
    }

    /**
     * Mark password reset token as used
     */
    @Transactional(readOnly = false)
    public void markPasswordResetTokenAsUsed(String token) {
        verificationTokenRepository.findByToken(token)
            .ifPresent(verificationToken -> {
                verificationToken.markAsUsed();
                verificationTokenRepository.save(verificationToken);

                log.info("Password reset token marked as used for user: {}",
                        verificationToken.getUser().getId());
            });
    }

    /**
     * Check if user has valid email verification token
     */
    public boolean hasValidEmailVerificationToken(Long userId) {
        return verificationTokenRepository.hasValidTokenForUser(
            userId, VerificationToken.TokenType.EMAIL_VERIFICATION, LocalDateTime.now());
    }

    /**
     * Clean up expired tokens
     */
    @Transactional(readOnly = false)
    public int cleanupExpiredTokens() {
        int deletedCount = verificationTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up {} expired verification tokens", deletedCount);
        return deletedCount;
    }

    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32]; // 32 bytes = 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}