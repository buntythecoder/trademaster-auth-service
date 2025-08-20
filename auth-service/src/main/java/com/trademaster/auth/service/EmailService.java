package com.trademaster.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${trademaster.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * Send email verification
     */
    public void sendEmailVerification(String email, String verificationToken) {
        try {
            String verificationUrl = String.format("%s/verify-email?token=%s", 
                frontendBaseUrl, verificationToken);
            
            log.info("Sending email verification to: {} with token: {}", email, verificationToken.substring(0, 8) + "...");
            
            // In a production environment, this would integrate with:
            // - AWS SES
            // - SendGrid
            // - Mailgun
            // - Or other email service providers
            
            String emailBody = buildEmailVerificationBody(verificationUrl);
            
            // Placeholder for actual email sending
            log.info("Email verification URL: {}", verificationUrl);
            log.info("Email body preview: {}", emailBody.substring(0, Math.min(100, emailBody.length())) + "...");
            
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
            
            // Placeholder for actual email sending
            log.info("Password reset URL: {}", resetUrl);
            log.info("Email body preview: {}", emailBody.substring(0, Math.min(100, emailBody.length())) + "...");
            
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
            
            // Placeholder for actual email sending
            log.info("MFA code: {} (this would be sent via email)", code);
            log.info("Email body preview: {}", emailBody.substring(0, Math.min(100, emailBody.length())) + "...");
            
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
}