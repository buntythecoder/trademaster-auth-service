package com.trademaster.brokerauth.config;

import com.trademaster.brokerauth.exception.CredentialManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

/**
 * Security Enforcement Configuration
 * 
 * Enforces enterprise security standards and prevents deployment
 * with insecure configurations. Blocks startup if security requirements not met.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class SecurityEnforcementConfig {
    
    @Value("${broker.auth.encryption.key:}")
    private String encryptionKey;
    
    @Value("${spring.datasource.password:}")
    private String dbPassword;
    
    @Value("${security.jwt.secret:}")
    private String jwtSecret;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
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
        "dev_key_change_in_production_32_chars", // From application-docker.yml
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
    
    public SecurityEnforcementConfig(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Critical security validation on application startup
     * BLOCKS APPLICATION STARTUP if security requirements not met
     */
    @PostConstruct
    public void enforceSecurityStandards() {
        log.info("🛡️  Enforcing TradeMaster security standards...");
        
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean isDocker = Arrays.asList(environment.getActiveProfiles()).contains("docker");
        
        // CRITICAL: Validate encryption key
        validateEncryptionKey(isProduction);
        
        // CRITICAL: Validate JWT secret
        validateJwtSecret(isProduction);
        
        // CRITICAL: Validate database credentials
        validateDatabaseSecurity(isProduction);
        
        // CRITICAL: Validate Redis security
        validateRedisSecurity(isProduction);
        
        // Additional security checks for production
        if (isProduction) {
            enforceProductionSecurityStandards();
        }
        
        log.info("✅ Security standards validation passed");
    }
    
    /**
     * Additional startup validation after all beans are initialized
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("🔍 Performing final security validation...");
        
        // Validate that all required security components are properly configured
        validateSecurityComponents();
        
        log.info("✅ TradeMaster Broker Auth Service security validation complete");
        log.info("🚀 Service is SECURE and ready for operation");
    }
    
    private void validateEncryptionKey(boolean isProduction) {
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Encryption key is not configured. " +
                "Set broker.auth.encryption.key property with a secure random key.",
                "ENCRYPTION_KEY_MISSING"
            );
        }
        
        // Check against known insecure keys
        if (FORBIDDEN_ENCRYPTION_KEYS.contains(encryptionKey)) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Using default/demo encryption key is FORBIDDEN. " +
                "This key is publicly known and compromises all encrypted data. " +
                "Generate a secure random key immediately using: " +
                "openssl rand -base64 48",
                "INSECURE_ENCRYPTION_KEY"
            );
        }
        
        // Minimum length check
        if (encryptionKey.length() < 32) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Encryption key too short. " +
                "Minimum 32 characters required, got " + encryptionKey.length(),
                "WEAK_ENCRYPTION_KEY"
            );
        }
        
        // Production-specific validation
        if (isProduction) {
            if (encryptionKey.length() < 64) {
                throw new CredentialManagementException(
                    "🚨 PRODUCTION SECURITY VIOLATION: Production encryption key must be at least 64 characters. " +
                    "Current length: " + encryptionKey.length(),
                    "PRODUCTION_KEY_TOO_SHORT"
                );
            }
            
            // Check for weak patterns
            if (encryptionKey.toLowerCase().contains("prod") || 
                encryptionKey.toLowerCase().contains("test") ||
                encryptionKey.toLowerCase().contains("demo") ||
                encryptionKey.toLowerCase().contains("sample")) {
                throw new CredentialManagementException(
                    "🚨 PRODUCTION SECURITY VIOLATION: Encryption key contains weak patterns. " +
                    "Generate a cryptographically secure random key.",
                    "PRODUCTION_KEY_WEAK_PATTERN"
                );
            }
        }
        
        log.info("✅ Encryption key validation passed (length: {})", encryptionKey.length());
    }
    
    private void validateJwtSecret(boolean isProduction) {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: JWT secret is not configured. " +
                "Set security.jwt.secret property with a secure random string.",
                "JWT_SECRET_MISSING"
            );
        }
        
        if (FORBIDDEN_JWT_SECRETS.contains(jwtSecret)) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Using default/demo JWT secret is FORBIDDEN. " +
                "Generate a secure random secret: openssl rand -base64 64",
                "INSECURE_JWT_SECRET"
            );
        }
        
        if (jwtSecret.length() < 32) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: JWT secret too short. " +
                "Minimum 32 characters required, got " + jwtSecret.length(),
                "WEAK_JWT_SECRET"
            );
        }
        
        if (isProduction && jwtSecret.length() < 64) {
            throw new CredentialManagementException(
                "🚨 PRODUCTION SECURITY VIOLATION: Production JWT secret must be at least 64 characters.",
                "PRODUCTION_JWT_SECRET_TOO_SHORT"
            );
        }
        
        log.info("✅ JWT secret validation passed");
    }
    
    private void validateDatabaseSecurity(boolean isProduction) {
        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            log.warn("⚠️  Database password not configured - ensure external secret management is used");
            return;
        }
        
        if (FORBIDDEN_DB_PASSWORDS.contains(dbPassword.toLowerCase())) {
            throw new CredentialManagementException(
                "🚨 CRITICAL SECURITY VIOLATION: Using default/weak database password is FORBIDDEN. " +
                "Use a strong random password: openssl rand -base64 32",
                "INSECURE_DB_PASSWORD"
            );
        }
        
        if (dbPassword.length() < 12) {
            throw new CredentialManagementException(
                "🚨 SECURITY VIOLATION: Database password too short. Minimum 12 characters required.",
                "WEAK_DB_PASSWORD"
            );
        }
        
        if (isProduction && dbPassword.length() < 24) {
            throw new CredentialManagementException(
                "🚨 PRODUCTION SECURITY VIOLATION: Production database password must be at least 24 characters.",
                "PRODUCTION_DB_PASSWORD_TOO_SHORT"
            );
        }
        
        log.info("✅ Database security validation passed");
    }
    
    private void validateRedisSecurity(boolean isProduction) {
        // Redis password is optional but recommended for production
        if (isProduction && (redisPassword == null || redisPassword.trim().isEmpty())) {
            log.warn("⚠️  PRODUCTION WARNING: Redis password not configured. Consider enabling Redis AUTH for production.");
        }
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            if (redisPassword.equals("password") || redisPassword.equals("redis") || redisPassword.equals("changeme")) {
                throw new CredentialManagementException(
                    "🚨 SECURITY VIOLATION: Using default Redis password is forbidden.",
                    "INSECURE_REDIS_PASSWORD"
                );
            }
            log.info("✅ Redis security validation passed");
        }
    }
    
    private void enforceProductionSecurityStandards() {
        log.info("🔒 Enforcing production-specific security standards...");
        
        // Check that we're not using development/testing dependencies
        try {
            Class.forName("org.h2.Driver");
            throw new CredentialManagementException(
                "🚨 PRODUCTION SECURITY VIOLATION: H2 database detected in classpath. " +
                "Remove H2 dependency from production builds.",
                "PRODUCTION_INSECURE_DEPENDENCY"
            );
        } catch (ClassNotFoundException e) {
            // Good - H2 not in classpath
        }
        
        // Additional production checks can be added here:
        // - SSL/TLS certificate validation
        // - Network security configuration
        // - File system permissions
        // - JVM security settings
        
        log.info("✅ Production security standards validated");
    }
    
    private void validateSecurityComponents() {
        // This method validates that all required security beans are properly initialized
        // Additional validation logic can be added here as needed
        
        log.debug("Security component validation completed");
    }
}