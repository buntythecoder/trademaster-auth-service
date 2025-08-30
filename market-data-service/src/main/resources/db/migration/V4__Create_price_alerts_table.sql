-- =====================================================
-- TradeMaster Market Data Service
-- Migration V4: Price Alerts Table
-- Description: Create price_alerts table for user-defined alerts with intelligent triggering
-- Author: Database Administrator
-- Version: 1.0.0
-- =====================================================

-- Create price_alerts table
CREATE TABLE price_alerts (
    -- Primary Key
    id                          BIGSERIAL PRIMARY KEY,
    
    -- User and instrument
    user_id                     VARCHAR(100) NOT NULL,
    symbol                      VARCHAR(50) NOT NULL,
    exchange                    VARCHAR(50) NOT NULL,
    
    -- Alert configuration
    name                        VARCHAR(200) NOT NULL,
    description                 TEXT,
    alert_type                  VARCHAR(50) NOT NULL,
    trigger_condition           VARCHAR(50) NOT NULL,
    
    -- Price conditions (high precision for financial data)
    target_price                NUMERIC(15,6),
    stop_price                  NUMERIC(15,6),      -- For range alerts
    baseline_price              NUMERIC(15,6),      -- Reference price for percentage alerts
    percentage_change           NUMERIC(8,4),       -- For percentage-based alerts
    
    -- Technical condition parameters
    moving_average_price        NUMERIC(15,6),      -- For MA crossover alerts
    moving_average_period       INTEGER,            -- MA period (e.g., 20, 50, 200)
    rsi_threshold               NUMERIC(8,4),       -- For RSI alerts
    volume_threshold            NUMERIC(15,6),      -- For volume alerts
    volatility_threshold        NUMERIC(8,4),       -- For volatility alerts
    
    -- Advanced conditions (JSON for flexibility)
    multi_conditions            JSONB,              -- Complex multi-condition alerts
    custom_parameters           JSONB,              -- Custom alert parameters
    
    -- Status and lifecycle
    status                      VARCHAR(20) NOT NULL,
    priority                    VARCHAR(20) NOT NULL,
    is_triggered                BOOLEAN NOT NULL DEFAULT false,
    is_active                   BOOLEAN NOT NULL DEFAULT true,
    is_recurring                BOOLEAN NOT NULL DEFAULT false,
    
    -- Timing
    triggered_at                TIMESTAMP WITHOUT TIME ZONE,
    expires_at                  TIMESTAMP WITHOUT TIME ZONE,
    last_checked_at             TIMESTAMP WITHOUT TIME ZONE,
    next_check_at               TIMESTAMP WITHOUT TIME ZONE,
    
    -- Trigger details
    triggered_price             NUMERIC(15,6),
    triggered_volume            BIGINT,
    trigger_context             JSONB,              -- Market context at trigger time
    trigger_reason              TEXT,               -- Human-readable trigger explanation
    
    -- Notification settings
    notification_method         VARCHAR(20) NOT NULL,
    notification_settings       JSONB,              -- Notification preferences
    email_sent                  BOOLEAN NOT NULL DEFAULT false,
    sms_sent                    BOOLEAN NOT NULL DEFAULT false,
    push_sent                   BOOLEAN NOT NULL DEFAULT false,
    notification_attempts       INTEGER NOT NULL DEFAULT 0,
    
    -- Performance tracking
    times_triggered             INTEGER NOT NULL DEFAULT 0,
    false_positives             INTEGER NOT NULL DEFAULT 0,
    accuracy_score              NUMERIC(8,4),       -- 0-100 accuracy rating
    average_response_time_ms    BIGINT,             -- Avg time from trigger to notification
    
    -- Market context (current state)
    market_price                NUMERIC(15,6),      -- Last known market price
    market_volume               BIGINT,             -- Last known volume
    last_price_update           TIMESTAMP WITHOUT TIME ZONE,
    market_indicators           JSONB,              -- Technical indicators
    
    -- Audit fields
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                     BIGINT NOT NULL DEFAULT 0
);

-- Performance Indexes
CREATE INDEX idx_price_alerts_user ON price_alerts(user_id);
CREATE INDEX idx_price_alerts_symbol ON price_alerts(symbol);
CREATE INDEX idx_price_alerts_status ON price_alerts(status);
CREATE INDEX idx_price_alerts_type ON price_alerts(alert_type);
CREATE INDEX idx_price_alerts_triggered ON price_alerts(is_triggered);
CREATE INDEX idx_price_alerts_expiry ON price_alerts(expires_at);
CREATE INDEX idx_price_alerts_priority ON price_alerts(priority);

-- Composite indexes for common queries
CREATE INDEX idx_price_alerts_user_symbol ON price_alerts(user_id, symbol);
CREATE INDEX idx_price_alerts_user_status ON price_alerts(user_id, status);
CREATE INDEX idx_price_alerts_symbol_active ON price_alerts(symbol, is_active) WHERE is_active = true;
CREATE INDEX idx_price_alerts_next_check ON price_alerts(next_check_at) WHERE is_active = true AND is_triggered = false;

-- GIN indexes for JSON columns
CREATE INDEX idx_price_alerts_multi_conditions ON price_alerts USING GIN(multi_conditions);
CREATE INDEX idx_price_alerts_trigger_context ON price_alerts USING GIN(trigger_context);
CREATE INDEX idx_price_alerts_market_indicators ON price_alerts USING GIN(market_indicators);

-- Partial indexes for performance
CREATE INDEX idx_price_alerts_active_monitoring ON price_alerts(next_check_at, priority) 
    WHERE status = 'ACTIVE' AND is_active = true AND is_triggered = false 
    AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP);

CREATE INDEX idx_price_alerts_due_for_check ON price_alerts(priority, symbol) 
    WHERE is_active = true AND is_triggered = false 
    AND (next_check_at IS NULL OR next_check_at <= CURRENT_TIMESTAMP);

CREATE INDEX idx_price_alerts_high_priority ON price_alerts(next_check_at) 
    WHERE priority IN ('URGENT', 'CRITICAL') AND is_active = true;

-- Add CHECK constraints for data validation
ALTER TABLE price_alerts ADD CONSTRAINT chk_alert_type 
    CHECK (alert_type IN ('PRICE_TARGET', 'PRICE_RANGE', 'PERCENTAGE_CHANGE', 'VOLUME_SPIKE', 
                         'MA_CROSSOVER', 'RSI_THRESHOLD', 'VOLATILITY_SPIKE', 'SUPPORT_RESISTANCE', 
                         'PATTERN_RECOGNITION', 'NEWS_IMPACT', 'MULTI_CONDITION', 'CUSTOM'));

ALTER TABLE price_alerts ADD CONSTRAINT chk_trigger_condition 
    CHECK (trigger_condition IN ('GREATER_THAN', 'LESS_THAN', 'EQUALS', 'BETWEEN', 'OUTSIDE_RANGE',
                                'PERCENTAGE_UP', 'PERCENTAGE_DOWN', 'CROSSES_ABOVE', 'CROSSES_BELOW',
                                'MA_GOLDEN_CROSS', 'MA_DEATH_CROSS', 'RSI_OVERBOUGHT', 'RSI_OVERSOLD',
                                'VOLUME_BREAKOUT', 'VOLATILITY_EXPANSION', 'CUSTOM_LOGIC'));

ALTER TABLE price_alerts ADD CONSTRAINT chk_status 
    CHECK (status IN ('ACTIVE', 'TRIGGERED', 'EXPIRED', 'CANCELLED', 'PAUSED', 'ERROR', 'PENDING'));

ALTER TABLE price_alerts ADD CONSTRAINT chk_priority 
    CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT', 'CRITICAL'));

ALTER TABLE price_alerts ADD CONSTRAINT chk_notification_method 
    CHECK (notification_method IN ('EMAIL', 'SMS', 'PUSH', 'WEBHOOK', 'IN_APP', 'MULTIPLE', 'NONE'));

ALTER TABLE price_alerts ADD CONSTRAINT chk_percentage_range 
    CHECK (percentage_change IS NULL OR (percentage_change >= 0 AND percentage_change <= 1000));

ALTER TABLE price_alerts ADD CONSTRAINT chk_rsi_range 
    CHECK (rsi_threshold IS NULL OR (rsi_threshold >= 0 AND rsi_threshold <= 100));

ALTER TABLE price_alerts ADD CONSTRAINT chk_ma_period 
    CHECK (moving_average_period IS NULL OR (moving_average_period > 0 AND moving_average_period <= 500));

ALTER TABLE price_alerts ADD CONSTRAINT chk_accuracy_score 
    CHECK (accuracy_score IS NULL OR (accuracy_score >= 0 AND accuracy_score <= 100));

-- Add NOT NULL constraints for required fields based on alert type
-- Note: These would ideally be conditional constraints, but PostgreSQL doesn't support them directly
-- Application layer should enforce these rules

-- Add table comment
COMMENT ON TABLE price_alerts IS 'User-defined price alerts with intelligent triggering and advanced notification management';

-- Add column comments for clarity
COMMENT ON COLUMN price_alerts.alert_type IS 'Type of alert: PRICE_TARGET, PRICE_RANGE, PERCENTAGE_CHANGE, etc.';
COMMENT ON COLUMN price_alerts.trigger_condition IS 'Condition logic: GREATER_THAN, LESS_THAN, BETWEEN, etc.';
COMMENT ON COLUMN price_alerts.status IS 'Alert status: ACTIVE, TRIGGERED, EXPIRED, CANCELLED, PAUSED, ERROR, PENDING';
COMMENT ON COLUMN price_alerts.priority IS 'Processing priority: LOW, NORMAL, HIGH, URGENT, CRITICAL';
COMMENT ON COLUMN price_alerts.is_recurring IS 'Whether alert resets after triggering for repeated use';
COMMENT ON COLUMN price_alerts.multi_conditions IS 'JSON array of complex multi-condition alert definitions';
COMMENT ON COLUMN price_alerts.trigger_context IS 'JSON snapshot of market conditions when alert triggered';
COMMENT ON COLUMN price_alerts.accuracy_score IS 'Historical accuracy score (0-100) based on past performance';

-- Create trigger for updating updated_at
CREATE TRIGGER price_alerts_update_timestamp
    BEFORE UPDATE ON price_alerts
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified();