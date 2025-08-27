-- Payment Service Database Schema
-- Version: 1.0.0
-- Description: Core payment processing tables for TradeMaster platform

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Custom Types
CREATE TYPE payment_status AS ENUM (
    'pending',
    'processing', 
    'completed',
    'failed',
    'cancelled',
    'refunded',
    'partially_refunded'
);

CREATE TYPE payment_gateway_enum AS ENUM (
    'razorpay',
    'stripe',
    'upi'
);

CREATE TYPE payment_method_enum AS ENUM (
    'card',
    'upi',
    'netbanking',
    'wallet',
    'bnpl'
);

CREATE TYPE subscription_status AS ENUM (
    'active',
    'inactive',
    'cancelled',
    'expired',
    'suspended'
);

CREATE TYPE billing_cycle AS ENUM (
    'monthly',
    'quarterly', 
    'annual'
);

-- Subscription Plans Table
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Pricing
    price DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    billing_cycle billing_cycle NOT NULL DEFAULT 'monthly',
    
    -- Features
    features JSONB NOT NULL DEFAULT '{}',
    limits JSONB NOT NULL DEFAULT '{}',
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    
    -- Trial Configuration
    trial_days INTEGER DEFAULT 0,
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT valid_price CHECK (price >= 0),
    CONSTRAINT valid_trial_days CHECK (trial_days >= 0)
);

-- User Subscriptions Table
CREATE TABLE user_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    
    -- Subscription Details
    status subscription_status NOT NULL DEFAULT 'active',
    
    -- Billing Information
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    next_billing_date TIMESTAMP WITH TIME ZONE,
    
    -- Trial Information
    trial_start TIMESTAMP WITH TIME ZONE,
    trial_end TIMESTAMP WITH TIME ZONE,
    is_trial_used BOOLEAN DEFAULT false,
    
    -- Pricing
    amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    
    -- Gateway Information
    gateway_subscription_id VARCHAR(255),
    payment_gateway payment_gateway_enum,
    
    -- Cancellation Information
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    cancel_at_period_end BOOLEAN DEFAULT false,
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT valid_amount CHECK (amount >= 0),
    CONSTRAINT valid_period CHECK (current_period_end > current_period_start),
    CONSTRAINT valid_trial_period CHECK (
        (trial_start IS NULL AND trial_end IS NULL) OR 
        (trial_start IS NOT NULL AND trial_end IS NOT NULL AND trial_end > trial_start)
    )
);

-- Payment Transactions Table  
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_id UUID REFERENCES user_subscriptions(id),
    
    -- Payment Details
    amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    status payment_status NOT NULL DEFAULT 'pending',
    
    -- Gateway Integration
    payment_gateway payment_gateway_enum NOT NULL,
    gateway_transaction_id VARCHAR(255),
    gateway_order_id VARCHAR(255),
    gateway_payment_id VARCHAR(255),
    
    -- Payment Method
    payment_method payment_method_enum NOT NULL,
    payment_method_details JSONB DEFAULT '{}',
    
    -- Transaction Details
    description TEXT,
    receipt_number VARCHAR(100),
    
    -- Failure Information
    failure_reason TEXT,
    failure_code VARCHAR(50),
    
    -- Refund Information
    refunded_amount DECIMAL(10,2) DEFAULT 0,
    refund_reason TEXT,
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    gateway_response JSONB DEFAULT '{}',
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT valid_amount CHECK (amount >= 0),
    CONSTRAINT valid_refunded_amount CHECK (refunded_amount >= 0 AND refunded_amount <= amount)
);

-- User Payment Methods Table (Tokenized Storage)
CREATE TABLE user_payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    
    -- Tokenized Payment Details (No sensitive data stored)
    payment_method_token VARCHAR(255) NOT NULL,
    payment_method_type payment_method_enum NOT NULL,
    gateway_provider payment_gateway_enum NOT NULL,
    
    -- Display Information Only (No sensitive data)
    display_name VARCHAR(100),
    last_four_digits CHAR(4),
    expiry_month INTEGER,
    expiry_year INTEGER,
    brand VARCHAR(50), -- Visa, Mastercard, etc.
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT valid_expiry_month CHECK (expiry_month >= 1 AND expiry_month <= 12),
    CONSTRAINT valid_expiry_year CHECK (expiry_year >= EXTRACT(YEAR FROM NOW())),
    CONSTRAINT unique_user_default_method EXCLUDE (user_id WITH =) WHERE (is_default = true)
);

-- Payment Events Table (Audit Trail)
CREATE TABLE payment_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID REFERENCES payment_transactions(id),
    subscription_id UUID REFERENCES user_subscriptions(id),
    
    -- Event Details
    event_type VARCHAR(100) NOT NULL, -- payment.created, payment.succeeded, subscription.updated, etc.
    event_source VARCHAR(50) NOT NULL, -- webhook, api, internal
    
    -- Event Data
    event_data JSONB NOT NULL DEFAULT '{}',
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    
    -- Gateway Information
    gateway_event_id VARCHAR(255),
    gateway_signature VARCHAR(500),
    
    -- Processing Information
    processed BOOLEAN DEFAULT false,
    processing_attempts INTEGER DEFAULT 0,
    processing_error TEXT,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Webhook Logs Table
CREATE TABLE webhook_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Webhook Details
    gateway payment_gateway_enum NOT NULL,
    webhook_id VARCHAR(255),
    event_type VARCHAR(100) NOT NULL,
    
    -- Request Information
    request_headers JSONB DEFAULT '{}',
    request_body JSONB NOT NULL,
    signature VARCHAR(500),
    
    -- Processing Information
    signature_verified BOOLEAN DEFAULT false,
    processed BOOLEAN DEFAULT false,
    processing_attempts INTEGER DEFAULT 0,
    processing_error TEXT,
    
    -- Response Information
    response_status INTEGER,
    response_body TEXT,
    
    -- Audit
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for Performance
CREATE INDEX idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);
CREATE INDEX idx_user_subscriptions_next_billing_date ON user_subscriptions(next_billing_date) WHERE status = 'active';

CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_gateway_payment_id ON payment_transactions(gateway_payment_id);
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at);
CREATE INDEX idx_payment_transactions_subscription_id ON payment_transactions(subscription_id);

CREATE INDEX idx_user_payment_methods_user_id ON user_payment_methods(user_id);
CREATE INDEX idx_user_payment_methods_active ON user_payment_methods(user_id) WHERE is_active = true;

CREATE INDEX idx_payment_events_transaction_id ON payment_events(transaction_id);
CREATE INDEX idx_payment_events_subscription_id ON payment_events(subscription_id);
CREATE INDEX idx_payment_events_created_at ON payment_events(created_at);
CREATE INDEX idx_payment_events_processed ON payment_events(processed) WHERE processed = false;

CREATE INDEX idx_webhook_logs_gateway ON webhook_logs(gateway);
CREATE INDEX idx_webhook_logs_processed ON webhook_logs(processed) WHERE processed = false;
CREATE INDEX idx_webhook_logs_received_at ON webhook_logs(received_at);

-- Triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_subscription_plans_updated_at
    BEFORE UPDATE ON subscription_plans
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_subscriptions_updated_at
    BEFORE UPDATE ON user_subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_transactions_updated_at
    BEFORE UPDATE ON payment_transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_payment_methods_updated_at
    BEFORE UPDATE ON user_payment_methods
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default subscription plans
INSERT INTO subscription_plans (name, description, price, currency, billing_cycle, features, limits) VALUES
('Free', 'Basic trading features with limited access', 0.00, 'INR', 'monthly', 
    '{"real_time_data": false, "advanced_charts": false, "ai_insights": false, "portfolio_analytics": "basic", "trading_accounts": 1}',
    '{"trades_per_month": 10, "watchlist_symbols": 50, "alerts": 5}'
),
('Pro', 'Advanced trading with real-time data and analytics', 999.00, 'INR', 'monthly',
    '{"real_time_data": true, "advanced_charts": true, "ai_insights": "basic", "portfolio_analytics": "advanced", "trading_accounts": 3, "api_access": false}',
    '{"trades_per_month": 1000, "watchlist_symbols": 500, "alerts": 50}'
),
('AI Premium', 'Full AI-powered trading with behavioral analysis', 2999.00, 'INR', 'monthly',
    '{"real_time_data": true, "advanced_charts": true, "ai_insights": "premium", "behavioral_ai": true, "portfolio_analytics": "premium", "trading_accounts": 5, "api_access": true}',
    '{"trades_per_month": -1, "watchlist_symbols": 2000, "alerts": 200}'
),
('Institutional', 'Enterprise-grade features for professional traders', 9999.00, 'INR', 'monthly',
    '{"real_time_data": true, "advanced_charts": true, "ai_insights": "premium", "behavioral_ai": true, "institutional_features": true, "portfolio_analytics": "institutional", "trading_accounts": 10, "api_access": true, "white_label": true}',
    '{"trades_per_month": -1, "watchlist_symbols": -1, "alerts": -1}'
);

-- Comments on tables
COMMENT ON TABLE subscription_plans IS 'Subscription plan definitions with pricing and features';
COMMENT ON TABLE user_subscriptions IS 'User subscription records with billing information';
COMMENT ON TABLE payment_transactions IS 'All payment transactions with gateway integration details';
COMMENT ON TABLE user_payment_methods IS 'Tokenized payment method storage (PCI compliant)';
COMMENT ON TABLE payment_events IS 'Audit trail of all payment-related events';
COMMENT ON TABLE webhook_logs IS 'Webhook processing logs for debugging and compliance';