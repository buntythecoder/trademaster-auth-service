package com.trademaster.marketdata.config;

import com.trademaster.marketdata.websocket.MarketDataWebSocketHandler;
import com.trademaster.marketdata.websocket.WebSocketConnectionManager;
import com.trademaster.marketdata.websocket.WebSocketSessionInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.List;

/**
 * WebSocket Configuration for Real-time Market Data Streaming
 * 
 * Configures:
 * - High-performance WebSocket handlers for market data streaming
 * - Connection management with scalability optimizations
 * - Authentication and authorization for WebSocket connections
 * - CORS configuration for cross-origin access
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketDataWebSocketHandler marketDataWebSocketHandler;
    private final WebSocketSessionInterceptor webSocketSessionInterceptor;

    @Value("${app.market-data.websocket.endpoint:/ws/market-data}")
    private String marketDataEndpoint;

    @Value("${app.market-data.websocket.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.market-data.websocket.connection-limit:10000}")
    private int connectionLimit;

    /**
     * Register WebSocket handlers with optimized configuration
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("Registering WebSocket handlers for market data streaming");
        
        registry.addHandler(marketDataWebSocketHandler, marketDataEndpoint)
            .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
            .addInterceptors(webSocketSessionInterceptor)
            .withSockJS(); // Enable SockJS fallback for older browsers
        
        registry.addHandler(marketDataWebSocketHandler, marketDataEndpoint + "/native")
            .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
            .addInterceptors(webSocketSessionInterceptor);
        
        log.info("WebSocket handlers registered successfully - Endpoint: {}, Connection Limit: {}", 
            marketDataEndpoint, connectionLimit);
    }

    /**
     * Configure servlet container for WebSocket optimization
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        
        // Buffer sizes for high-frequency data
        container.setMaxTextMessageBufferSize(32768); // 32KB for text messages
        container.setMaxBinaryMessageBufferSize(65536); // 64KB for binary messages
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes idle timeout
        
        log.info("WebSocket container configured with optimized buffer sizes");
        return container;
    }

    /**
     * WebSocket Connection Manager Bean
     */
    @Bean
    public WebSocketConnectionManager webSocketConnectionManager() {
        return new WebSocketConnectionManager(connectionLimit);
    }
}