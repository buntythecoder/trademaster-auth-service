package com.trademaster.marketdata.config;

import com.trademaster.common.openapi.AbstractOpenApiConfig;
import com.trademaster.common.properties.CommonServiceProperties;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 Configuration for Market Data Service
 *
 * Extends AbstractOpenApiConfig from common library with market-data-service specific documentation.
 * Complies with TradeMaster Golden Specification requirements.
 *
 * Features:
 * - API metadata and versioning (from AbstractOpenApiConfig)
 * - Security schemes (JWT, API Key) (from AbstractOpenApiConfig)
 * - Market-data-service specific descriptions and servers
 * - OpenAPI documentation (from AbstractOpenApiConfig)
 *
 * Access documentation at:
 * - Swagger UI: http://localhost:8084/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8084/v3/api-docs
 * - OpenAPI YAML: http://localhost:8084/v3/api-docs.yaml
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Using Common Library)
 */
@Configuration
public class OpenAPIConfig extends AbstractOpenApiConfig {

    @Value("${server.port:8084}")
    private String serverPort;

    public OpenAPIConfig(CommonServiceProperties properties) {
        super(properties);
    }

    /**
     * Override to customize API metadata with market-data-service details
     */
    @Override
    protected Info createApiInfo() {
        return super.createApiInfo()
            .description("""
                Real-time and historical market data API with multi-provider support and circuit breaker protection.

                **Core Capabilities**:
                - Real-time quotes from NSE, BSE, Alpha Vantage
                - Historical OHLCV data with full market history
                - Technical indicators (RSI, MACD, Bollinger Bands, Moving Averages)
                - Price alerts with webhook notifications
                - Market scanner for pattern detection
                - Economic calendar integration
                - Market news and sentiment analysis
                - Charting data with customizable timeframes

                **Supported Exchanges**:
                - NSE (National Stock Exchange of India)
                - BSE (Bombay Stock Exchange)
                - NYSE, NASDAQ (via Alpha Vantage)
                - FOREX markets
                - Cryptocurrency markets

                **Data Providers**:
                - NSE Direct API (primary for Indian markets)
                - BSE Direct API (primary for BSE)
                - Alpha Vantage (fallback for international markets)
                - Multi-provider fallback strategy for high availability

                **Technology Stack**:
                - Java 24 with Virtual Threads for unlimited scalability
                - Spring Boot 3.5.3 with Spring MVC (non-reactive)
                - Functional programming with Result types and pattern matching
                - Circuit breakers with Resilience4j for fault tolerance
                - Real-time data processing with InfluxDB
                - Redis caching for sub-100ms response times
                - Kafka for event streaming
                - PostgreSQL for persistent storage

                **Performance Targets**:
                - Real-time quotes: <100ms with caching
                - Historical data: <500ms for full dataset
                - Technical indicators: <200ms calculation time
                - API response: <200ms for standard operations
                - Concurrent users: 10,000+ supported

                **Resilience Features**:
                - Circuit breakers on ALL external API calls (Rule #25)
                - Automatic provider fallback on failures
                - Request rate limiting per provider
                - Graceful degradation when providers unavailable
                - Comprehensive health monitoring

                **Security**:
                - JWT-based authentication
                - Role-based access control (TRADER, ADMIN)
                - API key authentication for internal services
                - Zero-trust security architecture
                - Comprehensive audit logging
                """);
    }

    /**
     * Override to add market-data-service specific servers
     */
    @Override
    protected List<Server> createServerList() {
        return List.of(
            createServer("http://localhost:" + serverPort, "Local development server"),
            createServer("https://dev-api.trademaster.com/market-data",
                "Development environment (via Kong Gateway)"),
            createServer("https://api.trademaster.com/market-data",
                "Production environment (via Kong Gateway)")
        );
    }
}
