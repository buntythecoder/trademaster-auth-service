-- =====================================================
-- TradeMaster Market Data Service
-- Migration V6: Seed Data
-- Description: Insert sample data for development and testing
-- Author: Database Administrator
-- Version: 1.0.0
-- =====================================================

-- =====================================================
-- SAMPLE CHART DATA
-- =====================================================

-- Insert sample daily chart data for popular Indian stocks
INSERT INTO chart_data (
    symbol, exchange, timeframe, timestamp, 
    open, high, low, close, volume,
    sma20, sma50, rsi, is_complete, data_source
) VALUES 
    -- RELIANCE daily data
    ('RELIANCE', 'NSE', 'D1', '2024-08-29 15:30:00', 2450.50, 2485.75, 2435.20, 2470.30, 2500000, 2465.50, 2455.80, 58.5, true, 'NSE_API'),
    ('RELIANCE', 'NSE', 'D1', '2024-08-28 15:30:00', 2420.80, 2455.90, 2410.25, 2450.50, 2200000, 2460.20, 2450.30, 55.2, true, 'NSE_API'),
    ('RELIANCE', 'NSE', 'D1', '2024-08-27 15:30:00', 2395.60, 2430.40, 2380.15, 2420.80, 1900000, 2455.75, 2445.60, 52.8, true, 'NSE_API'),
    
    -- TCS daily data
    ('TCS', 'NSE', 'D1', '2024-08-29 15:30:00', 3850.25, 3885.60, 3830.40, 3875.80, 1200000, 3860.40, 3845.20, 62.3, true, 'NSE_API'),
    ('TCS', 'NSE', 'D1', '2024-08-28 15:30:00', 3820.90, 3860.30, 3805.70, 3850.25, 1100000, 3855.80, 3840.90, 59.7, true, 'NSE_API'),
    ('TCS', 'NSE', 'D1', '2024-08-27 15:30:00', 3795.40, 3830.85, 3780.20, 3820.90, 950000, 3850.60, 3835.40, 57.1, true, 'NSE_API'),
    
    -- INFY daily data
    ('INFY', 'NSE', 'D1', '2024-08-29 15:30:00', 1825.75, 1845.30, 1815.60, 1838.90, 1800000, 1830.25, 1825.80, 65.8, true, 'NSE_API'),
    ('INFY', 'NSE', 'D1', '2024-08-28 15:30:00', 1810.40, 1835.20, 1805.90, 1825.75, 1650000, 1825.90, 1820.40, 63.2, true, 'NSE_API'),
    ('INFY', 'NSE', 'D1', '2024-08-27 15:30:00', 1795.80, 1820.60, 1790.30, 1810.40, 1500000, 1820.75, 1815.90, 60.5, true, 'NSE_API'),
    
    -- NIFTY index data
    ('NIFTY', 'NSE', 'D1', '2024-08-29 15:30:00', 25150.50, 25245.80, 25100.20, 25230.75, 0, 25180.30, 25120.60, 61.2, true, 'NSE_API'),
    ('NIFTY', 'NSE', 'D1', '2024-08-28 15:30:00', 25080.90, 25180.40, 25020.60, 25150.50, 0, 25165.80, 25105.90, 58.9, true, 'NSE_API'),
    ('NIFTY', 'NSE', 'D1', '2024-08-27 15:30:00', 25010.40, 25120.70, 24980.90, 25080.90, 0, 25150.40, 25090.30, 56.4, true, 'NSE_API');

-- Insert some intraday 1-hour data for real-time testing
INSERT INTO chart_data (
    symbol, exchange, timeframe, timestamp, 
    open, high, low, close, volume,
    is_complete, data_source
) VALUES 
    ('RELIANCE', 'NSE', 'H1', '2024-08-30 14:00:00', 2470.30, 2478.60, 2465.80, 2475.20, 180000, false, 'NSE_REALTIME'),
    ('RELIANCE', 'NSE', 'H1', '2024-08-30 13:00:00', 2465.80, 2475.90, 2460.40, 2470.30, 165000, true, 'NSE_REALTIME'),
    ('RELIANCE', 'NSE', 'H1', '2024-08-30 12:00:00', 2472.40, 2480.20, 2465.50, 2465.80, 142000, true, 'NSE_REALTIME'),
    
    ('TCS', 'NSE', 'H1', '2024-08-30 14:00:00', 3875.80, 3885.40, 3870.20, 3882.50, 95000, false, 'NSE_REALTIME'),
    ('TCS', 'NSE', 'H1', '2024-08-30 13:00:00', 3880.20, 3890.60, 3872.40, 3875.80, 88000, true, 'NSE_REALTIME'),
    ('TCS', 'NSE', 'H1', '2024-08-30 12:00:00', 3885.60, 3895.80, 3878.90, 3880.20, 82000, true, 'NSE_REALTIME');

-- =====================================================
-- SAMPLE MARKET NEWS DATA
-- =====================================================

INSERT INTO market_news (
    news_id, title, summary, content, source, author, published_at, url,
    category, sub_category, region, tags, related_symbols, related_sectors,
    sentiment_score, sentiment_label, confidence_score, relevance_score, impact_score, market_impact,
    urgency_score, is_trending, is_breaking_news, is_market_moving,
    word_count, reading_time_minutes, quality_score, is_verified, data_provider
) VALUES 
    (
        'news_001_reliance_q1_earnings',
        'Reliance Industries Reports Strong Q1 FY25 Earnings, Beats Estimates',
        'Reliance Industries posted consolidated net profit of ₹18,951 crore for Q1 FY25, up 12.3% YoY, beating analyst estimates of ₹17,800 crore.',
        'Reliance Industries Limited (RIL) on Friday reported a consolidated net profit of ₹18,951 crore for the first quarter ended June 30, 2024, marking a 12.3% increase from ₹16,878 crore in the same period last year. The result exceeded analyst estimates of ₹17,800 crore. Revenue from operations stood at ₹2,35,122 crore, up 8.5% YoY...',
        'Economic Times',
        'Rajesh Sharma',
        '2024-08-30 10:30:00',
        'https://economictimes.indiatimes.com/reliance-q1-earnings',
        'EARNINGS',
        'Quarterly Results',
        'INDIA',
        '["earnings", "quarterly", "reliance", "beat estimates", "petrochemicals", "retail"]',
        '["RELIANCE"]',
        '["Energy", "Petrochemicals", "Retail"]',
        0.75,
        'POSITIVE',
        0.88,
        92.5,
        85.0,
        'HIGH',
        78.0,
        true,
        false,
        true,
        650,
        3,
        0.92,
        true,
        'NewsAPI'
    ),
    (
        'news_002_rbi_policy_decision',
        'RBI Keeps Repo Rate Unchanged at 6.5%, Maintains Stance on Withdrawal of Accommodation',
        'The Reserve Bank of India maintained the repo rate at 6.5% for the ninth consecutive time, focusing on bringing inflation closer to the 4% target.',
        'The Reserve Bank of India (RBI) on Thursday kept the benchmark repo rate unchanged at 6.5% for the ninth consecutive meeting. The six-member Monetary Policy Committee (MPC) also decided to remain focused on withdrawal of accommodation to ensure that inflation progressively aligns with the target while supporting growth...',
        'Business Standard',
        'Anup Roy',
        '2024-08-29 16:45:00',
        'https://business-standard.com/rbi-policy-decision',
        'ECONOMY',
        'Monetary Policy',
        'INDIA',
        '["rbi", "repo rate", "monetary policy", "inflation", "accommodation withdrawal"]',
        '["NIFTY", "BANKNIFTY", "RELIANCE", "TCS", "INFY"]',
        '["Banking", "Financial Services", "All Sectors"]',
        0.15,
        'SLIGHTLY_POSITIVE',
        0.82,
        98.0,
        95.0,
        'CRITICAL',
        90.0,
        true,
        true,
        true,
        820,
        4,
        0.95,
        true,
        'Reuters'
    ),
    (
        'news_003_it_sector_outlook',
        'IT Sector Shows Signs of Recovery as Deal Pipeline Improves: Analysts',
        'Leading IT companies report improvement in client conversations and deal pipeline, signaling potential recovery in the sector after a challenging period.',
        'The Indian IT sector is showing early signs of recovery with companies reporting improved client conversations and a stronger deal pipeline. Industry analysts believe that the worst may be behind the sector as enterprises resume their digital transformation initiatives...',
        'Mint',
        'Debasis Mohapatra',
        '2024-08-28 14:20:00',
        'https://livemint.com/it-sector-recovery-outlook',
        'TECHNOLOGY',
        'IT Services',
        'INDIA',
        '["IT sector", "recovery", "digital transformation", "deal pipeline", "client conversations"]',
        '["TCS", "INFY", "WIPRO", "HCL"]',
        '["Information Technology", "Software Services"]',
        0.45,
        'POSITIVE',
        0.75,
        88.0,
        75.0,
        'HIGH',
        65.0,
        false,
        false,
        true,
        480,
        2,
        0.85,
        true,
        'NewsAPI'
    );

-- =====================================================
-- SAMPLE ECONOMIC EVENTS DATA
-- =====================================================

INSERT INTO economic_events (
    event_id, title, description, country, category, importance, event_date, frequency, unit,
    forecast_value, previous_value, status, source, data_provider,
    market_impact_score, expected_sentiment, related_symbols, related_sectors
) VALUES 
    (
        'event_001_india_gdp_q1',
        'India GDP Growth Rate (Q1 FY25)',
        'Quarterly Gross Domestic Product growth rate for India, measuring the economic expansion compared to the previous quarter and year.',
        'IND',
        'GDP',
        'CRITICAL',
        '2024-08-31 17:30:00',
        'Quarterly',
        '%',
        6.8,
        6.1,
        'SCHEDULED',
        'Ministry of Statistics and Programme Implementation',
        'TradingEconomics',
        95.0,
        'BULLISH',
        '["NIFTY", "BANKNIFTY", "RELIANCE", "TCS", "INFY"]',
        '["All Sectors"]'
    ),
    (
        'event_002_india_inflation_cpi',
        'India Consumer Price Index (CPI) - August 2024',
        'Monthly Consumer Price Index measuring inflation rate for essential goods and services consumed by households.',
        'IND',
        'Inflation',
        'HIGH',
        '2024-09-12 17:30:00',
        'Monthly',
        '%',
        4.2,
        3.54,
        'SCHEDULED',
        'National Sample Survey Office',
        'TradingEconomics',
        85.0,
        'BEARISH',
        '["NIFTY", "BANKNIFTY"]',
        '["Banking", "Consumer Goods", "FMCG"]'
    ),
    (
        'event_003_usa_fed_decision',
        'Federal Reserve Interest Rate Decision',
        'Federal Open Market Committee (FOMC) decision on the federal funds rate, impacting global markets and capital flows.',
        'USA',
        'Interest Rates',
        'CRITICAL',
        '2024-09-18 18:00:00',
        'Every 6-8 weeks',
        '%',
        5.25,
        5.50,
        'SCHEDULED',
        'Federal Reserve',
        'TradingEconomics',
        98.0,
        'BULLISH',
        '["NIFTY", "BANKNIFTY", "RELIANCE", "TCS", "INFY"]',
        '["All Sectors"]'
    ),
    (
        'event_004_india_manufacturing_pmi',
        'India Manufacturing PMI - August 2024',
        'Purchasing Managers Index for the manufacturing sector, indicating business conditions and economic health.',
        'IND',
        'Manufacturing',
        'MEDIUM',
        '2024-09-02 09:30:00',
        'Monthly',
        'Index',
        57.8,
        58.1,
        'SCHEDULED',
        'IHS Markit',
        'TradingEconomics',
        65.0,
        'NEUTRAL',
        '["NIFTY", "RELIANCE"]',
        '["Manufacturing", "Industrial"]'
    );

-- =====================================================
-- SAMPLE PRICE ALERTS DATA
-- =====================================================

INSERT INTO price_alerts (
    user_id, symbol, exchange, name, description, alert_type, trigger_condition,
    target_price, baseline_price, percentage_change, status, priority,
    notification_method, is_active, expires_at, next_check_at
) VALUES 
    (
        'user_12345',
        'RELIANCE',
        'NSE',
        'RELIANCE Breakout Alert',
        'Alert when RELIANCE breaks above resistance level of 2500',
        'PRICE_TARGET',
        'GREATER_THAN',
        2500.00,
        2470.30,
        NULL,
        'ACTIVE',
        'HIGH',
        'PUSH',
        true,
        '2024-09-30 23:59:59',
        '2024-08-30 15:00:00'
    ),
    (
        'user_12345',
        'TCS',
        'NSE',
        'TCS Support Alert',
        'Alert when TCS falls below support level of 3800',
        'PRICE_TARGET',
        'LESS_THAN',
        3800.00,
        3875.80,
        NULL,
        'ACTIVE',
        'NORMAL',
        'EMAIL',
        true,
        '2024-09-15 23:59:59',
        '2024-08-30 15:30:00'
    ),
    (
        'user_67890',
        'INFY',
        'NSE',
        'INFY 5% Gain Alert',
        'Alert when INFY gains 5% from current price',
        'PERCENTAGE_CHANGE',
        'PERCENTAGE_UP',
        NULL,
        1838.90,
        5.0,
        'ACTIVE',
        'NORMAL',
        'SMS',
        true,
        '2024-09-10 23:59:59',
        '2024-08-30 16:00:00'
    ),
    (
        'user_67890',
        'NIFTY',
        'NSE',
        'NIFTY RSI Oversold',
        'Alert when NIFTY RSI drops below 30 (oversold condition)',
        'RSI_THRESHOLD',
        'RSI_OVERSOLD',
        NULL,
        NULL,
        NULL,
        'ACTIVE',
        'URGENT',
        'MULTIPLE',
        true,
        NULL, -- No expiry for this alert
        '2024-08-30 15:15:00'
    ),
    (
        'user_11111',
        'RELIANCE',
        'NSE',
        'RELIANCE High Volume Alert',
        'Alert when RELIANCE volume exceeds 3 million shares',
        'VOLUME_SPIKE',
        'VOLUME_BREAKOUT',
        NULL,
        NULL,
        NULL,
        'ACTIVE',
        'LOW',
        'IN_APP',
        true,
        '2024-12-31 23:59:59',
        '2024-08-30 15:45:00'
    );

-- Update the volume_threshold for the volume alert
UPDATE price_alerts 
SET volume_threshold = 3000000 
WHERE alert_type = 'VOLUME_SPIKE' AND user_id = 'user_11111';

-- Update the rsi_threshold for the RSI alert
UPDATE price_alerts 
SET rsi_threshold = 30 
WHERE alert_type = 'RSI_THRESHOLD' AND user_id = 'user_67890';

-- =====================================================
-- UPDATE STATISTICS
-- =====================================================

-- Update table statistics after inserting seed data
ANALYZE chart_data;
ANALYZE market_news;
ANALYZE economic_events;
ANALYZE price_alerts;

-- Log the seed data insertion
INSERT INTO chart_data_cleanup_log (cleanup_date, records_deleted, cleanup_type) 
VALUES (CURRENT_TIMESTAMP, 0, 'seed_data_inserted');

-- Add comments
COMMENT ON TABLE chart_data IS 'Time-series OHLCV market data with technical indicators - includes sample data for RELIANCE, TCS, INFY, NIFTY';
COMMENT ON TABLE market_news IS 'Market news with sentiment analysis - includes sample earnings, policy, and sector news';
COMMENT ON TABLE economic_events IS 'Economic calendar events - includes sample GDP, inflation, and policy events';
COMMENT ON TABLE price_alerts IS 'User price alerts - includes sample breakout, support, percentage, RSI, and volume alerts';