package com.trademaster.portfolio.domain;

import java.math.BigDecimal;

/**
 * Broker Contribution Data
 * 
 * Represents performance contribution by broker.
 * Used in attribution analysis.
 */
public record BrokerContribution(
    String brokerName,
    String brokerId,
    BigDecimal contribution, // Contribution to total return
    BigDecimal brokerReturn, // Return from this broker
    BigDecimal executionCost, // Total execution costs
    BigDecimal commission, // Total commission paid
    int numberOfTrades,
    BigDecimal averageExecutionQuality // Execution quality score
) {}