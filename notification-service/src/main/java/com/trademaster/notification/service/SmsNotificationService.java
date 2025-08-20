package com.trademaster.notification.service;

import com.trademaster.notification.model.NotificationRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "true", matchIfMissing = true)
public class SmsNotificationService {
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;
    
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS service initialized");
    }
    
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public void sendSms(NotificationRequest request) {
        log.info("Sending SMS notification to: {}", request.getPhoneRecipient());
        
        try {
            Message message = Message.creator(
                new PhoneNumber(request.getPhoneRecipient()),
                new PhoneNumber(twilioPhoneNumber),
                request.getContent()
            ).create();
            
            log.info("SMS sent successfully to: {}, SID: {}", 
                    request.getPhoneRecipient(), message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}, error: {}", 
                     request.getPhoneRecipient(), e.getMessage(), e);
            throw e;
        }
    }
    
    // Pre-built SMS templates for common scenarios
    public NotificationRequest createWelcomeSms(String phoneNumber, String firstName) {
        String content = String.format(
            "Welcome to TradeMaster, %s! Your account is ready. Start trading at app.trademaster.com. Reply STOP to opt out.",
            firstName
        );
        
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.SMS)
            .phoneRecipient(phoneNumber)
            .recipient(phoneNumber)
            .subject("Welcome to TradeMaster")
            .content(content)
            .priority(NotificationRequest.Priority.MEDIUM)
            .referenceType("USER_REGISTRATION")
            .build();
    }
    
    public NotificationRequest createKycApprovalSms(String phoneNumber, String firstName) {
        String content = String.format(
            "Great news %s! Your KYC verification is approved. You can now start trading on TradeMaster. Visit app.trademaster.com",
            firstName
        );
        
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.SMS)
            .phoneRecipient(phoneNumber)
            .recipient(phoneNumber)
            .subject("KYC Approved")
            .content(content)
            .priority(NotificationRequest.Priority.HIGH)
            .referenceType("KYC_APPROVAL")
            .build();
    }
    
    public NotificationRequest createTradeExecutionSms(String phoneNumber, String firstName,
                                                      String symbol, String action, 
                                                      String quantity, String price) {
        String content = String.format(
            "Trade Alert: %s %s shares of %s at ₹%s. Check your portfolio at app.trademaster.com",
            action, quantity, symbol, price
        );
        
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.SMS)
            .phoneRecipient(phoneNumber)
            .recipient(phoneNumber)
            .subject("Trade Executed")
            .content(content)
            .priority(NotificationRequest.Priority.HIGH)
            .referenceType("TRADE_EXECUTION")
            .build();
    }
    
    public NotificationRequest createSecurityAlertSms(String phoneNumber, String firstName, 
                                                     String alertType) {
        String content = String.format(
            "SECURITY ALERT: %s detected on your TradeMaster account. If this wasn't you, secure your account immediately at app.trademaster.com/security",
            alertType
        );
        
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.SMS)
            .phoneRecipient(phoneNumber)
            .recipient(phoneNumber)
            .subject("Security Alert")
            .content(content)
            .priority(NotificationRequest.Priority.URGENT)
            .referenceType("SECURITY_ALERT")
            .build();
    }
    
    public NotificationRequest createOtpSms(String phoneNumber, String otp) {
        String content = String.format(
            "Your TradeMaster verification code is: %s. Valid for 10 minutes. Do not share this code with anyone.",
            otp
        );
        
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.SMS)
            .phoneRecipient(phoneNumber)
            .recipient(phoneNumber)
            .subject("Verification Code")
            .content(content)
            .priority(NotificationRequest.Priority.HIGH)
            .referenceType("OTP_VERIFICATION")
            .build();
    }
    
    public NotificationRequest createLowBalanceSms(String phoneNumber, String firstName, String balance) {
        String content = String.format(
            "Hi %s, your TradeMaster account balance is low (₹%s). Add funds to continue trading at app.trademaster.com",
            firstName, balance
        );
        
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.SMS)
            .phoneRecipient(phoneNumber)
            .recipient(phoneNumber)
            .subject("Low Balance Alert")
            .content(content)
            .priority(NotificationRequest.Priority.MEDIUM)
            .referenceType("BALANCE_ALERT")
            .build();
    }
}