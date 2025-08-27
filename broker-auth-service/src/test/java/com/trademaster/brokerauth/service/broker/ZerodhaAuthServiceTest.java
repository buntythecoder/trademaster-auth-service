package com.trademaster.brokerauth.service.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
import com.trademaster.brokerauth.service.CredentialEncryptionService;
import com.trademaster.brokerauth.service.CredentialManagementService.BrokerCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ZerodhaAuthService
 * 
 * Tests authentication flows, token management, session validation,
 * and error handling for Zerodha Kite API integration.
 */
@ExtendWith(MockitoExtension.class)
class ZerodhaAuthServiceTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private CredentialEncryptionService encryptionService;
    
    private ObjectMapper objectMapper;
    private ZerodhaAuthService zerodhaAuthService;
    
    private BrokerCredentials testCredentials;
    private BrokerSession testSession;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        zerodhaAuthService = new ZerodhaAuthService(webClient, objectMapper, encryptionService);
        
        // Setup test credentials
        testCredentials = BrokerCredentials.builder()
                .brokerType(BrokerType.ZERODHA)
                .clientId("test-client-id")
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .redirectUri("http://localhost:8087/api/v1/auth/zerodha/callback")
                .build();
        
        // Setup test session
        testSession = new BrokerSession();
        testSession.setId("test-session-id");
        testSession.setBrokerType(BrokerType.ZERODHA);
        testSession.setUserId(123L);
        testSession.setStatus(SessionStatus.ACTIVE);
        testSession.setEncryptedAccessToken("encrypted-access-token");
        testSession.setEncryptedRefreshToken("encrypted-refresh-token");
        testSession.setExpiresAt(LocalDateTime.now().plusHours(1));
        testSession.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        testSession.setLastUsedAt(LocalDateTime.now().minusMinutes(5));
    }
    
    @Test
    void getBrokerType_ShouldReturnZerodhaType() {
        // When
        BrokerType result = zerodhaAuthService.getBrokerType();
        
        // Then
        assertThat(result).isEqualTo(BrokerType.ZERODHA);
    }
    
    @Test
    void getAuthorizationUrl_ShouldReturnValidUrl_WhenValidCredentials() {
        // Given
        String state = "test-state-123";
        
        // When
        String authUrl = zerodhaAuthService.getAuthorizationUrl(testCredentials, state);
        
        // Then
        assertThat(authUrl).isNotNull();
        assertThat(authUrl).contains("https://kite.trade/connect/login");
        assertThat(authUrl).contains("client_id=" + testCredentials.getClientId());
        assertThat(authUrl).contains("response_type=code");
        assertThat(authUrl).contains("state=" + state);
        assertThat(authUrl).contains("redirect_uri=" + testCredentials.getRedirectUri());
    }
    
    @Test
    void getAuthorizationUrl_ShouldUseDefaultRedirectUri_WhenNotProvided() {
        // Given
        BrokerCredentials credentialsWithoutRedirect = BrokerCredentials.builder()
                .brokerType(BrokerType.ZERODHA)
                .clientId("test-client-id")
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .build();
        
        // When
        String authUrl = zerodhaAuthService.getAuthorizationUrl(credentialsWithoutRedirect, "state");
        
        // Then
        assertThat(authUrl).contains("redirect_uri=http://localhost:8087/api/v1/auth/zerodha/callback");
    }
    
    @Test
    void getAuthorizationUrl_ShouldThrowException_WhenClientIdIsNull() {
        // Given
        BrokerCredentials invalidCredentials = BrokerCredentials.builder()
                .brokerType(BrokerType.ZERODHA)
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .build();
        
        // When & Then
        assertThatThrownBy(() -> zerodhaAuthService.getAuthorizationUrl(invalidCredentials, "state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client ID is required for Zerodha");
    }
    
    @Test
    void exchangeCodeForTokens_ShouldReturnSuccess_WhenValidResponse() {
        // Given
        String authCode = "test-auth-code";
        String state = "test-state";
        
        String successResponse = """
        {
            "status": "success",
            "data": {
                "access_token": "test-access-token",
                "refresh_token": "test-refresh-token",
                "expires_in": 3600
            }
        }
        """;
        
        // Setup WebClient mocks
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(successResponse));
        
        // When
        CompletableFuture<BrokerAuthService.AuthResult> future = 
                zerodhaAuthService.exchangeCodeForTokens(testCredentials, authCode, state);
        BrokerAuthService.AuthResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("test-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("test-refresh-token");
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
    }
    
    @Test
    void exchangeCodeForTokens_ShouldReturnFailure_WhenApiReturnsError() {
        // Given
        String authCode = "invalid-auth-code";
        String state = "test-state";
        
        String errorResponse = """
        {
            "status": "error",
            "message": "Invalid authorization code",
            "error_type": "TokenException"
        }
        """;
        
        // Setup WebClient mocks
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(errorResponse));
        
        // When
        CompletableFuture<BrokerAuthService.AuthResult> future = 
                zerodhaAuthService.exchangeCodeForTokens(testCredentials, authCode, state);
        BrokerAuthService.AuthResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid authorization code");
    }
    
    @Test
    void exchangeCodeForTokens_ShouldReturnFailure_WhenWebClientThrowsException() {
        // Given
        String authCode = "test-auth-code";
        String state = "test-state";
        
        // Setup WebClient mocks to throw exception
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(WebClientResponseException.create(400, "Bad Request", null, null, null)));
        
        // When
        CompletableFuture<BrokerAuthService.AuthResult> future = 
                zerodhaAuthService.exchangeCodeForTokens(testCredentials, authCode, state);
        BrokerAuthService.AuthResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Token exchange failed");
    }
    
    @Test
    void refreshToken_ShouldReturnSuccess_WhenValidRefreshToken() {
        // Given
        String refreshToken = "test-refresh-token";
        
        String successResponse = """
        {
            "status": "success",
            "data": {
                "access_token": "new-access-token",
                "refresh_token": "new-refresh-token",
                "expires_in": 3600
            }
        }
        """;
        
        // Setup WebClient mocks
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(successResponse));
        
        // When
        CompletableFuture<BrokerAuthService.AuthResult> future = 
                zerodhaAuthService.refreshToken(testCredentials, refreshToken);
        BrokerAuthService.AuthResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
    }
    
    @Test
    void validateSession_ShouldReturnTrue_WhenSessionIsValid() {
        // Given
        String decryptedToken = "valid-access-token";
        String profileResponse = """
        {
            "status": "success",
            "data": {
                "user_id": "test123",
                "user_name": "Test User"
            }
        }
        """;
        
        // Setup mocks
        when(encryptionService.decrypt("encrypted-access-token")).thenReturn(decryptedToken);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(profileResponse));
        
        // When
        CompletableFuture<Boolean> future = zerodhaAuthService.validateSession(testCredentials, testSession);
        Boolean result = future.join();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void validateSession_ShouldReturnFalse_WhenSessionIsInvalid() {
        // Given
        String decryptedToken = "invalid-access-token";
        
        // Setup mocks
        when(encryptionService.decrypt("encrypted-access-token")).thenReturn(decryptedToken);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(WebClientResponseException.create(401, "Unauthorized", null, null, null)));
        
        // When
        CompletableFuture<Boolean> future = zerodhaAuthService.validateSession(testCredentials, testSession);
        Boolean result = future.join();
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void revokeSession_ShouldReturnTrue_WhenRevocationSucceeds() {
        // Given
        String decryptedToken = "valid-access-token";
        
        // Setup mocks
        when(encryptionService.decrypt("encrypted-access-token")).thenReturn(decryptedToken);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{}"));
        
        // When
        CompletableFuture<Boolean> future = zerodhaAuthService.revokeSession(testCredentials, testSession);
        Boolean result = future.join();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void revokeSession_ShouldReturnFalse_WhenRevocationFails() {
        // Given
        String decryptedToken = "valid-access-token";
        
        // Setup mocks
        when(encryptionService.decrypt("encrypted-access-token")).thenReturn(decryptedToken);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Network error")));
        
        // When
        CompletableFuture<Boolean> future = zerodhaAuthService.revokeSession(testCredentials, testSession);
        Boolean result = future.join();
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void supportsTokenRefresh_ShouldReturnTrue() {
        // When
        boolean result = zerodhaAuthService.supportsTokenRefresh();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void getSessionValiditySeconds_ShouldReturnCorrectValue() {
        // When
        long result = zerodhaAuthService.getSessionValiditySeconds();
        
        // Then
        assertThat(result).isEqualTo(86400); // 24 hours
    }
    
    @Test
    void getMaxSessionsPerUser_ShouldReturnCorrectValue() {
        // When
        int result = zerodhaAuthService.getMaxSessionsPerUser();
        
        // Then
        assertThat(result).isEqualTo(3);
    }
    
    @Test
    void exchangeCodeForTokens_ShouldHandleInvalidJsonResponse() {
        // Given
        String authCode = "test-auth-code";
        String state = "test-state";
        String invalidJsonResponse = "{ invalid json response }";
        
        // Setup WebClient mocks
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(invalidJsonResponse));
        
        // When
        CompletableFuture<BrokerAuthService.AuthResult> future = 
                zerodhaAuthService.exchangeCodeForTokens(testCredentials, authCode, state);
        BrokerAuthService.AuthResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Failed to parse authentication response");
    }
    
    @Test
    void parseTokenResponse_ShouldHandleMissingDataField() {
        // Given
        String authCode = "test-auth-code";
        String state = "test-state";
        String responseWithoutData = """
        {
            "status": "success"
        }
        """;
        
        // Setup WebClient mocks
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseWithoutData));
        
        // When
        CompletableFuture<BrokerAuthService.AuthResult> future = 
                zerodhaAuthService.exchangeCodeForTokens(testCredentials, authCode, state);
        BrokerAuthService.AuthResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid response format");
    }
}