package com.trademaster.userprofile.service;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.service.UserProfileService.ProfileError;
import com.trademaster.userprofile.service.UserProfileService.ValidationError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Profile Business Logic Service - Single Responsibility Pattern
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
public class ProfileBusinessLogicService {

    /**
     * Create risk level filter using functional composition
     */
    public Predicate<UserProfile> createRiskLevelFilter(String riskLevel) {
        return profile -> riskLevel.equals(profile.getRiskLevel().name());
    }
    
    /**
     * Create search filter with functional composition
     */
    public Predicate<UserProfile> createSearchFilter(String searchTerm) {
        String lowerTerm = searchTerm.toLowerCase();
        return profile -> {
            Map<String, Object> personalInfo = profile.getPersonalInfo();
            if (personalInfo == null) return false;
            
            return personalInfo.values().stream()
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .anyMatch(value -> value.toLowerCase().contains(lowerTerm));
        };
    }
    
    /**
     * Profile activation marker using functional update
     */
    public Function<UserProfile, UserProfile> createProfileActivator() {
        return profile -> {
            Map<String, Object> updatedInfo = new HashMap<>(profile.getPersonalInfo());
            updatedInfo.put("status", "ACTIVE");
            updatedInfo.put("activatedAt", LocalDateTime.now());
            return profile.withPersonalInfo(updatedInfo);
        };
    }
    
    /**
     * Validate profile for activation
     */
    public Result<UserProfile, ProfileError> validateProfileForActivation(UserProfile profile) {
        return profile.isKycCompleted() 
            ? Result.success(profile)
            : Result.failure(new ValidationError("KYC must be completed before activation", List.of()));
    }
}