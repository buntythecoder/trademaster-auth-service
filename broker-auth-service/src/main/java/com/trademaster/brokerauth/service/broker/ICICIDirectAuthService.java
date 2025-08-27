package com.trademaster.brokerauth.service.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.service.CredentialEncryptionService;
import com.trademaster.brokerauth.service.CredentialManagementService.BrokerCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ICICI Direct Authentication Service
 * 
 * Handles authentication flow for ICICI Direct API using session-based authentication.
 * Implements ICICI Direct-specific login flow with session token management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ICICIDirectAuthService implements BrokerAuthService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CredentialEncryptionService encryptionService;
    
    @Value("${broker.icici-direct.api-url:https://api.icicidirect.com}")
    private String apiUrl;
    
    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ICICI_DIRECT;
    }
    
    @Override
    public String getAuthorizationUrl(BrokerCredentials credentials, String state) {
        // ICICI Direct doesn't use browser-based OAuth flow
        // Authentication is done via API with username/password
        throw new UnsupportedOperationException(
            "ICICI Direct uses API-based authentication, not browser redirect flow"
        );
    }
    
    @Override
    public CompletableFuture<AuthResult> exchangeCodeForTokens(
            BrokerCredentials credentials, String authorizationCode, String state) {
        
        log.debug("Authenticating with ICICI Direct API");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> requestBody = Map.of(
                    "LoginId", credentials.getBrokerUserId(),
                    "Password", credentials.getPassword()
                );
                
                String response = webClient.post()
                    .uri(apiUrl + "/breezeapi/api/v1/customerlogin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return parseTokenResponse(response);
                
            } catch (WebClientResponseException e) {
                log.error("Failed to authenticate with ICICI Direct: {}", 
                         e.getResponseBodyAsString(), e);
                return AuthResult.failure("Authentication failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during ICICI Direct authentication", e);
                return AuthResult.failure("Authentication failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<AuthResult> refreshToken(BrokerCredentials credentials, String refreshToken) {
        // ICICI Direct doesn't support token refresh - need to re-authenticate
        log.warn("Token refresh not supported for ICICI Direct - manual re-authentication required");
        return CompletableFuture.completedFuture(
            AuthResult.failure("Token refresh not supported - please re-authenticate")
        );
    }
    
    @Override
    public CompletableFuture<Boolean> validateSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Validating ICICI Direct session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                String response = webClient.get()
                    .uri(apiUrl + "/breezeapi/api/v1/customerdetails")
                    .header("X-SessionToken", decryptedToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                String status = (String) responseMap.get("Status");
                
                return "Success".equalsIgnoreCase(status);
                
            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    log.warn("ICICI Direct session validation failed - unauthorized");
                    return false;
                }
                log.error("Error validating ICICI Direct session", e);
                return false;
            } catch (Exception e) {
                log.error("Unexpected error validating ICICI Direct session", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> revokeSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Revoking ICICI Direct session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                webClient.post()
                    .uri(apiUrl + "/breezeapi/api/v1/customerlogout")
                    .header("X-SessionToken", decryptedToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                log.info("ICICI Direct session revoked successfully");
                return true;
                
            } catch (Exception e) {
                log.error("Failed to revoke ICICI Direct session", e);
                return false; // Don't fail if revocation fails
            }
        });
    }
    
    /**
     * Parse token response from ICICI Direct API
     */
    private AuthResult parseTokenResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            String status = (String) responseMap.get("Status");
            if (!"Success".equalsIgnoreCase(status)) {
                String error = (String) responseMap.get("Error");
                return AuthResult.failure("Authentication failed: " + error);
            }
            
            Map<String, Object> success = (Map<String, Object>) responseMap.get("Success");
            if (success == null) {
                return AuthResult.failure("Invalid response format");
            }
            
            String sessionToken = (String) success.get("session_token");
            
            if (sessionToken == null) {
                return AuthResult.failure("Session token not found in response");
            }
            
            // ICICI Direct tokens are typically valid for 8 hours
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(8);
            
            // No refresh token for ICICI Direct
            return AuthResult.success(sessionToken, null, expiresAt);
            
        } catch (Exception e) {
            log.error("Failed to parse ICICI Direct token response", e);
            return AuthResult.failure("Failed to parse authentication response");
        }
    }
    
    @Override
    public boolean supportsTokenRefresh() {
        return false;
    }
    
    @Override
    public long getSessionValiditySeconds() {
        return 28800; // 8 hours
    }
    
    @Override
    public int getMaxSessionsPerUser() {
        return 2; // ICICI Direct allows limited concurrent sessions
    }
}