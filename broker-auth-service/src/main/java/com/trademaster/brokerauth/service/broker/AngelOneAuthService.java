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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Angel One SmartAPI Authentication Service
 * 
 * Handles authentication flow for Angel One SmartAPI using API key and TOTP.
 * Implements Angel One-specific login flow with TOTP-based authentication.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AngelOneAuthService implements BrokerAuthService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CredentialEncryptionService encryptionService;
    
    @Value("${broker.angel-one.api-url:https://apiconnect.angelbroking.com}")
    private String apiUrl;
    
    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ANGEL_ONE;
    }
    
    @Override
    public String getAuthorizationUrl(BrokerCredentials credentials, String state) {
        // Angel One doesn't use browser-based OAuth flow
        // Authentication is done via API with credentials and TOTP
        throw new UnsupportedOperationException(
            "Angel One uses API-based authentication, not browser redirect flow"
        );
    }
    
    @Override
    public CompletableFuture<AuthResult> exchangeCodeForTokens(
            BrokerCredentials credentials, String authorizationCode, String state) {
        
        log.debug("Authenticating with Angel One SmartAPI");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For Angel One, authorizationCode is actually the TOTP token
                String totpToken = authorizationCode;
                
                if (totpToken == null || totpToken.isEmpty()) {
                    // Generate TOTP if not provided
                    totpToken = generateTOTP(credentials.getTotpSecret());
                }
                
                Map<String, Object> requestBody = Map.of(
                    "clientcode", credentials.getBrokerUserId(),
                    "password", credentials.getPassword(),
                    "totp", totpToken
                );
                
                String response = webClient.post()
                    .uri(apiUrl + "/rest/auth/angelbroking/user/v1/loginByPassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-UserType", "USER")
                    .header("X-SourceID", "WEB")
                    .header("X-ClientLocalIP", "192.168.1.1")
                    .header("X-ClientPublicIP", "192.168.1.1")
                    .header("X-MACAddress", "00:00:00:00:00:00")
                    .header("X-PrivateKey", credentials.getApiKey())
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return parseTokenResponse(response);
                
            } catch (WebClientResponseException e) {
                log.error("Failed to authenticate with Angel One: {}", 
                         e.getResponseBodyAsString(), e);
                return AuthResult.failure("Authentication failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during Angel One authentication", e);
                return AuthResult.failure("Authentication failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<AuthResult> refreshToken(BrokerCredentials credentials, String refreshToken) {
        log.debug("Refreshing JWT token for Angel One");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> requestBody = Map.of(
                    "refreshToken", refreshToken
                );
                
                String response = webClient.post()
                    .uri(apiUrl + "/rest/auth/angelbroking/jwt/v1/generateTokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-UserType", "USER")
                    .header("X-SourceID", "WEB")
                    .header("X-ClientLocalIP", "192.168.1.1")
                    .header("X-ClientPublicIP", "192.168.1.1")
                    .header("X-MACAddress", "00:00:00:00:00:00")
                    .header("X-PrivateKey", credentials.getApiKey())
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                return parseTokenResponse(response);
                
            } catch (WebClientResponseException e) {
                log.error("Failed to refresh token for Angel One: {}", 
                         e.getResponseBodyAsString(), e);
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    return AuthResult.failure("Refresh token expired - re-authentication required", "token_expired");
                }
                return AuthResult.failure("Token refresh failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during Angel One token refresh", e);
                return AuthResult.failure("Token refresh failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> validateSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Validating Angel One session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                String response = webClient.get()
                    .uri(apiUrl + "/rest/secure/angelbroking/user/v1/getProfile")
                    .header("Authorization", "Bearer " + decryptedToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-UserType", "USER")
                    .header("X-SourceID", "WEB")
                    .header("X-ClientLocalIP", "192.168.1.1")
                    .header("X-ClientPublicIP", "192.168.1.1")
                    .header("X-MACAddress", "00:00:00:00:00:00")
                    .header("X-PrivateKey", credentials.getApiKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                return Boolean.TRUE.equals(responseMap.get("status"));
                
            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    log.warn("Angel One session validation failed - unauthorized");
                    return false;
                }
                log.error("Error validating Angel One session", e);
                return false;
            } catch (Exception e) {
                log.error("Unexpected error validating Angel One session", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> revokeSession(BrokerCredentials credentials, BrokerSession session) {
        log.debug("Revoking Angel One session");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Decrypt access token before use
                String decryptedToken = encryptionService.decrypt(session.getEncryptedAccessToken());
                
                webClient.post()
                    .uri(apiUrl + "/rest/secure/angelbroking/user/v1/logout")
                    .header("Authorization", "Bearer " + decryptedToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-UserType", "USER")
                    .header("X-SourceID", "WEB")
                    .header("X-ClientLocalIP", "192.168.1.1")
                    .header("X-ClientPublicIP", "192.168.1.1")
                    .header("X-MACAddress", "00:00:00:00:00:00")
                    .header("X-PrivateKey", credentials.getApiKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                log.info("Angel One session revoked successfully");
                return true;
                
            } catch (Exception e) {
                log.error("Failed to revoke Angel One session", e);
                return false; // Don't fail if revocation fails
            }
        });
    }
    
    /**
     * Generate TOTP token from secret
     */
    private String generateTOTP(String secret) {
        try {
            // This is a simplified TOTP implementation
            // In production, use a proper TOTP library like java-otp
            long timeStep = System.currentTimeMillis() / 30000; // 30-second windows
            
            byte[] key = Base64.getDecoder().decode(secret);
            byte[] timeBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                timeBytes[i] = (byte) (timeStep & 0xFF);
                timeStep >>= 8;
            }
            
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(timeBytes);
            
            int offset = hash[hash.length - 1] & 0x0F;
            int code = ((hash[offset] & 0x7F) << 24) |
                       ((hash[offset + 1] & 0xFF) << 16) |
                       ((hash[offset + 2] & 0xFF) << 8) |
                       (hash[offset + 3] & 0xFF);
            
            code = code % 1000000; // 6-digit code
            return String.format("%06d", code);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }
    
    /**
     * Parse token response from Angel One API
     */
    private AuthResult parseTokenResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            Boolean status = (Boolean) responseMap.get("status");
            if (!Boolean.TRUE.equals(status)) {
                String message = (String) responseMap.get("message");
                return AuthResult.failure("Authentication failed: " + message);
            }
            
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            if (data == null) {
                return AuthResult.failure("Invalid response format");
            }
            
            String jwtToken = (String) data.get("jwtToken");
            String refreshToken = (String) data.get("refreshToken");
            
            if (jwtToken == null) {
                return AuthResult.failure("JWT token not found in response");
            }
            
            // Angel One tokens are typically valid for 12 hours
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(12);
            
            return AuthResult.success(jwtToken, refreshToken, expiresAt);
            
        } catch (Exception e) {
            log.error("Failed to parse Angel One token response", e);
            return AuthResult.failure("Failed to parse authentication response");
        }
    }
    
    @Override
    public boolean supportsTokenRefresh() {
        return true;
    }
    
    @Override
    public long getSessionValiditySeconds() {
        return 43200; // 12 hours
    }
    
    @Override
    public int getMaxSessionsPerUser() {
        return 1; // Angel One allows only one active session
    }
}