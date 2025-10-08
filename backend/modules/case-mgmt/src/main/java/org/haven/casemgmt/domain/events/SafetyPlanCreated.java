package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SafetyPlanCreated extends DomainEvent {
    private final UUID clientId;
    private final UUID caseId;
    private final String planTitle;
    private final List<String> safetyStrategies;
    private final List<String> warningSignsIdentified;
    private final List<String> resourcesIdentified;
    private final String emergencyContact;
    private final String emergencyContactPhone;
    private final String safeLocation;
    private final String createdBy;

    public SafetyPlanCreated(UUID safetyPlanId, UUID clientId, UUID caseId, String planTitle, List<String> safetyStrategies, List<String> warningSignsIdentified, List<String> resourcesIdentified, String emergencyContact, String emergencyContactPhone, String safeLocation, String createdBy, Instant occurredAt) {
        super(safetyPlanId, occurredAt != null ? occurredAt : Instant.now());
        if (safetyPlanId == null) throw new IllegalArgumentException("Safety plan ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (planTitle == null || planTitle.trim().isEmpty()) throw new IllegalArgumentException("Plan title cannot be null or empty");
        if (createdBy == null || createdBy.trim().isEmpty()) throw new IllegalArgumentException("Created by cannot be null or empty");

        this.clientId = clientId;
        this.caseId = caseId;
        this.planTitle = planTitle;
        this.safetyStrategies = safetyStrategies;
        this.warningSignsIdentified = warningSignsIdentified;
        this.resourcesIdentified = resourcesIdentified;
        this.emergencyContact = emergencyContact;
        this.emergencyContactPhone = emergencyContactPhone;
        this.safeLocation = safeLocation;
        this.createdBy = createdBy;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID caseId() {
        return caseId;
    }

    public String planTitle() {
        return planTitle;
    }

    public List<String> safetyStrategies() {
        return safetyStrategies;
    }

    public List<String> warningSignsIdentified() {
        return warningSignsIdentified;
    }

    public List<String> resourcesIdentified() {
        return resourcesIdentified;
    }

    public String emergencyContact() {
        return emergencyContact;
    }

    public String emergencyContactPhone() {
        return emergencyContactPhone;
    }

    public String safeLocation() {
        return safeLocation;
    }

    public String createdBy() {
        return createdBy;
    }


    public UUID safetyPlanId() {
        return getAggregateId();
    }

    @Override
    public String eventType() {
        return "SafetyPlanCreated";
    }

    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getCaseId() { return caseId; }
    public String getPlanTitle() { return planTitle; }
    public List<String> getSafetyStrategies() { return safetyStrategies; }
    public List<String> getWarningSignsIdentified() { return warningSignsIdentified; }
    public List<String> getResourcesIdentified() { return resourcesIdentified; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getSafeLocation() { return safeLocation; }
    public String getCreatedBy() { return createdBy; }
}