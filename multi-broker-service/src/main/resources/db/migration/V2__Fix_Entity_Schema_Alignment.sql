-- Multi-Broker Service Database Schema Alignment Fix
-- Version: V2 - Fix critical entity vs migration schema alignment issues
-- Author: TradeMaster Development Team
-- Date: 2024-12-01
--
-- MANDATORY: Fix 6 critical schema alignment issues identified in analysis
-- PRIORITY: High - Required for entity-migration compatibility

-- Add missing columns that exist in BrokerConnection entity but not in migration
ALTER TABLE broker_connections 
ADD COLUMN connected_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN disconnected_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN last_synced TIMESTAMP WITH TIME ZONE,
ADD COLUMN sync_count BIGINT DEFAULT 0 CHECK (sync_count >= 0),
ADD COLUMN error_count BIGINT DEFAULT 0 CHECK (error_count >= 0),
ADD COLUMN is_healthy BOOLEAN DEFAULT true;

-- Add indexes for new columns to support entity business logic queries
CREATE INDEX idx_broker_connections_connected_at ON broker_connections(connected_at);
CREATE INDEX idx_broker_connections_disconnected_at ON broker_connections(disconnected_at);
CREATE INDEX idx_broker_connections_last_synced ON broker_connections(last_synced);
CREATE INDEX idx_broker_connections_sync_count ON broker_connections(sync_count);
CREATE INDEX idx_broker_connections_error_count ON broker_connections(error_count);
CREATE INDEX idx_broker_connections_is_healthy ON broker_connections(is_healthy);

-- Add composite indexes for common query patterns used by entity business logic
CREATE INDEX idx_broker_connections_user_status_healthy 
ON broker_connections(user_id, status, is_healthy);

CREATE INDEX idx_broker_connections_broker_healthy 
ON broker_connections(broker_type, is_healthy);

CREATE INDEX idx_broker_connections_health_monitoring 
ON broker_connections(last_health_check, consecutive_failures, is_healthy);

-- Update existing connections to set default values for new columns
-- Set connected_at to created_at for existing CONNECTED records
UPDATE broker_connections 
SET connected_at = created_at 
WHERE status = 'CONNECTED' AND connected_at IS NULL;

-- Set last_synced to last_successful_call for existing records with successful calls
UPDATE broker_connections 
SET last_synced = last_successful_call 
WHERE last_successful_call IS NOT NULL AND last_synced IS NULL;

-- Initialize sync_count and error_count based on existing data patterns
UPDATE broker_connections 
SET sync_count = CASE 
    WHEN last_successful_call IS NOT NULL THEN 1 
    ELSE 0 
END,
error_count = consecutive_failures
WHERE sync_count = 0 OR error_count = 0;

-- Set is_healthy based on current status and consecutive failures
UPDATE broker_connections 
SET is_healthy = CASE 
    WHEN status IN ('CONNECTED', 'CONNECTING') AND consecutive_failures < 3 THEN true
    WHEN status IN ('ERROR', 'SUSPENDED', 'RATE_LIMITED') THEN false
    WHEN consecutive_failures >= 5 THEN false
    ELSE true
END
WHERE is_healthy IS NULL;

-- Add trigger to automatically update is_healthy based on status changes
CREATE OR REPLACE FUNCTION update_broker_connection_health()
RETURNS TRIGGER AS $$
BEGIN
    -- Automatically update is_healthy based on status and failure count
    NEW.is_healthy = CASE 
        WHEN NEW.status IN ('CONNECTED', 'CONNECTING') AND COALESCE(NEW.consecutive_failures, 0) < 3 THEN true
        WHEN NEW.status IN ('ERROR', 'SUSPENDED', 'RATE_LIMITED') THEN false
        WHEN COALESCE(NEW.consecutive_failures, 0) >= 5 THEN false
        ELSE COALESCE(NEW.is_healthy, true)
    END;
    
    -- Update error_count when consecutive_failures changes
    IF NEW.consecutive_failures IS DISTINCT FROM OLD.consecutive_failures THEN
        NEW.error_count = COALESCE(OLD.error_count, 0) + 
            CASE WHEN NEW.consecutive_failures > COALESCE(OLD.consecutive_failures, 0) THEN 1 ELSE 0 END;
    END IF;
    
    -- Update sync_count on successful calls
    IF NEW.last_successful_call IS DISTINCT FROM OLD.last_successful_call 
       AND NEW.last_successful_call IS NOT NULL THEN
        NEW.sync_count = COALESCE(OLD.sync_count, 0) + 1;
        NEW.last_synced = NEW.last_successful_call;
    END IF;
    
    -- Set connected_at when status changes to CONNECTED
    IF NEW.status = 'CONNECTED' AND OLD.status != 'CONNECTED' THEN
        NEW.connected_at = CURRENT_TIMESTAMP;
    END IF;
    
    -- Set disconnected_at when status changes from CONNECTED to something else
    IF OLD.status = 'CONNECTED' AND NEW.status != 'CONNECTED' THEN
        NEW.disconnected_at = CURRENT_TIMESTAMP;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- Create trigger for automatic health and status management
DROP TRIGGER IF EXISTS update_broker_connection_health_trigger ON broker_connections;
CREATE TRIGGER update_broker_connection_health_trigger
    BEFORE UPDATE ON broker_connections
    FOR EACH ROW
    EXECUTE FUNCTION update_broker_connection_health();

-- Add comprehensive check constraints to ensure data integrity
ALTER TABLE broker_connections 
ADD CONSTRAINT chk_connected_disconnected_logic 
    CHECK (connected_at IS NULL OR disconnected_at IS NULL OR connected_at <= disconnected_at),
ADD CONSTRAINT chk_sync_count_non_negative 
    CHECK (sync_count >= 0),
ADD CONSTRAINT chk_error_count_non_negative 
    CHECK (error_count >= 0),
ADD CONSTRAINT chk_healthy_status_logic 
    CHECK (
        (is_healthy = true AND status NOT IN ('ERROR', 'SUSPENDED')) OR
        (is_healthy = false) OR
        (is_healthy IS NULL)
    );

-- Update function to handle new fields in health monitoring
CREATE OR REPLACE FUNCTION broker_connection_health_summary()
RETURNS TABLE(
    broker_type VARCHAR(20),
    total_connections BIGINT,
    healthy_connections BIGINT,
    avg_sync_count NUMERIC,
    avg_error_count NUMERIC,
    last_health_check TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        bc.broker_type,
        COUNT(*)::BIGINT as total_connections,
        COUNT(CASE WHEN bc.is_healthy = true THEN 1 END)::BIGINT as healthy_connections,
        AVG(bc.sync_count) as avg_sync_count,
        AVG(bc.error_count) as avg_error_count,
        MAX(bc.last_health_check) as last_health_check
    FROM broker_connections bc
    GROUP BY bc.broker_type
    ORDER BY bc.broker_type;
END;
$$ LANGUAGE 'plpgsql';

-- Add comments for new columns
COMMENT ON COLUMN broker_connections.connected_at IS 'Timestamp when connection was established';
COMMENT ON COLUMN broker_connections.disconnected_at IS 'Timestamp when connection was terminated';
COMMENT ON COLUMN broker_connections.last_synced IS 'Timestamp of last successful portfolio synchronization';
COMMENT ON COLUMN broker_connections.sync_count IS 'Total number of successful portfolio synchronizations';
COMMENT ON COLUMN broker_connections.error_count IS 'Total number of errors encountered';
COMMENT ON COLUMN broker_connections.is_healthy IS 'Boolean health status derived from status and failure count';

-- Verify schema alignment by creating a test view
CREATE OR REPLACE VIEW broker_connection_schema_verification AS
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'broker_connections' 
    AND table_schema = 'public'
ORDER BY ordinal_position;

-- Create function to validate entity-migration alignment
CREATE OR REPLACE FUNCTION validate_broker_connection_schema()
RETURNS TABLE(
    validation_result TEXT,
    column_name TEXT,
    status TEXT
) AS $$
BEGIN
    -- Check all required entity columns exist
    RETURN QUERY
    SELECT 
        'Entity Column Check'::TEXT as validation_result,
        unnest(ARRAY[
            'id', 'user_id', 'broker_type', 'account_id', 'display_name',
            'access_token_encrypted', 'refresh_token_encrypted', 'token_expires_at',
            'status', 'last_successful_call', 'last_health_check', 
            'connected_at', 'disconnected_at', 'last_synced',
            'sync_count', 'error_count', 'is_healthy', 'consecutive_failures',
            'error_message', 'capabilities', 'connection_config',
            'api_calls_today', 'last_api_call', 'rate_limit_reset_at',
            'created_at', 'updated_at', 'version'
        ]) as column_name,
        CASE 
            WHEN column_name IN (
                SELECT c.column_name 
                FROM information_schema.columns c
                WHERE c.table_name = 'broker_connections' 
                    AND c.table_schema = 'public'
            ) THEN 'EXISTS'
            ELSE 'MISSING'
        END as status;
END;
$$ LANGUAGE 'plpgsql';

-- Run validation and display results
DO $$
DECLARE
    missing_columns INTEGER;
BEGIN
    SELECT COUNT(*) INTO missing_columns
    FROM validate_broker_connection_schema()
    WHERE status = 'MISSING';
    
    IF missing_columns > 0 THEN
        RAISE NOTICE 'Schema validation failed: % missing columns', missing_columns;
    ELSE
        RAISE NOTICE 'Schema validation successful: All entity columns exist in database';
    END IF;
END;
$$;