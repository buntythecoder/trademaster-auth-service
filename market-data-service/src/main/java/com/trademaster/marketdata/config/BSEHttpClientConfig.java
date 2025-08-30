package com.trademaster.marketdata.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for BSE HTTP Client using OkHttp
 */
@Configuration
@ConditionalOnProperty(name = "trademaster.exchanges.bse.enabled", havingValue = "true")
class BSEHttpClientConfig {

    @Value("${trademaster.exchanges.bse.api-key:}")
    private String apiKey;

    @Bean
    public OkHttpClient bseHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        // Add interceptor for API key if configured
        if (!apiKey.isEmpty()) {
            builder.addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                okhttp3.Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + apiKey)
                        .build();
                return chain.proceed(request);
            });
        }

        return builder.build();
    }
}
