package com.trademaster.brokerauth.integration;

import com.trademaster.brokerauth.BrokerAuthServiceApplication;
import com.trademaster.brokerauth.service.VaultSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for HashiCorp Vault functionality
 * 
 * Tests the complete Vault integration including:
 * - Secret storage and retrieval
 * - Authentication and authorization
 * - Caching functionality
 * - Security enforcement
 * - Error handling and resilience
 */
@SpringBootTest(
    classes = BrokerAuthServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
class VaultIntegrationTest {

    @Container
    static GenericContainer<?> vault = new GenericContainer<>(DockerImageName.parse("hashicorp/vault:1.15.2"))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "test-root-token")
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withCommand("vault", "server", "-dev", "-dev-root-token-id=test-root-token")
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        vault.start();
        
        String vaultUrl = "http://" + vault.getHost() + ":" + vault.getMappedPort(8200);
        
        registry.add("vault.uri", () -> vaultUrl);
        registry.add("vault.token", () -> "test-root-token");
        registry.add("vault.enabled", () -> "true");
        registry.add("vault.secret-path", () -> "secret/broker-auth-test");
        registry.add("vault.cache.enabled", () -> "true");
        registry.add("vault.cache.ttl-minutes", () -> "1");
        
        // Disable vault enforcement for testing
        registry.add("vault.enforce-secrets", () -> "false");
        
        // Spring Cloud Vault configuration
        registry.add("spring.cloud.vault.enabled", () -> "true");
        registry.add("spring.cloud.vault.uri", () -> vaultUrl);
        registry.add("spring.cloud.vault.token", () -> "test-root-token");
        registry.add("spring.cloud.vault.fail-fast", () -> "false");
    }

    @Autowired
    private VaultSecretService vaultSecretService;

    @Autowired
    private VaultTemplate vaultTemplate;

    @BeforeEach
    void setUp() {
        // Clean up any existing test secrets
        try {
            vaultTemplate.delete("secret/broker-auth-test");
            vaultSecretService.clearCache();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void vaultConnection_ShouldBeHealthy() {
        // Given & When & Then
        assertThat(vaultTemplate).isNotNull();
        
        // Test Vault connectivity
        assertThatNoException().isThrownBy(() -> {
            vaultTemplate.opsForSys().health();
        });
    }

    @Test
    void storeAndRetrieveSecret_ShouldWorkCorrectly() {
        // Given
        String secretKey = "test-encryption-key";
        String secretValue = "secure_test_key_32_characters_long";

        // When
        vaultSecretService.storeSecret(secretKey, secretValue);
        Optional<String> retrievedSecret = vaultSecretService.getSecret(secretKey);

        // Then
        assertThat(retrievedSecret).isPresent();
        assertThat(retrievedSecret.get()).isEqualTo(secretValue);
    }

    @Test
    void getNonExistentSecret_ShouldReturnEmpty() {
        // Given
        String nonExistentKey = "non-existent-secret";

        // When
        Optional<String> secret = vaultSecretService.getSecret(nonExistentKey);

        // Then
        assertThat(secret).isEmpty();
    }

    @Test
    void storeMultipleSecrets_ShouldPreserveExistingSecrets() {
        // Given
        String key1 = "secret1";
        String value1 = "value1";
        String key2 = "secret2";  
        String value2 = "value2";

        // When
        vaultSecretService.storeSecret(key1, value1);
        vaultSecretService.storeSecret(key2, value2);

        // Then
        assertThat(vaultSecretService.getSecret(key1)).contains(value1);
        assertThat(vaultSecretService.getSecret(key2)).contains(value2);
    }

    @Test
    void deleteSecret_ShouldRemoveSecretFromVault() {
        // Given
        String secretKey = "secret-to-delete";
        String secretValue = "value-to-delete";
        vaultSecretService.storeSecret(secretKey, secretValue);

        // Verify secret exists
        assertThat(vaultSecretService.getSecret(secretKey)).contains(secretValue);

        // When
        vaultSecretService.deleteSecret(secretKey);

        // Then
        assertThat(vaultSecretService.getSecret(secretKey)).isEmpty();
    }

    @Test
    void caching_ShouldWorkCorrectly() {
        // Given
        String secretKey = "cached-secret";
        String secretValue = "cached-value";
        vaultSecretService.storeSecret(secretKey, secretValue);

        // When - First retrieval (should hit Vault)
        Optional<String> firstRetrieval = vaultSecretService.getSecret(secretKey);
        
        // When - Second retrieval (should hit cache)
        Optional<String> secondRetrieval = vaultSecretService.getSecret(secretKey);

        // Then
        assertThat(firstRetrieval).contains(secretValue);
        assertThat(secondRetrieval).contains(secretValue);
        
        // Verify cache statistics
        Map<String, Object> cacheStats = vaultSecretService.getCacheStats();
        assertThat(cacheStats.get("cacheEnabled")).isEqualTo(true);
        assertThat((Integer) cacheStats.get("cachedSecrets")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void clearCache_ShouldRemoveAllCachedSecrets() {
        // Given
        String secretKey = "cache-test-secret";
        String secretValue = "cache-test-value";
        vaultSecretService.storeSecret(secretKey, secretValue);
        
        // Cache the secret
        vaultSecretService.getSecret(secretKey);
        
        // Verify cache has entries
        Map<String, Object> statsBefore = vaultSecretService.getCacheStats();
        assertThat((Integer) statsBefore.get("cachedSecrets")).isGreaterThanOrEqualTo(1);

        // When
        vaultSecretService.clearCache();

        // Then
        Map<String, Object> statsAfter = vaultSecretService.getCacheStats();
        assertThat((Integer) statsAfter.get("cachedSecrets")).isEqualTo(0);
    }

    @Test
    void getBrokerCredentials_ShouldReturnCorrectCredentials() {
        // Given
        String brokerType = "zerodha";
        String apiKey = "test_zerodha_api_key";
        String apiSecret = "test_zerodha_api_secret";
        
        vaultSecretService.storeSecret(brokerType + "-api-key", apiKey);
        vaultSecretService.storeSecret(brokerType + "-api-secret", apiSecret);

        // When
        Map<String, String> credentials = vaultSecretService.getBrokerCredentials(brokerType);

        // Then
        assertThat(credentials).containsEntry("apiKey", apiKey);
        assertThat(credentials).containsEntry("apiSecret", apiSecret);
    }

    @Test
    void getBrokerCredentials_WithMissingCredentials_ShouldReturnPartialMap() {
        // Given
        String brokerType = "upstox";
        String apiKey = "test_upstox_api_key";
        // Note: Not storing api-secret
        
        vaultSecretService.storeSecret(brokerType + "-api-key", apiKey);

        // When
        Map<String, String> credentials = vaultSecretService.getBrokerCredentials(brokerType);

        // Then
        assertThat(credentials).containsEntry("apiKey", apiKey);
        assertThat(credentials).doesNotContainKey("apiSecret");
    }

    @Test
    void getEncryptionKey_WithStoredKey_ShouldReturnKey() {
        // Given
        String encryptionKey = "secure_encryption_key_for_production_use_64_chars_long_test";
        vaultSecretService.storeSecret("encryption-key", encryptionKey);

        // When
        String retrievedKey = vaultSecretService.getEncryptionKey();

        // Then
        assertThat(retrievedKey).isEqualTo(encryptionKey);
    }

    @Test
    void getEncryptionKey_WithoutStoredKey_ShouldThrowException() {
        // Given - No encryption key stored

        // When & Then
        assertThatThrownBy(() -> vaultSecretService.getEncryptionKey())
            .hasMessageContaining("Encryption key not found in Vault");
    }

    @Test
    void getJwtSecret_WithStoredSecret_ShouldReturnSecret() {
        // Given
        String jwtSecret = "secure_jwt_secret_for_production_use_64_chars_long_test_key";
        vaultSecretService.storeSecret("jwt-secret", jwtSecret);

        // When
        String retrievedSecret = vaultSecretService.getJwtSecret();

        // Then
        assertThat(retrievedSecret).isEqualTo(jwtSecret);
    }

    @Test
    void getJwtSecret_WithoutStoredSecret_ShouldThrowException() {
        // Given - No JWT secret stored

        // When & Then
        assertThatThrownBy(() -> vaultSecretService.getJwtSecret())
            .hasMessageContaining("JWT secret not found in Vault");
    }

    @Test
    void getDatabasePassword_WithStoredPassword_ShouldReturnPassword() {
        // Given
        String dbPassword = "secure_database_password_123";
        vaultSecretService.storeSecret("database-password", dbPassword);

        // When
        Optional<String> retrievedPassword = vaultSecretService.getDatabasePassword();

        // Then
        assertThat(retrievedPassword).contains(dbPassword);
    }

    @Test
    void getDatabasePassword_WithoutStoredPassword_ShouldReturnEmpty() {
        // Given - No database password stored

        // When
        Optional<String> retrievedPassword = vaultSecretService.getDatabasePassword();

        // Then
        assertThat(retrievedPassword).isEmpty();
    }

    @Test
    void getRedisPassword_WithStoredPassword_ShouldReturnPassword() {
        // Given
        String redisPassword = "secure_redis_password_123";
        vaultSecretService.storeSecret("redis-password", redisPassword);

        // When
        Optional<String> retrievedPassword = vaultSecretService.getRedisPassword();

        // Then
        assertThat(retrievedPassword).contains(redisPassword);
    }

    @Test
    void secretOperations_ShouldHandleSpecialCharacters() {
        // Given
        String secretKey = "special-char-secret";
        String secretValue = "Special@#$%^&*()_+{}|:<>?[]\\;',./`~Value!";

        // When
        vaultSecretService.storeSecret(secretKey, secretValue);
        Optional<String> retrievedSecret = vaultSecretService.getSecret(secretKey);

        // Then
        assertThat(retrievedSecret).contains(secretValue);
    }

    @Test
    void secretOperations_ShouldHandleLargeValues() {
        // Given
        String secretKey = "large-secret";
        String secretValue = "x".repeat(10000); // 10KB secret

        // When
        vaultSecretService.storeSecret(secretKey, secretValue);
        Optional<String> retrievedSecret = vaultSecretService.getSecret(secretKey);

        // Then
        assertThat(retrievedSecret).contains(secretValue);
        assertThat(retrievedSecret.get()).hasSize(10000);
    }

    @Test
    void cacheExpiry_ShouldWorkCorrectly() throws InterruptedException {
        // Given
        String secretKey = "expiry-test-secret";
        String secretValue = "expiry-test-value";
        vaultSecretService.storeSecret(secretKey, secretValue);

        // When - Retrieve secret to cache it
        vaultSecretService.getSecret(secretKey);
        
        // Verify it's cached
        Map<String, Object> statsInitial = vaultSecretService.getCacheStats();
        assertThat((Integer) statsInitial.get("cachedSecrets")).isGreaterThanOrEqualTo(1);

        // Wait for cache to expire (TTL is set to 1 minute for tests)
        Thread.sleep(61000); // Wait 61 seconds

        // Then - Check expired secrets count
        Map<String, Object> statsAfterExpiry = vaultSecretService.getCacheStats();
        assertThat((Long) statsAfterExpiry.get("expiredSecrets")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void concurrentSecretOperations_ShouldBeThreadSafe() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        int operationsPerThread = 5;
        Thread[] threads = new Thread[numberOfThreads];

        // When - Run concurrent operations
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread-" + threadIndex + "-secret-" + j;
                    String value = "thread-" + threadIndex + "-value-" + j;
                    
                    // Store secret
                    vaultSecretService.storeSecret(key, value);
                    
                    // Retrieve secret
                    Optional<String> retrieved = vaultSecretService.getSecret(key);
                    assertThat(retrieved).contains(value);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - Verify all secrets are stored correctly
        for (int i = 0; i < numberOfThreads; i++) {
            for (int j = 0; j < operationsPerThread; j++) {
                String key = "thread-" + i + "-secret-" + j;
                String expectedValue = "thread-" + i + "-value-" + j;
                
                Optional<String> retrieved = vaultSecretService.getSecret(key);
                assertThat(retrieved).contains(expectedValue);
            }
        }
    }
}