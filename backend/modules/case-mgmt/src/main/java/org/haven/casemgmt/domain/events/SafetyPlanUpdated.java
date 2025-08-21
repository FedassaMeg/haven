package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SafetyPlanUpdated(
    UUID safetyPlanId,
    UUID clientId,
    List<String> safetyStrategies,
    List<String> warningSignsIdentified,
    List<String> resourcesIdentified,
    String emergencyContact,
    String emergencyContactPhone,
    String safeLocation,
    String codeWord,
    List<String> supportNetwork,
    String legalProtections,
    String childrenSafetyPlan,
    String workplaceSafetyPlan,
    String updatedBy,
    String updateReason,
    Instant occurredAt
) implements DomainEvent {
    
    public SafetyPlanUpdated {
        if (safetyPlanId == null) throw new IllegalArgumentException("Safety plan ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return safetyPlanId;
    }
    
    @Override
    public String eventType() {
        return "SafetyPlanUpdated";
    }
}