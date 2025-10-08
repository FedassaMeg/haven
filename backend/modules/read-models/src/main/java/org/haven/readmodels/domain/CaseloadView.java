package org.haven.readmodels.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CaseloadView {
    private UUID caseId;
    private String caseNumber;
    private UUID clientId;
    private String clientName;
    private UUID workerId;
    private String workerName;
    private CaseStage stage;
    private RiskLevel riskLevel;
    private String programName;
    private UUID programId;
    private LocalDate enrollmentDate;
    private LocalDate lastServiceDate;
    private Integer serviceCount;
    private Integer daysSinceLastContact;
    private List<String> activeAlerts;
    private CaseStatus status;
    private Boolean requiresAttention;
    private String primaryNeed;
    private Instant lastUpdated;
    
    // Confidentiality flags
    private Boolean isSafeAtHome;
    private Boolean isConfidentialLocation;
    private String dataSystem; // HMIS or COMPARABLE_DB
    
    public enum CaseStage {
        INTAKE,           // Initial enrollment, assessment phase
        ACTIVE,           // Receiving regular services
        HOUSING_SEARCH,   // Actively searching for housing
        STABILIZATION,    // Post-housing stabilization services
        EXIT_PLANNING,    // Preparing for program exit
        FOLLOW_UP,        // Post-exit follow-up period
        CLOSED            // Case closed
    }
    
    public enum RiskLevel {
        CRITICAL,  // Lethality assessment indicates immediate danger
        HIGH,      // Significant safety concerns
        MEDIUM,    // Moderate risk factors present
        LOW,       // Minimal risk factors
        STABLE     // No current risk factors identified
    }
    
    public enum CaseStatus {
        OPEN,
        IN_PROGRESS,
        ON_HOLD,
        CLOSED,
        CANCELLED
    }
    
    // Constructor
    public CaseloadView() {}
    
    // Methods
    public boolean needsUrgentAttention() {
        return riskLevel == RiskLevel.CRITICAL || 
               (riskLevel == RiskLevel.HIGH && daysSinceLastContact != null && daysSinceLastContact > 3);
    }
    
    public boolean isOverdue() {
        return daysSinceLastContact != null && daysSinceLastContact > 7;
    }
    
    public String getStageDescription() {
        switch (stage) {
            case INTAKE:
                return "Client intake and assessment in progress";
            case ACTIVE:
                return "Actively receiving services";
            case HOUSING_SEARCH:
                return "Searching for safe housing";
            case STABILIZATION:
                return "Housing secured, stabilization services ongoing";
            case EXIT_PLANNING:
                return "Preparing for program completion";
            case FOLLOW_UP:
                return "Post-exit follow-up period";
            case CLOSED:
                return "Case closed";
            default:
                return "Unknown stage";
        }
    }
    
    // Getters and Setters
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public UUID getWorkerId() { return workerId; }
    public void setWorkerId(UUID workerId) { this.workerId = workerId; }
    
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    
    public CaseStage getStage() { return stage; }
    public void setStage(CaseStage stage) { this.stage = stage; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    
    public LocalDate getLastServiceDate() { return lastServiceDate; }
    public void setLastServiceDate(LocalDate lastServiceDate) { this.lastServiceDate = lastServiceDate; }
    
    public Integer getServiceCount() { return serviceCount; }
    public void setServiceCount(Integer serviceCount) { this.serviceCount = serviceCount; }
    
    public Integer getDaysSinceLastContact() { return daysSinceLastContact; }
    public void setDaysSinceLastContact(Integer daysSinceLastContact) { this.daysSinceLastContact = daysSinceLastContact; }
    
    public List<String> getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(List<String> activeAlerts) { this.activeAlerts = activeAlerts; }
    
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    
    public Boolean getRequiresAttention() { return requiresAttention; }
    public void setRequiresAttention(Boolean requiresAttention) { this.requiresAttention = requiresAttention; }
    
    public String getPrimaryNeed() { return primaryNeed; }
    public void setPrimaryNeed(String primaryNeed) { this.primaryNeed = primaryNeed; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Boolean getIsSafeAtHome() { return isSafeAtHome; }
    public void setIsSafeAtHome(Boolean isSafeAtHome) { this.isSafeAtHome = isSafeAtHome; }
    
    public Boolean getIsConfidentialLocation() { return isConfidentialLocation; }
    public void setIsConfidentialLocation(Boolean isConfidentialLocation) { this.isConfidentialLocation = isConfidentialLocation; }
    
    public String getDataSystem() { return dataSystem; }
    public void setDataSystem(String dataSystem) { this.dataSystem = dataSystem; }
}