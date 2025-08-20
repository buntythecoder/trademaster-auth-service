package com.trademaster.userprofile.exception;

/**
 * Exception thrown when a requested document is not found
 */
public class DocumentNotFoundException extends RuntimeException {
    
    public DocumentNotFoundException(String message) {
        super(message);
    }
    
    public DocumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}