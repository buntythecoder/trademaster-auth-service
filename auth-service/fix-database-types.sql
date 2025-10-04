-- Fix database type inconsistencies for user_id columns
-- Convert all VARCHAR user_id columns to BIGINT to match entity expectations

-- First, drop constraints that depend on these columns
ALTER TABLE mfa_configuration DROP CONSTRAINT IF EXISTS unique_user_mfa_type;
ALTER TABLE device_settings DROP CONSTRAINT IF EXISTS device_settings_user_id_fkey;
ALTER TABLE mfa_configuration DROP CONSTRAINT IF EXISTS mfa_configuration_user_id_fkey;
ALTER TABLE security_audit_logs DROP CONSTRAINT IF EXISTS security_audit_logs_user_id_fkey;
ALTER TABLE session_settings DROP CONSTRAINT IF EXISTS session_settings_user_id_fkey;
ALTER TABLE user_devices DROP CONSTRAINT IF EXISTS user_devices_user_id_fkey;
ALTER TABLE user_sessions DROP CONSTRAINT IF EXISTS user_sessions_user_id_fkey;

-- Convert VARCHAR columns to BIGINT
ALTER TABLE device_settings ALTER COLUMN user_id TYPE BIGINT USING CASE WHEN user_id ~ '^[0-9]+$' THEN user_id::BIGINT ELSE NULL END;
ALTER TABLE mfa_configuration ALTER COLUMN user_id TYPE BIGINT USING CASE WHEN user_id ~ '^[0-9]+$' THEN user_id::BIGINT ELSE NULL END;
ALTER TABLE security_audit_logs ALTER COLUMN user_id TYPE BIGINT USING CASE WHEN user_id ~ '^[0-9]+$' THEN user_id::BIGINT ELSE NULL END;
ALTER TABLE session_settings ALTER COLUMN user_id TYPE BIGINT USING CASE WHEN user_id ~ '^[0-9]+$' THEN user_id::BIGINT ELSE NULL END;
ALTER TABLE user_devices ALTER COLUMN user_id TYPE BIGINT USING CASE WHEN user_id ~ '^[0-9]+$' THEN user_id::BIGINT ELSE NULL END;
ALTER TABLE user_sessions ALTER COLUMN user_id TYPE BIGINT USING CASE WHEN user_id ~ '^[0-9]+$' THEN user_id::BIGINT ELSE NULL END;

-- Re-add the unique constraint for mfa_configuration
ALTER TABLE mfa_configuration ADD CONSTRAINT unique_user_mfa_type UNIQUE (user_id, mfa_type);

-- Note: Foreign key constraints will be re-added by Hibernate if needed