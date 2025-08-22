package com.trademaster.auth.service;

import com.trademaster.auth.entity.MfaConfiguration;
import com.trademaster.auth.entity.SecurityAuditLog;
import com.trademaster.auth.repository.MfaConfigurationRepository;
import com.trademaster.auth.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Multi-Factor Authentication Service
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final MfaConfigurationRepository mfaConfigurationRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;
    private final EncryptionService encryptionService;
    
    private static final int TOTP_WINDOW_SIZE = 1; // Allow 1 window before/after current
    private static final int TOTP_INTERVAL = 30; // 30 seconds
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int BACKUP_CODE_COUNT = 10;

    /**
     * Check if user has MFA enabled
     */
    public boolean isUserMfaEnabled(String userId) {
        log.debug("Checking MFA status for user: {}", userId);
        return mfaConfigurationRepository.countEnabledConfigurationsForUser(userId) > 0;
    }

    /**
     * Get all MFA configurations for user
     */
    public List<MfaConfiguration> getUserMfaConfigurations(String userId) {
        return mfaConfigurationRepository.findByUserId(userId);
    }

    /**
     * Get enabled MFA configurations for user
     */
    public List<MfaConfiguration> getEnabledMfaConfigurations(String userId) {
        return mfaConfigurationRepository.findEnabledConfigurationsForUser(userId);
    }

    /**
     * Setup TOTP MFA for user
     */
    @Transactional
    public MfaConfiguration setupTotpMfa(String userId, String sessionId) {
        log.info("Setting up TOTP MFA for user: {}", userId);
        
        // Check if TOTP is already configured
        Optional<MfaConfiguration> existing = mfaConfigurationRepository.findByUserIdAndMfaType(userId, MfaConfiguration.MfaType.TOTP);
        if (existing.isPresent()) {
            throw new IllegalStateException("TOTP MFA already configured for user");
        }
        
        // Generate secret key
        String secretKey = generateTotpSecret();
        String encryptedSecret = encryptionService.encrypt(secretKey);
        
        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();
        
        MfaConfiguration mfaConfig = MfaConfiguration.builder()
                .userId(userId)
                .mfaType(MfaConfiguration.MfaType.TOTP)
                .secretKey(encryptedSecret)
                .backupCodes(encryptBackupCodes(backupCodes))
                .enabled(false) // Will be enabled after verification
                .failedAttempts(0)
                .build();
        
        mfaConfig = mfaConfigurationRepository.save(mfaConfig);
        
        // Log setup event
        SecurityAuditLog auditLog = SecurityAuditLog.mfaEnabled(userId, sessionId, MfaConfiguration.MfaType.TOTP);
        securityAuditLogRepository.save(auditLog);
        
        log.info("TOTP MFA configuration created for user: {}", userId);
        return mfaConfig;
    }

    /**
     * Verify TOTP code and enable MFA
     */
    @Transactional
    public boolean verifyAndEnableTotp(String userId, String totpCode, String sessionId) {
        log.info("Verifying TOTP code for user: {}", userId);
        
        Optional<MfaConfiguration> configOpt = mfaConfigurationRepository.findByUserIdAndMfaType(userId, MfaConfiguration.MfaType.TOTP);
        if (configOpt.isEmpty()) {
            log.warn("No TOTP configuration found for user: {}", userId);
            return false;
        }
        
        MfaConfiguration config = configOpt.get();
        String decryptedSecret = encryptionService.decrypt(config.getSecretKey());
        
        if (verifyTotpCode(decryptedSecret, totpCode)) {
            config.enable();
            config.markAsUsed();
            mfaConfigurationRepository.save(config);
            
            // Log successful enablement
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("MFA_TOTP_ENABLED")
                    .description("TOTP MFA successfully enabled")
                    .riskLevel(SecurityAuditLog.RiskLevel.LOW)
                    .build();
            securityAuditLogRepository.save(auditLog);
            
            log.info("TOTP MFA enabled for user: {}", userId);
            return true;
        } else {
            config.incrementFailedAttempts();
            mfaConfigurationRepository.save(config);
            
            log.warn("Invalid TOTP code for user: {}", userId);
            return false;
        }
    }

    /**
     * Verify MFA code (TOTP or backup code)
     */
    @Transactional
    public boolean verifyMfaCode(String userId, String code, String sessionId) {
        log.info("Verifying MFA code for user: {}", userId);
        
        List<MfaConfiguration> configs = mfaConfigurationRepository.findEnabledConfigurationsForUser(userId);
        if (configs.isEmpty()) {
            log.warn("No enabled MFA configurations for user: {}", userId);
            return false;
        }
        
        for (MfaConfiguration config : configs) {
            if (config.isLocked()) {
                log.warn("MFA configuration is locked for user: {} (too many failed attempts)", userId);
                continue;
            }
            
            boolean verified = false;
            
            if (config.getMfaType() == MfaConfiguration.MfaType.TOTP) {
                String decryptedSecret = encryptionService.decrypt(config.getSecretKey());
                verified = verifyTotpCode(decryptedSecret, code) || verifyBackupCode(config, code);
            }
            
            if (verified) {
                config.markAsUsed();
                mfaConfigurationRepository.save(config);
                
                // Log successful verification
                SecurityAuditLog auditLog = SecurityAuditLog.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .eventType("MFA_VERIFICATION_SUCCESS")
                        .description("MFA code verified successfully")
                        .riskLevel(SecurityAuditLog.RiskLevel.LOW)
                        .build();
                securityAuditLogRepository.save(auditLog);
                
                log.info("MFA verification successful for user: {}", userId);
                return true;
            } else {
                config.incrementFailedAttempts();
                mfaConfigurationRepository.save(config);
            }
        }
        
        // Log failed verification
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("MFA_VERIFICATION_FAILED")
                .description("MFA code verification failed")
                .riskLevel(SecurityAuditLog.RiskLevel.MEDIUM)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        log.warn("MFA verification failed for user: {}", userId);
        return false;
    }

    /**
     * Disable MFA for user
     */
    @Transactional
    public void disableMfa(String userId, MfaConfiguration.MfaType mfaType, String sessionId) {
        log.info("Disabling MFA type {} for user: {}", mfaType, userId);
        
        Optional<MfaConfiguration> configOpt = mfaConfigurationRepository.findByUserIdAndMfaType(userId, mfaType);
        if (configOpt.isPresent()) {
            MfaConfiguration config = configOpt.get();
            config.disable();
            mfaConfigurationRepository.save(config);
            
            // Log disabling
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("MFA_DISABLED")
                    .description("MFA disabled for type: " + mfaType)
                    .riskLevel(SecurityAuditLog.RiskLevel.MEDIUM)
                    .build();
            securityAuditLogRepository.save(auditLog);
            
            log.info("MFA type {} disabled for user: {}", mfaType, userId);
        }
    }

    /**
     * Generate new backup codes
     */
    @Transactional
    public List<String> regenerateBackupCodes(String userId, MfaConfiguration.MfaType mfaType, String sessionId) {
        log.info("Regenerating backup codes for user: {} type: {}", userId, mfaType);
        
        Optional<MfaConfiguration> configOpt = mfaConfigurationRepository.findByUserIdAndMfaType(userId, mfaType);
        if (configOpt.isEmpty()) {
            throw new IllegalStateException("MFA configuration not found");
        }
        
        MfaConfiguration config = configOpt.get();
        List<String> newBackupCodes = generateBackupCodes();
        config.regenerateBackupCodes(encryptBackupCodes(newBackupCodes));
        mfaConfigurationRepository.save(config);
        
        // Log backup code regeneration
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("MFA_BACKUP_CODES_REGENERATED")
                .description("Backup codes regenerated")
                .riskLevel(SecurityAuditLog.RiskLevel.LOW)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        log.info("Backup codes regenerated for user: {}", userId);
        return newBackupCodes;
    }

    // Private helper methods
    
    private String generateTotpSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new java.util.ArrayList<>();
        
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(random.nextInt(10));
            }
            codes.add(code.toString());
        }
        
        return codes;
    }

    private List<String> encryptBackupCodes(List<String> codes) {
        return codes.stream()
                .map(encryptionService::encrypt)
                .toList();
    }

    private boolean verifyTotpCode(String secret, String code) {
        try {
            long currentTimeWindow = Instant.now().getEpochSecond() / TOTP_INTERVAL;
            
            // Check current window and adjacent windows
            for (int i = -TOTP_WINDOW_SIZE; i <= TOTP_WINDOW_SIZE; i++) {
                String expectedCode = generateTotpCode(secret, currentTimeWindow + i);
                if (expectedCode.equals(code)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }

    private String generateTotpCode(String secret, long timeWindow) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        byte[] timeBytes = java.nio.ByteBuffer.allocate(8).putLong(timeWindow).array();
        
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
        byte[] hash = mac.doFinal(timeBytes);
        
        int offset = hash[hash.length - 1] & 0x0f;
        int code = ((hash[offset] & 0x7f) << 24) |
                   ((hash[offset + 1] & 0xff) << 16) |
                   ((hash[offset + 2] & 0xff) << 8) |
                   (hash[offset + 3] & 0xff);
        
        code = code % 1000000;
        return String.format("%06d", code);
    }

    private boolean verifyBackupCode(MfaConfiguration config, String code) {
        if (config.getBackupCodes() == null) {
            return false;
        }
        
        for (String encryptedCode : config.getBackupCodes()) {
            String decryptedCode = encryptionService.decrypt(encryptedCode);
            if (decryptedCode.equals(code)) {
                // Remove used backup code
                List<String> remainingCodes = config.getBackupCodes().stream()
                        .filter(c -> !c.equals(encryptedCode))
                        .toList();
                config.regenerateBackupCodes(remainingCodes);
                return true;
            }
        }
        
        return false;
    }
}