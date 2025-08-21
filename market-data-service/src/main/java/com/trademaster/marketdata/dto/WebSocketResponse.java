package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket Response DTO
 * 
 * Standard response format for WebSocket communications including:
 * - Response type and status
 * - Timestamp for client-side ordering
 * - Flexible data payload
 * - Error handling information
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketResponse {

    /**
     * Response type
     */
    private String type;

    /**
     * Response status
     */
    private String status;

    /**
     * Server timestamp
     */
    private Long timestamp;

    /**
     * Response data payload
     */
    private Object data;

    /**
     * Error information (if applicable)
     */
    private ErrorInfo error;

    /**
     * Message sequence number
     */
    private Long sequenceNumber;

    /**
     * Server identifier
     */
    private String serverId;

    /**
     * Error information nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        private String code;
        private String message;
        private String details;
        private Long timestamp;
    }

    /**
     * Create success response
     */
    public static WebSocketResponse success(String type, Object data) {
        return WebSocketResponse.builder()
            .type(type)
            .status("success")
            .timestamp(System.currentTimeMillis())
            .data(data)
            .build();
    }

    /**
     * Create error response
     */
    public static WebSocketResponse error(String type, String errorCode, String errorMessage) {
        return WebSocketResponse.builder()
            .type(type)
            .status("error")
            .timestamp(System.currentTimeMillis())
            .error(ErrorInfo.builder()
                .code(errorCode)
                .message(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build())
            .build();
    }
}