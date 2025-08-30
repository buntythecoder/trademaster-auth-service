package com.trademaster.marketdata.config;

import com.trademaster.marketdata.provider.MarketDataProvider;
import lombok.Data;
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
@Data
@Component
@ConfigurationProperties(prefix = "trademaster.providers.alphavantage")
public class AlphaVantageProviderConfig implements MarketDataProvider.ProviderConfig {
    
    private String apiKey;
    private String apiSecret;
    private String baseUrl = "https://www.alphavantage.co/query";
    private int timeoutMs = 30000;
    private int retryAttempts = 3;
    private boolean enabled = true;
}