package com.trademaster.userprofile.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record Address(
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 100, message = "Address line 1 cannot exceed 100 characters")
    String addressLine1,
    
    @Size(max = 100, message = "Address line 2 cannot exceed 100 characters")
    String addressLine2,
    
    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City name cannot exceed 50 characters")
    String city,
    
    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State name cannot exceed 50 characters")
    String state,
    
    @NotBlank(message = "PIN code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian PIN code")
    String pinCode,
    
    @NotBlank(message = "Country is required")
    @Size(max = 50, message = "Country name cannot exceed 50 characters")
    String country
) {
    
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        fullAddress.append(addressLine1);
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            fullAddress.append(", ").append(addressLine2);
        }
        
        fullAddress.append(", ").append(city)
                   .append(", ").append(state)
                   .append(" - ").append(pinCode)
                   .append(", ").append(country);
                   
        return fullAddress.toString();
    }
    
    public boolean isIndia() {
        return "India".equalsIgnoreCase(country) || "IN".equalsIgnoreCase(country);
    }
}