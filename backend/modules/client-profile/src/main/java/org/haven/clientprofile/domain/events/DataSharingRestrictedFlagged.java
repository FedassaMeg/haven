package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataSharingRestrictedFlagged(
    UUID clientId,
    List<String> restrictedDataTypes,
    CodeableConcept restrictionReason,
    String restrictionLevel,
    List<String> authorizedRoles,
    List<String> authorizedAgencies,
    String flaggedBy,
    UUID flaggedByUserId,
    String restrictionNotes,
    boolean isPermanentRestriction,
    Instant restrictionExpiryDate,
    String legalBasis,
    Instant occurredAt
) implements DomainEvent {
    
    public DataSharingRestrictedFlagged {
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (restrictedDataTypes == null || restrictedDataTypes.isEmpty()) throw new IllegalArgumentException("Restricted data types cannot be null or empty");
        if (restrictionReason == null) throw new IllegalArgumentException("Restriction reason cannot be null");
        if (restrictionLevel == null || restrictionLevel.trim().isEmpty()) throw new IllegalArgumentException("Restriction level cannot be null or empty");
        if (flaggedBy == null || flaggedBy.trim().isEmpty()) throw new IllegalArgumentException("Flagged by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "DataSharingRestrictedFlagged";
    }
}