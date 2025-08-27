package com.trademaster.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Payment Service Application
 * 
 * Handles all payment processing, subscription management, and financial transactions
 * for the TradeMaster platform.
 * 
 * Features:
 * - Multi-gateway payment processing (Razorpay, Stripe)
 * - Subscription lifecycle management
 * - Secure payment method storage with tokenization
 * - Webhook processing for real-time payment updates
 * - PCI DSS compliant payment handling
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2025-01-25
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableTransactionManagement
public class PaymentServiceApplication {

    public static void main(String[] args) {
        // Enable virtual threads for better performance
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}