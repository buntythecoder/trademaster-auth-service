package com.trademaster.brokerauth.service;

import com.trademaster.brokerauth.exception.CredentialManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Credential Encryption Service
 * 
 * Provides secure encryption and decryption of sensitive broker credentials
 * using AES-256-GCM encryption for maximum security.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class CredentialEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    
    private final SecretKey masterKey;
    private final SecureRandom secureRandom;
    
    public CredentialEncryptionService(@Value("${broker.auth.encryption.key}") String masterKeyString) {
        this.secureRandom = new SecureRandom();
        this.masterKey = createMasterKey(masterKeyString);
        
        log.info("Credential encryption service initialized with AES-256-GCM");
    }
    
    /**
     * Encrypt sensitive data
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);
            
            // Encrypt data
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            // Encode to Base64
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            log.error("Failed to encrypt credential", e);
            throw new CredentialManagementException("Encryption failed: " + e.getMessage(), "ENCRYPTION_FAILURE");
        }
    }
    
    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null;
        }
        
        try {
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedData);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);
            
            // Decrypt data
            byte[] decryptedData = cipher.doFinal(encrypted);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Failed to decrypt credential", e);
            throw new CredentialManagementException("Decryption failed: " + e.getMessage(), "DECRYPTION_FAILURE");
        }
    }
    
    /**
     * Check if data is encrypted (Base64 encoded)
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Generate a secure random key for new installations
     */
    public static String generateMasterKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256); // AES-256
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate master key", e);
        }
    }
    
    /**
     * Create master key from string
     */
    private SecretKey createMasterKey(String masterKeyString) {
        try {
            byte[] keyBytes;
            
            if (masterKeyString == null || masterKeyString.trim().isEmpty()) {
                throw new CredentialManagementException("Master encryption key is required", "KEY_MISSING");
            } else {
                // Decode from Base64 or create from string
                try {
                    keyBytes = Base64.getDecoder().decode(masterKeyString);
                } catch (IllegalArgumentException e) {
                    // If not Base64, use SHA-256 hash of the string
                    keyBytes = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(masterKeyString.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // Ensure key is exactly 32 bytes (256 bits)
            if (keyBytes.length != 32) {
                byte[] normalizedKey = new byte[32];
                if (keyBytes.length > 32) {
                    System.arraycopy(keyBytes, 0, normalizedKey, 0, 32);
                } else {
                    System.arraycopy(keyBytes, 0, normalizedKey, 0, keyBytes.length);
                    // Fill remaining bytes with zeros (not ideal but functional)
                }
                keyBytes = normalizedKey;
            }
            
            return new SecretKeySpec(keyBytes, ALGORITHM);
            
        } catch (Exception e) {
            throw new CredentialManagementException("Failed to create master key: " + e.getMessage(), "KEY_CREATION_FAILURE");
        }
    }
    
    /**
     * Rotate encryption key (re-encrypt all data with new key)
     * This method would be used in key rotation scenarios
     */
    public String reEncrypt(String encryptedData, SecretKey oldKey, SecretKey newKey) {
        // This is a placeholder for key rotation functionality
        // In a production system, this would:
        // 1. Decrypt with old key
        // 2. Encrypt with new key
        // 3. Update database atomically
        throw new UnsupportedOperationException("Key rotation not yet implemented");
    }
    
    /**
     * Validate encryption integrity
     */
    public boolean validateIntegrity(String encryptedData) {
        try {
            String decrypted = decrypt(encryptedData);
            return decrypted != null;
        } catch (Exception e) {
            return false;
        }
    }
}