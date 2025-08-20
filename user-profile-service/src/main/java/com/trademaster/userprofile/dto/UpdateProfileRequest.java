package com.trademaster.userprofile.dto;

import com.trademaster.userprofile.entity.*;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileRequest {
    
    @Valid
    private PersonalInformation personalInfo;
    
    @Valid
    private TradingPreferences tradingPreferences;
    
    @Valid
    private KYCInformation kycInfo;
    
    @Valid
    private NotificationSettings notificationSettings;
}