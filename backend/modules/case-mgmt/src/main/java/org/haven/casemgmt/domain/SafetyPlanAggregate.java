package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Safety Plan aggregate for DV survivor safety planning
 * Contains safety strategies, warning signs, and emergency procedures
 */
public class SafetyPlanAggregate extends AggregateRoot<SafetyPlanId> {
    
    private ClientId clientId;
    private CaseId caseId;
    private String planTitle;
    private LocalDate createdDate;
    private LocalDate lastUpdatedDate;
    private String createdBy;
    private String lastUpdatedBy;
    private SafetyPlanStatus status;
    
    // Safety planning components
    private List<String> safetyStrategies = new ArrayList<>();
    private List<String> warningSignsIdentified = new ArrayList<>();
    private List<String> resourcesIdentified = new ArrayList<>();
    private String emergencyContact;
    private String emergencyContactPhone;
    private String safeLocation;
    private String codeWord;
    private List<String> supportNetwork = new ArrayList<>();
    private String legalProtections;
    private String childrenSafetyPlan;
    private String workplaceSafetyPlan;
    
    // Review and effectiveness tracking
    private LocalDate nextReviewDate;
    private String effectivenessNotes;
    private Instant createdAt;
    private Instant lastModified;
    
    public static SafetyPlanAggregate create(ClientId clientId, CaseId caseId, String planTitle,
                                           List<String> safetyStrategies, List<String> warningSignsIdentified,
                                           List<String> resourcesIdentified, String emergencyContact,
                                           String emergencyContactPhone, String safeLocation,
                                           String createdBy) {
        SafetyPlanId planId = SafetyPlanId.generate();
        SafetyPlanAggregate plan = new SafetyPlanAggregate();
        plan.apply(new SafetyPlanCreated(
            planId.value(),
            clientId.value(),
            caseId.value(),
            planTitle,
            safetyStrategies,
            warningSignsIdentified,
            resourcesIdentified,
            emergencyContact,
            emergencyContactPhone,
            safeLocation,
            createdBy,
            Instant.now()
        ));
        return plan;
    }
    
    public void update(List<String> safetyStrategies, List<String> warningSignsIdentified,
                      List<String> resourcesIdentified, String emergencyContact,
                      String emergencyContactPhone, String safeLocation,
                      String codeWord, List<String> supportNetwork, String legalProtections,
                      String childrenSafetyPlan, String workplaceSafetyPlan,
                      String updatedBy, String updateReason) {
        apply(new SafetyPlanUpdated(
            id.value(),
            clientId.value(),
            safetyStrategies,
            warningSignsIdentified,
            resourcesIdentified,
            emergencyContact,
            emergencyContactPhone,
            safeLocation,
            codeWord,
            supportNetwork,
            legalProtections,
            childrenSafetyPlan,
            workplaceSafetyPlan,
            updatedBy,
            updateReason,
            Instant.now()
        ));
    }
    
    public void scheduleReview(LocalDate reviewDate, String reason) {
        this.nextReviewDate = reviewDate;
    }
    
    public void markInactive(String reason) {
        this.status = SafetyPlanStatus.INACTIVE;
        this.lastModified = Instant.now();
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof SafetyPlanCreated e) {
            this.id = new SafetyPlanId(e.safetyPlanId());
            this.clientId = new ClientId(e.clientId());
            this.caseId = new CaseId(e.caseId());
            this.planTitle = e.planTitle();
            this.safetyStrategies = new ArrayList<>(e.safetyStrategies());
            this.warningSignsIdentified = new ArrayList<>(e.warningSignsIdentified());
            this.resourcesIdentified = new ArrayList<>(e.resourcesIdentified());
            this.emergencyContact = e.emergencyContact();
            this.emergencyContactPhone = e.emergencyContactPhone();
            this.safeLocation = e.safeLocation();
            this.createdBy = e.createdBy();
            this.createdDate = LocalDate.now();
            this.status = SafetyPlanStatus.ACTIVE;
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();
        } else if (event instanceof SafetyPlanUpdated e) {
            this.safetyStrategies = new ArrayList<>(e.safetyStrategies());
            this.warningSignsIdentified = new ArrayList<>(e.warningSignsIdentified());
            this.resourcesIdentified = new ArrayList<>(e.resourcesIdentified());
            this.emergencyContact = e.emergencyContact();
            this.emergencyContactPhone = e.emergencyContactPhone();
            this.safeLocation = e.safeLocation();
            this.codeWord = e.codeWord();
            this.supportNetwork = new ArrayList<>(e.supportNetwork() != null ? e.supportNetwork() : List.of());
            this.legalProtections = e.legalProtections();
            this.childrenSafetyPlan = e.childrenSafetyPlan();
            this.workplaceSafetyPlan = e.workplaceSafetyPlan();
            this.lastUpdatedBy = e.updatedBy();
            this.lastUpdatedDate = LocalDate.now();
            this.lastModified = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum SafetyPlanStatus {
        ACTIVE, INACTIVE, SUPERSEDED
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public CaseId getCaseId() { return caseId; }
    public String getPlanTitle() { return planTitle; }
    public LocalDate getCreatedDate() { return createdDate; }
    public LocalDate getLastUpdatedDate() { return lastUpdatedDate; }
    public String getCreatedBy() { return createdBy; }
    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public SafetyPlanStatus getStatus() { return status; }
    public List<String> getSafetyStrategies() { return List.copyOf(safetyStrategies); }
    public List<String> getWarningSignsIdentified() { return List.copyOf(warningSignsIdentified); }
    public List<String> getResourcesIdentified() { return List.copyOf(resourcesIdentified); }
    public String getEmergencyContact() { return emergencyContact; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getSafeLocation() { return safeLocation; }
    public String getCodeWord() { return codeWord; }
    public List<String> getSupportNetwork() { return List.copyOf(supportNetwork); }
    public String getLegalProtections() { return legalProtections; }
    public String getChildrenSafetyPlan() { return childrenSafetyPlan; }
    public String getWorkplaceSafetyPlan() { return workplaceSafetyPlan; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public String getEffectivenessNotes() { return effectivenessNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    public boolean isActive() {
        return status == SafetyPlanStatus.ACTIVE;
    }
    
    public boolean isOverdueForReview() {
        return nextReviewDate != null && LocalDate.now().isAfter(nextReviewDate);
    }
}