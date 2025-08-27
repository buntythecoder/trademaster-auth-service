package com.trademaster.auth.service;

import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.time.Instant;
import java.util.Optional;

/**
 * Email Service for sending verification and notification emails
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    // Using VirtualThreadFactory pattern for consistent virtual thread management
    
    @Value("${trademaster.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;
    
    @Value("${spring.mail.username:noreply@trademaster.com}")
    private String fromAddress;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send email verification
     */
    public void sendEmailVerification(String email, String verificationToken) {
        try {
            String verificationUrl = String.format("%s/verify-email?token=%s", 
                frontendBaseUrl, verificationToken);
            
            log.info("Sending email verification to: {} with token: {}", email, verificationToken.substring(0, 8) + "...");
            
            String emailBody = buildEmailVerificationBody(verificationUrl);
            sendEmailInternal(email, "TradeMaster - Verify Your Email", emailBody, verificationUrl);
            
        } catch (Exception e) {
            log.error("Failed to send email verification to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            String resetUrl = String.format("%s/reset-password?token=%s", 
                frontendBaseUrl, resetToken);
            
            log.info("Sending password reset email to: {} with token: {}", email, resetToken.substring(0, 8) + "...");
            
            String emailBody = buildPasswordResetBody(resetUrl);
            
            sendEmailInternal(email, "TradeMaster - Password Reset", emailBody, resetUrl);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send MFA code via email
     */
    public void sendMfaCode(String email, String code) {
        try {
            log.info("Sending MFA code to: {}", email);
            
            String emailBody = buildMfaCodeBody(code);
            
            sendEmailInternal(email, "TradeMaster - MFA Verification Code", emailBody, null);
            
        } catch (Exception e) {
            log.error("Failed to send MFA code to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send MFA code", e);
        }
    }

    private String buildEmailVerificationBody(String verificationUrl) {
        return String.format("""
            <html>
            <body>
                <h2>Welcome to TradeMaster!</h2>
                <p>Please verify your email address by clicking the link below:</p>
                <p><a href="%s" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Verify Email</a></p>
                <p>Or copy and paste this URL into your browser:</p>
                <p>%s</p>
                <p>This link will expire in 24 hours.</p>
                <p>If you didn't create an account with TradeMaster, please ignore this email.</p>
                <br>
                <p>Best regards,<br>The TradeMaster Team</p>
            </body>
            </html>
            """, verificationUrl, verificationUrl);
    }

    private String buildPasswordResetBody(String resetUrl) {
        return String.format("""
            <html>
            <body>
                <h2>TradeMaster Password Reset</h2>
                <p>You requested to reset your password. Click the link below to create a new password:</p>
                <p><a href="%s" style="background-color: #dc3545; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Reset Password</a></p>
                <p>Or copy and paste this URL into your browser:</p>
                <p>%s</p>
                <p>This link will expire in 1 hour.</p>
                <p>If you didn't request a password reset, please ignore this email and your password will remain unchanged.</p>
                <br>
                <p>Best regards,<br>The TradeMaster Team</p>
            </body>
            </html>
            """, resetUrl, resetUrl);
    }

    private String buildMfaCodeBody(String code) {
        return String.format("""
            <html>
            <body>
                <h2>TradeMaster Security Code</h2>
                <p>Your multi-factor authentication code is:</p>
                <h1 style="color: #007bff; font-family: monospace; letter-spacing: 5px;">%s</h1>
                <p>This code will expire in 5 minutes.</p>
                <p>If you didn't attempt to log in, please secure your account immediately.</p>
                <br>
                <p>Best regards,<br>The TradeMaster Team</p>
            </body>
            </html>
            """, code);
    }

    /**
     * Production-ready email sending with retry mechanism and async processing.
     */
    @Retryable(value = {MailException.class, MessagingException.class}, 
               maxAttempts = 3, backoff = @Backoff(delay = 2000))
    private void sendEmailInternal(String recipient, String subject, String htmlBody, String actionUrl) {
        // Functional approach - replaces if-else
        Optional.of(emailEnabled)
            .filter(enabled -> !enabled)
            .ifPresent(enabled -> {
                log.info("Email sending disabled - logging email details", 
                    StructuredArguments.kv("recipient", recipient),
                    StructuredArguments.kv("subject", subject));
                return;
            });
            
        Optional.of(emailEnabled)
            .filter(enabled -> enabled)
            .ifPresent(enabled -> sendEmailFunctionally(recipient, subject, htmlBody, actionUrl));

    }
    
    private void sendEmailFunctionally(String recipient, String subject, String htmlBody, String actionUrl) {
        VirtualThreadFactory.INSTANCE.runAsync(() -> 
            SafeOperations.safelyToResult(() -> {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper;
                
                try {
                    helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(fromAddress);
                    helper.setTo(recipient);
                    helper.setSubject(subject);
                    helper.setText(htmlBody, true);
                } catch (MessagingException e) {
                    throw new RuntimeException("Failed to prepare email message: " + e.getMessage(), e);
                }
                
                mailSender.send(message);
                return "Email sent successfully";
            })
            .map(result -> {
                log.info("Email sent successfully", 
                    StructuredArguments.kv("recipient", recipient),
                    StructuredArguments.kv("subject", subject),
                    StructuredArguments.kv("timestamp", Instant.now()),
                    StructuredArguments.kv("service", "email-delivery"));
                return result;
            })
            .mapError(error -> {
                log.error("Email delivery failed", 
                    StructuredArguments.kv("recipient", recipient),
                    StructuredArguments.kv("subject", subject),
                    StructuredArguments.kv("error", error),
                    StructuredArguments.kv("errorType", "EmailDeliveryError"));
                return error;
            })
        );
    }
    
    /**
     * Fallback method for email sending failures
     */
    private void handleEmailFailure(MailException ex, String recipient, String subject, String htmlBody, String actionUrl) {
        log.error("All email delivery attempts failed for recipient: {}", recipient, ex);
        
        // Could implement fallback strategies here:
        // - Queue for later retry
        // - Use alternative email service
        // - Send notification to admin team
    }
}