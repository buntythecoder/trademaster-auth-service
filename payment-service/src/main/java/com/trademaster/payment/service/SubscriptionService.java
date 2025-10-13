package com.trademaster.payment.service;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.entity.UserSubscription;

/**
 * Subscription Service - Interface
 * Manages recurring subscription billing and lifecycle
 *
 * Compliance:
 * - Rule 3: Functional interface with Result types
 * - Rule 11: Railway programming for error handling
 * - Rule 12: Virtual Threads for async billing operations
 *
 * Subscription Lifecycle:
 * - ACTIVE: Subscription is active and billing
 * - PAUSED: Temporarily suspended, no billing
 * - CANCELLED: Terminated, no further billing
 * - EXPIRED: Billing period ended, awaiting renewal
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface SubscriptionService {

    /**
     * Process billing for subscription
     * Creates payment transaction for current billing cycle
     *
     * @param subscription Subscription to bill
     * @return Result with payment transaction or error
     */
    Result<PaymentTransaction, String> processBilling(UserSubscription subscription);

    /**
     * Create new subscription
     * Validates plan, payment method, and processes first payment
     *
     * @param subscription Subscription details
     * @return Result with created subscription
     */
    Result<UserSubscription, String> createSubscription(UserSubscription subscription);

    /**
     * Cancel subscription
     * Can be immediate or at end of billing period
     *
     * @param subscriptionId Subscription ID
     * @param immediate true for immediate cancellation, false for end of period
     * @return Result indicating success or failure
     */
    Result<Boolean, String> cancelSubscription(Long subscriptionId, boolean immediate);

    /**
     * Pause subscription
     * Suspends billing temporarily
     *
     * @param subscriptionId Subscription ID
     * @return Result with paused subscription
     */
    Result<UserSubscription, String> pauseSubscription(Long subscriptionId);

    /**
     * Resume subscription
     * Reactivates paused subscription
     *
     * @param subscriptionId Subscription ID
     * @return Result with resumed subscription
     */
    Result<UserSubscription, String> resumeSubscription(Long subscriptionId);

    /**
     * Upgrade/downgrade subscription plan
     * Changes subscription plan with proration
     *
     * @param subscriptionId Subscription ID
     * @param newPlanId New plan ID
     * @return Result with updated subscription
     */
    Result<UserSubscription, String> changeSubscriptionPlan(Long subscriptionId, String newPlanId);
}
