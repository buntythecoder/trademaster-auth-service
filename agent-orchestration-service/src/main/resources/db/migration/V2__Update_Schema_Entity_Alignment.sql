-- ✅ MANDATORY: Database Schema Update V2
-- Phase 4 Entity Alignment: Update schema to match current JPA entities
-- Addresses gaps between entity definitions and existing schema

-- ===============================
-- AGENT ENTITY UPDATES
-- ===============================

-- Add missing error tracking fields to agents table
DO $$
BEGIN
    -- Add last_error column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'agents' AND column_name = 'last_error'
    ) THEN
        ALTER TABLE agents ADD COLUMN last_error VARCHAR(1000);
    END IF;

    -- Add last_error_timestamp column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'agents' AND column_name = 'last_error_timestamp'
    ) THEN
        ALTER TABLE agents ADD COLUMN last_error_timestamp TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;

-- Update agent status values to match AgentStatus enum
UPDATE agents SET status = 'IDLE' WHERE status = 'INACTIVE';
UPDATE agents SET status = 'ACTIVE' WHERE status = 'STARTING';

-- Add index for error tracking
CREATE INDEX IF NOT EXISTS idx_agents_last_error ON agents(last_error_timestamp) WHERE last_error IS NOT NULL;

-- ===============================
-- TASK ENTITY UPDATES  
-- ===============================

-- Ensure task table has all required columns matching Task.java
DO $$
BEGIN
    -- Verify agent_id column exists with proper foreign key
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tasks' AND column_name = 'agent_id'
    ) THEN
        ALTER TABLE tasks ADD COLUMN agent_id BIGINT REFERENCES agents(agent_id);
    END IF;

    -- Verify assigned_agent_type column exists (for convenience methods)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tasks' AND column_name = 'assigned_agent_type'
    ) THEN
        ALTER TABLE tasks ADD COLUMN assigned_agent_type VARCHAR(50);
    END IF;
END $$;

-- ===============================
-- CONSTRAINT UPDATES
-- ===============================

-- Update agent constraints to match entity validation
DO $$
BEGIN
    -- Update status constraint to match AgentStatus enum
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints 
        WHERE constraint_name = 'chk_agent_status'
    ) THEN
        ALTER TABLE agents DROP CONSTRAINT chk_agent_status;
    END IF;
    
    ALTER TABLE agents ADD CONSTRAINT chk_agent_status 
        CHECK (status IN ('INACTIVE', 'STARTING', 'ACTIVE', 'IDLE', 'BUSY', 'ERROR', 'STOPPING', 'UNRESPONSIVE'));
END $$;

-- Update task constraints to match TaskStatus enum  
DO $$
BEGIN
    -- Update task status constraint
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints 
        WHERE constraint_name = 'chk_task_status'
    ) THEN
        ALTER TABLE tasks DROP CONSTRAINT chk_task_status;
    END IF;
    
    ALTER TABLE tasks ADD CONSTRAINT chk_task_status 
        CHECK (status IN ('PENDING', 'QUEUED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'FAILED', 'ERROR', 'CANCELLED', 'TIMEOUT', 'WAITING'));
END $$;

-- Update task priority constraint to match TaskPriority enum
DO $$
BEGIN
    -- Update task priority constraint
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints 
        WHERE constraint_name = 'chk_task_priority'
    ) THEN
        ALTER TABLE tasks DROP CONSTRAINT chk_task_priority;
    END IF;
    
    ALTER TABLE tasks ADD CONSTRAINT chk_task_priority 
        CHECK (priority IN ('CRITICAL', 'HIGH', 'NORMAL', 'MEDIUM', 'LOW', 'DEFERRED'));
END $$;

-- ===============================
-- INDEX OPTIMIZATIONS
-- ===============================

-- Add missing indexes from entity definitions
CREATE INDEX IF NOT EXISTS idx_agents_status_load ON agents(status, current_load);
CREATE INDEX IF NOT EXISTS idx_tasks_status_priority ON tasks(status, priority);
CREATE INDEX IF NOT EXISTS idx_tasks_agent_status ON tasks(agent_id, status);

-- ===============================
-- PERFORMANCE VIEWS UPDATE
-- ===============================

-- Update agent performance view to include error tracking
DROP VIEW IF EXISTS agent_performance_summary;
CREATE VIEW agent_performance_summary AS
SELECT 
    a.agent_id,
    a.agent_name,
    a.agent_type,
    a.status,
    a.total_tasks_completed,
    a.success_rate,
    a.average_response_time,
    a.current_load,
    a.max_concurrent_tasks,
    ROUND((a.current_load::DECIMAL / a.max_concurrent_tasks::DECIMAL) * 100, 2) as utilization_percentage,
    COUNT(t.task_id) FILTER (WHERE t.status IN ('IN_PROGRESS', 'QUEUED')) as active_tasks,
    COALESCE(AVG(CASE WHEN t.status = 'COMPLETED' AND t.actual_duration_seconds IS NOT NULL THEN t.actual_duration_seconds END), 0) as avg_task_duration_seconds,
    a.last_error,
    a.last_error_timestamp,
    CASE WHEN a.last_error IS NOT NULL THEN true ELSE false END as has_recent_error
FROM agents a
LEFT JOIN tasks t ON a.agent_id = t.agent_id
GROUP BY a.agent_id, a.agent_name, a.agent_type, a.status, a.total_tasks_completed, 
         a.success_rate, a.average_response_time, a.current_load, a.max_concurrent_tasks,
         a.last_error, a.last_error_timestamp
ORDER BY a.success_rate DESC, a.average_response_time ASC;

-- ===============================
-- DATA INTEGRITY CHECKS
-- ===============================

-- Update statistics for query optimizer
ANALYZE agents;
ANALYZE tasks;
ANALYZE agent_capabilities;

-- Verify referential integrity
DO $$
DECLARE
    orphaned_tasks INTEGER;
    invalid_capabilities INTEGER;
BEGIN
    -- Check for orphaned tasks
    SELECT COUNT(*) INTO orphaned_tasks 
    FROM tasks t 
    WHERE t.agent_id IS NOT NULL 
    AND NOT EXISTS (SELECT 1 FROM agents a WHERE a.agent_id = t.agent_id);
    
    IF orphaned_tasks > 0 THEN
        RAISE WARNING 'Found % orphaned tasks with invalid agent references', orphaned_tasks;
    END IF;
    
    -- Check for invalid agent capabilities
    SELECT COUNT(*) INTO invalid_capabilities
    FROM agent_capabilities ac
    WHERE NOT EXISTS (SELECT 1 FROM agents a WHERE a.agent_id = ac.agent_id);
    
    IF invalid_capabilities > 0 THEN
        RAISE WARNING 'Found % orphaned agent capabilities', invalid_capabilities;
    END IF;
    
    RAISE NOTICE 'Schema alignment validation completed. Orphaned tasks: %, Invalid capabilities: %', 
                 orphaned_tasks, invalid_capabilities;
END $$;

-- ===============================
-- COMMENT DOCUMENTATION
-- ===============================

-- Add helpful comments for maintenance
COMMENT ON COLUMN agents.last_error IS 'Latest error message for debugging and monitoring';
COMMENT ON COLUMN agents.last_error_timestamp IS 'Timestamp of the last recorded error';
COMMENT ON VIEW agent_performance_summary IS 'Comprehensive agent performance metrics with error tracking';
COMMENT ON INDEX idx_agents_last_error IS 'Index for querying agents with recent errors';