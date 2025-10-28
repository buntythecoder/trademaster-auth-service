package com.trademaster.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Verification Token Entity
 * 
 * Represents verification tokens for:
 * - Email verification
 * - Password reset
 * - MFA backup codes
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "verification_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    /**
     * Token Types
     */
    public enum TokenType {
        EMAIL_VERIFICATION("EMAIL_VERIFICATION"),
        PASSWORD_RESET("PASSWORD_RESET"),
        MFA_BACKUP("MFA_BACKUP");

        private final String value;

        TokenType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is used
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Check if token is valid (not expired and not used)
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * Mark token as used
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Optional.ofNullable(createdAt)
            .orElse(LocalDateTime.now());
    }
}