package com.trademaster.payment.domain;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentStatus;

import java.util.Map;

/**
 * Webhook Event Type Hierarchy
 * Sealed interface for type-safe webhook event processing
 *
 * Compliance:
 * - Rule 3: Functional Programming - Sealed types for pattern matching
 * - Rule 4: Advanced Design Patterns - Type hierarchy with sealed interfaces
 * - Rule 9: Immutability - Records for all event types
 * - Rule 14: Pattern matching with sealed types
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface WebhookEvent
    permits WebhookEvent.PaymentSucceeded,
            WebhookEvent.PaymentFailed,
            WebhookEvent.PaymentAuthorized,
            WebhookEvent.PaymentCaptured,
            WebhookEvent.RefundProcessed,
            WebhookEvent.RefundFailed,
            WebhookEvent.Unknown {

    /**
     * Get payment gateway that generated this event
     */
    PaymentGateway gateway();

    /**
     * Get original event type string
     */
    String eventType();

    /**
     * Get event payload data
     */
    Map<String, Object> payload();

    /**
     * Payment succeeded event
     * Fired when payment is successfully completed
     */
    record PaymentSucceeded(
        PaymentGateway gateway,
        String eventType,
        String paymentId,
        String orderId,
        Map<String, Object> payload
    ) implements WebhookEvent {
        public PaymentStatus toPaymentStatus() {
            return PaymentStatus.COMPLETED;
        }
    }

    /**
     * Payment failed event
     * Fired when payment processing fails
     */
    record PaymentFailed(
        PaymentGateway gateway,
        String eventType,
        String paymentId,
        String orderId,
        String errorCode,
        String errorMessage,
        Map<String, Object> payload
    ) implements WebhookEvent {
        public PaymentStatus toPaymentStatus() {
            return PaymentStatus.FAILED;
        }
    }

    /**
     * Payment authorized event
     * Fired when payment is authorized but not captured
     */
    record PaymentAuthorized(
        PaymentGateway gateway,
        String eventType,
        String paymentId,
        String orderId,
        Map<String, Object> payload
    ) implements WebhookEvent {
        public PaymentStatus toPaymentStatus() {
            return PaymentStatus.AUTHORIZED;
        }
    }

    /**
     * Payment captured event
     * Fired when authorized payment is captured
     */
    record PaymentCaptured(
        PaymentGateway gateway,
        String eventType,
        String paymentId,
        String orderId,
        Map<String, Object> payload
    ) implements WebhookEvent {
        public PaymentStatus toPaymentStatus() {
            return PaymentStatus.COMPLETED;
        }
    }

    /**
     * Refund processed event
     * Fired when refund is successfully processed
     */
    record RefundProcessed(
        PaymentGateway gateway,
        String eventType,
        String refundId,
        String paymentId,
        Map<String, Object> payload
    ) implements WebhookEvent {
        public PaymentStatus toPaymentStatus() {
            return PaymentStatus.REFUNDED;
        }
    }

    /**
     * Refund failed event
     * Fired when refund processing fails
     */
    record RefundFailed(
        PaymentGateway gateway,
        String eventType,
        String refundId,
        String paymentId,
        String errorMessage,
        Map<String, Object> payload
    ) implements WebhookEvent {
        public PaymentStatus toPaymentStatus() {
            return PaymentStatus.COMPLETED; // Keep original status
        }
    }

    /**
     * Unknown event
     * Fallback for unrecognized event types
     */
    record Unknown(
        PaymentGateway gateway,
        String eventType,
        Map<String, Object> payload
    ) implements WebhookEvent {
    }

    /**
     * Factory method for creating webhook events from raw data
     * Pattern matching for event type routing (NO if-else)
     */
    @SuppressWarnings("unchecked")
    static Result<WebhookEvent, String> fromRawEvent(
            PaymentGateway gateway,
            String eventType,
            Map<String, Object> payload
    ) {
        return switch (gateway) {
            case RAZORPAY -> parseRazorpayEvent(eventType, payload);
            case STRIPE -> parseStripeEvent(eventType, payload);
            case UPI -> parseRazorpayEvent(eventType, payload); // UPI uses Razorpay
        };
    }

    /**
     * Parse Razorpay webhook event
     * Pattern matching for event type (NO if-else)
     */
    @SuppressWarnings("unchecked")
    private static Result<WebhookEvent, String> parseRazorpayEvent(
            String eventType,
            Map<String, Object> payload
    ) {
        return switch (eventType) {
            case "payment.captured" -> {
                Map<String, Object> paymentEntity = extractRazorpayPaymentEntity(payload);
                yield Result.success(new PaymentSucceeded(
                    PaymentGateway.RAZORPAY,
                    eventType,
                    (String) paymentEntity.get("id"),
                    (String) paymentEntity.get("order_id"),
                    payload
                ));
            }
            case "payment.failed" -> {
                Map<String, Object> paymentEntity = extractRazorpayPaymentEntity(payload);
                Map<String, Object> error = (Map<String, Object>) paymentEntity.get("error");
                yield Result.success(new PaymentFailed(
                    PaymentGateway.RAZORPAY,
                    eventType,
                    (String) paymentEntity.get("id"),
                    (String) paymentEntity.get("order_id"),
                    error != null ? (String) error.get("code") : null,
                    error != null ? (String) error.get("description") : null,
                    payload
                ));
            }
            case "payment.authorized" -> {
                Map<String, Object> paymentEntity = extractRazorpayPaymentEntity(payload);
                yield Result.success(new PaymentAuthorized(
                    PaymentGateway.RAZORPAY,
                    eventType,
                    (String) paymentEntity.get("id"),
                    (String) paymentEntity.get("order_id"),
                    payload
                ));
            }
            case "refund.created", "refund.processed" -> {
                Map<String, Object> refundEntity = extractRazorpayRefundEntity(payload);
                yield Result.success(new RefundProcessed(
                    PaymentGateway.RAZORPAY,
                    eventType,
                    (String) refundEntity.get("id"),
                    (String) refundEntity.get("payment_id"),
                    payload
                ));
            }
            case "refund.failed" -> {
                Map<String, Object> refundEntity = extractRazorpayRefundEntity(payload);
                yield Result.success(new RefundFailed(
                    PaymentGateway.RAZORPAY,
                    eventType,
                    (String) refundEntity.get("id"),
                    (String) refundEntity.get("payment_id"),
                    (String) refundEntity.get("error_description"),
                    payload
                ));
            }
            default -> Result.success(new Unknown(PaymentGateway.RAZORPAY, eventType, payload));
        };
    }

    /**
     * Parse Stripe webhook event
     * Pattern matching for event type (NO if-else)
     */
    @SuppressWarnings("unchecked")
    private static Result<WebhookEvent, String> parseStripeEvent(
            String eventType,
            Map<String, Object> payload
    ) {
        return switch (eventType) {
            case "payment_intent.succeeded" -> {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                yield Result.success(new PaymentSucceeded(
                    PaymentGateway.STRIPE,
                    eventType,
                    (String) object.get("id"),
                    null,
                    payload
                ));
            }
            case "payment_intent.payment_failed" -> {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                Map<String, Object> lastError = (Map<String, Object>) object.get("last_payment_error");
                yield Result.success(new PaymentFailed(
                    PaymentGateway.STRIPE,
                    eventType,
                    (String) object.get("id"),
                    null,
                    lastError != null ? (String) lastError.get("code") : null,
                    lastError != null ? (String) lastError.get("message") : null,
                    payload
                ));
            }
            case "payment_intent.amount_capturable_updated" -> {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                yield Result.success(new PaymentAuthorized(
                    PaymentGateway.STRIPE,
                    eventType,
                    (String) object.get("id"),
                    null,
                    payload
                ));
            }
            case "charge.captured" -> {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                yield Result.success(new PaymentCaptured(
                    PaymentGateway.STRIPE,
                    eventType,
                    (String) object.get("payment_intent"),
                    null,
                    payload
                ));
            }
            case "charge.refunded" -> {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                yield Result.success(new RefundProcessed(
                    PaymentGateway.STRIPE,
                    eventType,
                    (String) object.get("id"),
                    (String) object.get("payment_intent"),
                    payload
                ));
            }
            case "charge.refund.failed" -> {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                yield Result.success(new RefundFailed(
                    PaymentGateway.STRIPE,
                    eventType,
                    (String) object.get("id"),
                    (String) object.get("payment_intent"),
                    (String) object.get("failure_message"),
                    payload
                ));
            }
            default -> Result.success(new Unknown(PaymentGateway.STRIPE, eventType, payload));
        };
    }

    /**
     * Extract Razorpay payment entity from payload
     * Functional data extraction (NO if-else)
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractRazorpayPaymentEntity(Map<String, Object> payload) {
        return java.util.Optional.ofNullable(payload.get("payload"))
            .map(p -> (Map<String, Object>) p)
            .flatMap(p -> java.util.Optional.ofNullable(p.get("payment")))
            .map(p -> (Map<String, Object>) p)
            .flatMap(p -> java.util.Optional.ofNullable(p.get("entity")))
            .map(e -> (Map<String, Object>) e)
            .orElseGet(Map::of);
    }

    /**
     * Extract Razorpay refund entity from payload
     * Functional data extraction (NO if-else)
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractRazorpayRefundEntity(Map<String, Object> payload) {
        return java.util.Optional.ofNullable(payload.get("payload"))
            .map(p -> (Map<String, Object>) p)
            .flatMap(p -> java.util.Optional.ofNullable(p.get("refund")))
            .map(r -> (Map<String, Object>) r)
            .flatMap(r -> java.util.Optional.ofNullable(r.get("entity")))
            .map(e -> (Map<String, Object>) e)
            .orElseGet(Map::of);
    }
}
