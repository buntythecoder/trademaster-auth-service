package com.trademaster.brokerauth.exception;

import com.trademaster.brokerauth.config.CorrelationConfig;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.service.StructuredLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler
 * 
 * Tests exception handling, error response formatting,
 * correlation tracking, and logging integration.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private StructuredLoggingService loggingService;
    
    @Mock
    private WebRequest webRequest;
    
    @Mock
    private BindingResult bindingResult;
    
    private GlobalExceptionHandler globalExceptionHandler;
    
    private static final String TEST_CORRELATION_ID = "test-correlation-123";
    private static final String TEST_REQUEST_ID = "test-request-456";
    private static final String TEST_USER_ID = "test-user-789";
    private static final String TEST_PATH = "/api/v1/auth/zerodha/callback";
    
    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler(loggingService);
        
        // Setup WebRequest mock
        when(webRequest.getDescription(false)).thenReturn("uri=" + TEST_PATH);
    }
    
    @Test
    void handleAuthenticationException_ShouldReturnUnauthorizedResponse() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials") {};
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleAuthenticationException(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
            assertThat(response.getBody().getMessage()).isEqualTo("Authentication failed");
            assertThat(response.getBody().getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(response.getBody().getPath()).isEqualTo(TEST_PATH);
        }
    }
    
    @Test
    void handleAccessDenied_ShouldReturnForbiddenResponse() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            correlationMock.when(CorrelationConfig.CorrelationContext::getUserId)
                          .thenReturn(TEST_USER_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleAccessDenied(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(403);
            assertThat(response.getBody().getError()).isEqualTo("Forbidden");
            assertThat(response.getBody().getMessage()).isEqualTo("Access denied - insufficient privileges");
            assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
        }
    }
    
    @Test
    void handleBrokerAuthentication_ShouldReturnCorrectStatusAndLogError() {
        // Given
        BrokerAuthenticationException exception = BrokerAuthenticationException
                .invalidCredentials(BrokerType.ZERODHA, 123L);
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleBrokerAuthentication(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
            assertThat(response.getBody().getMessage()).contains("Invalid credentials");
            
            // Verify logging
            verify(loggingService).logError(
                    eq("broker_auth_exception"),
                    anyString(),
                    eq("INVALID_CREDENTIALS"),
                    eq(exception),
                    argThat(details -> {
                        Map<String, Object> detailsMap = (Map<String, Object>) details;
                        return "ZERODHA".equals(detailsMap.get("brokerType")) &&
                               "123".equals(detailsMap.get("userId")) &&
                               TEST_CORRELATION_ID.equals(detailsMap.get("correlationId"));
                    })
            );
        }
    }
    
    @Test
    void handleSessionManagement_ShouldReturnBadRequestResponse() {
        // Given
        SessionManagementException exception = SessionManagementException
                .sessionNotFound("session-123");
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleSessionManagement(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Session Error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("SESSION_ERROR");
            assertThat(response.getBody().getDetails()).containsEntry("sessionId", "session-123");
        }
    }
    
    @Test
    void handleRateLimitExceeded_ShouldReturnTooManyRequestsAndLogSecurityIncident() {
        // Given
        RateLimitExceededException exception = RateLimitExceededException
                .apiRateLimit(BrokerType.UPSTOX, 456L, 100, 50, 60);
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleRateLimitExceeded(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(429);
            assertThat(response.getBody().getError()).isEqualTo("Too Many Requests");
            assertThat(response.getBody().getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(response.getBody().getDetails()).containsEntry("retryAfterSeconds", 60L);
            assertThat(response.getBody().getDetails()).containsEntry("brokerType", "UPSTOX");
            
            // Verify security incident logging
            verify(loggingService).logSecurityIncident(
                    eq("rate_limit_exceeded"),
                    eq("medium"),
                    eq("456"),
                    isNull(),
                    isNull(),
                    argThat(details -> {
                        Map<String, Object> detailsMap = (Map<String, Object>) details;
                        return "UPSTOX".equals(detailsMap.get("brokerType")) &&
                               Integer.valueOf(60).equals(detailsMap.get("retryAfterSeconds"));
                    })
            );
        }
    }
    
    @Test
    void handleCredentialManagement_ShouldReturnBadRequestResponse() {
        // Given
        CredentialManagementException exception = CredentialManagementException
                .credentialNotFound(BrokerType.ANGEL_ONE, 789L);
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleCredentialManagement(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Credential Error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("CREDENTIAL_ERROR");
            assertThat(response.getBody().getMessage()).contains("Credentials not found for ANGEL_ONE");
        }
    }
    
    @Test
    void handleWebClientResponse_ShouldReturnBadGatewayResponse() {
        // Given
        WebClientResponseException exception = WebClientResponseException.create(
                502, "Bad Gateway", null, "{\"error\": \"Broker API error\"}".getBytes(), null);
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleWebClientResponse(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(502);
            assertThat(response.getBody().getError()).isEqualTo("Broker API Error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("BROKER_API_ERROR");
            assertThat(response.getBody().getMessage()).isEqualTo("Broker service temporarily unavailable");
            assertThat(response.getBody().getDetails()).containsEntry("brokerResponseStatus", 502);
        }
    }
    
    @Test
    void handleMethodArgumentNotValid_ShouldReturnValidationErrorResponse() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError1 = new FieldError("testObject", "field1", "Field1 is required");
        FieldError fieldError2 = new FieldError("testObject", "field2", "Field2 must be positive");
        List<FieldError> fieldErrors = List.of(fieldError1, fieldError2);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleMethodArgumentNotValid(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Validation Error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid request data");
            assertThat(response.getBody().getFieldErrors()).containsKey("field1");
            assertThat(response.getBody().getFieldErrors()).containsKey("field2");
            assertThat(response.getBody().getFieldErrors().get("field1")).contains("Field1 is required");
            assertThat(response.getBody().getFieldErrors().get("field2")).contains("Field2 must be positive");
        }
    }
    
    @Test
    void handleIllegalArgument_ShouldReturnBadRequestResponse() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid broker type");
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleIllegalArgument(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid broker type");
        }
    }
    
    @Test
    void handleRuntimeException_ShouldReturnInternalServerErrorAndLogError() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected runtime error");
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            correlationMock.when(CorrelationConfig.CorrelationContext::getRequestId)
                          .thenReturn(TEST_REQUEST_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleRuntimeException(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getDetails()).containsEntry("exceptionType", "RuntimeException");
            
            // Verify error logging
            verify(loggingService).logError(
                    eq("runtime_exception"),
                    eq("Unexpected runtime error"),
                    eq("RUNTIME_EXCEPTION"),
                    eq(exception),
                    argThat(details -> {
                        Map<String, Object> detailsMap = (Map<String, Object>) details;
                        return TEST_PATH.equals(detailsMap.get("path")) &&
                               TEST_CORRELATION_ID.equals(detailsMap.get("correlationId")) &&
                               TEST_REQUEST_ID.equals(detailsMap.get("requestId"));
                    })
            );
        }
    }
    
    @Test
    void handleGenericException_ShouldReturnInternalServerErrorAndLogError() {
        // Given
        Exception exception = new Exception("Generic exception");
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            correlationMock.when(CorrelationConfig.CorrelationContext::getRequestId)
                          .thenReturn(TEST_REQUEST_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleGenericException(exception, webRequest);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("GENERIC_EXCEPTION");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getDetails()).containsEntry("exceptionType", "Exception");
            
            // Verify error logging
            verify(loggingService).logError(
                    eq("generic_exception"),
                    eq("Generic exception"),
                    eq("GENERIC_EXCEPTION"),
                    eq(exception),
                    any()
            );
        }
    }
    
    @Test
    void createErrorResponse_ShouldIncludeCorrelationData() {
        // Given
        BrokerAuthenticationException exception = BrokerAuthenticationException
                .invalidCredentials(BrokerType.ZERODHA, 123L);
        
        try (MockedStatic<CorrelationConfig.CorrelationContext> correlationMock = 
             mockStatic(CorrelationConfig.CorrelationContext.class)) {
            
            correlationMock.when(CorrelationConfig.CorrelationContext::getCorrelationId)
                          .thenReturn(TEST_CORRELATION_ID);
            correlationMock.when(CorrelationConfig.CorrelationContext::getRequestId)
                          .thenReturn(TEST_REQUEST_ID);
            correlationMock.when(CorrelationConfig.CorrelationContext::getUserId)
                          .thenReturn(TEST_USER_ID);
            
            // When
            ResponseEntity<ErrorResponse> response = globalExceptionHandler
                    .handleBrokerAuthentication(exception, webRequest);
            
            // Then
            assertThat(response.getBody().getDetails()).containsEntry("correlationId", TEST_CORRELATION_ID);
            assertThat(response.getBody().getDetails()).containsEntry("requestId", TEST_REQUEST_ID);
            assertThat(response.getBody().getDetails()).containsEntry("userId", TEST_USER_ID);
            assertThat(response.getBody().getDetails()).containsKey("timestamp");
        }
    }
}