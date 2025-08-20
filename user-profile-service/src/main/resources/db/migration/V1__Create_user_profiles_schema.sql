-- Create user profiles database schema
-- Migration V1: Initial schema for user profile service

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create user_profiles table
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    personal_info JSONB NOT NULL,
    trading_preferences JSONB NOT NULL,
    kyc_information JSONB NOT NULL,
    notification_settings JSONB NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT user_profiles_user_id_unique UNIQUE (user_id)
);

-- Create indexes for user_profiles
CREATE INDEX idx_user_profiles_user_id ON user_profiles (user_id);
CREATE INDEX idx_user_profiles_created_at ON user_profiles (created_at);
CREATE INDEX idx_user_profiles_updated_at ON user_profiles (updated_at);

-- Create GIN indexes for JSONB columns for efficient querying
CREATE INDEX idx_user_profiles_personal_info_gin ON user_profiles USING GIN (personal_info);
CREATE INDEX idx_user_profiles_trading_preferences_gin ON user_profiles USING GIN (trading_preferences);
CREATE INDEX idx_user_profiles_kyc_information_gin ON user_profiles USING GIN (kyc_information);

-- Create specific indexes for commonly queried JSONB fields
CREATE INDEX idx_user_profiles_pan_number ON user_profiles USING GIN ((personal_info->'panNumber'));
CREATE INDEX idx_user_profiles_mobile_number ON user_profiles USING GIN ((personal_info->'mobileNumber'));
CREATE INDEX idx_user_profiles_email ON user_profiles USING GIN ((personal_info->'emailAddress'));
CREATE INDEX idx_user_profiles_kyc_status ON user_profiles USING GIN ((kyc_information->'kycStatus'));

-- Create user_documents table
CREATE TABLE user_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    verification_remarks TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT user_documents_verification_status_check 
        CHECK (verification_status IN ('pending', 'verified', 'rejected', 'expired'))
);

-- Create indexes for user_documents
CREATE INDEX idx_user_documents_profile_id ON user_documents (user_profile_id);
CREATE INDEX idx_user_documents_type ON user_documents (document_type);
CREATE INDEX idx_user_documents_status ON user_documents (verification_status);
CREATE INDEX idx_user_documents_uploaded_at ON user_documents (uploaded_at);

-- Create trading_profiles table (separate from main profile for performance)
CREATE TABLE trading_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    segment_preferences JSONB NOT NULL,
    risk_profile JSONB NOT NULL,
    default_settings JSONB NOT NULL,
    broker_configurations JSONB,
    watchlist_symbols TEXT[], -- Array of stock symbols
    custom_settings JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT trading_profiles_user_profile_unique UNIQUE (user_profile_id)
);

-- Create indexes for trading_profiles
CREATE INDEX idx_trading_profiles_profile_id ON trading_profiles (user_profile_id);
CREATE INDEX idx_trading_profiles_risk_profile ON trading_profiles USING GIN (risk_profile);
CREATE INDEX idx_trading_profiles_segments ON trading_profiles USING GIN (segment_preferences);
CREATE INDEX idx_trading_profiles_watchlist ON trading_profiles USING GIN (watchlist_symbols);

-- Create profile_audit_logs table
CREATE TABLE profile_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    changed_by UUID NOT NULL, -- Reference to user who made the change
    change_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL, -- profile, document, trading_preferences
    entity_id UUID, -- ID of the changed entity
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT audit_logs_change_type_check 
        CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'KYC_SUBMIT', 'KYC_VERIFY')),
    CONSTRAINT audit_logs_entity_type_check 
        CHECK (entity_type IN ('profile', 'document', 'trading_preferences', 'kyc', 'notification_settings'))
);

-- Create indexes for profile_audit_logs
CREATE INDEX idx_audit_logs_profile_id ON profile_audit_logs (user_profile_id);
CREATE INDEX idx_audit_logs_changed_by ON profile_audit_logs (changed_by);
CREATE INDEX idx_audit_logs_change_type ON profile_audit_logs (change_type);
CREATE INDEX idx_audit_logs_entity_type ON profile_audit_logs (entity_type);
CREATE INDEX idx_audit_logs_created_at ON profile_audit_logs (created_at);
CREATE INDEX idx_audit_logs_ip_address ON profile_audit_logs (ip_address);

-- Create kyc_verifications table for tracking KYC verification process
CREATE TABLE kyc_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    verification_type VARCHAR(50) NOT NULL, -- PAN, AADHAAR, BANK_ACCOUNT, VIDEO_KYC
    verification_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    verification_data JSONB NOT NULL,
    external_reference_id VARCHAR(255),
    verified_by UUID, -- Reference to admin/system user who verified
    verification_remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT kyc_verifications_type_check 
        CHECK (verification_type IN ('PAN', 'AADHAAR', 'BANK_ACCOUNT', 'VIDEO_KYC', 'DOCUMENT')),
    CONSTRAINT kyc_verifications_status_check 
        CHECK (verification_status IN ('pending', 'in_progress', 'verified', 'rejected', 'expired'))
);

-- Create indexes for kyc_verifications
CREATE INDEX idx_kyc_verifications_profile_id ON kyc_verifications (user_profile_id);
CREATE INDEX idx_kyc_verifications_type ON kyc_verifications (verification_type);
CREATE INDEX idx_kyc_verifications_status ON kyc_verifications (verification_status);
CREATE INDEX idx_kyc_verifications_created_at ON kyc_verifications (created_at);
CREATE INDEX idx_kyc_verifications_external_ref ON kyc_verifications (external_reference_id);

-- Create user_preferences table for additional settings
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    preference_category VARCHAR(50) NOT NULL, -- UI, TRADING, ALERTS, PRIVACY
    preference_key VARCHAR(100) NOT NULL,
    preference_value JSONB NOT NULL,
    is_system_preference BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT user_preferences_unique_key UNIQUE (user_profile_id, preference_category, preference_key)
);

-- Create indexes for user_preferences
CREATE INDEX idx_user_preferences_profile_id ON user_preferences (user_profile_id);
CREATE INDEX idx_user_preferences_category ON user_preferences (preference_category);
CREATE INDEX idx_user_preferences_key ON user_preferences (preference_key);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_user_profiles_updated_at 
    BEFORE UPDATE ON user_profiles 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_trading_profiles_updated_at 
    BEFORE UPDATE ON trading_profiles 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at 
    BEFORE UPDATE ON user_preferences 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE user_profiles IS 'Main user profile table containing personal info, KYC, and preferences';
COMMENT ON TABLE user_documents IS 'User uploaded documents for KYC and verification';
COMMENT ON TABLE trading_profiles IS 'Trading-specific preferences and configurations';
COMMENT ON TABLE profile_audit_logs IS 'Audit trail for all profile-related changes';
COMMENT ON TABLE kyc_verifications IS 'KYC verification tracking and status management';
COMMENT ON TABLE user_preferences IS 'User-specific application preferences and settings';

-- Insert initial reference data if needed
-- This would typically be in a separate migration file