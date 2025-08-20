package com.trademaster.userprofile.exception;

public class DuplicateProfileException extends RuntimeException {
    public DuplicateProfileException(String message) {
        super(message);
    }
    
    public DuplicateProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}