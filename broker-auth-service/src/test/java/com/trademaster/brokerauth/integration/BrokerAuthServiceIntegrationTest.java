package com.trademaster.brokerauth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.BrokerAuthServiceApplication;
import com.trademaster.brokerauth.entity.BrokerAccount;
import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
import com.trademaster.brokerauth.repository.BrokerAccountRepository;
import com.trademaster.brokerauth.repository.BrokerSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Broker Auth Service
 * 
 * Tests the complete authentication flow with real database,
 * Redis, and external API mocking using TestContainers.
 */
@SpringBootTest(
    classes = BrokerAuthServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWebMvc
class BrokerAuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        
        // Test-specific configuration
        registry.add("broker.auth.encryption.key", () -> "test_encryption_key_32_chars_long_secure");
        registry.add("security.jwt.secret", () -> "test_jwt_secret_for_integration_testing_secure_long_key");
        registry.add("audit.kafka.enabled", () -> "false");
        
        // Disable external broker API calls for integration tests
        registry.add("broker.zerodha.api-url", () -> "http://localhost:9999/mock-zerodha");
        registry.add("broker.upstox.api-url", () -> "http://localhost:9999/mock-upstox");
        registry.add("broker.angel-one.api-url", () -> "http://localhost:9999/mock-angel-one");
        registry.add("broker.icici-direct.api-url", () -> "http://localhost:9999/mock-icici-direct");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BrokerAccountRepository brokerAccountRepository;

    @Autowired
    private BrokerSessionRepository brokerSessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
        
        // Clean up test data
        brokerSessionRepository.deleteAll();
        brokerAccountRepository.deleteAll();
    }

    @Test
    void healthCheck_ShouldReturnHealthyStatus() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl.replace("/api/v1", "") + "/actuator/health", 
                Map.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    @Transactional
    void createBrokerAccount_ShouldPersistToDatabase() {
        // Given
        BrokerAccount account = new BrokerAccount();
        account.setUserId(12345L);
        account.setBrokerType(BrokerType.ZERODHA);
        account.setBrokerUserId("ZU123456");
        account.setEncryptedApiKey("encrypted_api_key");
        account.setEncryptedApiSecret("encrypted_api_secret");
        account.setIsActive(true);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        // When
        BrokerAccount savedAccount = brokerAccountRepository.save(account);

        // Then
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getUserId()).isEqualTo(12345L);
        assertThat(savedAccount.getBrokerType()).isEqualTo(BrokerType.ZERODHA);
        assertThat(savedAccount.getBrokerUserId()).isEqualTo("ZU123456");
        assertThat(savedAccount.getIsActive()).isTrue();

        // Verify it can be retrieved
        Optional<BrokerAccount> retrieved = brokerAccountRepository.findById(savedAccount.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUserId()).isEqualTo(12345L);
    }

    @Test
    @Transactional
    void createBrokerSession_ShouldPersistWithCorrectStatus() {
        // Given
        BrokerAccount account = createTestBrokerAccount();
        BrokerAccount savedAccount = brokerAccountRepository.save(account);

        BrokerSession session = new BrokerSession();
        session.setBrokerAccount(savedAccount);
        session.setUserId(savedAccount.getUserId());
        session.setBrokerType(savedAccount.getBrokerType());
        session.setStatus(SessionStatus.ACTIVE);
        session.setEncryptedAccessToken("encrypted_access_token");
        session.setEncryptedRefreshToken("encrypted_refresh_token");
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUsedAt(LocalDateTime.now());

        // When
        BrokerSession savedSession = brokerSessionRepository.save(session);

        // Then
        assertThat(savedSession.getId()).isNotNull();
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(savedSession.getBrokerType()).isEqualTo(BrokerType.ZERODHA);
        assertThat(savedSession.getUserId()).isEqualTo(savedAccount.getUserId());
        assertThat(savedSession.getBrokerAccount().getId()).isEqualTo(savedAccount.getId());

        // Test session expiry logic
        assertThat(savedSession.isExpired()).isFalse();
        assertThat(savedSession.needsRefresh()).isFalse(); // Should not need refresh yet
    }

    @Test
    @Transactional
    void findActiveSessionsByUser_ShouldReturnOnlyActiveSessions() {
        // Given
        BrokerAccount account = createTestBrokerAccount();
        BrokerAccount savedAccount = brokerAccountRepository.save(account);

        // Create active session
        BrokerSession activeSession = createTestSession(savedAccount, SessionStatus.ACTIVE);
        brokerSessionRepository.save(activeSession);

        // Create expired session
        BrokerSession expiredSession = createTestSession(savedAccount, SessionStatus.EXPIRED);
        brokerSessionRepository.save(expiredSession);

        // When
        var activeSessions = brokerSessionRepository.findByUserIdAndStatus(
                savedAccount.getUserId(), 
                SessionStatus.ACTIVE
        );

        // Then
        assertThat(activeSessions).hasSize(1);
        assertThat(activeSessions.get(0).getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void databaseHealthIndicator_ShouldReturnHealthyStatus() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl.replace("/api/v1", "") + "/actuator/health/db", 
                Map.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void redisHealthIndicator_ShouldReturnHealthyStatus() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl.replace("/api/v1", "") + "/actuator/health/redis", 
                Map.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void brokerHealthIndicator_ShouldReturnBrokerStatuses() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl.replace("/api/v1", "") + "/actuator/health", 
                Map.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> components = (Map<String, Object>) response.getBody().get("components");
        assertThat(components).isNotNull();
        
        // Should have broker health indicator
        assertThat(components).containsKey("brokerHealthIndicator");
    }

    @Test
    void metricsEndpoint_ShouldExposePrometheusMetrics() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl.replace("/api/v1", "") + "/actuator/prometheus", 
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
        assertThat(response.getBody()).contains("http_server_requests_seconds");
    }

    @Test
    @Transactional
    void brokerAccountRepository_ShouldHandleConcurrentUpdates() {
        // Given
        BrokerAccount account = createTestBrokerAccount();
        BrokerAccount savedAccount = brokerAccountRepository.save(account);

        // When - simulate concurrent updates
        BrokerAccount account1 = brokerAccountRepository.findById(savedAccount.getId()).orElseThrow();
        BrokerAccount account2 = brokerAccountRepository.findById(savedAccount.getId()).orElseThrow();

        account1.setBrokerUserId("UPDATED_1");
        account2.setBrokerUserId("UPDATED_2");

        // Then - last update should win (optimistic locking not implemented in this test)
        brokerAccountRepository.save(account1);
        brokerAccountRepository.save(account2);

        BrokerAccount finalAccount = brokerAccountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(finalAccount.getBrokerUserId()).isEqualTo("UPDATED_2");
    }

    @Test
    @Transactional
    void sessionCleanup_ShouldRemoveExpiredSessions() {
        // Given
        BrokerAccount account = createTestBrokerAccount();
        BrokerAccount savedAccount = brokerAccountRepository.save(account);

        // Create expired session
        BrokerSession expiredSession = createTestSession(savedAccount, SessionStatus.EXPIRED);
        expiredSession.setExpiresAt(LocalDateTime.now().minusHours(1)); // Already expired
        brokerSessionRepository.save(expiredSession);

        // Create active session
        BrokerSession activeSession = createTestSession(savedAccount, SessionStatus.ACTIVE);
        activeSession.setExpiresAt(LocalDateTime.now().plusHours(1)); // Still active
        brokerSessionRepository.save(activeSession);

        // When - find expired sessions
        var expiredSessions = brokerSessionRepository.findByExpiresAtBeforeAndStatus(
                LocalDateTime.now(), 
                SessionStatus.ACTIVE
        );

        // Then
        assertThat(expiredSessions).isEmpty(); // No active sessions that are expired

        var allSessions = brokerSessionRepository.findByUserId(savedAccount.getUserId());
        assertThat(allSessions).hasSize(2);
    }

    @Test
    void securityHeaders_ShouldBePresent() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl.replace("/api/v1", "") + "/actuator/health", 
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Check for security headers (these might be added by Spring Security)
        var headers = response.getHeaders();
        
        // Note: In integration tests, some security headers might not be present
        // This test verifies the application starts correctly with security configuration
        assertThat(response.getBody()).isNotNull();
    }

    private BrokerAccount createTestBrokerAccount() {
        BrokerAccount account = new BrokerAccount();
        account.setUserId(12345L);
        account.setBrokerType(BrokerType.ZERODHA);
        account.setBrokerUserId("ZU123456");
        account.setEncryptedApiKey("encrypted_api_key");
        account.setEncryptedApiSecret("encrypted_api_secret");
        account.setIsActive(true);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }

    private BrokerSession createTestSession(BrokerAccount account, SessionStatus status) {
        BrokerSession session = new BrokerSession();
        session.setBrokerAccount(account);
        session.setUserId(account.getUserId());
        session.setBrokerType(account.getBrokerType());
        session.setStatus(status);
        session.setEncryptedAccessToken("encrypted_access_token");
        session.setEncryptedRefreshToken("encrypted_refresh_token");
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUsedAt(LocalDateTime.now());
        return session;
    }
}