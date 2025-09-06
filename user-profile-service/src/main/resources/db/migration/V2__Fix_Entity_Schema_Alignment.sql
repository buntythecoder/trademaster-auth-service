-- Fix Entity Schema Alignment Migration
-- Migration V2: Align database schema with actual entity definitions
-- This migration addresses discrepancies between entities and V1 migration

-- Add missing correlation_id column to profile_audit_logs
ALTER TABLE profile_audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);

-- Create index for correlation_id for better query performance
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id ON profile_audit_logs (correlation_id);

-- Drop and recreate user_preferences table to match UserPreferences entity structure
DROP TABLE IF EXISTS user_preferences CASCADE;

-- Create user_preferences table matching UserPreferences entity
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    
    -- Basic preferences
    theme VARCHAR(20) NOT NULL DEFAULT 'auto',
    language VARCHAR(5) NOT NULL DEFAULT 'en',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Notification preferences
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    trading_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    market_news BOOLEAN NOT NULL DEFAULT TRUE,
    price_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Dashboard and UI preferences
    dashboard_layout VARCHAR(50) NOT NULL DEFAULT 'default',
    chart_type VARCHAR(30) NOT NULL DEFAULT 'candlestick',
    default_time_frame VARCHAR(10) NOT NULL DEFAULT '1d',
    show_portfolio_performance BOOLEAN NOT NULL DEFAULT TRUE,
    show_market_overview BOOLEAN NOT NULL DEFAULT TRUE,
    show_watchlist BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Trading preferences
    default_order_type VARCHAR(20) NOT NULL DEFAULT 'limit',
    confirmation_dialogs BOOLEAN NOT NULL DEFAULT TRUE,
    risk_warnings BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Privacy preferences
    profile_visibility VARCHAR(20) NOT NULL DEFAULT 'private',
    show_trading_history BOOLEAN NOT NULL DEFAULT FALSE,
    allow_analytics BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Security preferences
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    session_timeout INTEGER NOT NULL DEFAULT 3600,
    
    -- Versioning and timestamps
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Ensure one-to-one relationship with user_profiles
    CONSTRAINT user_preferences_user_profile_unique UNIQUE (user_profile_id)
);

-- Create user_preference_data table for advanced preferences (ElementCollection mapping)
CREATE TABLE user_preference_data (
    user_preferences_id UUID NOT NULL REFERENCES user_preferences(id) ON DELETE CASCADE,
    preference_key VARCHAR(100) NOT NULL,
    preference_value VARCHAR(1000),
    
    PRIMARY KEY (user_preferences_id, preference_key)
);

-- Create indexes for user_preferences
CREATE INDEX idx_user_preferences_profile_id ON user_preferences (user_profile_id);
CREATE INDEX idx_user_preferences_theme ON user_preferences (theme);
CREATE INDEX idx_user_preferences_language ON user_preferences (language);
CREATE INDEX idx_user_preferences_created_at ON user_preferences (created_at);

-- Create indexes for user_preference_data
CREATE INDEX idx_user_preference_data_key ON user_preference_data (preference_key);

-- Update trigger for user_preferences updated_at
CREATE TRIGGER update_user_preferences_updated_at 
    BEFORE UPDATE ON user_preferences 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Update audit log constraints to match ChangeType and EntityType enums
ALTER TABLE profile_audit_logs DROP CONSTRAINT IF EXISTS audit_logs_change_type_check;
ALTER TABLE profile_audit_logs ADD CONSTRAINT audit_logs_change_type_check 
    CHECK (change_type IN (
        'CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'SECURITY_UPDATE', 'SYSTEM_UPDATE',
        'LOGIN', 'LOGOUT', 'PASSWORD_CHANGE', 'TWO_FACTOR_ENABLE', 'TWO_FACTOR_DISABLE',
        'PROFILE_CREATE', 'PROFILE_UPDATE', 'PROFILE_ACTIVATE', 'PROFILE_DEACTIVATE', 'PROFILE_DELETE',
        'KYC_SUBMIT', 'KYC_VERIFY', 'KYC_APPROVE', 'KYC_REJECT', 'KYC_EXPIRE',
        'DOCUMENT_UPLOAD', 'DOCUMENT_UPDATE', 'DOCUMENT_DELETE', 'DOCUMENT_VERIFY', 'DOCUMENT_APPROVE', 'DOCUMENT_REJECT',
        'PREFERENCES_UPDATE', 'NOTIFICATION_SETTINGS_UPDATE', 'PRIVACY_SETTINGS_UPDATE', 'TRADING_SETTINGS_UPDATE',
        'RISK_PROFILE_UPDATE', 'COMPLIANCE_CHECK', 'SUSPICIOUS_ACTIVITY',
        'DATA_EXPORT', 'DATA_IMPORT', 'DATA_MIGRATION', 'DATA_BACKUP',
        'SYSTEM_MAINTENANCE', 'SYSTEM_UPGRADE', 'SYSTEM_ERROR', 'SYSTEM_RECOVERY'
    ));

ALTER TABLE profile_audit_logs DROP CONSTRAINT IF EXISTS audit_logs_entity_type_check;
ALTER TABLE profile_audit_logs ADD CONSTRAINT audit_logs_entity_type_check 
    CHECK (entity_type IN (
        'USER_PROFILE', 'USER_PREFERENCES', 'USER_DOCUMENT', 'KYC_INFORMATION',
        'NOTIFICATION_SETTINGS', 'TRADING_PREFERENCES', 'SUBSCRIPTION', 'AUDIT_LOG'
    ));

-- Update user_documents verification_status constraint to match VerificationStatus enum
ALTER TABLE user_documents DROP CONSTRAINT IF EXISTS user_documents_verification_status_check;
ALTER TABLE user_documents ADD CONSTRAINT user_documents_verification_status_check 
    CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED'));

-- Clean up unused tables that don't match current entity structure
-- Note: Keeping trading_profiles and kyc_verifications as they might be used by other services
-- or represent future entity implementations

-- Add comments for documentation
COMMENT ON TABLE user_preferences IS 'User preferences matching UserPreferences entity with specific columns for each preference type';
COMMENT ON TABLE user_preference_data IS 'Advanced user preferences stored as key-value pairs using ElementCollection mapping';
COMMENT ON COLUMN profile_audit_logs.correlation_id IS 'Correlation ID for tracing related audit events across the system';

-- Insert default preferences for existing user profiles (if any)
INSERT INTO user_preferences (user_profile_id, theme, language, timezone, currency)
SELECT id, 'auto', 'en', 'UTC', 'USD' 
FROM user_profiles
WHERE NOT EXISTS (SELECT 1 FROM user_preferences WHERE user_profile_id = user_profiles.id);