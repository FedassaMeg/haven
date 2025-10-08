-- ============================================================================
-- V40a: Extend user_role enum for RBAC
-- ============================================================================
-- Purpose: Add new enum values to user_role type
-- Date: 2025-10-07
-- Note: Split from V40 due to PostgreSQL enum transaction requirements
-- ============================================================================

SET search_path TO haven, public;

-- Add new role types to existing enum
-- Note: PostgreSQL requires enum values to be committed before use
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'CE_INTAKE';
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'DV_ADVOCATE';
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'COMPLIANCE_AUDITOR';
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'EXEC';

-- Note: INTAKE_SPECIALIST, REPORT_VIEWER, EXTERNAL_PARTNER already exist from V1
