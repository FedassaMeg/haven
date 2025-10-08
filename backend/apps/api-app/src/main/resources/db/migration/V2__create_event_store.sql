-- ============================================================================
-- V2: Create Event Store Schema for Event Sourcing
-- ============================================================================

-- Create event store schema
CREATE SCHEMA IF NOT EXISTS event_store;
SET search_path TO event_store, public;

-- Ensure UUID extension is available (in case it's not in search path)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Event Store Tables
-- ============================================================================

-- Domain events table
CREATE TABLE IF NOT EXISTS domain_events (
    id UUID PRIMARY KEY DEFAULT haven.uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    event_data JSONB NOT NULL,
    metadata JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID,
    correlation_id UUID,
    causation_id UUID,
    sequence_number BIGSERIAL,
    UNIQUE(aggregate_id, event_version)
);

-- Event snapshots for performance
CREATE TABLE IF NOT EXISTS aggregate_snapshots (
    id UUID PRIMARY KEY DEFAULT haven.uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    snapshot_data JSONB NOT NULL,
    version INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(aggregate_id, version)
);

-- Event projections tracking
CREATE TABLE IF NOT EXISTS projection_checkpoints (
    projection_name VARCHAR(255) PRIMARY KEY,
    last_processed_position BIGINT NOT NULL DEFAULT 0,
    last_processed_timestamp TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Command tracking for idempotency
CREATE TABLE IF NOT EXISTS processed_commands (
    command_id UUID PRIMARY KEY,
    command_type VARCHAR(255) NOT NULL,
    aggregate_id UUID,
    payload JSONB,
    result JSONB,
    status VARCHAR(50) NOT NULL, -- 'SUCCESS', 'FAILED', 'PENDING'
    error_message TEXT,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Saga/Process Manager state
CREATE TABLE IF NOT EXISTS sagas (
    id UUID PRIMARY KEY DEFAULT haven.uuid_generate_v4(),
    saga_type VARCHAR(255) NOT NULL,
    saga_data JSONB NOT NULL,
    status VARCHAR(50) NOT NULL, -- 'ACTIVE', 'COMPLETED', 'FAILED', 'COMPENSATING'
    current_step VARCHAR(255),
    correlation_id UUID,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Outbox pattern for reliable messaging
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT haven.uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    destination VARCHAR(255), -- Topic/Queue name
    status VARCHAR(50) DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'FAILED'
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Domain events indexes
CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate ON domain_events(aggregate_id, event_version);
CREATE INDEX IF NOT EXISTS idx_domain_events_type ON domain_events(aggregate_type, event_type);
CREATE INDEX IF NOT EXISTS idx_domain_events_occurred_at ON domain_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_domain_events_sequence ON domain_events(sequence_number);
CREATE INDEX IF NOT EXISTS idx_domain_events_correlation ON domain_events(correlation_id) WHERE correlation_id IS NOT NULL;

-- Snapshots indexes
CREATE INDEX IF NOT EXISTS idx_snapshots_aggregate ON aggregate_snapshots(aggregate_id, version DESC);

-- Commands indexes
CREATE INDEX IF NOT EXISTS idx_commands_aggregate ON processed_commands(aggregate_id) WHERE aggregate_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_commands_created_at ON processed_commands(created_at);

-- Sagas indexes
CREATE INDEX IF NOT EXISTS idx_sagas_type_status ON sagas(saga_type, status);
CREATE INDEX IF NOT EXISTS idx_sagas_correlation ON sagas(correlation_id) WHERE correlation_id IS NOT NULL;

-- Outbox indexes
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status, next_retry_at) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox_events(created_at);

-- ============================================================================
-- Functions and Triggers
-- ============================================================================

-- Function to prevent event modification
CREATE OR REPLACE FUNCTION prevent_event_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Domain events are immutable and cannot be modified';
END;
$$ LANGUAGE plpgsql;

-- Trigger to prevent updates and deletes on domain events
DROP TRIGGER IF EXISTS prevent_domain_events_update ON domain_events;
CREATE TRIGGER prevent_domain_events_update
    BEFORE UPDATE ON domain_events
    FOR EACH ROW EXECUTE FUNCTION prevent_event_modification();

DROP TRIGGER IF EXISTS prevent_domain_events_delete ON domain_events;
CREATE TRIGGER prevent_domain_events_delete
    BEFORE DELETE ON domain_events
    FOR EACH ROW EXECUTE FUNCTION prevent_event_modification();

-- Function to update projection checkpoints
CREATE OR REPLACE FUNCTION update_projection_checkpoint(
    p_projection_name VARCHAR(255),
    p_position BIGINT
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO projection_checkpoints (projection_name, last_processed_position, last_processed_timestamp, updated_at)
    VALUES (p_projection_name, p_position, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (projection_name)
    DO UPDATE SET
        last_processed_position = p_position,
        last_processed_timestamp = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function to retry failed outbox events
CREATE OR REPLACE FUNCTION retry_failed_outbox_events()
RETURNS INTEGER AS $$
DECLARE
    retry_count INTEGER := 0;
BEGIN
    UPDATE outbox_events
    SET status = 'PENDING',
        retry_count = retry_count + 1,
        next_retry_at = CURRENT_TIMESTAMP + (INTERVAL '1 minute' * POWER(2, retry_count))
    WHERE status = 'FAILED'
      AND retry_count < max_retries
      AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP);
    
    GET DIAGNOSTICS retry_count = ROW_COUNT;
    RETURN retry_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Views for Querying
-- ============================================================================

-- View for latest aggregate states
DROP VIEW IF EXISTS latest_aggregate_versions;
CREATE VIEW latest_aggregate_versions AS
SELECT
    aggregate_id,
    aggregate_type,
    MAX(event_version) as latest_version,
    MAX(occurred_at) as last_modified
FROM domain_events
GROUP BY aggregate_id, aggregate_type;

-- View for active sagas
DROP VIEW IF EXISTS active_sagas;
CREATE VIEW active_sagas AS
SELECT *
FROM sagas
WHERE status IN ('ACTIVE', 'COMPENSATING');

-- View for pending outbox events
DROP VIEW IF EXISTS pending_outbox;
CREATE VIEW pending_outbox AS
SELECT *
FROM outbox_events
WHERE status = 'PENDING'
  AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)
ORDER BY created_at;

-- ============================================================================
-- Permissions
-- ============================================================================

-- Grant permissions to application role
GRANT ALL ON SCHEMA event_store TO haven_app;
GRANT ALL ON ALL TABLES IN SCHEMA event_store TO haven_app;
GRANT ALL ON ALL SEQUENCES IN SCHEMA event_store TO haven_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA event_store TO haven_app;

-- Comments for documentation
COMMENT ON TABLE domain_events IS 'Immutable event store for event sourcing pattern';
COMMENT ON TABLE aggregate_snapshots IS 'Snapshots for aggregate performance optimization';
COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable event publishing';
COMMENT ON TABLE sagas IS 'Saga/Process manager state for long-running transactions';