package com.trademaster.auth.config;

import com.trademaster.auth.security.KongJwtAuthenticationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * Kong API Gateway Configuration
 *
 * MANDATORY: Golden Specification Requirement - Kong API Gateway Integration
 *
 * Features:
 * - Kong admin API client configuration
 * - JWT authentication provider for Kong tokens
 * - Service registration and health checks
 * - Rate limiting integration
 * - Circuit breaker integration with Kong upstream health
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
@Profile("!test") // Exclude from test profiles
public class KongConfiguration {

    @Value("${kong.admin.url:http://localhost:8001}")
    private String kongAdminUrl;

    @Value("${kong.gateway.url:http://localhost:8000}")
    private String kongGatewayUrl;

    @Value("${kong.service.name:trademaster-auth-service}")
    private String serviceName;

    @Value("${kong.service.url:http://auth-service:8080}")
    private String serviceUrl;

    @Value("${kong.admin.auth.enabled:false}")
    private boolean adminAuthEnabled;

    @Value("${kong.admin.auth.token:}")
    private String adminAuthToken;

    @Value("${trademaster.security.kong.headers.consumer-id:X-Consumer-ID}")
    private String consumerIdHeader;

    @Value("${trademaster.security.kong.headers.consumer-username:X-Consumer-Username}")
    private String consumerUsernameHeader;

    @Value("${trademaster.security.kong.headers.consumer-custom-id:X-Consumer-Custom-ID}")
    private String consumerCustomIdHeader;

    @Value("${trademaster.security.kong.headers.api-key:X-API-Key}")
    private String apiKeyHeader;

    /**
     * Kong Admin API client for service management
     */
    @Bean
    public RestTemplate kongAdminClient() {
        RestTemplate restTemplate = new RestTemplate();

        // Add interceptors for authentication if enabled
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Add Kong admin authentication header if configured
            java.util.Optional.of(adminAuthEnabled)
                .filter(enabled -> enabled)
                .map(enabled -> adminAuthToken)
                .filter(token -> !token.isEmpty())
                .ifPresent(token -> request.getHeaders().set("Kong-Admin-Token", token));

            // Add correlation ID for tracing
            String correlationId = java.util.UUID.randomUUID().toString();
            request.getHeaders().set("X-Correlation-ID", correlationId);

            return execution.execute(request, body);
        });

        log.info("Kong Admin API client configured for URL: {}", kongAdminUrl);
        return restTemplate;
    }

    /**
     * Kong JWT Authentication Provider for handling Kong-generated JWTs
     */
    @Bean
    public KongJwtAuthenticationProvider kongJwtAuthenticationProvider() {
        return new KongJwtAuthenticationProvider();
    }

    /**
     * Kong Gateway client for health checks and service communication
     */
    @Bean
    public RestTemplate kongGatewayClient() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Add service identification headers
            request.getHeaders().set("X-Service-Name", serviceName);
            request.getHeaders().set("X-Service-Version", "1.0.0");

            // Add correlation ID for tracing
            String correlationId = java.util.UUID.randomUUID().toString();
            request.getHeaders().set("X-Correlation-ID", correlationId);

            return execution.execute(request, body);
        });

        log.info("Kong Gateway client configured for URL: {}", kongGatewayUrl);
        return restTemplate;
    }

    /**
     * Kong service configuration bean for dependency injection
     */
    @Bean
    public KongServiceConfig kongServiceConfig() {
        return new KongServiceConfig(
                kongAdminUrl,
                kongGatewayUrl,
                serviceName,
                serviceUrl,
                adminAuthEnabled,
                adminAuthToken
        );
    }

    /**
     * Kong Headers configuration for ServiceApiKeyFilter
     */
    @Bean
    public KongHeaders getHeaders() {
        return new KongHeaders(consumerIdHeader, consumerUsernameHeader,
                              consumerCustomIdHeader, apiKeyHeader);
    }

    /**
     * Kong Headers record for immutable header configuration
     */
    public record KongHeaders(
            String consumerId,
            String consumerUsername,
            String consumerCustomId,
            String apiKey
    ) {};

    /**
     * Kong service configuration record for immutable config
     */
    public record KongServiceConfig(
            String adminUrl,
            String gatewayUrl,
            String serviceName,
            String serviceUrl,
            boolean adminAuthEnabled,
            String adminAuthToken
    ) {
        public boolean isConfigured() {
            return adminUrl != null && !adminUrl.isEmpty() &&
                   gatewayUrl != null && !gatewayUrl.isEmpty() &&
                   serviceName != null && !serviceName.isEmpty();
        }

        public String getHealthCheckUrl() {
            return gatewayUrl + "/api/v1/auth/health";
        }

        public String getAdminServiceUrl() {
            return adminUrl + "/services/" + serviceName;
        }

        public String getAdminUpstreamUrl() {
            return adminUrl + "/upstreams/auth-service-upstream";
        }
    }
}