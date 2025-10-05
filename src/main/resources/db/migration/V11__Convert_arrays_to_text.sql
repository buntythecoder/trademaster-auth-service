-- V11: Convert PostgreSQL array columns to TEXT columns for Hibernate 6.x compatibility
-- This migration fixes the ClassCastException by removing all array types from the database schema

-- Update MFA Configuration table
ALTER TABLE mfa_configuration
ALTER COLUMN backup_codes TYPE TEXT USING COALESCE(array_to_string(backup_codes, ','), '');

-- Update Device Settings table
ALTER TABLE device_settings
ALTER COLUMN blocked_devices TYPE TEXT USING COALESCE(array_to_string(blocked_devices, ','), '');

-- Add comments for the new format
COMMENT ON COLUMN mfa_configuration.backup_codes IS 'Comma-separated backup codes for TOTP (converted from array)';
COMMENT ON COLUMN device_settings.blocked_devices IS 'Comma-separated blocked device fingerprints (converted from array)';

-- Update any existing data to ensure proper format
UPDATE mfa_configuration SET backup_codes = '' WHERE backup_codes IS NULL;
UPDATE device_settings SET blocked_devices = '' WHERE blocked_devices IS NULL;