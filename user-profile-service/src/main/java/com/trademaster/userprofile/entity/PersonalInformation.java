package com.trademaster.userprofile.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PersonalInformation(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth,
    
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid Indian mobile number")
    String mobileNumber,
    
    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be valid")
    String emailAddress,
    
    @NotNull(message = "Address is required")
    Address address,
    
    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN number must be valid (e.g., ABCDE1234F)")
    String panNumber,
    
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be 12 digits")
    String aadhaarNumber,
    
    String profilePhotoUrl,
    
    @NotBlank(message = "Occupation is required")
    String occupation,
    
    @NotBlank(message = "Annual income is required")
    String annualIncome,
    
    String educationLevel,
    
    String tradingExperience,
    
    String emergencyContactName,
    
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Emergency contact must be a valid Indian mobile number")
    String emergencyContactNumber,
    
    String maritalStatus,
    
    String nationality,
    
    String fatherName,
    
    String motherName
) {
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isKYCComplete() {
        return panNumber != null && !panNumber.trim().isEmpty() &&
               aadhaarNumber != null && !aadhaarNumber.trim().isEmpty();
    }
    
    public int getAge() {
        if (dateOfBirth == null) return 0;
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}