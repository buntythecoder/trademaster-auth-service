package com.trademaster.auth.dto;

import com.trademaster.auth.entity.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration request DTO for new user signup
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Size(max = 3, message = "Country code must not exceed 3 characters")
    private String countryCode;

    private String timezone;

    private UserProfile.RiskTolerance riskTolerance;
    
    private java.time.LocalDate dateOfBirth;
    
    private String address;

    private UserProfile.TradingExperience tradingExperience;

    private boolean agreeToTerms;

    private boolean agreeToPrivacyPolicy;

    @Builder.Default
    private boolean subscribeToNewsletter = false;

    // Marketing and referral information
    private String referralCode;
    private String marketingSource;
}