-- Initialize TradeMaster Authentication Database
-- This script runs automatically when PostgreSQL container starts

-- Create database if it doesn't exist (handled by POSTGRES_DB environment variable)
-- The database 'trademaster_auth' is created automatically by the postgres image

-- Create user if it doesn't exist (handled by POSTGRES_USER environment variable)
-- The user 'trademaster_user' is created automatically by the postgres image

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE trademaster_auth TO trademaster_user;

-- Set timezone
SET timezone = 'UTC';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Log initialization
SELECT 'TradeMaster Authentication Database initialized successfully' AS status;