-- Portfolio Management Service Database Schema
-- Version: 2.0.0 (Java 24 + Virtual Threads)
-- Author: TradeMaster Development Team

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create portfolios table
CREATE TABLE portfolios (
    portfolio_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    portfolio_name VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    total_value DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    cash_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    total_cost DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    realized_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    unrealized_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    day_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cost_basis_method VARCHAR(20) NOT NULL DEFAULT 'FIFO',
    margin_balance DECIMAL(19,4) DEFAULT 0.0000,
    buying_power DECIMAL(19,4) DEFAULT 0.0000,
    day_trades_count INTEGER NOT NULL DEFAULT 0,
    last_valuation_at TIMESTAMP WITH TIME ZONE,
    last_pnl_calculation_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT chk_portfolio_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'FROZEN', 'CLOSED', 'LIQUIDATING')),
    CONSTRAINT chk_cost_basis_method CHECK (cost_basis_method IN ('FIFO', 'LIFO', 'WEIGHTED_AVERAGE', 'SPECIFIC_ID')),
    CONSTRAINT chk_currency_code CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_portfolio_name_length CHECK (LENGTH(portfolio_name) >= 1),
    CONSTRAINT chk_day_trades_count CHECK (day_trades_count >= 0)
);

-- Create positions table
CREATE TABLE positions (
    position_id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    instrument_type VARCHAR(20) DEFAULT 'EQUITY',
    quantity INTEGER NOT NULL,
    average_cost DECIMAL(12,4) NOT NULL,
    total_cost DECIMAL(19,4) NOT NULL,
    current_price DECIMAL(12,4),
    market_value DECIMAL(19,4),
    unrealized_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    realized_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    day_pnl DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    previous_close_price DECIMAL(12,4),
    position_type VARCHAR(10) NOT NULL DEFAULT 'LONG',
    last_trade_price DECIMAL(12,4),
    last_trade_quantity INTEGER,
    last_trade_at TIMESTAMP WITH TIME ZONE,
    last_price_update_at TIMESTAMP WITH TIME ZONE,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_position_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT chk_position_type CHECK (position_type IN ('LONG', 'SHORT', 'HEDGED')),
    CONSTRAINT chk_symbol_format CHECK (symbol ~ '^[A-Z0-9_]{1,20}$'),
    CONSTRAINT chk_exchange_format CHECK (exchange ~ '^[A-Z]{2,10}$'),
    CONSTRAINT chk_quantity_not_zero CHECK (quantity != 0),
    CONSTRAINT chk_average_cost_positive CHECK (average_cost > 0),
    CONSTRAINT chk_total_cost_positive CHECK (total_cost > 0)
);

-- Create portfolio_transactions table
CREATE TABLE portfolio_transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    order_id BIGINT,
    trade_id VARCHAR(50),
    symbol VARCHAR(20),
    exchange VARCHAR(10),
    transaction_type VARCHAR(20) NOT NULL,
    quantity INTEGER,
    price DECIMAL(12,4),
    amount DECIMAL(19,4) NOT NULL,
    commission DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    tax DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    other_fees DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    net_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    settlement_date TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reference_number VARCHAR(50),
    description VARCHAR(500),
    realized_pnl DECIMAL(19,4),
    cost_basis DECIMAL(12,4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN (
        'BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER', 'DIVIDEND', 'INTEREST', 
        'FEE', 'DEPOSIT', 'WITHDRAWAL', 'SPLIT', 'STOCK_DIVIDEND', 'SPINOFF'
    )),
    CONSTRAINT chk_transaction_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_quantity_for_trades CHECK (
        (transaction_type IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER', 'SPLIT', 'STOCK_DIVIDEND') AND quantity IS NOT NULL AND quantity > 0)
        OR (transaction_type NOT IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER', 'SPLIT', 'STOCK_DIVIDEND'))
    ),
    CONSTRAINT chk_price_for_trades CHECK (
        (transaction_type IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER') AND price IS NOT NULL AND price > 0)
        OR (transaction_type NOT IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER'))
    ),
    CONSTRAINT chk_symbol_for_trades CHECK (
        (transaction_type IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER', 'DIVIDEND', 'SPLIT', 'STOCK_DIVIDEND', 'SPINOFF') AND symbol IS NOT NULL)
        OR (transaction_type NOT IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER', 'DIVIDEND', 'SPLIT', 'STOCK_DIVIDEND', 'SPINOFF'))
    )
);

-- Create indexes for optimal query performance
CREATE INDEX idx_portfolio_user_id ON portfolios(user_id);
CREATE INDEX idx_portfolio_status ON portfolios(status);
CREATE INDEX idx_portfolio_updated_at ON portfolios(updated_at);

CREATE INDEX idx_position_portfolio_id ON positions(portfolio_id);
CREATE INDEX idx_position_symbol ON positions(symbol);
CREATE UNIQUE INDEX idx_position_portfolio_symbol ON positions(portfolio_id, symbol);
CREATE INDEX idx_position_updated_at ON positions(updated_at);

CREATE INDEX idx_transaction_portfolio_id ON portfolio_transactions(portfolio_id);
CREATE INDEX idx_transaction_order_id ON portfolio_transactions(order_id);
CREATE INDEX idx_transaction_symbol ON portfolio_transactions(symbol);
CREATE INDEX idx_transaction_type ON portfolio_transactions(transaction_type);
CREATE INDEX idx_transaction_executed_at ON portfolio_transactions(executed_at);
CREATE INDEX idx_transaction_portfolio_date ON portfolio_transactions(portfolio_id, executed_at);

-- Create trigger function for updating timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_portfolios_updated_at 
    BEFORE UPDATE ON portfolios 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_positions_updated_at 
    BEFORE UPDATE ON positions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function for portfolio valuation updates
CREATE OR REPLACE FUNCTION update_portfolio_valuation()
RETURNS TRIGGER AS $$
BEGIN
    -- Update portfolio total value when position is modified
    UPDATE portfolios 
    SET total_value = (
        SELECT COALESCE(cash_balance, 0) + COALESCE(SUM(COALESCE(market_value, 0)), 0)
        FROM positions 
        WHERE portfolio_id = COALESCE(NEW.portfolio_id, OLD.portfolio_id)
    ),
    unrealized_pnl = (
        SELECT COALESCE(SUM(COALESCE(unrealized_pnl, 0)), 0)
        FROM positions 
        WHERE portfolio_id = COALESCE(NEW.portfolio_id, OLD.portfolio_id)
    )
    WHERE portfolio_id = COALESCE(NEW.portfolio_id, OLD.portfolio_id);
    
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

-- Create triggers for automatic portfolio valuation updates
CREATE TRIGGER trigger_update_portfolio_on_position_change
    AFTER INSERT OR UPDATE OR DELETE ON positions
    FOR EACH ROW EXECUTE FUNCTION update_portfolio_valuation();

-- Create function for cash balance updates
CREATE OR REPLACE FUNCTION update_cash_balance()
RETURNS TRIGGER AS $$
BEGIN
    -- Update portfolio cash balance when transaction is added
    UPDATE portfolios 
    SET cash_balance = cash_balance + 
        CASE 
            WHEN NEW.transaction_type IN ('BUY', 'BUY_TO_COVER', 'FEE', 'WITHDRAWAL') THEN -NEW.net_amount
            WHEN NEW.transaction_type IN ('SELL', 'SHORT_SELL', 'DIVIDEND', 'INTEREST', 'DEPOSIT') THEN NEW.net_amount
            ELSE 0
        END
    WHERE portfolio_id = NEW.portfolio_id;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic cash balance updates
CREATE TRIGGER trigger_update_cash_on_transaction
    AFTER INSERT ON portfolio_transactions
    FOR EACH ROW EXECUTE FUNCTION update_cash_balance();

-- Insert sample data for development (will be removed in production)
INSERT INTO portfolios (user_id, portfolio_name, currency, cash_balance, total_value) VALUES
(1, 'Main Trading Portfolio', 'INR', 100000.00, 100000.00),
(2, 'Growth Portfolio', 'INR', 250000.00, 250000.00),
(3, 'Conservative Portfolio', 'INR', 50000.00, 50000.00);

-- Add comments for documentation
COMMENT ON TABLE portfolios IS 'User portfolios with comprehensive tracking of value, P&L, and status';
COMMENT ON TABLE positions IS 'Individual security positions within portfolios with real-time P&L calculation';
COMMENT ON TABLE portfolio_transactions IS 'Complete audit trail of all portfolio transactions and cash movements';

COMMENT ON COLUMN portfolios.cost_basis_method IS 'Method used for calculating average cost basis (FIFO, LIFO, WEIGHTED_AVERAGE, SPECIFIC_ID)';
COMMENT ON COLUMN portfolios.day_trades_count IS 'Number of day trades executed (resets daily for PDT rule compliance)';
COMMENT ON COLUMN portfolios.buying_power IS 'Available buying power including margin (if applicable)';

COMMENT ON COLUMN positions.position_type IS 'Type of position: LONG (owned), SHORT (borrowed/sold), HEDGED (complex strategy)';
COMMENT ON COLUMN positions.instrument_type IS 'Type of financial instrument (EQUITY, OPTION, FUTURE, BOND, etc.)';
COMMENT ON COLUMN positions.unrealized_pnl IS 'Mark-to-market profit/loss for open position';
COMMENT ON COLUMN positions.realized_pnl IS 'Cumulative realized profit/loss from closed portions';

COMMENT ON COLUMN portfolio_transactions.settlement_date IS 'Date when transaction settles (typically T+2 for equities)';
COMMENT ON COLUMN portfolio_transactions.net_amount IS 'Transaction amount after fees, commissions, and taxes';
COMMENT ON COLUMN portfolio_transactions.realized_pnl IS 'Realized P&L for sell transactions';