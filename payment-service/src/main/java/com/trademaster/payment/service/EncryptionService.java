package com.trademaster.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption Service
 * 
 * Provides AES-256-GCM encryption for sensitive payment data.
 * Implements PCI DSS compliant encryption for data at rest.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 256; // bits
    
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom;
    
    public EncryptionService(@Value("${app.encryption.key:#{null}}") String encryptionKeyString) {
        this.secureRandom = new SecureRandom();
        this.encryptionKey = initializeKey(encryptionKeyString);
    }
    
    /**
     * Encrypt sensitive data
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return plaintext;
        }
        
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
            
            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and ciphertext
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            
            // Encode to Base64
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return encryptedText;
        }
        
        try {
            // Decode from Base64
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            System.arraycopy(encryptedData, iv.length, ciphertext, 0, ciphertext.length);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
            
            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Generate a new encryption key
     */
    public String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            log.error("Failed to generate encryption key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }
    
    /**
     * Hash sensitive data for indexing (one-way)
     */
    public String hash(String data) {
        if (data == null || data.trim().isEmpty()) {
            return data;
        }
        
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to hash data", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }
    
    /**
     * Mask sensitive data for logging
     */
    public String mask(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.length() <= 4) {
            return "****";
        }
        
        if (sensitiveData.length() <= 8) {
            return "****" + sensitiveData.substring(sensitiveData.length() - 2);
        }
        
        return "****-****-****-" + sensitiveData.substring(sensitiveData.length() - 4);
    }
    
    /**
     * Validate encryption key strength
     */
    public boolean validateKeyStrength(String keyString) {
        if (keyString == null) {
            return false;
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            return keyBytes.length >= (KEY_LENGTH / 8); // Convert bits to bytes
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Initialize encryption key from configuration or generate new one
     */
    private SecretKey initializeKey(String encryptionKeyString) {
        try {
            if (encryptionKeyString != null && !encryptionKeyString.trim().isEmpty()) {
                // Use provided key
                if (!validateKeyStrength(encryptionKeyString)) {
                    log.warn("Provided encryption key does not meet strength requirements");
                }
                
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyString.trim());
                return new SecretKeySpec(keyBytes, ALGORITHM);
                
            } else {
                // Generate new key for development/testing
                log.warn("No encryption key provided - generating temporary key. " +
                        "This should not be used in production!");
                
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                keyGenerator.init(KEY_LENGTH);
                SecretKey tempKey = keyGenerator.generateKey();
                
                log.info("Generated temporary encryption key: {}", 
                        Base64.getEncoder().encodeToString(tempKey.getEncoded()));
                
                return tempKey;
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize encryption key", e);
            throw new RuntimeException("Encryption key initialization failed", e);
        }
    }
    
    /**
     * Securely wipe sensitive data from memory
     */
    public void secureWipe(char[] sensitiveData) {
        if (sensitiveData != null) {
            java.util.Arrays.fill(sensitiveData, '\0');
        }
    }
    
    /**
     * Securely wipe sensitive data from memory
     */
    public void secureWipe(byte[] sensitiveData) {
        if (sensitiveData != null) {
            java.util.Arrays.fill(sensitiveData, (byte) 0);
        }
    }
}