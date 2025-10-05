package com.trademaster.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MfaSetupResponse {
    private String mfaType;
    private String secretKey;
    private String qrCodeUrl;
    private List<String> backupCodes;
    private boolean enabled;
    private String message;
    
    public static MfaSetupResponse success(String mfaType, String secretKey, String qrCodeUrl, List<String> backupCodes) {
        return MfaSetupResponse.builder()
                .mfaType(mfaType)
                .secretKey(secretKey)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .enabled(false)
                .message("MFA setup initiated. Please verify with your authenticator app.")
                .build();
    }
    
    public static MfaSetupResponse enabled(String mfaType) {
        return MfaSetupResponse.builder()
                .mfaType(mfaType)
                .enabled(true)
                .message("MFA successfully enabled.")
                .build();
    }
}