-- TradeMaster Database Initialization Script
-- This script creates all required databases for TradeMaster services

-- Create databases for each service
CREATE DATABASE IF NOT EXISTS trademaster_agentos;
CREATE DATABASE IF NOT EXISTS trademaster_broker_auth;
CREATE DATABASE IF NOT EXISTS trademaster_behavioral_ai;
CREATE DATABASE IF NOT EXISTS ml_platform;
CREATE DATABASE IF NOT EXISTS trademaster_market_data;
CREATE DATABASE IF NOT EXISTS trademaster_notifications;
CREATE DATABASE IF NOT EXISTS trademaster_risk_management;
CREATE DATABASE IF NOT EXISTS trademaster_payments;
CREATE DATABASE IF NOT EXISTS mlflow;

-- Create service-specific users with appropriate permissions
-- Agent Orchestration Service
CREATE USER IF NOT EXISTS trademaster_agent WITH ENCRYPTED PASSWORD 'agent_secure_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_agentos TO trademaster_agent;

-- Broker Authentication Service  
CREATE USER IF NOT EXISTS broker_auth_user WITH ENCRYPTED PASSWORD 'broker_auth_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_broker_auth TO broker_auth_user;

-- Behavioral AI Service
CREATE USER IF NOT EXISTS behavioral_ai_user WITH ENCRYPTED PASSWORD 'behavioral_ai_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_behavioral_ai TO behavioral_ai_user;

-- ML Infrastructure Platform
CREATE USER IF NOT EXISTS ml_platform_user WITH ENCRYPTED PASSWORD 'ml_platform_password';
GRANT ALL PRIVILEGES ON DATABASE ml_platform TO ml_platform_user;

-- Market Data Service
CREATE USER IF NOT EXISTS market_data_user WITH ENCRYPTED PASSWORD 'market_data_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_market_data TO market_data_user;

-- Notification Service
CREATE USER IF NOT EXISTS notification_user WITH ENCRYPTED PASSWORD 'notification_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_notifications TO notification_user;

-- Risk Management Service
CREATE USER IF NOT EXISTS risk_mgmt_user WITH ENCRYPTED PASSWORD 'risk_mgmt_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_risk_management TO risk_mgmt_user;

-- Payment Gateway Service
CREATE USER IF NOT EXISTS payment_user WITH ENCRYPTED PASSWORD 'payment_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_payments TO payment_user;

-- MLflow
CREATE USER IF NOT EXISTS mlflow_user WITH ENCRYPTED PASSWORD 'mlflow_password';
GRANT ALL PRIVILEGES ON DATABASE mlflow TO mlflow_user;

-- Enable required extensions
\c trademaster_agentos;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c trademaster_broker_auth;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c trademaster_behavioral_ai;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c ml_platform;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c trademaster_market_data;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c trademaster_notifications;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c trademaster_risk_management;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c trademaster_payments;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c mlflow;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";