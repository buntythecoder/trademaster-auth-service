-- ✅ MANDATORY: Database Schema Migration V1
-- Phase 1 Requirement: Agent Orchestration Database Schema
-- PostgreSQL DDL with proper indexes and constraints

-- ===============================
-- AGENT ENTITY TABLE
-- ===============================

CREATE TABLE agents (
    agent_id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL UNIQUE,
    agent_type VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'STARTING',
    current_load INTEGER NOT NULL DEFAULT 0,
    max_concurrent_tasks INTEGER NOT NULL DEFAULT 5,
    success_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    average_response_time BIGINT NOT NULL DEFAULT 0,
    total_tasks_completed BIGINT NOT NULL DEFAULT 0,
    last_heartbeat TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    retry_count INTEGER NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- ✅ CONSTRAINTS
    CONSTRAINT chk_agents_current_load CHECK (current_load >= 0),
    CONSTRAINT chk_agents_max_concurrent_tasks CHECK (max_concurrent_tasks > 0),
    CONSTRAINT chk_agents_success_rate CHECK (success_rate >= 0.0 AND success_rate <= 1.0),
    CONSTRAINT chk_agents_average_response_time CHECK (average_response_time >= 0),
    CONSTRAINT chk_agents_total_tasks_completed CHECK (total_tasks_completed >= 0),
    CONSTRAINT chk_agents_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_agents_user_id CHECK (user_id > 0)
);

-- ✅ INDEXES for Agent Performance
CREATE INDEX idx_agents_agent_type ON agents(agent_type);
CREATE INDEX idx_agents_status ON agents(status);
CREATE INDEX idx_agents_user_id ON agents(user_id);
CREATE INDEX idx_agents_last_heartbeat ON agents(last_heartbeat);
CREATE INDEX idx_agents_current_load ON agents(current_load);
CREATE INDEX idx_agents_created_at ON agents(created_at);

-- ✅ COMPOSITE INDEXES for Agent Selection
CREATE INDEX idx_agents_type_status ON agents(agent_type, status);
CREATE INDEX idx_agents_status_load ON agents(status, current_load);
CREATE INDEX idx_agents_type_status_load ON agents(agent_type, status, current_load);

-- ✅ PARTIAL INDEXES for Active Agents
CREATE INDEX idx_agents_active_by_type ON agents(agent_type, current_load) WHERE status = 'ACTIVE';
CREATE INDEX idx_agents_available_by_load ON agents(agent_id, current_load, max_concurrent_tasks) WHERE status IN ('ACTIVE', 'IDLE');

-- ===============================
-- TASK ENTITY TABLE
-- ===============================

CREATE TABLE tasks (
    task_id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    agent_id BIGINT REFERENCES agents(agent_id),
    assigned_agent_type VARCHAR(50),
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    estimated_duration BIGINT,
    actual_duration BIGINT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    input_parameters TEXT, -- JSON format
    output_result TEXT,    -- JSON format
    error_message TEXT,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- ✅ CONSTRAINTS
    CONSTRAINT chk_tasks_progress_percentage CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    CONSTRAINT chk_tasks_estimated_duration CHECK (estimated_duration IS NULL OR estimated_duration > 0),
    CONSTRAINT chk_tasks_actual_duration CHECK (actual_duration IS NULL OR actual_duration > 0),
    CONSTRAINT chk_tasks_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_tasks_max_retries CHECK (max_retries >= 0),
    CONSTRAINT chk_tasks_user_id CHECK (user_id > 0),
    CONSTRAINT chk_tasks_started_completed CHECK (started_at IS NULL OR completed_at IS NULL OR started_at <= completed_at)
);

-- ✅ INDEXES for Task Performance
CREATE INDEX idx_tasks_task_type ON tasks(task_type);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_agent_id ON tasks(agent_id);
CREATE INDEX idx_tasks_assigned_agent_type ON tasks(assigned_agent_type);
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);
CREATE INDEX idx_tasks_completed_at ON tasks(completed_at);

-- ✅ COMPOSITE INDEXES for Task Querying
CREATE INDEX idx_tasks_status_priority ON tasks(status, priority);
CREATE INDEX idx_tasks_type_status ON tasks(task_type, status);
CREATE INDEX idx_tasks_agent_status ON tasks(agent_id, status);
CREATE INDEX idx_tasks_user_status ON tasks(user_id, status);

-- ✅ PARTIAL INDEXES for Active Tasks
CREATE INDEX idx_tasks_pending_by_priority ON tasks(priority, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_tasks_in_progress_by_agent ON tasks(agent_id, started_at) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_tasks_failed_for_retry ON tasks(task_id, retry_count, max_retries) WHERE status = 'FAILED' AND retry_count < max_retries;

-- ===============================
-- AGENT CAPABILITIES TABLE
-- ===============================

CREATE TABLE agent_capabilities (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(agent_id) ON DELETE CASCADE,
    capability VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- ✅ UNIQUE CONSTRAINT
    UNIQUE(agent_id, capability)
);

-- ✅ INDEXES for Capability Lookup
CREATE INDEX idx_agent_capabilities_agent_id ON agent_capabilities(agent_id);
CREATE INDEX idx_agent_capabilities_capability ON agent_capabilities(capability);

-- ===============================
-- AGENT PERFORMANCE METRICS TABLE
-- ===============================

CREATE TABLE agent_performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(agent_id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(50),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- ✅ CONSTRAINTS
    CONSTRAINT chk_agent_performance_metrics_metric_value CHECK (metric_value >= 0)
);

-- ✅ INDEXES for Performance Metrics
CREATE INDEX idx_agent_performance_metrics_agent_id ON agent_performance_metrics(agent_id);
CREATE INDEX idx_agent_performance_metrics_name ON agent_performance_metrics(metric_name);
CREATE INDEX idx_agent_performance_metrics_recorded_at ON agent_performance_metrics(recorded_at);
CREATE INDEX idx_agent_performance_metrics_agent_name_time ON agent_performance_metrics(agent_id, metric_name, recorded_at);

-- ===============================
-- TASK DEPENDENCIES TABLE
-- ===============================

CREATE TABLE task_dependencies (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    depends_on_task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    dependency_type VARCHAR(50) NOT NULL DEFAULT 'SEQUENTIAL',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- ✅ CONSTRAINTS
    UNIQUE(task_id, depends_on_task_id),
    CONSTRAINT chk_task_dependencies_no_self_ref CHECK (task_id != depends_on_task_id)
);

-- ✅ INDEXES for Dependency Resolution
CREATE INDEX idx_task_dependencies_task_id ON task_dependencies(task_id);
CREATE INDEX idx_task_dependencies_depends_on ON task_dependencies(depends_on_task_id);

-- ===============================
-- AUDIT LOG TABLE
-- ===============================

CREATE TABLE agent_audit_log (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(agent_id),
    task_id BIGINT REFERENCES tasks(task_id),
    action VARCHAR(100) NOT NULL,
    details TEXT, -- JSON format
    user_id BIGINT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ✅ INDEXES for Audit Querying
CREATE INDEX idx_agent_audit_log_agent_id ON agent_audit_log(agent_id);
CREATE INDEX idx_agent_audit_log_task_id ON agent_audit_log(task_id);
CREATE INDEX idx_agent_audit_log_action ON agent_audit_log(action);
CREATE INDEX idx_agent_audit_log_user_id ON agent_audit_log(user_id);
CREATE INDEX idx_agent_audit_log_created_at ON agent_audit_log(created_at);

-- ===============================
-- TRIGGERS FOR AUTOMATIC UPDATES
-- ===============================

-- ✅ TRIGGER: Auto-update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_agents_updated_at 
    BEFORE UPDATE ON agents 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at 
    BEFORE UPDATE ON tasks 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ===============================
-- VIEWS FOR COMMON QUERIES
-- ===============================

-- ✅ VIEW: Active agents with current load
CREATE VIEW active_agents_with_load AS
SELECT 
    a.agent_id,
    a.agent_name,
    a.agent_type,
    a.status,
    a.current_load,
    a.max_concurrent_tasks,
    ROUND(a.current_load::numeric / a.max_concurrent_tasks::numeric * 100, 2) as load_percentage,
    a.success_rate,
    a.average_response_time,
    a.total_tasks_completed,
    a.last_heartbeat
FROM agents a
WHERE a.status IN ('ACTIVE', 'IDLE', 'BUSY')
ORDER BY load_percentage ASC, a.success_rate DESC;

-- ✅ VIEW: Task queue with priority ordering
CREATE VIEW pending_tasks_queue AS
SELECT 
    t.task_id,
    t.task_name,
    t.task_type,
    t.priority,
    t.assigned_agent_type,
    t.estimated_duration,
    t.retry_count,
    t.created_at,
    EXTRACT(EPOCH FROM (NOW() - t.created_at)) as wait_time_seconds
FROM tasks t
WHERE t.status = 'PENDING'
ORDER BY 
    CASE t.priority 
        WHEN 'CRITICAL' THEN 1
        WHEN 'HIGH' THEN 2
        WHEN 'MEDIUM' THEN 3
        WHEN 'LOW' THEN 4
        ELSE 5
    END,
    t.created_at ASC;

-- ✅ VIEW: Agent performance summary
CREATE VIEW agent_performance_summary AS
SELECT 
    a.agent_id,
    a.agent_name,
    a.agent_type,
    a.status,
    a.total_tasks_completed,
    a.success_rate,
    a.average_response_time,
    COUNT(t.task_id) as active_tasks,
    COALESCE(AVG(CASE WHEN t.status = 'COMPLETED' AND t.actual_duration IS NOT NULL THEN t.actual_duration END), 0) as avg_task_duration
FROM agents a
LEFT JOIN tasks t ON a.agent_id = t.agent_id AND t.status IN ('IN_PROGRESS', 'PENDING')
GROUP BY a.agent_id, a.agent_name, a.agent_type, a.status, a.total_tasks_completed, a.success_rate, a.average_response_time
ORDER BY a.success_rate DESC, a.average_response_time ASC;

-- ===============================
-- INITIAL DATA
-- ===============================

-- ✅ SAMPLE: Create system agent types for testing
INSERT INTO agents (agent_name, agent_type, description, status, max_concurrent_tasks, user_id) VALUES
('system-market-analyzer', 'MARKET_ANALYSIS', 'System market analysis agent for testing', 'ACTIVE', 10, 1),
('system-portfolio-manager', 'PORTFOLIO_MANAGEMENT', 'System portfolio management agent for testing', 'ACTIVE', 5, 1),
('system-trade-executor', 'TRADING_EXECUTION', 'System trade execution agent for testing', 'ACTIVE', 20, 1),
('system-risk-manager', 'RISK_MANAGEMENT', 'System risk management agent for testing', 'ACTIVE', 3, 1);

-- ✅ SAMPLE: Create system capabilities
INSERT INTO agent_capabilities (agent_id, capability) VALUES
(1, 'TECHNICAL_ANALYSIS'),
(1, 'FUNDAMENTAL_ANALYSIS'),
(2, 'PORTFOLIO_OPTIMIZATION'),
(2, 'REBALANCING'),
(3, 'ORDER_EXECUTION'),
(3, 'MULTI_BROKER_SUPPORT'),
(4, 'RISK_ASSESSMENT'),
(4, 'VAR_CALCULATION');

-- ===============================
-- PERFORMANCE OPTIMIZATIONS
-- ===============================

-- ✅ STATISTICS: Update table statistics for query optimization
ANALYZE agents;
ANALYZE tasks;
ANALYZE agent_capabilities;
ANALYZE agent_performance_metrics;
ANALYZE task_dependencies;
ANALYZE agent_audit_log;