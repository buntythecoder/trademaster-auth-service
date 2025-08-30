-- =====================================================
-- TradeMaster Market Data Service
-- Migration V5: Additional Indexes and Constraints
-- Description: Create additional performance indexes and data integrity constraints
-- Author: Database Administrator
-- Version: 1.0.0
-- =====================================================

-- =====================================================
-- ADDITIONAL PERFORMANCE OPTIMIZATIONS
-- =====================================================

-- Chart Data: Additional indexes for time-series queries
CREATE INDEX CONCURRENTLY idx_chart_data_volume_high ON chart_data(volume DESC) 
    WHERE volume > 1000000; -- High volume candles

CREATE INDEX CONCURRENTLY idx_chart_data_volatility ON chart_data(symbol, volatility DESC) 
    WHERE volatility IS NOT NULL AND volatility > 0.1; -- High volatility periods

CREATE INDEX CONCURRENTLY idx_chart_data_recent_active ON chart_data(symbol, timeframe, timestamp DESC) 
    WHERE timestamp >= CURRENT_DATE - INTERVAL '30 days'; -- Recent active data

-- Market News: Additional indexes for content queries
CREATE INDEX CONCURRENTLY idx_market_news_quality_recent ON market_news(quality_score DESC, published_at DESC) 
    WHERE quality_score >= 0.8 AND published_at >= CURRENT_TIMESTAMP - INTERVAL '7 days';

CREATE INDEX CONCURRENTLY idx_market_news_engagement ON market_news(
    (COALESCE(view_count, 0) + COALESCE(share_count, 0) * 5 + COALESCE(comment_count, 0) * 3) DESC
) WHERE is_trending = true;

-- Economic Events: Additional indexes for calendar queries
CREATE INDEX CONCURRENTLY idx_economic_events_next_week ON economic_events(importance, event_date) 
    WHERE event_date >= CURRENT_DATE AND event_date <= CURRENT_DATE + INTERVAL '7 days';

CREATE INDEX CONCURRENTLY idx_economic_events_global_impact ON economic_events(event_date, market_impact_score DESC) 
    WHERE country IN ('USA', 'CHN', 'EUR') OR importance = 'CRITICAL';

-- Price Alerts: Additional indexes for monitoring queries
CREATE INDEX CONCURRENTLY idx_price_alerts_monitoring_queue ON price_alerts(
    CASE priority 
        WHEN 'CRITICAL' THEN 1
        WHEN 'URGENT' THEN 2
        WHEN 'HIGH' THEN 3
        WHEN 'NORMAL' THEN 4
        WHEN 'LOW' THEN 5
    END,
    next_check_at NULLS FIRST
) WHERE is_active = true AND is_triggered = false;

-- =====================================================
-- DATA INTEGRITY CONSTRAINTS
-- =====================================================

-- Chart Data: Business logic constraints
ALTER TABLE chart_data ADD CONSTRAINT chk_chart_data_ohlc_logical 
    CHECK (low <= high AND low <= open AND low <= close AND open <= high AND close <= high);

ALTER TABLE chart_data ADD CONSTRAINT chk_chart_data_volume_positive 
    CHECK (volume >= 0);

ALTER TABLE chart_data ADD CONSTRAINT chk_chart_data_rsi_range 
    CHECK (rsi IS NULL OR (rsi >= 0 AND rsi <= 100));

ALTER TABLE chart_data ADD CONSTRAINT chk_chart_data_stoch_range 
    CHECK ((stoch_k IS NULL OR (stoch_k >= 0 AND stoch_k <= 100)) AND 
           (stoch_d IS NULL OR (stoch_d >= 0 AND stoch_d <= 100)));

ALTER TABLE chart_data ADD CONSTRAINT chk_chart_data_williams_r_range 
    CHECK (williams_r IS NULL OR (williams_r >= -100 AND williams_r <= 0));

-- Market News: Content validation constraints
ALTER TABLE market_news ADD CONSTRAINT chk_market_news_scores_range 
    CHECK ((sentiment_score IS NULL OR (sentiment_score >= -1 AND sentiment_score <= 1)) AND
           (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)) AND
           (relevance_score IS NULL OR (relevance_score >= 0 AND relevance_score <= 100)) AND
           (impact_score IS NULL OR (impact_score >= 0 AND impact_score <= 100)) AND
           (urgency_score IS NULL OR (urgency_score >= 0 AND urgency_score <= 100)) AND
           (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 1)));

ALTER TABLE market_news ADD CONSTRAINT chk_market_news_engagement_positive 
    CHECK (COALESCE(view_count, 0) >= 0 AND 
           COALESCE(share_count, 0) >= 0 AND 
           COALESCE(comment_count, 0) >= 0);

ALTER TABLE market_news ADD CONSTRAINT chk_market_news_word_count_positive 
    CHECK (word_count IS NULL OR word_count > 0);

ALTER TABLE market_news ADD CONSTRAINT chk_market_news_reading_time_positive 
    CHECK (reading_time_minutes IS NULL OR reading_time_minutes > 0);

-- Economic Events: Value validation constraints
ALTER TABLE economic_events ADD CONSTRAINT chk_economic_events_dates_logical 
    CHECK (release_time IS NULL OR release_time >= event_date);

-- Price Alerts: Price validation constraints
ALTER TABLE price_alerts ADD CONSTRAINT chk_price_alerts_prices_positive 
    CHECK ((target_price IS NULL OR target_price > 0) AND
           (stop_price IS NULL OR stop_price > 0) AND
           (baseline_price IS NULL OR baseline_price > 0) AND
           (moving_average_price IS NULL OR moving_average_price > 0) AND
           (volume_threshold IS NULL OR volume_threshold > 0) AND
           (triggered_price IS NULL OR triggered_price > 0) AND
           (market_price IS NULL OR market_price > 0));

ALTER TABLE price_alerts ADD CONSTRAINT chk_price_alerts_counters_positive 
    CHECK (times_triggered >= 0 AND false_positives >= 0 AND notification_attempts >= 0);

ALTER TABLE price_alerts ADD CONSTRAINT chk_price_alerts_response_time_positive 
    CHECK (average_response_time_ms IS NULL OR average_response_time_ms >= 0);

-- =====================================================
-- TABLE STATISTICS AND MAINTENANCE
-- =====================================================

-- Update table statistics for better query planning
ANALYZE chart_data;
ANALYZE market_news;
ANALYZE economic_events;
ANALYZE price_alerts;

-- =====================================================
-- STORED PROCEDURES FOR MAINTENANCE
-- =====================================================

-- Function to clean up old chart data (keep last 2 years for intraday, 10 years for daily+)
CREATE OR REPLACE FUNCTION cleanup_old_chart_data()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER := 0;
BEGIN
    -- Delete old intraday data (older than 2 years)
    DELETE FROM chart_data 
    WHERE timeframe IN ('M1', 'M5', 'M15', 'M30', 'H1', 'H4') 
    AND timestamp < CURRENT_TIMESTAMP - INTERVAL '2 years';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log the cleanup
    INSERT INTO chart_data_cleanup_log(cleanup_date, records_deleted, cleanup_type) 
    VALUES (CURRENT_TIMESTAMP, deleted_count, 'intraday_cleanup');
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up old market news (keep last 1 year)
CREATE OR REPLACE FUNCTION cleanup_old_market_news()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER := 0;
BEGIN
    DELETE FROM market_news 
    WHERE published_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up expired and old price alerts
CREATE OR REPLACE FUNCTION cleanup_old_price_alerts()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER := 0;
BEGIN
    -- Delete expired alerts older than 30 days
    DELETE FROM price_alerts 
    WHERE (status = 'EXPIRED' OR status = 'CANCELLED') 
    AND updated_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
    
    -- Delete old triggered alerts (non-recurring) older than 90 days
    DELETE FROM price_alerts 
    WHERE status = 'TRIGGERED' 
    AND is_recurring = false 
    AND triggered_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- MAINTENANCE LOG TABLE
-- =====================================================

CREATE TABLE chart_data_cleanup_log (
    id              BIGSERIAL PRIMARY KEY,
    cleanup_date    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    records_deleted INTEGER NOT NULL,
    cleanup_type    VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PERFORMANCE MONITORING VIEWS
-- =====================================================

-- View for monitoring chart data volume and performance
CREATE VIEW chart_data_stats AS
SELECT 
    symbol,
    timeframe,
    COUNT(*) as total_records,
    MIN(timestamp) as earliest_data,
    MAX(timestamp) as latest_data,
    AVG(volume) as avg_volume,
    COUNT(*) FILTER (WHERE is_complete = false) as incomplete_records,
    COUNT(*) FILTER (WHERE has_gaps = true) as gap_records
FROM chart_data
GROUP BY symbol, timeframe
ORDER BY symbol, timeframe;

-- View for monitoring alert performance
CREATE VIEW price_alerts_performance AS
SELECT 
    alert_type,
    priority,
    COUNT(*) as total_alerts,
    COUNT(*) FILTER (WHERE is_active = true) as active_alerts,
    COUNT(*) FILTER (WHERE is_triggered = true) as triggered_alerts,
    AVG(times_triggered) as avg_triggers,
    AVG(accuracy_score) as avg_accuracy,
    AVG(average_response_time_ms) as avg_response_time_ms
FROM price_alerts
GROUP BY alert_type, priority
ORDER BY alert_type, priority;

-- Add comments for the new objects
COMMENT ON FUNCTION cleanup_old_chart_data() IS 'Maintenance function to remove old intraday chart data older than 2 years';
COMMENT ON FUNCTION cleanup_old_market_news() IS 'Maintenance function to remove market news older than 1 year';
COMMENT ON FUNCTION cleanup_old_price_alerts() IS 'Maintenance function to remove expired and old price alerts';
COMMENT ON VIEW chart_data_stats IS 'Performance monitoring view for chart data volume and quality metrics';
COMMENT ON VIEW price_alerts_performance IS 'Performance monitoring view for price alert effectiveness and response times';