package com.trademaster.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.auth.AuthServiceApplication;
import com.trademaster.auth.config.MockBeanConfiguration;
import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.entity.User;
import com.trademaster.auth.entity.UserProfile;
import com.trademaster.auth.repository.UserRepository;
import com.trademaster.auth.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication flow
 * 
 * These tests verify the complete authentication flow including:
 * - User registration
 * - User login
 * - Token validation
 * - Error handling
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, 
    classes = {AuthServiceApplication.class, MockBeanConfiguration.class})
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private RegistrationRequest validRegistrationRequest;
    private AuthenticationRequest validAuthenticationRequest;

    @BeforeEach
    void setUp() {
        // Create test data
        validRegistrationRequest = RegistrationRequest.builder()
            .email("test@trademaster.com")
            .password("StrongPassword123!")
            .firstName("Test")
            .lastName("User")
            .countryCode("IN")
            .riskTolerance(UserProfile.RiskTolerance.MODERATE)
            .tradingExperience(UserProfile.TradingExperience.INTERMEDIATE)
            .agreeToTerms(true)
            .agreeToPrivacyPolicy(true)
            .build();

        validAuthenticationRequest = AuthenticationRequest.builder()
            .email("test@trademaster.com")
            .password("StrongPassword123!")
            .build();
    }

    @Test
    void register_ShouldCreateUserSuccessfully() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0")
                .header("X-Forwarded-For", "127.0.0.1"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.requiresEmailVerification").value(true))
            .andExpect(jsonPath("$.requiresMfa").value(false))
            .andExpect(jsonPath("$.user.email").value("test@trademaster.com"))
            .andExpect(jsonPath("$.user.kycStatus").value("PENDING"))
            .andExpect(jsonPath("$.user.subscriptionTier").value("FREE"))
            .andExpect(jsonPath("$.message").value(containsString("Registration successful")))
            .andReturn();

        // Verify user was created in database
        assertTrue(userRepository.existsByEmailIgnoreCase("test@trademaster.com"));
    }

    @Test
    void register_ShouldFailWithDuplicateEmail() throws Exception {
        // Arrange - register user first
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isCreated());

        // Act & Assert - try to register again with same email
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("User with this email already exists")));
    }

    @Test
    void register_ShouldFailWithWeakPassword() throws Exception {
        // Arrange
        RegistrationRequest weakPasswordRequest = RegistrationRequest.builder()
            .email("test2@trademaster.com")
            .password("weak")
            .firstName("Test")
            .lastName("User")
            .countryCode("IN")
            .agreeToTerms(true)
            .agreeToPrivacyPolicy(true)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPasswordRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Registration failed")));
    }

    @Test
    void register_ShouldFailWithInvalidEmail() throws Exception {
        // Arrange
        RegistrationRequest invalidEmailRequest = RegistrationRequest.builder()
            .email("invalid-email")
            .password("StrongPassword123!")
            .firstName("Test")
            .lastName("User")
            .countryCode("IN")
            .agreeToTerms(true)
            .agreeToPrivacyPolicy(true)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmailRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldAuthenticateSuccessfully() throws Exception {
        // Arrange - register user first
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthenticationRequest))
                .header("User-Agent", "TestAgent/1.0")
                .header("X-Forwarded-For", "127.0.0.1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.requiresMfa").value(false))
            .andExpect(jsonPath("$.user.email").value("test@trademaster.com"))
            .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_ShouldFailWithInvalidCredentials() throws Exception {
        // Arrange
        AuthenticationRequest invalidRequest = AuthenticationRequest.builder()
            .email("nonexistent@trademaster.com")
            .password("WrongPassword123!")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(containsString("Login failed")));
    }

    @Test
    void login_ShouldFailWithWrongPassword() throws Exception {
        // Arrange - register user first
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"));

        AuthenticationRequest wrongPasswordRequest = AuthenticationRequest.builder()
            .email("test@trademaster.com")
            .password("WrongPassword123!")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(containsString("Login failed")));
    }

    @Test
    void refreshToken_ShouldGenerateNewTokens() throws Exception {
        // Arrange - register and login to get refresh token
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthenticationRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}")
                .header("User-Agent", "TestAgent/1.0")
                .header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.message").value("Token refreshed successfully"));
    }

    @Test
    void refreshToken_ShouldFailWithInvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"invalid.token.here\"}")
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(containsString("Token refresh failed")));
    }

    @Test
    void healthCheck_ShouldReturnServiceStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/health")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("TradeMaster Auth Service"))
            .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void completeAuthenticationFlow_ShouldWorkEndToEnd() throws Exception {
        // 1. Register user
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0")
                .header("X-Forwarded-For", "127.0.0.1"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("test@trademaster.com"))
            .andReturn();

        // 2. Login with registered credentials
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthenticationRequest))
                .header("User-Agent", "TestAgent/1.0")
                .header("X-Forwarded-For", "127.0.0.1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andReturn();

        // 3. Extract tokens from login response
        String loginResponse = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // 4. Use refresh token to get new tokens
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}")
                .header("User-Agent", "TestAgent/1.0")
                .header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());

        // 5. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Verify user exists in database
        assertTrue(userRepository.existsByEmailIgnoreCase("test@trademaster.com"));
        
        // Verify user details
        User user = userRepository.findByEmailIgnoreCase("test@trademaster.com").orElse(null);
        assertNotNull(user);
        assertEquals(User.KycStatus.PENDING, user.getKycStatus());
        assertEquals(User.SubscriptionTier.FREE, user.getSubscriptionTier());
        assertEquals(User.AccountStatus.ACTIVE, user.getAccountStatus());
    }

    @Test
    void register_ShouldHandleConcurrentRequests() throws Exception {
        // This test would ideally use multiple threads, but for simplicity,
        // we'll test that the second registration fails appropriately
        
        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isCreated());

        // Second registration with same email should fail
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
                .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("User with this email already exists")));

        // Verify only one user was created
        assertEquals(1, userRepository.count());
    }
}