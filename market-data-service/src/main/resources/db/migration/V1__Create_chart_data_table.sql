-- =====================================================
-- TradeMaster Market Data Service
-- Migration V1: Chart Data Table
-- Description: Create chart_data table for time-series OHLCV data with technical indicators
-- Author: Database Administrator
-- Version: 1.0.0
-- =====================================================

-- Create chart_data table
CREATE TABLE chart_data (
    -- Primary Key
    id                     BIGSERIAL PRIMARY KEY,
    
    -- Core identifiers
    symbol                 VARCHAR(20) NOT NULL,
    exchange               VARCHAR(10) NOT NULL,
    timeframe              VARCHAR(5) NOT NULL,
    timestamp              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    
    -- OHLCV data (precision 15, scale 6 for high precision)
    open                   NUMERIC(15,6) NOT NULL,
    high                   NUMERIC(15,6) NOT NULL,
    low                    NUMERIC(15,6) NOT NULL,
    close                  NUMERIC(15,6) NOT NULL,
    volume                 BIGINT NOT NULL,
    
    -- Adjusted data for corporate actions
    adjusted_close         NUMERIC(15,6),
    split_coefficient      NUMERIC(8,6),
    dividend_amount        NUMERIC(15,6),
    
    -- Moving Averages
    sma20                  NUMERIC(15,6), -- Simple Moving Average 20
    sma50                  NUMERIC(15,6), -- Simple Moving Average 50
    sma200                 NUMERIC(15,6), -- Simple Moving Average 200
    ema12                  NUMERIC(15,6), -- Exponential Moving Average 12
    ema26                  NUMERIC(15,6), -- Exponential Moving Average 26
    
    -- Momentum Indicators
    rsi                    NUMERIC(8,4),  -- Relative Strength Index (0-100)
    
    -- MACD Indicators
    macd                   NUMERIC(15,6), -- MACD Line
    macd_signal            NUMERIC(15,6), -- MACD Signal Line
    macd_histogram         NUMERIC(15,6), -- MACD Histogram
    
    -- Bollinger Bands
    bollinger_upper        NUMERIC(15,6), -- Upper Band
    bollinger_middle       NUMERIC(15,6), -- Middle Band (SMA20)
    bollinger_lower        NUMERIC(15,6), -- Lower Band
    
    -- Stochastic Oscillator
    stoch_k                NUMERIC(8,4),  -- %K (0-100)
    stoch_d                NUMERIC(8,4),  -- %D (0-100)
    
    -- Volatility and Volume Indicators
    atr                    NUMERIC(15,6), -- Average True Range
    obv                    NUMERIC(15,6), -- On Balance Volume
    williams_r             NUMERIC(8,4),  -- Williams %R (-100 to 0)
    cci                    NUMERIC(8,4),  -- Commodity Channel Index
    
    -- Market microstructure
    trade_count            INTEGER,
    vwap                   NUMERIC(15,6), -- Volume Weighted Average Price
    twap                   NUMERIC(15,6), -- Time Weighted Average Price
    volatility             NUMERIC(8,4),  -- Historical volatility
    
    -- Data quality metadata
    is_complete            BOOLEAN NOT NULL DEFAULT true,
    has_gaps               BOOLEAN NOT NULL DEFAULT false,
    data_source            VARCHAR(100),
    
    -- Audit fields
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                BIGINT NOT NULL DEFAULT 0
);

-- Performance Indexes
CREATE INDEX idx_chart_data_symbol_timeframe ON chart_data(symbol, timeframe);
CREATE INDEX idx_chart_data_timestamp ON chart_data(timestamp);
CREATE INDEX idx_chart_data_symbol_timestamp ON chart_data(symbol, timestamp);
CREATE INDEX idx_chart_data_timeframe_timestamp ON chart_data(timeframe, timestamp);

-- Composite index for range queries
CREATE INDEX idx_chart_data_symbol_timeframe_timestamp ON chart_data(symbol, timeframe, timestamp);

-- Partial index for incomplete candles
CREATE INDEX idx_chart_data_incomplete ON chart_data(symbol, timeframe, timestamp) WHERE is_complete = false;

-- Add table comment
COMMENT ON TABLE chart_data IS 'Time-series OHLCV market data with technical indicators optimized for charting applications';

-- Add column comments for clarity
COMMENT ON COLUMN chart_data.timeframe IS 'Time interval: M1, M5, M15, M30, H1, H4, D1, W1, MN1';
COMMENT ON COLUMN chart_data.is_complete IS 'Whether this candle/bar is complete (not still forming)';
COMMENT ON COLUMN chart_data.has_gaps IS 'Whether trading gaps were detected in this period';
COMMENT ON COLUMN chart_data.version IS 'Optimistic locking version for concurrent updates';