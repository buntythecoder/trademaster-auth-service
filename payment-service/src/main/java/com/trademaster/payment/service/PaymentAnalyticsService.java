package com.trademaster.payment.service;

import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.entity.UserSubscription;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.enums.SubscriptionStatus;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import com.trademaster.payment.repository.UserSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ⚡ PAYMENT ANALYTICS SERVICE: Advanced Payment Analytics
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - ONLY payment analytics
 * - Rule #3: Functional programming (no if-else, use Stream API)
 * - Rule #13: Stream API for all aggregations
 * - Rule #12: Virtual Threads for async operations
 *
 * RESPONSIBILITIES:
 * - Track payment success rates by gateway
 * - Track payment method preferences
 * - Calculate revenue metrics (MRR, ARR, Churn rate)
 * - Track failed payment reasons
 * - Provide analytics aggregations
 *
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAnalyticsService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    // ⚡ VIRTUAL THREADS: Dedicated executor
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * ✅ FUNCTIONAL: Calculate payment success rate by gateway
     * Cognitive Complexity: 4
     */
    public CompletableFuture<GatewaySuccessRateAnalytics> calculateGatewaySuccessRate(
            final PaymentGateway gateway,
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<PaymentTransaction> transactions = fetchTransactionsByGatewayAndTime(
                gateway, timeRange);

            final long totalAttempts = transactions.size();
            final long successful = countByStatus(transactions, PaymentStatus.COMPLETED);
            final long failed = countByStatus(transactions, PaymentStatus.FAILED);

            final double successRate = calculateRate(successful, totalAttempts);

            log.debug("Gateway success rate for {}: {}%", gateway, successRate);

            return new GatewaySuccessRateAnalytics(
                gateway,
                totalAttempts,
                successful,
                failed,
                successRate,
                timeRange
            );
        }, virtualThreadExecutor);
    }

    /**
     * ✅ FUNCTIONAL: Calculate payment method preferences
     * Cognitive Complexity: 3
     */
    public CompletableFuture<List<PaymentMethodPreference>> calculatePaymentMethodPreferences(
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<PaymentTransaction> transactions = fetchTransactionsByTime(timeRange);

            return groupByPaymentMethod(transactions)
                .entrySet()
                .stream()
                .map(entry -> calculateMethodMetrics(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.totalTransactions(), a.totalTransactions()))
                .collect(Collectors.toList());
        }, virtualThreadExecutor);
    }

    /**
     * ✅ FUNCTIONAL: Calculate Monthly Recurring Revenue (MRR)
     * Cognitive Complexity: 4
     */
    public CompletableFuture<RevenueMetrics> calculateMRR(final YearMonth month) {

        return CompletableFuture.supplyAsync(() -> {
            final Instant monthStart = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            final Instant monthEnd = month.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

            final List<UserSubscription> activeSubscriptions =
                userSubscriptionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                    .filter(s -> isInTimeRange(s.getUpdatedAt(), monthStart, monthEnd))
                    .collect(Collectors.toList());

            final BigDecimal monthlyRevenue = activeSubscriptions.stream()
                .map(this::calculateMonthlySubscriptionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            final long subscriberCount = activeSubscriptions.size();

            log.debug("MRR for {}: {} from {} subscribers", month, monthlyRevenue, subscriberCount);

            return new RevenueMetrics(
                monthlyRevenue,
                calculateARR(monthlyRevenue),
                subscriberCount,
                month
            );
        }, virtualThreadExecutor);
    }

    /**
     * ✅ FUNCTIONAL: Calculate Annual Recurring Revenue (ARR)
     * Cognitive Complexity: 1
     */
    private BigDecimal calculateARR(final BigDecimal mrr) {
        return mrr.multiply(BigDecimal.valueOf(12))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ✅ FUNCTIONAL: Calculate churn rate for a period
     * Cognitive Complexity: 5
     */
    public CompletableFuture<ChurnAnalytics> calculateChurnRate(final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final Instant periodStart = timeRange.startTime();
            final Instant periodEnd = timeRange.endTime();

            final long startingSubscribers = countActiveSubscribersAt(periodStart);
            final long endingSubscribers = countActiveSubscribersAt(periodEnd);
            final long churnedSubscribers = countChurnedSubscribers(periodStart, periodEnd);

            final double churnRate = calculateChurnRate(churnedSubscribers, startingSubscribers);

            log.debug("Churn rate for period: {}% ({} churned out of {} starting)",
                churnRate, churnedSubscribers, startingSubscribers);

            return new ChurnAnalytics(
                startingSubscribers,
                endingSubscribers,
                churnedSubscribers,
                churnRate,
                timeRange
            );
        }, virtualThreadExecutor);
    }

    /**
     * ✅ FUNCTIONAL: Track failed payment reasons
     * Cognitive Complexity: 3
     */
    public CompletableFuture<List<FailureReasonAnalytics>> analyzeFailureReasons(
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<PaymentTransaction> failedTransactions =
                fetchFailedTransactionsByTime(timeRange);

            return groupByFailureReason(failedTransactions)
                .entrySet()
                .stream()
                .map(entry -> createFailureReasonMetrics(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.occurrences(), a.occurrences()))
                .collect(Collectors.toList());
        }, virtualThreadExecutor);
    }

    /**
     * ✅ FUNCTIONAL: Calculate comprehensive revenue analytics
     * Cognitive Complexity: 5
     */
    public CompletableFuture<ComprehensiveRevenueAnalytics> calculateComprehensiveRevenue(
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<PaymentTransaction> completedTransactions =
                fetchCompletedTransactionsByTime(timeRange);

            final BigDecimal totalRevenue = completedTransactions.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            final BigDecimal averageTransactionValue = calculateAverage(
                totalRevenue, completedTransactions.size());

            final Map<PaymentGateway, BigDecimal> revenueByGateway =
                calculateRevenueByGateway(completedTransactions);

            log.debug("Total revenue for period: {} from {} transactions",
                totalRevenue, completedTransactions.size());

            return new ComprehensiveRevenueAnalytics(
                totalRevenue,
                averageTransactionValue,
                completedTransactions.size(),
                revenueByGateway,
                timeRange
            );
        }, virtualThreadExecutor);
    }

    // ==================== Private Helper Methods ====================

    /**
     * ✅ FUNCTIONAL: Fetch transactions by gateway and time
     * Cognitive Complexity: 1
     */
    private List<PaymentTransaction> fetchTransactionsByGatewayAndTime(
            final PaymentGateway gateway,
            final TimeRange timeRange) {

        return paymentTransactionRepository.findAll().stream()
            .filter(t -> t.getPaymentGateway() == gateway)
            .filter(t -> isInTimeRange(t, timeRange))
            .collect(Collectors.toList());
    }

    /**
     * ✅ FUNCTIONAL: Fetch all transactions by time
     * Cognitive Complexity: 1
     */
    private List<PaymentTransaction> fetchTransactionsByTime(final TimeRange timeRange) {
        return paymentTransactionRepository.findAll().stream()
            .filter(t -> isInTimeRange(t, timeRange))
            .collect(Collectors.toList());
    }

    /**
     * ✅ FUNCTIONAL: Fetch failed transactions by time
     * Cognitive Complexity: 1
     */
    private List<PaymentTransaction> fetchFailedTransactionsByTime(final TimeRange timeRange) {
        return paymentTransactionRepository.findAll().stream()
            .filter(t -> t.getStatus() == PaymentStatus.FAILED)
            .filter(t -> isInTimeRange(t, timeRange))
            .collect(Collectors.toList());
    }

    /**
     * ✅ FUNCTIONAL: Fetch completed transactions by time
     * Cognitive Complexity: 1
     */
    private List<PaymentTransaction> fetchCompletedTransactionsByTime(final TimeRange timeRange) {
        return paymentTransactionRepository.findAll().stream()
            .filter(t -> t.getStatus() == PaymentStatus.COMPLETED)
            .filter(t -> isInTimeRange(t, timeRange))
            .collect(Collectors.toList());
    }

    /**
     * ✅ FUNCTIONAL: Check if transaction is in time range
     * Cognitive Complexity: 1
     */
    private boolean isInTimeRange(final PaymentTransaction transaction, final TimeRange timeRange) {
        final Instant createdAt = transaction.getCreatedAt();
        return !createdAt.isBefore(timeRange.startTime()) &&
               !createdAt.isAfter(timeRange.endTime());
    }

    /**
     * ✅ FUNCTIONAL: Check if timestamp is in time range
     * Cognitive Complexity: 1
     */
    private boolean isInTimeRange(final Instant timestamp, final Instant startTime, final Instant endTime) {
        return !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime);
    }

    /**
     * ✅ FUNCTIONAL: Count transactions by status using Stream API
     * Cognitive Complexity: 1
     */
    private long countByStatus(
            final List<PaymentTransaction> transactions,
            final PaymentStatus status) {

        return transactions.stream()
            .filter(t -> t.getStatus() == status)
            .count();
    }

    /**
     * ✅ FUNCTIONAL: Calculate percentage rate
     * Cognitive Complexity: 1
     */
    private double calculateRate(final long numerator, final long denominator) {
        return denominator == 0 ? 0.0 : (numerator * 100.0) / denominator;
    }

    /**
     * ✅ FUNCTIONAL: Group transactions by payment method using Stream API
     * Cognitive Complexity: 1
     */
    private Map<PaymentMethod, List<PaymentTransaction>> groupByPaymentMethod(
            final List<PaymentTransaction> transactions) {

        return transactions.stream()
            .collect(Collectors.groupingBy(PaymentTransaction::getPaymentMethod));
    }

    /**
     * ✅ FUNCTIONAL: Group transactions by failure reason using Stream API
     * Cognitive Complexity: 1
     */
    private Map<String, List<PaymentTransaction>> groupByFailureReason(
            final List<PaymentTransaction> transactions) {

        return transactions.stream()
            .filter(t -> t.getFailureReason() != null)
            .collect(Collectors.groupingBy(PaymentTransaction::getFailureReason));
    }

    /**
     * ✅ FUNCTIONAL: Calculate payment method metrics
     * Cognitive Complexity: 2
     */
    private PaymentMethodPreference calculateMethodMetrics(
            final PaymentMethod method,
            final List<PaymentTransaction> transactions) {

        final long totalTransactions = transactions.size();
        final long successful = transactions.stream()
            .filter(PaymentTransaction::isCompleted)
            .count();

        final double successRate = calculateRate(successful, totalTransactions);

        return new PaymentMethodPreference(
            method,
            totalTransactions,
            successful,
            successRate
        );
    }

    /**
     * ✅ FUNCTIONAL: Create failure reason metrics
     * Cognitive Complexity: 1
     */
    private FailureReasonAnalytics createFailureReasonMetrics(
            final String reason,
            final List<PaymentTransaction> transactions) {

        return new FailureReasonAnalytics(
            reason,
            transactions.size(),
            calculateTotalFailedAmount(transactions)
        );
    }

    /**
     * ✅ FUNCTIONAL: Calculate total failed amount
     * Cognitive Complexity: 1
     */
    private BigDecimal calculateTotalFailedAmount(final List<PaymentTransaction> transactions) {
        return transactions.stream()
            .map(PaymentTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ✅ FUNCTIONAL: Calculate monthly subscription amount
     * Cognitive Complexity: 1
     */
    private BigDecimal calculateMonthlySubscriptionAmount(final UserSubscription subscription) {
        return subscription.getAmount();
    }

    /**
     * ✅ FUNCTIONAL: Count active subscribers at a point in time
     * Cognitive Complexity: 1
     */
    private long countActiveSubscribersAt(final Instant timestamp) {
        return userSubscriptionRepository.findAll().stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(s -> !s.getCurrentPeriodStart().isAfter(timestamp))
            .filter(s -> s.getCurrentPeriodEnd() == null || !s.getCurrentPeriodEnd().isBefore(timestamp))
            .count();
    }

    /**
     * ✅ FUNCTIONAL: Count churned subscribers in period
     * Cognitive Complexity: 1
     */
    private long countChurnedSubscribers(final Instant periodStart, final Instant periodEnd) {
        return userSubscriptionRepository.findAll().stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.CANCELLED ||
                        s.getStatus() == SubscriptionStatus.EXPIRED)
            .filter(s -> s.getUpdatedAt() != null)
            .filter(s -> !s.getUpdatedAt().isBefore(periodStart) &&
                        !s.getUpdatedAt().isAfter(periodEnd))
            .count();
    }

    /**
     * ✅ FUNCTIONAL: Calculate churn rate
     * Cognitive Complexity: 1
     */
    private double calculateChurnRate(final long churned, final long starting) {
        return calculateRate(churned, starting);
    }

    /**
     * ✅ FUNCTIONAL: Calculate average value
     * Cognitive Complexity: 1
     */
    private BigDecimal calculateAverage(final BigDecimal total, final long count) {
        return count == 0 ? BigDecimal.ZERO :
            total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    /**
     * ✅ FUNCTIONAL: Calculate revenue by gateway
     * Cognitive Complexity: 1
     */
    private Map<PaymentGateway, BigDecimal> calculateRevenueByGateway(
            final List<PaymentTransaction> transactions) {

        return transactions.stream()
            .collect(Collectors.groupingBy(
                PaymentTransaction::getPaymentGateway,
                Collectors.mapping(
                    PaymentTransaction::getAmount,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
    }

    // ==================== Supporting Types ====================

    /**
     * ✅ IMMUTABLE: Time range record
     */
    public record TimeRange(
        Instant startTime,
        Instant endTime
    ) {
        public static TimeRange ofDays(final int days) {
            final Instant now = Instant.now();
            final Instant start = now.minusSeconds(days * 86400L);
            return new TimeRange(start, now);
        }
    }

    /**
     * ✅ IMMUTABLE: Gateway success rate analytics record
     */
    public record GatewaySuccessRateAnalytics(
        PaymentGateway gateway,
        long totalAttempts,
        long successful,
        long failed,
        double successRate,
        TimeRange timeRange
    ) {}

    /**
     * ✅ IMMUTABLE: Payment method preference record
     */
    public record PaymentMethodPreference(
        PaymentMethod method,
        long totalTransactions,
        long successfulTransactions,
        double successRate
    ) {}

    /**
     * ✅ IMMUTABLE: Revenue metrics record
     */
    public record RevenueMetrics(
        BigDecimal monthlyRecurringRevenue,
        BigDecimal annualRecurringRevenue,
        long subscriberCount,
        YearMonth month
    ) {}

    /**
     * ✅ IMMUTABLE: Churn analytics record
     */
    public record ChurnAnalytics(
        long startingSubscribers,
        long endingSubscribers,
        long churnedSubscribers,
        double churnRate,
        TimeRange timeRange
    ) {}

    /**
     * ✅ IMMUTABLE: Failure reason analytics record
     */
    public record FailureReasonAnalytics(
        String reason,
        long occurrences,
        BigDecimal totalFailedAmount
    ) {}

    /**
     * ✅ IMMUTABLE: Comprehensive revenue analytics record
     */
    public record ComprehensiveRevenueAnalytics(
        BigDecimal totalRevenue,
        BigDecimal averageTransactionValue,
        long totalTransactions,
        Map<PaymentGateway, BigDecimal> revenueByGateway,
        TimeRange timeRange
    ) {}
}
