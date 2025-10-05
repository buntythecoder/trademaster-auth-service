-- V12: Convert PostgreSQL INET columns to VARCHAR for Hibernate 6.x compatibility
-- This migration fixes remaining type compatibility issues with INET types

-- Update User Devices table
ALTER TABLE user_devices
ALTER COLUMN ip_address TYPE VARCHAR(45) USING COALESCE(CAST(ip_address AS VARCHAR), '');

-- Update Security Audit Logs table
ALTER TABLE security_audit_logs
ALTER COLUMN ip_address TYPE VARCHAR(45) USING COALESCE(CAST(ip_address AS VARCHAR), '');

-- Update User Sessions table
ALTER TABLE user_sessions
ALTER COLUMN ip_address TYPE VARCHAR(45) USING COALESCE(CAST(ip_address AS VARCHAR), '');

-- Add comments for the new format
COMMENT ON COLUMN user_devices.ip_address IS 'IP address as string (converted from INET for Hibernate compatibility)';
COMMENT ON COLUMN security_audit_logs.ip_address IS 'IP address as string (converted from INET for Hibernate compatibility)';
COMMENT ON COLUMN user_sessions.ip_address IS 'IP address as string (converted from INET for Hibernate compatibility)';

-- Update any existing data to ensure proper format (should be handled by USING clause but ensure consistency)
UPDATE user_devices SET ip_address = '' WHERE ip_address IS NULL;
UPDATE security_audit_logs SET ip_address = '' WHERE ip_address IS NULL;
UPDATE user_sessions SET ip_address = '' WHERE ip_address IS NULL;