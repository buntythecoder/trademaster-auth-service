package com.trademaster.auth.integration;

import com.trademaster.auth.config.TestApplication;
import com.trademaster.auth.config.TestConfig;
import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.RegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.kms.KmsClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Working integration test using H2 in-memory database and mocked external dependencies.
 * This test focuses on testing the core authentication workflow without external infrastructure.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = {TestApplication.class, TestConfig.class}
)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class WorkingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock external dependencies that require infrastructure
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;
    
    @MockBean
    private KmsClient kmsClient;

    @Test
    void healthCheck_ShouldReturnServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void register_ShouldCreateUserSuccessfully() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_ShouldFailWithInvalidEmail() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("invalid-email");
        request.setPassword("StrongPassword123!");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_ShouldFailWithWeakPassword() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("weak");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_ShouldAuthenticateSuccessfully() throws Exception {
        // First register a user
        RegistrationRequest registerRequest = new RegistrationRequest();
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("StrongPassword123!");
        registerRequest.setFirstName("Login");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login with the same credentials
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("StrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_ShouldFailWithInvalidCredentials() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with all necessary beans configured for testing
    }
}