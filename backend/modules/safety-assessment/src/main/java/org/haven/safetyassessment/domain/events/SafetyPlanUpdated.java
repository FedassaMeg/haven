package org.haven.safetyassessment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SafetyPlanUpdated(
    UUID assessmentId,
    UUID clientId,
    List<String> safetyStrategies,
    List<String> warningSignsIdentified,
    List<String> resourcesIdentified,
    String emergencyContact,
    String emergencyContactPhone,
    String safeLocation,
    String updatedBy,
    UUID updatedByUserId,
    String updateReason,
    Instant occurredAt
) implements DomainEvent {
    
    public SafetyPlanUpdated {
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return assessmentId;
    }
    
    @Override
    public String eventType() {
        return "SafetyPlanUpdated";
    }
}