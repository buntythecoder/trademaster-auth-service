package com.trademaster.auth.repository;

import com.trademaster.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for VerificationToken entities
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Find token by token string
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Find valid token by token string and type
     */
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.token = :token " +
           "AND vt.tokenType = :tokenType AND vt.usedAt IS NULL AND vt.expiresAt > :now")
    Optional<VerificationToken> findValidTokenByTokenAndType(
        @Param("token") String token,
        @Param("tokenType") VerificationToken.TokenType tokenType,
        @Param("now") LocalDateTime now
    );

    /**
     * Find all tokens for a user by type
     */
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.user.id = :userId " +
           "AND vt.tokenType = :tokenType ORDER BY vt.createdAt DESC")
    java.util.List<VerificationToken> findByUserIdAndTokenType(
        @Param("userId") Long userId,
        @Param("tokenType") VerificationToken.TokenType tokenType
    );

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete all tokens for a user of a specific type
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.id = :userId AND vt.tokenType = :tokenType")
    int deleteByUserIdAndTokenType(
        @Param("userId") Long userId,
        @Param("tokenType") VerificationToken.TokenType tokenType
    );

    /**
     * Check if user has any valid tokens of a specific type
     */
    @Query("SELECT COUNT(vt) > 0 FROM VerificationToken vt WHERE vt.user.id = :userId " +
           "AND vt.tokenType = :tokenType AND vt.usedAt IS NULL AND vt.expiresAt > :now")
    boolean hasValidTokenForUser(
        @Param("userId") Long userId,
        @Param("tokenType") VerificationToken.TokenType tokenType,
        @Param("now") LocalDateTime now
    );
}