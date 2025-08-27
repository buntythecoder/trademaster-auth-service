package com.trademaster.brokerauth.service;

import com.trademaster.brokerauth.entity.BrokerAccount;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.repository.BrokerAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Credential Management Service
 * 
 * Manages secure storage and retrieval of broker credentials.
 * Handles encryption/decryption and credential validation.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialManagementService {
    
    private final BrokerAccountRepository brokerAccountRepository;
    private final CredentialEncryptionService encryptionService;
    
    /**
     * Store encrypted credentials for a broker account
     */
    @Transactional
    public void storeCredentials(Long userId, BrokerType brokerType, BrokerCredentials credentials) {
        log.debug("Storing credentials for user {} and broker {}", userId, brokerType);
        
        Optional<BrokerAccount> existingAccount = brokerAccountRepository
                .findByUserIdAndBrokerType(userId, brokerType);
        
        BrokerAccount account = existingAccount.orElse(
                BrokerAccount.builder()
                        .userId(userId)
                        .brokerType(brokerType)
                        .isActive(true)
                        .isVerified(false)
                        .build()
        );
        
        // Encrypt and store credentials based on broker type
        updateCredentialsForBrokerType(account, brokerType, credentials);
        
        // Save the account
        brokerAccountRepository.save(account);
        
        log.info("Credentials stored successfully for user {} and broker {}", userId, brokerType);
    }
    
    /**
     * Retrieve and decrypt credentials for a broker account
     */
    @Transactional(readOnly = true)
    public Optional<BrokerCredentials> getCredentials(Long userId, BrokerType brokerType) {
        log.debug("Retrieving credentials for user {} and broker {}", userId, brokerType);
        
        Optional<BrokerAccount> accountOpt = brokerAccountRepository
                .findByUserIdAndBrokerType(userId, brokerType);
        
        if (accountOpt.isEmpty()) {
            log.warn("No broker account found for user {} and broker {}", userId, brokerType);
            return Optional.empty();
        }
        
        BrokerAccount account = accountOpt.get();
        if (!account.canAuthenticate()) {
            log.warn("Broker account cannot authenticate for user {} and broker {}", userId, brokerType);
            return Optional.empty();
        }
        
        try {
            BrokerCredentials credentials = decryptCredentialsForBrokerType(account, brokerType);
            return Optional.of(credentials);
        } catch (Exception e) {
            log.error("Failed to decrypt credentials for user {} and broker {}", userId, brokerType, e);
            return Optional.empty();
        }
    }
    
    /**
     * Update credentials for existing account
     */
    @Transactional
    public void updateCredentials(Long userId, BrokerType brokerType, BrokerCredentials credentials) {
        log.debug("Updating credentials for user {} and broker {}", userId, brokerType);
        
        BrokerAccount account = brokerAccountRepository
                .findByUserIdAndBrokerType(userId, brokerType)
                .orElseThrow(() -> new IllegalArgumentException("Broker account not found"));
        
        updateCredentialsForBrokerType(account, brokerType, credentials);
        account.setIsVerified(false); // Need to re-verify after update
        
        brokerAccountRepository.save(account);
        
        log.info("Credentials updated successfully for user {} and broker {}", userId, brokerType);
    }
    
    /**
     * Delete credentials for a broker account
     */
    @Transactional
    public void deleteCredentials(Long userId, BrokerType brokerType) {
        log.debug("Deleting credentials for user {} and broker {}", userId, brokerType);
        
        Optional<BrokerAccount> accountOpt = brokerAccountRepository
                .findByUserIdAndBrokerType(userId, brokerType);
        
        if (accountOpt.isPresent()) {
            BrokerAccount account = accountOpt.get();
            account.deactivate("Credentials deleted by user");
            
            // Clear all encrypted fields
            account.setEncryptedApiKey(null);
            account.setEncryptedApiSecret(null);
            account.setEncryptedPassword(null);
            account.setEncryptedTotpSecret(null);
            
            brokerAccountRepository.save(account);
            
            log.info("Credentials deleted successfully for user {} and broker {}", userId, brokerType);
        }
    }
    
    /**
     * Verify credentials by attempting to decrypt them
     */
    @Transactional
    public boolean verifyCredentials(Long userId, BrokerType brokerType) {
        log.debug("Verifying credentials for user {} and broker {}", userId, brokerType);
        
        Optional<BrokerAccount> accountOpt = brokerAccountRepository
                .findByUserIdAndBrokerType(userId, brokerType);
        
        if (accountOpt.isEmpty()) {
            return false;
        }
        
        BrokerAccount account = accountOpt.get();
        
        try {
            // Try to decrypt credentials
            decryptCredentialsForBrokerType(account, brokerType);
            
            // Mark as verified if successful
            account.verify();
            brokerAccountRepository.save(account);
            
            log.info("Credentials verified successfully for user {} and broker {}", userId, brokerType);
            return true;
            
        } catch (Exception e) {
            log.error("Credential verification failed for user {} and broker {}", userId, brokerType, e);
            return false;
        }
    }
    
    /**
     * Check if credentials exist for a broker account
     */
    @Transactional(readOnly = true)
    public boolean hasCredentials(Long userId, BrokerType brokerType) {
        return brokerAccountRepository
                .findByUserIdAndBrokerType(userId, brokerType)
                .map(BrokerAccount::hasRequiredCredentials)
                .orElse(false);
    }
    
    /**
     * Update credentials based on broker type
     */
    private void updateCredentialsForBrokerType(BrokerAccount account, BrokerType brokerType, 
                                               BrokerCredentials credentials) {
        switch (brokerType) {
            case ZERODHA -> {
                if (credentials.getApiKey() == null || credentials.getApiSecret() == null) {
                    throw new IllegalArgumentException("Zerodha requires API key and secret");
                }
                account.setEncryptedApiKey(encryptionService.encrypt(credentials.getApiKey()));
                account.setEncryptedApiSecret(encryptionService.encrypt(credentials.getApiSecret()));
            }
            
            case UPSTOX -> {
                if (credentials.getClientId() == null || credentials.getApiSecret() == null) {
                    throw new IllegalArgumentException("Upstox requires client ID and API secret");
                }
                account.setClientId(credentials.getClientId()); // Not encrypted
                account.setEncryptedApiSecret(encryptionService.encrypt(credentials.getApiSecret()));
                if (credentials.getRedirectUri() != null) {
                    account.setRedirectUri(credentials.getRedirectUri());
                }
            }
            
            case ANGEL_ONE -> {
                if (credentials.getApiKey() == null || credentials.getPassword() == null || 
                    credentials.getTotpSecret() == null) {
                    throw new IllegalArgumentException("Angel One requires API key, password, and TOTP secret");
                }
                account.setEncryptedApiKey(encryptionService.encrypt(credentials.getApiKey()));
                account.setEncryptedPassword(encryptionService.encrypt(credentials.getPassword()));
                account.setEncryptedTotpSecret(encryptionService.encrypt(credentials.getTotpSecret()));
                if (credentials.getBrokerUserId() != null) {
                    account.setBrokerUserId(credentials.getBrokerUserId());
                }
            }
            
            case ICICI_DIRECT -> {
                if (credentials.getBrokerUserId() == null || credentials.getPassword() == null) {
                    throw new IllegalArgumentException("ICICI Direct requires user ID and password");
                }
                account.setBrokerUserId(credentials.getBrokerUserId());
                account.setEncryptedPassword(encryptionService.encrypt(credentials.getPassword()));
            }
        }
        
        // Set common fields
        if (credentials.getAccountName() != null) {
            account.setAccountName(credentials.getAccountName());
        }
        if (credentials.getBrokerUsername() != null) {
            account.setBrokerUsername(credentials.getBrokerUsername());
        }
    }
    
    /**
     * Decrypt credentials based on broker type
     */
    private BrokerCredentials decryptCredentialsForBrokerType(BrokerAccount account, BrokerType brokerType) {
        BrokerCredentials.BrokerCredentialsBuilder builder = BrokerCredentials.builder()
                .brokerType(brokerType)
                .accountName(account.getAccountName())
                .brokerUserId(account.getBrokerUserId())
                .brokerUsername(account.getBrokerUsername());
        
        switch (brokerType) {
            case ZERODHA -> {
                builder.apiKey(encryptionService.decrypt(account.getEncryptedApiKey()));
                builder.apiSecret(encryptionService.decrypt(account.getEncryptedApiSecret()));
            }
            
            case UPSTOX -> {
                builder.clientId(account.getClientId());
                builder.apiSecret(encryptionService.decrypt(account.getEncryptedApiSecret()));
                builder.redirectUri(account.getRedirectUri());
            }
            
            case ANGEL_ONE -> {
                builder.apiKey(encryptionService.decrypt(account.getEncryptedApiKey()));
                builder.password(encryptionService.decrypt(account.getEncryptedPassword()));
                builder.totpSecret(encryptionService.decrypt(account.getEncryptedTotpSecret()));
            }
            
            case ICICI_DIRECT -> {
                builder.password(encryptionService.decrypt(account.getEncryptedPassword()));
            }
        }
        
        return builder.build();
    }
    
    /**
     * Broker Credentials DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BrokerCredentials {
        private BrokerType brokerType;
        private String accountName;
        private String brokerUserId;
        private String brokerUsername;
        
        // Common credential fields
        private String apiKey;
        private String apiSecret;
        private String password;
        private String totpSecret;
        
        // OAuth specific
        private String clientId;
        private String redirectUri;
        
        /**
         * Validate credentials for broker type
         */
        public boolean isValid() {
            if (brokerType == null) {
                return false;
            }
            
            return switch (brokerType) {
                case ZERODHA -> apiKey != null && apiSecret != null;
                case UPSTOX -> clientId != null && apiSecret != null;
                case ANGEL_ONE -> apiKey != null && password != null && totpSecret != null;
                case ICICI_DIRECT -> brokerUserId != null && password != null;
            };
        }
    }
}