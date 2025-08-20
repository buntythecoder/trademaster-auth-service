package com.trademaster.notification.service;

import com.trademaster.notification.model.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationService {
    
    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    
    @Value("${notification.email.default-sender}")
    private String defaultSender;
    
    @Value("${notification.email.templates-path}")
    private String templatesPath;
    
    @Retryable(
        retryFor = {MailException.class, MessagingException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public void sendEmail(NotificationRequest request) throws MessagingException {
        log.info("Sending email notification to: {}, subject: {}", 
                request.getEmailRecipient(), request.getSubject());
        
        try {
            if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
                sendTemplatedEmail(request);
            } else {
                sendSimpleEmail(request);
            }
            log.info("Email sent successfully to: {}", request.getEmailRecipient());
        } catch (Exception e) {
            log.error("Failed to send email to: {}, error: {}", 
                     request.getEmailRecipient(), e.getMessage(), e);
            throw e;
        }
    }
    
    private void sendSimpleEmail(NotificationRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultSender);
        message.setTo(request.getEmailRecipient());
        message.setSubject(request.getSubject());
        message.setText(request.getContent());
        
        emailSender.send(message);
    }
    
    private void sendTemplatedEmail(NotificationRequest request) throws MessagingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setFrom(defaultSender);
        helper.setTo(request.getEmailRecipient());
        helper.setSubject(request.getSubject());
        
        // Process template with variables
        String htmlContent = processTemplate(request.getTemplateName(), request.getTemplateVariables());
        helper.setText(htmlContent, true);
        
        emailSender.send(mimeMessage);
    }
    
    private String processTemplate(String templateName, String variablesJson) {
        try {
            Context context = new Context();
            
            if (variablesJson != null && !variablesJson.isEmpty()) {
                Map<String, Object> variables = objectMapper.readValue(
                    variablesJson, new TypeReference<Map<String, Object>>() {}
                );
                context.setVariables(variables);
            }
            
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Failed to process email template: {}, error: {}", templateName, e.getMessage());
            throw new RuntimeException("Template processing failed", e);
        }
    }
    
    // Pre-built email templates for common scenarios
    public NotificationRequest createWelcomeEmail(String email, String firstName, String lastName) {
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.EMAIL)
            .emailRecipient(email)
            .recipient(email)
            .subject("Welcome to TradeMaster - Your Trading Journey Begins!")
            .templateName("welcome")
            .templateVariables(createTemplateVariables(Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "dashboardUrl", "https://app.trademaster.com/dashboard"
            )))
            .priority(NotificationRequest.Priority.MEDIUM)
            .referenceType("USER_REGISTRATION")
            .build();
    }
    
    public NotificationRequest createKycApprovalEmail(String email, String firstName) {
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.EMAIL)
            .emailRecipient(email)
            .recipient(email)
            .subject("KYC Verification Approved - Start Trading Now!")
            .templateName("kyc-approved")
            .templateVariables(createTemplateVariables(Map.of(
                "firstName", firstName,
                "tradingUrl", "https://app.trademaster.com/trading"
            )))
            .priority(NotificationRequest.Priority.HIGH)
            .referenceType("KYC_APPROVAL")
            .build();
    }
    
    public NotificationRequest createTradeExecutionEmail(String email, String firstName, 
                                                        String symbol, String action, 
                                                        String quantity, String price) {
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.EMAIL)
            .emailRecipient(email)
            .recipient(email)
            .subject("Trade Executed: " + action + " " + quantity + " " + symbol)
            .templateName("trade-execution")
            .templateVariables(createTemplateVariables(Map.of(
                "firstName", firstName,
                "symbol", symbol,
                "action", action,
                "quantity", quantity,
                "price", price,
                "portfolioUrl", "https://app.trademaster.com/portfolio"
            )))
            .priority(NotificationRequest.Priority.HIGH)
            .referenceType("TRADE_EXECUTION")
            .build();
    }
    
    public NotificationRequest createSecurityAlertEmail(String email, String firstName, 
                                                       String alertType, String details) {
        return NotificationRequest.builder()
            .type(NotificationRequest.NotificationType.EMAIL)
            .emailRecipient(email)
            .recipient(email)
            .subject("Security Alert: " + alertType)
            .templateName("security-alert")
            .templateVariables(createTemplateVariables(Map.of(
                "firstName", firstName,
                "alertType", alertType,
                "details", details,
                "securityUrl", "https://app.trademaster.com/security"
            )))
            .priority(NotificationRequest.Priority.URGENT)
            .referenceType("SECURITY_ALERT")
            .build();
    }
    
    private String createTemplateVariables(Map<String, Object> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            log.error("Failed to serialize template variables", e);
            return "{}";
        }
    }
}