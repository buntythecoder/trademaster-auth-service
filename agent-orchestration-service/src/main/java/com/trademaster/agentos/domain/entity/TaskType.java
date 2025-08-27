package com.trademaster.agentos.domain.entity;

/**
 * Task Type Enumeration
 * 
 * Defines the different types of tasks that can be executed by AI agents
 * in the TradeMaster Agent OS. Each task type corresponds to specific
 * business operations and capabilities.
 */
public enum TaskType {
    // Market Analysis Tasks
    MARKET_ANALYSIS("Market Analysis", "Analyze market data, trends, and generate insights", AgentType.MARKET_ANALYSIS),
    TECHNICAL_ANALYSIS("Technical Analysis", "Perform technical analysis on securities", AgentType.MARKET_ANALYSIS),
    FUNDAMENTAL_ANALYSIS("Fundamental Analysis", "Analyze company fundamentals and valuation", AgentType.MARKET_ANALYSIS),
    SENTIMENT_ANALYSIS("Sentiment Analysis", "Analyze market sentiment from news and social media", AgentType.MARKET_ANALYSIS),
    MARKET_SCREENING("Market Screening", "Screen securities based on specific criteria", AgentType.MARKET_ANALYSIS),
    PRICE_PREDICTION("Price Prediction", "Predict future price movements", AgentType.MARKET_ANALYSIS),
    
    // Portfolio Management Tasks
    PORTFOLIO_ANALYSIS("Portfolio Analysis", "Analyze current portfolio composition and performance", AgentType.PORTFOLIO_MANAGEMENT),
    PORTFOLIO_OPTIMIZATION("Portfolio Optimization", "Optimize portfolio allocation for risk/return", AgentType.PORTFOLIO_MANAGEMENT),
    ASSET_ALLOCATION("Asset Allocation", "Determine optimal asset allocation strategy", AgentType.PORTFOLIO_MANAGEMENT),
    REBALANCING("Portfolio Rebalancing", "Rebalance portfolio to target allocation", AgentType.PORTFOLIO_MANAGEMENT),
    PERFORMANCE_ANALYSIS("Performance Analysis", "Analyze portfolio performance metrics", AgentType.PORTFOLIO_MANAGEMENT),
    DIVERSIFICATION_ANALYSIS("Diversification Analysis", "Analyze portfolio diversification", AgentType.PORTFOLIO_MANAGEMENT),
    
    // Trading Execution Tasks
    ORDER_EXECUTION("Order Execution", "Execute trading orders across brokers", AgentType.TRADING_EXECUTION),
    SMART_ROUTING("Smart Order Routing", "Route orders to optimal brokers", AgentType.TRADING_EXECUTION),
    EXECUTION_MONITORING("Execution Monitoring", "Monitor order execution progress", AgentType.TRADING_EXECUTION),
    SLIPPAGE_ANALYSIS("Slippage Analysis", "Analyze and minimize execution slippage", AgentType.TRADING_EXECUTION),
    LIQUIDITY_ANALYSIS("Liquidity Analysis", "Analyze market liquidity for execution", AgentType.TRADING_EXECUTION),
    ALGORITHMIC_EXECUTION("Algorithmic Execution", "Execute orders using algorithms (TWAP, VWAP)", AgentType.TRADING_EXECUTION),
    
    // Risk Management Tasks
    RISK_ASSESSMENT("Risk Assessment", "Assess portfolio and position risks", AgentType.RISK_MANAGEMENT),
    RISK_MONITORING("Risk Monitoring", "Monitor risks in real-time", AgentType.RISK_MANAGEMENT),
    VAR_CALCULATION("VaR Calculation", "Calculate Value at Risk metrics", AgentType.RISK_MANAGEMENT),
    STRESS_TESTING("Stress Testing", "Perform portfolio stress testing", AgentType.RISK_MANAGEMENT),
    COMPLIANCE_CHECK("Compliance Check", "Validate compliance with regulations", AgentType.RISK_MANAGEMENT),
    DRAWDOWN_MONITORING("Drawdown Monitoring", "Monitor portfolio drawdowns", AgentType.RISK_MANAGEMENT),
    
    // Notification Tasks
    ALERT_GENERATION("Alert Generation", "Generate and send alerts to users", AgentType.NOTIFICATION),
    REPORT_GENERATION("Report Generation", "Generate performance and analysis reports", AgentType.NOTIFICATION),
    EMAIL_NOTIFICATION("Email Notification", "Send email notifications", AgentType.NOTIFICATION),
    SMS_NOTIFICATION("SMS Notification", "Send SMS alerts", AgentType.NOTIFICATION),
    PUSH_NOTIFICATION("Push Notification", "Send mobile push notifications", AgentType.NOTIFICATION),
    
    // Workflow and Orchestration Tasks
    WORKFLOW_EXECUTION("Workflow Execution", "Execute multi-step workflows", null),
    TASK_COORDINATION("Task Coordination", "Coordinate tasks across agents", null),
    AGENT_COMMUNICATION("Agent Communication", "Facilitate inter-agent communication", null),
    DECISION_MAKING("Decision Making", "Make autonomous trading decisions", null),
    
    // Custom Tasks
    CUSTOM_TASK("Custom Task", "Execute custom user-defined logic", AgentType.CUSTOM),
    SCRIPT_EXECUTION("Script Execution", "Execute custom scripts", AgentType.CUSTOM),
    PLUGIN_EXECUTION("Plugin Execution", "Execute custom plugins", AgentType.CUSTOM);

    private final String displayName;
    private final String description;
    private final AgentType preferredAgentType;

    TaskType(String displayName, String description, AgentType preferredAgentType) {
        this.displayName = displayName;
        this.description = description;
        this.preferredAgentType = preferredAgentType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public AgentType getPreferredAgentType() {
        return preferredAgentType;
    }

    /**
     * Get the default timeout for this task type in seconds
     */
    public int getDefaultTimeoutSeconds() {
        return switch (this) {
            // Quick tasks (1-2 minutes)
            case ALERT_GENERATION, EMAIL_NOTIFICATION, SMS_NOTIFICATION, PUSH_NOTIFICATION -> 60;
            
            // Short tasks (5 minutes)
            case TECHNICAL_ANALYSIS, ORDER_EXECUTION, RISK_MONITORING, COMPLIANCE_CHECK -> 300;
            
            // Medium tasks (10 minutes)
            case MARKET_ANALYSIS, FUNDAMENTAL_ANALYSIS, SENTIMENT_ANALYSIS, 
                 PORTFOLIO_ANALYSIS, EXECUTION_MONITORING, RISK_ASSESSMENT -> 600;
            
            // Long tasks (30 minutes)
            case PORTFOLIO_OPTIMIZATION, ASSET_ALLOCATION, REBALANCING, 
                 VAR_CALCULATION, STRESS_TESTING, REPORT_GENERATION -> 1800;
            
            // Very long tasks (1 hour)
            case MARKET_SCREENING, PERFORMANCE_ANALYSIS, DIVERSIFICATION_ANALYSIS,
                 SLIPPAGE_ANALYSIS, LIQUIDITY_ANALYSIS -> 3600;
            
            // Extended tasks (2 hours)
            case ALGORITHMIC_EXECUTION, WORKFLOW_EXECUTION, TASK_COORDINATION -> 7200;
            
            // Custom tasks (15 minutes default)
            case CUSTOM_TASK, SCRIPT_EXECUTION, PLUGIN_EXECUTION -> 900;
            
            // Default for others
            default -> 600;
        };
    }

    /**
     * Get the estimated duration for this task type in seconds
     */
    public int getEstimatedDurationSeconds() {
        return switch (this) {
            case ALERT_GENERATION, EMAIL_NOTIFICATION, SMS_NOTIFICATION, PUSH_NOTIFICATION -> 5;
            case ORDER_EXECUTION, COMPLIANCE_CHECK -> 15;
            case TECHNICAL_ANALYSIS, RISK_MONITORING -> 30;
            case MARKET_ANALYSIS, FUNDAMENTAL_ANALYSIS, PORTFOLIO_ANALYSIS, RISK_ASSESSMENT -> 120;
            case SENTIMENT_ANALYSIS, EXECUTION_MONITORING -> 60;
            case PORTFOLIO_OPTIMIZATION, ASSET_ALLOCATION, VAR_CALCULATION -> 300;
            case REBALANCING, STRESS_TESTING -> 600;
            case MARKET_SCREENING, PERFORMANCE_ANALYSIS, REPORT_GENERATION -> 900;
            case DIVERSIFICATION_ANALYSIS, SLIPPAGE_ANALYSIS, LIQUIDITY_ANALYSIS -> 1200;
            case ALGORITHMIC_EXECUTION -> 3600;
            case WORKFLOW_EXECUTION, TASK_COORDINATION -> 1800;
            case CUSTOM_TASK, SCRIPT_EXECUTION, PLUGIN_EXECUTION -> 300;
            default -> 300;
        };
    }

    /**
     * Get the required capabilities for this task type
     */
    public AgentCapability[] getRequiredCapabilities() {
        return switch (this) {
            case MARKET_ANALYSIS -> new AgentCapability[]{AgentCapability.MARKET_DATA_INTEGRATION};
            case TECHNICAL_ANALYSIS -> new AgentCapability[]{AgentCapability.TECHNICAL_ANALYSIS};
            case FUNDAMENTAL_ANALYSIS -> new AgentCapability[]{AgentCapability.FUNDAMENTAL_ANALYSIS};
            case SENTIMENT_ANALYSIS -> new AgentCapability[]{AgentCapability.SENTIMENT_ANALYSIS};
            case MARKET_SCREENING -> new AgentCapability[]{AgentCapability.MARKET_SCREENING};
            case PRICE_PREDICTION -> new AgentCapability[]{AgentCapability.PRICE_PREDICTION};
            
            case PORTFOLIO_ANALYSIS, PORTFOLIO_OPTIMIZATION -> new AgentCapability[]{AgentCapability.PORTFOLIO_OPTIMIZATION};
            case ASSET_ALLOCATION -> new AgentCapability[]{AgentCapability.ASSET_ALLOCATION};
            case REBALANCING -> new AgentCapability[]{AgentCapability.REBALANCING};
            case PERFORMANCE_ANALYSIS -> new AgentCapability[]{AgentCapability.PERFORMANCE_ANALYSIS};
            case DIVERSIFICATION_ANALYSIS -> new AgentCapability[]{AgentCapability.DIVERSIFICATION_ANALYSIS};
            
            case ORDER_EXECUTION -> new AgentCapability[]{AgentCapability.ORDER_EXECUTION};
            case SMART_ROUTING -> new AgentCapability[]{AgentCapability.BROKER_ROUTING};
            case EXECUTION_MONITORING -> new AgentCapability[]{AgentCapability.SLIPPAGE_MONITORING};
            case SLIPPAGE_ANALYSIS -> new AgentCapability[]{AgentCapability.SLIPPAGE_MONITORING};
            case LIQUIDITY_ANALYSIS -> new AgentCapability[]{AgentCapability.LIQUIDITY_ANALYSIS};
            case ALGORITHMIC_EXECUTION -> new AgentCapability[]{AgentCapability.ALGORITHMIC_TRADING};
            
            case RISK_ASSESSMENT, RISK_MONITORING -> new AgentCapability[]{AgentCapability.RISK_MONITORING};
            case VAR_CALCULATION -> new AgentCapability[]{AgentCapability.VAR_CALCULATION};
            case STRESS_TESTING -> new AgentCapability[]{AgentCapability.STRESS_TESTING};
            case COMPLIANCE_CHECK -> new AgentCapability[]{AgentCapability.COMPLIANCE_CHECK};
            case DRAWDOWN_MONITORING -> new AgentCapability[]{AgentCapability.DRAWDOWN_MONITORING};
            
            case ALERT_GENERATION -> new AgentCapability[]{AgentCapability.REAL_TIME_ALERTS};
            case REPORT_GENERATION -> new AgentCapability[]{AgentCapability.REPORT_GENERATION};
            case EMAIL_NOTIFICATION -> new AgentCapability[]{AgentCapability.EMAIL_ALERTS};
            case SMS_NOTIFICATION -> new AgentCapability[]{AgentCapability.SMS_ALERTS};
            case PUSH_NOTIFICATION -> new AgentCapability[]{AgentCapability.PUSH_NOTIFICATIONS};
            
            case WORKFLOW_EXECUTION -> new AgentCapability[]{AgentCapability.WORKFLOW_EXECUTION};
            case TASK_COORDINATION -> new AgentCapability[]{AgentCapability.TASK_COORDINATION};
            case AGENT_COMMUNICATION -> new AgentCapability[]{AgentCapability.MULTI_AGENT_COMMUNICATION};
            case DECISION_MAKING -> new AgentCapability[]{AgentCapability.DECISION_MAKING};
            
            case CUSTOM_TASK, SCRIPT_EXECUTION, PLUGIN_EXECUTION -> new AgentCapability[]{AgentCapability.CUSTOM_LOGIC};
            
            default -> new AgentCapability[0];
        };
    }

    /**
     * Get the default priority for this task type
     */
    public TaskPriority getDefaultPriority() {
        return switch (this) {
            case ORDER_EXECUTION, SMART_ROUTING, ALGORITHMIC_EXECUTION -> TaskPriority.HIGH;
            case ALERT_GENERATION, EMAIL_NOTIFICATION, SMS_NOTIFICATION, PUSH_NOTIFICATION -> TaskPriority.HIGH;
            case COMPLIANCE_CHECK, RISK_MONITORING -> TaskPriority.HIGH;
            case EXECUTION_MONITORING, RISK_ASSESSMENT -> TaskPriority.NORMAL;
            case MARKET_ANALYSIS, TECHNICAL_ANALYSIS, PORTFOLIO_ANALYSIS -> TaskPriority.NORMAL;
            case REPORT_GENERATION, PERFORMANCE_ANALYSIS -> TaskPriority.LOW;
            case MARKET_SCREENING, DIVERSIFICATION_ANALYSIS -> TaskPriority.LOW;
            default -> TaskPriority.NORMAL;
        };
    }

    /**
     * Check if this task type can be executed in parallel with others
     */
    public boolean canExecuteInParallel() {
        return switch (this) {
            case ORDER_EXECUTION, SMART_ROUTING, ALGORITHMIC_EXECUTION -> false; // Sequential execution required
            case REBALANCING, PORTFOLIO_OPTIMIZATION -> false; // Avoid conflicts
            default -> true;
        };
    }

    /**
     * Check if this task type requires user approval
     */
    public boolean requiresUserApproval() {
        return switch (this) {
            case ORDER_EXECUTION, SMART_ROUTING, ALGORITHMIC_EXECUTION -> true;
            case REBALANCING -> true;
            default -> false;
        };
    }
}