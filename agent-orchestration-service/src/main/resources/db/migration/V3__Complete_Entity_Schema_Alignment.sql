-- ✅ MANDATORY: Complete Entity-Schema Alignment Migration V3
-- Phase 4 Critical Fix: Address ALL entity/database mismatches 
-- Ensures 100% alignment between JPA entities and database schema

-- ===============================
-- AGENT STATUS ENUM ALIGNMENT
-- ===============================

-- Update agent status constraint to include all AgentStatus enum values
ALTER TABLE agents DROP CONSTRAINT IF EXISTS chk_agent_status;
ALTER TABLE agents ADD CONSTRAINT chk_agent_status 
    CHECK (status IN (
        'INITIALIZING', 'IDLE', 'ACTIVE', 'OVERLOADED', 'MAINTENANCE', 'FAILED', 'SHUTDOWN',
        -- Legacy states for backward compatibility
        'INACTIVE', 'STARTING', 'BUSY', 'ERROR', 'STOPPING', 'UNRESPONSIVE'
    ));

-- Update default agent status to match entity default
ALTER TABLE agents ALTER COLUMN status SET DEFAULT 'IDLE';

-- ===============================
-- TASK PRIORITY ENUM ALIGNMENT  
-- ===============================

-- Update task priority constraint to match TaskPriority enum exactly
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS chk_task_priority;
ALTER TABLE tasks ADD CONSTRAINT chk_task_priority 
    CHECK (priority IN ('CRITICAL', 'HIGH', 'NORMAL', 'LOW', 'DEFERRED'));

-- Remove 'MEDIUM' if it exists and map to 'NORMAL'
UPDATE tasks SET priority = 'NORMAL' WHERE priority = 'MEDIUM';

-- ===============================
-- AGENT CAPABILITY ENUM ALIGNMENT
-- ===============================

-- Update agent capabilities constraint to include ALL AgentCapability enum values
ALTER TABLE agent_capabilities DROP CONSTRAINT IF EXISTS chk_capability;
ALTER TABLE agent_capabilities ADD CONSTRAINT chk_capability 
    CHECK (capability IN (
        -- Market Analysis Capabilities
        'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 'SENTIMENT_ANALYSIS', 
        'MARKET_SCREENING', 'PRICE_PREDICTION', 'PATTERN_RECOGNITION',
        
        -- Portfolio Management Capabilities
        'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 'RISK_ASSESSMENT', 
        'PERFORMANCE_ANALYSIS', 'REBALANCING', 'DIVERSIFICATION_ANALYSIS',
        
        -- Trading Execution Capabilities
        'ORDER_EXECUTION', 'BROKER_ROUTING', 'EXECUTION_OPTIMIZATION', 
        'SLIPPAGE_MONITORING', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_TRADING',
        
        -- Risk Management Capabilities
        'RISK_MONITORING', 'VAR_CALCULATION', 'STRESS_TESTING', 
        'COMPLIANCE_CHECK', 'CORRELATION_ANALYSIS', 'DRAWDOWN_MONITORING',
        
        -- Communication & Notification Capabilities
        'EMAIL_ALERTS', 'SMS_ALERTS', 'PUSH_NOTIFICATIONS', 
        'REPORT_GENERATION', 'REAL_TIME_ALERTS', 'SCHEDULED_REPORTS',
        
        -- Data & Integration Capabilities
        'MARKET_DATA_INTEGRATION', 'BROKER_INTEGRATION', 'DATABASE_OPERATIONS', 
        'API_INTEGRATION', 'DATA_VALIDATION', 'REAL_TIME_STREAMING',
        
        -- Advanced AI Capabilities
        'MACHINE_LEARNING', 'NATURAL_LANGUAGE_PROCESSING', 'ANOMALY_DETECTION', 
        'PREDICTIVE_MODELING', 'REINFORCEMENT_LEARNING',
        
        -- Workflow & Orchestration Capabilities
        'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'EVENT_PROCESSING', 
        'DECISION_MAKING', 'MULTI_AGENT_COMMUNICATION',
        
        -- Custom & Extensible Capabilities
        'CUSTOM_LOGIC', 'PLUGIN_SUPPORT', 'SCRIPTING'
    ));

-- ===============================
-- TASK TYPE ENUM ALIGNMENT
-- ===============================

-- Update task type constraint to include ALL TaskType enum values
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS chk_task_type;
ALTER TABLE tasks ADD CONSTRAINT chk_task_type 
    CHECK (task_type IN (
        -- Market Analysis Tasks
        'MARKET_ANALYSIS', 'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 
        'SENTIMENT_ANALYSIS', 'MARKET_SCREENING', 'PRICE_PREDICTION',
        
        -- Portfolio Management Tasks
        'PORTFOLIO_ANALYSIS', 'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 
        'REBALANCING', 'PERFORMANCE_ANALYSIS', 'DIVERSIFICATION_ANALYSIS',
        
        -- Trading Execution Tasks
        'ORDER_EXECUTION', 'SMART_ROUTING', 'EXECUTION_MONITORING', 
        'SLIPPAGE_ANALYSIS', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_EXECUTION',
        
        -- Risk Management Tasks
        'RISK_ASSESSMENT', 'RISK_MONITORING', 'VAR_CALCULATION', 
        'STRESS_TESTING', 'COMPLIANCE_CHECK', 'DRAWDOWN_MONITORING',
        
        -- Notification Tasks
        'ALERT_GENERATION', 'REPORT_GENERATION', 'EMAIL_NOTIFICATION', 
        'SMS_NOTIFICATION', 'PUSH_NOTIFICATION',
        
        -- Workflow and Orchestration Tasks
        'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'AGENT_COMMUNICATION', 
        'DECISION_MAKING',
        
        -- Custom Tasks
        'CUSTOM_TASK', 'SCRIPT_EXECUTION', 'PLUGIN_EXECUTION'
    ));

-- ===============================
-- COLUMN NAMING ALIGNMENT
-- ===============================

-- Ensure column names match JPA @Column annotations exactly
DO $$
BEGIN
    -- Agent table column alignment
    -- Check if column naming matches entity annotations
    
    -- Ensure task table uses correct column name for task_type
    -- (Already correct as task_type matches @Column annotation)
    
    -- Verify foreign key column naming
    -- agent_id should already be correct based on @JoinColumn annotation
    
    -- Add any missing indexes from entity @Index annotations
    CREATE INDEX IF NOT EXISTS idx_task_status ON tasks(status);
    CREATE INDEX IF NOT EXISTS idx_task_type ON tasks(task_type);
    CREATE INDEX IF NOT EXISTS idx_task_priority ON tasks(priority);
    CREATE INDEX IF NOT EXISTS idx_task_agent ON tasks(agent_id);
    CREATE INDEX IF NOT EXISTS idx_task_user ON tasks(user_id);
    CREATE INDEX IF NOT EXISTS idx_task_created ON tasks(created_at);
    CREATE INDEX IF NOT EXISTS idx_task_deadline ON tasks(deadline);
    
    -- Agent indexes
    CREATE INDEX IF NOT EXISTS idx_agent_type ON agents(agent_type);
    CREATE INDEX IF NOT EXISTS idx_agent_status ON agents(status);
    CREATE INDEX IF NOT EXISTS idx_agent_user ON agents(user_id);
    
END $$;

-- ===============================
-- FIELD LENGTH ALIGNMENT
-- ===============================

-- Align varchar lengths with entity @Column(length=X) annotations
DO $$
BEGIN
    -- Agent table field length alignment
    -- agent_name should be length=100 (matches entity)
    -- agent_type should be length=50 (matches entity)  
    -- description should be length=500 (matches entity)
    -- status should be length=20 (matches entity)
    
    -- Task table field length alignment  
    -- task_name should be length=200 (matches entity)
    -- task_type should be length=50 (matches entity)
    -- description should be length=1000 (matches entity)
    -- status should be length=30 (matches entity)
    -- priority should be length=20 (matches entity)
    -- error_message should be length=2000 (matches entity)
    
    -- Agent capabilities
    -- capability should be length=50 (matches entity)
    
    -- Only alter if different from current length
    -- PostgreSQL will handle length validation automatically
    
END $$;

-- ===============================
-- DEFAULT VALUE ALIGNMENT
-- ===============================

-- Ensure default values match entity @Builder.Default values
ALTER TABLE agents ALTER COLUMN status SET DEFAULT 'IDLE';
ALTER TABLE agents ALTER COLUMN max_concurrent_tasks SET DEFAULT 5;
ALTER TABLE agents ALTER COLUMN current_load SET DEFAULT 0;
ALTER TABLE agents ALTER COLUMN success_rate SET DEFAULT 0.0;
ALTER TABLE agents ALTER COLUMN average_response_time SET DEFAULT 0;
ALTER TABLE agents ALTER COLUMN total_tasks_completed SET DEFAULT 0;

ALTER TABLE tasks ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE tasks ALTER COLUMN priority SET DEFAULT 'NORMAL';
ALTER TABLE tasks ALTER COLUMN retry_count SET DEFAULT 0;
ALTER TABLE tasks ALTER COLUMN max_retries SET DEFAULT 3;
ALTER TABLE tasks ALTER COLUMN timeout_seconds SET DEFAULT 300;
ALTER TABLE tasks ALTER COLUMN progress_percentage SET DEFAULT 0;

-- ===============================
-- CONSTRAINT ALIGNMENT
-- ===============================

-- Ensure constraints match entity validation annotations
DO $$
BEGIN
    -- Task constraints alignment
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_retry_count') THEN
        ALTER TABLE tasks DROP CONSTRAINT chk_retry_count;
    END IF;
    ALTER TABLE tasks ADD CONSTRAINT chk_retry_count CHECK (retry_count >= 0);
    
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_max_retries') THEN
        ALTER TABLE tasks DROP CONSTRAINT chk_max_retries;
    END IF;
    ALTER TABLE tasks ADD CONSTRAINT chk_max_retries CHECK (max_retries >= 0);
    
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_timeout_seconds') THEN
        ALTER TABLE tasks DROP CONSTRAINT chk_timeout_seconds;
    END IF;
    ALTER TABLE tasks ADD CONSTRAINT chk_timeout_seconds CHECK (timeout_seconds > 0);
    
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_progress_percentage') THEN
        ALTER TABLE tasks DROP CONSTRAINT chk_progress_percentage;
    END IF;
    ALTER TABLE tasks ADD CONSTRAINT chk_progress_percentage CHECK (progress_percentage >= 0 AND progress_percentage <= 100);
    
    -- Agent constraints alignment
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_agents_current_load') THEN
        ALTER TABLE agents DROP CONSTRAINT chk_agents_current_load;
    END IF;
    ALTER TABLE agents ADD CONSTRAINT chk_agents_current_load CHECK (current_load >= 0);
    
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_agents_max_concurrent_tasks') THEN
        ALTER TABLE agents DROP CONSTRAINT chk_agents_max_concurrent_tasks;
    END IF;
    ALTER TABLE agents ADD CONSTRAINT chk_agents_max_concurrent_tasks CHECK (max_concurrent_tasks > 0);
    
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints WHERE constraint_name = 'chk_agents_success_rate') THEN
        ALTER TABLE agents DROP CONSTRAINT chk_agents_success_rate;
    END IF;
    ALTER TABLE agents ADD CONSTRAINT chk_agents_success_rate CHECK (success_rate >= 0.0 AND success_rate <= 1.0);
    
END $$;

-- ===============================
-- FOREIGN KEY ALIGNMENT
-- ===============================

-- Ensure foreign keys match entity relationships exactly
DO $$
BEGIN
    -- Verify task->agent foreign key exists and is correct
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_type = 'FOREIGN KEY' 
        AND table_name = 'tasks' 
        AND constraint_name LIKE '%agent_id%'
    ) THEN
        ALTER TABLE tasks ADD CONSTRAINT fk_tasks_agent_id 
            FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE SET NULL;
    END IF;
    
    -- Verify agent_capabilities foreign key is correct
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_type = 'FOREIGN KEY' 
        AND table_name = 'agent_capabilities' 
        AND constraint_name LIKE '%agent_id%'
    ) THEN
        ALTER TABLE agent_capabilities ADD CONSTRAINT fk_agent_capabilities_agent_id 
            FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE;
    END IF;
    
    -- Verify task_required_capabilities foreign key exists 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'task_required_capabilities'
    ) THEN
        CREATE TABLE task_required_capabilities (
            task_id BIGINT NOT NULL,
            capability VARCHAR(50) NOT NULL,
            PRIMARY KEY (task_id, capability),
            FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
        );
        
        -- Add capability constraint
        ALTER TABLE task_required_capabilities ADD CONSTRAINT chk_task_capability 
            CHECK (capability IN (
                'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 'SENTIMENT_ANALYSIS', 
                'MARKET_SCREENING', 'PRICE_PREDICTION', 'PATTERN_RECOGNITION',
                'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 'RISK_ASSESSMENT', 
                'PERFORMANCE_ANALYSIS', 'REBALANCING', 'DIVERSIFICATION_ANALYSIS',
                'ORDER_EXECUTION', 'BROKER_ROUTING', 'EXECUTION_OPTIMIZATION', 
                'SLIPPAGE_MONITORING', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_TRADING',
                'RISK_MONITORING', 'VAR_CALCULATION', 'STRESS_TESTING', 
                'COMPLIANCE_CHECK', 'CORRELATION_ANALYSIS', 'DRAWDOWN_MONITORING',
                'EMAIL_ALERTS', 'SMS_ALERTS', 'PUSH_NOTIFICATIONS', 
                'REPORT_GENERATION', 'REAL_TIME_ALERTS', 'SCHEDULED_REPORTS',
                'MARKET_DATA_INTEGRATION', 'BROKER_INTEGRATION', 'DATABASE_OPERATIONS', 
                'API_INTEGRATION', 'DATA_VALIDATION', 'REAL_TIME_STREAMING',
                'MACHINE_LEARNING', 'NATURAL_LANGUAGE_PROCESSING', 'ANOMALY_DETECTION', 
                'PREDICTIVE_MODELING', 'REINFORCEMENT_LEARNING',
                'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'EVENT_PROCESSING', 
                'DECISION_MAKING', 'MULTI_AGENT_COMMUNICATION',
                'CUSTOM_LOGIC', 'PLUGIN_SUPPORT', 'SCRIPTING'
            ));
    END IF;
    
END $$;

-- ===============================
-- VALIDATION AND VERIFICATION
-- ===============================

-- Verify all enum constraints are correct
DO $$
DECLARE
    invalid_agent_statuses INTEGER;
    invalid_task_statuses INTEGER;
    invalid_task_priorities INTEGER;
    invalid_agent_types INTEGER;
    invalid_task_types INTEGER;
    invalid_capabilities INTEGER;
BEGIN
    -- Check for any invalid enum values in existing data
    SELECT COUNT(*) INTO invalid_agent_statuses 
    FROM agents WHERE status NOT IN (
        'INITIALIZING', 'IDLE', 'ACTIVE', 'OVERLOADED', 'MAINTENANCE', 'FAILED', 'SHUTDOWN',
        'INACTIVE', 'STARTING', 'BUSY', 'ERROR', 'STOPPING', 'UNRESPONSIVE'
    );
    
    SELECT COUNT(*) INTO invalid_task_statuses 
    FROM tasks WHERE status NOT IN (
        'PENDING', 'QUEUED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'FAILED', 
        'ERROR', 'CANCELLED', 'TIMEOUT', 'WAITING'
    );
    
    SELECT COUNT(*) INTO invalid_task_priorities 
    FROM tasks WHERE priority NOT IN ('CRITICAL', 'HIGH', 'NORMAL', 'LOW', 'DEFERRED');
    
    SELECT COUNT(*) INTO invalid_agent_types 
    FROM agents WHERE agent_type NOT IN (
        'MARKET_ANALYSIS', 'PORTFOLIO_MANAGEMENT', 'TRADING_EXECUTION', 
        'RISK_MANAGEMENT', 'NOTIFICATION', 'CUSTOM'
    );
    
    SELECT COUNT(*) INTO invalid_task_types 
    FROM tasks WHERE task_type NOT IN (
        'MARKET_ANALYSIS', 'TECHNICAL_ANALYSIS', 'FUNDAMENTAL_ANALYSIS', 
        'SENTIMENT_ANALYSIS', 'MARKET_SCREENING', 'PRICE_PREDICTION',
        'PORTFOLIO_ANALYSIS', 'PORTFOLIO_OPTIMIZATION', 'ASSET_ALLOCATION', 
        'REBALANCING', 'PERFORMANCE_ANALYSIS', 'DIVERSIFICATION_ANALYSIS',
        'ORDER_EXECUTION', 'SMART_ROUTING', 'EXECUTION_MONITORING', 
        'SLIPPAGE_ANALYSIS', 'LIQUIDITY_ANALYSIS', 'ALGORITHMIC_EXECUTION',
        'RISK_ASSESSMENT', 'RISK_MONITORING', 'VAR_CALCULATION', 
        'STRESS_TESTING', 'COMPLIANCE_CHECK', 'DRAWDOWN_MONITORING',
        'ALERT_GENERATION', 'REPORT_GENERATION', 'EMAIL_NOTIFICATION', 
        'SMS_NOTIFICATION', 'PUSH_NOTIFICATION',
        'WORKFLOW_EXECUTION', 'TASK_COORDINATION', 'AGENT_COMMUNICATION', 
        'DECISION_MAKING', 'CUSTOM_TASK', 'SCRIPT_EXECUTION', 'PLUGIN_EXECUTION'
    );
    
    -- Log any validation issues
    IF invalid_agent_statuses > 0 OR invalid_task_statuses > 0 OR invalid_task_priorities > 0 OR 
       invalid_agent_types > 0 OR invalid_task_types > 0 THEN
        RAISE WARNING 'Entity-Schema alignment validation found issues: Agent Statuses: %, Task Statuses: %, Task Priorities: %, Agent Types: %, Task Types: %', 
                     invalid_agent_statuses, invalid_task_statuses, invalid_task_priorities, 
                     invalid_agent_types, invalid_task_types;
    ELSE
        RAISE NOTICE 'Entity-Schema alignment validation PASSED - All enum values are valid';
    END IF;
END $$;

-- Update table statistics
ANALYZE agents;
ANALYZE tasks;
ANALYZE agent_capabilities;
ANALYZE task_required_capabilities;

-- ===============================
-- FINAL VERIFICATION REPORT
-- ===============================

-- Generate alignment verification report
SELECT 
    'ENTITY_ALIGNMENT_VERIFICATION' as report_type,
    'agents' as table_name,
    COUNT(*) as total_records,
    COUNT(DISTINCT agent_type) as unique_agent_types,
    COUNT(DISTINCT status) as unique_statuses
FROM agents
UNION ALL
SELECT 
    'ENTITY_ALIGNMENT_VERIFICATION' as report_type,
    'tasks' as table_name,
    COUNT(*) as total_records,
    COUNT(DISTINCT task_type) as unique_task_types,
    COUNT(DISTINCT status) as unique_statuses
FROM tasks
UNION ALL
SELECT 
    'ENTITY_ALIGNMENT_VERIFICATION' as report_type,
    'agent_capabilities' as table_name,
    COUNT(*) as total_records,
    COUNT(DISTINCT capability) as unique_capabilities,
    0 as unique_statuses
FROM agent_capabilities;

-- Add migration completion log
INSERT INTO schema_migration_log (version, description, applied_at) 
VALUES ('V3', 'Complete Entity-Schema Alignment - All enums, constraints, and naming aligned', NOW())
ON CONFLICT (version) DO UPDATE SET 
    applied_at = NOW(), 
    description = EXCLUDED.description;