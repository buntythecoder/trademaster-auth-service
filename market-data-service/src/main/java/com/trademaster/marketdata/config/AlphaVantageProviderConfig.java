package com.trademaster.marketdata.config;

import com.trademaster.marketdata.provider.MarketDataProvider;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Alpha Vantage Provider Configuration
 *
 * Configuration implementation for Alpha Vantage market data provider.
 * Validated at runtime per Rule #26 (Configuration Synchronization).
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "trademaster.providers.alphavantage")
public class AlphaVantageProviderConfig implements MarketDataProvider.ProviderConfig {

    private String apiKey;
    private String apiSecret;

    @NotBlank
    private String baseUrl = "https://www.alphavantage.co/query";

    @Min(1000)
    private int timeoutMs = 30000;

    @Min(1)
    private int retryAttempts = 3;

    private boolean enabled = true;
}