-- V1__Create_Behavioral_AI_Schema.sql
-- Initial schema for TradeMaster Behavioral AI Service
-- Follows specification requirements for behavioral pattern recognition and coaching

-- Create behavioral_patterns table
CREATE TABLE behavioral_patterns (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    pattern_type VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(5,4) NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    pattern_data JSONB DEFAULT '{}',
    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    trading_session_id VARCHAR(255),
    emotional_state VARCHAR(50),
    risk_score DECIMAL(5,4) CHECK (risk_score >= 0.0 AND risk_score <= 1.0),
    intervention_triggered BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT valid_pattern_type CHECK (pattern_type IN (
        'IMPULSIVE_TRADING',
        'FEAR_OF_MISSING_OUT', 
        'LOSS_AVERSION',
        'OVERCONFIDENCE_BIAS',
        'REVENGE_TRADING',
        'ANALYSIS_PARALYSIS',
        'HERD_MENTALITY',
        'CONFIRMATION_BIAS',
        'ANCHORING_BIAS',
        'PANIC_SELLING'
    )),
    
    CONSTRAINT valid_emotional_state CHECK (emotional_state IN (
        'CALM',
        'EXCITED',
        'ANXIOUS', 
        'FEARFUL',
        'CONFIDENT',
        'FRUSTRATED',
        'EUPHORIC',
        'PANICKED',
        'GREEDY',
        'REGRETFUL'
    ))
);

-- Create trading_psychology_profiles table
CREATE TABLE trading_psychology_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL,
    risk_tolerance_score DECIMAL(5,4) NOT NULL CHECK (risk_tolerance_score >= 0.0 AND risk_tolerance_score <= 1.0),
    emotional_stability_score DECIMAL(5,4) NOT NULL CHECK (emotional_stability_score >= 0.0 AND emotional_stability_score <= 1.0),
    impulsivity_score DECIMAL(5,4) NOT NULL CHECK (impulsivity_score >= 0.0 AND impulsivity_score <= 1.0),
    overconfidence_score DECIMAL(5,4) NOT NULL CHECK (overconfidence_score >= 0.0 AND overconfidence_score <= 1.0),
    loss_aversion_score DECIMAL(5,4) NOT NULL CHECK (loss_aversion_score >= 0.0 AND loss_aversion_score <= 1.0),
    dominant_pattern VARCHAR(50),
    trader_type VARCHAR(50) NOT NULL,
    confidence_level DECIMAL(5,4) CHECK (confidence_level >= 0.0 AND confidence_level <= 1.0),
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT valid_dominant_pattern CHECK (dominant_pattern IN (
        'IMPULSIVE_TRADING',
        'FEAR_OF_MISSING_OUT',
        'LOSS_AVERSION', 
        'OVERCONFIDENCE_BIAS',
        'REVENGE_TRADING',
        'ANALYSIS_PARALYSIS',
        'HERD_MENTALITY',
        'CONFIRMATION_BIAS',
        'ANCHORING_BIAS',
        'PANIC_SELLING'
    )),
    
    CONSTRAINT valid_trader_type CHECK (trader_type IN (
        'CONSERVATIVE',
        'MODERATE',
        'AGGRESSIVE', 
        'BALANCED'
    ))
);

-- Create coaching_interventions table
CREATE TABLE coaching_interventions (
    id BIGSERIAL PRIMARY KEY,
    intervention_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    intervention_type VARCHAR(50) NOT NULL,
    trigger_pattern VARCHAR(50),
    message TEXT NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    trading_session_id VARCHAR(255),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    expected_effectiveness DECIMAL(3,2) CHECK (expected_effectiveness >= 0.0 AND expected_effectiveness <= 1.0),
    user_response VARCHAR(50),
    user_feedback TEXT,
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
    responded_at TIMESTAMP,
    actual_effectiveness DECIMAL(3,2) CHECK (actual_effectiveness >= 0.0 AND actual_effectiveness <= 1.0),
    action_taken BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT valid_intervention_type CHECK (intervention_type IN (
        'PRE_TRADE_WARNING',
        'REAL_TIME_ALERT',
        'POST_TRADE_ANALYSIS',
        'DAILY_INSIGHT',
        'WEEKLY_REPORT', 
        'EDUCATIONAL',
        'MINDFULNESS'
    )),
    
    CONSTRAINT valid_trigger_pattern CHECK (trigger_pattern IN (
        'IMPULSIVE_TRADING',
        'FEAR_OF_MISSING_OUT',
        'LOSS_AVERSION',
        'OVERCONFIDENCE_BIAS', 
        'REVENGE_TRADING',
        'ANALYSIS_PARALYSIS',
        'HERD_MENTALITY',
        'CONFIRMATION_BIAS',
        'ANCHORING_BIAS',
        'PANIC_SELLING'
    )),
    
    CONSTRAINT valid_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    
    CONSTRAINT valid_user_response CHECK (user_response IN (
        'ACCEPTED',
        'DISMISSED', 
        'IGNORED',
        'FEEDBACK_PROVIDED'
    ))
);

-- Create indexes for performance optimization

-- Behavioral patterns indexes
CREATE INDEX idx_behavioral_pattern_user_id ON behavioral_patterns(user_id);
CREATE INDEX idx_behavioral_pattern_detected_at ON behavioral_patterns(detected_at);
CREATE INDEX idx_behavioral_pattern_session ON behavioral_patterns(trading_session_id);
CREATE INDEX idx_behavioral_pattern_type ON behavioral_patterns(pattern_type);
CREATE INDEX idx_behavioral_pattern_intervention ON behavioral_patterns(intervention_triggered);
CREATE INDEX idx_behavioral_pattern_risk ON behavioral_patterns(risk_score) WHERE risk_score IS NOT NULL;
CREATE INDEX idx_behavioral_pattern_user_time ON behavioral_patterns(user_id, detected_at);
CREATE INDEX idx_behavioral_pattern_user_type ON behavioral_patterns(user_id, pattern_type);

-- Psychology profiles indexes  
CREATE INDEX idx_psychology_profile_user_id ON trading_psychology_profiles(user_id);
CREATE INDEX idx_psychology_profile_updated ON trading_psychology_profiles(last_updated);
CREATE INDEX idx_psychology_profile_trader_type ON trading_psychology_profiles(trader_type);
CREATE INDEX idx_psychology_profile_dominant_pattern ON trading_psychology_profiles(dominant_pattern);
CREATE INDEX idx_psychology_profile_confidence ON trading_psychology_profiles(confidence_level) WHERE confidence_level IS NOT NULL;

-- Coaching interventions indexes
CREATE INDEX idx_coaching_intervention_user_id ON coaching_interventions(user_id);
CREATE INDEX idx_coaching_intervention_triggered ON coaching_interventions(triggered_at);
CREATE INDEX idx_coaching_intervention_type ON coaching_interventions(intervention_type);
CREATE INDEX idx_coaching_intervention_session ON coaching_interventions(trading_session_id);
CREATE INDEX idx_coaching_intervention_priority ON coaching_interventions(priority);
CREATE INDEX idx_coaching_intervention_response ON coaching_interventions(user_response);
CREATE INDEX idx_coaching_intervention_pattern ON coaching_interventions(trigger_pattern);
CREATE INDEX idx_coaching_intervention_user_time ON coaching_interventions(user_id, triggered_at);
CREATE INDEX idx_coaching_intervention_effectiveness ON coaching_interventions(actual_effectiveness) WHERE actual_effectiveness IS NOT NULL;

-- Composite indexes for common query patterns
CREATE INDEX idx_behavioral_pattern_analysis ON behavioral_patterns(user_id, pattern_type, detected_at, confidence_score);
CREATE INDEX idx_coaching_intervention_analytics ON coaching_interventions(user_id, intervention_type, triggered_at, user_response);
CREATE INDEX idx_psychology_profile_risk_assessment ON trading_psychology_profiles(user_id, trader_type, last_updated);

-- Comments for documentation
COMMENT ON TABLE behavioral_patterns IS 'Stores detected behavioral patterns from trading activity analysis';
COMMENT ON TABLE trading_psychology_profiles IS 'Comprehensive psychological profiles for traders with behavioral scores';
COMMENT ON TABLE coaching_interventions IS 'AI-generated coaching interventions and user responses for behavioral modification';

COMMENT ON COLUMN behavioral_patterns.pattern_data IS 'JSONB column storing pattern-specific metrics and indicators';
COMMENT ON COLUMN behavioral_patterns.confidence_score IS 'ML model confidence score for pattern detection (0.0-1.0)';
COMMENT ON COLUMN behavioral_patterns.risk_score IS 'Risk assessment score based on pattern severity (0.0-1.0)';

COMMENT ON COLUMN trading_psychology_profiles.risk_tolerance_score IS 'User risk tolerance assessment (0.0=conservative, 1.0=aggressive)';
COMMENT ON COLUMN trading_psychology_profiles.emotional_stability_score IS 'Emotional consistency under stress (0.0=volatile, 1.0=stable)';
COMMENT ON COLUMN trading_psychology_profiles.impulsivity_score IS 'Tendency for spontaneous decisions (0.0=methodical, 1.0=impulsive)';
COMMENT ON COLUMN trading_psychology_profiles.overconfidence_score IS 'Self-assessment accuracy vs performance (0.0=realistic, 1.0=overconfident)';
COMMENT ON COLUMN trading_psychology_profiles.loss_aversion_score IS 'Emotional response to losses (0.0=accepts losses, 1.0=loss averse)';

COMMENT ON COLUMN coaching_interventions.expected_effectiveness IS 'Predicted intervention effectiveness before user response (0.0-1.0)';
COMMENT ON COLUMN coaching_interventions.actual_effectiveness IS 'Calculated effectiveness after user response (0.0-1.0)';
COMMENT ON COLUMN coaching_interventions.user_rating IS 'User satisfaction rating for intervention (1-5 stars)';

-- Create functions for data integrity and business logic
CREATE OR REPLACE FUNCTION update_psychology_profile_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers
CREATE TRIGGER trigger_update_psychology_profile_timestamp
    BEFORE UPDATE ON trading_psychology_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_psychology_profile_timestamp();

-- Create function to calculate risk score from pattern
CREATE OR REPLACE FUNCTION calculate_pattern_risk_score(
    p_pattern_type VARCHAR,
    p_confidence_score DECIMAL
) RETURNS DECIMAL AS $$
BEGIN
    -- High risk patterns get higher risk scores
    RETURN CASE 
        WHEN p_pattern_type IN ('IMPULSIVE_TRADING', 'REVENGE_TRADING', 'PANIC_SELLING', 'OVERCONFIDENCE_BIAS', 'FEAR_OF_MISSING_OUT') THEN
            p_confidence_score * 0.9
        ELSE
            p_confidence_score * 0.3
    END;
END;
$$ LANGUAGE plpgsql;

-- Insert default data for testing and development
-- Note: In production, this would be handled by application logic

-- Create view for pattern analytics
CREATE VIEW behavioral_pattern_analytics AS
SELECT 
    user_id,
    pattern_type,
    COUNT(*) as pattern_count,
    AVG(confidence_score) as avg_confidence,
    AVG(risk_score) as avg_risk_score,
    COUNT(*) FILTER (WHERE intervention_triggered = true) as interventions_triggered,
    MIN(detected_at) as first_detection,
    MAX(detected_at) as latest_detection,
    DATE_TRUNC('day', detected_at) as detection_date
FROM behavioral_patterns
GROUP BY user_id, pattern_type, DATE_TRUNC('day', detected_at);

COMMENT ON VIEW behavioral_pattern_analytics IS 'Aggregated analytics for behavioral pattern analysis and reporting';

-- Create view for coaching effectiveness
CREATE VIEW coaching_effectiveness_analytics AS
SELECT 
    user_id,
    intervention_type,
    COUNT(*) as total_interventions,
    COUNT(*) FILTER (WHERE user_response = 'ACCEPTED') as accepted_interventions,
    COUNT(*) FILTER (WHERE action_taken = true) as actions_taken,
    AVG(actual_effectiveness) as avg_effectiveness,
    AVG(user_rating) as avg_rating,
    AVG(EXTRACT(EPOCH FROM (responded_at - triggered_at))) as avg_response_time_seconds
FROM coaching_interventions
WHERE user_response IS NOT NULL
GROUP BY user_id, intervention_type;

COMMENT ON VIEW coaching_effectiveness_analytics IS 'Coaching intervention effectiveness metrics for optimization';

-- Grant permissions (adjust based on your security requirements)
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO behavioral_ai_service;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO behavioral_ai_service;
-- GRANT SELECT ON ALL VIEWS IN SCHEMA public TO behavioral_ai_service;