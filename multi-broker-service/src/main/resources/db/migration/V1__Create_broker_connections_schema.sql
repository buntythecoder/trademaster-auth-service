-- Multi-Broker Service Database Schema
-- Version: V1 - Initial schema for broker connections and portfolio aggregation
-- Author: TradeMaster Development Team
-- Date: 2024-12-01
--
-- MANDATORY: PostgreSQL 15+ with JSONB support
-- MANDATORY: UUID extension for broker connection IDs
-- MANDATORY: Encrypted token storage with AES-256

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgcrypto for encryption functions (if needed)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create broker connections table
-- Stores encrypted OAuth tokens and connection metadata for each user-broker pair
CREATE TABLE broker_connections (
    -- Primary key and identification
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(50) NOT NULL,
    broker_type VARCHAR(20) NOT NULL CHECK (broker_type IN ('ZERODHA', 'UPSTOX', 'ANGEL_ONE', 'ICICI_DIRECT', 'FYERS', 'IIFL')),
    account_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    
    -- OAuth token storage (AES-256 encrypted)
    -- SECURITY: Never store plain text tokens, always encrypted
    access_token_encrypted TEXT,
    refresh_token_encrypted TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Connection status and health monitoring
    status VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED' CHECK (status IN ('CONNECTED', 'CONNECTING', 'DISCONNECTED', 'TOKEN_EXPIRED', 'RATE_LIMITED', 'MAINTENANCE', 'ERROR', 'SUSPENDED')),
    last_successful_call TIMESTAMP WITH TIME ZONE,
    last_health_check TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    consecutive_failures INTEGER DEFAULT 0 CHECK (consecutive_failures >= 0),
    error_message TEXT,
    
    -- Broker capabilities and configuration (JSONB for PostgreSQL optimization)
    capabilities JSONB,
    connection_config JSONB,
    
    -- Rate limiting tracking
    api_calls_today BIGINT DEFAULT 0 CHECK (api_calls_today >= 0),
    last_api_call TIMESTAMP WITH TIME ZONE,
    rate_limit_reset_at TIMESTAMP WITH TIME ZONE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    
    -- Constraints
    CONSTRAINT uk_user_broker_account UNIQUE (user_id, broker_type, account_id)
);

-- Create consolidated positions table
-- Stores aggregated position data across brokers for faster portfolio retrieval
CREATE TABLE consolidated_positions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(200),
    
    -- Position details
    total_quantity BIGINT NOT NULL CHECK (total_quantity > 0),
    avg_price DECIMAL(12,4) NOT NULL CHECK (avg_price > 0),
    current_price DECIMAL(12,4) CHECK (current_price > 0),
    
    -- Financial calculations
    total_cost DECIMAL(15,2) NOT NULL CHECK (total_cost > 0),
    current_value DECIMAL(15,2) CHECK (current_value > 0),
    unrealized_pnl DECIMAL(15,2),
    unrealized_pnl_percent DECIMAL(8,4),
    day_change DECIMAL(15,2),
    day_change_percent DECIMAL(8,4),
    
    -- Metadata
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    data_source VARCHAR(50) DEFAULT 'AGGREGATION',
    
    -- Constraints
    CONSTRAINT uk_user_symbol UNIQUE (user_id, symbol)
);

-- Create broker position breakdown table
-- Tracks individual broker contributions to each consolidated position
CREATE TABLE broker_position_breakdown (
    id BIGSERIAL PRIMARY KEY,
    consolidated_position_id BIGINT NOT NULL,
    broker_connection_id UUID NOT NULL,
    
    -- Position details for this broker
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    avg_price DECIMAL(12,4) NOT NULL CHECK (avg_price > 0),
    current_value DECIMAL(15,2) NOT NULL CHECK (current_value > 0),
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_synced TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_consolidated_position FOREIGN KEY (consolidated_position_id) REFERENCES consolidated_positions(id) ON DELETE CASCADE,
    CONSTRAINT fk_broker_connection FOREIGN KEY (broker_connection_id) REFERENCES broker_connections(id) ON DELETE CASCADE
);

-- Create OAuth states table for security
-- Stores temporary OAuth state parameters to prevent CSRF attacks
CREATE TABLE oauth_states (
    state VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    redirect_uri TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create broker health logs table
-- Stores health check results and API call monitoring data
CREATE TABLE broker_health_logs (
    id BIGSERIAL PRIMARY KEY,
    broker_connection_id UUID NOT NULL,
    
    -- Health check details
    check_type VARCHAR(20) NOT NULL CHECK (check_type IN ('API_CALL', 'PORTFOLIO_SYNC', 'TOKEN_REFRESH', 'HEARTBEAT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILURE', 'TIMEOUT', 'RATE_LIMITED')),
    response_time_ms INTEGER CHECK (response_time_ms >= 0),
    error_message TEXT,
    
    -- Additional context
    api_endpoint VARCHAR(200),
    request_id VARCHAR(100),
    
    -- Timestamp
    checked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_broker_connection_health FOREIGN KEY (broker_connection_id) REFERENCES broker_connections(id) ON DELETE CASCADE
);

-- Create portfolio cache table
-- Caches aggregated portfolio data for fast retrieval
CREATE TABLE portfolio_cache (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    
    -- Portfolio summary (JSONB for complex data structure)
    portfolio_data JSONB NOT NULL,
    
    -- Cache metadata
    cache_key VARCHAR(200) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT uk_user_cache_key UNIQUE (user_id, cache_key)
);

-- Create indexes for optimal query performance
-- Broker connections indexes
CREATE INDEX idx_broker_connections_user_id ON broker_connections(user_id);
CREATE INDEX idx_broker_connections_status ON broker_connections(status);
CREATE INDEX idx_broker_connections_broker_type ON broker_connections(broker_type);
CREATE INDEX idx_broker_connections_health_check ON broker_connections(last_health_check);
CREATE INDEX idx_broker_connections_token_expiry ON broker_connections(token_expires_at);
CREATE INDEX idx_broker_connections_rate_limit ON broker_connections(rate_limit_reset_at) WHERE rate_limit_reset_at IS NOT NULL;

-- Consolidated positions indexes
CREATE INDEX idx_consolidated_positions_user_id ON consolidated_positions(user_id);
CREATE INDEX idx_consolidated_positions_symbol ON consolidated_positions(symbol);
CREATE INDEX idx_consolidated_positions_last_updated ON consolidated_positions(last_updated);
CREATE INDEX idx_consolidated_positions_pnl ON consolidated_positions(unrealized_pnl_percent);

-- Broker position breakdown indexes
CREATE INDEX idx_broker_breakdown_consolidated_id ON broker_position_breakdown(consolidated_position_id);
CREATE INDEX idx_broker_breakdown_connection_id ON broker_position_breakdown(broker_connection_id);

-- OAuth states indexes
CREATE INDEX idx_oauth_states_expires_at ON oauth_states(expires_at);
CREATE INDEX idx_oauth_states_user_id ON oauth_states(user_id);

-- Health logs indexes
CREATE INDEX idx_health_logs_connection_id ON broker_health_logs(broker_connection_id);
CREATE INDEX idx_health_logs_checked_at ON broker_health_logs(checked_at);
CREATE INDEX idx_health_logs_status ON broker_health_logs(status);
CREATE INDEX idx_health_logs_check_type ON broker_health_logs(check_type);

-- Portfolio cache indexes
CREATE INDEX idx_portfolio_cache_user_id ON portfolio_cache(user_id);
CREATE INDEX idx_portfolio_cache_expires_at ON portfolio_cache(expires_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_broker_connections_updated_at 
    BEFORE UPDATE ON broker_connections 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_consolidated_positions_last_updated
    BEFORE UPDATE ON consolidated_positions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to clean up expired OAuth states
CREATE OR REPLACE FUNCTION cleanup_expired_oauth_states()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM oauth_states 
    WHERE expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE 'plpgsql';

-- Create function to clean up old health logs (keep last 30 days)
CREATE OR REPLACE FUNCTION cleanup_old_health_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM broker_health_logs 
    WHERE checked_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE 'plpgsql';

-- Create function to reset daily API call counters
CREATE OR REPLACE FUNCTION reset_daily_api_counters()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE broker_connections 
    SET api_calls_today = 0,
        rate_limit_reset_at = NULL
    WHERE api_calls_today > 0;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE 'plpgsql';

-- Add comments for documentation
COMMENT ON TABLE broker_connections IS 'Stores encrypted OAuth connections between users and brokers with health monitoring';
COMMENT ON TABLE consolidated_positions IS 'Aggregated position data across all brokers for fast portfolio retrieval';
COMMENT ON TABLE broker_position_breakdown IS 'Individual broker contributions to consolidated positions';
COMMENT ON TABLE oauth_states IS 'Temporary OAuth state parameters for CSRF protection';
COMMENT ON TABLE broker_health_logs IS 'Health check results and API monitoring data';
COMMENT ON TABLE portfolio_cache IS 'Cached aggregated portfolio data for performance';

COMMENT ON COLUMN broker_connections.access_token_encrypted IS 'AES-256 encrypted OAuth access token - NEVER store in plain text';
COMMENT ON COLUMN broker_connections.refresh_token_encrypted IS 'AES-256 encrypted OAuth refresh token - NEVER store in plain text';
COMMENT ON COLUMN broker_connections.capabilities IS 'JSONB: Broker-specific capabilities and limits';
COMMENT ON COLUMN broker_connections.connection_config IS 'JSONB: OAuth client configuration and settings';
COMMENT ON COLUMN broker_connections.consecutive_failures IS 'Counter for consecutive API failures - resets on success';

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO trademaster_api;
-- GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO trademaster_api;