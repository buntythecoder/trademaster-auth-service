-- Add risk_level column to portfolios table
-- Version: 2.0.0 (Java 24 + Virtual Threads)
-- Author: TradeMaster Development Team
-- Purpose: Add risk tolerance level column to match Portfolio entity

-- Add risk_level column with default MODERATE
ALTER TABLE portfolios
ADD COLUMN risk_level VARCHAR(20) NOT NULL DEFAULT 'MODERATE';

-- Add CHECK constraint for valid risk level values
ALTER TABLE portfolios
ADD CONSTRAINT chk_risk_level CHECK (risk_level IN ('VERY_LOW', 'LOW', 'MODERATE', 'HIGH', 'VERY_HIGH'));

-- Add comment for documentation
COMMENT ON COLUMN portfolios.risk_level IS 'Portfolio risk tolerance level (VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH)';
