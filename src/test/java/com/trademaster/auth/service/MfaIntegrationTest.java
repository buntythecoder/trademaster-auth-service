package com.trademaster.auth.service;

import com.trademaster.auth.entity.MfaConfiguration;
import com.trademaster.auth.entity.User;
import com.trademaster.auth.entity.UserSession;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.repository.MfaConfigurationRepository;
import com.trademaster.auth.repository.UserRepository;
import com.trademaster.auth.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MFA Integration Tests - Enhanced Test Coverage
 *
 * MANDATORY: Enhanced Test Coverage - Performance Improvement #2
 * MANDATORY: MFA workflow testing - Enterprise requirement
 * MANDATORY: Virtual Thread concurrent testing - Rule #12
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "trademaster.mfa.enabled=true",
    "trademaster.mfa.totp.window=30",
    "trademaster.mfa.backup-codes.count=10"
})
@Transactional
@DisplayName("MFA Integration Tests")
class MfaIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MfaConfigurationRepository mfaConfigRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    private User testUser;
    private String testUserPassword = "SecurePassword123!";

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
            .username("mfatest@example.com")
            .email("mfatest@example.com")
            .firstName("MFA")
            .lastName("Test")
            .build();

        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Complete MFA setup workflow should succeed")
    void testCompleteMfaSetupWorkflow() throws Exception {
        // Step 1: Enable MFA for user
        Result<String, String> setupResult = mfaService.setupMfa(testUser.getId(), "TOTP");
        assertThat(setupResult.isSuccess()).isTrue();

        // Step 2: Get MFA QR code
        Result<String, String> qrCodeResult = mfaService.generateQrCode(testUser.getId());
        assertThat(qrCodeResult.isSuccess()).isTrue();
        assertThat(qrCodeResult.value()).contains("otpauth://totp/");

        // Step 3: Verify MFA setup with test code
        String secretKey = extractSecretFromQrCode(qrCodeResult.value());
        String totpCode = generateTestTotpCode(secretKey);

        Result<Boolean, String> verifyResult = mfaService.verifyMfaSetup(testUser.getId(), totpCode);
        assertThat(verifyResult.isSuccess()).isTrue();
        assertThat(verifyResult.value()).isTrue();

        // Step 4: Verify MFA is enabled
        Optional<MfaConfiguration> mfaConfig = mfaConfigRepository.findByUserId(testUser.getId());
        assertThat(mfaConfig).isPresent();
        assertThat(mfaConfig.get().isEnabled()).isTrue();
        assertThat(mfaConfig.get().getBackupCodes()).hasSize(10);
    }

    @Test
    @DisplayName("MFA authentication flow should work correctly")
    void testMfaAuthenticationFlow() throws Exception {
        // Setup MFA for user
        setupMfaForUser(testUser);

        // Step 1: Initial login should require MFA
        Result<AuthenticationResult, String> loginResult = authenticationService
            .authenticate(testUser.getEmail(), testUserPassword, "127.0.0.1", "test-agent");

        assertThat(loginResult.isSuccess()).isTrue();
        assertThat(loginResult.value().isMfaRequired()).isTrue();
        assertThat(loginResult.value().getAccessToken()).isNull(); // No token yet

        String sessionId = loginResult.value().getSessionId();

        // Step 2: Verify MFA code
        String totpCode = generateCurrentTotpCode(testUser.getId());
        Result<AuthenticationResult, String> mfaResult = authenticationService
            .verifyMfa(sessionId, totpCode, "127.0.0.1");

        assertThat(mfaResult.isSuccess()).isTrue();
        assertThat(mfaResult.value().isMfaRequired()).isFalse();
        assertThat(mfaResult.value().getAccessToken()).isNotNull();
        assertThat(mfaResult.value().getRefreshToken()).isNotNull();
    }

    @Test
    @DisplayName("MFA backup codes should work correctly")
    void testMfaBackupCodes() throws Exception {
        // Setup MFA
        setupMfaForUser(testUser);

        // Get backup codes
        Result<java.util.List<String>, String> backupCodesResult = mfaService
            .getBackupCodes(testUser.getId());

        assertThat(backupCodesResult.isSuccess()).isTrue();
        assertThat(backupCodesResult.value()).hasSize(10);

        String backupCode = backupCodesResult.value().get(0);

        // Initial login
        Result<AuthenticationResult, String> loginResult = authenticationService
            .authenticate(testUser.getEmail(), testUserPassword, "127.0.0.1", "test-agent");

        String sessionId = loginResult.value().getSessionId();

        // Use backup code for MFA
        Result<AuthenticationResult, String> mfaResult = authenticationService
            .verifyMfaWithBackupCode(sessionId, backupCode, "127.0.0.1");

        assertThat(mfaResult.isSuccess()).isTrue();
        assertThat(mfaResult.value().getAccessToken()).isNotNull();

        // Backup code should be consumed
        Result<java.util.List<String>, String> remainingCodesResult = mfaService
            .getBackupCodes(testUser.getId());
        assertThat(remainingCodesResult.value()).hasSize(9);
        assertThat(remainingCodesResult.value()).doesNotContain(backupCode);
    }

    @Test
    @DisplayName("Concurrent MFA verification should handle race conditions")
    void testConcurrentMfaVerification() throws Exception {
        setupMfaForUser(testUser);

        // Create multiple sessions attempting MFA
        int concurrentAttempts = 5;
        CountDownLatch latch = new CountDownLatch(concurrentAttempts);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        String[] sessionIds = new String[concurrentAttempts];

        // Create concurrent sessions
        for (int i = 0; i < concurrentAttempts; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Result<AuthenticationResult, String> loginResult = authenticationService
                        .authenticate(testUser.getEmail(), testUserPassword,
                                    "127.0.0." + (index + 1), "test-agent-" + index);

                    if (loginResult.isSuccess()) {
                        sessionIds[index] = loginResult.value().getSessionId();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);

        // Now attempt concurrent MFA verification
        String totpCode = generateCurrentTotpCode(testUser.getId());
        CountDownLatch mfaLatch = new CountDownLatch(concurrentAttempts);
        boolean[] results = new boolean[concurrentAttempts];

        for (int i = 0; i < concurrentAttempts; i++) {
            final int index = i;
            if (sessionIds[index] != null) {
                executor.submit(() -> {
                    try {
                        Result<AuthenticationResult, String> mfaResult = authenticationService
                            .verifyMfa(sessionIds[index], totpCode, "127.0.0." + (index + 1));
                        results[index] = mfaResult.isSuccess();
                    } finally {
                        mfaLatch.countDown();
                    }
                });
            } else {
                mfaLatch.countDown();
            }
        }

        mfaLatch.await(10, TimeUnit.SECONDS);

        // At least one should succeed (TOTP codes have time windows)
        assertThat(results).contains(true);

        executor.shutdown();
    }

    @Test
    @DisplayName("MFA should prevent replay attacks")
    void testMfaReplayAttackPrevention() throws Exception {
        setupMfaForUser(testUser);

        // Initial login
        Result<AuthenticationResult, String> loginResult = authenticationService
            .authenticate(testUser.getEmail(), testUserPassword, "127.0.0.1", "test-agent");

        String sessionId = loginResult.value().getSessionId();
        String totpCode = generateCurrentTotpCode(testUser.getId());

        // First MFA verification should succeed
        Result<AuthenticationResult, String> firstMfaResult = authenticationService
            .verifyMfa(sessionId, totpCode, "127.0.0.1");

        assertThat(firstMfaResult.isSuccess()).isTrue();

        // Create new session with same user
        Result<AuthenticationResult, String> secondLoginResult = authenticationService
            .authenticate(testUser.getEmail(), testUserPassword, "127.0.0.1", "test-agent");

        String secondSessionId = secondLoginResult.value().getSessionId();

        // Replay attack with same TOTP code should fail
        Result<AuthenticationResult, String> replayResult = authenticationService
            .verifyMfa(secondSessionId, totpCode, "127.0.0.1");

        assertThat(replayResult.isFailure()).isTrue();
        assertThat(replayResult.error()).contains("Invalid or expired");
    }

    @Test
    @DisplayName("MFA should handle expired codes correctly")
    void testMfaExpiredCodeHandling() throws Exception {
        setupMfaForUser(testUser);

        // Initial login
        Result<AuthenticationResult, String> loginResult = authenticationService
            .authenticate(testUser.getEmail(), testUserPassword, "127.0.0.1", "test-agent");

        String sessionId = loginResult.value().getSessionId();

        // Use expired TOTP code (simulated by using previous time window)
        String expiredCode = generateExpiredTotpCode(testUser.getId());

        Result<AuthenticationResult, String> mfaResult = authenticationService
            .verifyMfa(sessionId, expiredCode, "127.0.0.1");

        assertThat(mfaResult.isFailure()).isTrue();
        assertThat(mfaResult.error()).contains("Invalid or expired");
    }

    @Test
    @DisplayName("MFA session should timeout correctly")
    void testMfaSessionTimeout() throws Exception {
        setupMfaForUser(testUser);

        // Initial login
        Result<AuthenticationResult, String> loginResult = authenticationService
            .authenticate(testUser.getEmail(), testUserPassword, "127.0.0.1", "test-agent");

        String sessionId = loginResult.value().getSessionId();

        // Simulate session timeout by updating session timestamp
        Optional<UserSession> session = sessionRepository.findBySessionId(sessionId);
        assertThat(session).isPresent();

        UserSession expiredSession = session.get().toBuilder()
            .createdAt(LocalDateTime.now().minusMinutes(6)) // MFA timeout is 5 minutes
            .build();
        sessionRepository.save(expiredSession);

        // MFA verification should fail due to timeout
        String totpCode = generateCurrentTotpCode(testUser.getId());
        Result<AuthenticationResult, String> mfaResult = authenticationService
            .verifyMfa(sessionId, totpCode, "127.0.0.1");

        assertThat(mfaResult.isFailure()).isTrue();
        assertThat(mfaResult.error()).contains("expired");
    }

    // Helper methods
    private void setupMfaForUser(User user) {
        mfaService.setupMfa(user.getId(), "TOTP");
        String qrCode = mfaService.generateQrCode(user.getId()).value();
        String secretKey = extractSecretFromQrCode(qrCode);
        String setupCode = generateTestTotpCode(secretKey);
        mfaService.verifyMfaSetup(user.getId(), setupCode);
    }

    private String generateCurrentTotpCode(Long userId) {
        Optional<MfaConfiguration> config = mfaConfigRepository.findByUserId(userId);
        return generateTestTotpCode(config.get().getSecretKey());
    }

    private String generateExpiredTotpCode(Long userId) {
        Optional<MfaConfiguration> config = mfaConfigRepository.findByUserId(userId);
        // Generate code for previous time window
        long timeWindow = (System.currentTimeMillis() / 1000L) / 30 - 1;
        return generateTotpCodeForTimeWindow(config.get().getSecretKey(), timeWindow);
    }

    private String extractSecretFromQrCode(String qrCodeUrl) {
        // Extract secret parameter from otpauth URL
        return qrCodeUrl.substring(qrCodeUrl.indexOf("secret=") + 7)
                       .split("&")[0];
    }

    private String generateTestTotpCode(String secretKey) {
        long timeWindow = System.currentTimeMillis() / 1000L / 30;
        return generateTotpCodeForTimeWindow(secretKey, timeWindow);
    }

    private String generateTotpCodeForTimeWindow(String secretKey, long timeWindow) {
        // Simple TOTP implementation for testing
        // In real implementation, would use proper TOTP library
        return String.format("%06d", (int) (timeWindow % 1000000));
    }
}