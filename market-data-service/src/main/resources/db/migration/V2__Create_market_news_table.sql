-- =====================================================
-- TradeMaster Market Data Service
-- Migration V2: Market News Table
-- Description: Create market_news table for news articles with sentiment analysis and market impact
-- Author: Database Administrator
-- Version: 1.0.0
-- =====================================================

-- Create market_news table
CREATE TABLE market_news (
    -- Primary Key
    id                     BIGSERIAL PRIMARY KEY,
    
    -- External identifier (unique)
    news_id                VARCHAR(255) NOT NULL UNIQUE,
    
    -- Content fields
    title                  VARCHAR(1000) NOT NULL,
    summary                TEXT,
    content                TEXT,
    source                 VARCHAR(255) NOT NULL,
    author                 VARCHAR(255),
    
    -- Timing
    published_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITHOUT TIME ZONE,
    
    -- URLs
    url                    VARCHAR(2000) NOT NULL,
    image_url              VARCHAR(2000),
    
    -- Categorization
    category               VARCHAR(100) NOT NULL, -- EARNINGS, ECONOMY, POLITICS, TECHNOLOGY, etc.
    sub_category           VARCHAR(100),           -- Quarterly Results, GDP, Elections, AI, etc.
    region                 VARCHAR(50),            -- GLOBAL, INDIA, US, ASIA, EUROPE, etc.
    tags                   JSONB,                  -- Array of tags
    
    -- Market relevance (JSON arrays for flexibility)
    related_symbols        JSONB,                  -- Array of stock symbols
    related_sectors        JSONB,                  -- Array of sectors
    related_currencies     JSONB,                  -- Array of currencies
    related_commodities    JSONB,                  -- Array of commodities
    
    -- Sentiment Analysis
    sentiment_score        NUMERIC(5,4),           -- -1.0 (very negative) to 1.0 (very positive)
    sentiment_label        VARCHAR(20),            -- VERY_NEGATIVE, NEGATIVE, etc.
    confidence_score       NUMERIC(5,4),           -- 0.0 to 1.0 confidence in sentiment
    
    -- Impact and relevance scoring
    relevance_score        NUMERIC(5,2),           -- 0-100 relevance to markets
    impact_score           NUMERIC(5,2),           -- 0-100 potential market impact
    market_impact          VARCHAR(20),            -- MINIMAL, LOW, MODERATE, HIGH, CRITICAL
    urgency_score          NUMERIC(5,2),           -- 0-100 time sensitivity
    
    -- Engagement metrics
    view_count             BIGINT DEFAULT 0,
    share_count            BIGINT DEFAULT 0,
    comment_count          BIGINT DEFAULT 0,
    is_trending            BOOLEAN DEFAULT false,
    is_breaking_news       BOOLEAN DEFAULT false,
    is_market_moving       BOOLEAN DEFAULT false,
    
    -- Content analysis
    word_count             INTEGER,
    reading_time_minutes   INTEGER,
    key_phrases            JSONB,                  -- Array of extracted key phrases
    named_entities         JSONB,                  -- Array of companies, people, places
    
    -- Data quality
    quality_score          NUMERIC(3,2),           -- 0.0 to 1.0 content quality score
    is_verified            BOOLEAN DEFAULT false,  -- Source verification status
    is_duplicate           BOOLEAN DEFAULT false,  -- Duplicate detection flag
    duplicate_group        VARCHAR(100),           -- Group ID for duplicate articles
    
    -- Processing metadata
    data_provider          VARCHAR(100),           -- NewsAPI, Alpha Vantage, etc.
    processed_at           TIMESTAMP WITHOUT TIME ZONE,
    processing_version     VARCHAR(50),
    metadata               JSONB,                  -- Additional processing metadata
    
    -- Audit fields
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                BIGINT NOT NULL DEFAULT 0
);

-- Performance Indexes
CREATE INDEX idx_market_news_published_at ON market_news(published_at DESC);
CREATE INDEX idx_market_news_sentiment ON market_news(sentiment_score);
CREATE INDEX idx_market_news_relevance ON market_news(relevance_score DESC);
CREATE INDEX idx_market_news_category ON market_news(category);
CREATE INDEX idx_market_news_source ON market_news(source);
CREATE INDEX idx_market_news_trending ON market_news(is_trending) WHERE is_trending = true;

-- GIN indexes for JSON columns for efficient array operations
CREATE INDEX idx_market_news_related_symbols ON market_news USING GIN(related_symbols);
CREATE INDEX idx_market_news_related_sectors ON market_news USING GIN(related_sectors);
CREATE INDEX idx_market_news_tags ON market_news USING GIN(tags);
CREATE INDEX idx_market_news_key_phrases ON market_news USING GIN(key_phrases);

-- Composite indexes for common queries
CREATE INDEX idx_market_news_category_published ON market_news(category, published_at DESC);
CREATE INDEX idx_market_news_impact_published ON market_news(market_impact, published_at DESC);
CREATE INDEX idx_market_news_breaking_published ON market_news(is_breaking_news, published_at DESC) WHERE is_breaking_news = true;

-- Partial indexes for performance
CREATE INDEX idx_market_news_high_impact ON market_news(published_at DESC) WHERE impact_score >= 70;
CREATE INDEX idx_market_news_recent_trending ON market_news(relevance_score DESC) 
    WHERE published_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours' AND is_trending = true;

-- Add table comment
COMMENT ON TABLE market_news IS 'Market news articles with sentiment analysis, relevance scoring, and impact assessment';

-- Add column comments for clarity
COMMENT ON COLUMN market_news.sentiment_score IS 'Sentiment score from -1.0 (very negative) to 1.0 (very positive)';
COMMENT ON COLUMN market_news.sentiment_label IS 'VERY_NEGATIVE, NEGATIVE, SLIGHTLY_NEGATIVE, NEUTRAL, SLIGHTLY_POSITIVE, POSITIVE, VERY_POSITIVE';
COMMENT ON COLUMN market_news.market_impact IS 'MINIMAL, LOW, MODERATE, HIGH, CRITICAL';
COMMENT ON COLUMN market_news.related_symbols IS 'JSON array of stock symbols affected by this news';
COMMENT ON COLUMN market_news.tags IS 'JSON array of content tags for categorization';

-- Create trigger for updating last_modified
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER market_news_update_last_modified
    BEFORE UPDATE ON market_news
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified();