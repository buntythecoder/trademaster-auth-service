-- ML Infrastructure Platform Database Schema
-- Initial schema creation for ML platform entities

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ML Experiments Table
CREATE TABLE ml_experiments (
    id BIGSERIAL PRIMARY KEY,
    experiment_name VARCHAR(255) NOT NULL,
    model_type VARCHAR(100) NOT NULL,
    training_dataset_id VARCHAR(255),
    feature_set_version VARCHAR(50),
    hyperparameters JSONB,
    metrics JSONB,
    artifacts JSONB,
    experiment_status VARCHAR(30) DEFAULT 'RUNNING',
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    created_by VARCHAR(255),
    mlflow_run_id VARCHAR(255) UNIQUE,
    mlflow_experiment_id VARCHAR(255),
    tags JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Model Registry Table
CREATE TABLE model_registry (
    id BIGSERIAL PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    model_stage VARCHAR(30) DEFAULT 'None',
    model_uri TEXT NOT NULL,
    model_type VARCHAR(100) NOT NULL,
    run_id VARCHAR(255),
    training_experiment_id BIGINT REFERENCES ml_experiments(id),
    performance_metrics JSONB,
    validation_results JSONB,
    metadata JSONB,
    deployment_config JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    deployed_at TIMESTAMP,
    retired_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(model_name, model_version)
);

-- Feature Store Table
CREATE TABLE feature_store (
    id BIGSERIAL PRIMARY KEY,
    feature_name VARCHAR(255) NOT NULL,
    feature_version VARCHAR(50) NOT NULL,
    feature_definition TEXT NOT NULL,
    feature_type VARCHAR(50) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    computation_type VARCHAR(20) NOT NULL,
    dependencies JSONB,
    validation_rules JSONB,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    last_updated TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(255),
    UNIQUE(feature_name, feature_version)
);

-- Model Deployments Table
CREATE TABLE model_deployments (
    id BIGSERIAL PRIMARY KEY,
    deployment_id VARCHAR(255) UNIQUE NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    replicas INTEGER DEFAULT 1,
    min_replicas INTEGER DEFAULT 1,
    max_replicas INTEGER DEFAULT 10,
    target_cpu_utilization INTEGER DEFAULT 70,
    endpoint TEXT,
    namespace VARCHAR(100) DEFAULT 'ml-platform',
    resource_config JSONB,
    environment_variables JSONB,
    deployment_metadata JSONB,
    deployed_at TIMESTAMP DEFAULT NOW(),
    last_health_check TIMESTAMP,
    health_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Model Predictions Table (for monitoring)
CREATE TABLE model_predictions (
    id BIGSERIAL PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    prediction_id VARCHAR(255) UNIQUE NOT NULL,
    input_features JSONB NOT NULL,
    prediction_result JSONB NOT NULL,
    confidence_score DECIMAL(5,4),
    inference_latency_ms INTEGER,
    prediction_timestamp TIMESTAMP DEFAULT NOW(),
    user_id VARCHAR(255),
    request_id VARCHAR(255),
    session_id VARCHAR(255),
    request_metadata JSONB,
    response_metadata JSONB,
    feedback_score DECIMAL(3,2),
    feedback_comment TEXT,
    feedback_timestamp TIMESTAMP,
    is_training_data BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Model Performance Metrics Table
CREATE TABLE model_performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(10,6) NOT NULL,
    metric_timestamp TIMESTAMP DEFAULT NOW(),
    aggregation_period VARCHAR(20) NOT NULL,
    metadata JSONB
);

-- Data Drift Detection Table
CREATE TABLE data_drift_detection (
    id BIGSERIAL PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    feature_name VARCHAR(255) NOT NULL,
    drift_score DECIMAL(6,4) NOT NULL,
    drift_threshold DECIMAL(6,4) NOT NULL,
    is_drifting BOOLEAN NOT NULL,
    detection_method VARCHAR(50) NOT NULL,
    reference_period_start TIMESTAMP NOT NULL,
    reference_period_end TIMESTAMP NOT NULL,
    comparison_period_start TIMESTAMP NOT NULL,
    comparison_period_end TIMESTAMP NOT NULL,
    detected_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_ml_experiments_status ON ml_experiments(experiment_status);
CREATE INDEX idx_ml_experiments_type ON ml_experiments(model_type);
CREATE INDEX idx_ml_experiments_created_by ON ml_experiments(created_by);
CREATE INDEX idx_ml_experiments_mlflow_run ON ml_experiments(mlflow_run_id);
CREATE INDEX idx_ml_experiments_mlflow_exp ON ml_experiments(mlflow_experiment_id);

CREATE INDEX idx_model_registry_name ON model_registry(model_name);
CREATE INDEX idx_model_registry_stage ON model_registry(model_stage);
CREATE INDEX idx_model_registry_type ON model_registry(model_type);
CREATE INDEX idx_model_registry_run_id ON model_registry(run_id);
CREATE INDEX idx_model_registry_training_exp ON model_registry(training_experiment_id);

CREATE INDEX idx_feature_store_name ON feature_store(feature_name);
CREATE INDEX idx_feature_store_type ON feature_store(computation_type);
CREATE INDEX idx_feature_store_active ON feature_store(is_active);
CREATE INDEX idx_feature_store_data_type ON feature_store(data_type);

CREATE INDEX idx_model_deployments_status ON model_deployments(status);
CREATE INDEX idx_model_deployments_model ON model_deployments(model_name, model_version);
CREATE INDEX idx_model_deployments_deployment_id ON model_deployments(deployment_id);

CREATE INDEX idx_model_predictions_timestamp ON model_predictions(model_name, model_version, prediction_timestamp);
CREATE INDEX idx_model_predictions_user ON model_predictions(user_id, prediction_timestamp);
CREATE INDEX idx_model_predictions_request ON model_predictions(request_id);
CREATE INDEX idx_model_predictions_session ON model_predictions(session_id);

CREATE INDEX idx_performance_metrics_time ON model_performance_metrics(model_name, model_version, metric_name, metric_timestamp);
CREATE INDEX idx_performance_metrics_model ON model_performance_metrics(model_name, model_version);

CREATE INDEX idx_drift_detection_model_feature ON data_drift_detection(model_name, feature_name, detected_at);
CREATE INDEX idx_drift_detection_timestamp ON data_drift_detection(detected_at);

-- Create triggers for updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_ml_experiments_updated_at BEFORE UPDATE ON ml_experiments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_model_registry_updated_at BEFORE UPDATE ON model_registry
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_feature_store_updated_at BEFORE UPDATE ON feature_store
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_model_deployments_updated_at BEFORE UPDATE ON model_deployments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert initial data
INSERT INTO ml_experiments (experiment_name, model_type, tags, created_by, mlflow_experiment_id) 
VALUES ('default-experiment', 'classification', '{"environment": "development", "team": "ml-team"}', 'system', '0');

INSERT INTO feature_store (feature_name, feature_version, feature_definition, feature_type, data_type, computation_type, description, created_by)
VALUES 
    ('user_age', '1.0', 'SELECT age FROM users WHERE id = ?', 'demographic', 'integer', 'real_time', 'User age in years', 'system'),
    ('account_balance', '1.0', 'SELECT balance FROM accounts WHERE user_id = ?', 'financial', 'float', 'real_time', 'Current account balance', 'system'),
    ('trade_count_7d', '1.0', 'SELECT COUNT(*) FROM trades WHERE user_id = ? AND created_at > NOW() - INTERVAL 7 DAY', 'behavioral', 'integer', 'batch', '7-day trade count', 'system');

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO postgres;

-- Create database for MLflow if it doesn't exist (for local development)
CREATE DATABASE mlflow;
CREATE DATABASE feature_store;