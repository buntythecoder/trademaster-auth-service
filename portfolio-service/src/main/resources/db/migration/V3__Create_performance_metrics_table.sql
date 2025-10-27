-- Performance Metrics Snapshots Table
-- Version: 2.0.0 (Java 24 + Virtual Threads)
-- Author: TradeMaster Development Team

-- Create performance_metrics table for time-series performance tracking
CREATE TABLE performance_metrics (
    metric_id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    snapshot_date TIMESTAMP WITH TIME ZONE NOT NULL,
    period_type VARCHAR(20) NOT NULL DEFAULT 'DAILY',

    -- Return metrics
    total_return DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    total_return_percent DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    annualized_return DECIMAL(10,4) NOT NULL DEFAULT 0.0000,

    -- Risk metrics
    sharpe_ratio DECIMAL(10,4),
    sortino_ratio DECIMAL(10,4),
    alpha DECIMAL(10,4),
    beta DECIMAL(10,4),

    -- Drawdown metrics
    max_drawdown DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    max_drawdown_percent DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    current_drawdown DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    current_drawdown_percent DECIMAL(10,4) NOT NULL DEFAULT 0.0000,

    -- Win/Loss metrics
    win_rate DECIMAL(5,2),
    avg_win DECIMAL(19,4),
    avg_loss DECIMAL(19,4),
    profit_factor DECIMAL(10,4),

    -- Volatility metrics
    volatility DECIMAL(10,4),
    downside_volatility DECIMAL(10,4),
    var_95 DECIMAL(19,4),
    var_99 DECIMAL(19,4),

    -- Portfolio values
    portfolio_value DECIMAL(19,4) NOT NULL,
    cash_balance DECIMAL(19,4) NOT NULL,
    positions_value DECIMAL(19,4) NOT NULL,

    -- P&L metrics
    realized_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    unrealized_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    day_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,

    -- Position metrics
    position_count INTEGER NOT NULL DEFAULT 0,
    open_position_count INTEGER NOT NULL DEFAULT 0,
    avg_position_size DECIMAL(19,4),
    largest_position_value DECIMAL(19,4),

    -- Trade metrics
    trade_count INTEGER NOT NULL DEFAULT 0,
    winning_trades INTEGER NOT NULL DEFAULT 0,
    losing_trades INTEGER NOT NULL DEFAULT 0,

    -- Benchmark comparison
    benchmark_symbol VARCHAR(20),
    benchmark_return DECIMAL(10,4),
    relative_return DECIMAL(10,4),

    -- Metadata
    calculation_method VARCHAR(50) DEFAULT 'TIME_WEIGHTED',
    calculation_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_performance_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT chk_period_type CHECK (period_type IN ('INTRADAY', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'CUSTOM')),
    CONSTRAINT chk_calculation_method CHECK (calculation_method IN ('TIME_WEIGHTED', 'MONEY_WEIGHTED', 'SIMPLE', 'COMPOUND')),
    CONSTRAINT chk_win_rate CHECK (win_rate IS NULL OR (win_rate >= 0 AND win_rate <= 100)),
    CONSTRAINT chk_portfolio_value_positive CHECK (portfolio_value >= 0),
    CONSTRAINT chk_position_count CHECK (position_count >= 0),
    CONSTRAINT chk_trade_count CHECK (trade_count >= 0)
);

-- Create indexes for optimal time-series query performance
CREATE INDEX idx_performance_portfolio_id ON performance_metrics(portfolio_id);
CREATE INDEX idx_performance_snapshot_date ON performance_metrics(snapshot_date);
CREATE INDEX idx_performance_period_type ON performance_metrics(period_type);
CREATE INDEX idx_performance_portfolio_date ON performance_metrics(portfolio_id, snapshot_date DESC);
CREATE UNIQUE INDEX idx_performance_portfolio_period_snapshot ON performance_metrics(portfolio_id, period_type, snapshot_date);

-- Create hypertable for time-series optimization (TimescaleDB extension - optional)
-- SELECT create_hypertable('performance_metrics', 'snapshot_date', if_not_exists => TRUE);

-- Add comments for documentation
COMMENT ON TABLE performance_metrics IS 'Time-series performance metrics snapshots for portfolio analytics and historical tracking';
COMMENT ON COLUMN performance_metrics.snapshot_date IS 'Date and time of the performance snapshot';
COMMENT ON COLUMN performance_metrics.period_type IS 'Aggregation period (INTRADAY, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)';
COMMENT ON COLUMN performance_metrics.total_return IS 'Absolute total return since portfolio inception';
COMMENT ON COLUMN performance_metrics.total_return_percent IS 'Total return percentage since portfolio inception';
COMMENT ON COLUMN performance_metrics.annualized_return IS 'Annualized return percentage (CAGR)';
COMMENT ON COLUMN performance_metrics.sharpe_ratio IS 'Risk-adjusted return metric (return per unit of volatility)';
COMMENT ON COLUMN performance_metrics.sortino_ratio IS 'Downside risk-adjusted return metric';
COMMENT ON COLUMN performance_metrics.alpha IS 'Excess return vs. benchmark (Jensen''s Alpha)';
COMMENT ON COLUMN performance_metrics.beta IS 'Systematic risk relative to benchmark';
COMMENT ON COLUMN performance_metrics.max_drawdown IS 'Maximum peak-to-trough decline in portfolio value';
COMMENT ON COLUMN performance_metrics.max_drawdown_percent IS 'Maximum drawdown as percentage of peak value';
COMMENT ON COLUMN performance_metrics.current_drawdown IS 'Current drawdown from recent peak';
COMMENT ON COLUMN performance_metrics.win_rate IS 'Percentage of profitable trades';
COMMENT ON COLUMN performance_metrics.volatility IS 'Annualized standard deviation of returns';
COMMENT ON COLUMN performance_metrics.downside_volatility IS 'Standard deviation of negative returns only';
COMMENT ON COLUMN performance_metrics.var_95 IS 'Value at Risk at 95% confidence level';
COMMENT ON COLUMN performance_metrics.var_99 IS 'Value at Risk at 99% confidence level';
COMMENT ON COLUMN performance_metrics.benchmark_symbol IS 'Benchmark index for comparison (e.g., NIFTY_50, SENSEX)';
COMMENT ON COLUMN performance_metrics.calculation_method IS 'Return calculation method (TIME_WEIGHTED, MONEY_WEIGHTED)';
