package com.trademaster.brokerauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultResponseSupport;

import java.net.URI;
import java.time.Duration;

/**
 * HashiCorp Vault Configuration for Secure Secrets Management
 * 
 * Provides enterprise-grade secrets management for TradeMaster Broker Auth Service.
 * Supports multiple authentication methods and secure credential storage.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class VaultConfig extends AbstractVaultConfiguration {

    @Value("${vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${vault.token:}")
    private String vaultToken;

    @Value("${vault.app-role.role-id:}")
    private String roleId;

    @Value("${vault.app-role.secret-id:}")
    private String secretId;

    @Value("${vault.app-role.path:approle}")
    private String appRolePath;

    @Value("${vault.connection-timeout:5s}")
    private Duration connectionTimeout;

    @Value("${vault.read-timeout:15s}")
    private Duration readTimeout;

    @Value("${vault.enabled:true}")
    private boolean vaultEnabled;

    /**
     * Vault endpoint configuration
     */
    @Override
    public VaultEndpoint vaultEndpoint() {
        try {
            URI uri = URI.create(vaultUri);
            VaultEndpoint endpoint = VaultEndpoint.from(uri);
            
            log.info("🔒 Configured Vault endpoint: {}", vaultUri);
            return endpoint;
        } catch (Exception e) {
            log.error("❌ Failed to configure Vault endpoint: {}", e.getMessage());
            throw new IllegalStateException("Invalid Vault URI: " + vaultUri, e);
        }
    }

    /**
     * Client authentication configuration - supports both Token and AppRole
     */
    @Override
    public ClientAuthentication clientAuthentication() {
        if (!vaultEnabled) {
            log.warn("⚠️ Vault is disabled - using token authentication with empty token");
            return new TokenAuthentication(VaultToken.of(""));
        }

        // Prefer AppRole authentication for production
        if (isAppRoleConfigured()) {
            log.info("🔐 Using AppRole authentication for Vault");
            return createAppRoleAuthentication();
        }
        
        // Fallback to token authentication for development
        if (isTokenConfigured()) {
            log.info("🎫 Using Token authentication for Vault");
            return new TokenAuthentication(VaultToken.of(vaultToken));
        }

        throw new IllegalStateException(
            "❌ Vault authentication not configured. " +
            "Please configure either vault.token or vault.app-role.* properties"
        );
    }

    /**
     * Custom VaultTemplate with enhanced configuration
     */
    @Bean
    @Profile("!test")
    public VaultTemplate vaultTemplate() {
        if (!vaultEnabled) {
            log.warn("⚠️ Vault is disabled - returning mock VaultTemplate");
            return new MockVaultTemplate();
        }

        try {
            VaultTemplate template = new VaultTemplate(
                vaultEndpoint(), 
                clientAuthentication()
            );

            // Configure timeouts
            template.opsForSys().health();
            
            log.info("✅ Vault connection established successfully");
            return template;
        } catch (Exception e) {
            log.error("❌ Failed to establish Vault connection: {}", e.getMessage());
            
            // In non-production environments, continue with disabled Vault
            if (isNonProductionEnvironment()) {
                log.warn("⚠️ Continuing with disabled Vault in non-production environment");
                return new MockVaultTemplate();
            }
            
            throw new IllegalStateException("Failed to connect to Vault", e);
        }
    }

    /**
     * Create AppRole authentication
     */
    private ClientAuthentication createAppRoleAuthentication() {
        AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
            .roleId(AppRoleAuthenticationOptions.RoleId.provided(roleId))
            .secretId(AppRoleAuthenticationOptions.SecretId.provided(secretId))
            .path(appRolePath)
            .build();

        return new AppRoleAuthentication(options, restOperations());
    }

    /**
     * Check if AppRole authentication is configured
     */
    private boolean isAppRoleConfigured() {
        return roleId != null && !roleId.trim().isEmpty() &&
               secretId != null && !secretId.trim().isEmpty();
    }

    /**
     * Check if Token authentication is configured
     */
    private boolean isTokenConfigured() {
        return vaultToken != null && !vaultToken.trim().isEmpty();
    }

    /**
     * Check if running in non-production environment
     */
    private boolean isNonProductionEnvironment() {
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        return activeProfiles.contains("dev") || 
               activeProfiles.contains("test") || 
               activeProfiles.contains("local");
    }

    /**
     * Mock VaultTemplate for development/testing
     */
    private static class MockVaultTemplate extends VaultTemplate {
        public MockVaultTemplate() {
            super(VaultEndpoint.create("localhost", 8200), 
                  new TokenAuthentication(VaultToken.of("mock")));
        }

        @Override
        public <T> VaultResponseSupport<T> read(String path, Class<T> responseType) {
            log.warn("⚠️ Mock VaultTemplate - returning null for path: {}", path);
            return null;
        }

        @Override
        public VaultResponse write(String path, Object body) {
            log.warn("⚠️ Mock VaultTemplate - ignoring write to path: {}", path);
            return null;
        }
    }
}