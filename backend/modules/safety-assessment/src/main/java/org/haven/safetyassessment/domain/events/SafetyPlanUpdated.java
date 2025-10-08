package org.haven.safetyassessment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SafetyPlanUpdated extends DomainEvent {
    private final UUID assessmentId;
    private final UUID clientId;
    private final List<String> safetyStrategies;
    private final List<String> warningSignsIdentified;
    private final List<String> resourcesIdentified;
    private final String emergencyContact;
    private final String emergencyContactPhone;
    private final String safeLocation;
    private final String updatedBy;
    private final UUID updatedByUserId;
    private final String updateReason;

    public SafetyPlanUpdated(
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
    ) {
        super(assessmentId, occurredAt != null ? occurredAt : Instant.now());
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");

        this.assessmentId = assessmentId;
        this.clientId = clientId;
        this.safetyStrategies = safetyStrategies;
        this.warningSignsIdentified = warningSignsIdentified;
        this.resourcesIdentified = resourcesIdentified;
        this.emergencyContact = emergencyContact;
        this.emergencyContactPhone = emergencyContactPhone;
        this.safeLocation = safeLocation;
        this.updatedBy = updatedBy;
        this.updatedByUserId = updatedByUserId;
        this.updateReason = updateReason;
    }

    // Getter style methods
    public UUID getAssessmentId() { return assessmentId; }
    public UUID assessmentId() { return assessmentId; }

    public UUID getClientId() { return clientId; }
    public UUID clientId() { return clientId; }

    public List<String> getSafetyStrategies() { return safetyStrategies; }
    public List<String> safetyStrategies() { return safetyStrategies; }

    public List<String> getWarningSignsIdentified() { return warningSignsIdentified; }
    public List<String> warningSignsIdentified() { return warningSignsIdentified; }

    public List<String> getResourcesIdentified() { return resourcesIdentified; }
    public List<String> resourcesIdentified() { return resourcesIdentified; }

    public String getEmergencyContact() { return emergencyContact; }
    public String emergencyContact() { return emergencyContact; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String emergencyContactPhone() { return emergencyContactPhone; }

    public String getSafeLocation() { return safeLocation; }
    public String safeLocation() { return safeLocation; }

    public String getUpdatedBy() { return updatedBy; }
    public String updatedBy() { return updatedBy; }

    public UUID getUpdatedByUserId() { return updatedByUserId; }
    public UUID updatedByUserId() { return updatedByUserId; }

    public String getUpdateReason() { return updateReason; }
    public String updateReason() { return updateReason; }
}