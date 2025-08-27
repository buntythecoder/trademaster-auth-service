package com.trademaster.agentos.domain.entity;

/**
 * Agent Capability Enumeration
 * 
 * Defines the specific capabilities that AI agents can possess in the TradeMaster Agent OS.
 * Used for agent selection, task routing, and capability-based filtering.
 */
public enum AgentCapability {
    // Market Analysis Capabilities
    TECHNICAL_ANALYSIS("Technical Analysis", "Analyze charts, patterns, and technical indicators"),
    FUNDAMENTAL_ANALYSIS("Fundamental Analysis", "Analyze company fundamentals and financial data"),
    SENTIMENT_ANALYSIS("Sentiment Analysis", "Analyze market sentiment and news impact"),
    MARKET_SCREENING("Market Screening", "Screen and filter securities based on criteria"),
    PRICE_PREDICTION("Price Prediction", "Predict future price movements using models"),
    PATTERN_RECOGNITION("Pattern Recognition", "Identify chart patterns and formations"),
    
    // Portfolio Management Capabilities
    PORTFOLIO_OPTIMIZATION("Portfolio Optimization", "Optimize asset allocation for risk/return"),
    ASSET_ALLOCATION("Asset Allocation", "Determine optimal asset distribution"),
    RISK_ASSESSMENT("Risk Assessment", "Assess and quantify portfolio risks"),
    PERFORMANCE_ANALYSIS("Performance Analysis", "Analyze portfolio performance metrics"),
    REBALANCING("Rebalancing", "Automatically rebalance portfolio allocations"),
    DIVERSIFICATION_ANALYSIS("Diversification Analysis", "Analyze portfolio diversification"),
    
    // Trading Execution Capabilities
    ORDER_EXECUTION("Order Execution", "Execute trading orders across brokers"),
    BROKER_ROUTING("Broker Routing", "Route orders to optimal brokers"),
    EXECUTION_OPTIMIZATION("Execution Optimization", "Optimize order execution timing and size"),
    SLIPPAGE_MONITORING("Slippage Monitoring", "Monitor and minimize execution slippage"),
    LIQUIDITY_ANALYSIS("Liquidity Analysis", "Analyze market liquidity for execution"),
    ALGORITHMIC_TRADING("Algorithmic Trading", "Execute algorithmic trading strategies"),
    
    // Risk Management Capabilities
    RISK_MONITORING("Risk Monitoring", "Monitor portfolio and position risks in real-time"),
    VAR_CALCULATION("VaR Calculation", "Calculate Value at Risk metrics"),
    STRESS_TESTING("Stress Testing", "Perform portfolio stress testing scenarios"),
    COMPLIANCE_CHECK("Compliance Check", "Validate compliance with regulations and limits"),
    CORRELATION_ANALYSIS("Correlation Analysis", "Analyze asset correlations and dependencies"),
    DRAWDOWN_MONITORING("Drawdown Monitoring", "Monitor and alert on portfolio drawdowns"),
    
    // Communication & Notification Capabilities
    EMAIL_ALERTS("Email Alerts", "Send email notifications and alerts"),
    SMS_ALERTS("SMS Alerts", "Send SMS text message alerts"),
    PUSH_NOTIFICATIONS("Push Notifications", "Send mobile push notifications"),
    REPORT_GENERATION("Report Generation", "Generate automated reports and summaries"),
    REAL_TIME_ALERTS("Real-time Alerts", "Send immediate real-time alerts"),
    SCHEDULED_REPORTS("Scheduled Reports", "Generate and send scheduled reports"),
    
    // Data & Integration Capabilities
    MARKET_DATA_INTEGRATION("Market Data Integration", "Integrate with market data providers"),
    BROKER_INTEGRATION("Broker Integration", "Integrate with broker APIs and systems"),
    DATABASE_OPERATIONS("Database Operations", "Perform database queries and operations"),
    API_INTEGRATION("API Integration", "Integrate with external APIs and services"),
    DATA_VALIDATION("Data Validation", "Validate and cleanse data inputs"),
    REAL_TIME_STREAMING("Real-time Streaming", "Handle real-time data streams"),
    
    // Advanced AI Capabilities
    MACHINE_LEARNING("Machine Learning", "Apply machine learning models and predictions"),
    NATURAL_LANGUAGE_PROCESSING("Natural Language Processing", "Process and understand natural language"),
    ANOMALY_DETECTION("Anomaly Detection", "Detect unusual patterns and anomalies"),
    PREDICTIVE_MODELING("Predictive Modeling", "Build and apply predictive models"),
    REINFORCEMENT_LEARNING("Reinforcement Learning", "Learn and adapt from trading outcomes"),
    
    // Workflow & Orchestration Capabilities
    WORKFLOW_EXECUTION("Workflow Execution", "Execute multi-step workflows"),
    TASK_COORDINATION("Task Coordination", "Coordinate tasks across multiple agents"),
    EVENT_PROCESSING("Event Processing", "Process and respond to system events"),
    DECISION_MAKING("Decision Making", "Make autonomous trading decisions"),
    MULTI_AGENT_COMMUNICATION("Multi-agent Communication", "Communicate with other agents"),
    
    // Custom & Extensible Capabilities
    CUSTOM_LOGIC("Custom Logic", "Execute custom user-defined logic"),
    PLUGIN_SUPPORT("Plugin Support", "Support for custom plugins and extensions"),
    SCRIPTING("Scripting", "Execute custom scripts and automation");

    private final String displayName;
    private final String description;

    AgentCapability(String displayName, String description) {
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
     * Get the category of this capability for grouping purposes
     */
    public CapabilityCategory getCategory() {
        return switch (this) {
            case TECHNICAL_ANALYSIS, FUNDAMENTAL_ANALYSIS, SENTIMENT_ANALYSIS, 
                 MARKET_SCREENING, PRICE_PREDICTION, PATTERN_RECOGNITION -> CapabilityCategory.MARKET_ANALYSIS;
                 
            case PORTFOLIO_OPTIMIZATION, ASSET_ALLOCATION, RISK_ASSESSMENT, 
                 PERFORMANCE_ANALYSIS, REBALANCING, DIVERSIFICATION_ANALYSIS -> CapabilityCategory.PORTFOLIO_MANAGEMENT;
                 
            case ORDER_EXECUTION, BROKER_ROUTING, EXECUTION_OPTIMIZATION, 
                 SLIPPAGE_MONITORING, LIQUIDITY_ANALYSIS, ALGORITHMIC_TRADING -> CapabilityCategory.TRADING_EXECUTION;
                 
            case RISK_MONITORING, VAR_CALCULATION, STRESS_TESTING, 
                 COMPLIANCE_CHECK, CORRELATION_ANALYSIS, DRAWDOWN_MONITORING -> CapabilityCategory.RISK_MANAGEMENT;
                 
            case EMAIL_ALERTS, SMS_ALERTS, PUSH_NOTIFICATIONS, 
                 REPORT_GENERATION, REAL_TIME_ALERTS, SCHEDULED_REPORTS -> CapabilityCategory.COMMUNICATION;
                 
            case MARKET_DATA_INTEGRATION, BROKER_INTEGRATION, DATABASE_OPERATIONS, 
                 API_INTEGRATION, DATA_VALIDATION, REAL_TIME_STREAMING -> CapabilityCategory.DATA_INTEGRATION;
                 
            case MACHINE_LEARNING, NATURAL_LANGUAGE_PROCESSING, ANOMALY_DETECTION, 
                 PREDICTIVE_MODELING, REINFORCEMENT_LEARNING -> CapabilityCategory.AI_ADVANCED;
                 
            case WORKFLOW_EXECUTION, TASK_COORDINATION, EVENT_PROCESSING, 
                 DECISION_MAKING, MULTI_AGENT_COMMUNICATION -> CapabilityCategory.ORCHESTRATION;
                 
            case CUSTOM_LOGIC, PLUGIN_SUPPORT, SCRIPTING -> CapabilityCategory.CUSTOM;
        };
    }

    /**
     * Check if this capability requires special permissions or privileges
     */
    public boolean requiresElevatedPrivileges() {
        return switch (this) {
            case ORDER_EXECUTION, BROKER_ROUTING, DATABASE_OPERATIONS, 
                 COMPLIANCE_CHECK, REAL_TIME_STREAMING -> true;
            default -> false;
        };
    }

    /**
     * Get the complexity level of this capability (1-5 scale)
     */
    public int getComplexityLevel() {
        return switch (this) {
            // Basic capabilities (1-2)
            case EMAIL_ALERTS, SMS_ALERTS, PUSH_NOTIFICATIONS, 
                 DATABASE_OPERATIONS, API_INTEGRATION -> 1;
                 
            case MARKET_DATA_INTEGRATION, BROKER_INTEGRATION, 
                 DATA_VALIDATION, REPORT_GENERATION -> 2;
                 
            // Intermediate capabilities (3)
            case TECHNICAL_ANALYSIS, FUNDAMENTAL_ANALYSIS, RISK_ASSESSMENT,
                 PERFORMANCE_ANALYSIS, ORDER_EXECUTION, RISK_MONITORING -> 3;
                 
            // Advanced capabilities (4)
            case PORTFOLIO_OPTIMIZATION, EXECUTION_OPTIMIZATION, VAR_CALCULATION,
                 STRESS_TESTING, SENTIMENT_ANALYSIS, PATTERN_RECOGNITION -> 4;
                 
            // Expert capabilities (5)
            case MACHINE_LEARNING, PREDICTIVE_MODELING, REINFORCEMENT_LEARNING,
                 ANOMALY_DETECTION, ALGORITHMIC_TRADING, WORKFLOW_EXECUTION -> 5;
                 
            default -> 3;
        };
    }

    /**
     * Capability categories for grouping and organization
     */
    public enum CapabilityCategory {
        MARKET_ANALYSIS("Market Analysis", "Capabilities related to market research and analysis"),
        PORTFOLIO_MANAGEMENT("Portfolio Management", "Capabilities for portfolio optimization and management"),
        TRADING_EXECUTION("Trading Execution", "Capabilities for order execution and trading"),
        RISK_MANAGEMENT("Risk Management", "Capabilities for risk monitoring and management"),
        COMMUNICATION("Communication", "Capabilities for notifications and reporting"),
        DATA_INTEGRATION("Data Integration", "Capabilities for data access and integration"),
        AI_ADVANCED("Advanced AI", "Advanced artificial intelligence capabilities"),
        ORCHESTRATION("Orchestration", "Capabilities for workflow and task coordination"),
        CUSTOM("Custom", "Custom and extensible capabilities");

        private final String displayName;
        private final String description;

        CapabilityCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}