package com.trademaster.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Payment Gateway Configuration
 * 
 * Configures payment gateway clients and API connections.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class PaymentConfig {

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;
    
    @Value("${payment.razorpay.key-secret}")
    private String razorpayKeySecret;
    
    @Value("${payment.stripe.secret-key}")
    private String stripeSecretKey;
    
    /**
     * Configure Razorpay client
     */
    @Bean
    @Primary
    public RazorpayClient razorpayClient() throws RazorpayException {
        log.info("Initializing Razorpay client with key ID: {}", 
                razorpayKeyId.substring(0, Math.min(razorpayKeyId.length(), 8)) + "...");
        
        return new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }
    
    /**
     * Configure Stripe API
     * Initializes Stripe SDK with API key on application startup
     */
    @PostConstruct
    public void configureStripe() {
        log.info("Configuring Stripe API with secret key");
        Stripe.apiKey = stripeSecretKey;
    }
}