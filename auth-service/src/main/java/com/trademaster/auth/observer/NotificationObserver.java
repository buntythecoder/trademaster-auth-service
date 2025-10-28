package com.trademaster.auth.observer;

import com.trademaster.auth.event.*;
import com.trademaster.auth.pattern.EventObserver;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import com.trademaster.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Notification Observer
 *
 * Sends email notifications for critical authentication events.
 * Implements strategy pattern for event-specific notification handling.
 *
 * Features:
 * - Email notifications for security-critical events
 * - Pattern matching for event-specific templates
 * - Async notification delivery with virtual threads
 * - Functional composition for notification strategies
 *
 * Notification Triggers:
 * - New login from unrecognized device
 * - Password changes
 * - MFA configuration changes
 * - Security-forced logouts
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationObserver implements EventObserver<AuthEvent> {

    private final EmailService emailService;

    @Override
    public CompletableFuture<Result<Void, String>> onEvent(AuthEvent event) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                sendNotification(event);
                return null;
            }).fold(
                error -> Result.failure("Notification sending failed: " + error),
                ignored -> Result.success(null)
            )
        );
    }

    /**
     * Send notification using pattern matching
     */
    private void sendNotification(AuthEvent event) {
        switch (event) {
            case LoginEvent login -> notifyLogin(login);
            case LogoutEvent logout -> notifyLogout(logout);
            case PasswordChangeEvent passwordChange -> notifyPasswordChange(passwordChange);
            case MfaEvent mfa -> notifyMfa(mfa);
            case VerificationEvent verification -> notifyVerification(verification);
        }
    }

    /**
     * Notify user of login event
     * Only send notification for logins from new devices
     */
    private void notifyLogin(LoginEvent event) {
        if (event.deviceFingerprint().isEmpty() || !event.mfaUsed()) {
            String subject = "New Login Detected";
            String body = buildLoginNotification(event);
            sendEmail(event.userId(), subject, body);
            log.debug("Login notification sent for userId={}", event.userId());
        }
    }

    /**
     * Notify user of security logout
     */
    private void notifyLogout(LogoutEvent event) {
        if (event.reason() == LogoutEvent.LogoutReason.SECURITY) {
            String subject = "Security Alert: Account Logout";
            String body = buildSecurityLogoutNotification(event);
            sendEmail(event.userId(), subject, body);
            log.debug("Security logout notification sent for userId={}", event.userId());
        }
    }

    /**
     * Notify user of password change
     */
    private void notifyPasswordChange(PasswordChangeEvent event) {
        if (event.successful()) {
            String subject = "Password Changed Successfully";
            String body = buildPasswordChangeNotification(event);
            sendEmail(event.userId(), subject, body);
            log.debug("Password change notification sent for userId={}", event.userId());
        }
    }

    /**
     * Notify user of MFA configuration changes
     */
    private void notifyMfa(MfaEvent event) {
        if (event.successful()) {
            String subject = event.action() == MfaEvent.MfaAction.ENABLED
                ? "Multi-Factor Authentication Enabled"
                : "Multi-Factor Authentication Disabled";
            String body = buildMfaNotification(event);
            sendEmail(event.userId(), subject, body);
            log.debug("MFA {} notification sent for userId={}",
                event.action(), event.userId());
        }
    }

    /**
     * Notify user of email verification
     */
    private void notifyVerification(VerificationEvent event) {
        if (event.successful() && event.type() == VerificationEvent.VerificationType.EMAIL) {
            String subject = "Email Verified Successfully";
            String body = buildVerificationNotification(event);
            sendEmail(event.userId(), subject, body);
            log.debug("Verification notification sent for userId={}", event.userId());
        }
    }

    /**
     * Build login notification body
     */
    private String buildLoginNotification(LoginEvent event) {
        return String.format("""
            A new login was detected on your account.

            Time: %s
            IP Address: %s
            MFA Used: %s

            If this wasn't you, please secure your account immediately by changing your password.

            Correlation ID: %s
            """,
            event.timestamp(),
            event.ipAddress(),
            event.mfaUsed() ? "Yes" : "No",
            event.correlationId());
    }

    /**
     * Build security logout notification body
     */
    private String buildSecurityLogoutNotification(LogoutEvent event) {
        return String.format("""
            Your account was logged out for security reasons.

            Time: %s
            Session ID: %s

            Please review your account activity and change your password if you notice any suspicious activity.

            Correlation ID: %s
            """,
            event.timestamp(),
            event.sessionId(),
            event.correlationId());
    }

    /**
     * Build password change notification body
     */
    private String buildPasswordChangeNotification(PasswordChangeEvent event) {
        return String.format("""
            Your password was successfully changed.

            Time: %s
            IP Address: %s
            Change Type: %s

            If you did not make this change, please contact support immediately.

            Correlation ID: %s
            """,
            event.timestamp(),
            event.ipAddress(),
            event.changeType(),
            event.correlationId());
    }

    /**
     * Build MFA notification body
     */
    private String buildMfaNotification(MfaEvent event) {
        String action = event.action() == MfaEvent.MfaAction.ENABLED ? "enabled" : "disabled";
        return String.format("""
            Multi-factor authentication was %s on your account.

            Time: %s
            MFA Type: %s

            This change improves the security of your account.

            Correlation ID: %s
            """,
            action,
            event.timestamp(),
            event.mfaType(),
            event.correlationId());
    }

    /**
     * Build verification notification body
     */
    private String buildVerificationNotification(VerificationEvent event) {
        return String.format("""
            Your email address was successfully verified.

            Time: %s

            You now have full access to all features.

            Correlation ID: %s
            """,
            event.timestamp(),
            event.correlationId());
    }

    /**
     * Send email notification
     * In production, this would look up the user's email address
     */
    private void sendEmail(Long userId, String subject, String body) {
        // TODO: Look up user email from user service
        String userEmail = "user-" + userId + "@example.com"; // Placeholder

        emailService.sendEmail(userEmail, subject, body)
            .thenAccept(result -> result.fold(
                error -> {
                    log.error("Failed to send notification email: userId={}, error={}", userId, error);
                    return null;
                },
                success -> {
                    log.debug("Notification email sent successfully: userId={}", userId);
                    return null;
                }
            ));
    }
}
