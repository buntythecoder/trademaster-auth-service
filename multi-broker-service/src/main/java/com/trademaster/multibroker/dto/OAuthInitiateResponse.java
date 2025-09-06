package com.trademaster.multibroker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trademaster.multibroker.entity.BrokerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * OAuth Initiate Response DTO
 * 
 * MANDATORY: Immutable Record + Error Handling + OpenAPI Documentation
 * 
 * Response object for OAuth flow initiation containing authorization URL
 * and flow metadata. Supports both success and error responses.
 * 
 * Success Flow:
 * 1. Client calls initiate endpoint with broker type
 * 2. Service generates OAuth state and authorization URL
 * 3. Client redirects user to authorization URL
 * 4. User authenticates with broker
 * 5. Broker redirects back with authorization code
 * 
 * Security Features:
 * - State parameter for CSRF protection
 * - Time-limited OAuth flow (10 minutes default)
 * - Correlation ID for request tracking
 * - No sensitive data in response
 * 
 * @param authorizationUrl OAuth authorization URL for user redirect
 * @param brokerType Broker type for this OAuth flow
 * @param state OAuth state parameter for security
 * @param correlationId Request correlation ID
 * @param expiresIn Flow expiry time in seconds
 * @param success Whether operation was successful
 * @param error Error message if operation failed
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (OAuth Flow Response)
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for OAuth flow initiation")
public record OAuthInitiateResponse(
    
    @Schema(
        description = "OAuth authorization URL for user redirect",
        example = "https://kite.zerodha.com/connect/login?api_key=xxx&request_token=xxx&state=xxx"
    )
    String authorizationUrl,
    
    @Schema(
        description = "Broker type for this OAuth flow",
        example = "ZERODHA"
    )
    BrokerType brokerType,
    
    @Schema(
        description = "OAuth state parameter for CSRF protection",
        example = "user123-zerodha-1638360000"
    )
    String state,
    
    @Schema(
        description = "Request correlation ID for tracking",
        example = "A1B2C3D4"
    )
    String correlationId,
    
    @Schema(
        description = "Flow expiry time in seconds",
        example = "600"
    )
    Integer expiresIn,
    
    @Schema(
        description = "Whether operation was successful",
        example = "true"
    )
    Boolean success,
    
    @Schema(
        description = "Error message if operation failed",
        example = "Broker is currently not available"
    )
    String error
    
) {
    
    /**
     * Create successful OAuth initiate response
     * 
     * @param authorizationUrl OAuth authorization URL
     * @param brokerType Broker type
     * @param state OAuth state parameter
     * @param correlationId Correlation ID
     * @param expiresIn Expiry time in seconds
     * @return Success response
     */
    public static OAuthInitiateResponse success(String authorizationUrl,
                                              BrokerType brokerType,
                                              String state,
                                              String correlationId,
                                              Integer expiresIn) {
        return OAuthInitiateResponse.builder()
            .authorizationUrl(authorizationUrl)
            .brokerType(brokerType)
            .state(state)
            .correlationId(correlationId)
            .expiresIn(expiresIn)
            .success(Boolean.TRUE)
            .build();
    }
    
    /**
     * Create error OAuth initiate response
     * 
     * @param error Error message
     * @return Error response
     */
    public static OAuthInitiateResponse error(String error) {
        return OAuthInitiateResponse.builder()
            .success(Boolean.FALSE)
            .error(error)
            .build();
    }
    
    /**
     * Create error response with correlation ID
     * 
     * @param error Error message
     * @param correlationId Correlation ID
     * @return Error response with correlation ID
     */
    public static OAuthInitiateResponse error(String error, String correlationId) {
        return OAuthInitiateResponse.builder()
            .success(Boolean.FALSE)
            .error(error)
            .correlationId(correlationId)
            .build();
    }
    
    /**
     * Check if response indicates success
     * 
     * @return true if successful
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * Check if response indicates error
     * 
     * @return true if error occurred
     */
    public boolean isError() {
        return !isSuccess();
    }
    
    /**
     * Check if OAuth flow has expired
     * 
     * @param flowStartTime Flow start timestamp
     * @return true if flow has expired
     */
    public boolean isExpired(long flowStartTime) {
        if (expiresIn == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        return (currentTime - flowStartTime) > expiresIn;
    }
    
    /**
     * Get broker display name
     * 
     * @return Human-readable broker name
     */
    public String getBrokerDisplayName() {
        return brokerType != null ? brokerType.getDisplayName() : "Unknown";
    }
    
    /**
     * Get remaining time until expiry
     * 
     * @param flowStartTime Flow start timestamp in seconds
     * @return Remaining seconds until expiry
     */
    public long getRemainingSeconds(long flowStartTime) {
        if (expiresIn == null) {
            return Long.MAX_VALUE;
        }
        
        long currentTime = System.currentTimeMillis() / 1000;
        long elapsed = currentTime - flowStartTime;
        return Math.max(0, expiresIn - elapsed);
    }
    
    /**
     * Validate response contains required fields for success
     * 
     * @return true if valid success response
     */
    public boolean isValidSuccessResponse() {
        return isSuccess() &&
               authorizationUrl != null &&
               !authorizationUrl.trim().isEmpty() &&
               brokerType != null &&
               state != null &&
               !state.trim().isEmpty();
    }
    
    /**
     * Validate response contains error message for failure
     * 
     * @return true if valid error response
     */
    public boolean isValidErrorResponse() {
        return isError() &&
               error != null &&
               !error.trim().isEmpty();
    }
}