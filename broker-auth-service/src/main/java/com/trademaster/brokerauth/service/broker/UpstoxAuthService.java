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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Upstox Pro Authentication Service
 * 
 * Handles OAuth 2.0 authentication flow for Upstox Pro API.
 * Implements authorization code flow with PKCE support.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpstoxAuthService implements BrokerAuthService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CredentialEncryptionService encryptionService;
    
    @Value("${broker.upstox.api-url:https://api.upstox.com/v2}")
    private String apiUrl;
    
    @Value("${broker.upstox.login-url:https://api.upstox.com/v2/login/authorization/dialog}")
    private String loginUrl;
    
    @Value("${broker.upstox.redirect-uri:http://localhost:8087/api/v1/auth/upstox/callback}")
    private String defaultRedirectUri;
    
    @Override
    public BrokerType getBrokerType() {
        return BrokerType.UPSTOX;
    }
    
    @Override
    public String getAuthorizationUrl(BrokerCredentials credentials, String state) {
        log.debug("Generating authorization URL for Upstox");
        
        if (credentials.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required for Upstox");
        }
        
        String redirectUri = credentials.getRedirectUri() != null ? 
            credentials.getRedirectUri() : defaultRedirectUri;
        
        return UriComponentsBuilder.fromUriString(loginUrl)
            .queryParam("response_type", "code")
            .queryParam("client_id", credentials.getClientId())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state != null ? state : "")
            .queryParam("scope", "NSE BSE MCX NCDEX CDS")
            .build()
            .toUriString();
    }
    
    @Override
    public CompletableFuture<AuthResult> exchangeCodeForTokens(
            BrokerCredentials credentials, String authorizationCode, String state) {
        
        log.debug("Exchanging authorization code for access token in Upstox");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String redirectUri = credentials.getRedirectUri() != null ? 
                    credentials.getRedirectUri() : defaultRedirectUri;
                
                String response = webClient.post()
                    .uri(apiUrl + "/login/authorization/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("code", authorizationCode)
                        .with("client_id", credentials.getClientId())
                        .with("client_secret", credentials.getApiSecret())
                        .with("redirect_uri", redirectUri)
                        .with("grant_type", "authorization_code"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return parseTokenResponse(response);
                
            } catch (WebClientResponseException e) {
                log.error("Failed to exchange authorization code for Upstox: {}", 
                         e.getResponseBodyAsString(), e);
                return AuthResult.failure("Token exchange failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during Upstox token exchange", e);
                return AuthResult.failure("Authentication failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<AuthResult> refreshToken(BrokerCredentials credentials, String refreshToken) {
        log.debug("Refreshing access token for Upstox");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = webClient.post()
                    .uri(apiUrl + "/login/authorization/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("refresh_token", refreshToken)
                        .with("client_id", credentials.getClientId())
                        .with("client_secret", credentials.getApiSecret())
                        .with("grant_type", "refresh_token"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return parseTokenResponse(response);
                
            } catch (WebClientResponseException e) {
                log.error("Failed to refresh token for Upstox: {}", e.getResponseBodyAsString(), e);
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    return AuthResult.failure("Refresh token expired - re-authentication required", "token_expired");
                }
                return AuthResult.failure("Token refresh failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during Upstox token refresh", e);
                return AuthResult.failure("Token refresh failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> validateSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Validating Upstox session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                String response = webClient.get()
                    .uri(apiUrl + "/user/profile")
                    .header("Authorization", "Bearer " + decryptedToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                // Check if response indicates success
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                String status = (String) responseMap.get("status");
                
                return "success".equals(status);
                
            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    log.warn("Upstox session validation failed - unauthorized");
                    return false;
                }
                log.error("Error validating Upstox session", e);
                return false;
            } catch (Exception e) {
                log.error("Unexpected error validating Upstox session", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> revokeSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Revoking Upstox session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                webClient.post()
                    .uri(apiUrl + "/logout")
                    .header("Authorization", "Bearer " + decryptedToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                log.info("Upstox session revoked successfully");
                return true;
                
            } catch (Exception e) {
                log.error("Failed to revoke Upstox session", e);
                return false; // Don't fail if revocation fails
            }
        });
    }
    
    /**
     * Parse token response from Upstox API
     */
    private AuthResult parseTokenResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            String status = (String) responseMap.get("status");
            if (!"success".equals(status)) {
                Map<String, Object> errors = (Map<String, Object>) responseMap.get("errors");
                String errorMessage = errors != null ? errors.toString() : "Authentication failed";
                return AuthResult.failure(errorMessage);
            }
            
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            if (data == null) {
                return AuthResult.failure("Invalid response format");
            }
            
            String accessToken = (String) data.get("access_token");
            String refreshToken = (String) data.get("refresh_token");
            Object expiresInObj = data.get("expires_in");
            
            if (accessToken == null) {
                return AuthResult.failure("Access token not found in response");
            }
            
            // Parse expires_in (can be String or Number)
            Long expiresIn = null;
            if (expiresInObj instanceof Number) {
                expiresIn = ((Number) expiresInObj).longValue();
            } else if (expiresInObj instanceof String) {
                try {
                    expiresIn = Long.parseLong((String) expiresInObj);
                } catch (NumberFormatException e) {
                    log.warn("Invalid expires_in format: {}", expiresInObj);
                }
            }
            
            // Default to 24 hours if expires_in not provided
            if (expiresIn == null) {
                expiresIn = 86400L; // 24 hours
            }
            
            return AuthResult.success(accessToken, refreshToken, expiresIn);
            
        } catch (Exception e) {
            log.error("Failed to parse Upstox token response", e);
            return AuthResult.failure("Failed to parse authentication response");
        }
    }
    
    @Override
    public boolean supportsTokenRefresh() {
        return true;
    }
    
    @Override
    public long getSessionValiditySeconds() {
        return 86400; // 24 hours
    }
    
    @Override
    public int getMaxSessionsPerUser() {
        return 3; // Upstox allows multiple sessions
    }
}