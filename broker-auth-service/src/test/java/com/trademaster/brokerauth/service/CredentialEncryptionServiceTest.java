package com.trademaster.brokerauth.service;

import com.trademaster.brokerauth.exception.CredentialManagementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CredentialEncryptionService
 * 
 * Tests encryption/decryption functionality, key management,
 * and error handling for credential security.
 */
@ExtendWith(MockitoExtension.class)
class CredentialEncryptionServiceTest {

    private CredentialEncryptionService encryptionService;
    private static final String TEST_MASTER_KEY = "test-master-key-32-chars-long!!!";
    private static final String TEST_PLAINTEXT = "sensitive-credential-data";

    @BeforeEach
    void setUp() {
        encryptionService = new CredentialEncryptionService();
        ReflectionTestUtils.setField(encryptionService, "masterKey", TEST_MASTER_KEY);
        encryptionService.init();
    }

    @Test
    void encrypt_ShouldEncryptPlaintext_WhenValidInput() {
        // Given
        String plaintext = TEST_PLAINTEXT;

        // When
        String encrypted = encryptionService.encrypt(plaintext);

        // Then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encrypted).contains(":"); // Should contain IV separator
    }

    @Test
    void decrypt_ShouldDecryptToOriginalPlaintext_WhenValidEncryptedData() {
        // Given
        String plaintext = TEST_PLAINTEXT;
        String encrypted = encryptionService.encrypt(plaintext);

        // When
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptDecrypt_ShouldBeSymmetric_ForMultipleValues() {
        // Given
        String[] testValues = {
            "api-key-12345",
            "secret-token-abcdef",
            "password-with-special-chars!@#$%",
            "very-long-credential-string-with-multiple-segments-and-numbers-123456789",
            "short",
            ""
        };

        for (String value : testValues) {
            // When
            String encrypted = encryptionService.encrypt(value);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted).isEqualTo(value);
            assertThat(encrypted).isNotEqualTo(value);
        }
    }

    @Test
    void encrypt_ShouldGenerateDifferentCiphertext_ForSameInput() {
        // Given
        String plaintext = TEST_PLAINTEXT;

        // When
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // Different IVs should produce different ciphertext
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_ShouldThrowException_WhenPlaintextIsNull() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.encrypt(null))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Plaintext cannot be null or empty");
    }

    @Test
    void encrypt_ShouldThrowException_WhenPlaintextIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.encrypt(""))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Plaintext cannot be null or empty");
    }

    @Test
    void decrypt_ShouldThrowException_WhenCiphertextIsNull() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(null))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Ciphertext cannot be null or empty");
    }

    @Test
    void decrypt_ShouldThrowException_WhenCiphertextIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(""))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Ciphertext cannot be null or empty");
    }

    @Test
    void decrypt_ShouldThrowException_WhenCiphertextIsInvalidFormat() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt("invalid-ciphertext-format"))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Invalid ciphertext format");
    }

    @Test
    void decrypt_ShouldThrowException_WhenCiphertextIsMalformed() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt("invalid:base64:format"))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    @Test
    void decrypt_ShouldThrowException_WhenCiphertextHasInvalidIV() {
        // Given
        String invalidCiphertext = "aW52YWxpZC1pdg==:dmFsaWQtY2lwaGVydGV4dA=="; // Invalid IV

        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(invalidCiphertext))
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    @Test
    void init_ShouldSetupEncryption_WhenValidMasterKey() {
        // Given
        CredentialEncryptionService newService = new CredentialEncryptionService();
        ReflectionTestUtils.setField(newService, "masterKey", "another-32-char-master-key-here!");

        // When & Then
        assertThatCode(() -> newService.init()).doesNotThrowAnyException();
        
        // Verify it works
        String plaintext = "test-data";
        String encrypted = newService.encrypt(plaintext);
        String decrypted = newService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void init_ShouldThrowException_WhenMasterKeyIsNull() {
        // Given
        CredentialEncryptionService newService = new CredentialEncryptionService();
        ReflectionTestUtils.setField(newService, "masterKey", null);

        // When & Then
        assertThatThrownBy(() -> newService.init())
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Master key not configured");
    }

    @Test
    void init_ShouldThrowException_WhenMasterKeyIsTooShort() {
        // Given
        CredentialEncryptionService newService = new CredentialEncryptionService();
        ReflectionTestUtils.setField(newService, "masterKey", "short-key");

        // When & Then
        assertThatThrownBy(() -> newService.init())
                .isInstanceOf(CredentialManagementException.class)
                .hasMessageContaining("Master key must be at least 32 characters");
    }

    @Test
    void encryptionDecryption_ShouldHandleLargeData() {
        // Given
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("This is test data segment ").append(i).append(" with some content. ");
        }
        String plaintext = largeData.toString();

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted.length()).isGreaterThan(plaintext.length()); // Should include IV and base64 overhead
    }

    @Test
    void encryptionDecryption_ShouldHandleSpecialCharacters() {
        // Given
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~àáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
        
        // When
        String encrypted = encryptionService.encrypt(specialChars);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(specialChars);
    }

    @Test
    void encryptionDecryption_ShouldHandleUnicodeCharacters() {
        // Given
        String unicode = "Hello 世界! नमस्ते! مرحبا! 🚀🔐💰📈";
        
        // When
        String encrypted = encryptionService.encrypt(unicode);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(unicode);
    }

    @Test
    void performanceTest_EncryptionDecryption_ShouldBeReasonablyFast() {
        // Given
        String plaintext = "api-key-that-needs-to-be-encrypted";
        int iterations = 1000;

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String encrypted = encryptionService.encrypt(plaintext);
            String decrypted = encryptionService.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(plaintext);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then
        assertThat(totalTime).isLessThan(5000); // Should complete 1000 encrypt/decrypt cycles in under 5 seconds
        System.out.println("Performed " + iterations + " encrypt/decrypt cycles in " + totalTime + "ms");
    }
}