-- Risk Limits Table
-- Version: 2.0.0 (Java 24 + Virtual Threads)
-- Author: TradeMaster Development Team

-- Create risk_limits table for portfolio risk management
CREATE TABLE risk_limits (
    risk_limit_id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    max_single_position_percent DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    max_sector_concentration_percent DECIMAL(5,2) NOT NULL DEFAULT 30.00,
    max_leverage_ratio DECIMAL(5,2) NOT NULL DEFAULT 2.00,
    daily_loss_limit DECIMAL(19,4),
    max_drawdown_percent DECIMAL(5,2) NOT NULL DEFAULT 25.00,
    var_95_limit DECIMAL(19,4),
    var_99_limit DECIMAL(19,4),
    max_day_trades INTEGER NOT NULL DEFAULT 3,
    margin_call_threshold DECIMAL(5,2) NOT NULL DEFAULT 30.00,
    margin_maintenance_ratio DECIMAL(5,2) NOT NULL DEFAULT 25.00,
    sector_limits JSONB,
    instrument_type_limits JSONB,
    exchange_limits JSONB,
    auto_liquidation_enabled BOOLEAN NOT NULL DEFAULT false,
    alerts_enabled BOOLEAN NOT NULL DEFAULT true,
    risk_framework VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    effective_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT fk_risk_limit_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT chk_max_single_position_percent CHECK (max_single_position_percent > 0 AND max_single_position_percent <= 100),
    CONSTRAINT chk_max_sector_concentration_percent CHECK (max_sector_concentration_percent > 0 AND max_sector_concentration_percent <= 100),
    CONSTRAINT chk_max_leverage_ratio CHECK (max_leverage_ratio >= 1.00),
    CONSTRAINT chk_max_drawdown_percent CHECK (max_drawdown_percent > 0 AND max_drawdown_percent <= 100),
    CONSTRAINT chk_max_day_trades CHECK (max_day_trades >= 0),
    CONSTRAINT chk_margin_call_threshold CHECK (margin_call_threshold > 0 AND margin_call_threshold <= 100),
    CONSTRAINT chk_margin_maintenance_ratio CHECK (margin_maintenance_ratio > 0 AND margin_maintenance_ratio <= 100),
    CONSTRAINT chk_risk_framework CHECK (risk_framework IN ('STANDARD', 'AGGRESSIVE', 'CONSERVATIVE', 'CUSTOM'))
);

-- Create indexes for optimal query performance
CREATE INDEX idx_risk_limit_portfolio_id ON risk_limits(portfolio_id);
CREATE INDEX idx_risk_limit_effective_date ON risk_limits(effective_date);
CREATE INDEX idx_risk_limit_risk_framework ON risk_limits(risk_framework);
CREATE UNIQUE INDEX idx_risk_limit_portfolio_active ON risk_limits(portfolio_id, effective_date);

-- Create trigger for automatic timestamp updates
CREATE TRIGGER update_risk_limits_updated_at
    BEFORE UPDATE ON risk_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default risk limits for existing portfolios
INSERT INTO risk_limits (portfolio_id, risk_framework, effective_date)
SELECT portfolio_id, 'STANDARD', CURRENT_TIMESTAMP
FROM portfolios
WHERE NOT EXISTS (
    SELECT 1 FROM risk_limits WHERE risk_limits.portfolio_id = portfolios.portfolio_id
);

-- Add comments for documentation
COMMENT ON TABLE risk_limits IS 'Risk limit configurations for portfolio risk management and compliance';
COMMENT ON COLUMN risk_limits.max_single_position_percent IS 'Maximum percentage of portfolio value that can be allocated to a single position';
COMMENT ON COLUMN risk_limits.max_sector_concentration_percent IS 'Maximum percentage of portfolio value that can be allocated to a single sector';
COMMENT ON COLUMN risk_limits.max_leverage_ratio IS 'Maximum leverage ratio allowed (e.g., 2.00 for 2:1 leverage)';
COMMENT ON COLUMN risk_limits.daily_loss_limit IS 'Maximum daily loss amount before trading is halted';
COMMENT ON COLUMN risk_limits.max_drawdown_percent IS 'Maximum drawdown percentage from peak before alerts/actions';
COMMENT ON COLUMN risk_limits.var_95_limit IS 'Value at Risk at 95% confidence level limit';
COMMENT ON COLUMN risk_limits.var_99_limit IS 'Value at Risk at 99% confidence level limit';
COMMENT ON COLUMN risk_limits.max_day_trades IS 'Maximum number of day trades allowed (PDT rule compliance)';
COMMENT ON COLUMN risk_limits.margin_call_threshold IS 'Margin call threshold percentage';
COMMENT ON COLUMN risk_limits.margin_maintenance_ratio IS 'Minimum margin maintenance ratio required';
COMMENT ON COLUMN risk_limits.sector_limits IS 'JSON object containing sector-specific limit overrides';
COMMENT ON COLUMN risk_limits.instrument_type_limits IS 'JSON object containing instrument type-specific limit overrides';
COMMENT ON COLUMN risk_limits.exchange_limits IS 'JSON object containing exchange-specific limit overrides';
COMMENT ON COLUMN risk_limits.auto_liquidation_enabled IS 'Whether positions are automatically liquidated when limits are breached';
COMMENT ON COLUMN risk_limits.alerts_enabled IS 'Whether risk limit breach alerts are enabled';
COMMENT ON COLUMN risk_limits.risk_framework IS 'Risk management framework applied (STANDARD, AGGRESSIVE, CONSERVATIVE, CUSTOM)';
