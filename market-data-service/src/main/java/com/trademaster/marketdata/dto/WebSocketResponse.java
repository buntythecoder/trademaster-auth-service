package com.trademaster.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * WebSocket Response DTO
 *
 * Standard response format for WebSocket communications including:
 * - Response type and status
 * - Timestamp for client-side ordering
 * - Flexible data payload
 * - Error handling information
 *
 * Converted to immutable record for MANDATORY RULE #9 compliance.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketResponse(
    String type,
    String status,
    Long timestamp,
    Object data,
    ErrorInfo error,
    Long sequenceNumber,
    String serverId
) {

    /**
     * Error information nested record
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorInfo(
        String code,
        String message,
        String details,
        Long timestamp
    ) {}

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