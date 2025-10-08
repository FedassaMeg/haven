package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SafetyPlanUpdated extends DomainEvent {
    private final UUID clientId;
    private final List<String> safetyStrategies;
    private final List<String> warningSignsIdentified;
    private final List<String> resourcesIdentified;
    private final String emergencyContact;
    private final String emergencyContactPhone;
    private final String safeLocation;
    private final String codeWord;
    private final List<String> supportNetwork;
    private final String legalProtections;
    private final String childrenSafetyPlan;
    private final String workplaceSafetyPlan;
    private final String updatedBy;
    private final String updateReason;

    public SafetyPlanUpdated(UUID safetyPlanId, UUID clientId, List<String> safetyStrategies, List<String> warningSignsIdentified, List<String> resourcesIdentified, String emergencyContact, String emergencyContactPhone, String safeLocation, String codeWord, List<String> supportNetwork, String legalProtections, String childrenSafetyPlan, String workplaceSafetyPlan, String updatedBy, String updateReason, Instant occurredAt) {
        super(safetyPlanId, occurredAt != null ? occurredAt : Instant.now());
        if (safetyPlanId == null) throw new IllegalArgumentException("Safety plan ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");

        this.clientId = clientId;
        this.safetyStrategies = safetyStrategies;
        this.warningSignsIdentified = warningSignsIdentified;
        this.resourcesIdentified = resourcesIdentified;
        this.emergencyContact = emergencyContact;
        this.emergencyContactPhone = emergencyContactPhone;
        this.safeLocation = safeLocation;
        this.codeWord = codeWord;
        this.supportNetwork = supportNetwork;
        this.legalProtections = legalProtections;
        this.childrenSafetyPlan = childrenSafetyPlan;
        this.workplaceSafetyPlan = workplaceSafetyPlan;
        this.updatedBy = updatedBy;
        this.updateReason = updateReason;
    }

    public UUID clientId() {
        return clientId;
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

    public String codeWord() {
        return codeWord;
    }

    public List<String> supportNetwork() {
        return supportNetwork;
    }

    public String legalProtections() {
        return legalProtections;
    }

    public String childrenSafetyPlan() {
        return childrenSafetyPlan;
    }

    public String workplaceSafetyPlan() {
        return workplaceSafetyPlan;
    }

    public String updatedBy() {
        return updatedBy;
    }

    public String updateReason() {
        return updateReason;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public List<String> getSafetyStrategies() { return safetyStrategies; }
    public List<String> getWarningSignsIdentified() { return warningSignsIdentified; }
    public List<String> getResourcesIdentified() { return resourcesIdentified; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getSafeLocation() { return safeLocation; }
    public String getCodeWord() { return codeWord; }
    public List<String> getSupportNetwork() { return supportNetwork; }
    public String getLegalProtections() { return legalProtections; }
    public String getChildrenSafetyPlan() { return childrenSafetyPlan; }
    public String getWorkplaceSafetyPlan() { return workplaceSafetyPlan; }
    public String getUpdatedBy() { return updatedBy; }
    public String getUpdateReason() { return updateReason; }
}