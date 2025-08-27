package com.trademaster.brokerauth.config;

import com.trademaster.brokerauth.exception.CredentialManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption Configuration
 * 
 * Manages encryption key configuration and validation.
 * Ensures secure key management across different environments.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class EncryptionConfig {
    
    @Value("${broker.auth.encryption.key:}")
    private String masterKey;
    
    @Value("${broker.auth.encryption.key-source:property}")
    private String keySource; // property, environment, vault, file
    
    @Value("${broker.auth.encryption.key-rotation.enabled:false}")
    private boolean keyRotationEnabled;
    
    @Value("${broker.auth.encryption.key-rotation.interval-hours:24}")
    private int keyRotationIntervalHours;
    
    private static final int MINIMUM_KEY_LENGTH = 32;
    private static final String[] WEAK_KEYS = {
        "dev_key_change_in_production_32_chars",
        "development_key_not_for_production",
        "test_key_12345678901234567890123456",
        "changeme_32_character_master_key_here"
    };
    
    /**
     * Validates encryption configuration on startup
     */
    @PostConstruct
    public void validateEncryptionConfig() {
        log.info("Validating encryption configuration...");
        
        // Check if key is configured
        if (masterKey == null || masterKey.trim().isEmpty()) {
            throw new CredentialManagementException(
                "Master encryption key not configured. Please set broker.auth.encryption.key property.",
                "VALIDATION"
            );
        }
        
        // Check key length
        if (masterKey.length() < MINIMUM_KEY_LENGTH) {
            throw new CredentialManagementException(
                String.format("Master key must be at least %d characters long. Current length: %d", 
                    MINIMUM_KEY_LENGTH, masterKey.length()),
                "VALIDATION"
            );
        }
        
        // Check for weak/default keys
        for (String weakKey : WEAK_KEYS) {
            if (weakKey.equals(masterKey)) {
                throw new CredentialManagementException(
                    "Using default/weak master key is not allowed in any environment. Please generate a strong key.",
                    "SECURITY_VALIDATION"
                );
            }
        }
        
        // Validate key strength (basic entropy check)
        if (!isKeyStrong(masterKey)) {
            log.warn("Master key may be weak. Consider using a randomly generated key with high entropy.");
        }
        
        log.info("Encryption configuration validated successfully. Key source: {}, Key rotation: {}", 
            keySource, keyRotationEnabled ? "enabled" : "disabled");
        
        // Log key rotation settings if enabled
        if (keyRotationEnabled) {
            log.info("Key rotation enabled with interval: {} hours", keyRotationIntervalHours);
        }
    }
    
    /**
     * Production-only encryption key validator
     * Stricter validation for production environments
     */
    @Bean
    @Profile("!dev & !test")
    public EncryptionKeyValidator productionKeyValidator() {
        return new EncryptionKeyValidator() {
            @PostConstruct
            public void validateProductionKey() {
                log.info("Performing production encryption key validation...");
                
                // Production keys must be at least 64 characters
                if (masterKey.length() < 64) {
                    throw new CredentialManagementException(
                        "Production encryption key must be at least 64 characters long for enhanced security.",
                        "PRODUCTION_SECURITY_VALIDATION"
                    );
                }
                
                // Check key entropy (more strict for production)
                if (!hasHighEntropy(masterKey)) {
                    throw new CredentialManagementException(
                        "Production encryption key must have high entropy. Use a cryptographically secure random generator.",
                        "PRODUCTION_ENTROPY_VALIDATION"
                    );
                }
                
                // Ensure key is not environment-specific default
                if (masterKey.contains("production") || masterKey.contains("prod") || 
                    masterKey.contains("default") || masterKey.contains("example")) {
                    throw new CredentialManagementException(
                        "Production key cannot contain common words like 'production', 'default', etc.",
                        "PRODUCTION_PATTERN_VALIDATION"
                    );
                }
                
                log.info("Production encryption key validation passed");
            }
        };
    }
    
    /**
     * Key generation utility for development and testing
     */
    @Bean
    @Profile("dev | test")
    public EncryptionKeyGenerator developmentKeyGenerator() {
        return new EncryptionKeyGenerator();
    }
    
    /**
     * Check if key has reasonable strength (basic entropy check)
     */
    private boolean isKeyStrong(String key) {
        if (key == null || key.length() < MINIMUM_KEY_LENGTH) {
            return false;
        }
        
        // Check for variety of characters
        boolean hasLower = key.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = key.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = key.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = key.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        
        int varietyCount = 0;
        if (hasLower) varietyCount++;
        if (hasUpper) varietyCount++;
        if (hasDigit) varietyCount++;
        if (hasSpecial) varietyCount++;
        
        // Should have at least 3 types of characters
        if (varietyCount < 3) {
            return false;
        }
        
        // Check for repeated patterns
        if (hasRepeatedPatterns(key)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check for high entropy (production validation)
     */
    private boolean hasHighEntropy(String key) {
        if (!isKeyStrong(key)) {
            return false;
        }
        
        // Calculate approximate entropy
        int uniqueChars = (int) key.chars().distinct().count();
        double entropy = uniqueChars * Math.log(uniqueChars) / Math.log(2);
        
        // High entropy threshold (adjust as needed)
        return entropy > 5.0 && uniqueChars > 20;
    }
    
    /**
     * Check for repeated patterns that indicate weak keys
     */
    private boolean hasRepeatedPatterns(String key) {
        // Check for repeated substrings
        for (int i = 0; i < key.length() - 3; i++) {
            String substring = key.substring(i, i + 4);
            if (key.indexOf(substring, i + 1) != -1) {
                return true;
            }
        }
        
        // Check for sequential characters
        int sequentialCount = 0;
        for (int i = 0; i < key.length() - 1; i++) {
            if (Math.abs(key.charAt(i) - key.charAt(i + 1)) == 1) {
                sequentialCount++;
                if (sequentialCount > 3) {
                    return true;
                }
            } else {
                sequentialCount = 0;
            }
        }
        
        return false;
    }
    
    /**
     * Encryption key validator interface
     */
    public interface EncryptionKeyValidator {
        // Marker interface for key validation
    }
    
    /**
     * Encryption key generator for development
     */
    public static class EncryptionKeyGenerator {
        
        private static final String CHARSET = 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_+=<>?";
        
        /**
         * Generate a cryptographically secure random key
         */
        public String generateSecureKey(int length) {
            if (length < MINIMUM_KEY_LENGTH) {
                throw new IllegalArgumentException("Key length must be at least " + MINIMUM_KEY_LENGTH);
            }
            
            SecureRandom random = new SecureRandom();
            StringBuilder key = new StringBuilder(length);
            
            for (int i = 0; i < length; i++) {
                key.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
            }
            
            return key.toString();
        }
        
        /**
         * Generate a base64-encoded random key
         */
        public String generateBase64Key(int byteLength) {
            SecureRandom random = new SecureRandom();
            byte[] keyBytes = new byte[byteLength];
            random.nextBytes(keyBytes);
            return Base64.getEncoder().encodeToString(keyBytes);
        }
        
        /**
         * Print a sample secure key for development (should not be used in production)
         */
        public void printSampleKey() {
            String sampleKey = generateSecureKey(64);
            System.out.println("Sample secure encryption key (DO NOT USE IN PRODUCTION):");
            System.out.println(sampleKey);
            System.out.println();
            System.out.println("Base64 encoded version:");
            System.out.println(generateBase64Key(48));
        }
    }
}