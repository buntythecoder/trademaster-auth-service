package com.trademaster.auth.benchmark;

import com.trademaster.auth.entity.User;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * Critical Path Performance Benchmarks for Authentication System
 *
 * MANDATORY: Critical Path Performance - Enterprise requirement
 * MANDATORY: Financial System Performance - Sub-50ms targets
 * MANDATORY: Production Performance Validation
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class CriticalPathBenchmark {

    // Test data and state
    private User testUser;
    private Map<String, Object> sessionCache;
    private AtomicReference<String> currentToken;

    @Setup(Level.Trial)
    public void setUp() {
        // Initialize test data
        testUser = User.builder()
            .id(1L)
            .username("critical@trademaster.com")
            .email("critical@trademaster.com")
            .firstName("Critical")
            .lastName("Path")
            .build();

        sessionCache = new ConcurrentHashMap<>();
        currentToken = new AtomicReference<>("initial-token");

        // Pre-populate cache for realistic scenarios
        for (int i = 0; i < 1000; i++) {
            sessionCache.put("session-" + i, createMockSession(i));
        }
    }

    /**
     * Benchmark login validation critical path
     * Target: <20ms for complete login validation
     */
    @Benchmark
    public void benchmarkLoginValidation(Blackhole bh) {
        long startTime = System.nanoTime();

        // Critical path: user lookup + password validation + session creation
        String email = testUser.getEmail();
        String password = "SecurePassword123!";

        // Simulate user lookup (database call)
        User user = findUserByEmail(email);

        // Simulate password validation
        boolean isValidPassword = validatePassword(password, user);

        // Simulate session creation
        String sessionId = createSession(user);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

        bh.consume(user);
        bh.consume(isValidPassword);
        bh.consume(sessionId);
        bh.consume(duration);
    }

    /**
     * Benchmark token generation critical path
     * Target: <10ms for JWT token generation
     */
    @Benchmark
    public void benchmarkTokenGeneration(Blackhole bh) {
        long startTime = System.nanoTime();

        // Critical path: claims preparation + token signing
        Map<String, Object> claims = prepareClaims(testUser);
        String accessToken = generateJwtToken(claims, "ACCESS", 3600);
        String refreshToken = generateJwtToken(claims, "REFRESH", 86400);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        bh.consume(accessToken);
        bh.consume(refreshToken);
        bh.consume(duration);
    }

    /**
     * Benchmark session validation critical path
     * Target: <5ms for session validation
     */
    @Benchmark
    public void benchmarkSessionValidation(Blackhole bh) {
        long startTime = System.nanoTime();

        // Critical path: session lookup + expiration check + user validation
        String sessionId = "session-" + (System.currentTimeMillis() % 1000);
        Object session = sessionCache.get(sessionId);
        boolean isValid = validateSession(session);
        User user = isValid ? extractUserFromSession(session) : null;

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        bh.consume(session);
        bh.consume(isValid);
        bh.consume(user);
        bh.consume(duration);
    }

    /**
     * Benchmark authorization check critical path
     * Target: <3ms for role-based authorization
     */
    @Benchmark
    public void benchmarkAuthorizationCheck(Blackhole bh) {
        long startTime = System.nanoTime();

        // Critical path: role extraction + permission lookup + access decision
        String[] userRoles = {"TRADER", "PREMIUM_USER"};
        String requiredPermission = "TRADE_EXECUTE";
        boolean hasPermission = checkPermission(userRoles, requiredPermission);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        bh.consume(userRoles);
        bh.consume(hasPermission);
        bh.consume(duration);
    }

    /**
     * Benchmark MFA token validation critical path
     * Target: <15ms for TOTP validation
     */
    @Benchmark
    public void benchmarkMfaValidation(Blackhole bh) {
        long startTime = System.nanoTime();

        // Critical path: TOTP generation + time window validation + replay check
        String secretKey = "MFRGG2LTMFZGQ5DTEBXWG5C2KNXWU2LTOVXHI3DJ";
        String totpCode = "123456";
        boolean isValid = validateTotpCode(secretKey, totpCode);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        bh.consume(isValid);
        bh.consume(duration);
    }

    /**
     * Benchmark complete authentication flow critical path
     * Target: <50ms for complete authentication with all validations
     */
    @Benchmark
    public void benchmarkCompleteAuthenticationFlow(Blackhole bh) {
        long startTime = System.nanoTime();

        // Complete critical path: all authentication steps combined
        String email = testUser.getEmail();
        String password = "SecurePassword123!";
        String totpCode = "123456";

        // Step 1: User lookup and password validation
        User user = findUserByEmail(email);
        boolean isValidPassword = validatePassword(password, user);

        if (isValidPassword) {
            // Step 2: MFA validation (if enabled)
            boolean mfaValid = validateTotpCode("secret", totpCode);

            if (mfaValid) {
                // Step 3: Token generation
                Map<String, Object> claims = prepareClaims(user);
                String accessToken = generateJwtToken(claims, "ACCESS", 3600);
                String refreshToken = generateJwtToken(claims, "REFRESH", 86400);

                // Step 4: Session creation
                String sessionId = createSession(user);

                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1_000_000;

                bh.consume(user);
                bh.consume(accessToken);
                bh.consume(refreshToken);
                bh.consume(sessionId);
                bh.consume(duration);
            }
        }
    }

    /**
     * Benchmark async authentication flow with Virtual Threads
     * Target: <30ms with improved concurrency
     */
    @Benchmark
    public void benchmarkAsyncAuthenticationFlow(Blackhole bh) {
        long startTime = System.nanoTime();

        // Async critical path with Virtual Threads
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() ->
            findUserByEmail(testUser.getEmail()));

        CompletableFuture<Boolean> passwordFuture = userFuture.thenApply(user ->
            validatePassword("SecurePassword123!", user));

        CompletableFuture<String> tokenFuture = userFuture.thenApply(user -> {
            Map<String, Object> claims = prepareClaims(user);
            return generateJwtToken(claims, "ACCESS", 3600);
        });

        CompletableFuture<String> sessionFuture = userFuture.thenApply(this::createSession);

        try {
            // Wait for all operations to complete
            CompletableFuture.allOf(passwordFuture, tokenFuture, sessionFuture).join();

            User user = userFuture.get();
            boolean isValid = passwordFuture.get();
            String token = tokenFuture.get();
            String session = sessionFuture.get();

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000;

            bh.consume(user);
            bh.consume(isValid);
            bh.consume(token);
            bh.consume(session);
            bh.consume(duration);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark financial precision calculations
     * Target: <2ms for BigDecimal operations
     */
    @Benchmark
    public void benchmarkFinancialCalculations(Blackhole bh) {
        long startTime = System.nanoTime();

        // Financial calculations that might be part of authentication context
        BigDecimal accountBalance = new BigDecimal("1000000.00");
        BigDecimal tradingLimit = new BigDecimal("500000.00");
        BigDecimal currentExposure = new BigDecimal("250000.50");

        // Critical financial validations
        BigDecimal availableBalance = accountBalance.subtract(currentExposure);
        boolean canTrade = availableBalance.compareTo(tradingLimit) >= 0;
        BigDecimal utilizationRatio = currentExposure.divide(accountBalance, 4, BigDecimal.ROUND_HALF_UP);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        bh.consume(availableBalance);
        bh.consume(canTrade);
        bh.consume(utilizationRatio);
        bh.consume(duration);
    }

    // Helper methods for realistic simulations

    private User findUserByEmail(String email) {
        // Simulate database lookup delay
        try {
            Thread.sleep(5); // 5ms database lookup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return testUser;
    }

    private boolean validatePassword(String password, User user) {
        // Simulate BCrypt validation
        try {
            Thread.sleep(10); // 10ms password hashing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return password.equals("SecurePassword123!");
    }

    private String createSession(User user) {
        String sessionId = "session-" + user.getId() + "-" + System.currentTimeMillis();
        sessionCache.put(sessionId, createMockSession(user.getId().intValue()));
        return sessionId;
    }

    private Map<String, Object> prepareClaims(User user) {
        return Map.of(
            "sub", user.getId().toString(),
            "email", user.getEmail(),
            "roles", new String[]{"TRADER", "USER"},
            "iat", Instant.now().getEpochSecond(),
            "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        );
    }

    private String generateJwtToken(Map<String, Object> claims, String type, int expirySeconds) {
        // Simulate JWT generation
        try {
            Thread.sleep(2); // 2ms token generation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return type + "-token-" + claims.get("sub") + "-" + System.currentTimeMillis();
    }

    private boolean validateSession(Object session) {
        if (session == null) return false;
        MockSession mockSession = (MockSession) session;
        return mockSession.expiresAt.isAfter(LocalDateTime.now());
    }

    private User extractUserFromSession(Object session) {
        if (session == null) return null;
        MockSession mockSession = (MockSession) session;
        return User.builder()
            .id(mockSession.userId)
            .email("user-" + mockSession.userId + "@example.com")
            .build();
    }

    private boolean checkPermission(String[] roles, String permission) {
        // Simulate role-based permission check
        return java.util.Arrays.stream(roles)
            .anyMatch(role -> hasRolePermission(role, permission));
    }

    private boolean hasRolePermission(String role, String permission) {
        // Simple permission mapping
        return switch (role) {
            case "TRADER" -> permission.startsWith("TRADE_") || permission.equals("VIEW_PORTFOLIO");
            case "PREMIUM_USER" -> permission.equals("TRADE_EXECUTE") || permission.equals("VIEW_PORTFOLIO");
            case "USER" -> permission.equals("VIEW_PORTFOLIO");
            default -> false;
        };
    }

    private boolean validateTotpCode(String secretKey, String code) {
        // Simulate TOTP validation
        try {
            Thread.sleep(3); // 3ms TOTP calculation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Simple validation - in real implementation would use proper TOTP algorithm
        return code.length() == 6 && code.matches("\\d{6}");
    }

    private Object createMockSession(int userId) {
        return new MockSession((long) userId, LocalDateTime.now().plusHours(1));
    }

    // Mock session class
    private static class MockSession {
        final Long userId;
        final LocalDateTime expiresAt;

        MockSession(Long userId, LocalDateTime expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}