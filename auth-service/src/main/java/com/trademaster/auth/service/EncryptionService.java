package com.trademaster.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Encryption Service for AES-256-GCM encryption with AWS KMS key management
 * 
 * Features:
 * - AES-256-GCM encryption for data at rest
 * - AWS KMS integration for key management
 * - Key rotation support
 * - Secure random IV generation
 * - Base64 encoding for storage
 * - Performance optimization with key caching
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionService {

    private final KmsClient kmsClient;
    
    @Value("${trademaster.aws.kms.key-id}")
    private String kmsKeyId;
    
    @Value("${trademaster.encryption.algorithm:AES/GCM/NoPadding}")
    private String encryptionAlgorithm;
    
    @Value("${trademaster.encryption.key-length:256}")
    private int keyLength;
    
    // Constants
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int DATA_KEY_CACHE_SIZE = 100;
    private static final long DATA_KEY_CACHE_TTL_MS = 3600000; // 1 hour
    
    // In-memory cache for data keys (in production, use a more sophisticated cache)
    private final ConcurrentMap<String, CachedDataKey> dataKeyCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypt sensitive data using AES-256-GCM with AWS KMS data key
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Get or generate data key
            byte[] dataKey = getDataKey();
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Create cipher
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            SecretKeySpec keySpec = new SecretKeySpec(dataKey, KEY_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            
            // Encrypt data
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            // Encode to Base64 for storage
            String result = Base64.getEncoder().encodeToString(encryptedWithIv);
            
            log.debug("Successfully encrypted data of length: {}", plaintext.length());
            return result;
            
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Data encryption failed", e);
        }
    }

    /**
     * Decrypt data using AES-256-GCM with AWS KMS data key
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        try {
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedData);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Get data key
            byte[] dataKey = getDataKey();
            
            // Create cipher
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            SecretKeySpec keySpec = new SecretKeySpec(dataKey, KEY_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            
            // Decrypt data
            byte[] decryptedData = cipher.doFinal(encrypted);
            
            String result = new String(decryptedData, StandardCharsets.UTF_8);
            log.debug("Successfully decrypted data");
            return result;
            
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Data decryption failed", e);
        }
    }

    /**
     * Encrypt field for database storage
     */
    public String encryptField(Object value) {
        if (value == null) {
            return null;
        }
        return encrypt(value.toString());
    }

    /**
     * Decrypt field from database storage
     */
    public String decryptField(String encryptedValue) {
        if (encryptedValue == null) {
            return null;
        }
        return decrypt(encryptedValue);
    }

    /**
     * Generate a hash for data integrity verification
     */
    public String generateHash(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Hash generation failed: {}", e.getMessage());
            throw new RuntimeException("Hash generation failed", e);
        }
    }

    /**
     * Verify data integrity using hash
     */
    public boolean verifyHash(String data, String expectedHash) {
        try {
            String actualHash = generateHash(data);
            return actualHash.equals(expectedHash);
        } catch (Exception e) {
            log.error("Hash verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get or generate AWS KMS data key with caching
     */
    private byte[] getDataKey() {
        String cacheKey = "data_key_" + kmsKeyId;
        CachedDataKey cachedKey = dataKeyCache.get(cacheKey);
        
        // Check if cached key is still valid
        if (cachedKey != null && !cachedKey.isExpired()) {
            return cachedKey.getPlaintextKey();
        }

        try {
            // Generate new data key from AWS KMS
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                .keyId(kmsKeyId)
                .keySpec(DataKeySpec.AES_256)
                .build();

            GenerateDataKeyResponse response = kmsClient.generateDataKey(request);
            
            byte[] plaintextKey = response.plaintext().asByteArray();
            byte[] encryptedKey = response.ciphertextBlob().asByteArray();
            
            // Cache the key
            CachedDataKey newCachedKey = new CachedDataKey(plaintextKey, encryptedKey);
            dataKeyCache.put(cacheKey, newCachedKey);
            
            // Clean up expired keys from cache
            cleanupExpiredKeys();
            
            log.debug("Generated new data key from AWS KMS");
            return plaintextKey;
            
        } catch (Exception e) {
            log.error("Failed to generate data key from AWS KMS: {}", e.getMessage());
            throw new RuntimeException("Data key generation failed", e);
        }
    }

    /**
     * Rotate encryption keys (invalidate cache to force new key generation)
     */
    public void rotateKeys() {
        dataKeyCache.clear();
        log.info("Encryption keys rotated - cache cleared");
    }

    /**
     * Clean up expired keys from cache
     */
    private void cleanupExpiredKeys() {
        if (dataKeyCache.size() > DATA_KEY_CACHE_SIZE) {
            dataKeyCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    /**
     * Health check for encryption service
     */
    public boolean isHealthy() {
        try {
            // Test encryption/decryption
            String testData = "health_check_" + System.currentTimeMillis();
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            
            boolean healthy = testData.equals(decrypted);
            log.debug("Encryption service health check: {}", healthy ? "PASSED" : "FAILED");
            return healthy;
            
        } catch (Exception e) {
            log.error("Encryption service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get encryption statistics
     */
    public EncryptionStats getStats() {
        return EncryptionStats.builder()
            .algorithm(encryptionAlgorithm)
            .keyLength(keyLength)
            .cachedKeysCount(dataKeyCache.size())
            .kmsKeyId(kmsKeyId)
            .isHealthy(isHealthy())
            .build();
    }

    /**
     * Cached data key wrapper
     */
    private static class CachedDataKey {
        private final byte[] plaintextKey;
        private final byte[] encryptedKey;
        private final long createdAt;

        public CachedDataKey(byte[] plaintextKey, byte[] encryptedKey) {
            this.plaintextKey = plaintextKey.clone();
            this.encryptedKey = encryptedKey.clone();
            this.createdAt = System.currentTimeMillis();
        }

        public byte[] getPlaintextKey() {
            return plaintextKey.clone();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > DATA_KEY_CACHE_TTL_MS;
        }
    }

    /**
     * Encryption statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EncryptionStats {
        private String algorithm;
        private int keyLength;
        private int cachedKeysCount;
        private String kmsKeyId;
        private boolean isHealthy;
    }
}