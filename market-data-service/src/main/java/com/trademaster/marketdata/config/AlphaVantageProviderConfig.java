package com.trademaster.marketdata.config;

import com.trademaster.marketdata.provider.MarketDataProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Alpha Vantage Provider Configuration
 * 
 * Configuration implementation for Alpha Vantage market data provider.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "trademaster.providers.alphavantage")
public class AlphaVantageProviderConfig implements MarketDataProvider.ProviderConfig {
    
    private String apiKey;
    private String apiSecret;
    private String baseUrl = "https://www.alphavantage.co/query";
    private int timeoutMs = 30000;
    private int retryAttempts = 3;
    private boolean enabled = true;
    
    @Override
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    @Override
    public String getApiSecret() {
        return apiSecret;
    }
    
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
    
    @Override
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    @Override
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    @Override
    public int getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}