package org.haven.shared.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;

/**
 * Structured audit event for privileged actions.
 *
 * Schema design:
 * - Standardized fields for SIEM ingestion
 * - JSON serializable for structured logging
 * - Immutable record for integrity
 * - Includes both success and denial metadata
 * - Cross-references consent ledger for audit trail
 * - Hash fingerprint for export/data integrity
 *
 * SIEM routing: Events tagged with "pii_audit" for security ops alerting
 * Retention: 7 years per SOX compliance (financial data exports)
 *
 * @param eventId Unique event identifier (UUID)
 * @param eventType Type of privileged action
 * @param outcome Success/denial/error result
 * @param timestamp Event occurrence time (UTC)
 * @param actorId User ID performing action
 * @param actorUsername Username for human readability
 * @param actorRoles User's role assignments at time of action
 * @param resourceType Type of resource accessed (e.g., "RestrictedNote", "ExportJob")
 * @param resourceId Resource identifier (UUID)
 * @param resourceDescription Human-readable resource description
 * @param consentLedgerId Cross-reference to consent ledger entry (nullable)
 * @param justification Required justification text (for audit trail)
 * @param hashFingerprint SHA-256 hash of exported/accessed data (for integrity)
 * @param denialReason Structured reason code for denials
 * @param denialDetails Additional denial context
 * @param requestId Request correlation ID (for distributed tracing)
 * @param sessionId User session identifier
 * @param ipAddress Source IP address
 * @param userAgent Client user agent string
 * @param metadata Additional context (JSON-serializable map)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrivilegedAuditEvent(
        UUID eventId,
        PrivilegedActionType eventType,
        AuditOutcome outcome,
        Instant timestamp,
        UUID actorId,
        String actorUsername,
        List<String> actorRoles,
        String resourceType,
        UUID resourceId,
        String resourceDescription,
        String consentLedgerId,
        String justification,
        String hashFingerprint,
        String denialReason,
        String denialDetails,
        String requestId,
        String sessionId,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata
) {

    /**
     * Builder for constructing audit events with validation
     */
    public static class Builder {
        private UUID eventId = UUID.randomUUID();
        private PrivilegedActionType eventType;
        private AuditOutcome outcome;
        private Instant timestamp = Instant.now();
        private UUID actorId;
        private String actorUsername;
        private List<String> actorRoles = new ArrayList<>();
        private String resourceType;
        private UUID resourceId;
        private String resourceDescription;
        private String consentLedgerId;
        private String justification;
        private String hashFingerprint;
        private String denialReason;
        private String denialDetails;
        private String requestId;
        private String sessionId;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder eventType(PrivilegedActionType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder outcome(AuditOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder actorId(UUID actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorUsername(String actorUsername) {
            this.actorUsername = actorUsername;
            return this;
        }

        public Builder actorRoles(List<String> roles) {
            this.actorRoles = new ArrayList<>(roles);
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(UUID resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder resourceDescription(String description) {
            this.resourceDescription = description;
            return this;
        }

        public Builder consentLedgerId(String consentLedgerId) {
            this.consentLedgerId = consentLedgerId;
            return this;
        }

        public Builder justification(String justification) {
            this.justification = justification;
            return this;
        }

        public Builder hashFingerprint(String hashFingerprint) {
            this.hashFingerprint = hashFingerprint;
            return this;
        }

        public Builder denialReason(String denialReason) {
            this.denialReason = denialReason;
            return this;
        }

        public Builder denialDetails(String denialDetails) {
            this.denialDetails = denialDetails;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public PrivilegedAuditEvent build() {
            // Validation
            if (eventType == null) {
                throw new IllegalArgumentException("Event type is required");
            }
            if (outcome == null) {
                throw new IllegalArgumentException("Outcome is required");
            }
            if (actorId == null) {
                throw new IllegalArgumentException("Actor ID is required");
            }
            if (actorUsername == null || actorUsername.trim().isEmpty()) {
                throw new IllegalArgumentException("Actor username is required");
            }
            if (resourceType == null || resourceType.trim().isEmpty()) {
                throw new IllegalArgumentException("Resource type is required");
            }

            // Validate justification for actions that require it
            if (eventType.requiresJustification() && outcome.isSuccess()) {
                if (justification == null || justification.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Justification is required for " + eventType + " actions"
                    );
                }
            }

            return new PrivilegedAuditEvent(
                eventId,
                eventType,
                outcome,
                timestamp,
                actorId,
                actorUsername,
                List.copyOf(actorRoles),
                resourceType,
                resourceId,
                resourceDescription,
                consentLedgerId,
                justification,
                hashFingerprint,
                denialReason,
                denialDetails,
                requestId,
                sessionId,
                ipAddress,
                userAgent,
                Map.copyOf(metadata)
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Serialize to JSON for structured logging
     */
    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Register Java 8 time module
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // Fallback to toString if JSON serialization fails
            return toString();
        }
    }

    /**
     * Get SIEM routing tag for this event
     */
    public String getSiemTag() {
        return eventType.getSiemTag();
    }

    /**
     * Check if this event represents a security concern (denial or error)
     */
    public boolean isSecurityConcern() {
        return outcome.isDenial() || outcome.isError();
    }

    /**
     * Get severity level for alerting
     */
    public String getSeverity() {
        return eventType.getSeverity();
    }
}
