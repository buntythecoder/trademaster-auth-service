package com.trademaster.userprofile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

/**
 * Profile Validation Service - Single Responsibility Pattern
 * 
 * MANDATORY: SOLID Principles Enforcement - Rule #2
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Cognitive Complexity Control - Rule #5
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileValidationService {

    /**
     * Validate user ID
     */
    public Optional<String> validateUserId(UUID userId) {
        return userId == null ? Optional.of("User ID is required") : Optional.empty();
    }
    
    /**
     * Validate personal information
     */
    public Optional<String> validatePersonalInfo(Map<String, Object> personalInfo) {
        if (personalInfo == null || personalInfo.isEmpty()) {
            return Optional.of("Personal information is required");
        }
        
        List<String> requiredFields = List.of("firstName", "lastName", "email", "mobileNumber");
        List<String> missingFields = requiredFields.stream()
            .filter(field -> !personalInfo.containsKey(field) || personalInfo.get(field) == null)
            .toList();
        
        return missingFields.isEmpty() 
            ? Optional.empty() 
            : Optional.of("Missing required fields: " + String.join(", ", missingFields));
    }
    
    /**
     * Validate trading preferences
     */
    public Optional<String> validateTradingPreferences(Map<String, Object> tradingPreferences) {
        return tradingPreferences == null ? Optional.of("Trading preferences are required") : Optional.empty();
    }
    
    /**
     * Validate KYC information
     */
    public Optional<String> validateKycInformationOptional(Map<String, Object> kycInformation) {
        return kycInformation == null ? Optional.of("KYC information is required") : Optional.empty();
    }
    
    /**
     * Validate notification settings
     */
    public Optional<String> validateNotificationSettings(Map<String, Object> notificationSettings) {
        return notificationSettings == null ? Optional.of("Notification settings are required") : Optional.empty();
    }
}