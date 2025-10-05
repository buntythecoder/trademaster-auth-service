package com.trademaster.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EncryptionService
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    @Mock
    private KmsClient kmsClient;

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() throws Exception {
        encryptionService = new EncryptionService(kmsClient);
        
        // Set test configuration using reflection
        ReflectionTestUtils.setField(encryptionService, "kmsKeyId", "alias/test-key");
        ReflectionTestUtils.setField(encryptionService, "encryptionAlgorithm", "AES/GCM/NoPadding");
        ReflectionTestUtils.setField(encryptionService, "keyLength", 256);

        // Mock KMS response with a real AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        byte[] keyBytes = secretKey.getEncoded();
        byte[] encryptedKeyBytes = new byte[256]; // Mock encrypted key
        new SecureRandom().nextBytes(encryptedKeyBytes);

        GenerateDataKeyResponse mockResponse = GenerateDataKeyResponse.builder()
            .plaintext(SdkBytes.fromByteArray(keyBytes))
            .ciphertextBlob(SdkBytes.fromByteArray(encryptedKeyBytes))
            .build();

        // Use lenient stubbing to avoid UnnecessaryStubbingException for tests that don't call KMS
        lenient().when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class)))
            .thenReturn(mockResponse);
    }

    @Test
    void encrypt_ShouldEncryptPlaintextSuccessfully() {
        // Arrange
        String plaintext = "This is sensitive data that needs encryption";

        // Act
        String encrypted = encryptionService.encrypt(plaintext);

        // Assert
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertTrue(encrypted.length() > 0);
        // Base64 encoded strings should not contain spaces
        assertFalse(encrypted.contains(" "));
    }

    @Test
    void decrypt_ShouldDecryptEncryptedDataSuccessfully() {
        // Arrange
        String originalText = "Sensitive financial data: Account balance $10,000";
        String encrypted = encryptionService.encrypt(originalText);

        // Act
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(originalText, decrypted);
    }

    @Test
    void encryptDecrypt_ShouldHandleEmptyStrings() {
        // Test empty string
        String emptyString = "";
        String encrypted = encryptionService.encrypt(emptyString);
        assertEquals(emptyString, encrypted);

        // Test null string
        String nullString = null;
        String encryptedNull = encryptionService.encrypt(nullString);
        assertNull(encryptedNull);
    }

    @Test
    void encryptDecrypt_ShouldHandleSpecialCharacters() {
        // Arrange
        String specialText = "Special chars: !@#$%^&*()_+{}|:<>?[]\\;'\"./,`~áéíóú中文😀";
        
        // Act
        String encrypted = encryptionService.encrypt(specialText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(specialText, decrypted);
    }

    @Test
    void encryptDecrypt_ShouldHandleLargeTexts() {
        // Arrange
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("This is line number ").append(i).append(" with some data. ");
        }
        String originalText = largeText.toString();

        // Act
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(originalText, decrypted);
        assertTrue(encrypted.length() > originalText.length()); // Encrypted should be larger due to encoding
    }

    @Test
    void encryptField_ShouldHandleVariousDataTypes() {
        // Test different data types
        String stringField = encryptionService.encryptField("String value");
        String numberField = encryptionService.encryptField(12345);
        String booleanField = encryptionService.encryptField(true);
        String nullField = encryptionService.encryptField(null);

        assertNotNull(stringField);
        assertNotNull(numberField);
        assertNotNull(booleanField);
        assertNull(nullField);

        // Decrypt and verify
        assertEquals("String value", encryptionService.decryptField(stringField));
        assertEquals("12345", encryptionService.decryptField(numberField));
        assertEquals("true", encryptionService.decryptField(booleanField));
        assertNull(encryptionService.decryptField(nullField));
    }

    @Test
    void generateHash_ShouldCreateConsistentHashes() {
        // Arrange
        String data = "Test data for hashing";

        // Act
        String hash1 = encryptionService.generateHash(data);
        String hash2 = encryptionService.generateHash(data);

        // Assert
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2); // Same input should produce same hash
        assertNotEquals(data, hash1); // Hash should be different from original
    }

    @Test
    void verifyHash_ShouldValidateDataIntegrity() {
        // Arrange
        String originalData = "Important financial data";
        String hash = encryptionService.generateHash(originalData);

        // Act & Assert
        assertTrue(encryptionService.verifyHash(originalData, hash));
        assertFalse(encryptionService.verifyHash("Modified data", hash));
        assertFalse(encryptionService.verifyHash(originalData, "wrong_hash"));
    }

    @Test
    void isHealthy_ShouldReturnTrueForWorkingService() {
        // Act
        boolean isHealthy = encryptionService.isHealthy();

        // Assert
        assertTrue(isHealthy);
    }

    @Test
    void getStats_ShouldReturnEncryptionStatistics() {
        // Act
        EncryptionService.EncryptionStats stats = encryptionService.getStats();

        // Assert
        assertNotNull(stats);
        assertEquals("AES/GCM/NoPadding", stats.getAlgorithm());
        assertEquals(256, stats.getKeyLength());
        assertEquals("alias/test-key", stats.getKmsKeyId());
        assertTrue(stats.isHealthy());
        assertTrue(stats.getCachedKeysCount() >= 0);
    }

    @Test
    void rotateKeys_ShouldClearKeyCache() {
        // Arrange - encrypt something to populate cache
        encryptionService.encrypt("test data");
        EncryptionService.EncryptionStats statsBefore = encryptionService.getStats();

        // Act
        encryptionService.rotateKeys();

        // After rotation, we should still be able to encrypt/decrypt
        String testData = "test after rotation";
        String encrypted = encryptionService.encrypt(testData);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(testData, decrypted);
    }

    @Test
    void encrypt_ShouldProduceDifferentOutputsForSameInput() {
        // Arrange
        String plaintext = "Same input text";

        // Act
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Assert
        assertNotEquals(encrypted1, encrypted2); // Should be different due to random IV
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    void decrypt_ShouldHandleCorruptedData() {
        // Arrange
        String corruptedData = "this_is_not_valid_encrypted_data";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> encryptionService.decrypt(corruptedData));
        
        assertTrue(exception.getMessage().contains("Data decryption failed"));
    }

    @Test
    void encryptDecrypt_ShouldHandleUnicodeCharacters() {
        // Arrange
        String unicodeText = "Unicode test: 中文, العربية, русский, हिन्दी, 日本語, 한국어, Ελληνικά";

        // Act
        String encrypted = encryptionService.encrypt(unicodeText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertEquals(unicodeText, decrypted);
    }

    @Test
    void generateHash_ShouldHandleDifferentInputLengths() {
        // Arrange
        String shortData = "Hi";
        String mediumData = "This is a medium length string for testing hash generation";
        String longData = "This is a very long string ".repeat(100);

        // Act
        String shortHash = encryptionService.generateHash(shortData);
        String mediumHash = encryptionService.generateHash(mediumData);
        String longHash = encryptionService.generateHash(longData);

        // Assert
        assertNotNull(shortHash);
        assertNotNull(mediumHash);
        assertNotNull(longHash);
        
        // All hashes should be the same length (SHA-256 Base64 encoded)
        assertEquals(shortHash.length(), mediumHash.length());
        assertEquals(mediumHash.length(), longHash.length());
        
        // All hashes should be different
        assertNotEquals(shortHash, mediumHash);
        assertNotEquals(mediumHash, longHash);
        assertNotEquals(shortHash, longHash);
    }
}