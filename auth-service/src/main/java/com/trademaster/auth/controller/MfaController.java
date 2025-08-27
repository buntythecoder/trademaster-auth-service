package com.trademaster.auth.controller;

import com.trademaster.auth.dto.MfaSetupResponse;
import com.trademaster.auth.dto.MfaVerificationRequest;
import com.trademaster.auth.entity.MfaConfiguration;
import com.trademaster.auth.service.MfaService;
import com.trademaster.auth.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Multi-Factor Authentication", description = "MFA setup and verification endpoints")
public class MfaController {

    private final MfaService mfaService;
    private final SecurityAuditService securityAuditService;

    @PostMapping("/setup")
    @Operation(summary = "Setup TOTP MFA", description = "Initialize TOTP MFA setup for the authenticated user")
    public ResponseEntity<MfaSetupResponse> setupMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        try {
            MfaConfiguration config = mfaService.setupTotpMfa(userId, sessionId);
            
            String qrCodeUrl = mfaService.generateQrCodeUrl(userId, config.getSecretKey());
            
            MfaSetupResponse response = MfaSetupResponse.success(
                    config.getMfaType().toString(),
                    config.getSecretKey(),
                    qrCodeUrl,
                    List.of() // Backup codes should be encrypted, return empty for security
            );
            
            securityAuditService.logMfaEvent(userId, "SETUP_INITIATED", "SUCCESS", 
                request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(MfaSetupResponse.builder()
                            .message("MFA already configured. Disable existing MFA first.")
                            .build());
        }
    }

    @PostMapping("/verify-setup")
    @Operation(summary = "Verify MFA Setup", description = "Verify and enable MFA with TOTP code")
    public ResponseEntity<MfaSetupResponse> verifyMfaSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = userDetails.getUsername();
        String sessionId = httpRequest.getSession().getId();
        
        boolean verified = mfaService.verifyAndEnableTotp(userId, request.getCode(), sessionId);
        
        if (verified) {
            securityAuditService.logMfaEvent(userId, "SETUP_COMPLETED", "SUCCESS", 
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(MfaSetupResponse.enabled("TOTP"));
        } else {
            securityAuditService.logMfaEvent(userId, "SETUP_FAILED", "FAILURE", 
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            return ResponseEntity.badRequest()
                    .body(MfaSetupResponse.builder()
                            .message("Invalid MFA code. Please try again.")
                            .build());
        }
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify MFA Code", description = "Verify MFA code during authentication")
    public ResponseEntity<Map<String, Object>> verifyMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = userDetails.getUsername();
        String sessionId = httpRequest.getSession().getId();
        
        boolean verified = mfaService.verifyMfaCode(userId, request.getCode(), sessionId).getValue();
        
        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "message", "MFA verification successful"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "message", "Invalid MFA code"
            ));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get MFA Status", description = "Get current MFA configuration status")
    public ResponseEntity<Map<String, Object>> getMfaStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        List<MfaConfiguration> configs = mfaService.getUserMfaConfigurations(userId);
        
        boolean hasMfa = mfaService.isUserMfaEnabled(userId).getValue();
        
        return ResponseEntity.ok(Map.of(
                "enabled", hasMfa,
                "configurations", configs.stream()
                        .map(config -> Map.of(
                                "type", config.getMfaType(),
                                "enabled", config.isEnabled(),
                                "lastUsed", config.getLastUsed()
                        ))
                        .toList()
        ));
    }

    @PostMapping("/regenerate-backup-codes")
    @Operation(summary = "Regenerate Backup Codes", description = "Generate new backup codes for MFA")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        try {
            List<String> backupCodes = mfaService.regenerateBackupCodes(userId, MfaConfiguration.MfaType.TOTP, sessionId);
            
            securityAuditService.logMfaEvent(userId, "BACKUP_CODES_REGENERATED", "SUCCESS", 
                request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            return ResponseEntity.ok(Map.of(
                    "backupCodes", backupCodes,
                    "message", "Backup codes regenerated successfully"
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "MFA not configured"
            ));
        }
    }

    @DeleteMapping("/disable")
    @Operation(summary = "Disable MFA", description = "Disable MFA for the authenticated user")
    public ResponseEntity<Map<String, Object>> disableMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String mfaType,
            HttpServletRequest request) {
        
        String userId = userDetails.getUsername();
        String sessionId = request.getSession().getId();
        
        try {
            MfaConfiguration.MfaType type = MfaConfiguration.MfaType.valueOf(mfaType.toUpperCase());
            mfaService.disableMfa(userId, type, sessionId);
            
            securityAuditService.logMfaEvent(userId, "DISABLED", "SUCCESS", 
                request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            return ResponseEntity.ok(Map.of(
                    "message", "MFA disabled successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid MFA type"
            ));
        }
    }
}