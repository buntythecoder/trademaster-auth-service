package com.trademaster.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 Configuration for Payment Service
 * Implements comprehensive API documentation per Golden Specification
 *
 * Compliance:
 * - Golden Spec: OpenAPI 3.0 with complete metadata
 * - Rule 16: Dynamic configuration with @Value
 * - Rule 10: @Slf4j for structured logging
 * - Rule 23: Security schemes documentation (JWT + API Key)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class OpenAPIConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port}")
    private int serverPort;

    @Value("${trademaster.kong.gateway-url:http://localhost:8000}")
    private String kongGatewayUrl;

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        log.info("Configuring OpenAPI documentation for Payment Service");

        return new OpenAPI()
            .info(createApiInfo())
            .servers(createServers())
            .addSecurityItem(createJwtSecurityRequirement())
            .addSecurityItem(createApiKeySecurityRequirement())
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("JWT", createJwtSecurityScheme())
                .addSecuritySchemes("API_KEY", createApiKeySecurityScheme())
            );
    }

    private Info createApiInfo() {
        return new Info()
            .title("TradeMaster Payment Service API")
            .description("""
                **Payment Gateway Integration Service**

                Provides comprehensive payment processing capabilities for TradeMaster platform:

                ### Features
                - **Multi-Gateway Support**: Razorpay (Indian market) and Stripe (International)
                - **Payment Methods**: UPI, Cards, Net Banking, Wallets
                - **Subscription Billing**: Automated recurring payments
                - **Webhook Processing**: Real-time payment status updates
                - **Refund Management**: Full and partial refund support
                - **Invoice Generation**: Automated invoice creation
                - **PCI Compliance**: Secure payment handling

                ### Security
                - **External APIs**: JWT Bearer authentication
                - **Internal APIs**: API Key authentication for service-to-service
                - **Circuit Breakers**: Resilience4j for fault tolerance
                - **Rate Limiting**: Protection against abuse

                ### Integration
                - **Event Bus**: Kafka-based event publishing
                - **Consul**: Service discovery and configuration
                - **Kong**: API Gateway integration

                ### Golden Specification Compliance
                - Java 24 with Virtual Threads
                - Functional Programming First
                - SOLID Principles
                - Zero Trust Security
                """)
            .version("1.0.0")
            .contact(createContact())
            .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
            .name("TradeMaster Platform Team")
            .email("platform@trademaster.app")
            .url("https://trademaster.app");
    }

    private License createLicense() {
        return new License()
            .name("Proprietary")
            .url("https://trademaster.app/license");
    }

    private List<Server> createServers() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server"),

            new Server()
                .url(kongGatewayUrl)
                .description("Kong API Gateway"),

            new Server()
                .url("https://api.trademaster.app")
                .description("Production API Gateway")
        );
    }

    private SecurityRequirement createJwtSecurityRequirement() {
        return new SecurityRequirement().addList("JWT");
    }

    private SecurityRequirement createApiKeySecurityRequirement() {
        return new SecurityRequirement().addList("API_KEY");
    }

    private SecurityScheme createJwtSecurityScheme() {
        return new SecurityScheme()
            .name("JWT")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .description("""
                **JWT Bearer Authentication** for external API access.

                ### How to Obtain Token
                1. Authenticate via Auth Service: `POST /api/v1/auth/login`
                2. Copy the JWT token from response
                3. Add to Authorization header: `Bearer <token>`

                ### Token Structure
                - Issuer: Auth Service
                - Expiry: 1 hour (3600 seconds)
                - Claims: userId, email, roles

                ### Usage
                ```
                Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                ```
                """);
    }

    private SecurityScheme createApiKeySecurityScheme() {
        return new SecurityScheme()
            .name("API_KEY")
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .description("""
                **API Key Authentication** for internal service-to-service communication.

                ### Configuration
                - Header: `X-API-Key`
                - Scope: `/api/internal/**` endpoints only
                - Security: Role-based access with `ROLE_SERVICE`

                ### Environment Variables
                - Development: `INTERNAL_API_KEY=trademaster_internal_dev_key`
                - Production: Stored in Consul KV or Vault

                ### Usage
                ```
                X-API-Key: trademaster_internal_dev_key
                X-Correlation-ID: <uuid>
                ```

                ### Important
                - Never use in client applications
                - Rotate keys every 90 days
                - Monitor usage with correlation IDs
                """);
    }
}
