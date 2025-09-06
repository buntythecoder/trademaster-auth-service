package com.trademaster.pnlengine.service;

/**
 * P&L update types for real-time streaming
 */
public enum PnLUpdateType {
    PORTFOLIO_VALUE, POSITION_PNL, REALIZED_PNL, UNREALIZED_PNL, DAY_PNL, 
    RISK_METRICS, PERFORMANCE_ATTRIBUTION, TAX_IMPLICATIONS
}