-- =====================================================
-- TradeMaster Market Data Service
-- Migration V7: Schema Column Name Alignment
-- Description: Fix JPA entity to database column mapping mismatches
-- Author: Database Administrator  
-- Version: 1.0.0
-- Critical: Resolves camelCase vs snake_case mapping failures
-- =====================================================

-- =====================================================
-- CHART_DATA TABLE COLUMN ALIGNMENT
-- =====================================================
-- Fix 16 column name mismatches in chart_data table

-- MACD indicators
ALTER TABLE chart_data RENAME COLUMN macd_signal TO macdSignal;
ALTER TABLE chart_data RENAME COLUMN macd_histogram TO macdHistogram;

-- Bollinger Bands
ALTER TABLE chart_data RENAME COLUMN bollinger_upper TO bollingerUpper;
ALTER TABLE chart_data RENAME COLUMN bollinger_middle TO bollingerMiddle;
ALTER TABLE chart_data RENAME COLUMN bollinger_lower TO bollingerLower;

-- Adjusted data fields
ALTER TABLE chart_data RENAME COLUMN adjusted_close TO adjustedClose;
ALTER TABLE chart_data RENAME COLUMN split_coefficient TO splitCoefficient;
ALTER TABLE chart_data RENAME COLUMN dividend_amount TO dividendAmount;

-- Stochastic indicators
ALTER TABLE chart_data RENAME COLUMN stoch_k TO stochK;
ALTER TABLE chart_data RENAME COLUMN stoch_d TO stochD;

-- Volume and volatility indicators
ALTER TABLE chart_data RENAME COLUMN williams_r TO williamsR;
ALTER TABLE chart_data RENAME COLUMN trade_count TO tradeCount;

-- Data quality fields
ALTER TABLE chart_data RENAME COLUMN is_complete TO isComplete;
ALTER TABLE chart_data RENAME COLUMN has_gaps TO hasGaps;
ALTER TABLE chart_data RENAME COLUMN data_source TO dataSource;
ALTER TABLE chart_data RENAME COLUMN created_at TO createdAt;

-- =====================================================
-- MARKET_NEWS TABLE COLUMN ALIGNMENT  
-- =====================================================
-- Fix 18 column name mismatches in market_news table

-- External identifier
ALTER TABLE market_news RENAME COLUMN news_id TO newsId;

-- Timing fields
ALTER TABLE market_news RENAME COLUMN published_at TO publishedAt;
ALTER TABLE market_news RENAME COLUMN updated_at TO updatedAt;

-- URL fields
ALTER TABLE market_news RENAME COLUMN image_url TO imageUrl;

-- Categorization fields
ALTER TABLE market_news RENAME COLUMN sub_category TO subCategory;

-- Market relevance fields
ALTER TABLE market_news RENAME COLUMN related_symbols TO relatedSymbols;
ALTER TABLE market_news RENAME COLUMN related_sectors TO relatedSectors;
ALTER TABLE market_news RENAME COLUMN related_currencies TO relatedCurrencies;
ALTER TABLE market_news RENAME COLUMN related_commodities TO relatedCommodities;

-- Sentiment analysis fields
ALTER TABLE market_news RENAME COLUMN sentiment_score TO sentimentScore;
ALTER TABLE market_news RENAME COLUMN sentiment_label TO sentimentLabel;
ALTER TABLE market_news RENAME COLUMN confidence_score TO confidenceScore;

-- Impact scoring fields
ALTER TABLE market_news RENAME COLUMN relevance_score TO relevanceScore;
ALTER TABLE market_news RENAME COLUMN impact_score TO impactScore;
ALTER TABLE market_news RENAME COLUMN market_impact TO marketImpact;
ALTER TABLE market_news RENAME COLUMN urgency_score TO urgencyScore;

-- Engagement metrics
ALTER TABLE market_news RENAME COLUMN view_count TO viewCount;
ALTER TABLE market_news RENAME COLUMN share_count TO shareCount;
ALTER TABLE market_news RENAME COLUMN comment_count TO commentCount;
ALTER TABLE market_news RENAME COLUMN is_trending TO isTrending;
ALTER TABLE market_news RENAME COLUMN is_breaking_news TO isBreakingNews;
ALTER TABLE market_news RENAME COLUMN is_market_moving TO isMarketMoving;

-- Content analysis fields
ALTER TABLE market_news RENAME COLUMN word_count TO wordCount;
ALTER TABLE market_news RENAME COLUMN reading_time_minutes TO readingTimeMinutes;
ALTER TABLE market_news RENAME COLUMN key_phrases TO keyPhrases;
ALTER TABLE market_news RENAME COLUMN named_entities TO namedEntities;

-- Data quality fields
ALTER TABLE market_news RENAME COLUMN quality_score TO qualityScore;
ALTER TABLE market_news RENAME COLUMN is_verified TO isVerified;
ALTER TABLE market_news RENAME COLUMN is_duplicate TO isDuplicate;
ALTER TABLE market_news RENAME COLUMN duplicate_group TO duplicateGroup;

-- Processing metadata fields
ALTER TABLE market_news RENAME COLUMN data_provider TO dataProvider;
ALTER TABLE market_news RENAME COLUMN processed_at TO processedAt;
ALTER TABLE market_news RENAME COLUMN processing_version TO processingVersion;

-- Audit fields
ALTER TABLE market_news RENAME COLUMN created_at TO createdAt;
ALTER TABLE market_news RENAME COLUMN last_modified TO lastModified;

-- =====================================================
-- ECONOMIC_EVENTS TABLE COLUMN ALIGNMENT
-- =====================================================
-- Fix 14 column name mismatches in economic_events table

-- External identifier
ALTER TABLE economic_events RENAME COLUMN event_id TO eventId;

-- Timing fields
ALTER TABLE economic_events RENAME COLUMN event_date TO eventDate;

-- Forecast and actual values
ALTER TABLE economic_events RENAME COLUMN forecast_value TO forecastValue;
ALTER TABLE economic_events RENAME COLUMN previous_value TO previousValue;
ALTER TABLE economic_events RENAME COLUMN actual_value TO actualValue;
ALTER TABLE economic_events RENAME COLUMN revision_value TO revisionValue;

-- Market impact analysis
ALTER TABLE economic_events RENAME COLUMN market_impact_score TO marketImpactScore;
ALTER TABLE economic_events RENAME COLUMN expected_sentiment TO expectedSentiment;
ALTER TABLE economic_events RENAME COLUMN actual_sentiment TO actualSentiment;

-- Status and timing
ALTER TABLE economic_events RENAME COLUMN release_time TO releaseTime;
ALTER TABLE economic_events RENAME COLUMN next_release_date TO nextReleaseDate;

-- Data source information
ALTER TABLE economic_events RENAME COLUMN source_url TO sourceUrl;
ALTER TABLE economic_events RENAME COLUMN data_provider TO dataProvider;

-- Additional metadata
ALTER TABLE economic_events RENAME COLUMN related_symbols TO relatedSymbols;
ALTER TABLE economic_events RENAME COLUMN related_sectors TO relatedSectors;
ALTER TABLE economic_events RENAME COLUMN historical_data TO historicalData;
ALTER TABLE economic_events RENAME COLUMN analysis_notes TO analysisNotes;

-- Audit fields  
ALTER TABLE economic_events RENAME COLUMN created_at TO createdAt;
ALTER TABLE economic_events RENAME COLUMN updated_at TO updatedAt;

-- =====================================================
-- UPDATE INDEX REFERENCES
-- =====================================================
-- Update index references to use new column names

-- Drop old indexes that reference renamed columns
DROP INDEX IF EXISTS idx_market_news_published_at;
DROP INDEX IF EXISTS idx_market_news_sentiment;
DROP INDEX IF EXISTS idx_market_news_relevance;
DROP INDEX IF EXISTS idx_market_news_symbols;
DROP INDEX IF EXISTS idx_market_news_trending;
DROP INDEX IF EXISTS idx_economic_events_date;

-- Create new indexes with correct column names
CREATE INDEX idx_market_news_publishedAt ON market_news(publishedAt DESC);
CREATE INDEX idx_market_news_sentimentScore ON market_news(sentimentScore);
CREATE INDEX idx_market_news_relevanceScore ON market_news(relevanceScore DESC);
CREATE INDEX idx_market_news_relatedSymbols ON market_news USING GIN(relatedSymbols);
CREATE INDEX idx_market_news_isTrending ON market_news(isTrending) WHERE isTrending = true;
CREATE INDEX idx_economic_events_eventDate ON economic_events(eventDate);

-- Update composite indexes
DROP INDEX IF EXISTS idx_market_news_category_published;
DROP INDEX IF EXISTS idx_market_news_impact_published;
DROP INDEX IF EXISTS idx_market_news_breaking_published;
DROP INDEX IF EXISTS idx_economic_events_country_date;
DROP INDEX IF EXISTS idx_economic_events_importance_date;
DROP INDEX IF EXISTS idx_economic_events_category_date;

CREATE INDEX idx_market_news_category_publishedAt ON market_news(category, publishedAt DESC);
CREATE INDEX idx_market_news_marketImpact_publishedAt ON market_news(marketImpact, publishedAt DESC);
CREATE INDEX idx_market_news_isBreakingNews_publishedAt ON market_news(isBreakingNews, publishedAt DESC) WHERE isBreakingNews = true;
CREATE INDEX idx_economic_events_country_eventDate ON economic_events(country, eventDate);
CREATE INDEX idx_economic_events_importance_eventDate ON economic_events(importance, eventDate);
CREATE INDEX idx_economic_events_category_eventDate ON economic_events(category, eventDate);

-- Update partial indexes
DROP INDEX IF EXISTS idx_market_news_high_impact;
DROP INDEX IF EXISTS idx_market_news_recent_trending;
DROP INDEX IF EXISTS idx_economic_events_upcoming;
DROP INDEX IF EXISTS idx_economic_events_today;
DROP INDEX IF EXISTS idx_economic_events_high_impact;

CREATE INDEX idx_market_news_high_impactScore ON market_news(publishedAt DESC) WHERE impactScore >= 70;
CREATE INDEX idx_market_news_recent_isTrending ON market_news(relevanceScore DESC) 
    WHERE publishedAt >= CURRENT_TIMESTAMP - INTERVAL '24 hours' AND isTrending = true;
CREATE INDEX idx_economic_events_upcoming_eventDate ON economic_events(eventDate) 
    WHERE eventDate > CURRENT_TIMESTAMP AND eventDate <= CURRENT_TIMESTAMP + INTERVAL '7 days';
CREATE INDEX idx_economic_events_today_eventDate ON economic_events(importance, eventDate) 
    WHERE DATE(eventDate) = CURRENT_DATE;
CREATE INDEX idx_economic_events_high_marketImpactScore ON economic_events(eventDate) 
    WHERE importance IN ('HIGH', 'CRITICAL') OR marketImpactScore >= 60;

-- =====================================================
-- ANALYZE TABLES FOR STATISTICS UPDATE
-- =====================================================
-- Update table statistics after schema changes

ANALYZE chart_data;
ANALYZE market_news;  
ANALYZE economic_events;

-- =====================================================
-- VALIDATION QUERIES
-- =====================================================
-- Verify schema alignment (comment out for production)

-- Verify chart_data columns
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'chart_data' AND column_name IN ('macdSignal', 'adjustedClose', 'isComplete');

-- Verify market_news columns  
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'market_news' AND column_name IN ('publishedAt', 'sentimentScore', 'isTrending');

-- Verify economic_events columns
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'economic_events' AND column_name IN ('eventDate', 'marketImpactScore', 'releaseTime');

-- Add completion log
COMMENT ON TABLE chart_data IS 'Time-series OHLCV market data with technical indicators - JPA mapping aligned';
COMMENT ON TABLE market_news IS 'Market news with sentiment analysis - JPA mapping aligned'; 
COMMENT ON TABLE economic_events IS 'Economic calendar events - JPA mapping aligned';

-- Log successful migration
INSERT INTO chart_data_cleanup_log (cleanup_date, records_deleted, cleanup_type) 
VALUES (CURRENT_TIMESTAMP, 0, 'schema_alignment_v7_completed');