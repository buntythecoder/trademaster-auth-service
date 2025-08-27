package com.trademaster.payment.entity;

import com.trademaster.payment.enums.PaymentGateway;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook Log Entity
 * 
 * Logs all incoming webhooks for debugging, compliance, and audit purposes.
 * Essential for troubleshooting payment gateway integration issues.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "webhook_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    // Webhook Details
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGateway gateway;
    
    @Size(max = 255)
    @Column(name = "webhook_id")
    private String webhookId;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    // Request Information
    @Type(JsonType.class)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, Object> requestHeaders;
    
    @Type(JsonType.class)
    @Column(name = "request_body", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> requestBody;
    
    @Size(max = 500)
    @Column(length = 500)
    private String signature;
    
    // Processing Information
    @Builder.Default
    @Column(name = "signature_verified", nullable = false)
    private Boolean signatureVerified = false;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean processed = false;
    
    @Builder.Default
    @Column(name = "processing_attempts", nullable = false)
    private Integer processingAttempts = 0;
    
    @Size(max = 1000)
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;
    
    // Response Information
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Size(max = 1000)
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    // Audit
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
    
    // Helper methods
    public void markAsProcessed(int responseStatus, String responseBody) {
        this.processed = true;
        this.processedAt = Instant.now();
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }
    
    public void markSignatureVerified(boolean verified) {
        this.signatureVerified = verified;
    }
    
    public void recordProcessingAttempt(String error) {
        this.processingAttempts++;
        this.processingError = error;
    }
    
    public boolean hasMaxAttemptsExceeded() {
        return processingAttempts >= 3;
    }
    
    public boolean isRetryable() {
        return !processed && !hasMaxAttemptsExceeded() && signatureVerified;
    }
    
    public boolean isSuccess() {
        return processed && responseStatus != null && responseStatus >= 200 && responseStatus < 300;
    }
    
    public String getProcessingStatus() {
        if (!signatureVerified) {
            return "SIGNATURE_FAILED";
        }
        if (processed && isSuccess()) {
            return "SUCCESS";
        }
        if (processed && !isSuccess()) {
            return "FAILED";
        }
        if (hasMaxAttemptsExceeded()) {
            return "MAX_RETRIES_EXCEEDED";
        }
        return "PENDING";
    }
    
    public long getProcessingTimeSeconds() {
        if (receivedAt == null || processedAt == null) {
            return 0;
        }
        return java.time.Duration.between(receivedAt, processedAt).getSeconds();
    }
    
    @SuppressWarnings("unchecked")
    public String getPaymentId() {
        if (requestBody == null) {
            return null;
        }
        
        return switch (gateway) {
            case RAZORPAY -> {
                Map<String, Object> payload = (Map<String, Object>) requestBody.get("payload");
                if (payload != null) {
                    Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
                    if (payment != null) {
                        Map<String, Object> entity = (Map<String, Object>) payment.get("entity");
                        if (entity != null) {
                            yield (String) entity.get("id");
                        }
                    }
                }
                yield null;
            }
            case STRIPE -> {
                Map<String, Object> data = (Map<String, Object>) requestBody.get("data");
                if (data != null) {
                    Map<String, Object> object = (Map<String, Object>) data.get("object");
                    if (object != null) {
                        yield (String) object.get("id");
                    }
                }
                yield null;
            }
            default -> null;
        };
    }
    
    @SuppressWarnings("unchecked")
    public String getOrderId() {
        if (requestBody == null || gateway != PaymentGateway.RAZORPAY) {
            return null;
        }
        
        Map<String, Object> payload = (Map<String, Object>) requestBody.get("payload");
        if (payload != null) {
            Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
            if (payment != null) {
                Map<String, Object> entity = (Map<String, Object>) payment.get("entity");
                if (entity != null) {
                    return (String) entity.get("order_id");
                }
            }
        }
        return null;
    }
    
    // Factory methods
    public static WebhookLog create(PaymentGateway gateway, String eventType, 
                                   Map<String, Object> headers, Map<String, Object> body, 
                                   String signature) {
        return WebhookLog.builder()
                .gateway(gateway)
                .eventType(eventType)
                .requestHeaders(headers)
                .requestBody(body)
                .signature(signature)
                .receivedAt(Instant.now())
                .build();
    }
    
    public static WebhookLog createRazorpayLog(String eventType, Map<String, Object> headers, 
                                              Map<String, Object> body, String signature) {
        return create(PaymentGateway.RAZORPAY, eventType, headers, body, signature);
    }
    
    public static WebhookLog createStripeLog(String eventType, Map<String, Object> headers, 
                                            Map<String, Object> body, String signature) {
        return create(PaymentGateway.STRIPE, eventType, headers, body, signature);
    }
}