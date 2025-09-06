package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Broker Information DTO
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Comprehensive metadata and capability information for supported brokers.
 * Provides static broker information, API capabilities, trading features,
 * and integration details for client applications.
 * 
 * Information Categories:
 * - Basic broker identification and branding
 * - API capabilities and versions
 * - Trading features and limitations
 * - Fee structure and pricing
 * - Supported markets and instruments
 * 
 * Integration Details:
 * - OAuth flow requirements
 * - Rate limiting and quotas
 * - Real-time data capabilities
 * - Order types and execution modes
 * - Risk management features
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Metadata & Capabilities)
 */
@Builder
public record BrokerInfo(
    BrokerType brokerType,
    String name,
    String displayName,
    String description,
    String logoUrl,
    String websiteUrl,
    String apiVersion,
    Boolean isActive,
    Boolean supportsRealTimeData,
    Boolean supportsWebSocket,
    Boolean supportsOptionsTrading,
    Boolean supportsCommodityTrading,
    Boolean supportsCurrencyTrading,
    Boolean supportsMutualFunds,
    List<String> supportedExchanges,
    List<String> supportedOrderTypes,
    Set<String> supportedInstruments,
    BigDecimal brokeragePerTrade,
    BigDecimal accountMaintenanceFee,
    Integer rateLimit,
    Long sessionTimeoutMinutes,
    String oauthRedirectPattern,
    Boolean requiresOTP,
    String documentationUrl,
    String supportEmail,
    String supportPhone
) {
    
    /**
     * Check if broker supports equity trading
     * 
     * @return true if equity trading is supported
     */
    public boolean supportsEquityTrading() {
        return supportedInstruments != null &&
               (supportedInstruments.contains("EQUITY") ||
                supportedInstruments.contains("EQ"));
    }
    
    /**
     * Check if broker supports derivative trading
     * 
     * @return true if derivatives are supported
     */
    public boolean supportsDerivativeTrading() {
        return Boolean.TRUE.equals(supportsOptionsTrading) ||
               (supportedInstruments != null && 
                (supportedInstruments.contains("FUTURES") ||
                 supportedInstruments.contains("OPTIONS")));
    }
    
    /**
     * Get primary exchange for this broker
     * 
     * @return primary exchange name
     */
    public String getPrimaryExchange() {
        if (supportedExchanges == null || supportedExchanges.isEmpty()) {
            return "NSE"; // Default to NSE for Indian brokers
        }
        return supportedExchanges.get(0);
    }
    
    /**
     * Check if broker has premium features
     * 
     * @return true if broker offers premium features
     */
    public boolean hasPremiumFeatures() {
        return Boolean.TRUE.equals(supportsRealTimeData) &&
               Boolean.TRUE.equals(supportsWebSocket) &&
               supportedOrderTypes != null && supportedOrderTypes.size() > 5;
    }
    
    /**
     * Get feature completeness score (0-100)
     * 
     * @return feature score based on supported capabilities
     */
    public int getFeatureScore() {
        int score = 0;
        
        if (Boolean.TRUE.equals(supportsRealTimeData)) score += 20;
        if (Boolean.TRUE.equals(supportsWebSocket)) score += 15;
        if (Boolean.TRUE.equals(supportsOptionsTrading)) score += 15;
        if (Boolean.TRUE.equals(supportsCommodityTrading)) score += 10;
        if (Boolean.TRUE.equals(supportsCurrencyTrading)) score += 10;
        if (Boolean.TRUE.equals(supportsMutualFunds)) score += 10;
        
        if (supportedOrderTypes != null && supportedOrderTypes.size() > 5) score += 10;
        if (supportedExchanges != null && supportedExchanges.size() > 2) score += 5;
        if (rateLimit != null && rateLimit > 3) score += 5;
        
        return Math.min(score, 100);
    }
    
    /**
     * Get cost structure tier
     * 
     * @return cost tier (LOW, MEDIUM, HIGH)
     */
    public String getCostTier() {
        if (brokeragePerTrade == null) {
            return "UNKNOWN";
        }
        
        BigDecimal lowThreshold = new BigDecimal("20.00");
        BigDecimal highThreshold = new BigDecimal("50.00");
        
        if (brokeragePerTrade.compareTo(lowThreshold) <= 0) {
            return "LOW";
        } else if (brokeragePerTrade.compareTo(highThreshold) <= 0) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
    
    /**
     * Check if broker is suitable for beginners
     * 
     * @return true if broker is beginner-friendly
     */
    public boolean isBeginnerFriendly() {
        return "LOW".equals(getCostTier()) &&
               !Boolean.TRUE.equals(requiresOTP) &&
               supportEmail != null &&
               documentationUrl != null;
    }
    
    /**
     * Check if broker is suitable for active trading
     * 
     * @return true if broker supports active trading
     */
    public boolean supportsActiveTrading() {
        return Boolean.TRUE.equals(supportsRealTimeData) &&
               Boolean.TRUE.equals(supportsWebSocket) &&
               rateLimit != null && rateLimit >= 5 &&
               supportedOrderTypes != null && supportedOrderTypes.size() >= 6;
    }
    
    /**
     * Get recommended usage category
     * 
     * @return usage category (BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL)
     */
    public String getRecommendedUsage() {
        int featureScore = getFeatureScore();
        
        if (isBeginnerFriendly() && featureScore < 60) {
            return "BEGINNER";
        } else if (featureScore >= 60 && featureScore < 80) {
            return "INTERMEDIATE";
        } else if (supportsActiveTrading() && featureScore >= 80) {
            return "ADVANCED";
        } else if (hasPremiumFeatures() && featureScore >= 90) {
            return "PROFESSIONAL";
        }
        
        return "GENERAL";
    }
    
    /**
     * Get integration complexity level
     * 
     * @return integration complexity (SIMPLE, MODERATE, COMPLEX)
     */
    public String getIntegrationComplexity() {
        int complexityScore = 0;
        
        if (Boolean.TRUE.equals(requiresOTP)) complexityScore += 2;
        if (sessionTimeoutMinutes != null && sessionTimeoutMinutes < 60) complexityScore += 2;
        if (rateLimit != null && rateLimit < 3) complexityScore += 1;
        if (oauthRedirectPattern != null && oauthRedirectPattern.contains("*")) complexityScore += 1;
        
        return switch (complexityScore) {
            case 0, 1 -> "SIMPLE";
            case 2, 3 -> "MODERATE";
            default -> "COMPLEX";
        };
    }
    
    /**
     * Create display-optimized broker info
     * 
     * @return broker info optimized for UI display
     */
    public BrokerInfo forDisplay() {
        return BrokerInfo.builder()
            .brokerType(brokerType)
            .name(name)
            .displayName(displayName)
            .description(description)
            .logoUrl(logoUrl)
            .websiteUrl(websiteUrl)
            .isActive(isActive)
            .supportsRealTimeData(supportsRealTimeData)
            .supportsWebSocket(supportsWebSocket)
            .supportsOptionsTrading(supportsOptionsTrading)
            .supportsCommodityTrading(supportsCommodityTrading)
            .supportsCurrencyTrading(supportsCurrencyTrading)
            .supportsMutualFunds(supportsMutualFunds)
            .supportedExchanges(supportedExchanges)
            .supportedOrderTypes(supportedOrderTypes)
            .brokeragePerTrade(brokeragePerTrade)
            .documentationUrl(documentationUrl)
            .supportEmail(supportEmail)
            .supportPhone(supportPhone)
            .build();
    }
}