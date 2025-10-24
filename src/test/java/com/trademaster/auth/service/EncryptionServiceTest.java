package com.trademaster.auth.service;

import com.trademaster.auth.pattern.Result;
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
        Result<String, String> result = encryptionService.encrypt(plaintext);

        // Assert
        assertTrue(result.isSuccess());
        String encrypted = result.getValue().orElseThrow();
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
        Result<String, String> encryptResult = encryptionService.encrypt(originalText);
        assertTrue(encryptResult.isSuccess());
        String encrypted = encryptResult.getValue().orElseThrow();

        // Act
        Result<String, String> decryptResult = encryptionService.decrypt(encrypted);

        // Assert
        assertTrue(decryptResult.isSuccess());
        assertEquals(originalText, decryptResult.getValue().orElseThrow());
    }

    @Test
    void encryptDecrypt_ShouldHandleEmptyStrings() {
        // Test empty string
        String emptyString = "";
        Result<String, String> result = encryptionService.encrypt(emptyString);
        assertTrue(result.isSuccess());
        assertEquals(emptyString, result.getValue().orElseThrow());

        // Test null string
        String nullString = null;
        Result<String, String> nullResult = encryptionService.encrypt(nullString);
        assertTrue(nullResult.isFailure());
    }

    @Test
    void encryptDecrypt_ShouldHandleSpecialCharacters() {
        // Arrange
        String specialText = "Special chars: !@#$%^&*()_+{}|:<>?[]\\;'\"./,`~áéíóú中文😀";

        // Act
        Result<String, String> encryptResult = encryptionService.encrypt(specialText);
        assertTrue(encryptResult.isSuccess());
        Result<String, String> decryptResult = encryptionService.decrypt(encryptResult.getValue().orElseThrow());

        // Assert
        assertTrue(decryptResult.isSuccess());
        assertEquals(specialText, decryptResult.getValue().orElseThrow());
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
        Result<String, String> encryptResult = encryptionService.encrypt(originalText);
        assertTrue(encryptResult.isSuccess());
        Result<String, String> decryptResult = encryptionService.decrypt(encryptResult.getValue().orElseThrow());

        // Assert
        assertTrue(decryptResult.isSuccess());
        assertEquals(originalText, decryptResult.getValue().orElseThrow());
        assertTrue(encryptResult.getValue().orElseThrow().length() > originalText.length()); // Encrypted should be larger due to encoding
    }

    @Test
    void encryptField_ShouldHandleVariousDataTypes() {
        // Test different data types
        String stringResult = encryptionService.encryptField("String value");
        String numberResult = encryptionService.encryptField(12345);
        String booleanResult = encryptionService.encryptField(true);
        String nullResult = encryptionService.encryptField(null);

        assertNotNull(stringResult);
        assertNotNull(numberResult);
        assertNotNull(booleanResult);
        assertNull(nullResult);

        // Decrypt and verify
        String decryptString = encryptionService.decryptField(stringResult);
        String decryptNumber = encryptionService.decryptField(numberResult);
        String decryptBoolean = encryptionService.decryptField(booleanResult);

        assertNotNull(decryptString);
        assertEquals("String value", decryptString);
        assertNotNull(decryptNumber);
        assertEquals("12345", decryptNumber);
        assertNotNull(decryptBoolean);
        assertEquals("true", decryptBoolean);
    }

    @Test
    void generateHash_ShouldCreateConsistentHashes() {
        // Arrange
        String data = "Test data for hashing";

        // Act
        String hashResult1 = encryptionService.generateHash(data);
        String hashResult2 = encryptionService.generateHash(data);

        // Assert
        assertNotNull(hashResult1);
        assertNotNull(hashResult2);
        assertEquals(hashResult1, hashResult2); // Same input should produce same hash
        assertNotEquals(data, hashResult1); // Hash should be different from original
    }

    @Test
    void verifyHash_ShouldValidateDataIntegrity() {
        // Arrange
        String originalData = "Important financial data";
        String hash = encryptionService.generateHash(originalData);
        assertNotNull(hash);

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
        Result<String, String> encryptResult = encryptionService.encrypt(testData);
        assertTrue(encryptResult.isSuccess());
        String encrypted = encryptResult.getValue().orElseThrow();

        Result<String, String> decryptResult = encryptionService.decrypt(encrypted);
        assertTrue(decryptResult.isSuccess());
        String decrypted = decryptResult.getValue().orElseThrow();

        // Assert
        assertEquals(testData, decrypted);
    }

    @Test
    void encrypt_ShouldProduceDifferentOutputsForSameInput() {
        // Arrange
        String plaintext = "Same input text";

        // Act
        Result<String, String> encryptResult1 = encryptionService.encrypt(plaintext);
        Result<String, String> encryptResult2 = encryptionService.encrypt(plaintext);

        assertTrue(encryptResult1.isSuccess());
        assertTrue(encryptResult2.isSuccess());

        String encrypted1 = encryptResult1.getValue().orElseThrow();
        String encrypted2 = encryptResult2.getValue().orElseThrow();

        // Assert
        assertNotEquals(encrypted1, encrypted2); // Should be different due to random IV

        Result<String, String> decryptResult1 = encryptionService.decrypt(encrypted1);
        Result<String, String> decryptResult2 = encryptionService.decrypt(encrypted2);

        assertTrue(decryptResult1.isSuccess());
        assertTrue(decryptResult2.isSuccess());

        assertEquals(plaintext, decryptResult1.getValue().orElseThrow());
        assertEquals(plaintext, decryptResult2.getValue().orElseThrow());
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
        Result<String, String> encryptResult = encryptionService.encrypt(unicodeText);
        assertTrue(encryptResult.isSuccess());
        String encrypted = encryptResult.getValue().orElseThrow();

        Result<String, String> decryptResult = encryptionService.decrypt(encrypted);
        assertTrue(decryptResult.isSuccess());
        String decrypted = decryptResult.getValue().orElseThrow();

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