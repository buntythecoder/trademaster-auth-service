package com.trademaster.payment.entity;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * User Payment Method Entity
 * 
 * Stores tokenized payment methods for users (PCI DSS compliant).
 * No sensitive payment data is stored directly.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class UserPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    // Tokenized Payment Details (No sensitive data stored)
    @NotBlank
    @Size(max = 255)
    @Column(name = "payment_method_token", nullable = false)
    private String paymentMethodToken;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", nullable = false)
    private PaymentMethod paymentMethodType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_provider", nullable = false)
    private PaymentGateway gatewayProvider;
    
    // Display Information Only (No sensitive data)
    @Size(max = 100)
    @Column(name = "display_name")
    private String displayName;
    
    @Size(min = 4, max = 4)
    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;
    
    @Min(1)
    @Max(12)
    @Column(name = "expiry_month")
    private Integer expiryMonth;
    
    @Min(2024)
    @Column(name = "expiry_year")
    private Integer expiryYear;
    
    @Size(max = 50)
    @Column(length = 50)
    private String brand; // Visa, Mastercard, etc.
    
    // Convenience aliases for compatibility
    @Size(max = 50)
    @Column(name = "card_brand", length = 50)
    private String cardBrand; // Same as brand
    
    @Size(max = 255)
    @Column(name = "cardholder_name")
    private String cardholderName; // Encrypted
    
    @Size(max = 1000)
    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress; // Encrypted
    
    // Status flags
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    // Additional metadata
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Helper methods
    public boolean isExpired() {
        if (expiryMonth == null || expiryYear == null) {
            return false;
        }
        
        Instant now = Instant.now();
        java.time.LocalDate currentDate = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        java.time.LocalDate expiryDate = java.time.LocalDate.of(expiryYear, expiryMonth, 1)
                .plusMonths(1).minusDays(1); // Last day of expiry month
        
        return currentDate.isAfter(expiryDate);
    }
    
    public boolean isCard() {
        return paymentMethodType == PaymentMethod.CARD;
    }
    
    public boolean isUpi() {
        return paymentMethodType == PaymentMethod.UPI;
    }
    
    public String getMaskedDisplay() {
        if (lastFourDigits != null) {
            return String.format("**** **** **** %s", lastFourDigits);
        }
        return displayName != null ? displayName : paymentMethodType.getDisplayName();
    }
    
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }
    
    public void setAsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public void deactivate() {
        this.isActive = false;
    }
    
    public boolean canBeUsedForPayment() {
        return isActive && isVerified && !isExpired();
    }
}