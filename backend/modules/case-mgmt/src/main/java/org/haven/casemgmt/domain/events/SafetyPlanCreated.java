package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SafetyPlanCreated(
    UUID safetyPlanId,
    UUID clientId,
    UUID caseId,
    String planTitle,
    List<String> safetyStrategies,
    List<String> warningSignsIdentified,
    List<String> resourcesIdentified,
    String emergencyContact,
    String emergencyContactPhone,
    String safeLocation,
    String createdBy,
    Instant occurredAt
) implements DomainEvent {
    
    public SafetyPlanCreated {
        if (safetyPlanId == null) throw new IllegalArgumentException("Safety plan ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (planTitle == null || planTitle.trim().isEmpty()) throw new IllegalArgumentException("Plan title cannot be null or empty");
        if (createdBy == null || createdBy.trim().isEmpty()) throw new IllegalArgumentException("Created by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return safetyPlanId;
    }
    
    @Override
    public String eventType() {
        return "SafetyPlanCreated";
    }
}