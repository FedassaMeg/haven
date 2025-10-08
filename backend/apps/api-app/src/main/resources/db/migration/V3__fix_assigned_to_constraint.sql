-- ============================================================================
-- V3: Fix assigned_to column constraints
-- ============================================================================

-- Drop the foreign key constraint on cases.assigned_to
-- The domain model treats assignee IDs as strings, not UUID references
ALTER TABLE haven.cases DROP CONSTRAINT IF EXISTS cases_assigned_to_fkey;

-- Update the column to handle NULL values properly and ensure it's varchar
-- This allows the domain model to store arbitrary string assignee IDs
ALTER TABLE haven.cases ALTER COLUMN assigned_to TYPE VARCHAR(255);