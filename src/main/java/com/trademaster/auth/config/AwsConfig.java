package com.trademaster.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

/**
 * AWS Configuration for KMS and other AWS services
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
@Profile("!test") // Exclude this configuration in test profile
public class AwsConfig {

    @Value("${trademaster.aws.region:us-east-1}")
    private String awsRegion;

    /**
     * AWS KMS Client configuration
     */
    @Bean
    public KmsClient kmsClient() {
        return com.trademaster.auth.pattern.SafeOperations.safelyToResult(() -> {
            KmsClient client = KmsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

            log.info("AWS KMS client configured for region: {}", awsRegion);
            return client;
        }).orElseThrow(error -> {
            log.error("Failed to configure AWS KMS client: {}", error);
            return new RuntimeException("AWS KMS configuration failed" + error);
        });
    }
}