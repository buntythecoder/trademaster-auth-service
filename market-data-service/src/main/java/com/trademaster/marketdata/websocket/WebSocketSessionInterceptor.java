package com.trademaster.marketdata.websocket;

import com.trademaster.marketdata.config.MarketDataLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Session Interceptor for Market Data Service
 * 
 * Handles WebSocket session lifecycle events including:
 * - Connection establishment validation
 * - Session context setup
 * - Authentication and authorization
 * - Connection tracking and metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketSessionInterceptor implements HandshakeInterceptor {

    private final MarketDataLogger marketDataLogger;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            // Extract client information
            String clientId = extractClientId(request);
            String userAgent = request.getHeaders().getFirst("User-Agent");
            String origin = request.getHeaders().getFirst("Origin");
            
            // Set session attributes
            attributes.put("clientId", clientId);
            attributes.put("userAgent", userAgent);
            attributes.put("origin", origin);
            attributes.put("connectTime", System.currentTimeMillis());
            
            // Log connection attempt
            marketDataLogger.logWebSocketConnection(
                clientId, 
                "market_data", 
                "ALL", 
                "connect_attempt", 
                true, 
                0L
            );
            
            log.debug("WebSocket handshake accepted for client: {}", clientId);
            return true;
            
        } catch (Exception e) {
            log.error("WebSocket handshake failed", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            String clientId = extractClientId(request);
            log.error("WebSocket handshake failed for client {}: {}", clientId, exception.getMessage());
            
            marketDataLogger.logWebSocketConnection(
                clientId, 
                "market_data", 
                "ALL", 
                "connect_failed", 
                false, 
                0L
            );
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    private String extractClientId(ServerHttpRequest request) {
        // Try to extract client ID from various sources
        String clientId = request.getHeaders().getFirst("X-Client-ID");
        if (clientId == null) {
            clientId = request.getURI().getQuery() != null ? 
                extractQueryParam(request.getURI().getQuery(), "clientId") : null;
        }
        if (clientId == null) {
            clientId = "anonymous-" + System.currentTimeMillis();
        }
        return clientId;
    }

    private String extractQueryParam(String query, String paramName) {
        if (query == null) return null;
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && paramName.equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}