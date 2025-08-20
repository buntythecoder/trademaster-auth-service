package com.trademaster.userprofile.dto;

import com.trademaster.userprofile.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateProfileRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Personal information is required")
    @Valid
    private PersonalInformation personalInfo;
    
    @NotNull(message = "Trading preferences are required")
    @Valid
    private TradingPreferences tradingPreferences;
    
    @NotNull(message = "KYC information is required")
    @Valid
    private KYCInformation kycInfo;
    
    @NotNull(message = "Notification settings are required")
    @Valid
    private NotificationSettings notificationSettings;
}