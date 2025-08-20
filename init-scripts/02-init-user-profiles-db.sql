-- Initialize TradeMaster User Profile Database
-- This script runs automatically when PostgreSQL container starts

-- Create the user profile database
CREATE DATABASE trademaster_profiles WITH OWNER = trademaster_user;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE trademaster_profiles TO trademaster_user;

-- Connect to the new database and set it up
\c trademaster_profiles;

-- Set timezone
SET timezone = 'UTC';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Log initialization
SELECT 'TradeMaster User Profile Database initialized successfully' AS status;