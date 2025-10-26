package com.trademaster.payment.dto.internal;

import java.time.Instant;

/**
 * Internal Error Response DTO
 * MANDATORY: Rule #9 - Immutable Records for all DTOs
 *
 * Standardized error response for internal service-to-service communication.
 * Includes correlation ID for distributed tracing and debugging.
 *
 * Compliance:
 * - Rule #9: Immutable records
 * - Rule #15: Structured logging with correlation IDs
 * - Rule #18: Descriptive naming
 *
 * @param statusCode HTTP status code
 * @param message Error message description
 * @param correlationId Distributed tracing correlation ID
 * @param timestamp Error occurrence timestamp
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record InternalErrorResponse(
    int statusCode,
    String message,
    String correlationId,
    Instant timestamp
) {}
