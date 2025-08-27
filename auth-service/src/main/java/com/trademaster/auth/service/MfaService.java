package com.trademaster.auth.service;

import com.trademaster.auth.entity.MfaConfiguration;
import com.trademaster.auth.entity.SecurityAuditLog;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.repository.MfaConfigurationRepository;
import com.trademaster.auth.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final int TOTP_WINDOW_SIZE = 1; // Allow 1 window before/after current
    private static final int TOTP_INTERVAL = 30; // 30 seconds
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int BACKUP_CODE_COUNT = 10;

    /**
     * Check if user has MFA enabled using Result pattern
     */
    public Result<Boolean, String> isUserMfaEnabled(String userId) {
        return SafeOperations.safelyToResult(() -> {
            log.debug("Checking MFA status for user: {}", userId);
            return mfaConfigurationRepository.countEnabledConfigurationsForUser(userId) > 0;
        })
        .mapError(error -> {
            log.error("Error checking MFA status for user {}: {}", userId, error);
            return "Failed to check MFA status: " + error;
        });
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public boolean isUserMfaEnabledLegacy(String userId) {
        return isUserMfaEnabled(userId)
            .mapError(error -> {
                log.warn("Using legacy MFA check fallback due to error: {}", error);
                return false;
            })
            .orElse(false);
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
        Optional<MfaConfiguration> existing = mfaConfigurationRepository.findByUserIdAndMfaType(userId, MfaConfiguration.MfaType.TOTP);
        if (existing.isPresent()) {
            log.warn("TOTP MFA already configured for user: {}", userId);
            throw new IllegalStateException("TOTP MFA already configured for user");
        }
        
        try {
            return createNewTotpConfiguration(userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to setup TOTP MFA for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to setup TOTP MFA: " + e.getMessage());
        }
    }

    /**
     * Verify TOTP code and enable MFA
     */
    @Transactional
    public boolean verifyAndEnableTotp(String userId, String totpCode, String sessionId) {
        log.info("Verifying TOTP code for user: {}", userId);
        
        return mfaConfigurationRepository.findByUserIdAndMfaType(userId, MfaConfiguration.MfaType.TOTP)
            .map(config -> processToTotpVerification(config, userId, totpCode, sessionId))
            .orElseGet(() -> {
                log.warn("No TOTP configuration found for user: {}", userId);
                return false;
            });
    }

    /**
     * Verify MFA code (TOTP or backup code) using Result pattern
     */
    @Transactional
    public Result<Boolean, String> verifyMfaCode(String userId, String code, String sessionId) {
        return SafeOperations.safelyToResult(() -> {
            log.info("Verifying MFA code for user: {}", userId);
            
            List<MfaConfiguration> configs = mfaConfigurationRepository.findEnabledConfigurationsForUser(userId);
            
            return Optional.of(configs)
                .filter(list -> !list.isEmpty())
                .map(configList -> processConfigurationsVerification(configList, userId, code, sessionId))
                .orElseGet(() -> {
                    log.warn("No enabled MFA configurations for user: {}", userId);
                    return false;
                });
        })
        .mapError(error -> {
            log.error("Error verifying MFA code for user {}: {}", userId, error);
            return "MFA verification failed: " + error;
        });
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public boolean verifyMfaCodeLegacy(String userId, String code, String sessionId) {
        return verifyMfaCode(userId, code, sessionId)
            .mapError(error -> {
                log.warn("Using legacy MFA verification fallback due to error: {}", error);
                return false;
            })
            .orElse(false);
    }

    /**
     * Disable MFA for user
     */
    @Transactional
    public void disableMfa(String userId, MfaConfiguration.MfaType mfaType, String sessionId) {
        log.info("Disabling MFA type {} for user: {}", mfaType, userId);
        
        mfaConfigurationRepository.findByUserIdAndMfaType(userId, mfaType)
            .ifPresent(config -> {
                config.disable();
                mfaConfigurationRepository.save(config);
                
                SecurityAuditLog auditLog = SecurityAuditLog.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .eventType("MFA_DISABLED")
                        .description("MFA disabled for type: " + mfaType)
                        .riskLevel(SecurityAuditLog.RiskLevel.MEDIUM)
                        .build();
                securityAuditLogRepository.save(auditLog);
                
                log.info("MFA type {} disabled for user: {}", mfaType, userId);
            });
    }

    /**
     * Generate new backup codes
     */
    @Transactional
    public List<String> regenerateBackupCodes(String userId, MfaConfiguration.MfaType mfaType, String sessionId) {
        log.info("Regenerating backup codes for user: {} type: {}", userId, mfaType);
        
        return mfaConfigurationRepository.findByUserIdAndMfaType(userId, mfaType)
            .map(config -> processBackupCodeRegeneration(config, userId, sessionId))
            .orElseThrow(() -> new IllegalStateException("MFA configuration not found"));
    }

    // Private helper methods and functional support
    
    private MfaConfiguration createNewTotpConfiguration(String userId, String sessionId) {
        String secretKey = generateTotpSecret();
        String encryptedSecret = encryptionService.encrypt(secretKey).getValue();
        List<String> backupCodes = generateBackupCodes();
        
        MfaConfiguration mfaConfig = MfaConfiguration.builder()
                .userId(userId)
                .mfaType(MfaConfiguration.MfaType.TOTP)
                .secretKey(encryptedSecret)
                .backupCodes(encryptBackupCodes(backupCodes))
                .enabled(false)
                .failedAttempts(0)
                .build();
        
        MfaConfiguration saved = mfaConfigurationRepository.save(mfaConfig);
        
        SecurityAuditLog auditLog = SecurityAuditLog.mfaEnabled(userId, sessionId, MfaConfiguration.MfaType.TOTP);
        securityAuditLogRepository.save(auditLog);
        
        log.info("TOTP MFA configuration created for user: {}", userId);
        return saved;
    }
    
    private String generateTotpSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        
        return IntStream.range(0, BACKUP_CODE_COUNT)
            .mapToObj(i -> IntStream.range(0, BACKUP_CODE_LENGTH)
                .map(j -> random.nextInt(10))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining()))
            .collect(Collectors.toList());
    }

    private List<String> encryptBackupCodes(List<String> codes) {
        return codes.stream()
                .map(encryptionService::encrypt)
                .map(Result::getValue)
                .toList();
    }

    private boolean verifyTotpCode(String secret, String code) {
        return SafeOperations.safelyToResult(() -> {
            long currentTimeWindow = Instant.now().getEpochSecond() / TOTP_INTERVAL;
            
            return IntStream.rangeClosed(-TOTP_WINDOW_SIZE, TOTP_WINDOW_SIZE)
                .mapToObj(i -> {
                    try {
                        return generateTotpCode(secret, currentTimeWindow + i);
                    } catch (Exception e) {
                        return "";
                    }
                })
                .anyMatch(expectedCode -> expectedCode.equals(code));
        })
        .fold(
            isValid -> isValid,
            error -> {
                log.error("Error verifying TOTP code", error);
                return false;
            }
        );
    }

    private String generateTotpCode(String secret, long timeWindow) {
        return SafeOperations.safelyToResult(() -> {
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            byte[] timeBytes = java.nio.ByteBuffer.allocate(8).putLong(timeWindow).array();
            
            Mac mac;
            try {
                mac = Mac.getInstance("HmacSHA1");
                mac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("TOTP code generation failed: " + e.getMessage(), e);
            }
            
            byte[] hash = mac.doFinal(timeBytes);
            
            int offset = hash[hash.length - 1] & 0x0f;
            int code = ((hash[offset] & 0x7f) << 24) |
                       ((hash[offset + 1] & 0xff) << 16) |
                       ((hash[offset + 2] & 0xff) << 8) |
                       (hash[offset + 3] & 0xff);
            
            code = code % 1000000;
            return String.format("%06d", code);
        })
        .fold(
            totpCode -> totpCode,
            error -> {
                log.error("TOTP code generation failed: {}", error);
                return ""; // Return empty string on error
            }
        );
    }

    private boolean verifyBackupCode(MfaConfiguration config, String code) {
        return Optional.ofNullable(config.getBackupCodes())
            .filter(codes -> !codes.isEmpty())
            .flatMap(codes -> codes.stream()
                .filter(encryptedCode -> encryptionService.decrypt(encryptedCode).equals(code))
                .findFirst()
                .map(matchedCode -> {
                    List<String> remainingCodes = codes.stream()
                        .filter(c -> !c.equals(matchedCode))
                        .collect(Collectors.toList());
                    config.regenerateBackupCodes(remainingCodes);
                    return true;
                }))
            .orElse(false);
    }
    
    /**
     * Generate MFA challenge for user authentication
     */
    public String generateMfaChallenge(Long userId) {
        String userIdString = String.valueOf(userId);
        log.info("Generating MFA challenge for user: {}", userIdString);
        
        List<MfaConfiguration> configs = mfaConfigurationRepository.findEnabledConfigurationsForUser(userIdString);
        
        return Optional.of(configs)
            .filter(list -> !list.isEmpty())
            .map(list -> createMfaChallengeToken(userIdString))
            .orElseGet(() -> {
                log.warn("No enabled MFA configurations for user: {}", userIdString);
                return null;
            });
    }

    /**
     * Generate QR code URL for TOTP setup
     */
    public String generateQrCodeUrl(String userId, String secretKey) {
        return String.format("otpauth://totp/TradeMaster:%s?secret=%s&issuer=TradeMaster&algorithm=SHA1&digits=6&period=30", 
                userId, secretKey);
    }

    // Functional helper methods for MFA operations
    
    private boolean processToTotpVerification(MfaConfiguration config, String userId, String totpCode, String sessionId) {
        String decryptedSecret = encryptionService.decrypt(config.getSecretKey()).getValue();
        
        return Optional.of(verifyTotpCode(decryptedSecret, totpCode))
            .filter(verified -> verified)
            .map(verified -> {
                config.enable();
                config.markAsUsed();
                mfaConfigurationRepository.save(config);
                
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
            })
            .orElseGet(() -> {
                config.incrementFailedAttempts();
                mfaConfigurationRepository.save(config);
                log.warn("Invalid TOTP code for user: {}", userId);
                return false;
            });
    }
    
    private boolean processConfigurationsVerification(List<MfaConfiguration> configs, String userId, String code, String sessionId) {
        boolean verified = configs.stream()
            .filter(config -> !config.isLocked())
            .anyMatch(config -> {
                boolean configVerified = Optional.of(config.getMfaType())
                    .filter(type -> type == MfaConfiguration.MfaType.TOTP)
                    .map(type -> {
                        String decryptedSecret = encryptionService.decrypt(config.getSecretKey()).getValue();
                        return verifyTotpCode(decryptedSecret, code) || verifyBackupCode(config, code);
                    })
                    .orElse(false);
                
                Optional.of(configVerified)
                    .filter(v -> v)
                    .ifPresentOrElse(
                        v -> {
                            config.markAsUsed();
                            mfaConfigurationRepository.save(config);
                        },
                        () -> {
                            config.incrementFailedAttempts();
                            mfaConfigurationRepository.save(config);
                        }
                    );
                    
                return configVerified;
            });
            
        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType(verified ? "MFA_VERIFICATION_SUCCESS" : "MFA_VERIFICATION_FAILED")
                .description(verified ? "MFA code verified successfully" : "MFA code verification failed")
                .riskLevel(verified ? SecurityAuditLog.RiskLevel.LOW : SecurityAuditLog.RiskLevel.MEDIUM)
                .build();
        securityAuditLogRepository.save(auditLog);
        
        Optional.of(verified)
            .ifPresentOrElse(
                v -> log.info("MFA verification successful for user: {}", userId),
                () -> log.warn("MFA verification failed for user: {}", userId)
            );
        
        return verified;
    }
    
    private List<String> processBackupCodeRegeneration(MfaConfiguration config, String userId, String sessionId) {
        List<String> newBackupCodes = generateBackupCodes();
        config.regenerateBackupCodes(encryptBackupCodes(newBackupCodes));
        mfaConfigurationRepository.save(config);
        
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
    
    private String createMfaChallengeToken(String userIdString) {
        String challengeToken = java.util.UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        String challengeKey = String.format("MFA_CHALLENGE_%s_%s_%s", userIdString, challengeToken, timestamp);
        redisTemplate.opsForValue().set(challengeKey, challengeToken, Duration.ofMinutes(5));
        
        log.debug("MFA challenge created for user", 
            StructuredArguments.kv("userId", userIdString),
            StructuredArguments.kv("challengeKey", challengeKey));
        
        return challengeKey;
    }
}