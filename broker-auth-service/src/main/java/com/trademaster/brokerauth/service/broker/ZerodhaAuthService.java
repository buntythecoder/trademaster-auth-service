package com.trademaster.brokerauth.service.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
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

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Zerodha Kite Authentication Service
 * 
 * Handles authentication flow for Zerodha Kite API using request token mechanism.
 * Implements Zerodha-specific login flow and session management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZerodhaAuthService implements BrokerAuthService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CredentialEncryptionService encryptionService;
    
    @Value("${broker.zerodha.api-url:https://api.kite.trade}")
    private String apiUrl;
    
    @Value("${broker.zerodha.login-url:https://kite.zerodha.com/connect/login}")
    private String loginUrl;
    
    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ZERODHA;
    }
    
    @Override
    public String getAuthorizationUrl(BrokerCredentials credentials, String state) {
        log.debug("Generating authorization URL for Zerodha");
        
        if (credentials.getApiKey() == null) {
            throw new IllegalArgumentException("API key is required for Zerodha");
        }
        
        return String.format("%s?api_key=%s&v=3", loginUrl, credentials.getApiKey());
    }
    
    @Override
    public CompletableFuture<AuthResult> exchangeCodeForTokens(
            BrokerCredentials credentials, String authorizationCode, String state) {
        
        log.debug("Exchanging request token for access token in Zerodha");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Zerodha uses request_token instead of authorization_code
                String requestToken = authorizationCode;
                String checksum = generateChecksum(credentials.getApiKey(), requestToken, credentials.getApiSecret());
                
                Map<String, String> requestBody = Map.of(
                    "api_key", credentials.getApiKey(),
                    "request_token", requestToken,
                    "checksum", checksum
                );
                
                String response = webClient.post()
                    .uri(apiUrl + "/session/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("api_key", credentials.getApiKey())
                        .with("request_token", requestToken)
                        .with("checksum", checksum))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return parseTokenResponse(response);
                
            } catch (WebClientResponseException e) {
                log.error("Failed to exchange request token for Zerodha: {}", e.getResponseBodyAsString(), e);
                return AuthResult.failure("Token exchange failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during Zerodha token exchange", e);
                return AuthResult.failure("Authentication failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<AuthResult> refreshToken(BrokerCredentials credentials, String refreshToken) {
        // Zerodha doesn't support token refresh - need to re-authenticate
        log.warn("Token refresh not supported for Zerodha - manual re-authentication required");
        return CompletableFuture.completedFuture(
            AuthResult.failure("Token refresh not supported - please re-authenticate")
        );
    }
    
    @Override
    public CompletableFuture<Boolean> validateSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Validating Zerodha session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                String response = webClient.get()
                    .uri(apiUrl + "/user/profile")
                    .header("Authorization", "token " + credentials.getApiKey() + ":" + decryptedToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return response != null && !response.contains("error");
                
            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    log.warn("Zerodha session validation failed - unauthorized");
                    return false;
                }
                log.error("Error validating Zerodha session", e);
                return false;
            } catch (Exception e) {
                log.error("Unexpected error validating Zerodha session", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> revokeSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Revoking Zerodha session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                webClient.delete()
                    .uri(apiUrl + "/session/token")
                    .header("Authorization", "token " + credentials.getApiKey() + ":" + decryptedToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                log.info("Zerodha session revoked successfully");
                return true;
                
            } catch (Exception e) {
                log.error("Failed to revoke Zerodha session", e);
                return false; // Don't fail if revocation fails
            }
        });
    }
    
    /**
     * Generate checksum for Zerodha authentication
     */
    private String generateChecksum(String apiKey, String requestToken, String apiSecret) {
        try {
            String data = apiKey + requestToken + apiSecret;
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(data.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate checksum", e);
        }
    }
    
    /**
     * Parse token response from Zerodha API
     */
    private AuthResult parseTokenResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            if (responseMap.containsKey("error_type")) {
                String error = (String) responseMap.get("message");
                return AuthResult.failure("Authentication failed: " + error);
            }
            
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            if (data == null) {
                return AuthResult.failure("Invalid response format");
            }
            
            String accessToken = (String) data.get("access_token");
            String publicToken = (String) data.get("public_token");
            
            if (accessToken == null) {
                return AuthResult.failure("Access token not found in response");
            }
            
            // Zerodha tokens are valid for 24 hours
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
            
            return AuthResult.success(accessToken, null, expiresAt); // No refresh token for Zerodha
            
        } catch (Exception e) {
            log.error("Failed to parse Zerodha token response", e);
            return AuthResult.failure("Failed to parse authentication response");
        }
    }
    
    @Override
    public boolean supportsTokenRefresh() {
        return false;
    }
    
    @Override
    public long getSessionValiditySeconds() {
        return 86400; // 24 hours
    }
    
    @Override
    public int getMaxSessionsPerUser() {
        return 1; // Zerodha allows only one active session
    }
}