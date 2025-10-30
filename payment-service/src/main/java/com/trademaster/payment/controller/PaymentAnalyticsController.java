package com.trademaster.payment.controller;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.service.PaymentAnalyticsService;
import com.trademaster.payment.service.PaymentAnalyticsService.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ⚡ PAYMENT ANALYTICS CONTROLLER: Payment Analytics API Endpoints
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - ONLY analytics endpoint handling
 * - Rule #6: Zero Trust Security - PreAuthorize for access control
 * - Rule #15: Structured logging with correlation IDs
 *
 * RESPONSIBILITIES:
 * - Handle analytics API requests
 * - Validate query parameters
 * - Invoke analytics service
 * - Return structured responses
 *
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/payments/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Analytics", description = "Payment analytics and reporting endpoints")
public class PaymentAnalyticsController {

    private final PaymentAnalyticsService analyticsService;

    /**
     * ✅ FUNCTIONAL: Get gateway success rate
     * Cognitive Complexity: 2
     */
    @GetMapping("/gateway-success-rate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get gateway success rate",
               description = "Get payment success rate by gateway")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success rate retrieved successfully",
            content = @Content(schema = @Schema(implementation = GatewaySuccessRateAnalytics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<GatewaySuccessRateAnalytics>> getGatewaySuccessRate(
            @RequestParam(value = "gateway") final String gateway,
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate startDate,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate endDate) {

        log.info("Fetching gateway success rate: gateway={}, startDate={}, endDate={}",
            gateway, startDate, endDate);

        final PaymentGateway gatewayEnum = PaymentGateway.valueOf(gateway.toUpperCase());
        final TimeRange timeRange = createTimeRange(startDate, endDate);

        return analyticsService.calculateGatewaySuccessRate(gatewayEnum, timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * ✅ FUNCTIONAL: Get payment method preferences
     * Cognitive Complexity: 2
     */
    @GetMapping("/payment-methods")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get payment method preferences",
               description = "Get payment method usage statistics and preferences")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment methods retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentMethodPreference.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PaymentMethodPreference>>> getPaymentMethodPreferences(
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate startDate,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate endDate) {

        log.info("Fetching payment method preferences: startDate={}, endDate={}",
            startDate, endDate);

        final TimeRange timeRange = createTimeRange(startDate, endDate);

        return analyticsService.calculatePaymentMethodPreferences(timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * ✅ FUNCTIONAL: Get Monthly Recurring Revenue (MRR)
     * Cognitive Complexity: 2
     */
    @GetMapping("/revenue/mrr")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_USER')")
    @Operation(summary = "Get Monthly Recurring Revenue",
               description = "Calculate MRR and ARR for a specific month")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "MRR retrieved successfully",
            content = @Content(schema = @Schema(implementation = RevenueMetrics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<RevenueMetrics>> getMRR(
            @RequestParam(value = "year") final int year,
            @RequestParam(value = "month") final int month) {

        log.info("Fetching MRR: year={}, month={}", year, month);

        final YearMonth yearMonth = YearMonth.of(year, month);

        return analyticsService.calculateMRR(yearMonth)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * ✅ FUNCTIONAL: Get churn rate
     * Cognitive Complexity: 2
     */
    @GetMapping("/churn-rate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get churn rate",
               description = "Calculate subscriber churn rate for a period")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Churn rate retrieved successfully",
            content = @Content(schema = @Schema(implementation = ChurnAnalytics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<ChurnAnalytics>> getChurnRate(
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate startDate,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate endDate) {

        log.info("Fetching churn rate: startDate={}, endDate={}", startDate, endDate);

        final TimeRange timeRange = createTimeRange(startDate, endDate);

        return analyticsService.calculateChurnRate(timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * ✅ FUNCTIONAL: Get failure reasons analysis
     * Cognitive Complexity: 2
     */
    @GetMapping("/failures")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get failure reasons",
               description = "Analyze failed payment reasons and patterns")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Failure analysis retrieved successfully",
            content = @Content(schema = @Schema(implementation = FailureReasonAnalytics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<FailureReasonAnalytics>>> getFailureReasons(
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate startDate,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate endDate) {

        log.info("Fetching failure reasons: startDate={}, endDate={}", startDate, endDate);

        final TimeRange timeRange = createTimeRange(startDate, endDate);

        return analyticsService.analyzeFailureReasons(timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * ✅ FUNCTIONAL: Get comprehensive revenue analytics
     * Cognitive Complexity: 2
     */
    @GetMapping("/revenue/comprehensive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_USER')")
    @Operation(summary = "Get comprehensive revenue analytics",
               description = "Get detailed revenue metrics including breakdown by gateway")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Revenue analytics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ComprehensiveRevenueAnalytics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<ComprehensiveRevenueAnalytics>> getComprehensiveRevenue(
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate startDate,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDate endDate) {

        log.info("Fetching comprehensive revenue: startDate={}, endDate={}", startDate, endDate);

        final TimeRange timeRange = createTimeRange(startDate, endDate);

        return analyticsService.calculateComprehensiveRevenue(timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     * ✅ FUNCTIONAL: Get quick analytics summary (last 30 days)
     * Cognitive Complexity: 1
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get analytics summary",
               description = "Get quick analytics summary for last 30 days")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<AnalyticsSummary>> getAnalyticsSummary() {

        log.info("Fetching analytics summary for last 30 days");

        final TimeRange timeRange = TimeRange.ofDays(30);

        return CompletableFuture.allOf(
            analyticsService.calculatePaymentMethodPreferences(timeRange),
            analyticsService.calculateComprehensiveRevenue(timeRange),
            analyticsService.analyzeFailureReasons(timeRange)
        ).thenCompose(v ->
            CompletableFuture.supplyAsync(() -> {
                final List<PaymentMethodPreference> methods =
                    analyticsService.calculatePaymentMethodPreferences(timeRange).join();
                final ComprehensiveRevenueAnalytics revenue =
                    analyticsService.calculateComprehensiveRevenue(timeRange).join();
                final List<FailureReasonAnalytics> failures =
                    analyticsService.analyzeFailureReasons(timeRange).join();

                return new AnalyticsSummary(methods, revenue, failures, timeRange);
            })
        ).thenApply(ResponseEntity::ok);
    }

    // ==================== Helper Methods ====================

    /**
     * ✅ FUNCTIONAL: Create time range from dates
     * Cognitive Complexity: 1
     */
    private TimeRange createTimeRange(final LocalDate startDate, final LocalDate endDate) {
        final Instant startTime = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant endTime = endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return new TimeRange(startTime, endTime);
    }

    // ==================== Supporting Types ====================

    /**
     * ✅ IMMUTABLE: Analytics summary record
     */
    public record AnalyticsSummary(
        List<PaymentMethodPreference> paymentMethods,
        ComprehensiveRevenueAnalytics revenue,
        List<FailureReasonAnalytics> topFailures,
        TimeRange period
    ) {}
}
