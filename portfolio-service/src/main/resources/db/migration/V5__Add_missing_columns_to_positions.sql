-- Add missing columns to positions table
-- Version: 2.0.0 (Java 24 + Virtual Threads)
-- Author: TradeMaster Development Team
-- Purpose: Add sector and expiry_date columns to match Position entity

-- Add sector column with default UNKNOWN
ALTER TABLE positions
ADD COLUMN sector VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN';

-- Add expiry_date column (nullable for non-derivative positions)
ALTER TABLE positions
ADD COLUMN expiry_date TIMESTAMP WITH TIME ZONE;

-- Create index on expiry_date for efficient querying of expiring positions
CREATE INDEX idx_position_expiry_date ON positions(expiry_date) WHERE expiry_date IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN positions.sector IS 'Industry sector classification (e.g., TECHNOLOGY, FINANCE, HEALTHCARE)';
COMMENT ON COLUMN positions.expiry_date IS 'Expiration date for derivatives (options, futures); NULL for equity positions';
