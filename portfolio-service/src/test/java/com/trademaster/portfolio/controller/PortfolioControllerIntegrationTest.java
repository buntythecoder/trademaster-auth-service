package com.trademaster.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.model.AccountType;
import com.trademaster.portfolio.model.PortfolioStatus;
import com.trademaster.portfolio.model.RiskLevel;
import com.trademaster.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Portfolio Controller Integration Tests
 *
 * End-to-end integration tests for Portfolio REST API.
 *
 * Rule #20: Testing Standards - >70% integration coverage
 * Rule #24: Zero compilation errors
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("PortfolioController Integration Tests")
class PortfolioControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Disable security for testing
        registry.add("spring.security.enabled", () -> "false");
        registry.add("trademaster.common.security.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();

        testPortfolio = Portfolio.builder()
            .userId(1001L)
            .portfolioName("Test Portfolio")
            .totalValue(new BigDecimal("100000"))
            .cashBalance(new BigDecimal("50000"))
            .totalCost(new BigDecimal("90000"))
            .realizedPnl(new BigDecimal("5000"))
            .unrealizedPnl(new BigDecimal("5000"))
            .status(PortfolioStatus.ACTIVE)
            .currency("USD")
            .dayTradesCount(0)
            .build();
        testPortfolio = portfolioRepository.save(testPortfolio);
    }

    @Nested
    @DisplayName("Portfolio Creation API")
    class PortfolioCreationAPI {

        @Test
        @DisplayName("Should create new portfolio successfully")
        void shouldCreateNewPortfolio() throws Exception {
            // Given
            CreatePortfolioRequest request = new CreatePortfolioRequest(
                "New Portfolio",
                new BigDecimal("100000"),
                RiskLevel.MODERATE,
                "USD",
                AccountType.INDIVIDUAL,
                false
            );

            // When/Then
            mockMvc.perform(post("/api/v1/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portfolioId").exists())
                .andExpect(jsonPath("$.portfolioName").value("New Portfolio"))
                .andExpect(jsonPath("$.userId").value(1002))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given: Invalid request with null fields (this should fail validation)
            String invalidJson = """
                {
                    "portfolioName": null,
                    "initialCashBalance": null,
                    "riskLevel": null,
                    "currency": null,
                    "accountType": null,
                    "marginEnabled": null
                }
                """;

            // When/Then
            mockMvc.perform(post("/api/v1/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Portfolio Retrieval API")
    class PortfolioRetrievalAPI {

        @Test
        @DisplayName("Should get portfolio by ID")
        void shouldGetPortfolioById() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", testPortfolio.getPortfolioId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(testPortfolio.getPortfolioId()))
                .andExpect(jsonPath("$.portfolioName").value("Test Portfolio"))
                .andExpect(jsonPath("$.totalValue").value(100000))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 404 when portfolio not found")
        void shouldReturn404WhenPortfolioNotFound() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", 99999L))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get portfolio summary")
        void shouldGetPortfolioSummary() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/summary",
                    testPortfolio.getPortfolioId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(testPortfolio.getPortfolioId()))
                .andExpect(jsonPath("$.totalValue").exists())
                .andExpect(jsonPath("$.realizedPnl").exists())
                .andExpect(jsonPath("$.unrealizedPnl").exists());
        }
    }

    @Nested
    @DisplayName("Portfolio Update API")
    class PortfolioUpdateAPI {

        @Test
        @DisplayName("Should update portfolio successfully")
        void shouldUpdatePortfolio() throws Exception {
            // Given: Update request
            String updateJson = """
                {
                    "portfolioName": "Updated Portfolio",
                    "status": "ACTIVE"
                }
                """;

            // When/Then
            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", testPortfolio.getPortfolioId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioName").value("Updated Portfolio"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent portfolio")
        void shouldReturn404WhenUpdatingNonExistentPortfolio() throws Exception {
            // Given
            String updateJson = """
                {
                    "portfolioName": "Updated Portfolio"
                }
                """;

            // When/Then
            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", 99999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Portfolio Analytics API")
    class PortfolioAnalyticsAPI {

        @Test
        @DisplayName("Should get portfolio performance metrics")
        void shouldGetPortfolioPerformance() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/analytics/performance",
                    testPortfolio.getPortfolioId())
                    .param("startDate", "2024-01-01")
                    .param("endDate", "2024-12-31"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should get portfolio risk metrics")
        void shouldGetPortfolioRisk() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/risk",
                    testPortfolio.getPortfolioId()))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Internal API Endpoints")
    class InternalAPIEndpoints {

        @Test
        @DisplayName("Should access internal health endpoint")
        void shouldAccessInternalHealthEndpoint() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/internal/v1/portfolio/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("portfolio-service"));
        }

        @Test
        @DisplayName("Should access greetings endpoint")
        void shouldAccessGreetingsEndpoint() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/internal/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("portfolio-service"))
                .andExpect(jsonPath("$.status").value("OPERATIONAL"))
                .andExpect(jsonPath("$.capabilities").exists());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJSON() throws Exception {
            // Given: Malformed JSON
            String malformedJson = "{invalid json}";

            // When/Then
            mockMvc.perform(post("/api/v1/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle unsupported media type")
        void shouldHandleUnsupportedMediaType() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/portfolios")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("text content"))
                .andExpect(status().isUnsupportedMediaType());
        }
    }
}
