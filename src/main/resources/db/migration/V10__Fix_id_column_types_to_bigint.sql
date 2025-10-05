-- TradeMaster Authentication Service - Fix ID Column Types
-- Version: 10.0
-- Description: Fix SERIAL (INTEGER) columns to BIGINT to match JPA entity expectations

-- Fix auth_audit_log.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE auth_audit_log ALTER COLUMN id TYPE BIGINT;

-- Fix security_events.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE security_events ALTER COLUMN id TYPE BIGINT;

-- Fix rate_limit_violations.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE rate_limit_violations ALTER COLUMN id TYPE BIGINT;

-- Fix user_sessions.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE user_sessions ALTER COLUMN id TYPE BIGINT;

-- Fix compliance_reports.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE compliance_reports ALTER COLUMN id TYPE BIGINT;

-- Fix api_access_log.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE api_access_log ALTER COLUMN id TYPE BIGINT;

-- Fix data_retention_policies.id column type from SERIAL (INTEGER) to BIGINT
ALTER TABLE data_retention_policies ALTER COLUMN id TYPE BIGINT;

-- Check if users table exists and has SERIAL id, fix if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'id'
        AND data_type = 'integer'
    ) THEN
        ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
    END IF;
END $$;

-- Check other audit tables that might have SERIAL columns
DO $$
BEGIN
    -- Fix user_roles table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_roles') THEN
        ALTER TABLE user_roles ALTER COLUMN id TYPE BIGINT;
    END IF;

    -- Fix user_role_assignments table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_role_assignments') THEN
        ALTER TABLE user_role_assignments ALTER COLUMN id TYPE BIGINT;
    END IF;

    -- Fix mfa_configurations table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'mfa_configurations') THEN
        ALTER TABLE mfa_configurations ALTER COLUMN id TYPE BIGINT;
    END IF;

    -- Fix verification_tokens table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'verification_tokens') THEN
        ALTER TABLE verification_tokens ALTER COLUMN id TYPE BIGINT;
    END IF;

    -- Fix user_profiles table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_profiles') THEN
        ALTER TABLE user_profiles ALTER COLUMN id TYPE BIGINT;
    END IF;
END $$;

-- Update foreign key references that may need adjustment
-- Fix user_id references in audit tables
ALTER TABLE auth_audit_log ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE security_events ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE rate_limit_violations ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE user_sessions ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE api_access_log ALTER COLUMN user_id TYPE BIGINT;

-- Add comments for this migration
COMMENT ON TABLE auth_audit_log IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';
COMMENT ON TABLE security_events IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';
COMMENT ON TABLE rate_limit_violations IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';
COMMENT ON TABLE user_sessions IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';
COMMENT ON TABLE compliance_reports IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';
COMMENT ON TABLE api_access_log IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';
COMMENT ON TABLE data_retention_policies IS 'Fixed ID column types to BIGINT for JPA compatibility - Migration V10';