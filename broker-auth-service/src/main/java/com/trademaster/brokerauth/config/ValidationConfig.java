package com.trademaster.brokerauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import java.util.regex.Pattern;

/**
 * Validation Configuration
 * 
 * Configures input validation, custom validators,
 * and security hardening for all API inputs.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class ValidationConfig {
    
    /**
     * Bean validation factory
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
    
    /**
     * Method validation post processor
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }
    
    // Custom validation annotations for broker auth security
    
    /**
     * Validates broker user IDs to prevent injection attacks
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = BrokerUserIdValidator.class)
    @Documented
    public @interface ValidBrokerUserId {
        String message() default "Invalid broker user ID format";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    /**
     * Validates API keys to ensure proper format
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ApiKeyValidator.class)
    @Documented
    public @interface ValidApiKey {
        String message() default "Invalid API key format";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    /**
     * Validates session IDs to prevent session hijacking
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = SessionIdValidator.class)
    @Documented
    public @interface ValidSessionId {
        String message() default "Invalid session ID format";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    /**
     * Validates authorization codes
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = AuthorizationCodeValidator.class)
    @Documented
    public @interface ValidAuthorizationCode {
        String message() default "Invalid authorization code format";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    /**
     * Validates redirect URIs for security
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = RedirectUriValidator.class)
    @Documented
    public @interface ValidRedirectUri {
        String message() default "Invalid redirect URI";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    // Validator implementations
    
    public static class BrokerUserIdValidator implements ConstraintValidator<ValidBrokerUserId, String> {
        
        private static final Pattern BROKER_USER_ID_PATTERN = 
            Pattern.compile("^[a-zA-Z0-9_\\-\\.]{1,50}$");
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            
            // Check format
            if (!BROKER_USER_ID_PATTERN.matcher(value).matches()) {
                return false;
            }
            
            // Prevent common injection patterns
            String lowerValue = value.toLowerCase();
            if (lowerValue.contains("script") || lowerValue.contains("select") || 
                lowerValue.contains("union") || lowerValue.contains("drop") ||
                lowerValue.contains("delete") || lowerValue.contains("insert") ||
                lowerValue.contains("update") || lowerValue.contains("exec")) {
                return false;
            }
            
            return true;
        }
    }
    
    public static class ApiKeyValidator implements ConstraintValidator<ValidApiKey, String> {
        
        private static final Pattern API_KEY_PATTERN = 
            Pattern.compile("^[a-zA-Z0-9_\\-]{16,128}$");
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            
            // Check basic format
            if (!API_KEY_PATTERN.matcher(value).matches()) {
                return false;
            }
            
            // Check for obviously fake or test keys
            String lowerValue = value.toLowerCase();
            if (lowerValue.contains("test") || lowerValue.contains("demo") || 
                lowerValue.contains("fake") || lowerValue.contains("sample") ||
                lowerValue.equals("your_api_key_here") || value.equals("12345678901234567890")) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "API key appears to be a test/demo key. Use a real API key.").addConstraintViolation();
                return false;
            }
            
            return true;
        }
    }
    
    public static class SessionIdValidator implements ConstraintValidator<ValidSessionId, String> {
        
        private static final Pattern SESSION_ID_PATTERN = 
            Pattern.compile("^[a-zA-Z0-9_\\-]{20,100}$");
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            
            // Check format
            if (!SESSION_ID_PATTERN.matcher(value).matches()) {
                return false;
            }
            
            // Session IDs should have reasonable entropy
            long uniqueChars = value.chars().distinct().count();
            if (uniqueChars < 10) { // Should have at least 10 unique characters
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Session ID has insufficient entropy").addConstraintViolation();
                return false;
            }
            
            return true;
        }
    }
    
    public static class AuthorizationCodeValidator implements ConstraintValidator<ValidAuthorizationCode, String> {
        
        private static final Pattern AUTH_CODE_PATTERN = 
            Pattern.compile("^[a-zA-Z0-9_\\-\\.]{10,500}$");
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            
            // Check basic format
            if (!AUTH_CODE_PATTERN.matcher(value).matches()) {
                return false;
            }
            
            // Authorization codes should be reasonably long for security
            if (value.length() < 20) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Authorization code is too short").addConstraintViolation();
                return false;
            }
            
            return true;
        }
    }
    
    public static class RedirectUriValidator implements ConstraintValidator<ValidRedirectUri, String> {
        
        private static final Pattern URI_PATTERN = 
            Pattern.compile("^https?://[a-zA-Z0-9\\-\\.]+[a-zA-Z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=]*$");
        
        private static final String[] ALLOWED_HOSTS = {
            "localhost",
            "127.0.0.1",
            "trademaster.com",
            "api.trademaster.com",
            "auth.trademaster.com"
        };
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            
            // Check basic URI format
            if (!URI_PATTERN.matcher(value).matches()) {
                return false;
            }
            
            // Must use HTTPS in production (allow HTTP for localhost in dev)
            if (!value.startsWith("https://") && !value.startsWith("http://localhost") && 
                !value.startsWith("http://127.0.0.1")) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Redirect URI must use HTTPS (except localhost for development)").addConstraintViolation();
                return false;
            }
            
            // Extract host for validation
            try {
                java.net.URI uri = java.net.URI.create(value);
                String host = uri.getHost();
                
                if (host == null) {
                    return false;
                }
                
                // Check if host is in allowed list
                boolean hostAllowed = false;
                for (String allowedHost : ALLOWED_HOSTS) {
                    if (host.equals(allowedHost) || host.endsWith("." + allowedHost)) {
                        hostAllowed = true;
                        break;
                    }
                }
                
                if (!hostAllowed) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                        "Redirect URI host is not in the allowed list").addConstraintViolation();
                    return false;
                }
                
                // Additional security checks
                if (value.contains("..") || value.contains("//") || value.contains("javascript:") ||
                    value.contains("data:") || value.contains("vbscript:")) {
                    return false;
                }
                
            } catch (Exception e) {
                return false;
            }
            
            return true;
        }
    }
}