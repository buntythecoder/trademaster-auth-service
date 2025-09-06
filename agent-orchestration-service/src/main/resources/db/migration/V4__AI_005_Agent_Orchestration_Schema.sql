-- AI-005: Agent Orchestration Engine - Complete Schema Implementation
-- Comprehensive schema for AI-005 Agent Orchestration Engine specification
-- Implements all required tables for agent lifecycle, communication, workflows, and performance

-- ===============================
-- AGENT WORKFLOWS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS agent_workflows (
    id BIGSERIAL PRIMARY KEY,
    workflow_name VARCHAR(255) NOT NULL,
    workflow_version VARCHAR(50) NOT NULL,
    workflow_definition JSONB NOT NULL,
    workflow_status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(workflow_name, workflow_version),
    CONSTRAINT chk_workflow_status CHECK (workflow_status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED'))
);

CREATE INDEX IF NOT EXISTS idx_workflow_name ON agent_workflows(workflow_name);
CREATE INDEX IF NOT EXISTS idx_workflow_status ON agent_workflows(workflow_status);

-- ===============================
-- WORKFLOW EXECUTIONS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS workflow_executions (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(255) UNIQUE NOT NULL,
    workflow_id BIGINT REFERENCES agent_workflows(id),
    trigger_type VARCHAR(50) NOT NULL,
    trigger_data JSONB,
    status VARCHAR(20) DEFAULT 'RUNNING',
    current_step VARCHAR(255),
    step_results JSONB,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    total_duration_ms INTEGER,
    error_details JSONB,
    CONSTRAINT chk_execution_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'PAUSED'))
);

CREATE INDEX IF NOT EXISTS idx_execution_workflow ON workflow_executions(workflow_id);
CREATE INDEX IF NOT EXISTS idx_execution_status ON workflow_executions(status);
CREATE INDEX IF NOT EXISTS idx_execution_started ON workflow_executions(started_at);

-- ===============================
-- AGENT COMMUNICATIONS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS agent_communications (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) UNIQUE NOT NULL,
    sender_agent_id BIGINT REFERENCES agents(agent_id),
    receiver_agent_id BIGINT REFERENCES agents(agent_id),
    message_type VARCHAR(50) NOT NULL,
    message_data JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'SENT',
    priority INTEGER DEFAULT 5,
    sent_at TIMESTAMP DEFAULT NOW(),
    delivered_at TIMESTAMP,
    acknowledged_at TIMESTAMP,
    response_message_id VARCHAR(255),
    ttl_seconds INTEGER DEFAULT 3600,
    CONSTRAINT chk_message_status CHECK (status IN ('SENT', 'DELIVERED', 'ACKNOWLEDGED', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_message_priority CHECK (priority >= 1 AND priority <= 10),
    CONSTRAINT chk_ttl_positive CHECK (ttl_seconds > 0)
);

CREATE INDEX IF NOT EXISTS idx_comm_sender ON agent_communications(sender_agent_id);
CREATE INDEX IF NOT EXISTS idx_comm_receiver ON agent_communications(receiver_agent_id);
CREATE INDEX IF NOT EXISTS idx_comm_status ON agent_communications(status);
CREATE INDEX IF NOT EXISTS idx_comm_type ON agent_communications(message_type);
CREATE INDEX IF NOT EXISTS idx_comm_sent_at ON agent_communications(sent_at);

-- ===============================
-- AGENT PERFORMANCE METRICS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS agent_performance (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(agent_id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,6) NOT NULL,
    metric_unit VARCHAR(20),
    recorded_at TIMESTAMP DEFAULT NOW(),
    aggregation_period VARCHAR(20) NOT NULL, -- minute, hour, day
    CONSTRAINT chk_aggregation_period CHECK (aggregation_period IN ('minute', 'hour', 'day'))
);

CREATE INDEX IF NOT EXISTS idx_performance_agent ON agent_performance(agent_id);
CREATE INDEX IF NOT EXISTS idx_performance_metric ON agent_performance(metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_time ON agent_performance(agent_id, metric_name, recorded_at);

-- ===============================
-- RESOURCE ALLOCATIONS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS resource_allocations (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(agent_id) ON DELETE CASCADE,
    resource_type VARCHAR(50) NOT NULL, -- cpu, memory, gpu, disk
    allocated_amount DECIMAL(10,3) NOT NULL,
    allocated_unit VARCHAR(10) NOT NULL,
    utilization_percentage DECIMAL(5,2),
    allocated_at TIMESTAMP DEFAULT NOW(),
    released_at TIMESTAMP,
    cost_per_hour DECIMAL(8,4),
    CONSTRAINT chk_resource_type CHECK (resource_type IN ('cpu', 'memory', 'gpu', 'disk', 'network')),
    CONSTRAINT chk_allocated_amount CHECK (allocated_amount > 0),
    CONSTRAINT chk_utilization CHECK (utilization_percentage >= 0 AND utilization_percentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_resource_agent ON resource_allocations(agent_id);
CREATE INDEX IF NOT EXISTS idx_resource_type ON resource_allocations(resource_type);
CREATE INDEX IF NOT EXISTS idx_resource_allocated_at ON resource_allocations(allocated_at);

-- ===============================
-- AGENT HEALTH MONITORING TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS agent_health_checks (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(agent_id) ON DELETE CASCADE,
    check_type VARCHAR(50) NOT NULL,
    check_result VARCHAR(20) NOT NULL,
    check_details JSONB,
    response_time_ms INTEGER,
    checked_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_health_result CHECK (check_result IN ('HEALTHY', 'DEGRADED', 'UNHEALTHY', 'TIMEOUT', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_health_agent ON agent_health_checks(agent_id);
CREATE INDEX IF NOT EXISTS idx_health_type ON agent_health_checks(check_type);
CREATE INDEX IF NOT EXISTS idx_health_result ON agent_health_checks(check_result);
CREATE INDEX IF NOT EXISTS idx_health_checked_at ON agent_health_checks(checked_at);

-- ===============================
-- TASK QUEUE MANAGEMENT TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS task_queue_management (
    id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(100) NOT NULL,
    queue_type VARCHAR(50) NOT NULL,
    queue_config JSONB,
    current_size INTEGER DEFAULT 0,
    max_size INTEGER DEFAULT 1000,
    processing_rate DECIMAL(10,3),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_queue_type CHECK (queue_type IN ('PRIORITY', 'FIFO', 'LIFO', 'WEIGHTED', 'CUSTOM')),
    CONSTRAINT chk_queue_size CHECK (current_size >= 0 AND max_size > 0)
);

CREATE INDEX IF NOT EXISTS idx_queue_name ON task_queue_management(queue_name);
CREATE INDEX IF NOT EXISTS idx_queue_type ON task_queue_management(queue_type);

-- ===============================
-- AGENT REGISTRY METADATA TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS agent_registry_metadata (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(agent_id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value JSONB NOT NULL,
    metadata_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(agent_id, metadata_key)
);

CREATE INDEX IF NOT EXISTS idx_metadata_agent ON agent_registry_metadata(agent_id);
CREATE INDEX IF NOT EXISTS idx_metadata_key ON agent_registry_metadata(metadata_key);
CREATE INDEX IF NOT EXISTS idx_metadata_type ON agent_registry_metadata(metadata_type);

-- ===============================
-- ORCHESTRATION EVENTS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS orchestration_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_source VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    correlation_id VARCHAR(255),
    agent_id BIGINT REFERENCES agents(agent_id),
    task_id BIGINT REFERENCES tasks(task_id),
    workflow_execution_id BIGINT REFERENCES workflow_executions(id),
    event_timestamp TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP,
    processing_status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT chk_processing_status CHECK (processing_status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_event_type ON orchestration_events(event_type);
CREATE INDEX IF NOT EXISTS idx_event_source ON orchestration_events(event_source);
CREATE INDEX IF NOT EXISTS idx_event_correlation ON orchestration_events(correlation_id);
CREATE INDEX IF NOT EXISTS idx_event_agent ON orchestration_events(agent_id);
CREATE INDEX IF NOT EXISTS idx_event_task ON orchestration_events(task_id);
CREATE INDEX IF NOT EXISTS idx_event_timestamp ON orchestration_events(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_event_status ON orchestration_events(processing_status);

-- ===============================
-- LOAD BALANCING RULES TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS load_balancing_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) UNIQUE NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    rule_config JSONB NOT NULL,
    priority INTEGER DEFAULT 100,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_load_balancing_rule_type CHECK (rule_type IN ('ROUND_ROBIN', 'WEIGHTED', 'LEAST_LOADED', 'CAPABILITY_BASED', 'PERFORMANCE_BASED', 'CUSTOM'))
);

CREATE INDEX IF NOT EXISTS idx_load_rule_type ON load_balancing_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_load_rule_priority ON load_balancing_rules(priority);
CREATE INDEX IF NOT EXISTS idx_load_rule_active ON load_balancing_rules(is_active);

-- ===============================
-- AGENT VERSION MANAGEMENT TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS agent_versions (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(agent_id) ON DELETE CASCADE,
    version_number VARCHAR(50) NOT NULL,
    version_config JSONB,
    capabilities JSONB,
    deployment_config JSONB,
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    activated_at TIMESTAMP,
    deprecated_at TIMESTAMP,
    UNIQUE(agent_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_version_agent ON agent_versions(agent_id);
CREATE INDEX IF NOT EXISTS idx_version_number ON agent_versions(version_number);
CREATE INDEX IF NOT EXISTS idx_version_active ON agent_versions(is_active);

-- ===============================
-- ENHANCE EXISTING TABLES
-- ===============================

-- Add orchestration-specific columns to agents table
ALTER TABLE agents ADD COLUMN IF NOT EXISTS version VARCHAR(50) DEFAULT '1.0.0';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS configuration JSONB;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS health_status VARCHAR(20) DEFAULT 'HEALTHY';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS performance_metrics JSONB;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS resource_allocation JSONB;

-- Update agent status constraint for orchestration states
ALTER TABLE agents DROP CONSTRAINT IF EXISTS chk_agent_status;
ALTER TABLE agents ADD CONSTRAINT chk_agent_status 
    CHECK (status IN (
        'INITIALIZING', 'IDLE', 'ACTIVE', 'OVERLOADED', 'MAINTENANCE', 
        'FAILED', 'SHUTDOWN', 'STARTING', 'STOPPING', 'BUSY', 'ERROR', 'UNRESPONSIVE'
    ));

-- Update health status constraint
ALTER TABLE agents ADD CONSTRAINT chk_health_status 
    CHECK (health_status IN ('HEALTHY', 'DEGRADED', 'UNHEALTHY', 'UNKNOWN'));

-- Add orchestration-specific columns to tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS timeout_at TIMESTAMP;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS workflow_execution_id BIGINT REFERENCES workflow_executions(id);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS parent_task_id BIGINT REFERENCES tasks(task_id);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS dependency_tasks JSONB;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS execution_context JSONB;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS performance_metrics JSONB;

-- ===============================
-- VIEWS FOR ORCHESTRATION MONITORING
-- ===============================

-- Agent Health Summary View
CREATE OR REPLACE VIEW agent_health_summary AS
SELECT 
    a.agent_id,
    a.agent_name,
    a.agent_type,
    a.status,
    a.health_status,
    a.current_load,
    a.max_concurrent_tasks,
    a.success_rate,
    a.total_tasks_completed,
    a.last_heartbeat,
    COUNT(DISTINCT t.task_id) as active_tasks,
    COALESCE(AVG(ahc.response_time_ms), 0) as avg_health_response_time
FROM agents a
LEFT JOIN tasks t ON a.agent_id = t.agent_id AND t.status IN ('IN_PROGRESS', 'QUEUED')
LEFT JOIN agent_health_checks ahc ON a.agent_id = ahc.agent_id 
    AND ahc.checked_at > NOW() - INTERVAL '5 minutes'
GROUP BY a.agent_id, a.agent_name, a.agent_type, a.status, a.health_status,
         a.current_load, a.max_concurrent_tasks, a.success_rate, 
         a.total_tasks_completed, a.last_heartbeat;

-- Task Queue Statistics View
CREATE OR REPLACE VIEW task_queue_statistics AS
SELECT 
    t.priority,
    t.status,
    COUNT(*) as task_count,
    AVG(EXTRACT(EPOCH FROM (COALESCE(t.completed_at, NOW()) - t.created_at))) as avg_duration_seconds,
    MIN(t.created_at) as oldest_task,
    MAX(t.created_at) as newest_task
FROM tasks t
GROUP BY t.priority, t.status;

-- Orchestration Performance View
CREATE OR REPLACE VIEW orchestration_performance AS
SELECT 
    COUNT(DISTINCT a.agent_id) as total_agents,
    COUNT(DISTINCT CASE WHEN a.status = 'ACTIVE' THEN a.agent_id END) as active_agents,
    COUNT(DISTINCT CASE WHEN a.status = 'BUSY' THEN a.agent_id END) as busy_agents,
    COUNT(DISTINCT CASE WHEN a.health_status = 'HEALTHY' THEN a.agent_id END) as healthy_agents,
    COUNT(DISTINCT t.task_id) as total_tasks,
    COUNT(DISTINCT CASE WHEN t.status = 'PENDING' THEN t.task_id END) as pending_tasks,
    COUNT(DISTINCT CASE WHEN t.status = 'IN_PROGRESS' THEN t.task_id END) as active_tasks,
    COUNT(DISTINCT CASE WHEN t.status = 'COMPLETED' THEN t.task_id END) as completed_tasks,
    COUNT(DISTINCT we.id) as active_workflows,
    AVG(a.success_rate) as avg_agent_success_rate,
    AVG(a.current_load::decimal / a.max_concurrent_tasks) * 100 as avg_agent_utilization
FROM agents a
FULL OUTER JOIN tasks t ON TRUE
FULL OUTER JOIN workflow_executions we ON we.status = 'RUNNING';

-- ===============================
-- FUNCTIONS FOR ORCHESTRATION
-- ===============================

-- Function to get optimal agent for task
CREATE OR REPLACE FUNCTION get_optimal_agent_for_task(
    p_required_capabilities TEXT[],
    p_agent_type TEXT DEFAULT NULL
) RETURNS TABLE (
    agent_id BIGINT,
    agent_name VARCHAR(100),
    load_score DECIMAL,
    capability_match DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        a.agent_id,
        a.agent_name,
        (a.current_load::decimal / a.max_concurrent_tasks) as load_score,
        (SELECT COUNT(*)::decimal / array_length(p_required_capabilities, 1) 
         FROM unnest(p_required_capabilities) cap
         WHERE cap::TEXT = ANY(
             SELECT unnest(array_agg(ac.capability::TEXT))
             FROM agent_capabilities ac 
             WHERE ac.agent_id = a.agent_id
         )) as capability_match
    FROM agents a
    WHERE a.status = 'ACTIVE'
      AND a.health_status = 'HEALTHY'
      AND a.current_load < a.max_concurrent_tasks
      AND (p_agent_type IS NULL OR a.agent_type::TEXT = p_agent_type)
    ORDER BY capability_match DESC, load_score ASC, a.success_rate DESC
    LIMIT 10;
END;
$$ LANGUAGE plpgsql;

-- Function to record agent performance metric
CREATE OR REPLACE FUNCTION record_agent_performance(
    p_agent_id BIGINT,
    p_metric_name VARCHAR(100),
    p_metric_value DECIMAL,
    p_metric_unit VARCHAR(20) DEFAULT NULL,
    p_aggregation_period VARCHAR(20) DEFAULT 'minute'
) RETURNS VOID AS $$
BEGIN
    INSERT INTO agent_performance (
        agent_id, metric_name, metric_value, metric_unit, aggregation_period
    ) VALUES (
        p_agent_id, p_metric_name, p_metric_value, p_metric_unit, p_aggregation_period
    );
END;
$$ LANGUAGE plpgsql;

-- ===============================
-- TRIGGERS FOR ORCHESTRATION
-- ===============================

-- Trigger to update agent updated_at timestamp
CREATE OR REPLACE FUNCTION update_agent_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER agent_update_timestamp
    BEFORE UPDATE ON agents
    FOR EACH ROW
    EXECUTE FUNCTION update_agent_timestamp();

-- Trigger to automatically create orchestration event
CREATE OR REPLACE FUNCTION create_orchestration_event()
RETURNS TRIGGER AS $$
DECLARE
    event_type_name TEXT;
    event_data_json JSONB;
BEGIN
    -- Determine event type based on table and operation
    IF TG_TABLE_NAME = 'agents' THEN
        IF TG_OP = 'INSERT' THEN
            event_type_name = 'AGENT_REGISTERED';
        ELSIF TG_OP = 'UPDATE' AND OLD.status != NEW.status THEN
            event_type_name = 'AGENT_STATUS_CHANGED';
        ELSE
            RETURN NEW;
        END IF;
        event_data_json = to_jsonb(NEW);
    ELSIF TG_TABLE_NAME = 'tasks' THEN
        IF TG_OP = 'INSERT' THEN
            event_type_name = 'TASK_CREATED';
        ELSIF TG_OP = 'UPDATE' AND OLD.status != NEW.status THEN
            event_type_name = 'TASK_STATUS_CHANGED';
        ELSE
            RETURN NEW;
        END IF;
        event_data_json = to_jsonb(NEW);
    ELSE
        RETURN NEW;
    END IF;
    
    -- Insert orchestration event
    INSERT INTO orchestration_events (
        event_id, event_type, event_source, event_data, 
        agent_id, task_id, event_timestamp
    ) VALUES (
        'evt_' || extract(epoch from now())::bigint || '_' || floor(random() * 1000)::int,
        event_type_name,
        TG_TABLE_NAME,
        event_data_json,
        CASE WHEN TG_TABLE_NAME = 'agents' THEN NEW.agent_id ELSE NULL END,
        CASE WHEN TG_TABLE_NAME = 'tasks' THEN NEW.task_id ELSE NULL END,
        NOW()
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for orchestration events
CREATE TRIGGER agent_orchestration_events
    AFTER INSERT OR UPDATE ON agents
    FOR EACH ROW
    EXECUTE FUNCTION create_orchestration_event();

CREATE TRIGGER task_orchestration_events
    AFTER INSERT OR UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION create_orchestration_event();

-- ===============================
-- INITIAL DATA AND CONFIGURATION
-- ===============================

-- Insert default load balancing rules
INSERT INTO load_balancing_rules (rule_name, rule_type, rule_config, priority) VALUES
('default_performance_based', 'PERFORMANCE_BASED', '{"weight_success_rate": 0.4, "weight_response_time": 0.3, "weight_load": 0.3}', 100),
('fallback_least_loaded', 'LEAST_LOADED', '{"consider_capability_match": true}', 200),
('emergency_round_robin', 'ROUND_ROBIN', '{"exclude_overloaded": true}', 300)
ON CONFLICT (rule_name) DO NOTHING;

-- Insert default task queues
INSERT INTO task_queue_management (queue_name, queue_type, queue_config, max_size) VALUES
('critical_tasks', 'PRIORITY', '{"priority_levels": ["CRITICAL"], "max_wait_time": 30}', 500),
('high_priority_tasks', 'PRIORITY', '{"priority_levels": ["HIGH"], "max_wait_time": 300}', 1000),
('standard_tasks', 'FIFO', '{"batch_size": 10}', 5000),
('background_tasks', 'WEIGHTED', '{"weight_factors": {"age": 0.6, "priority": 0.4}}', 10000)
ON CONFLICT (queue_name) DO NOTHING;

-- Create schema migration log if not exists
CREATE TABLE IF NOT EXISTS schema_migration_log (
    version VARCHAR(10) PRIMARY KEY,
    description TEXT,
    applied_at TIMESTAMP DEFAULT NOW()
);

-- Log this migration
INSERT INTO schema_migration_log (version, description, applied_at) 
VALUES ('V4', 'AI-005 Agent Orchestration Engine - Complete schema implementation', NOW())
ON CONFLICT (version) DO UPDATE SET 
    applied_at = NOW(), 
    description = EXCLUDED.description;

-- Update table statistics
ANALYZE;

-- Generate completion report
SELECT 
    'AI_005_ORCHESTRATION_SCHEMA' as implementation_phase,
    'COMPLETED' as status,
    (
        SELECT COUNT(*) FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name IN (
            'agent_workflows', 'workflow_executions', 'agent_communications',
            'agent_performance', 'resource_allocations', 'agent_health_checks',
            'task_queue_management', 'agent_registry_metadata', 'orchestration_events',
            'load_balancing_rules', 'agent_versions'
        )
    ) as orchestration_tables_created,
    (
        SELECT COUNT(*) FROM information_schema.views 
        WHERE table_schema = 'public' 
        AND table_name IN ('agent_health_summary', 'task_queue_statistics', 'orchestration_performance')
    ) as orchestration_views_created,
    NOW() as completed_at;