package com.trademaster.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI 3.0 Configuration for TradeMaster Auth Service
 *
 * MANDATORY: Golden Specification - OpenAPI 3.0 Comprehensive Documentation
 *
 * Features:
 * - Complete API documentation with examples
 * - Security scheme definitions (JWT, API Key)
 * - Error response schemas
 * - Request/response examples
 * - Rate limiting documentation
 * - Kong API Gateway integration notes
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfiguration {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${kong.gateway.url:http://localhost:8000}")
    private String kongGatewayUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServerList())
                .components(createComponents())
                .addSecurityItem(createSecurityRequirement());
    }

    /**
     * Create comprehensive API information
     */
    private Info createApiInfo() {
        return new Info()
                .title("TradeMaster Authentication Service API")
                .description("""
                        **TradeMaster Authentication Service** provides secure user authentication,
                        authorization, and session management for the TradeMaster trading platform.

                        ## Features
                        - **JWT-based Authentication**: Stateless authentication with refresh tokens
                        - **Multi-Factor Authentication (MFA)**: TOTP and SMS-based MFA support
                        - **Social Authentication**: OAuth2 integration with Google, GitHub, LinkedIn
                        - **Rate Limiting**: Built-in rate limiting and DDoS protection
                        - **Security Auditing**: Comprehensive audit logging and monitoring
                        - **Kong Integration**: Full API Gateway integration support
                        - **Consul Discovery**: Service discovery and health monitoring

                        ## Security
                        - **Zero Trust Architecture**: All endpoints require explicit authentication
                        - **Circuit Breaker Protection**: Resilient external service integration
                        - **Virtual Threads**: Java 24 Virtual Thread support for high concurrency
                        - **Structured Logging**: Complete audit trail with correlation IDs

                        ## Rate Limits
                        - **Public Endpoints**: 100 requests/minute, 1000 requests/hour
                        - **Authenticated**: 1000 requests/minute, 10000 requests/hour
                        - **Premium Users**: 5000 requests/minute, 50000 requests/hour

                        ## Error Handling
                        All endpoints return standardized error responses with correlation IDs
                        for debugging and audit purposes.
                        """)
                .version("1.0.0")
                .contact(createContact())
                .license(createLicense());
    }

    /**
     * Create contact information
     */
    private Contact createContact() {
        return new Contact()
                .name("TradeMaster Development Team")
                .email("dev@trademaster.com")
                .url("https://docs.trademaster.com");
    }

    /**
     * Create license information
     */
    private License createLicense() {
        return new License()
                .name("TradeMaster Enterprise License")
                .url("https://trademaster.com/license");
    }

    /**
     * Create server list for different environments
     */
    private List<Server> createServerList() {
        return List.of(
                createServer("http://localhost:" + serverPort, "Development Server"),
                createServer(kongGatewayUrl, "Kong API Gateway"),
                createServer("https://api.trademaster.com", "Production Server"),
                createServer("https://staging-api.trademaster.com", "Staging Server")
        );
    }

    /**
     * Create individual server configuration
     */
    private Server createServer(String url, String description) {
        return new Server()
                .url(url)
                .description(description);
    }

    /**
     * Create OpenAPI components with security schemes and common schemas
     */
    private Components createComponents() {
        return new Components()
                .securitySchemes(createSecuritySchemes())
                .schemas(createCommonSchemas());
    }

    /**
     * Create security schemes for authentication
     */
    private Map<String, SecurityScheme> createSecuritySchemes() {
        return Map.of(
                "bearerAuth", createBearerTokenScheme(),
                "apiKeyAuth", createApiKeyScheme(),
                "kongJwtAuth", createKongJwtScheme()
        );
    }

    /**
     * Create Bearer Token authentication scheme
     */
    private SecurityScheme createBearerTokenScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Bearer token authentication.

                        **Usage**: Include in Authorization header as `Bearer <token>`

                        **Token Structure**:
                        - **Header**: Algorithm and token type
                        - **Payload**: User claims, roles, and expiration
                        - **Signature**: HMAC SHA256 signature

                        **Expiration**: 15 minutes (configurable)
                        **Refresh**: Use refresh token endpoint to obtain new access token
                        """);
    }

    /**
     * Create API Key authentication scheme
     */
    private SecurityScheme createApiKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("""
                        API Key authentication for service-to-service communication.

                        **Usage**: Include in X-API-Key header

                        **Scope**: Limited to internal service endpoints
                        **Rate Limits**: Higher limits than public endpoints
                        **Security**: API keys should be rotated regularly
                        """);
    }

    /**
     * Create Kong JWT authentication scheme
     */
    private SecurityScheme createKongJwtScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Kong API Gateway JWT authentication.

                        **Usage**: Managed by Kong API Gateway
                        **Validation**: Kong validates JWT before forwarding to service
                        **Benefits**: Centralized authentication, rate limiting, monitoring
                        """);
    }

    /**
     * Create common schemas for reuse across endpoints
     */
    private Map<String, Schema> createCommonSchemas() {
        return Map.of(
                "ErrorResponse", createErrorResponseSchema(),
                "SuccessResponse", createSuccessResponseSchema(),
                "PaginationMeta", createPaginationMetaSchema(),
                "SecurityAuditLog", createSecurityAuditLogSchema(),
                "RateLimitInfo", createRateLimitInfoSchema()
        );
    }

    /**
     * Create standardized error response schema
     */
    private Schema<?> createErrorResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Standardized error response with correlation ID for debugging")
                .addProperty("error", new Schema<>().type("string").description("Error type or code"))
                .addProperty("message", new Schema<>().type("string").description("Human-readable error message"))
                .addProperty("details", new Schema<>().type("string").description("Additional error details"))
                .addProperty("correlationId", new Schema<>().type("string").description("Unique request correlation ID"))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Error timestamp"))
                .addProperty("path", new Schema<>().type("string").description("Request path that caused the error"))
                .example(Map.of(
                        "error", "VALIDATION_ERROR",
                        "message", "Invalid request parameters",
                        "details", "Email format is invalid",
                        "correlationId", "550e8400-e29b-41d4-a716-446655440000",
                        "timestamp", "2024-01-15T10:30:00Z",
                        "path", "/api/v1/auth/register"
                ));
    }

    /**
     * Create standardized success response schema
     */
    private Schema<?> createSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Standardized success response")
                .addProperty("data", new Schema<>().description("Response data"))
                .addProperty("message", new Schema<>().type("string").description("Success message"))
                .addProperty("correlationId", new Schema<>().type("string").description("Request correlation ID"))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Response timestamp"));
    }

    /**
     * Create pagination metadata schema
     */
    private Schema<?> createPaginationMetaSchema() {
        return new Schema<>()
                .type("object")
                .description("Pagination metadata for list endpoints")
                .addProperty("page", new Schema<>().type("integer").description("Current page number"))
                .addProperty("size", new Schema<>().type("integer").description("Page size"))
                .addProperty("totalElements", new Schema<>().type("integer").description("Total number of elements"))
                .addProperty("totalPages", new Schema<>().type("integer").description("Total number of pages"))
                .addProperty("hasNext", new Schema<>().type("boolean").description("Whether there are more pages"))
                .addProperty("hasPrevious", new Schema<>().type("boolean").description("Whether there are previous pages"));
    }

    /**
     * Create security audit log schema
     */
    private Schema<?> createSecurityAuditLogSchema() {
        return new Schema<>()
                .type("object")
                .description("Security audit log entry")
                .addProperty("id", new Schema<>().type("string").description("Unique audit log ID"))
                .addProperty("userId", new Schema<>().type("string").description("User ID involved in the event"))
                .addProperty("eventType", new Schema<>().type("string").description("Type of security event"))
                .addProperty("status", new Schema<>().type("string").description("Event status (SUCCESS/FAILURE)"))
                .addProperty("ipAddress", new Schema<>().type("string").description("Client IP address"))
                .addProperty("userAgent", new Schema<>().type("string").description("Client user agent"))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Event timestamp"))
                .addProperty("riskLevel", new Schema<>().type("string").description("Risk level (LOW/MEDIUM/HIGH)"))
                .addProperty("correlationId", new Schema<>().type("string").description("Request correlation ID"));
    }

    /**
     * Create rate limit information schema
     */
    private Schema<?> createRateLimitInfoSchema() {
        return new Schema<>()
                .type("object")
                .description("Rate limiting information")
                .addProperty("limit", new Schema<>().type("integer").description("Rate limit threshold"))
                .addProperty("remaining", new Schema<>().type("integer").description("Remaining requests"))
                .addProperty("resetTime", new Schema<>().type("string").format("date-time").description("Rate limit reset time"))
                .addProperty("retryAfter", new Schema<>().type("integer").description("Retry after seconds"));
    }

    /**
     * Create security requirement for protected endpoints
     */
    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement()
                .addList("bearerAuth")
                .addList("apiKeyAuth");
    }
}