-- =====================================================
-- TradeMaster Market Data Service
-- Migration V3: Economic Events Table
-- Description: Create economic_events table for economic calendar data with market impact analysis
-- Author: Database Administrator
-- Version: 1.0.0
-- =====================================================

-- Create economic_events table
CREATE TABLE economic_events (
    -- Primary Key
    id                     BIGSERIAL PRIMARY KEY,
    
    -- External identifier (unique)
    event_id               VARCHAR(255) NOT NULL UNIQUE,
    
    -- Event details
    title                  VARCHAR(500) NOT NULL,
    description            TEXT,
    country                CHAR(3) NOT NULL,       -- ISO 3-letter country code (IND, USA, etc.)
    category               VARCHAR(100) NOT NULL,  -- GDP, Inflation, Employment, etc.
    importance             VARCHAR(20) NOT NULL,   -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- Timing
    event_date             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    frequency              VARCHAR(50),            -- Monthly, Quarterly, Annual, etc.
    unit                   VARCHAR(100),           -- %, billions, millions, index, etc.
    
    -- Forecast and actual values (high precision for economic data)
    forecast_value         NUMERIC(15,4),
    previous_value         NUMERIC(15,4),
    actual_value           NUMERIC(15,4),
    revision_value         NUMERIC(10,4),          -- Revision to previous value
    
    -- Market impact analysis
    market_impact_score    NUMERIC(5,2),           -- 0-100 impact score
    expected_sentiment     VARCHAR(20),            -- VERY_BEARISH, BEARISH, etc.
    actual_sentiment       VARCHAR(20),            -- Actual sentiment after release
    
    -- Status and timing
    status                 VARCHAR(20) NOT NULL,   -- SCHEDULED, RELEASED, DELAYED, etc.
    release_time           TIMESTAMP WITHOUT TIME ZONE,
    next_release_date      TIMESTAMP WITHOUT TIME ZONE,
    
    -- Data source information
    source                 VARCHAR(100),           -- Central Bank, Government Agency, etc.
    source_url             VARCHAR(500),
    data_provider          VARCHAR(50),            -- TradingEconomics, Alpha Vantage, etc.
    
    -- Additional metadata (JSON for flexibility)
    related_symbols        JSONB,                  -- Array of affected symbols
    related_sectors        JSONB,                  -- Array of affected sectors
    historical_data        JSONB,                  -- Historical values for context
    analysis_notes         TEXT,
    
    -- Audit fields
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                BIGINT NOT NULL DEFAULT 0
);

-- Performance Indexes
CREATE INDEX idx_economic_events_date ON economic_events(event_date);
CREATE INDEX idx_economic_events_country ON economic_events(country);
CREATE INDEX idx_economic_events_importance ON economic_events(importance);
CREATE INDEX idx_economic_events_category ON economic_events(category);
CREATE INDEX idx_economic_events_status ON economic_events(status);

-- Composite indexes for common queries
CREATE INDEX idx_economic_events_country_date ON economic_events(country, event_date);
CREATE INDEX idx_economic_events_importance_date ON economic_events(importance, event_date);
CREATE INDEX idx_economic_events_category_date ON economic_events(category, event_date);

-- GIN indexes for JSON columns
CREATE INDEX idx_economic_events_related_symbols ON economic_events USING GIN(related_symbols);
CREATE INDEX idx_economic_events_related_sectors ON economic_events USING GIN(related_sectors);

-- Partial indexes for performance
CREATE INDEX idx_economic_events_upcoming ON economic_events(event_date) 
    WHERE event_date > CURRENT_TIMESTAMP AND event_date <= CURRENT_TIMESTAMP + INTERVAL '7 days';
CREATE INDEX idx_economic_events_today ON economic_events(importance, event_date) 
    WHERE DATE(event_date) = CURRENT_DATE;
CREATE INDEX idx_economic_events_high_impact ON economic_events(event_date) 
    WHERE importance IN ('HIGH', 'CRITICAL') OR market_impact_score >= 60;

-- Add CHECK constraints for data validation
ALTER TABLE economic_events ADD CONSTRAINT chk_country_code 
    CHECK (country ~ '^[A-Z]{3}$');

ALTER TABLE economic_events ADD CONSTRAINT chk_importance 
    CHECK (importance IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

ALTER TABLE economic_events ADD CONSTRAINT chk_status 
    CHECK (status IN ('SCHEDULED', 'RELEASED', 'DELAYED', 'CANCELLED', 'REVISED', 'PRELIMINARY'));

ALTER TABLE economic_events ADD CONSTRAINT chk_sentiment 
    CHECK (expected_sentiment IN ('VERY_BEARISH', 'BEARISH', 'NEUTRAL', 'BULLISH', 'VERY_BULLISH') OR expected_sentiment IS NULL);

ALTER TABLE economic_events ADD CONSTRAINT chk_actual_sentiment 
    CHECK (actual_sentiment IN ('VERY_BEARISH', 'BEARISH', 'NEUTRAL', 'BULLISH', 'VERY_BULLISH') OR actual_sentiment IS NULL);

ALTER TABLE economic_events ADD CONSTRAINT chk_impact_score_range 
    CHECK (market_impact_score IS NULL OR (market_impact_score >= 0 AND market_impact_score <= 100));

-- Add table comment
COMMENT ON TABLE economic_events IS 'Economic calendar events with market impact analysis and forecasting';

-- Add column comments for clarity
COMMENT ON COLUMN economic_events.country IS 'ISO 3-letter country code (IND, USA, CHN, etc.)';
COMMENT ON COLUMN economic_events.importance IS 'Event importance: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN economic_events.status IS 'Event status: SCHEDULED, RELEASED, DELAYED, CANCELLED, REVISED, PRELIMINARY';
COMMENT ON COLUMN economic_events.market_impact_score IS 'Calculated market impact score from 0-100';
COMMENT ON COLUMN economic_events.expected_sentiment IS 'Expected market sentiment: VERY_BEARISH to VERY_BULLISH';
COMMENT ON COLUMN economic_events.actual_sentiment IS 'Actual market sentiment after event release';

-- Create trigger for updating updated_at
CREATE TRIGGER economic_events_update_timestamp
    BEFORE UPDATE ON economic_events
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified();