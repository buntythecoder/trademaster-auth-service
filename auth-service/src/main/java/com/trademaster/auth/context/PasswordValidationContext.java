package com.trademaster.auth.context;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Password Validation Context
 * 
 * Contains all information needed for comprehensive password validation
 * including user context and previous password history.
 */
@Data
@Builder
public class PasswordValidationContext {
    
    private String password;
    private String username;
    private String email;
    private List<String> previousPasswordHashes;
    private String currentPasswordHash;
    private boolean requireSpecialCharacters;
    private boolean requireNumbers;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private int minimumLength;
    private int maximumLength;
    private int passwordHistorySize;
    
    public static PasswordValidationContext defaultContext(String password, String username) {
        return PasswordValidationContext.builder()
            .password(password)
            .username(username)
            .requireSpecialCharacters(true)
            .requireNumbers(true)
            .requireUppercase(true)
            .requireLowercase(true)
            .minimumLength(8)
            .maximumLength(128)
            .passwordHistorySize(5)
            .build();
    }
}