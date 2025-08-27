package com.trademaster.agentos.domain.entity;

/**
 * Agent Type Enumeration
 * 
 * Defines the different types of AI agents available in the TradeMaster Agent OS.
 * Each agent type has specialized capabilities and is optimized for specific trading tasks.
 */
public enum AgentType {
    /**
     * Market Analysis Agent
     * Specializes in technical analysis, fundamental analysis, and market sentiment analysis.
     * Capabilities: Chart analysis, indicator calculations, market screening, price predictions
     */
    MARKET_ANALYSIS("Market Analysis Agent", "Analyzes market data, trends, and generates trading signals"),
    
    /**
     * Portfolio Management Agent  
     * Specializes in portfolio optimization, asset allocation, and rebalancing strategies.
     * Capabilities: Portfolio optimization, risk assessment, rebalancing, performance analysis
     */
    PORTFOLIO_MANAGEMENT("Portfolio Management Agent", "Optimizes portfolio allocation and manages risk"),
    
    /**
     * Trading Execution Agent
     * Specializes in order execution, broker routing, and execution optimization.
     * Capabilities: Smart order routing, TWAP/VWAP execution, slippage optimization
     */
    TRADING_EXECUTION("Trading Execution Agent", "Executes trades with optimal timing and routing"),
    
    /**
     * Risk Management Agent
     * Specializes in risk monitoring, compliance checking, and risk mitigation.
     * Capabilities: Real-time risk monitoring, VaR calculations, compliance validation
     */
    RISK_MANAGEMENT("Risk Management Agent", "Monitors and manages trading risks"),
    
    /**
     * Notification Agent
     * Specializes in user communications, alerts, and reporting.
     * Capabilities: Email/SMS alerts, push notifications, report generation
     */
    NOTIFICATION("Notification Agent", "Handles user notifications and alerts"),
    
    /**
     * Custom Agent
     * User-defined agent with custom capabilities and workflows.
     * Capabilities: Configurable based on user requirements
     */
    CUSTOM("Custom Agent", "User-defined agent with custom capabilities");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the default maximum concurrent tasks for this agent type
     */
    public int getDefaultMaxConcurrentTasks() {
        return switch (this) {
            case MARKET_ANALYSIS -> 10;        // High throughput for market analysis
            case PORTFOLIO_MANAGEMENT -> 5;    // Moderate load for complex calculations
            case TRADING_EXECUTION -> 8;       // High priority, moderate concurrency
            case RISK_MANAGEMENT -> 15;        // High throughput for monitoring
            case NOTIFICATION -> 20;           // High throughput for notifications
            case CUSTOM -> 5;                  // Conservative default for custom agents
        };
    }

    /**
     * Get the default capabilities for this agent type
     */
    public AgentCapability[] getDefaultCapabilities() {
        return switch (this) {
            case MARKET_ANALYSIS -> new AgentCapability[]{
                AgentCapability.TECHNICAL_ANALYSIS,
                AgentCapability.FUNDAMENTAL_ANALYSIS,
                AgentCapability.MARKET_SCREENING,
                AgentCapability.PRICE_PREDICTION
            };
            case PORTFOLIO_MANAGEMENT -> new AgentCapability[]{
                AgentCapability.PORTFOLIO_OPTIMIZATION,
                AgentCapability.RISK_ASSESSMENT,
                AgentCapability.ASSET_ALLOCATION,
                AgentCapability.PERFORMANCE_ANALYSIS
            };
            case TRADING_EXECUTION -> new AgentCapability[]{
                AgentCapability.ORDER_EXECUTION,
                AgentCapability.BROKER_ROUTING,
                AgentCapability.EXECUTION_OPTIMIZATION,
                AgentCapability.SLIPPAGE_MONITORING
            };
            case RISK_MANAGEMENT -> new AgentCapability[]{
                AgentCapability.RISK_MONITORING,
                AgentCapability.COMPLIANCE_CHECK,
                AgentCapability.VAR_CALCULATION,
                AgentCapability.STRESS_TESTING
            };
            case NOTIFICATION -> new AgentCapability[]{
                AgentCapability.EMAIL_ALERTS,
                AgentCapability.SMS_ALERTS,
                AgentCapability.PUSH_NOTIFICATIONS,
                AgentCapability.REPORT_GENERATION
            };
            case CUSTOM -> new AgentCapability[]{
                AgentCapability.CUSTOM_LOGIC
            };
        };
    }
}