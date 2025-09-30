-- ==================================
-- V26: Coordinated Entry Consent-Aware Packet Structure
-- Adds hashed identifiers and consent metadata for CE payloads
-- ==================================

-- Hash algorithm enum for CE packets
DO 40877 BEGIN
    CREATE TYPE ce_hash_algorithm AS ENUM (
        'SHA256_SALT',
        'BCRYPT'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END 40877;

-- Share scope enum with explicit VAWA-aware boundaries
DO 40877 BEGIN
    CREATE TYPE ce_share_scope AS ENUM (
        'COC_COORDINATED_ENTRY',
        'HMIS_PARTICIPATION',
        'BY_NAME_LIST',
        'VAWA_RESTRICTED_PARTNERS',
        'SYSTEM_PERFORMANCE',
        'ADMIN_AUDIT'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END 40877;

-- Create consent-aware packet table
CREATE TABLE IF NOT EXISTS ce_packets (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    enrollment_id UUID,
    consent_id UUID NOT NULL,
    client_hash VARCHAR(128) NOT NULL,
    hash_algorithm ce_hash_algorithm NOT NULL DEFAULT 'SHA256_SALT',
    hash_salt BYTEA NOT NULL,
    hash_iterations INTEGER NOT NULL DEFAULT 0,
    consent_status VARCHAR(50) NOT NULL,
    consent_version BIGINT NOT NULL DEFAULT 0,
    consent_effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consent_expires_at TIMESTAMP WITH TIME ZONE,
    allowed_share_scopes ce_share_scope[] NOT NULL DEFAULT ARRAY['COC_COORDINATED_ENTRY'],
    encryption_scheme VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encryption_key_id VARCHAR(100) NOT NULL,
    encryption_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    encryption_tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    packet_checksum VARCHAR(128) NOT NULL,
    ledger_entry_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ce_packets_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_ce_packets_enrollment FOREIGN KEY (enrollment_id) REFERENCES program_enrollments(id) ON DELETE SET NULL,
    CONSTRAINT fk_ce_packets_consent FOREIGN KEY (consent_id) REFERENCES haven.client_consents(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ce_packets_consent_status CHECK (consent_status IN ('GRANTED', 'REVOKED', 'EXPIRED', 'PENDING', 'DENIED'))
);

-- Ensure uniqueness of hashed identity per consent version
CREATE UNIQUE INDEX IF NOT EXISTS idx_ce_packets_client_hash_version
    ON ce_packets(client_hash, consent_version);

-- Index share scopes for analytics and enforcement
CREATE INDEX IF NOT EXISTS idx_ce_packets_share_scope
    ON ce_packets USING GIN (allowed_share_scopes);

-- Attach CE packets to assessments/events
ALTER TABLE ce_assessments
    ADD COLUMN IF NOT EXISTS packet_id UUID,
    ADD COLUMN IF NOT EXISTS consent_ledger_id UUID,
    ADD COLUMN IF NOT EXISTS consent_scope ce_share_scope[];

ALTER TABLE ce_events
    ADD COLUMN IF NOT EXISTS packet_id UUID,
    ADD COLUMN IF NOT EXISTS consent_ledger_id UUID,
    ADD COLUMN IF NOT EXISTS consent_scope ce_share_scope[];

ALTER TABLE ce_assessments
    ADD CONSTRAINT fk_ce_assessments_packet FOREIGN KEY (packet_id) REFERENCES ce_packets(id) ON DELETE SET NULL;

ALTER TABLE ce_assessments
    ADD CONSTRAINT fk_ce_assessments_consent_ledger FOREIGN KEY (consent_ledger_id)
        REFERENCES haven.consent_ledger(id) ON DELETE SET NULL;

ALTER TABLE ce_events
    ADD CONSTRAINT fk_ce_events_packet FOREIGN KEY (packet_id) REFERENCES ce_packets(id) ON DELETE SET NULL;

ALTER TABLE ce_events
    ADD CONSTRAINT fk_ce_events_consent_ledger FOREIGN KEY (consent_ledger_id)
        REFERENCES haven.consent_ledger(id) ON DELETE SET NULL;

-- Function to maintain updated_at timestamp
CREATE OR REPLACE FUNCTION touch_ce_packets_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ce_packets_touch ON ce_packets;
CREATE TRIGGER trg_ce_packets_touch
    BEFORE UPDATE ON ce_packets
    FOR EACH ROW
    EXECUTE FUNCTION touch_ce_packets_updated_at();

-- Comments for data dictionary linkage
COMMENT ON TABLE ce_packets IS 'Consent-aware Coordinated Entry packet snapshots with hashed identifiers and encryption metadata';
COMMENT ON COLUMN ce_packets.client_hash IS 'Salted hash of HMIS PersonalID or comparable identifier';
COMMENT ON COLUMN ce_packets.allowed_share_scopes IS 'Authorized downstream sharing scopes enforced at runtime';
COMMENT ON COLUMN ce_packets.encryption_metadata IS 'Metadata tags capturing encryption version, key wrap, and consent flags';
COMMENT ON COLUMN ce_assessments.packet_id IS 'Consent-scoped CE packet binding assessment payload';
COMMENT ON COLUMN ce_events.packet_id IS 'Consent-scoped CE packet binding event/referral payload';
