package com.trademaster.userprofile.service;

import com.trademaster.userprofile.dto.CreateProfileRequest;
import com.trademaster.userprofile.dto.UpdateProfileRequest;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.exception.ValidationException;
import com.trademaster.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileValidationService {
    
    private final UserProfileRepository userProfileRepository;
    
    // Validation patterns
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern PIN_CODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    
    /**
     * Validate create profile request
     */
    public void validateCreateRequest(CreateProfileRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Validate personal information
        validatePersonalInformation(request.getPersonalInfo(), errors);
        
        // Validate trading preferences
        validateTradingPreferences(request.getTradingPreferences(), request.getPersonalInfo(), errors);
        
        // Validate KYC information
        validateKycInformation(request.getKycInfo(), errors);
        
        // Validate notification settings
        validateNotificationSettings(request.getNotificationSettings(), errors);
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }
    
    /**
     * Validate update profile request
     */
    public void validateUpdateRequest(UpdateProfileRequest request, UserProfile existingProfile) {
        List<String> errors = new ArrayList<>();
        
        if (request.getPersonalInfo() != null) {
            validatePersonalInformation(request.getPersonalInfo(), errors);
            
            // Check if critical fields are being changed (may require additional verification)
            validateCriticalFieldChanges(request.getPersonalInfo(), existingProfile.getPersonalInfo(), errors);
        }
        
        if (request.getTradingPreferences() != null) {
            PersonalInformation personalInfo = request.getPersonalInfo() != null 
                ? request.getPersonalInfo() 
                : existingProfile.getPersonalInfo();
            validateTradingPreferences(request.getTradingPreferences(), personalInfo, errors);
        }
        
        if (request.getKycInfo() != null) {
            validateKycInformation(request.getKycInfo(), errors);
        }
        
        if (request.getNotificationSettings() != null) {
            validateNotificationSettings(request.getNotificationSettings(), errors);
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }
    
    /**
     * Validate unique constraints (PAN, mobile, email)
     */
    public void validateUniqueConstraints(CreateProfileRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Check PAN uniqueness
        String panNumber = request.getPersonalInfo().panNumber();
        if (panNumber != null && !panNumber.trim().isEmpty()) {
            Optional<UserProfile> existingPan = userProfileRepository.findByPanNumber(panNumber);
            if (existingPan.isPresent()) {
                errors.add("PAN number already exists in the system");
            }
        }
        
        // Check mobile uniqueness (warn, don't block - family members might share)
        String mobile = request.getPersonalInfo().mobileNumber();
        if (mobile != null && !mobile.trim().isEmpty()) {
            List<UserProfile> existingMobile = userProfileRepository.findByMobileNumber(mobile);
            if (existingMobile.size() >= 3) { // Allow max 3 accounts per mobile
                errors.add("Mobile number is associated with too many accounts");
            }
        }
        
        // Check email uniqueness (warn, don't block)
        String email = request.getPersonalInfo().emailAddress();
        if (email != null && !email.trim().isEmpty()) {
            List<UserProfile> existingEmail = userProfileRepository.findByEmailAddress(email);
            if (existingEmail.size() >= 5) { // Allow max 5 accounts per email
                errors.add("Email address is associated with too many accounts");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Duplicate data validation failed", errors);
        }
    }
    
    /**
     * Validate personal information
     */
    public void validatePersonalInformation(PersonalInformation personalInfo) {
        List<String> errors = new ArrayList<>();
        validatePersonalInformation(personalInfo, errors);
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Personal information validation failed", errors);
        }
    }
    
    /**
     * Validate trading preferences
     */
    public void validateTradingPreferences(TradingPreferences preferences, PersonalInformation personalInfo) {
        List<String> errors = new ArrayList<>();
        validateTradingPreferences(preferences, personalInfo, errors);
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Trading preferences validation failed", errors);
        }
    }
    
    /**
     * Validate KYC information
     */
    public void validateKycInformation(KYCInformation kycInfo) {
        List<String> errors = new ArrayList<>();
        validateKycInformation(kycInfo, errors);
        
        if (!errors.isEmpty()) {
            throw new ValidationException("KYC information validation failed", errors);
        }
    }
    
    // Private validation methods
    
    private void validatePersonalInformation(PersonalInformation personalInfo, List<String> errors) {
        if (personalInfo == null) {
            errors.add("Personal information is required");
            return;
        }
        
        // Validate required fields
        if (personalInfo.firstName() == null || personalInfo.firstName().trim().isEmpty()) {
            errors.add("First name is required");
        } else if (personalInfo.firstName().length() > 50) {
            errors.add("First name cannot exceed 50 characters");
        }
        
        if (personalInfo.lastName() == null || personalInfo.lastName().trim().isEmpty()) {
            errors.add("Last name is required");
        } else if (personalInfo.lastName().length() > 50) {
            errors.add("Last name cannot exceed 50 characters");
        }
        
        // Validate date of birth
        if (personalInfo.dateOfBirth() == null) {
            errors.add("Date of birth is required");
        } else {
            LocalDate dob = personalInfo.dateOfBirth();
            LocalDate now = LocalDate.now();
            
            if (dob.isAfter(now)) {
                errors.add("Date of birth cannot be in the future");
            }
            
            int age = Period.between(dob, now).getYears();
            if (age < 18) {
                errors.add("User must be at least 18 years old");
            } else if (age > 100) {
                errors.add("Invalid date of birth - age cannot exceed 100 years");
            }
        }
        
        // Validate mobile number
        if (personalInfo.mobileNumber() == null || personalInfo.mobileNumber().trim().isEmpty()) {
            errors.add("Mobile number is required");
        } else if (!MOBILE_PATTERN.matcher(personalInfo.mobileNumber()).matches()) {
            errors.add("Mobile number must be a valid Indian mobile number (10 digits starting with 6-9)");
        }
        
        // Validate email
        if (personalInfo.emailAddress() == null || personalInfo.emailAddress().trim().isEmpty()) {
            errors.add("Email address is required");
        } else if (!isValidEmail(personalInfo.emailAddress())) {
            errors.add("Email address format is invalid");
        }
        
        // Validate PAN
        if (personalInfo.panNumber() == null || personalInfo.panNumber().trim().isEmpty()) {
            errors.add("PAN number is required");
        } else if (!PAN_PATTERN.matcher(personalInfo.panNumber().toUpperCase()).matches()) {
            errors.add("PAN number format is invalid (should be like ABCDE1234F)");
        }
        
        // Validate Aadhaar (optional but if provided, must be valid)
        if (personalInfo.aadhaarNumber() != null && !personalInfo.aadhaarNumber().trim().isEmpty()) {
            if (!AADHAAR_PATTERN.matcher(personalInfo.aadhaarNumber()).matches()) {
                errors.add("Aadhaar number must be 12 digits");
            }
        }
        
        // Validate address
        validateAddress(personalInfo.address(), errors);
    }
    
    private void validateAddress(Address address, List<String> errors) {
        if (address == null) {
            errors.add("Address is required");
            return;
        }
        
        if (address.addressLine1() == null || address.addressLine1().trim().isEmpty()) {
            errors.add("Address line 1 is required");
        }
        
        if (address.city() == null || address.city().trim().isEmpty()) {
            errors.add("City is required");
        }
        
        if (address.state() == null || address.state().trim().isEmpty()) {
            errors.add("State is required");
        }
        
        if (address.pinCode() == null || address.pinCode().trim().isEmpty()) {
            errors.add("PIN code is required");
        } else if (!PIN_CODE_PATTERN.matcher(address.pinCode()).matches()) {
            errors.add("PIN code must be a valid 6-digit Indian PIN code");
        }
        
        if (address.country() == null || address.country().trim().isEmpty()) {
            errors.add("Country is required");
        }
    }
    
    private void validateTradingPreferences(TradingPreferences preferences, PersonalInformation personalInfo, List<String> errors) {
        if (preferences == null) {
            errors.add("Trading preferences are required");
            return;
        }
        
        // Validate preferred segments
        if (preferences.preferredSegments() == null || preferences.preferredSegments().isEmpty()) {
            errors.add("At least one trading segment must be selected");
        }
        
        // Validate risk profile
        validateRiskProfile(preferences.riskProfile(), personalInfo, errors);
        
        // Validate default order settings
        validateDefaultOrderSettings(preferences.defaultOrderSettings(), errors);
        
        // Validate segment-risk compatibility
        validateSegmentRiskCompatibility(preferences, errors);
    }
    
    private void validateRiskProfile(RiskProfile riskProfile, PersonalInformation personalInfo, List<String> errors) {
        if (riskProfile == null) {
            errors.add("Risk profile is required");
            return;
        }
        
        if (riskProfile.riskLevel() == null) {
            errors.add("Risk level is required");
        }
        
        if (riskProfile.riskToleranceScore() == null) {
            errors.add("Risk tolerance score is required");
        } else {
            int score = riskProfile.riskToleranceScore();
            if (score < 1 || score > 10) {
                errors.add("Risk tolerance score must be between 1 and 10");
            }
            
            // Validate score matches risk level
            if (riskProfile.riskLevel() != null && !riskProfile.riskLevel().isValidScore(score)) {
                errors.add("Risk tolerance score doesn't match selected risk level");
            }
        }
        
        if (riskProfile.investmentHorizon() == null) {
            errors.add("Investment horizon is required");
        }
        
        // Validate age-appropriate risk level
        if (personalInfo != null && personalInfo.dateOfBirth() != null) {
            int age = Period.between(personalInfo.dateOfBirth(), LocalDate.now()).getYears();
            
            if (age > 60 && riskProfile.riskLevel() == RiskLevel.HIGH) {
                errors.add("High risk profile may not be suitable for users over 60. Please confirm your risk tolerance.");
            }
            
            if (age < 25 && riskProfile.investmentHorizon() == InvestmentHorizon.SHORT_TERM) {
                // This is just a warning, not a hard error
                log.warn("Young investor choosing short-term horizon: userId might benefit from long-term investment strategy");
            }
        }
    }
    
    private void validateDefaultOrderSettings(DefaultOrderSettings settings, List<String> errors) {
        if (settings == null) {
            errors.add("Default order settings are required");
            return;
        }
        
        if (settings.defaultOrderType() == null) {
            errors.add("Default order type is required");
        }
        
        if (settings.defaultOrderValidity() == null) {
            errors.add("Default order validity is required");
        }
        
        if (settings.defaultQuantity() != null && settings.defaultQuantity() <= 0) {
            errors.add("Default quantity must be greater than 0");
        }
        
        if (settings.defaultAmount() != null && settings.defaultAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Default amount must be greater than 0");
        }
        
        // Validate stop loss settings
        if (Boolean.TRUE.equals(settings.stopLossEnabled())) {
            if (settings.stopLossPercentage() == null) {
                errors.add("Stop loss percentage is required when stop loss is enabled");
            } else if (settings.stopLossPercentage().compareTo(java.math.BigDecimal.ZERO) <= 0 || 
                      settings.stopLossPercentage().compareTo(java.math.BigDecimal.valueOf(50)) > 0) {
                errors.add("Stop loss percentage must be between 0.01% and 50%");
            }
        }
        
        // Validate take profit settings
        if (Boolean.TRUE.equals(settings.takeProfitEnabled())) {
            if (settings.takeProfitPercentage() == null) {
                errors.add("Take profit percentage is required when take profit is enabled");
            } else if (settings.takeProfitPercentage().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                errors.add("Take profit percentage must be greater than 0");
            }
        }
    }
    
    private void validateSegmentRiskCompatibility(TradingPreferences preferences, List<String> errors) {
        if (preferences.preferredSegments() == null || preferences.riskProfile() == null) {
            return;
        }
        
        boolean hasHighRiskSegments = preferences.preferredSegments().stream()
            .anyMatch(TradingSegment::requiresHighRiskProfile);
            
        if (hasHighRiskSegments && preferences.riskProfile().riskLevel() != RiskLevel.HIGH) {
            errors.add("Derivatives trading (F&O, Commodity, Currency) requires High risk profile");
        }
    }
    
    private void validateKycInformation(KYCInformation kycInfo, List<String> errors) {
        if (kycInfo == null) {
            errors.add("KYC information is required");
            return;
        }
        
        if (kycInfo.kycStatus() == null) {
            errors.add("KYC status is required");
        }
        
        // Additional KYC validations based on status
        if (kycInfo.kycStatus() == KYCStatus.VERIFIED) {
            if (!kycInfo.panVerified()) {
                errors.add("PAN verification is required for verified KYC status");
            }
            if (!kycInfo.aadhaarVerified()) {
                errors.add("Aadhaar verification is required for verified KYC status");
            }
            if (!kycInfo.documentsVerified()) {
                errors.add("Document verification is required for verified KYC status");
            }
        }
        
        // Validate bank account information if provided
        if (kycInfo.bankAccountVerified() && (kycInfo.bankAccountNumber() == null || kycInfo.ifscCode() == null)) {
            errors.add("Bank account number and IFSC code are required for verified bank account");
        }
    }
    
    private void validateNotificationSettings(NotificationSettings settings, List<String> errors) {
        if (settings == null) {
            errors.add("Notification settings are required");
            return;
        }
        
        // Validate quiet hours format
        if (settings.quietHoursEnabled()) {
            if (settings.quietHoursStart() == null || settings.quietHoursEnd() == null) {
                errors.add("Quiet hours start and end times are required when quiet hours are enabled");
            } else {
                try {
                    java.time.LocalTime.parse(settings.quietHoursStart());
                    java.time.LocalTime.parse(settings.quietHoursEnd());
                } catch (Exception e) {
                    errors.add("Quiet hours must be in HH:mm format");
                }
            }
        }
    }
    
    private void validateCriticalFieldChanges(PersonalInformation newInfo, PersonalInformation oldInfo, List<String> errors) {
        // Check if PAN is being changed (usually not allowed)
        if (!newInfo.panNumber().equals(oldInfo.panNumber())) {
            errors.add("PAN number cannot be changed. Please contact support if this is incorrect.");
        }
        
        // Check if date of birth is being changed (requires verification)
        if (!newInfo.dateOfBirth().equals(oldInfo.dateOfBirth())) {
            log.warn("Date of birth change requested - may require additional verification");
        }
        
        // Check if name is being changed significantly (requires verification)
        if (!newInfo.firstName().equalsIgnoreCase(oldInfo.firstName()) || 
            !newInfo.lastName().equalsIgnoreCase(oldInfo.lastName())) {
            log.warn("Name change requested - may require additional verification");
        }
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".") && 
               email.length() > 5 && email.length() < 100;
    }
}