package com.trademaster.brokerauth.config;

import com.trademaster.brokerauth.exception.CredentialManagementException;
import com.trademaster.brokerauth.service.VaultSecretService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Vault-Enhanced Security Enforcement Configuration
 * 
 * Enforces enterprise security standards with HashiCorp Vault integration.
 * Validates that critical secrets are properly stored in Vault and prevents
 * deployment with insecure configurations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = true)
public class VaultSecurityEnforcementConfig {

    @Autowired(required = false)
    private VaultSecretService vaultSecretService;

    @Value("${vault.enabled:true}")
    private boolean vaultEnabled;

    @Value("${vault.enforce-secrets:true}")
    private boolean enforceVaultSecrets;

    private final Environment environment;

    // Known insecure/default values that MUST NOT be used in any environment
    private static final Set<String> FORBIDDEN_ENCRYPTION_KEYS = Set.of(
        "",
        "dev_key_change_in_production_32_chars",
        "development_key_not_for_production",
        "test_key_12345678901234567890123456",
        "changeme_32_character_master_key_here",
        "your_encryption_key_here_32_chars_",
        "default_encryption_key_not_secure_",
        "broker_auth_default_key_change_me_",
        "trademaster_default_key_changeme__",
        "CHANGE_ME_IN_PRODUCTION_32_CHARS__",
        "dev_key_change_in_production_32_chars",
        "sample_key_for_testing_only_32chars",
        "demo_key_not_for_production_use___"
    );

    private static final Set<String> FORBIDDEN_JWT_SECRETS = Set.of(
        "",
        "dev_jwt_secret_change_in_production",
        "your_jwt_secret_here",
        "changeme_jwt_secret",
        "default_jwt_secret",
        "jwt_secret_change_me",
        "trademaster_jwt_secret_default",
        "sample_jwt_secret_not_secure"
    );

    private static final Set<String> FORBIDDEN_DB_PASSWORDS = Set.of(
        "",
        "password",
        "admin",
        "postgres",
        "trademaster",
        "dev_password_change_in_prod",
        "changeme",
        "default",
        "test123",
        "admin123"
    );

    /**
     * Critical security validation on application startup
     * BLOCKS APPLICATION STARTUP if security requirements not met
     */
    @PostConstruct
    public void enforceVaultSecurityStandards() {
        log.info("🔒 Enforcing TradeMaster Vault security standards...");

        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        if (!vaultEnabled) {
            handleVaultDisabled(isProduction);
            return;
        }

        if (vaultSecretService == null) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault is enabled but VaultSecretService is not available",
                "VAULT_SERVICE_NOT_AVAILABLE"
            );
        }

        // Validate Vault connectivity
        validateVaultConnectivity();

        // Validate critical secrets in Vault
        validateVaultSecrets(isProduction);

        // Validate secret quality
        validateSecretQuality(isProduction);

        log.info("✅ Vault security standards validation passed");
    }

    /**
     * Additional startup validation after all beans are initialized
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("🔍 Performing final Vault security validation...");

        if (vaultEnabled && enforceVaultSecrets) {
            // Final validation that all critical secrets are accessible
            validateCriticalSecretsAccessible();
        }

        log.info("✅ TradeMaster Vault security validation complete");
        log.info("🚀 Service is VAULT-SECURED and ready for operation");
    }

    /**
     * Handle Vault disabled scenario
     */
    private void handleVaultDisabled(boolean isProduction) {
        String message = "⚠️ Vault is disabled - secrets will not be managed securely";
        
        if (isProduction) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault cannot be disabled in production",
                "VAULT_DISABLED_IN_PRODUCTION"
            );
        }
        
        if (enforceVaultSecrets) {
            throw new CredentialManagementException(
                "🚨 SECURITY VIOLATION: Vault enforcement is enabled but Vault is disabled",
                "VAULT_ENFORCEMENT_CONFLICT"
            );
        }

        log.warn(message);
        log.warn("🔓 Application will continue with traditional property-based secrets");
    }

    /**
     * Validate Vault connectivity
     */
    private void validateVaultConnectivity() {
        try {
            // This will throw an exception if Vault is not accessible
            vaultSecretService.getCacheStats();
            log.info("✅ Vault connectivity validated");
        } catch (Exception e) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Cannot connect to Vault: " + e.getMessage(),
                "VAULT_CONNECTIVITY_FAILED"
            );
        }
    }

    /**
     * Validate that critical secrets exist in Vault
     */
    private void validateVaultSecrets(boolean isProduction) {
        log.info("🔍 Validating critical secrets in Vault...");

        // Check encryption key
        if (!vaultSecretService.getSecret("encryption-key").isPresent()) {
            if (isProduction) {
                throw new CredentialManagementException(
                    "🚨 CRITICAL SECURITY VIOLATION: Encryption key not found in Vault",
                    "VAULT_ENCRYPTION_KEY_MISSING"
                );
            } else {
                log.warn("⚠️ Encryption key not found in Vault (development mode)");
            }
        }

        // Check JWT secret
        if (!vaultSecretService.getSecret("jwt-secret").isPresent()) {
            if (isProduction) {
                throw new CredentialManagementException(
                    "🚨 CRITICAL SECURITY VIOLATION: JWT secret not found in Vault",
                    "VAULT_JWT_SECRET_MISSING"
                );
            } else {
                log.warn("⚠️ JWT secret not found in Vault (development mode)");
            }
        }

        // Check database password (optional but recommended)
        if (!vaultSecretService.getDatabasePassword().isPresent()) {
            log.warn("⚠️ Database password not found in Vault - ensure external secret management is used");
        }

        log.info("✅ Critical secrets validation passed");
    }

    /**
     * Validate the quality of secrets stored in Vault
     */
    private void validateSecretQuality(boolean isProduction) {
        log.info("🔍 Validating secret quality in Vault...");

        // Validate encryption key quality
        Optional<String> encryptionKey = vaultSecretService.getSecret("encryption-key");
        if (encryptionKey.isPresent()) {
            validateEncryptionKeyQuality(encryptionKey.get(), isProduction);
        }

        // Validate JWT secret quality
        Optional<String> jwtSecret = vaultSecretService.getSecret("jwt-secret");
        if (jwtSecret.isPresent()) {
            validateJwtSecretQuality(jwtSecret.get(), isProduction);
        }

        // Validate database password quality
        Optional<String> dbPassword = vaultSecretService.getDatabasePassword();
        if (dbPassword.isPresent()) {
            validateDatabasePasswordQuality(dbPassword.get(), isProduction);
        }

        log.info("✅ Secret quality validation passed");
    }

    /**
     * Validate encryption key quality
     */
    private void validateEncryptionKeyQuality(String encryptionKey, boolean isProduction) {
        if (FORBIDDEN_ENCRYPTION_KEYS.contains(encryptionKey)) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault contains forbidden encryption key. " +
                "Generate a secure random key: openssl rand -base64 48",
                "VAULT_INSECURE_ENCRYPTION_KEY"
            );
        }

        if (encryptionKey.length() < 32) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault encryption key too short. " +
                "Minimum 32 characters required, got " + encryptionKey.length(),
                "VAULT_WEAK_ENCRYPTION_KEY"
            );
        }

        if (isProduction && encryptionKey.length() < 64) {
            throw new CredentialManagementException(
                "🚨 PRODUCTION SECURITY VIOLATION: Production encryption key must be at least 64 characters. " +
                "Current length: " + encryptionKey.length(),
                "VAULT_PRODUCTION_KEY_TOO_SHORT"
            );
        }

        // Check for weak patterns
        if (isProduction) {
            String lowerKey = encryptionKey.toLowerCase();
            if (lowerKey.contains("prod") || lowerKey.contains("test") ||
                lowerKey.contains("demo") || lowerKey.contains("sample")) {
                throw new CredentialManagementException(
                    "🚨 PRODUCTION SECURITY VIOLATION: Vault encryption key contains weak patterns",
                    "VAULT_PRODUCTION_KEY_WEAK_PATTERN"
                );
            }
        }

        log.info("✅ Vault encryption key validation passed (length: {})", encryptionKey.length());
    }

    /**
     * Validate JWT secret quality
     */
    private void validateJwtSecretQuality(String jwtSecret, boolean isProduction) {
        if (FORBIDDEN_JWT_SECRETS.contains(jwtSecret)) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault contains forbidden JWT secret. " +
                "Generate a secure random secret: openssl rand -base64 64",
                "VAULT_INSECURE_JWT_SECRET"
            );
        }

        if (jwtSecret.length() < 32) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault JWT secret too short. " +
                "Minimum 32 characters required, got " + jwtSecret.length(),
                "VAULT_WEAK_JWT_SECRET"
            );
        }

        if (isProduction && jwtSecret.length() < 64) {
            throw new CredentialManagementException(
                "🚨 PRODUCTION SECURITY VIOLATION: Production JWT secret must be at least 64 characters",
                "VAULT_PRODUCTION_JWT_SECRET_TOO_SHORT"
            );
        }

        log.info("✅ Vault JWT secret validation passed");
    }

    /**
     * Validate database password quality
     */
    private void validateDatabasePasswordQuality(String dbPassword, boolean isProduction) {
        if (FORBIDDEN_DB_PASSWORDS.contains(dbPassword.toLowerCase())) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Vault contains forbidden database password. " +
                "Use a strong random password: openssl rand -base64 32",
                "VAULT_INSECURE_DB_PASSWORD"
            );
        }

        if (dbPassword.length() < 12) {
            throw new CredentialManagementException(
                "🚨 SECURITY VIOLATION: Vault database password too short. Minimum 12 characters required",
                "VAULT_WEAK_DB_PASSWORD"
            );
        }

        if (isProduction && dbPassword.length() < 24) {
            throw new CredentialManagementException(
                "🚨 PRODUCTION SECURITY VIOLATION: Production database password must be at least 24 characters",
                "VAULT_PRODUCTION_DB_PASSWORD_TOO_SHORT"
            );
        }

        log.info("✅ Vault database password validation passed");
    }

    /**
     * Final validation that critical secrets are accessible
     */
    private void validateCriticalSecretsAccessible() {
        try {
            // Test that we can actually retrieve the encryption key
            vaultSecretService.getEncryptionKey();
            log.info("✅ Encryption key accessible from Vault");

            // Test that we can actually retrieve the JWT secret  
            vaultSecretService.getJwtSecret();
            log.info("✅ JWT secret accessible from Vault");

        } catch (Exception e) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Cannot access critical secrets from Vault: " + e.getMessage(),
                "VAULT_CRITICAL_SECRETS_INACCESSIBLE"
            );
        }
    }
}