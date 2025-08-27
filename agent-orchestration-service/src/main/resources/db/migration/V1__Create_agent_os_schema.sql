-- TradeMaster Agent OS - Database Schema Migration
-- Version 1.0.0 - Initial schema for agents, tasks, and workflows

-- Enable UUID extension if needed
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Agents table - Core agent registry
CREATE TABLE agents (
    agent_id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(100) NOT NULL UNIQUE,
    agent_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    user_id BIGINT NOT NULL,
    max_concurrent_tasks INTEGER DEFAULT 5,
    current_load INTEGER DEFAULT 0,
    success_rate DOUBLE PRECISION DEFAULT 0.0,
    average_response_time BIGINT DEFAULT 0,
    total_tasks_completed BIGINT DEFAULT 0,
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_agent_type CHECK (agent_type IN ('MARKET_ANALYSIS', 'PORTFOLIO_MANAGEMENT', 'TRADING_EXECUTION', 'RISK_MANAGEMENT', 'NOTIFICATION', 'CUSTOM')),
    CONSTRAINT chk_agent_status CHECK (status IN ('INACTIVE', 'STARTING', 'ACTIVE', 'BUSY', 'ERROR', 'STOPPING', 'UNRESPONSIVE')),
    CONSTRAINT chk_max_concurrent_tasks CHECK (max_concurrent_tasks >= 1 AND max_concurrent_tasks <= 100),
    CONSTRAINT chk_current_load CHECK (current_load >= 0),
    CONSTRAINT chk_success_rate CHECK (success_rate >= 0.0 AND success_rate <= 1.0)
);

-- Agent capabilities mapping table
CREATE TABLE agent_capabilities (
    agent_id BIGINT NOT NULL,
    capability VARCHAR(50) NOT NULL,
    PRIMARY KEY (agent_id, capability),
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE,
    
    CONSTRAINT chk_capability CHECK (capability IN (
        'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 'SENTIMENT_ANALYSIS', 'MARKET_SCREENING', 'PRICE_PREDICTION', 'PATTERN_RECOGNITION',
        'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 'RISK_ASSESSMENT', 'PERFORMANCE_ANALYSIS', 'REBALANCING', 'DIVERSIFICATION_ANALYSIS',
        'ORDER_EXECUTION', 'BROKER_ROUTING', 'EXECUTION_OPTIMIZATION', 'SLIPPAGE_MONITORING', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_TRADING',
        'RISK_MONITORING', 'VAR_CALCULATION', 'STRESS_TESTING', 'COMPLIANCE_CHECK', 'CORRELATION_ANALYSIS', 'DRAWDOWN_MONITORING',
        'EMAIL_ALERTS', 'SMS_ALERTS', 'PUSH_NOTIFICATIONS', 'REPORT_GENERATION', 'REAL_TIME_ALERTS', 'SCHEDULED_REPORTS',
        'MARKET_DATA_INTEGRATION', 'BROKER_INTEGRATION', 'DATABASE_OPERATIONS', 'API_INTEGRATION', 'DATA_VALIDATION', 'REAL_TIME_STREAMING',
        'MACHINE_LEARNING', 'NATURAL_LANGUAGE_PROCESSING', 'ANOMALY_DETECTION', 'PREDICTIVE_MODELING', 'REINFORCEMENT_LEARNING',
        'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'EVENT_PROCESSING', 'DECISION_MAKING', 'MULTI_AGENT_COMMUNICATION',
        'CUSTOM_LOGIC', 'PLUGIN_SUPPORT', 'SCRIPTING'
    ))
);

-- Tasks table - Task execution and tracking
CREATE TABLE tasks (
    task_id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    workflow_id BIGINT,
    parent_task_id BIGINT,
    input_parameters TEXT,
    output_result TEXT,
    error_message VARCHAR(2000),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 300,
    estimated_duration_seconds INTEGER,
    actual_duration_seconds INTEGER,
    progress_percentage INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    deadline TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE SET NULL,
    FOREIGN KEY (parent_task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    
    CONSTRAINT chk_task_type CHECK (task_type IN (
        'MARKET_ANALYSIS', 'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 'SENTIMENT_ANALYSIS', 'MARKET_SCREENING', 'PRICE_PREDICTION',
        'PORTFOLIO_ANALYSIS', 'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 'REBALANCING', 'PERFORMANCE_ANALYSIS', 'DIVERSIFICATION_ANALYSIS',
        'ORDER_EXECUTION', 'SMART_ROUTING', 'EXECUTION_MONITORING', 'SLIPPAGE_ANALYSIS', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_EXECUTION',
        'RISK_ASSESSMENT', 'RISK_MONITORING', 'VAR_CALCULATION', 'STRESS_TESTING', 'COMPLIANCE_CHECK', 'DRAWDOWN_MONITORING',
        'ALERT_GENERATION', 'REPORT_GENERATION', 'EMAIL_NOTIFICATION', 'SMS_NOTIFICATION', 'PUSH_NOTIFICATION',
        'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'AGENT_COMMUNICATION', 'DECISION_MAKING',
        'CUSTOM_TASK', 'SCRIPT_EXECUTION', 'PLUGIN_EXECUTION'
    )),
    CONSTRAINT chk_task_status CHECK (status IN ('PENDING', 'QUEUED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'FAILED', 'ERROR', 'CANCELLED', 'TIMEOUT', 'WAITING')),
    CONSTRAINT chk_task_priority CHECK (priority IN ('CRITICAL', 'HIGH', 'NORMAL', 'LOW', 'DEFERRED')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_max_retries CHECK (max_retries >= 0),
    CONSTRAINT chk_timeout_seconds CHECK (timeout_seconds > 0),
    CONSTRAINT chk_progress_percentage CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- Task required capabilities mapping table
CREATE TABLE task_required_capabilities (
    task_id BIGINT NOT NULL,
    capability VARCHAR(50) NOT NULL,
    PRIMARY KEY (task_id, capability),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    
    CONSTRAINT chk_task_capability CHECK (capability IN (
        'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 'SENTIMENT_ANALYSIS', 'MARKET_SCREENING', 'PRICE_PREDICTION', 'PATTERN_RECOGNITION',
        'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 'RISK_ASSESSMENT', 'PERFORMANCE_ANALYSIS', 'REBALANCING', 'DIVERSIFICATION_ANALYSIS',
        'ORDER_EXECUTION', 'BROKER_ROUTING', 'EXECUTION_OPTIMIZATION', 'SLIPPAGE_MONITORING', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_TRADING',
        'RISK_MONITORING', 'VAR_CALCULATION', 'STRESS_TESTING', 'COMPLIANCE_CHECK', 'CORRELATION_ANALYSIS', 'DRAWDOWN_MONITORING',
        'EMAIL_ALERTS', 'SMS_ALERTS', 'PUSH_NOTIFICATIONS', 'REPORT_GENERATION', 'REAL_TIME_ALERTS', 'SCHEDULED_REPORTS',
        'MARKET_DATA_INTEGRATION', 'BROKER_INTEGRATION', 'DATABASE_OPERATIONS', 'API_INTEGRATION', 'DATA_VALIDATION', 'REAL_TIME_STREAMING',
        'MACHINE_LEARNING', 'NATURAL_LANGUAGE_PROCESSING', 'ANOMALY_DETECTION', 'PREDICTIVE_MODELING', 'REINFORCEMENT_LEARNING',
        'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'EVENT_PROCESSING', 'DECISION_MAKING', 'MULTI_AGENT_COMMUNICATION',
        'CUSTOM_LOGIC', 'PLUGIN_SUPPORT', 'SCRIPTING'
    ))
);

-- Workflows table - Multi-task workflow definitions
CREATE TABLE workflows (
    workflow_id BIGSERIAL PRIMARY KEY,
    workflow_name VARCHAR(200) NOT NULL,
    workflow_type VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    user_id BIGINT NOT NULL,
    workflow_definition TEXT NOT NULL, -- JSON workflow definition
    input_parameters TEXT,
    output_result TEXT,
    error_message VARCHAR(2000),
    progress_percentage INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    deadline TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_workflow_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_workflow_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- Agent performance metrics table
CREATE TABLE agent_performance_metrics (
    metric_id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    metric_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cpu_usage_percentage DOUBLE PRECISION,
    memory_usage_mb BIGINT,
    network_usage_mbps DOUBLE PRECISION,
    tasks_completed_count INTEGER DEFAULT 0,
    tasks_failed_count INTEGER DEFAULT 0,
    average_task_duration_seconds DOUBLE PRECISION,
    success_rate DOUBLE PRECISION,
    
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE,
    
    CONSTRAINT chk_cpu_usage CHECK (cpu_usage_percentage >= 0.0 AND cpu_usage_percentage <= 100.0),
    CONSTRAINT chk_memory_usage CHECK (memory_usage_mb >= 0),
    CONSTRAINT chk_network_usage CHECK (network_usage_mbps >= 0.0),
    CONSTRAINT chk_tasks_completed CHECK (tasks_completed_count >= 0),
    CONSTRAINT chk_tasks_failed CHECK (tasks_failed_count >= 0),
    CONSTRAINT chk_perf_success_rate CHECK (success_rate >= 0.0 AND success_rate <= 1.0)
);

-- Task execution history table
CREATE TABLE task_execution_history (
    history_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    agent_id BIGINT,
    status_change VARCHAR(30) NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    change_reason VARCHAR(500),
    execution_context TEXT,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE SET NULL
);

-- System configuration table
CREATE TABLE system_configuration (
    config_id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    config_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(500),
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_config_type CHECK (config_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'JSON', 'ENCRYPTED'))
);

-- Create indexes for performance optimization
CREATE INDEX idx_agent_type ON agents(agent_type);
CREATE INDEX idx_agent_status ON agents(status);
CREATE INDEX idx_agent_user ON agents(user_id);
CREATE INDEX idx_agent_heartbeat ON agents(last_heartbeat);

CREATE INDEX idx_task_status ON tasks(status);
CREATE INDEX idx_task_type ON tasks(task_type);
CREATE INDEX idx_task_priority ON tasks(priority);
CREATE INDEX idx_task_agent ON tasks(agent_id);
CREATE INDEX idx_task_user ON tasks(user_id);
CREATE INDEX idx_task_workflow ON tasks(workflow_id);
CREATE INDEX idx_task_created ON tasks(created_at);
CREATE INDEX idx_task_deadline ON tasks(deadline);
CREATE INDEX idx_task_status_priority ON tasks(status, priority);

CREATE INDEX idx_workflow_status ON workflows(status);
CREATE INDEX idx_workflow_user ON workflows(user_id);
CREATE INDEX idx_workflow_type ON workflows(workflow_type);

CREATE INDEX idx_perf_metrics_agent ON agent_performance_metrics(agent_id);
CREATE INDEX idx_perf_metrics_timestamp ON agent_performance_metrics(metric_timestamp);
CREATE INDEX idx_perf_metrics_agent_time ON agent_performance_metrics(agent_id, metric_timestamp);

CREATE INDEX idx_task_history_task ON task_execution_history(task_id);
CREATE INDEX idx_task_history_agent ON task_execution_history(agent_id);
CREATE INDEX idx_task_history_timestamp ON task_execution_history(changed_at);

CREATE INDEX idx_system_config_key ON system_configuration(config_key);

-- Create functions for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_agents_updated_at BEFORE UPDATE ON agents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_workflows_updated_at BEFORE UPDATE ON workflows FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_configuration FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default system configuration
INSERT INTO system_configuration (config_key, config_value, config_type, description) VALUES
('max_concurrent_agents', '50', 'INTEGER', 'Maximum number of concurrent agents allowed'),
('default_task_timeout_seconds', '300', 'INTEGER', 'Default timeout for tasks in seconds'),
('agent_heartbeat_interval_seconds', '30', 'INTEGER', 'Interval between agent heartbeat checks'),
('task_queue_size', '1000', 'INTEGER', 'Maximum size of the task queue'),
('enable_auto_scaling', 'true', 'BOOLEAN', 'Enable automatic agent scaling'),
('system_maintenance_mode', 'false', 'BOOLEAN', 'System maintenance mode flag'),
('log_level', 'INFO', 'STRING', 'System logging level'),
('performance_monitoring_enabled', 'true', 'BOOLEAN', 'Enable performance monitoring'),
('task_retry_max_attempts', '3', 'INTEGER', 'Maximum retry attempts for failed tasks'),
('workflow_execution_timeout_seconds', '1800', 'INTEGER', 'Default timeout for workflow execution');

-- Create initial agent type statistics view
CREATE OR REPLACE VIEW agent_type_statistics AS
SELECT 
    agent_type,
    COUNT(*) as total_agents,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_agents,
    COUNT(*) FILTER (WHERE status = 'BUSY') as busy_agents,
    COUNT(*) FILTER (WHERE status = 'ERROR') as error_agents,
    AVG(success_rate) as avg_success_rate,
    AVG(average_response_time) as avg_response_time,
    SUM(total_tasks_completed) as total_tasks_completed
FROM agents 
GROUP BY agent_type;

-- Create task queue status view
CREATE OR REPLACE VIEW task_queue_status AS
SELECT 
    status,
    priority,
    COUNT(*) as task_count,
    AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at))) as avg_age_seconds,
    COUNT(*) FILTER (WHERE deadline < CURRENT_TIMESTAMP) as overdue_tasks
FROM tasks 
WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'ERROR')
GROUP BY status, priority
ORDER BY priority DESC, status;

-- Create agent workload view
CREATE OR REPLACE VIEW agent_workload AS
SELECT 
    a.agent_id,
    a.agent_name,
    a.agent_type,
    a.status,
    a.current_load,
    a.max_concurrent_tasks,
    ROUND((a.current_load::DECIMAL / a.max_concurrent_tasks::DECIMAL) * 100, 2) as utilization_percentage,
    COUNT(t.task_id) as pending_tasks
FROM agents a
LEFT JOIN tasks t ON a.agent_id = t.agent_id AND t.status IN ('QUEUED', 'IN_PROGRESS', 'PAUSED')
GROUP BY a.agent_id, a.agent_name, a.agent_type, a.status, a.current_load, a.max_concurrent_tasks
ORDER BY utilization_percentage DESC;