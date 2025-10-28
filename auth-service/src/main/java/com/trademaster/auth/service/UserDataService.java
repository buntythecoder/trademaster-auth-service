package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.projection.UserStatisticsProjection;
import com.trademaster.auth.repository.UserRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Data Service - SOLID Single Responsibility Principle
 *
 * Responsibilities:
 * - Core CRUD operations for User entity
 * - Spring Security UserDetailsService implementation
 * - User query operations and statistics
 * - Data access layer for user management
 *
 * This service is 100% functional programming compliant:
 * - No if-else statements (uses Optional, pattern matching)
 * - No try-catch blocks (uses Result types and SafeOperations)
 * - No for/while loops (uses Stream API)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserDataService implements UserDetailsService {

    private final UserRepository userRepository;

    // Security constants
    private static final int PASSWORD_EXPIRY_DAYS = 90;
    private static final int INACTIVE_USER_DAYS = 365;

    // ========== Spring Security Integration ==========

    /**
     * Load user by username (email) for Spring Security
     *
     * @param email User email address
     * @return UserDetails for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        log.debug("Loaded user: {} with roles: {}", user.getEmail(), user.getAuthorities());
        return user;
    }

    /**
     * Load user by ID (for JWT token validation)
     *
     * @param userId User identifier
     * @return UserDetails or null if not found
     */
    public UserDetails loadUserById(Long userId) {
        return userRepository.findById(userId)
                .orElse(null);
    }

    // ========== Core CRUD Operations ==========

    /**
     * Find user by email (case-insensitive)
     *
     * @param email User email address
     * @return Optional containing user if found
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    /**
     * Find user by ID
     *
     * @param userId User identifier
     * @return Optional containing user if found
     */
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Check if user exists by email
     *
     * @param email Email address to check
     * @return true if user exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Save user entity
     *
     * @param user User to save
     * @return Saved user
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Save user entity with Result wrapper
     *
     * @param user User to save
     * @return Result containing saved user or error
     */
    @Transactional
    public Result<User, String> saveUser(User user) {
        return SafeOperations.safelyToResult(() -> userRepository.save(user));
    }

    // ========== User Query Operations ==========

    /**
     * Find users requiring password change
     *
     * @return List of users with expired passwords
     */
    public List<User> findUsersRequiringPasswordChange() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(PASSWORD_EXPIRY_DAYS);
        return userRepository.findUsersRequiringPasswordChange(thresholdDate);
    }

    /**
     * Find inactive users
     *
     * @return List of inactive users
     */
    public List<User> findInactiveUsers() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(INACTIVE_USER_DAYS);
        return userRepository.findInactiveUsers(thresholdDate);
    }

    /**
     * Find users to unlock (locked period expired)
     *
     * @return List of users eligible for unlocking
     */
    public List<User> findUsersToUnlock() {
        return userRepository.findUsersToUnlock(LocalDateTime.now());
    }

    // ========== User Statistics ==========

    /**
     * Get user statistics using functional approach
     *
     * @return UserStatistics object with aggregated data
     */
    public UserStatistics getUserStatistics() {
        Result<UserStatistics, String> result = SafeOperations.safelyToResult(() -> {
            return Optional.ofNullable(userRepository.getUserStatistics())
                .map(this::convertToUserStatistics)
                .orElse(new UserStatistics());
        });

        return result
            .mapError(error -> {
                log.error("Error retrieving user statistics: {}", error);
                return error;
            })
            .orElse(new UserStatistics());
    }

    /**
     * Convert projection to DTO using functional mapping
     */
    private UserStatistics convertToUserStatistics(UserStatisticsProjection projection) {
        return UserStatistics.builder()
            .totalUsers(projection.getTotalUsers())
            .activeUsers(projection.getActiveUsers())
            .lockedUsers(projection.getLockedUsers())
            .suspendedUsers(projection.getSuspendedUsers())
            .verifiedUsers(projection.getVerifiedUsers())
            .recentLogins(projection.getRecentLogins())
            .build();
    }

    // ========== Data Transfer Objects ==========

    /**
     * User Statistics DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long lockedUsers;
        private long suspendedUsers;
        private long verifiedUsers;
        private long recentLogins;
    }
}
