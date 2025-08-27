package com.trademaster.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.subscription.dto.SubscriptionRequest;
import com.trademaster.subscription.dto.SubscriptionResponse;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Subscription Integration Tests
 * 
 * Comprehensive integration tests using TestContainers for database and Kafka.
 * Tests complete subscription workflows with high concurrency using Virtual Threads.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvcTest
@Testcontainers
@Transactional
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "app.services.payment-gateway.url=http://localhost:8999",
    "app.services.payment-gateway.api-key=test-key",
    "logging.level.com.trademaster.subscription=DEBUG"
})
class SubscriptionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("trademaster_subscription_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @WithMockUser(roles = "USER")
    void shouldCreateSubscription() throws Exception {
        // Given
        SubscriptionRequest request = createSubscriptionRequest();
        
        // When & Then
        String responseJson = mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tier").value("PRO"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        SubscriptionResponse response = objectMapper.readValue(responseJson, SubscriptionResponse.class);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo(request.getUserId());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldGetActiveSubscription() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Subscription subscription = createTestSubscription(userId);
        subscriptionRepository.save(subscription);

        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/users/{userId}/active", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldHandleHighConcurrencyWithVirtualThreads() throws Exception {
        // Given
        int concurrentRequests = 1000;
        
        // When - Create multiple subscriptions concurrently using Virtual Threads
        CompletableFuture<?>[] futures = new CompletableFuture[concurrentRequests];
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    SubscriptionRequest request = createSubscriptionRequest();
                    request.setUserId(UUID.randomUUID()); // Unique user for each request
                    
                    mockMvc.perform(post("/api/v1/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());
                            
                } catch (Exception e) {
                    throw new RuntimeException("Request " + index + " failed", e);
                }
            });
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        // Then - Verify all subscriptions were created
        long subscriptionCount = subscriptionRepository.count();
        assertThat(subscriptionCount).isGreaterThanOrEqualTo(concurrentRequests);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldValidateSubscriptionData() throws Exception {
        // Given - Invalid request
        SubscriptionRequest request = new SubscriptionRequest();
        request.setUserId(null); // Invalid - required field
        request.setTier(null);   // Invalid - required field
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.fieldErrors").exists())
                .andExpected(jsonPath("$.fieldErrors.userId").exists())
                .andExpected(jsonPath("$.fieldErrors.tier").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")  
    void shouldAllowAdminAccess() throws Exception {
        // When & Then - Admin should access admin endpoints
        mockMvc.perform(get("/api/v1/subscriptions/status/ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyUserAccessToAdminEndpoints() throws Exception {
        // When & Then - User should not access admin endpoints
        mockMvc.perform(get("/api/v1/subscriptions/status/ACTIVE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyUnauthenticatedAccess() throws Exception {
        // When & Then - Unauthenticated should be denied
        mockMvc.perform(get("/api/v1/subscriptions/users/{userId}/active", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private SubscriptionRequest createSubscriptionRequest() {
        return SubscriptionRequest.builder()
                .userId(UUID.randomUUID())
                .tier(SubscriptionTier.PRO)
                .billingCycle(BillingCycle.MONTHLY)
                .paymentMethodId(UUID.randomUUID())
                .build();
    }

    private Subscription createTestSubscription(UUID userId) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .paymentMethodId(UUID.randomUUID())
                .startDate(java.time.LocalDateTime.now())
                .nextBillingDate(java.time.LocalDateTime.now().plusMonths(1))
                .failedBillingAttempts(0)
                .build();
    }
}