package org.haven.safetyassessment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Safety Plan with confidential history tracking
 * Access controlled by role-based permissions
 */
public class SafetyPlan {
    private UUID safetyPlanId;
    private UUID currentVersionId;
    private SafetyPlanStatus status;
    
    // Core safety planning elements
    private List<String> warningSignsOfDanger = new ArrayList<>();
    private List<String> copingStrategies = new ArrayList<>();
    private List<SafetyContact> emergencyContacts = new ArrayList<>();
    private List<SafetyContact> supportContacts = new ArrayList<>();
    private String safeLocationPlan;
    private String childrenSafetyPlan;
    private String workplaceSafetyPlan;
    private String transportationPlan;
    private List<String> importantDocumentsToGather = new ArrayList<>();
    private List<String> financialSafetyPlan = new ArrayList<>();
    private String legalSafetyPlan;
    private String emotionalSafetyPlan;
    
    // Plan metadata
    private String developedBy;
    private String developedWith; // Client involvement level
    private LocalDate lastReviewDate;
    private LocalDate nextReviewDue;
    private String reviewNotes;
    private Integer planVersionNumber;
    
    // Confidentiality and access
    private SafetyPlanConfidentialityLevel confidentialityLevel;
    private List<String> authorizedViewers = new ArrayList<>();
    private boolean isClientCopyProvided;
    
    private Instant createdAt;
    private Instant lastModified;
    
    public SafetyPlan(String developedBy, SafetyPlanConfidentialityLevel confidentialityLevel) {
        this.safetyPlanId = UUID.randomUUID();
        this.currentVersionId = UUID.randomUUID();
        this.developedBy = developedBy;
        this.confidentialityLevel = confidentialityLevel;
        this.status = SafetyPlanStatus.DRAFT;
        this.planVersionNumber = 1;
        this.createdAt = Instant.now();
        this.lastModified = Instant.now();
        this.nextReviewDue = LocalDate.now().plusMonths(3); // Default 3-month review cycle
    }
    
    public void addWarningSign(String warningSign) {
        this.warningSignsOfDanger.add(warningSign);
        updateModificationTime();
    }
    
    public void addCopingStrategy(String strategy) {
        this.copingStrategies.add(strategy);
        updateModificationTime();
    }
    
    public void addEmergencyContact(SafetyContact contact) {
        contact.setContactType(ContactType.EMERGENCY);
        this.emergencyContacts.add(contact);
        updateModificationTime();
    }
    
    public void addSupportContact(SafetyContact contact) {
        contact.setContactType(ContactType.SUPPORT);
        this.supportContacts.add(contact);
        updateModificationTime();
    }
    
    public void updateSafeLocationPlan(String plan) {
        this.safeLocationPlan = plan;
        updateModificationTime();
    }
    
    public void updateChildrenSafetyPlan(String plan) {
        this.childrenSafetyPlan = plan;
        updateModificationTime();
    }
    
    public void activatePlan(String activatedBy) {
        this.status = SafetyPlanStatus.ACTIVE;
        this.lastReviewDate = LocalDate.now();
        updateModificationTime();
    }
    
    public void markForReview(String reason) {
        this.status = SafetyPlanStatus.NEEDS_REVIEW;
        this.reviewNotes = reason;
        updateModificationTime();
    }
    
    public SafetyPlan createNewVersion(String updatedBy, String changeReason) {
        // Create new version while preserving history
        SafetyPlan newVersion = new SafetyPlan(updatedBy, this.confidentialityLevel);
        
        // Copy current data to new version
        newVersion.planVersionNumber = this.planVersionNumber + 1;
        newVersion.warningSignsOfDanger = new ArrayList<>(this.warningSignsOfDanger);
        newVersion.copingStrategies = new ArrayList<>(this.copingStrategies);
        newVersion.emergencyContacts = new ArrayList<>(this.emergencyContacts);
        newVersion.supportContacts = new ArrayList<>(this.supportContacts);
        newVersion.safeLocationPlan = this.safeLocationPlan;
        newVersion.childrenSafetyPlan = this.childrenSafetyPlan;
        newVersion.workplaceSafetyPlan = this.workplaceSafetyPlan;
        newVersion.transportationPlan = this.transportationPlan;
        newVersion.importantDocumentsToGather = new ArrayList<>(this.importantDocumentsToGather);
        newVersion.financialSafetyPlan = new ArrayList<>(this.financialSafetyPlan);
        newVersion.legalSafetyPlan = this.legalSafetyPlan;
        newVersion.emotionalSafetyPlan = this.emotionalSafetyPlan;
        newVersion.authorizedViewers = new ArrayList<>(this.authorizedViewers);
        
        // Archive current version
        this.status = SafetyPlanStatus.SUPERSEDED;
        
        return newVersion;
    }
    
    public void authorizeViewer(String userId, String role) {
        if (!authorizedViewers.contains(userId)) {
            authorizedViewers.add(userId);
        }
        updateModificationTime();
    }
    
    public void revokeViewerAccess(String userId) {
        authorizedViewers.remove(userId);
        updateModificationTime();
    }
    
    public boolean canUserAccess(String userId, String userRole) {
        // Always allow the plan developer to access
        if (developedBy.equals(userId)) {
            return true;
        }
        
        // Check confidentiality level permissions
        switch (confidentialityLevel) {
            case PUBLIC -> {
                return true; // All staff can view
            }
            case RESTRICTED -> {
                return authorizedViewers.contains(userId) || 
                       "SUPERVISOR".equals(userRole) || 
                       "ADMIN".equals(userRole);
            }
            case CONFIDENTIAL -> {
                return authorizedViewers.contains(userId) || 
                       "ADMIN".equals(userRole);
            }
            case TOP_SECRET -> {
                return authorizedViewers.contains(userId);
            }
        }
        
        return false;
    }
    
    private void updateModificationTime() {
        this.lastModified = Instant.now();
    }
    
    public boolean isOverdueForReview() {
        return nextReviewDue != null && LocalDate.now().isAfter(nextReviewDue);
    }
    
    public enum SafetyPlanStatus {
        DRAFT,
        ACTIVE,
        NEEDS_REVIEW,
        SUPERSEDED,
        ARCHIVED
    }
    
    public enum SafetyPlanConfidentialityLevel {
        PUBLIC("All staff can view"),
        RESTRICTED("Authorized viewers and supervisors only"),
        CONFIDENTIAL("Authorized viewers and admin only"), 
        TOP_SECRET("Explicitly authorized viewers only");
        
        private final String description;
        
        SafetyPlanConfidentialityLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public static class SafetyContact {
        private String name;
        private String relationship;
        private String phoneNumber;
        private String alternatePhone;
        private String email;
        private String address;
        private ContactType contactType;
        private boolean isAvailable24Hour;
        private String specialInstructions;
        private boolean canProvideTransportation;
        private boolean canProvideEmergencyHousing;
        
        public SafetyContact(String name, String relationship, String phoneNumber) {
            this.name = name;
            this.relationship = relationship;
            this.phoneNumber = phoneNumber;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getAlternatePhone() { return alternatePhone; }
        public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public ContactType getContactType() { return contactType; }
        public void setContactType(ContactType contactType) { this.contactType = contactType; }
        public boolean isAvailable24Hour() { return isAvailable24Hour; }
        public void setAvailable24Hour(boolean available24Hour) { isAvailable24Hour = available24Hour; }
        public String getSpecialInstructions() { return specialInstructions; }
        public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
        public boolean canProvideTransportation() { return canProvideTransportation; }
        public void setCanProvideTransportation(boolean canProvideTransportation) { this.canProvideTransportation = canProvideTransportation; }
        public boolean canProvideEmergencyHousing() { return canProvideEmergencyHousing; }
        public void setCanProvideEmergencyHousing(boolean canProvideEmergencyHousing) { this.canProvideEmergencyHousing = canProvideEmergencyHousing; }
    }
    
    public enum ContactType {
        EMERGENCY,
        SUPPORT
    }
    
    // Getters
    public UUID getSafetyPlanId() { return safetyPlanId; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public SafetyPlanStatus getStatus() { return status; }
    public List<String> getWarningSignsOfDanger() { return List.copyOf(warningSignsOfDanger); }
    public List<String> getCopingStrategies() { return List.copyOf(copingStrategies); }
    public List<SafetyContact> getEmergencyContacts() { return List.copyOf(emergencyContacts); }
    public List<SafetyContact> getSupportContacts() { return List.copyOf(supportContacts); }
    public String getSafeLocationPlan() { return safeLocationPlan; }
    public String getChildrenSafetyPlan() { return childrenSafetyPlan; }
    public String getWorkplaceSafetyPlan() { return workplaceSafetyPlan; }
    public String getTransportationPlan() { return transportationPlan; }
    public List<String> getImportantDocumentsToGather() { return List.copyOf(importantDocumentsToGather); }
    public List<String> getFinancialSafetyPlan() { return List.copyOf(financialSafetyPlan); }
    public String getLegalSafetyPlan() { return legalSafetyPlan; }
    public String getEmotionalSafetyPlan() { return emotionalSafetyPlan; }
    public String getDevelopedBy() { return developedBy; }
    public String getDevelopedWith() { return developedWith; }
    public LocalDate getLastReviewDate() { return lastReviewDate; }
    public LocalDate getNextReviewDue() { return nextReviewDue; }
    public String getReviewNotes() { return reviewNotes; }
    public Integer getPlanVersionNumber() { return planVersionNumber; }
    public SafetyPlanConfidentialityLevel getConfidentialityLevel() { return confidentialityLevel; }
    public List<String> getAuthorizedViewers() { return List.copyOf(authorizedViewers); }
    public boolean isClientCopyProvided() { return isClientCopyProvided; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    // Setters for plan content
    public void setWorkplaceSafetyPlan(String workplaceSafetyPlan) { 
        this.workplaceSafetyPlan = workplaceSafetyPlan; 
        updateModificationTime(); 
    }
    public void setTransportationPlan(String transportationPlan) { 
        this.transportationPlan = transportationPlan; 
        updateModificationTime(); 
    }
    public void setLegalSafetyPlan(String legalSafetyPlan) { 
        this.legalSafetyPlan = legalSafetyPlan; 
        updateModificationTime(); 
    }
    public void setEmotionalSafetyPlan(String emotionalSafetyPlan) { 
        this.emotionalSafetyPlan = emotionalSafetyPlan; 
        updateModificationTime(); 
    }
    public void setDevelopedWith(String developedWith) { 
        this.developedWith = developedWith; 
        updateModificationTime(); 
    }
    public void setNextReviewDue(LocalDate nextReviewDue) { 
        this.nextReviewDue = nextReviewDue; 
        updateModificationTime(); 
    }
    public void setClientCopyProvided(boolean clientCopyProvided) { 
        isClientCopyProvided = clientCopyProvided; 
        updateModificationTime(); 
    }
}