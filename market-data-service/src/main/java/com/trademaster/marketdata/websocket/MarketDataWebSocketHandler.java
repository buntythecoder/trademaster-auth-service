package com.trademaster.marketdata.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.marketdata.dto.MarketDataMessage;
import com.trademaster.marketdata.dto.SubscriptionRequest;
import com.trademaster.marketdata.dto.WebSocketResponse;
import com.trademaster.marketdata.service.MarketDataSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket Handler for Real-time Market Data Streaming
 * 
 * Features:
 * - High-frequency market data streaming
 * - Symbol-based subscriptions
 * - Connection lifecycle management
 * - Error handling and recovery
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataWebSocketHandler implements WebSocketHandler {

    private final WebSocketConnectionManager connectionManager;
    private final MarketDataSubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;
    
    private final AtomicLong messageCounter = new AtomicLong(0);
    private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String userId = extractUserId(session);
        
        log.info("WebSocket connection established - Session: {}, User: {}, URI: {}", 
            sessionId, userId, session.getUri());
        
        // Register connection
        boolean registered = connectionManager.addConnection(session);
        if (!registered) {
            log.warn("Connection rejected - limit exceeded for session: {}", sessionId);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Connection limit exceeded"));
            return;
        }
        
        // Send welcome message
        WebSocketResponse welcomeResponse = WebSocketResponse.builder()
            .type("connection")
            .status("connected")
            .timestamp(System.currentTimeMillis())
            .data(Map.of(
                "sessionId", sessionId,
                "serverTime", System.currentTimeMillis(),
                "supportedSymbols", subscriptionService.getSupportedSymbols()
            ))
            .build();
        
        sendMessage(session, welcomeResponse);
        
        // Initialize heartbeat
        lastHeartbeat.put(sessionId, System.currentTimeMillis());
        
        log.debug("WebSocket connection setup completed for session: {}", sessionId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        
        if (!(message instanceof TextMessage textMessage)) {
            log.warn("Received non-text message from session: {}", sessionId);
            return;
        }
        
        try {
            String payload = textMessage.getPayload();
            log.debug("Received message from session {}: {}", sessionId, payload);

            // Parse message with proper type safety (RULE #8: Zero warnings)
            Map<String, Object> messageMap = objectMapper.readValue(payload,
                new TypeReference<Map<String, Object>>() {});
            String messageType = (String) messageMap.get("type");
            
            switch (messageType) {
                case "subscribe" -> handleSubscription(session, messageMap);
                case "unsubscribe" -> handleUnsubscription(session, messageMap);
                case "heartbeat" -> handleHeartbeat(session);
                case "getSnapshot" -> handleSnapshotRequest(session, messageMap);
                default -> {
                    log.warn("Unknown message type '{}' from session: {}", messageType, sessionId);
                    sendErrorResponse(session, "Unknown message type: " + messageType);
                }
            }
            
            // Update message counter
            messageCounter.incrementAndGet();
            
        } catch (Exception e) {
            log.error("Error processing message from session {}: {}", sessionId, e.getMessage(), e);
            sendErrorResponse(session, "Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket transport error for session {}: {}", sessionId, exception.getMessage(), exception);
        
        // Clean up subscriptions
        subscriptionService.removeAllSubscriptions(sessionId);
        
        // Remove from connection manager
        connectionManager.removeConnection(session);
        lastHeartbeat.remove(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket connection closed - Session: {}, Status: {}, Reason: {}", 
            sessionId, closeStatus.getCode(), closeStatus.getReason());
        
        // Clean up subscriptions
        subscriptionService.removeAllSubscriptions(sessionId);
        
        // Remove from connection manager
        connectionManager.removeConnection(session);
        lastHeartbeat.remove(sessionId);
        
        log.debug("WebSocket cleanup completed for session: {}", sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false; // We don't support partial messages for simplicity
    }

    /**
     * Handle subscription requests
     */
    private void handleSubscription(WebSocketSession session, Map<String, Object> messageMap) 
            throws IOException {
        String sessionId = session.getId();
        
        try {
            SubscriptionRequest request = objectMapper.convertValue(
                messageMap.get("data"), SubscriptionRequest.class);
            
            log.info("Processing subscription request from session {}: {}", sessionId, request);
            
            // Validate subscription request
            if (request.symbols() == null || request.symbols().isEmpty()) {
                sendErrorResponse(session, "Symbols are required for subscription");
                return;
            }
            
            // Process subscription
            boolean success = subscriptionService.subscribe(sessionId, request);
            
            if (success) {
                WebSocketResponse response = WebSocketResponse.builder()
                    .type("subscribed")
                    .status("success")
                    .timestamp(System.currentTimeMillis())
                    .data(Map.of(
                        "symbols", request.symbols(),
                        "dataTypes", request.dataTypes(),
                        "subscriptionId", subscriptionService.getSubscriptionId(sessionId)
                    ))
                    .build();

                sendMessage(session, response);
                log.info("Subscription successful for session {}: {}", sessionId, request.symbols());
            } else {
                sendErrorResponse(session, "Failed to create subscription");
            }
            
        } catch (Exception e) {
            log.error("Error processing subscription for session {}: {}", sessionId, e.getMessage());
            sendErrorResponse(session, "Invalid subscription request");
        }
    }

    /**
     * Handle unsubscription requests
     */
    private void handleUnsubscription(WebSocketSession session, Map<String, Object> messageMap) 
            throws IOException {
        String sessionId = session.getId();
        
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> symbols = (java.util.List<String>) messageMap.get("symbols");
            
            if (symbols == null || symbols.isEmpty()) {
                // Unsubscribe from all
                subscriptionService.removeAllSubscriptions(sessionId);
            } else {
                // Unsubscribe from specific symbols
                subscriptionService.unsubscribe(sessionId, symbols);
            }
            
            WebSocketResponse response = WebSocketResponse.builder()
                .type("unsubscribed")
                .status("success")
                .timestamp(System.currentTimeMillis())
                .data(Map.of("symbols", symbols != null ? symbols : "all"))
                .build();
            
            sendMessage(session, response);
            log.info("Unsubscription successful for session {}: {}", sessionId, symbols);
            
        } catch (Exception e) {
            log.error("Error processing unsubscription for session {}: {}", sessionId, e.getMessage());
            sendErrorResponse(session, "Invalid unsubscription request");
        }
    }

    /**
     * Handle heartbeat messages
     */
    private void handleHeartbeat(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        lastHeartbeat.put(sessionId, System.currentTimeMillis());
        
        WebSocketResponse response = WebSocketResponse.builder()
            .type("heartbeat")
            .status("pong")
            .timestamp(System.currentTimeMillis())
            .build();
        
        sendMessage(session, response);
    }

    /**
     * Handle snapshot requests
     */
    private void handleSnapshotRequest(WebSocketSession session, Map<String, Object> messageMap) 
            throws IOException {
        String sessionId = session.getId();
        
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> symbols = (java.util.List<String>) messageMap.get("symbols");
            
            if (symbols == null || symbols.isEmpty()) {
                sendErrorResponse(session, "Symbols are required for snapshot request");
                return;
            }
            
            // Get snapshot data
            Map<String, Object> snapshotData = subscriptionService.getSnapshot(symbols);
            
            WebSocketResponse response = WebSocketResponse.builder()
                .type("snapshot")
                .status("success")
                .timestamp(System.currentTimeMillis())
                .data(snapshotData)
                .build();
            
            sendMessage(session, response);
            log.debug("Snapshot sent for session {}: {}", sessionId, symbols);
            
        } catch (Exception e) {
            log.error("Error processing snapshot request for session {}: {}", sessionId, e.getMessage());
            sendErrorResponse(session, "Failed to get snapshot data");
        }
    }

    /**
     * Send market data message to session
     */
    public void sendMarketData(String sessionId, MarketDataMessage marketData) {
        WebSocketSession session = connectionManager.getSession(sessionId);
        if (session == null || !session.isOpen()) {
            return;
        }
        
        try {
            WebSocketResponse response = WebSocketResponse.builder()
                .type("marketData")
                .status("data")
                .timestamp(System.currentTimeMillis())
                .data(marketData)
                .build();
            
            sendMessage(session, response);
        } catch (Exception e) {
            log.error("Error sending market data to session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send message to WebSocket session
     */
    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    /**
     * Send error response to session
     */
    private void sendErrorResponse(WebSocketSession session, String errorMessage) {
        try {
            WebSocketResponse response = WebSocketResponse.builder()
                .type("error")
                .status("error")
                .timestamp(System.currentTimeMillis())
                .data(Map.of("message", errorMessage))
                .build();
            
            sendMessage(session, response);
        } catch (Exception e) {
            log.error("Error sending error response: {}", e.getMessage());
        }
    }

    /**
     * Extract user ID from WebSocket session
     */
    private String extractUserId(WebSocketSession session) {
        // Extract from session attributes (set during authentication)
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : "anonymous";
    }

    /**
     * Get connection statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "activeConnections", connectionManager.getActiveConnections(),
            "totalMessagesProcessed", messageCounter.get(),
            "connectedSessions", connectionManager.getSessionIds()
        );
    }
}